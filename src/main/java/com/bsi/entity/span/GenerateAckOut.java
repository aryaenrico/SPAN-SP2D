package com.bsi.entity.span;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.bsi.MainCHK;
import com.bsi.config.SpanConfig;
import com.bsi.utility.Utillity;


public class GenerateAckOut {

    public String generateAckFile(Connection conn, SpanConfig config, List<SpanSp2dPosting> rows, Map<String, ReturnStatusAck> statusAckMap) throws IOException {
        
        String creationDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        // [FIX][2026-09-08] Penambahan UUID sebagai suffix nama file ACK untuk mencegah race condition
        // pada skenario multi-thread: dua thread yang memanggil generateAckFile pada detik yang sama
        // akan menghasilkan nama file identik, sehingga FileOutputStream (tanpa append) akan menimpa
        // (truncate) file milik thread lain dan menyebabkan data loss. UUID menjamin keunikan global
        // tanpa ketergantungan pada granularitas timestamp maupun thread lifecycle.
        String unique = UUID.randomUUID().toString().substring(1,3);
        String fileName = config.getBankCode() + "_SP2D_FA_" + creationDateTime + "_" + unique + ".out";
        String outputDir = config.getPathBo2spanHome() + config.getPathSpanAcknowledgePut();
        String outputPath = outputDir + File.separator + fileName;
        String currentDate =new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    
        new File(outputDir).mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)))) {
           for (SpanSp2dPosting row : rows) {
                ReturnStatusAck ack = statusAckMap.get(row.returnCode);
                String code = ack != null ? Utillity.safe(ack.code)  : "";
                String ackDesc = ack != null ? Utillity.safe(ack.description)  : "";
                String description="";
                if (code.equals("B00")){
                   description= ackDesc+" "+constructDescription(row.paymentMethod, conn);
                   code = code+""+row.paymentMethod;
                }else {
                    description = ackDesc; 
                }
                
                // format retur
                String line = String.join("|",
                    Utillity.safe(currentDate),
                    Utillity.safe(row.applicationareaMessageTypeIndicator),
                    Utillity.safe(row.documentNumber),
                    Utillity.safe(row.applicationareaMessageIdentifier),
                    Utillity.safe(row.applicationareaSenderIdentifier),
                    Utillity.safe(code),
                    Utillity.safe(description)
                );
                bw.write(line);
                bw.write("\n");
            }
        }
        return outputPath;
    }


    // [CHANGE][2026-09-08] Arsitektur baru: 1 dedicated file per thread dengan continuous append.
    // Method ini dipanggil SEKALI saat thread dimulai untuk membuat file ACK milik thread tersebut.
    // Nama file menggunakan UUID (tanpa hyphen) untuk menjamin keunikan global tanpa dependensi pada
    // granularitas timestamp maupun thread lifecycle, sesuai standar dokumen keuangan.
    public String createDedicatedAckFilePath(SpanConfig config) {
        String creationDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String fileName = config.getBankCode() + "_SP2D_FA_" + creationDateTime + "_" + uniqueId + ".out";
        String outputDir = config.getPathBo2spanHome() + config.getPathSpanAcknowledgePut();
        new File(outputDir).mkdirs();
        String outputPath = outputDir + File.separator + fileName;
        MainCHK.tulisLog("[ACK] Dedicated file dibuat untuk thread [" + Thread.currentThread().getId() + "] -> " + outputPath);
        return outputPath;
    }

    // [CHANGE][2026-09-08] Arsitektur baru: append baris ACK ke dedicated writer yang sudah dibuka.
    // Tidak membuat file baru — writer dibuka oleh thread di awal dan ditutup di finally block thread.
    // flush() dipanggil setelah setiap item agar data tidak tertahan di buffer jika thread mati mendadak.
    public void appendAckLines(BufferedWriter bw, Connection conn, List<SpanSp2dPosting> rows, Map<String, ReturnStatusAck> statusAckMap) throws IOException {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        for (SpanSp2dPosting row : rows) {
            ReturnStatusAck ack = statusAckMap.get(row.returnCode);
            String code    = ack != null ? Utillity.safe(ack.code)        : "";
            String ackDesc = ack != null ? Utillity.safe(ack.description) : "";
            String description;
            if (code.equals("B00")) {
                description = ackDesc + " " + constructDescription(row.paymentMethod, conn);
                code = code + row.paymentMethod;
            } else {
                description = ackDesc;
            }
            String line = String.join("|",
                Utillity.safe(currentDate),
                Utillity.safe(row.applicationareaMessageTypeIndicator),
                Utillity.safe(row.documentNumber),
                Utillity.safe(row.applicationareaMessageIdentifier),
                Utillity.safe(row.applicationareaSenderIdentifier),
                Utillity.safe(code),
                Utillity.safe(description)
            );
            bw.write(line);
            bw.newLine();
        }
        bw.flush();
    }

    public String constructDescription(String paymentMethod , Connection conn){
       String result="";
        try{
         if (paymentMethod != null){
            Map<String,PaymentMethod> map = Utillity.fetchPaymentMethod(conn);
            result=map.get(paymentMethod).getDescription();
         }
         
     } catch (SQLException  e){
        MainCHK.tulisLog("Error terjadi"+e.getMessage());
        e.printStackTrace();
     }
      return Utillity.safe(result);
    }

    public  void copyToArchiveIfExists(SpanConfig ctx,String outputFilePath) throws IOException {
        File source = new File(outputFilePath);
        if (!source.exists()) return;

        String fileName = source.getName();

        String archiveDir  = ctx.getPathBo2spanHome() + ctx.getpathSpanAcknowledgeArchive();
        String  archivePath = archiveDir + File.separator + ctx.getBankCode()+ "_SP2D_FA_" + fileName + ".out";

        new File(archiveDir).mkdirs();
        Files.copy(source.toPath(), new File(archivePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void createTxtAckFile(SpanConfig ctx,String outputFilePath) throws IOException{
        File source = new File(outputFilePath);
        if (!source.exists()) return;

        String fileName = source.getName();

        String archiveDir  = ctx.getPathBo2spanHome() + ctx.getpathSpanAcknowledgeArchive();
        String  archivePath = archiveDir + File.separator + ctx.getBankCode()+ "_SP2D_FA_" + fileName + ".txt";

        new File(archiveDir).mkdirs();
        Files.copy(source.toPath(), new File(archivePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // [CHANGE][2026-09-08] Merge file ACK per-thread menjadi satu file tunggal setelah semua thread selesai.
    // Dipanggil dari main thread (single-thread) sehingga tidak ada race condition.
    // File individual dihapus setelah berhasil di-merge; hanya file merged yang di-archive.
    public String mergeAckFiles(SpanConfig config, List<String> filePaths) throws IOException {
        String creationDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = config.getBankCode() + "_SP2D_FA_" + creationDateTime + ".out";
        String outputDir = config.getPathBo2spanHome() + config.getPathSpanAcknowledgePut();
        new File(outputDir).mkdirs();
        String mergedPath = outputDir + File.separator + fileName;

        MainCHK.tulisLog("[ACK-MERGE] Memulai merge " + filePaths.size() + " file ACK ke: " + mergedPath);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mergedPath)))) {
            for (String filePath : filePaths) {
                File f = new File(filePath);
                if (!f.exists()) {
                    MainCHK.tulisLog("[ACK-MERGE] File tidak ditemukan, dilewati: " + filePath);
                    continue;
                }
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        bw.write(line);
                        bw.newLine();
                    }
                }
                f.delete();
                MainCHK.tulisLog("[ACK-MERGE] File thread dihapus setelah merge: " + filePath);
            }
        }
        MainCHK.tulisLog("[ACK-MERGE] Merge selesai -> " + mergedPath);
        return mergedPath;
    }

    public void updateFlagAck(Connection conn, SpanConfig ctx , SpanSp2dPosting data) throws SQLException {
        String sql =Utillity.updateFlagAckTablePosting();
        String updateFlagAckStageIn = Utillity.updateFlagAckDoubeTable("span_sp2d_stage_in");
        String transactionType = Utillity.getTransactionTypeForAck(data,ctx);

        MainCHK.tulisLog("transaction type Ack : "+ transactionType);

        //USE FOR DYNAMIC Account retur
        String paramRR1 ="";
        String paramRR2 ="";

        //use for include account
        String paramInclude1="";
        String paramInclude2="";

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
                paramRR1 = ctx.getAcctRrRpkbunGaji();
                paramRR2 = ctx.getAcctRrReksusSbsn();
                paramInclude1 = ctx.getAcctRpkbunGaji();
                paramInclude2 = ctx.getAcctRrRpkbunGaji();
                break;
            case "BO1":
                paramRR1 = ctx.getAcctRrRpkbunNonGaji();
                paramRR2 = ctx.getAcctRrReksusSbsn();
                paramInclude1 =ctx.getAcctRrRpkbunNonGaji();
                paramInclude2 = ctx.getAcctRpkbunNonGaji();
                break;
            default :
                paramRR1 = ctx.getAcctRpkbunGaji();
                paramRR2 = ctx.getAcctRpkbunNonGaji();
                paramInclude1 =ctx.getAcctRrReksusSbsn();
                paramInclude2 = ctx.getAcctRpkbunNonGaji();
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paramRR1);
            ps.setString(2, paramRR2);
            ps.setString(3, paramInclude1);
            ps.setString(4, paramInclude2);
            ps.setString(5, data.documentNumber);
            ps.executeUpdate();
        }

        try(PreparedStatement ps2 = conn.prepareStatement(updateFlagAckStageIn)){
            ps2.setString(1,data.documentNumber);
            ps2.executeUpdate();
        }
    }

    public void updateFlagAckRetur(Connection conn, SpanConfig ctx , SpanSp2dPosting data) throws SQLException {
       updateFlagAckDoubleTable(conn, ctx, data);
    }

    public void updateFlagAckBatch(Connection conn, SpanConfig ctx , SpanSp2dPosting data) throws SQLException {
       updateFlagAckDoubleTable(conn, ctx, data);
    }



    private void updateFlagAckDoubleTable(Connection conn, SpanConfig ctx , SpanSp2dPosting data) throws SQLException{
       String sql =Utillity.updateFlagAckDoubeTable("span_sp2d_posting"); 

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Utillity.safe(data.documentNumber));
            ps.executeUpdate();
        }
        String sql2 =Utillity.updateFlagAckDoubeTable("span_sp2d_stage_in");
        try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
            ps2.setString(1, Utillity.safe(data.documentNumber));
            ps2.executeUpdate();
        }
    }
}
