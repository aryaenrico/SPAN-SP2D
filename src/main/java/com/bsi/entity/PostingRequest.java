package com.bsi.entity;

public class PostingRequest {
    private String referenceNumber;
    private String documentNumber;
    //    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
//    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private String documentDate;
    private String beneficiaryAccount;
    private String amount;
    private String agentBankAccountNumber;
    private String applicationAreaMessageIdentifier;

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(String documentDate) {
        this.documentDate = documentDate;
    }

    public String getBeneficiaryAccount() {
        return beneficiaryAccount;
    }

    public void setBeneficiaryAccount(String beneficiaryAccount) {
        this.beneficiaryAccount = beneficiaryAccount;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getAgentBankAccountNumber() {
        return agentBankAccountNumber;
    }

    public void setAgentBankAccountNumber(String agentBankAccountNumber) {
        this.agentBankAccountNumber = agentBankAccountNumber;
    }

    public String getApplicationAreaMessageIdentifier() {
        return applicationAreaMessageIdentifier;
    }

    public void setApplicationAreaMessageIdentifier(String applicationAreaMessageIdentifier) {
        this.applicationAreaMessageIdentifier = applicationAreaMessageIdentifier;
    }

    public PostingRequest() {
    }

    @Override
    public String toString() {
        return "PostingRequest{" +
                "referenceNumber='" + referenceNumber + '\'' +
                ", documentNumber='" + documentNumber + '\'' +
                ", documentDate='" + documentDate + '\'' +
                ", beneficiaryAccount='" + beneficiaryAccount + '\'' +
                ", amount='" + amount + '\'' +
                ", agentBankAccountNumber='" + agentBankAccountNumber + '\'' +
                ", applicationAreaMessageIdentifier='" + applicationAreaMessageIdentifier + '\'' +
                '}';
    }
}
