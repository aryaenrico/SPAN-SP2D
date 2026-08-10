package com.bsi.entity.bifast.transactioninquiry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentStatusResponse {

    private String responseId;
    private String responseDate;
    private String responseCode;
    private String responseMessage;
    private String orgnlEndToEndId;
    private String referenceId;



    public Boolean isSuccess() {
        if ("00".equals(responseCode) || "000".equals(responseCode) || "0".equals(responseCode)) {
            return true;
        }
      
        return false;
    }

    public Boolean isTimeout() {
        if ("68".equals(responseCode)) {
            return true;
        }
       
        return false;
    }

    public Boolean isNotFoundOrFailed() {
        if ("25".equals(responseCode) || (responseMessage != null && responseMessage.toUpperCase().contains("U106"))) {
            return true;
        }
        return !isSuccess() && !isTimeout();
    }

    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }

    public String getResponseDate() { return responseDate; }
    public void setResponseDate(String responseDate) { this.responseDate = responseDate; }

    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }

    public String getOrgnlEndToEndId() { return orgnlEndToEndId; }
    public void setOrgnlEndToEndId(String orgnlEndToEndId) { this.orgnlEndToEndId = orgnlEndToEndId; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

   
}