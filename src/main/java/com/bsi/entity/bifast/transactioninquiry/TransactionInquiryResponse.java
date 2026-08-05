package com.bsi.entity.bifast.transactioninquiry;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionInquiryResponse {

    private String responseId;
    private String responseDate;
    private String responseCode;
    private String responseMessage;
    private String orgnlEndToEndId;
    private String referenceId;

    private ResponseMessageHeader responseMessageHeader;

    public Boolean isSuccess() {
        if ("00".equals(responseCode) || "000".equals(responseCode) || "0".equals(responseCode)) {
            return true;
        }
        if (responseMessageHeader != null && "00".equals(responseMessageHeader.getRsResponseCode())) {
            return true;
        }
        return false;
    }

    public Boolean isTimeout() {
        if ("68".equals(responseCode)) {
            return true;
        }
        if (responseMessageHeader != null && "51".equals(responseMessageHeader.getRsResponseCode())) {
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

    public ResponseMessageHeader getResponseMessageHeader() { return responseMessageHeader; }
    public void setResponseMessageHeader(ResponseMessageHeader responseMessageHeader) { this.responseMessageHeader = responseMessageHeader; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseMessageHeader{
      private String rsMsgDate;
      private String rsMsgId;
      private String rsResponseCode;
      private String rsResponseMsg;
      private String rsHasMoreRecord;

      public String getRsMsgDate() { return rsMsgDate; }
      public void setRsMsgDate(String rsMsgDate) { this.rsMsgDate = rsMsgDate; }

      public String getRsMsgId() { return rsMsgId; }
      public void setRsMsgId(String rsMsgId) { this.rsMsgId = rsMsgId; }

      public String getRsResponseCode() { return rsResponseCode; }
      public void setRsResponseCode(String rsResponseCode) { this.rsResponseCode = rsResponseCode; }

      public String getRsResponseMsg() { return rsResponseMsg; }
      public void setRsResponseMsg(String rsResponseMsg) { this.rsResponseMsg = rsResponseMsg; }

      public String getRsHasMoreRecord() { return rsHasMoreRecord; }
      public void setRsHasMoreRecord(String rsHasMoreRecord) { this.rsHasMoreRecord = rsHasMoreRecord; }
    }
}