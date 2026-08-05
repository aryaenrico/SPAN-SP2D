package com.bsi.entity.bifast.credittransfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreditTransferResponse {
    private String responseId;
    private String responseDate;
    private String responseCode;
    private String responseMessage;
    private String requestId;
    private String referenceId;
    private String creditorName;
    private String creditorNationalId;
    private String creditorType;
    private String creditorResidentStatus;
    private String creditorTownName;
    private String feeAmount;
    private String endToEndId;
    
    public boolean isSuccess() {
        return "000".equals(responseCode);
    }
    
    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }
    
    public String getResponseDate() { return this.responseDate; }
    public void setResponseDate(String responseDate) { this.responseDate = responseDate; }
    
    public String getResponseCode() { return this.responseCode; }
    // [FIX][2026-07-31] GAP 5: Perbaikan bug setter hardcode "99". Sebelumnya responseCode selalu di-set "99" sehingga seluruh logic CT failure (RC 51/57/05/25/78) tidak pernah bekerja.
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
    
    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public String getCreditorName() { return creditorName; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }
    
    public String getCreditorNationalId() { return creditorNationalId; }
    public void setCreditorNationalId(String creditorNationalId) { this.creditorNationalId = creditorNationalId; }
    
    public String getCreditorType() { return creditorType; }
    public void setCreditorType(String creditorType) { this.creditorType = creditorType; }
    
    public String getCreditorResidentStatus() { return creditorResidentStatus; }
    public void setCreditorResidentStatus(String creditorResidentStatus) { this.creditorResidentStatus = creditorResidentStatus; }
    
    public String getCreditorTownName() { return creditorTownName; }
    public void setCreditorTownName(String creditorTownName) { this.creditorTownName = creditorTownName; }
    
    public String getFeeAmount() { return feeAmount; }
    public void setFeeAmount(String feeAmount) { this.feeAmount = feeAmount; }
    
    public String getEndToEndId() { return endToEndId; }
    public void setEndToEndId(String endToEndId) { this.endToEndId = endToEndId; }
}
