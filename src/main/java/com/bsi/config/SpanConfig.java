package com.bsi.config;

import java.io.*;
import java.util.Properties;

public class SpanConfig {
    
    // Account Numbers
    String acctRpkbunGaji;
    String acctRrRpkbunGaji;

    String acctRpkbunNonGaji;
    String acctRrRpkbunNonGaji;

    String acctReksusSbsn;
    String acctRrReksusSbsn;

    // Bank / Branch
    String bankCode;
    String bankCodeBi;
    String branchCode;
    String rtgsCode;
    String currency = "IDR";

    // Dates
    String appDate;
    String appDateRealtime;

    // Paths
    String pathBo2spanHome;
    String pathBo2spanConfig;
    String pathCorePostingServiceRequest;
    String pathSpanAcknowledgePut ;
    String pathSpanAcknowledgeArchive;

    String acctIaKewajibanBifast;

    public static SpanConfig fromProperties(String location){
        Properties props = new Properties();
        InputStream in = null;
        try{
            File file = new File(location);
            if (file.isFile()){
                in = new FileInputStream(location);
            }else {
                in = BifastConfig.class.getClassLoader().getResourceAsStream(location);
            }

            if (in == null){
                throw new IllegalStateException("File Properties tidak ditemukan di filesystem maupun classpath"+location);
            }

            props.load(in);

        }catch(IOException e){
            throw new UncheckedIOException("gagal membaca file properties "+location,e);

        }finally{
            if (in != null){
                try {in.close();
                }catch(IOException ignore){}}
        }
        return fromProperties(props);
    }

    public static SpanConfig fromProperties(Properties prop){
        SpanConfig cfg = new SpanConfig();
        cfg.setPathBo2spanHome(trimToNull(prop.getProperty("path_bo2span_home")));
        cfg.setPathBo2spanConfig(trimToNull(prop.getProperty("path_bo2span_config")));
        cfg.setPathSpanAcknowledgePut(trimToNull(prop.getProperty("path_bo2span_span_acknowledge_put")));
        cfg.setPathSpanAcknowledgeArchive(trimToNull(prop.getProperty("path_bo2span_span_acknowledge_archive")));
        return cfg;
    }

    private static String trimToNull(String value){
        if (value==null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }


    public void setAcctIaKewajibanBifats(String iaKewajibanBifast){
     this.acctIaKewajibanBifast = iaKewajibanBifast;
    }

    public String getAcctIaKewajibanBifast(){
        return this.acctIaKewajibanBifast;
    }
    public String getpathSpanAcknowledgeArchive(){
        return this.pathSpanAcknowledgeArchive;
    }

    public String getAcctRrReksusSbsn() {
       return this.acctRrReksusSbsn;
   }
   
    public void setAcctRrReksusSbsn(String acctRrReksusSbsn) {
       this.acctRrReksusSbsn = acctRrReksusSbsn;
   }

    public String getAcctReksusSbsn() {
       return this.acctReksusSbsn;
   }
   
    public void setAcctReksusSbsn(String acctReksusSbsn) {
       this.acctReksusSbsn = acctReksusSbsn;
   }
   
    public String getAcctRrRpkbunGaji() {
       return acctRrRpkbunGaji;
   }
   
    public void setAcctRrRpkbunGaji(String acctRrRpkbunGaji) {
       this.acctRrRpkbunGaji = acctRrRpkbunGaji;
   }

    public String getAcctRpkbunNonGaji() {
       return this.acctRpkbunNonGaji;
   }
   
    public void setAcctRpkbunNonGaji(String acctRpkbunNonGaji) {
       this.acctRpkbunNonGaji = acctRpkbunNonGaji;
   }
   
    public String getAcctRrRpkbunNonGaji() {
       return acctRrRpkbunNonGaji;
   }
   
   public void setAcctRrRpkbunNonGaji(String acctRrRpkbunNonGaji) {
       this.acctRrRpkbunNonGaji = acctRrRpkbunNonGaji;
   }

   public String getAcctRpkbunGaji() {
       return acctRpkbunGaji;
   }
   
   public void setAcctRpkbunGaji(String acctRpkbunGaji) {
       this.acctRpkbunGaji = acctRpkbunGaji;
   }


   public String getBankCode() {
       return bankCode;
   }
   
   public void setBankCode(String bankCode) {
       this.bankCode = bankCode;
   }
   
   public String getBankCodeBi() {
       return bankCodeBi;
   }
   
   public void setBankCodeBi(String bankCodeBi) {
       this.bankCodeBi = bankCodeBi;
   }
   
   public String getBranchCode() {
       return branchCode;
   }
   
   public void setBranchCode(String branchCode) {
       this.branchCode = branchCode;
   }
   
   public String getRtgsCode() {
       return rtgsCode;
   }
   
   public void setRtgsCode(String rtgsCode) {
       this.rtgsCode = rtgsCode;
   }
   
   public String getCurrency() {
       return currency;
   }
   
   public void setCurrency(String currency) {
       this.currency = currency;
   }
   
   public String getAppDate() {
       return appDate;
   }
   
   public void setAppDate(String appDate) {
       this.appDate = appDate;
   }
   
   public String getAppDateRealtime() {
       return appDateRealtime;
   }
   
   public void setAppDateRealtime(String appDateRealtime) {
       this.appDateRealtime = appDateRealtime;
   }
   
   public String getPathBo2spanHome() {
       return pathBo2spanHome;
   }
   
   public void setPathBo2spanHome(String pathBo2spanHome) {
       this.pathBo2spanHome = pathBo2spanHome;
   }
   
   public String getPathBo2spanConfig() {
       return pathBo2spanConfig;
   }
   
   public void setPathBo2spanConfig(String pathBo2spanConfig) {
       this.pathBo2spanConfig = pathBo2spanConfig;
   }

   public void setPathSpanAcknowledgePut(String pathSpanAcknowledgePut){
        this.pathSpanAcknowledgePut = pathSpanAcknowledgePut;
   }

   public String getPathSpanAcknowledgePut(){
        return this.pathSpanAcknowledgePut;
   }

   public void setPathSpanAcknowledgeArchive(String pathSpanAcknowledgeArchive){
        this.pathSpanAcknowledgeArchive = pathSpanAcknowledgeArchive;
   }

   public String getPathSpanAcknowledgeArchive(){
        return this.pathSpanAcknowledgeArchive;
   }
   
   public String getPathCorePostingServiceRequest() {
       return pathCorePostingServiceRequest;
   }
   
   public void setPathCorePostingServiceRequest(String pathCorePostingServiceRequest) {
       this.pathCorePostingServiceRequest = pathCorePostingServiceRequest;
   }
}
