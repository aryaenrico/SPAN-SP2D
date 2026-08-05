package com.bsi.service;

import com.bsi.config.T24Config;
import com.bsi.entity.api.ApiClientException;
import com.bsi.entity.t24.FundsTransferSoapRequest;
import com.bsi.entity.t24.FundsTransferSoapResponse;

public class T24Demo {

    public static void main(String[] args) {
 
        T24Config config = T24Config.fromProperties();

        T24Client client = new T24Client(config);

        FundsTransferSoapRequest req = new FundsTransferSoapRequest();
  
        req.ofsFunction.messageId = "IMS1122421116";

        // Detail transaksi
        FundsTransferSoapRequest.FundsTransferIdiAcctTrfCmsType ft = req.fundsTransfer;
        ft.transactionType = "AC79";
        ft.debitAccount    = "7927927928";
        ft.debitCurrency   = "IDR";
        ft.debitAmount     = "2100.00";
        ft.debitValueDate  = "20230512";
        ft.creditAccount   = "7068264256";
        ft.creditCurrency  = "IDR";
        ft.creditAmount    = "";
        ft.creditValueDate = "";
        ft.prosesdate      = "";
        ft.gPaymentDetails.paymentDetails.add("Test BUKU BALIK BI FAST");
        ft.rekeningBiaya   = "";
        ft.kodeBiaya       = "";
        ft.gCommissionType.mCommissionType.add(
                new FundsTransferSoapRequest.MCommissionType("", ""));
        ft.profitCentreDept = "200";
        ft.msgId            = "IMS1122421116";

        try {
            FundsTransferSoapResponse resp = client.fundsTransfer(req);
            System.out.println("Hasil: " + resp);

            if (resp.isSuccess()) {
                System.out.println("Transfer sukses, FT ID: " + resp.getTransactionId());
                if (resp.fundsTransferType != null) {
                    System.out.println("  Amount debited : " + resp.fundsTransferType.amountDebited);
                    System.out.println("  Amount credited: " + resp.fundsTransferType.amountCredited);
                    System.out.println("  Auth date      : " + resp.fundsTransferType.authDate);
                }
            } else {
                System.out.println("Transfer GAGAL: "
                        + (resp.status != null ? resp.status.successIndicator : "unknown"));
                if (resp.status != null && !resp.status.messages.isEmpty()) {
                    resp.status.messages.forEach(m -> System.out.println("  - " + m));
                }
            }
        } catch (ApiClientException e) {
            System.err.println("Error: " + e.getMessage());
            if (e.getResponseBody() != null) {
                System.err.println("Body: " + e.getResponseBody());
            }
        }
    }
}
 