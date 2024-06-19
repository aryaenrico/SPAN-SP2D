package com.bsi.entity;

import java.time.LocalDate;

public class PostingRequest {
    private String referenceNumber;
    private String documentNumber;
    private LocalDate documentDate;
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

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
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

    public PostingRequest(String agentBankAccountNumber, String referenceNumber, String documentNumber, LocalDate documentDate, String beneficiaryAccount, String amount, String applicationAreaMessageIdentifier) {
        this.agentBankAccountNumber = agentBankAccountNumber;
        this.referenceNumber = referenceNumber;
        this.documentNumber = documentNumber;
        this.documentDate = documentDate;
        this.beneficiaryAccount = beneficiaryAccount;
        this.amount = amount;
        this.applicationAreaMessageIdentifier = applicationAreaMessageIdentifier;
    }
}
