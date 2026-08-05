package com.bsi.entity.t24;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "FundsTransfer", namespace = FundsTransferSoapRequest.NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class FundsTransferSoapRequest {

    
    public static final String NS = "T24WebServicesImpl";

    @XmlElement(name = "WebRequestCommon")
    public WebRequestCommon webRequestCommon = new WebRequestCommon();

    @XmlElement(name = "OfsFunction")
    public OfsFunction ofsFunction = new OfsFunction();

    @XmlElement(name = "FUNDSTRANSFERIDIACCTTRFCMSType")
    public FundsTransferIdiAcctTrfCmsType fundsTransfer = new FundsTransferIdiAcctTrfCmsType();

    // ------------------------------------------------------------------

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class WebRequestCommon {
        @XmlElement(name = "userName") public String userName = "";
        @XmlElement(name = "password") public String password = "";
        @XmlElement(name = "company")  public String company = "";
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class OfsFunction {
        @XmlElement(name = "gtsControl") public String gtsControl = "";
        @XmlElement(name = "messageId")  public String messageId = "";
        @XmlElement(name = "noOfAuth")   public String noOfAuth = "";
        @XmlElement(name = "replace")    public String replace = "";
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FundsTransferIdiAcctTrfCmsType {
        @XmlElement(name = "TransactionType") public String transactionType = "";
        @XmlElement(name = "DebitAccount")    public String debitAccount = "";
        @XmlElement(name = "DebitCurrency")   public String debitCurrency = "";
        @XmlElement(name = "DebitAmount")     public String debitAmount = "";
        @XmlElement(name = "DebitValueDate")  public String debitValueDate = "";
        @XmlElement(name = "CreditAccount")   public String creditAccount = "";
        @XmlElement(name = "CreditCurrency")  public String creditCurrency = "";
        @XmlElement(name = "CreditAmount")    public String creditAmount = "";
        @XmlElement(name = "CreditValueDate") public String creditValueDate = "";
        @XmlElement(name = "Prosesdate")      public String prosesdate = "";

        @XmlElement(name = "gPAYMENTDETAILS")
        public GPaymentDetails gPaymentDetails = new GPaymentDetails();

        @XmlElement(name = "RekeningBiaya") public String rekeningBiaya = "";
        @XmlElement(name = "KodeBiaya")     public String kodeBiaya = "";

        @XmlElement(name = "gCOMMISSIONTYPE")
        public GCommissionType gCommissionType = new GCommissionType();

        @XmlElement(name = "ProfitCentreDept") public String profitCentreDept = "";
        @XmlElement(name = "AmountDebited")    public String amountDebited = "";
        @XmlElement(name = "AmountCredited")   public String amountCredited = "";
        @XmlElement(name = "MSGID")            public String msgId = "";
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GPaymentDetails {
        @XmlElement(name = "PaymentDetails")
        public List<String> paymentDetails = new ArrayList<String>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GCommissionType {
        @XmlElement(name = "mCOMMISSIONTYPE")
        public List<MCommissionType> mCommissionType = new ArrayList<MCommissionType>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MCommissionType {
        @XmlElement(name = "JenisBiaya")   public String jenisBiaya = "";
        @XmlElement(name = "NominalBiaya") public String nominalBiaya = "";

        public MCommissionType() { }

        public MCommissionType(String jenisBiaya, String nominalBiaya) {
            this.jenisBiaya = jenisBiaya;
            this.nominalBiaya = nominalBiaya;
        }
    }
}
 