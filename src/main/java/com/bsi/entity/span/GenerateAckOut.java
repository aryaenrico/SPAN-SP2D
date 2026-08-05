package com.bsi.entity.span;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import com.bsi.MainCHK;
import com.bsi.config.SpanConfig;
import com.bsi.utility.Utillity;

public class GenerateAckOut {
    String tanggal;
    String type ;
    String documentNumber;
    String applicationareamessageidentifier;
    String returnCode;
    String description;

    public String generateAckFile(Connection conn, SpanConfig config,List<SpanSp2dPosting> rows, Map<String, ReturnStatusAck> statusAckMap) throws IOException {
        
        String creationDateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = config.getBankCode() + "_SP2D_FA_" + creationDateTime + ".out";
        String outputDir = "C:/Users/ven.arya/Downloads/Project/2026/SPAN/Custom Handler/put";
        //String outputDir = config.getPathBo2spanHome() + config.getPathSpanAcknowledgePut();
        String outputPath = outputDir + File.separator + fileName;
        String currentDate =new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        
    
        new File(outputDir).mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)))) {
           for (SpanSp2dPosting row : rows) {
                ReturnStatusAck ack = statusAckMap.get(row.returnCode);
                MainCHK.tulisLog("payment Method :"+row.paymentMethod);
                
                String code = ack != null ? Utillity.safe(ack.code)  : "";
                String ackDesc = ack != null ? Utillity.safe(ack.description)  : "";
                String description="";
                if (code.equals("B00")){
                   description= ackDesc+" "+constructDescription(row.paymentMethod, conn);
                   code = code+""+row.paymentMethod;
                }else {
                    description = ackDesc; 
                }
                
                // Menulis data sesuai urutan tMap -> tFileOutputDelimited
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

    public  void copyToArchiveIfExists(SpanConfig ctx,String outputFilePath, String creationDateTime) throws IOException {
        File source = new File(outputFilePath);
        if (!source.exists()) return;

        //String archiveDir  = ctx.getPathBo2spanHome() + ctx.getpathSpanAcknowledgeArchive();
        String   archiveDir  ="C:/Users/ven.arya/Downloads/Project/2026/SPAN/Custom Handler/archive";
        String  archivePath = archiveDir + File.separator + ctx.getBankCode()+ "_SP2D_FA_" + creationDateTime + ".out";

        new File(archiveDir).mkdirs();
        Files.copy(source.toPath(), new File(archivePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public  void updateFlagAck(Connection conn, SpanConfig ctx , SpanSp2dPosting data) throws SQLException {
        String sql =Utillity.updateFlagAckTablePosting();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ctx.getAcctRrRpkbunGaji());
            ps.setString(2, ctx.getAcctRrReksusSbsn());
            ps.setString(3, ctx.getAcctRpkbunGaji());
            ps.setString(4, ctx.getAcctRrRpkbunGaji());
            ps.setString(5, data.documentNumber);
            ps.executeUpdate();
        }
    }

    public  void updateFlagAckRetur(Connection conn, SpanConfig ctx , SpanSp2dPosting data) throws SQLException {
        String sql =Utillity.updateFlagAckReturProses("span_sp2d_posting"); 

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Utillity.safe(data.documentNumber));
            ps.executeUpdate();
        }
        String sql2 =Utillity.updateFlagAckReturProses("span_sp2d_stage_in");
        try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
            ps2.setString(1, Utillity.safe(data.documentNumber));
            ps2.executeUpdate();
        }
    }
}
