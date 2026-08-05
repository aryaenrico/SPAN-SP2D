package com.bsi.entity.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SpanSp2dStageIn {
    private int id;
    private String applicationAreaSenderIdentifier;
    private String applicationAreaReceiverIdentifier;
    private String applicationAreaDetailSenderIdentifier;
    private String applicationAreaDetailReceiverIdentifier;
    private LocalDateTime applicationAreaCreationDateTime;
    private String applicationAreaMessageIdentifier;
    private String applicationAreaMessageTypeIndicator;
    private String applicationAreaMessageVersionText;
    private LocalDate documentDate;
    private String documentNumber;
    private String beneficiaryName;
    private String beneficiaryBankCode;
    private String beneficiaryBank;
    private String beneficiaryAccount;
    private BigDecimal amount;
    private String currencyTarget;
    private String description;
    private String agentBankCode;
    private String agentBankAccountNumber;
    private String agentBankAccountName;
    private String emailAddress;
    private String swiftCode;
    private String ibanCode;
    private String paymentMethod;
    private Integer sp2dCount;
    private Integer totalCount;
    private BigDecimal totalAmount;
    private Integer totalBatchCount;
    private String sp2dNumber;
    private LocalDate datePosting;
    private String referenceNumber;
    private String returnCode;
    private String  status;
    private String flagAck;
    private String flagVoidChecked;
    private String jenisTransaksi;
    private String namaSupplier;
    private String kodeSatker;
    private String address;
    private String biFastResponseCode;
    private String biFastResponseMessage;

    // constructor
    public SpanSp2dStageIn(){

    }

    public SpanSp2dStageIn(int id,String status,String ack,BigDecimal amount,String beneficiaryAccount,String agentbankaccountnumber,String beneficiaryName) {
    this.id = id;
    this.status = status;
    this.flagAck = ack;
    this.amount = amount;
    this.beneficiaryAccount = beneficiaryAccount;
    this.agentBankAccountNumber = agentbankaccountnumber;
    this.beneficiaryName = beneficiaryName;
 }


    public SpanSp2dStageIn(int id,
    String applicationAreaSenderIdentifier,
    String applicationAreaReceiverIdentifier,
    String applicationAreaDetailSenderIdentifier,
    String applicationAreaDetailReceiverIdentifier,
    LocalDateTime applicationAreaCreationDateTime,
    String applicationAreaMessageIdentifier,
    String applicationAreaMessageTypeIndicator,
    String applicationAreaMessageVersionText,
    LocalDate documentDate,
    String documentNumber,
    String beneficiaryName,
    String beneficiaryBankCode,
    String beneficiaryBank,
    String beneficiaryAccount,
    BigDecimal amount,
    String currencyTarget,
    String description,
    String agentBankCode,
    String agentBankAccountNumber,
    String agentBankAccountName,
    String emailAddress,
    String swiftCode,
    String ibanCode,
    String paymentMethod,
    Integer sp2dCount,
    Integer totalCount,
    BigDecimal totalAmount,
    Integer totalBatchCount,
    String sp2dNumber,
    LocalDate datePosting,
    String referenceNumber,
    String returnCode,
    String status,
    String flagAck,
    String flagVoidChecked,
    String jenisTransaksi,
    String namaSupplier,
    String kodeSatker,
    String address,
    String biFastResponseCode,
    String biFastResponseMessage){
     this.id = id;
    this.applicationAreaSenderIdentifier = applicationAreaSenderIdentifier;
    this.applicationAreaReceiverIdentifier = applicationAreaReceiverIdentifier;
    this.applicationAreaDetailSenderIdentifier = applicationAreaDetailSenderIdentifier;
    this.applicationAreaDetailReceiverIdentifier = applicationAreaDetailReceiverIdentifier;
    this.applicationAreaCreationDateTime = applicationAreaCreationDateTime;
    this.applicationAreaMessageIdentifier = applicationAreaMessageIdentifier;
    this.applicationAreaMessageTypeIndicator = applicationAreaMessageTypeIndicator;
    this.applicationAreaMessageVersionText = applicationAreaMessageVersionText;
    this.documentDate = documentDate;
    this.documentNumber = documentNumber;
    this.beneficiaryName = beneficiaryName;
    this.beneficiaryBankCode = beneficiaryBankCode;
    this.beneficiaryBank = beneficiaryBank;
    this.beneficiaryAccount = beneficiaryAccount;
    this.amount = amount;
    this.currencyTarget = currencyTarget;
    this.description = description;
    this.agentBankCode = agentBankCode;
    this.agentBankAccountNumber = agentBankAccountNumber;
    this.agentBankAccountName = agentBankAccountName;
    this.emailAddress = emailAddress;
    this.swiftCode = swiftCode;
    this.ibanCode = ibanCode;
    this.paymentMethod = paymentMethod;
    this.sp2dCount = sp2dCount;
    this.totalCount = totalCount;
    this.totalAmount = totalAmount;
    this.totalBatchCount = totalBatchCount;
    this.sp2dNumber = sp2dNumber;
    this.datePosting = datePosting;
    this.referenceNumber = referenceNumber;
    this.returnCode = returnCode;
    this.status = status;
    this.flagAck = flagAck;
    this.flagVoidChecked = flagVoidChecked;
    this.jenisTransaksi = jenisTransaksi;
    this.namaSupplier = namaSupplier;
    this.kodeSatker = kodeSatker;
    this.address = address;
    this.biFastResponseCode = biFastResponseCode;
    this.biFastResponseMessage = biFastResponseMessage;
    }

    public SpanSp2dStageIn(
            int id, 
            String applicationAreaSenderIdentifier, 
            String applicationAreaReceiverIdentifier, 
            String applicationAreaDetailSenderIdentifier, 
            String applicationAreaDetailReceiverIdentifier, 
            LocalDateTime applicationAreaCreationDateTime, 
            String applicationAreaMessageIdentifier, 
            String applicationAreaMessageTypeIndicator, 
            String applicationAreaMessageVersionText, 
            LocalDate documentDate, 
            String documentNumber, 
            String beneficiaryName, 
            String beneficiaryBankCode, 
            String beneficiaryBank, 
            String beneficiaryAccount, 
            BigDecimal amount, 
            String currencyTarget, 
            String description, 
            String agentBankCode, 
            String agentBankAccountNumber, 
            String agentBankAccountName, 
            String emailAddress, 
            String swiftCode, 
            String ibanCode, 
            String paymentMethod, 
            Integer sp2dCount, 
            Integer totalCount, 
            BigDecimal totalAmount, 
            Integer totalBatchCount, 
            String sp2dNumber, 
            LocalDate datePosting, 
            String referenceNumber, 
            String returnCode, 
            String status) {
        this.id = id;
        this.applicationAreaSenderIdentifier = applicationAreaSenderIdentifier;
        this.applicationAreaReceiverIdentifier = applicationAreaReceiverIdentifier;
        this.applicationAreaDetailSenderIdentifier = applicationAreaDetailSenderIdentifier;
        this.applicationAreaDetailReceiverIdentifier = applicationAreaDetailReceiverIdentifier;
        this.applicationAreaCreationDateTime = applicationAreaCreationDateTime;
        this.applicationAreaMessageIdentifier = applicationAreaMessageIdentifier;
        this.applicationAreaMessageTypeIndicator = applicationAreaMessageTypeIndicator;
        this.applicationAreaMessageVersionText = applicationAreaMessageVersionText;
        this.documentDate = documentDate;
        this.documentNumber = documentNumber;
        this.beneficiaryName = beneficiaryName;
        this.beneficiaryBankCode = beneficiaryBankCode;
        this.beneficiaryBank = beneficiaryBank;
        this.beneficiaryAccount = beneficiaryAccount;
        this.amount = amount;
        this.currencyTarget = currencyTarget;
        this.description = description;
        this.agentBankCode = agentBankCode;
        this.agentBankAccountNumber = agentBankAccountNumber;
        this.agentBankAccountName = agentBankAccountName;
        this.emailAddress = emailAddress;
        this.swiftCode = swiftCode;
        this.ibanCode = ibanCode;
        this.paymentMethod = paymentMethod;
        this.sp2dCount = sp2dCount;
        this.totalCount = totalCount;
        this.totalAmount = totalAmount;
        this.totalBatchCount = totalBatchCount;
        this.sp2dNumber = sp2dNumber;
        this.datePosting = datePosting;
        this.referenceNumber = referenceNumber;
        this.returnCode = returnCode;
        this.status = status;
    }

    // getter & setter
    public int getId (){
        return this.id;
    }
    public String getApplicationAreaSenderIdentifier() { return applicationAreaSenderIdentifier; }
    public void setApplicationAreaSenderIdentifier(String applicationAreaSenderIdentifier) { this.applicationAreaSenderIdentifier = applicationAreaSenderIdentifier; }

    public String getApplicationAreaReceiverIdentifier() { return applicationAreaReceiverIdentifier; }
    public void setApplicationAreaReceiverIdentifier(String applicationAreaReceiverIdentifier) { this.applicationAreaReceiverIdentifier = applicationAreaReceiverIdentifier; }

    public String getApplicationAreaDetailSenderIdentifier() { return applicationAreaDetailSenderIdentifier; }
    public void setApplicationAreaDetailSenderIdentifier(String applicationAreaDetailSenderIdentifier) { this.applicationAreaDetailSenderIdentifier = applicationAreaDetailSenderIdentifier; }

    public String getApplicationAreaDetailReceiverIdentifier() { return applicationAreaDetailReceiverIdentifier; }
    public void setApplicationAreaDetailReceiverIdentifier(String applicationAreaDetailReceiverIdentifier) { this.applicationAreaDetailReceiverIdentifier = applicationAreaDetailReceiverIdentifier; }

    public LocalDateTime getApplicationAreaCreationDateTime() { return applicationAreaCreationDateTime; }
    public void setApplicationAreaCreationDateTime(LocalDateTime applicationAreaCreationDateTime) { this.applicationAreaCreationDateTime = applicationAreaCreationDateTime; }

    public String getApplicationAreaMessageIdentifier() { return applicationAreaMessageIdentifier; }
    public void setApplicationAreaMessageIdentifier(String applicationAreaMessageIdentifier) { this.applicationAreaMessageIdentifier = applicationAreaMessageIdentifier; }

    public String getApplicationAreaMessageTypeIndicator() { return applicationAreaMessageTypeIndicator; }
    public void setApplicationAreaMessageTypeIndicator(String applicationAreaMessageTypeIndicator) { this.applicationAreaMessageTypeIndicator = applicationAreaMessageTypeIndicator; }

    public String getApplicationAreaMessageVersionText() { return applicationAreaMessageVersionText; }
    public void setApplicationAreaMessageVersionText(String applicationAreaMessageVersionText) { this.applicationAreaMessageVersionText = applicationAreaMessageVersionText; }

    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate documentDate) { this.documentDate = documentDate; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getBeneficiaryBankCode() { return beneficiaryBankCode; }
    public void setBeneficiaryBankCode(String beneficiaryBankCode) { this.beneficiaryBankCode = beneficiaryBankCode; }

    public String getBeneficiaryBank() { return beneficiaryBank; }
    public void setBeneficiaryBank(String beneficiaryBank) { this.beneficiaryBank = beneficiaryBank; }

    public String getBeneficiaryAccount() { return beneficiaryAccount; }
    public void setBeneficiaryAccount(String beneficiaryAccount) { this.beneficiaryAccount = beneficiaryAccount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrencyTarget() { return currencyTarget; }
    public void setCurrencyTarget(String currencyTarget) { this.currencyTarget = currencyTarget; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAgentBankCode() { return agentBankCode; }
    public void setAgentBankCode(String agentBankCode) { this.agentBankCode = agentBankCode; }

    public String getAgentBankAccountNumber() { return agentBankAccountNumber; }
    public void setAgentBankAccountNumber(String agentBankAccountNumber) { this.agentBankAccountNumber = agentBankAccountNumber; }

    public String getAgentBankAccountName() { return agentBankAccountName; }
    public void setAgentBankAccountName(String agentBankAccountName) { this.agentBankAccountName = agentBankAccountName; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getSwiftCode() { return swiftCode; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }

    public String getIbanCode() { return ibanCode; }
    public void setIbanCode(String ibanCode) { this.ibanCode = ibanCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getSp2dCount() { return sp2dCount; }
    public void setSp2dCount(Integer sp2dCount) { this.sp2dCount = sp2dCount; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getTotalBatchCount() { return totalBatchCount; }
    public void setTotalBatchCount(Integer totalBatchCount) { this.totalBatchCount = totalBatchCount; }

    public String getSp2dNumber() { return sp2dNumber; }
    public void setSp2dNumber(String sp2dNumber) { this.sp2dNumber = sp2dNumber; }

    public LocalDate getDatePosting() { return datePosting; }
    public void setDatePosting(LocalDate datePosting) { this.datePosting = datePosting; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getReturnCode() { return returnCode; }
    public void setReturnCode(String returnCode) { this.returnCode = returnCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String isFlagAck() { return flagAck; }
    public void setFlagAck(String flagAck) { this.flagAck = flagAck; }

    public String isFlagVoidChecked() { return flagVoidChecked; }
    public void setFlagVoidChecked(String flagVoidChecked) { this.flagVoidChecked = flagVoidChecked; }

    public String getJenisTransaksi() { return jenisTransaksi; }
    public void setJenisTransaksi(String jenisTransaksi) { this.jenisTransaksi = jenisTransaksi; }

    public String getNamaSupplier() { return namaSupplier; }
    public void setNamaSupplier(String namaSupplier) { this.namaSupplier = namaSupplier; }

    public String getKodeSatker() { return kodeSatker; }
    public void setKodeSatker(String kodeSatker) { this.kodeSatker = kodeSatker; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBiFastResponseCode() { return biFastResponseCode; }
    public void setBiFastResponseCode(String biFastResponseCode) { this.biFastResponseCode = biFastResponseCode; }

    public String getBiFastResponseMessage() { return biFastResponseMessage; }
    public void setBiFastResponseMessage(String biFastResponseMessage) { this.biFastResponseMessage = biFastResponseMessage; }
}
 