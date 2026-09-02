package com.bsi.entity.bifast.credittransfer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditTransferRequest {
    
    private String requestId;
    private String twsMsgId;
    private String requestDate;
    private String channelType;
    private String categoryPurposeCode;
    private String interbankSettlementAmount;
    private String chargeBearerCode;
    private String debitorNationalId;
    private String debitorAccountId;
    private String debitorAccountType;
    private String creditorNationalId;
    private String creditorAccountId;
    private String creditorAccountType;
    private String bankCode;
    private String proxyValue;
    private String paymentInformation;
    private String debitorType;
    private String debitorResidentStatus;
    private String debitorTownName;
    private String creditorType;
    private String creditorName;
    private String creditorResidentStatus;
    private String creditorTownName;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getTwsMsgId() { return this.twsMsgId; }
    public void setTwsMsgId(String twsMsgId) { this.twsMsgId = twsMsgId; }

    public String getRequestDate() { return requestDate; }
    public void setRequestDate(String requestDate) { this.requestDate = requestDate; }

    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }

    public String getCategoryPurposeCode() { return categoryPurposeCode; }
    public void setCategoryPurposeCode(String categoryPurposeCode) { this.categoryPurposeCode = categoryPurposeCode; }

    public String getInterbankSettlementAmount() { return interbankSettlementAmount; }
    public void setInterbankSettlementAmount(String interbankSettlementAmount) { this.interbankSettlementAmount = interbankSettlementAmount; }

    public String getChargeBearerCode() { return chargeBearerCode; }
    public void setChargeBearerCode(String chargeBearerCode) { this.chargeBearerCode = chargeBearerCode; }

    public String getDebitorNationalId() { return debitorNationalId; }
    public void setDebitorNationalId(String debitorNationalId) { this.debitorNationalId = debitorNationalId; }

    public String getDebitorAccountId() { return debitorAccountId; }
    public void setDebitorAccountId(String debitorAccountId) { this.debitorAccountId = debitorAccountId; }

    public String getDebitorAccountType() { return debitorAccountType; }
    public void setDebitorAccountType(String debitorAccountType) { this.debitorAccountType = debitorAccountType; }

    public String getCreditorNationalId() { return creditorNationalId; }
    public void setCreditorNationalId(String creditorNationalId) { this.creditorNationalId = creditorNationalId; }

    public String getCreditorAccountId() { return creditorAccountId; }
    public void setCreditorAccountId(String creditorAccountId) { this.creditorAccountId = creditorAccountId; }

    public String getCreditorAccountType() { return creditorAccountType; }
    public void setCreditorAccountType(String creditorAccountType) { this.creditorAccountType = creditorAccountType; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getProxyValue() { return proxyValue; }
    public void setProxyValue(String proxyValue) { this.proxyValue = proxyValue; }

    public String getPaymentInformation() { return paymentInformation; }
    public void setPaymentInformation(String paymentInformation) { this.paymentInformation = paymentInformation; }

    public String getDebitorType() { return debitorType; }
    public void setDebitorType(String debitorType) { this.debitorType = debitorType; }

    public String getDebitorResidentStatus() { return debitorResidentStatus; }
    public void setDebitorResidentStatus(String debitorResidentStatus) { this.debitorResidentStatus = debitorResidentStatus; }

    public String getDebitorTownName() { return debitorTownName; }
    public void setDebitorTownName(String debitorTownName) { this.debitorTownName = debitorTownName; }

    public String getCreditorType() { return creditorType; }
    public void setCreditorType(String creditorType) { this.creditorType = creditorType; }

    public String getCreditorName() { return creditorName; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }

    public String getCreditorResidentStatus() { return creditorResidentStatus; }
    public void setCreditorResidentStatus(String creditorResidentStatus) { this.creditorResidentStatus = creditorResidentStatus; }

    public String getCreditorTownName() { return creditorTownName; }
    public void setCreditorTownName(String creditorTownName) { this.creditorTownName = creditorTownName; }
}