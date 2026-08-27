package com.bsi.service;


import com.bsi.config.T24Config;
import com.bsi.entity.mock.SpanSp2dStageIn;
import com.bsi.entity.t24.FundsTransferSoapRequest;
import com.bsi.entity.t24.FundsTransferSoapResponse;
import com.bsi.utility.RequestIdGenerator;

public class ProsesRetur {
    static T24Config configT24 = T24Config.fromProperties();
    static T24Client t24Client = new T24Client(configT24);
    public static FundsTransferSoapResponse returProcess(SpanSp2dStageIn item, String debitAcct, String creditAcct,String type) {
        String transactionType ="";
       switch (type) {
            case "BO2":
                 transactionType = "ACSR";
                break;
            default :
                 transactionType = "ACSC";
                 break;
       }
                     
        FundsTransferSoapRequest ftRequest = new FundsTransferSoapRequest();
        ftRequest.ofsFunction.messageId = RequestIdGenerator.generateRequestID();
        FundsTransferSoapRequest.FundsTransferIdiAcctTrfCmsType ft = ftRequest.fundsTransfer;
        ft.transactionType = transactionType;
        ft.debitAccount = debitAcct;
        ft.debitCurrency = item.getCurrencyTarget() != null ? item.getCurrencyTarget() : "IDR";
        ft.debitAmount = item.getAmount().toPlainString();
        //ft.debitValueDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
 
        ft.creditAccount = creditAcct;
        ft.creditCurrency = ft.debitCurrency;
        ft.creditAmount = null;
        ft.creditValueDate = null;
        ft.prosesdate = null;
 
        ft.gPaymentDetails.paymentDetails.add("RETUR SP2D " + item.getDocumentNumber());
        ft.rekeningBiaya = null;
        ft.kodeBiaya = null;
        ft.gCommissionType = null;
        ft.profitCentreDept = "200";
        ft.msgId = RequestIdGenerator.generateRequestID();
 
        return t24Client.fundsTransfer(ftRequest);
    }
  
}
