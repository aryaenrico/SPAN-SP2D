package com.bsi.config;

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
    String pathBo2spanHome = "/home/span";
    String pathBo2spanConfig = "/apps/bo2span/data/config";
    String pathCorePostingServiceRequest = "/apps/bo2span/data/core/posting_service/request";
    String pathSpanAcknowledgePut = "/apps/bo2span/data/span/acknowledge/put";
    String pathSpanAcknowledgeArchive= "/apps/bo2span/data/span/acknowledge/archive";

    public String getpathSpanAcknowledgeArchive(){
        return this.pathSpanAcknowledgeArchive;
    }

    // Getter & Setter
   public String getPathSpanAcknowledgePut(){
    return this.pathSpanAcknowledgePut;
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
       return acctRpkbunNonGaji;
   }
   
   public void setAcctRpkbunNonGaji(String acctRpkbunNonGaji) {
       this.acctRpkbunNonGaji = acctRpkbunNonGaji;
   }
   
   public String getAcctRrRpkbunNonGaji() {
       return acctRrRpkbunNonGaji;
   }
   
   public void setAcctRrRpkbunNonGaji(String acctRrRpkbunNonGaji) {
       this.acctRpkbunNonGaji = acctRrRpkbunNonGaji;
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
   
   public String getPathCorePostingServiceRequest() {
       return pathCorePostingServiceRequest;
   }
   
   public void setPathCorePostingServiceRequest(String pathCorePostingServiceRequest) {
       this.pathCorePostingServiceRequest = pathCorePostingServiceRequest;
   }
}
