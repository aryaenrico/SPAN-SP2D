package com.bsi.entity.t24;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "FundsTransferResponse", namespace = FundsTransferSoapRequest.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class FundsTransferSoapResponse {

    @XmlElement(name = "Status")
    public Status status;

    @XmlElement(name = "FUNDSTRANSFERType")
    public FundsTransferType fundsTransferType;

    /** Sukses jika successIndicator == "Success" */
    public boolean isSuccess() {
        return status != null && "Success".equalsIgnoreCase(status.successIndicator);
    }

    public String getTransactionId() {
        return status != null ? status.transactionId : null;
    }

    // ------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Status {
        @XmlElement(name = "transactionId")    public String transactionId;
        @XmlElement(name = "messageId")        public String messageId;
        @XmlElement(name = "successIndicator") public String successIndicator;
        @XmlElement(name = "application")      public String application;

        /** Diisi server bila gagal (umumnya berisi pesan error OFS) */
        @XmlElement(name = "messages")
        public List<String> messages = new ArrayList<String>();

        @Override
        public String toString() {
            return "Status{transactionId='" + transactionId + "', messageId='" + messageId
                    + "', successIndicator='" + successIndicator + "', application='" + application
                    + (messages.isEmpty() ? "" : "', messages=" + messages) + "'}";
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FundsTransferType {
        @XmlAttribute(name = "id")
        public String id;

        @XmlElement(name = "TRANSACTIONTYPE")  public String transactionType;
        @XmlElement(name = "DEBITACCTNO")      public String debitAcctNo;
        @XmlElement(name = "CURRENCYMKTDR")    public String currencyMktDr;
        @XmlElement(name = "DEBITCURRENCY")    public String debitCurrency;
        @XmlElement(name = "DEBITAMOUNT")      public String debitAmount;
        @XmlElement(name = "DEBITVALUEDATE")   public String debitValueDate;
        @XmlElement(name = "CREDITACCTNO")     public String creditAcctNo;
        @XmlElement(name = "CURRENCYMKTCR")    public String currencyMktCr;
        @XmlElement(name = "CREDITCURRENCY")   public String creditCurrency;
        @XmlElement(name = "CREDITAMOUNT")     public String creditAmount;
        @XmlElement(name = "CREDITVALUEDATE")  public String creditValueDate;
        @XmlElement(name = "PROCESSINGDATE")   public String processingDate;

        @XmlElement(name = "gORDERINGBANK")
        public GOrderingBank gOrderingBank;

        @XmlElement(name = "gPAYMENTDETAILS")
        public GPaymentDetails gPaymentDetails;

        @XmlElement(name = "CHARGESACCTNO")    public String chargesAcctNo;
        @XmlElement(name = "CHARGECOMDISPLAY") public String chargeComDisplay;
        @XmlElement(name = "COMMISSIONCODE")   public String commissionCode;

        @XmlElement(name = "gCOMMISSIONTYPE")
        public GCommissionType gCommissionType;

        @XmlElement(name = "CHARGECODE")       public String chargeCode;
        @XmlElement(name = "PROFITCENTREDEPT") public String profitCentreDept;
        @XmlElement(name = "RETURNTODEPT")     public String returnToDept;
        @XmlElement(name = "FEDFUNDS")         public String fedFunds;
        @XmlElement(name = "POSITIONTYPE")     public String positionType;
        @XmlElement(name = "AMOUNTDEBITED")    public String amountDebited;
        @XmlElement(name = "AMOUNTCREDITED")   public String amountCredited;
        @XmlElement(name = "CREDITCOMPCODE")   public String creditCompCode;
        @XmlElement(name = "DEBITCOMPCODE")    public String debitCompCode;
        @XmlElement(name = "LOCAMTDEBITED")    public String locAmtDebited;
        @XmlElement(name = "LOCAMTCREDITED")   public String locAmtCredited;
        @XmlElement(name = "CUSTGROUPLEVEL")   public String custGroupLevel;
        @XmlElement(name = "DEBITCUSTOMER")    public String debitCustomer;
        @XmlElement(name = "CREDITCUSTOMER")   public String creditCustomer;
        @XmlElement(name = "DRADVICEREQDYN")   public String drAdviceReqDyn;
        @XmlElement(name = "CRADVICEREQDYN")   public String crAdviceReqDyn;
        @XmlElement(name = "CHARGEDCUSTOMER")  public String chargedCustomer;
        @XmlElement(name = "TOTRECCOMM")       public String totRecComm;
        @XmlElement(name = "TOTRECCOMMLCL")    public String totRecCommLcl;
        @XmlElement(name = "TOTRECCHG")        public String totRecChg;
        @XmlElement(name = "TOTRECCHGLCL")     public String totRecChgLcl;
        @XmlElement(name = "RATEFIXING")       public String rateFixing;
        @XmlElement(name = "TOTRECCHGCRCCY")   public String totRecChgCrCcy;
        @XmlElement(name = "TOTSNDCHGCRCCY")   public String totSndChgCrCcy;
        @XmlElement(name = "AUTHDATE")         public String authDate;
        @XmlElement(name = "ROUNDTYPE")        public String roundType;

        @XmlElement(name = "gSTMTNOS")
        public GStmtNos gStmtNos;

        @XmlElement(name = "CURRNO")           public String currNo;

        @XmlElement(name = "gINPUTTER")
        public GInputter gInputter;

        @XmlElement(name = "gDATETIME")
        public GDateTime gDateTime;

        @XmlElement(name = "AUTHORISER")       public String authoriser;
        @XmlElement(name = "COCODE")           public String coCode;
        @XmlElement(name = "DEPTCODE")         public String deptCode;
        @XmlElement(name = "MSGID")            public String msgId;
        @XmlElement(name = "LASTVERSION")      public String lastVersion;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GOrderingBank {
        @XmlElement(name = "ORDERINGBANK")
        public List<String> orderingBank = new ArrayList<String>();
    }
    @XmlType(name = "gPaymentDetailsResp")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GPaymentDetails {
        @XmlElement(name = "PAYMENTDETAILS")
        public List<String> paymentDetails = new ArrayList<String>();
    }

    @XmlType(name = "gCommissionTypeResp")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GCommissionType {
        @XmlElement(name = "mCOMMISSIONTYPE")
        public List<MCommissionType> mCommissionType = new ArrayList<MCommissionType>();
    }

    @XmlType(name = "mCommissionTypeResp")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MCommissionType {
        @XmlElement(name = "COMMISSIONTYPE") public String commissionType;
        @XmlElement(name = "COMMISSIONAMT")  public String commissionAmt;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GStmtNos {
        @XmlElement(name = "STMTNOS")
        public List<String> stmtNos = new ArrayList<String>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GInputter {
        @XmlElement(name = "INPUTTER")
        public List<String> inputter = new ArrayList<String>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GDateTime {
        @XmlElement(name = "DATETIME")
        public List<String> dateTime = new ArrayList<String>();
    }

    @Override
    public String toString() {
        return "FundsTransferSoapResponse{" + status
                + (fundsTransferType != null
                    ? ", ftId='" + fundsTransferType.id
                      + "', amountDebited='" + fundsTransferType.amountDebited
                      + "', amountCredited='" + fundsTransferType.amountCredited + "'"
                    : "")
                + "}";
    }
}
 
