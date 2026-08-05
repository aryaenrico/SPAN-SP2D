package com.bsi.entity.bifast.accountinquiry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountInquiryResponse {
    private String responseId;
    private String responseDate;
    private String responseCode;
    private String responseMessage;
    private String requestId;
    private String creditorName;
    private String creditorAccountId;
    private String creditorAccountIdType;
    private String creditorType;
    private String creditorResidentStatus;
    private String creditorTownName;
    private String feeAmount;

    public boolean isSuccess(){
      return this.responseCode.equals("00");  
    }


   public String getResponseId() { return this.responseId; }
   public void setResponseId(String responseId) { this.responseId = responseId; }
   
   public String getResponseDate() { return responseDate; }
   public void setResponseDate(String responseDate) { this.responseDate = responseDate; }
   
   public String getResponseCode() { return this.responseCode; }
   public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
   
    // [FIX][2026-07-31] GAP 4: Perbaikan bug getter return field salah. Sebelumnya return this.responseCode, menyebabkan deteksi U136 (Account Not Found) dan U17x (Bank Maintenance) tidak bekerja.
    public String getResponseMessage() { return this.responseMessage; }
   public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
   
   public String getRequestId() { return requestId; }
   public void setRequestId(String requestId) { this.requestId = requestId; }
   
   public String getCreditorName() { return creditorName; }
   public void setCreditorName(String creditorName) { this.creditorName = creditorName; }
   
   public String getCreditorAccountId() { return creditorAccountId; }
   public void setCreditorAccountId(String creditorAccountId) { this.creditorAccountId = creditorAccountId; }
   
   public String getCreditorAccountIdType() { return creditorAccountIdType; }
   public void setCreditorAccountIdType(String creditorAccountIdType) { this.creditorAccountIdType = creditorAccountIdType; }
   
   public String getCreditorType() { return creditorType; }
   public void setCreditorType(String creditorType) { this.creditorType = creditorType; }
   
   public String getCreditorResidentStatus() { return creditorResidentStatus; }
   public void setCreditorResidentStatus(String creditorResidentStatus) { this.creditorResidentStatus = creditorResidentStatus; }
   
   public String getCreditorTownName() { return creditorTownName; }
   public void setCreditorTownName(String creditorTownName) { this.creditorTownName = creditorTownName; }
   
   public String getFeeAmount() { return feeAmount; }
   public void setFeeAmount(String feeAmount) { this.feeAmount = feeAmount; }
   
}
