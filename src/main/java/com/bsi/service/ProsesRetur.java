package com.bsi.service;

import com.bsi.config.T24Config;
import com.bsi.entity.span.SpanSp2dStageIn;
import com.bsi.entity.t24.FundsTransferSoapRequest;
import com.bsi.entity.t24.FundsTransferSoapResponse;
import com.bsi.utility.RequestIdGenerator;
import com.bsi.utility.Utillity;

public class ProsesRetur {
    
    private T24Config configT24;
    private T24Client t24Client;

    public ProsesRetur(T24Config t24Config){
        this.configT24 =t24Config;
        this.t24Client = new T24Client(configT24);
    }
    
    
    public FundsTransferSoapResponse returProcess(SpanSp2dStageIn item, String debitAcct, String creditAcct,String type,String coCode) {
       String transactionType ="";
       switch (Utillity.safe(type.trim().toUpperCase())) {
            case "BO2":
                 transactionType = "ACSR";
                break;
            default :
                 transactionType = "ACSC";
       }
                     
        FundsTransferSoapRequest ftRequest = new FundsTransferSoapRequest();
        ftRequest.ofsFunction.messageId = item.getDocumentNumber()+"02";
        ftRequest.webRequestCommon.setCompany(coCode);
        FundsTransferSoapRequest.FundsTransferIdiAcctTrfCmsType ft = ftRequest.fundsTransfer;
        ft.transactionType = transactionType;
        ft.debitAccount = debitAcct;
        ft.debitCurrency = item.getCurrencyTarget() != null ? item.getCurrencyTarget() : "IDR";
        ft.debitAmount = item.getAmount().toPlainString();
 
        ft.creditAccount = creditAcct;
        ft.creditCurrency = ft.debitCurrency;
        ft.creditAmount = null;
        ft.creditValueDate = null;
        ft.prosesdate = null;
 
        ft.gPaymentDetails.paymentDetails.add("RETUR SP2D " + item.getDocumentNumber());
        ft.rekeningBiaya = null;
        ft.kodeBiaya = null;
        ft.gCommissionType = null;
        ft.msgId = item.getDocumentNumber()+"02";
 
        return t24Client.fundsTransfer(ftRequest);
    }
  
}
