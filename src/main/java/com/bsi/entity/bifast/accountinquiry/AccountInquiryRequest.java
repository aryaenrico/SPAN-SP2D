package com.bsi.entity.bifast.accountinquiry;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountInquiryRequest {
    private String requestId;
    private String requestDate;
    private String channelType;
    private String interbankSettlementAmount;
    private String chargeBearerCode;
    private String debitorAccountId;
    private String bankCode;
    private String categoryPurposeCode;
    private String creditorAccountId;

   public String getRequestId() { return requestId; }
   public void setRequestId(String requestId) { this.requestId = requestId; }
   
   public String getRequestDate() { return requestDate; }
   public void setRequestDate(String requestDate) { this.requestDate = requestDate; }
   
   public String getChannelType() { return channelType; }
   public void setChannelType(String channelType) { this.channelType = channelType; }
   
   public String getInterbankSettlementAmount() { return interbankSettlementAmount; }
   public void setInterbankSettlementAmount(String interbankSettlementAmount) { this.interbankSettlementAmount = interbankSettlementAmount; }
   
   public String getChargeBearerCode() { return chargeBearerCode; }
   public void setChargeBearerCode(String chargeBearerCode) { this.chargeBearerCode = chargeBearerCode; }

   public String getdebitorAccountId() { return this.debitorAccountId; }
   public void setdebitorAccountId(String DEBT) { this.debitorAccountId = debitorAccountId; }
   
   public String getBankCode() { return bankCode; }
   public void setBankCode(String bankCode) { this.bankCode = bankCode; }
   
   public String getCategoryPurposeCode() { return categoryPurposeCode; }
   public void setCategoryPurposeCode(String categoryPurposeCode) { this.categoryPurposeCode = categoryPurposeCode; }
   
   public String getCreditorAccountId() { return creditorAccountId; }
   public void setCreditorAccountId(String creditorAccountId) { this.creditorAccountId = creditorAccountId; }



}
