package com.bsi.entity.mock;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.bsi.MainCHK;
import com.bsi.config.BifastConfig;
import com.bsi.config.T24Config;
import com.bsi.entity.api.ApiClientException;
import com.bsi.entity.bifast.accountinquiry.AccountInquiryRequest;
import com.bsi.entity.bifast.accountinquiry.AccountInquiryResponse;
import com.bsi.entity.bifast.credittransfer.CreditTransferRequest;
import com.bsi.entity.bifast.credittransfer.CreditTransferResponse;
import com.bsi.entity.bifast.transactioninquiry.PaymenStatusRequest;
import com.bsi.entity.bifast.transactioninquiry.PaymentStatusResponse;
import com.bsi.entity.span.BifastRcMapping;
import com.bsi.entity.t24.FundsTransferSoapResponse;
import com.bsi.service.BifastClient;
import com.bsi.service.ProsesRetur;
import com.bsi.service.ServiceMariaDb;
import com.bsi.service.T24Client;
import com.bsi.utility.RequestIdGenerator;
import com.bsi.utility.Utillity;

public class ProcessBifast {

    BifastConfig config = BifastConfig.fromProperties();
    BifastClient client = new BifastClient(config);

    T24Config configT24 = T24Config.fromProperties();
    T24Client t24Client = new T24Client(configT24);

    // mapping rc 25 ae
    private final String rcAccountNotFound = "AE028";
    private final String rcBankIsMaintanace = "AE021";

    // mapping rc 25 ct
    private final String rcFailedCt = "CT022";
    private final String rcFailedCtBankMaintanace = "CT021";

    private final String pathProp = "C:/Users/ven.arya/Downloads/Project/2026/SPAN/Custom Handler/span-custom-handler/tesDs";
    private final String propName = "bo2span";

    // [NEW][2026-08-04] Factory method untuk membuat dedicated instance ServiceMariaDb (1 koneksi DB terisolasi per Worker Thread)
    private ServiceMariaDb createMariaDbInstance() {
        return new ServiceMariaDb(pathProp, propName);
    }

    // [NEW][2026-08-04] Multi-Threading dengan 3 Dedicated Worker Threads & 3 Instance ServiceMariaDb Terpisah.
    // Menggantikan pemblokiran 'synchronized' pada layer DB sehingga setiap worker thread memiliki koneksi MariaDB
    // independen sendiri. Eksekusi HTTP REST/SOAP dan DB I/O kini berjalan 100% paralel secara optimal.
    public String paymentBifast(List<SpanSp2dStageIn> processData, String typeTransaction) {
        if (processData == null || processData.isEmpty()) {
            MainCHK.tulisLog("Tidak ada data BI-FAST untuk diproses.");
            return "00";
        }

        int numThreads = 3;
        int totalData = processData.size();
        MainCHK.tulisLog("[MULTI-THREAD] Memulai pemrosesan " + totalData + " data BI-FAST dengan " + numThreads + " Dedicated Worker Threads (1 DB Conn/Thread)...");

        // Fetch mapping awal menggunakan koneksi sementara
        Map<String, BifastRcMapping> mappingRcAe;
        ServiceMariaDb initDb = createMariaDbInstance();
        try {
            mappingRcAe = initDb.getBifastMappingRcAe();
        } finally {
            try { initDb.close(); } catch (Exception e) {}
        }

        Queue<SpanSp2dStageIn> taskQueue = new ConcurrentLinkedQueue<>(processData);
        AtomicInteger processedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 1; i <= numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                MainCHK.tulisLog("[WORKER-" + threadId + "] Worker thread dimulai (Membuka koneksi MariaDB dedicated)...");
                // [2026-08-04] Setiap worker thread mengelola instance ServiceMariaDb sendiri (Terisolasi)
                ServiceMariaDb threadMariaDb = createMariaDbInstance();
                try {
                    SpanSp2dStageIn item;
                    while ((item = taskQueue.poll()) != null) {
                        int count = processedCount.incrementAndGet();
                        MainCHK.tulisLog("[WORKER-" + threadId + "] Memproses document: " + item.getDocumentNumber() + " (" + count + "/" + totalData + ")");
                        processSingleItem(item, mappingRcAe, threadMariaDb);
                    }
                } finally {
                    try {
                        threadMariaDb.close();
                    } catch (Exception e) {
                        MainCHK.tulisLog("[WORKER-" + threadId + "] Error closing DB connection: " + e.getMessage());
                    }
                }
                MainCHK.tulisLog("[WORKER-" + threadId + "] Worker thread selesai (Koneksi DB ditutup).");
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            MainCHK.tulisLog("[MULTI-THREAD] Interrupted saat menunggu worker threads: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        MainCHK.tulisLog("[MULTI-THREAD] Selesai memproses seluruh " + totalData + " data BI-FAST.");
        return "00";
    }

    // [NEW][2026-08-04] Ekstraksi pemrosesan single item dengan instance mariaDb dedicated per-thread (Tanpa Synchronized)
    public void processSingleItem(SpanSp2dStageIn item, Map<String, BifastRcMapping> mappingRcAe, ServiceMariaDb mariaDb) {
        System.out.println("========================================");
        System.out.println("Processing : " + item.getBeneficiaryAccount());

        // ACCOUNT INQUIRY
        AccountInquiryRequest accountInquiryRequest = buildAccountInquiryRequest(item);
        AccountInquiryResponse accountInquiryResponse = null;

        // [FIX][2026-07-31] GAP 1: Wrap call AE dengan try-catch ApiClientException.
        try {
            accountInquiryResponse = client.accountInquiry(accountInquiryRequest);
        } catch (ApiClientException aeEx) {
            if (aeEx.isNetworkTimeout()) {
                MainCHK.tulisLog("[GAP1] Timeout saat Account Inquiry (Skenario 6) untuk doc: " + item.getDocumentNumber() + " - " + aeEx.getMessage());
                try {
                    mariaDb.failedProcessBifast(item, "51");
                } catch (SQLException sqlEx) {
                    MainCHK.tulisLog("Error update esb_response_code saat AE timeout: " + sqlEx.getMessage());
                }
            } else {
                MainCHK.tulisLog("[GAP1] API Error (non-timeout) saat Account Inquiry untuk doc: " + item.getDocumentNumber() + " - " + aeEx.getMessage());
                try {
                    mariaDb.failedProcessBifast(item, "ERR_HTTP");
                } catch (SQLException sqlEx) {
                    MainCHK.tulisLog("Error update esb_response_code saat AE error: " + sqlEx.getMessage());
                }
            }
            return;
        }

        // [FIX][2026-07-31] GAP 1: Null-check setelah AE call (safety net)
        if (accountInquiryResponse == null) {
            MainCHK.tulisLog("[GAP1] accountInquiryResponse null setelah call AE untuk doc: " + item.getDocumentNumber());
            try {
                mariaDb.failedProcessBifast(item, "ERR_NULL");
            } catch (SQLException sqlEx) {
                MainCHK.tulisLog("Error update status: " + sqlEx.getMessage());
            }
            return;
        }

        boolean isAccountValid = accountInquiryResponse != null && accountInquiryResponse.isSuccess();
        String responseCode = Utillity.safe(accountInquiryResponse.getResponseCode());
        boolean isNameMatched = isAccountValid 
                && item.getBeneficiaryName() != null 
                && accountInquiryResponse.getCreditorName() != null 
                && item.getBeneficiaryName().trim().equalsIgnoreCase(accountInquiryResponse.getCreditorName().trim());

        boolean accountNoutFound = false;
        boolean beneficiaryBankIsnotAvailable = false;
        String mappingRcSpan = "";

        if (accountInquiryResponse.getResponseCode().equals("25")) {
            MainCHK.tulisLog("HADUHHHH");
            accountNoutFound = isRc25ForAccountnotFound(Utillity.safe(accountInquiryResponse.getResponseMessage()));
            
            if (!accountNoutFound) {
                beneficiaryBankIsnotAvailable = isRc25ForBankMaintanance(Utillity.safe(accountInquiryResponse.getResponseMessage()));
            }
            mappingRcSpan = getMappingRc25(mappingRcAe);
        }

        boolean isReturCode = accountNoutFound || "78".equals(responseCode) || (isAccountValid && !isNameMatched);
        boolean isFallbackSkn = "500".equals(responseCode);

        try {
            MainCHK.tulisLog("Proses Generate Posting");
            mariaDb.updateSp2dstageinBo2(item);
            mariaDb.updateSp2dUploaded(item);
        } catch (SQLException e) {
            MainCHK.tulisLog("Error saat update status pst pada table stage in dan uploaded " + e.getMessage());
        }

        MainCHK.tulisLog("Status Ae " + accountInquiryResponse.getResponseCode());

        // Case 1 : Ae dan ct sukses
        if (accountInquiryResponse.isSuccess() && isAccountValid) {

            executeCreditTransferFlow(item, mariaDb);
        }
        // case 2 : AE retur 
        else if (isReturCode) {
            MainCHK.tulisLog("Proses Retur");
            executeAccountInquiryReturFlow(item, accountInquiryResponse, mariaDb);
        }
        else if (beneficiaryBankIsnotAvailable) {
            try {
                mariaDb.failedProcessBifast(item, mappingRcSpan);
            } catch (SQLException e) {
                MainCHK.tulisLog(e.getMessage());
            }
        }
        // Case 3 : Fallback skn
        else if (isFallbackSkn) {
            try {
                mariaDb.fallbackToSkn(item);
            } catch (SQLException e) {
                MainCHK.tulisLog(e.getMessage());
            }
        }
    }

    private void executeCreditTransferFlow(SpanSp2dStageIn item, ServiceMariaDb mariaDb) {
        MainCHK.tulisLog("Initiate Proses Credit Transfer");
        CreditTransferRequest ctRequest = buildCreditTransferRequest(item);
        try {
            CreditTransferResponse ctResponse = null;
            try {
                ctResponse = client.creditTransfer(ctRequest);
            } catch (ApiClientException e) {
                if (e.isNetworkTimeout()) {
                    MainCHK.tulisLog("[GAP6] Network timeout saat Credit Transfer untuk doc: " + item.getDocumentNumber() + " - " + e.getMessage());
                    ctResponse = new CreditTransferResponse();
                    ctResponse.setResponseCode("51");
                    handleCreditTransferTimeout(item, ctResponse, mariaDb);
                    return;
                } else {
                    MainCHK.tulisLog("API Client Error saat Credit Transfer: " + e.getMessage());
                    try {
                        mariaDb.failedProcessBifast(item, "ERR_HTTP");
                    } catch (SQLException sqlEx) {
                        MainCHK.tulisLog("Error update status error API: " + sqlEx.getMessage());
                    }
                    return;
                }
            }

            if (ctResponse == null) {
                MainCHK.tulisLog("[GAP6] ctResponse null setelah call CT untuk doc: " + item.getDocumentNumber());
                return;
            }

            MainCHK.tulisLog("Proses CT selesai dengan Response Code: " + ctResponse.getResponseCode());
          
            if (ctResponse.isSuccess()) {
                ctResponse.setReferenceId(("FTDUMMY_HARUSNYA INI DARI ESB"));
                mariaDb.postingMessageAfterCt(item, ctResponse);
                mariaDb.prosesAck(item.getDocumentNumber());
            } else {
                MainCHK.tulisLog("HADUHHHHHH");
                handleCreditTransferFailure(item, ctResponse, mariaDb);
            }
        } catch (Exception e) {
            MainCHK.tulisLog("Error pada alur Credit Transfer: " + e.getMessage());
        }
    }

    private void handleCreditTransferFailure(SpanSp2dStageIn item, CreditTransferResponse ctResponse, ServiceMariaDb mariaDb) throws SQLException {
        String respCode = Utillity.safe(ctResponse.getResponseCode());
        boolean timeoutFromCi = "51".equals(respCode);
        boolean timeoutCoreFt = "57".equals(respCode);
        boolean isBeneficiaryDormant = "78".equals(respCode);
        boolean corePostingFailed = "05".equals(respCode);
        boolean failedResponseFromCi = "25".equals(respCode);

        // Skenario 1: Timeout BI-FAST Hub (RC 51)
        if (true) {
            mariaDb.handleResponseTimeoutFromCi(item);
            int numOfRetryStatus = mariaDb.getNumRetryStatus(item);
            if (numOfRetryStatus < 5) {
                handleCreditTransferTimeout(item, ctResponse, mariaDb);
            }
        } 
        // Skenario 2: Failed Credit transfer (Timeout from Core Banking FT Processing) RC 57
        else if (timeoutCoreFt) {
            MainCHK.tulisLog("Failed Credit transfer: Timeout from Core Banking FT Processing (RC 57) untuk doc: " + item.getDocumentNumber());
            mariaDb.handleTimeoutCoreProcess(item);
        } 
        // Skenario 3: Failed Credit transfer (Core Banking Posting failed - error validasi core) RC 05
        else if (corePostingFailed) {
            MainCHK.tulisLog("Failed Credit transfer: Core Banking Posting failed - error validasi core (RC 05) untuk doc: " + item.getDocumentNumber());
            mariaDb.handleCorePostingFailed(item);
        } 
        // Skenario 4: Failed Response dari BI-FAST (RC 25) -> Retur FT
        else if (failedResponseFromCi) {
            executeTransactionRetur(item, ctResponse, mariaDb);
        }
        else if (isBeneficiaryDormant) {
            MainCHK.tulisLog("[GAP2] Failed CT: Account Inactive/Dormant (RC 78) untuk doc: " + item.getDocumentNumber() + ". Melakukan proses retur FT.");
            executeTransactionRetur(item, ctResponse, mariaDb);
        }
    }

    private void executeAccountInquiryReturFlow(SpanSp2dStageIn item, AccountInquiryResponse inquiryResponse, ServiceMariaDb mariaDb) {
        MainCHK.tulisLog("Account Inquiry retur process initiated");

        String debitAccount = Utillity.safe(item.getAgentBankAccountNumber());
        String creditAccount = "IDR1717700010001";
        String agentBankAccountName = "";

        String transactionType = Utillity.getTransactionType(item, mariaDb.getSpanconfig());

        switch (transactionType) {
            case "BO2":
                agentBankAccountName = "RPKBUN GAJI BSI";
                break;
            case "BO1":
                agentBankAccountName = "RPKBUN NON GAJI BSI";
                break;
        }

        FundsTransferSoapResponse resp = ProsesRetur.returProcess(item, debitAccount, creditAccount, transactionType);

        if (resp != null && resp.isSuccess()) {
            MainCHK.tulisLog("Proses Insert data awal ke tabel posting");
            try {
                mariaDb.insertPostingAeFailure(item, inquiryResponse);
            } catch (SQLException e) {
                MainCHK.tulisLog("Gagal insert posting AE Failure: " + e.getMessage());
            }
            MainCHK.tulisLog("Sukses retur AE transaksi: " + resp.getTransactionId());
            try {
                SpanSp2dStageIn dataRetur = cloneItemForRetur(item);
                dataRetur.setBeneficiaryAccount(creditAccount);
                dataRetur.setAgentBankAccountNumber(debitAccount);
                dataRetur.setAgentBankAccountName(agentBankAccountName);

                // Fetch mapping awal menggunakan koneksi sementara
                 ;
                String mappingRcSpan = getMappingRc25(mariaDb.getBifastMappingRcAe());

                mariaDb.updateEsbResponseCode(item, mappingRcSpan);
                mariaDb.insertReturDatainPostingTable(dataRetur, resp);
                mariaDb.updateSp2dstageinAEerror(item);
                mariaDb.prosesAckRetur(item.getDocumentNumber());
            } catch (Exception e) {
                MainCHK.tulisLog("Error insert data retur AE pada tabel posting: " + e.getMessage());
            }
        } else {
            MainCHK.tulisLog("Gagal retur AE, update status agar diproses scheduler retry");
            try {
                mariaDb.updateStatusForRetryRetur(item);
            } catch (SQLException e) {
                MainCHK.tulisLog("Error update status retry retur: " + e.getMessage());
            }
        }
    }

    private AccountInquiryRequest buildAccountInquiryRequest(SpanSp2dStageIn item) {
        AccountInquiryRequest inquiryRequest = new AccountInquiryRequest();
        inquiryRequest.setRequestId(RequestIdGenerator.generateRequestID());
        inquiryRequest.setRequestDate(RequestIdGenerator.currentRequestDate());
        inquiryRequest.setChannelType("02");
        inquiryRequest.setCategoryPurposeCode("01");
        inquiryRequest.setInterbankSettlementAmount(item.getAmount().toString());
        inquiryRequest.setChargeBearerCode("DEBT");
        inquiryRequest.setBankCode("BBBAIDJA");
        inquiryRequest.setCreditorAccountId(item.getBeneficiaryAccount());
        return inquiryRequest;
    }

    private CreditTransferRequest buildCreditTransferRequest(SpanSp2dStageIn item) {
        CreditTransferRequest ctRequest = new CreditTransferRequest();
        ctRequest.setRequestId(RequestIdGenerator.generateRequestID());
        ctRequest.setRequestDate(RequestIdGenerator.currentRequestDate());
        ctRequest.setChannelType("99");
        ctRequest.setCategoryPurposeCode("03");
        ctRequest.setInterbankSettlementAmount(item.getAmount().toString());
        ctRequest.setChargeBearerCode("DEBT");
        ctRequest.setDebitorAccountId("7927927928");
        ctRequest.setDebitorAccountType("OTHR");
        ctRequest.setCreditorAccountId("4124639862");
        ctRequest.setCreditorAccountType("SVGS");
        ctRequest.setCreditorName("XUSER XREMIT XSATU DUA");
        ctRequest.setBankCode("BBBAIDJA");
        ctRequest.setPaymentInformation("Cek ombak BIfast");
        ctRequest.setDebitorType("02");
        ctRequest.setDebitorResidentStatus("01");
        ctRequest.setDebitorTownName("");
        ctRequest.setCreditorType("02");
        ctRequest.setCreditorResidentStatus("02");
        return ctRequest;
    }

    private SpanSp2dStageIn cloneItemForRetur(SpanSp2dStageIn item) {
        return new SpanSp2dStageIn(
                item.getId(),
                item.getApplicationAreaSenderIdentifier(),
                item.getApplicationAreaReceiverIdentifier(),
                item.getApplicationAreaDetailSenderIdentifier(),
                item.getApplicationAreaDetailReceiverIdentifier(),
                item.getApplicationAreaCreationDateTime(),
                item.getApplicationAreaMessageIdentifier(),
                item.getApplicationAreaMessageTypeIndicator(),
                item.getApplicationAreaMessageVersionText(),
                item.getDocumentDate(),
                item.getDocumentNumber(),
                item.getBeneficiaryName(),
                item.getBeneficiaryBankCode(),
                item.getBeneficiaryBank(),
                item.getBeneficiaryAccount(),
                item.getAmount(),
                item.getCurrencyTarget(),
                item.getDescription(),
                item.getAgentBankCode(),
                item.getAgentBankAccountNumber(),
                item.getAgentBankAccountName(),
                item.getEmailAddress(),
                item.getSwiftCode(),
                item.getIbanCode(),
                item.getPaymentMethod(),
                item.getSp2dCount(),
                item.getTotalCount(),
                item.getTotalAmount(),
                item.getTotalBatchCount(),
                item.getSp2dNumber(),
                item.getDatePosting(),
                item.getReferenceNumber(),
                item.getReturnCode(),
                item.getStatus()
        );
    }

    public void handleCreditTransferTimeout(SpanSp2dStageIn item, CreditTransferResponse cTransferResponse, ServiceMariaDb mariaDb) {
        try {
            MainCHK.tulisLog("Memproses Status Payment Request  untuk doc: " + item.getDocumentNumber() + " [code=" + cTransferResponse.getResponseCode() + "]");

            PaymenStatusRequest tiRequest = new PaymenStatusRequest();
            tiRequest.setRequestId(RequestIdGenerator.generateRequestID());
            tiRequest.setChannelType("02");
            tiRequest.setBicSendSys("BSMDIDJA");
            tiRequest.setBicRecvSys("FASTIDJA");
            tiRequest.setOriginator("O");
            tiRequest.setEndToEndId("Dummy end to end id ");

            PaymentStatusResponse tiResponse = null;

            try {
                tiResponse = client.transactionInquiry(tiRequest);
            } catch (ApiClientException e) {
                if (e.isNetworkTimeout()) {
                    MainCHK.tulisLog("Network timeout saat get status inquiry: " + e.getMessage());
                } else {
                    MainCHK.tulisLog("API Exception saat get status inquiry: " + e.getMessage());
                }
            } catch (Exception e) {
                MainCHK.tulisLog("Error memanggil API Status Inquiry: " + e.getMessage());
            }

            if (tiResponse != null) {
                MainCHK.tulisLog("Menerima Response Get Status: Code=" + tiResponse.getResponseCode() + ", Message=" + tiResponse.getResponseMessage());

                if (tiResponse.isSuccess()) {
                    MainCHK.tulisLog("Get status SUKSES. Melakukan posting & ACK sukses untuk doc: " + item.getDocumentNumber());
                    try {
                        CreditTransferResponse ctResponse = new CreditTransferResponse();
                        ctResponse.setResponseCode("000");
                        ctResponse.setReferenceId(cTransferResponse.getReferenceId());
                        mariaDb.postingMessageAfterCt(item, ctResponse);
                        mariaDb.prosesAck(item.getDocumentNumber());
                    } catch (SQLException e) {
                        MainCHK.tulisLog("Error posting/ACK setelah get status sukses: " + e.getMessage());
                    }
                } else if (tiResponse.isTimeout()) {
                    MainCHK.tulisLog("Get status MASIH TIMEOUT [ResponseCode 51]. Increament retry counter.");
                    try {
                        int counterUpdate = mariaDb.getNumRetryStatus(item) + 1;
                        MainCHK.tulisLog("Counter retry check status sekarang: " + counterUpdate);
                        // 2026-08-06
                        if (counterUpdate == 5){
                        CreditTransferResponse ctResponse = new CreditTransferResponse();
                        ctResponse.setResponseCode("000");
                        ctResponse.setReferenceId("FT DUMMY TOBE NYA HARUS DARI ESB ");
                        mariaDb.postingMessageAfterCt(item, ctResponse);
                        mariaDb.prosesAck(item.getDocumentNumber());
                        } else {
                        mariaDb.increaseCounterCheckstatusBifast(item, counterUpdate);
                        mariaDb.initiateRetryCheckStatus(item);
                        }
                         // 2026-08-06
                       
                    } catch (SQLException e) {
                        MainCHK.tulisLog("Error update counter retry status: " + e.getMessage());
                    }
                } else {
                    MainCHK.tulisLog("Get status GAGAL/NOT FOUND [ResponseCode=" + tiResponse.getResponseCode() + ", Message=" + tiResponse.getResponseMessage() + "]. Melakukan proses retur & ACK gagal.");
                    try {
                        CreditTransferResponse failResponse = (cTransferResponse != null) ? cTransferResponse : new CreditTransferResponse();
                        if (failResponse.getResponseCode() == null) {
                            failResponse.setResponseCode(tiResponse.getResponseCode() != null ? tiResponse.getResponseCode() : "25");
                        }
                        executeTransactionRetur(item, failResponse, mariaDb);
                    } catch (SQLException e) {
                        MainCHK.tulisLog("Error proses retur/ACK gagal setelah get status: " + e.getMessage());
                    }
                }
            } else {
                MainCHK.tulisLog("Status Inquiry tidak mendapat respon / Network Timeout. Increament retry counter.");
                try {
                    int counterUpdate = mariaDb.getNumRetryStatus(item) + 1;
                    MainCHK.tulisLog("Counter retry check status sekarang: " + counterUpdate);
                    mariaDb.increaseCounterCheckstatusBifast(item, counterUpdate);
                    mariaDb.initiateRetryCheckStatus(item);
                } catch (SQLException e) {
                    MainCHK.tulisLog("Error update counter retry status: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            MainCHK.tulisLog("Error fatal saat penanganan timeout CT: " + e.getMessage());
        }
    }

    private void executeTransactionRetur(SpanSp2dStageIn item, CreditTransferResponse ctResponse, ServiceMariaDb mariaDb) throws SQLException {
        MainCHK.tulisLog("Proses retur FT");
        String debitAccount = "IDR1717700010001";
        String creditAccount;
        String transactionType = Utillity.getTransactionType(item, mariaDb.getSpanconfig());

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
                creditAccount = mariaDb.getSpanconfig().getAcctRrRpkbunGaji();
                break;
            case "BO1":
                creditAccount = mariaDb.getSpanconfig().getAcctRrRpkbunNonGaji();
                break;
            default:
                creditAccount = mariaDb.getSpanconfig().getAcctRrReksusSbsn();
                break;
        }

        MainCHK.tulisLog("Rekening Kredit :" + creditAccount);
        FundsTransferSoapResponse resp = ProsesRetur.returProcess(item, debitAccount, creditAccount, transactionType);

        if (resp != null && resp.isSuccess()) {
            MainCHK.tulisLog("Sukses retur CT pada T24, Transaction ID: " + resp.getTransactionId());
            SpanSp2dStageIn dataRetur = cloneItemForRetur(item);

            dataRetur.setBeneficiaryAccount(creditAccount);
            dataRetur.setAgentBankAccountNumber(debitAccount);
            dataRetur.setAgentBankAccountName("IA KEWAJIBAN BIFAST");
            dataRetur.setDescription("Retur Transaksi");

            mariaDb.insertPostingCtFailure(item, ctResponse);
            mariaDb.insertReturDatainPostingTable(dataRetur, resp);
            mariaDb.prosesAckRetur(item.getDocumentNumber());
            if (resp.isSuccess()) {
                mariaDb.finalizeSuccessRecords(item);
            }
        } else {
            MainCHK.tulisLog("Gagal retur CT pada Core T24!");

            if (resp == null) {
                MainCHK.tulisLog("[GAP3] Skenario 18: Retur timeout dari Core Banking (resp == null) untuk doc: " + item.getDocumentNumber());
                try {
                    mariaDb.handleTimeoutCoreProcess(item);
                } catch (SQLException sqlEx) {
                    MainCHK.tulisLog("Error update timeout core saat retur: " + sqlEx.getMessage());
                }
                mariaDb.updateStatusForRetryRetur(item);
                mariaDb.failedProcessRetur(item, "05");
            } else {
                MainCHK.tulisLog(resp.toString());
                String txId = (resp.getTransactionId() != null) ? resp.getTransactionId() : "";
                if (resp.status != null && resp.status.messages != null && !resp.status.messages.isEmpty()) {
                    MainCHK.tulisLog("[GAP3] Skenario 17: Retur posting failed dari Core Banking untuk doc: " + item.getDocumentNumber());
                    SpanSp2dStageIn cloneUpdate = item;
                    cloneUpdate.setBiFastResponseCode("05");
                    mariaDb.handleCorePostingFailed(cloneUpdate);
                    mariaDb.updateStatusForRetryRetur(item);
                } else {
                    MainCHK.tulisLog("[GAP3] Retur gagal tanpa error message (kemungkinan timeout partial) untuk doc: " + item.getDocumentNumber());
                    try {
                        mariaDb.handleTimeoutCoreProcess(item);
                    } catch (SQLException sqlEx) {
                        MainCHK.tulisLog("Error update timeout core saat retur: " + sqlEx.getMessage());
                    }
                    mariaDb.updateStatusForRetryRetur(item);
                }
            }
        }
    }

    private Boolean isRc25ForAccountnotFound(String responseMessage) {
        String safeMessage = (responseMessage != null) ? responseMessage.toUpperCase() : "";
        if (safeMessage.contains("U136")) {
            return true;
        }else if (safeMessage.contains("53")){
            return true;
        }
        return false;
    }

    public String getMappingRc25(Map<String, BifastRcMapping> mapParam) {
        String responseMessage = Utillity.safe(mapParam.get("25").bifast_description);
        if (responseMessage.contains("U136")) {
            return mapParam.get("25").span_rc;
        }
        return mapParam.get("25U").span_rc;
    }

    private Boolean isRc25ForBankMaintanance(String responseMessage) {
        String safeMessage = (responseMessage != null) ? responseMessage.toUpperCase() : "";
        if (safeMessage.matches(".*U17[0-9X].*")) {
            return true;
        }
        return false;
    }

    // [NEW][2026-08-04] GAP 7: Method scheduler untuk memproses ulang data berstatus TMO-000 / RGS-000 (Status Inquiry Retry)
    public void prosesTimeoutCtBifast(List<SpanSp2dStageIn> processData) {
        MainCHK.tulisLog("[GAP7] Scheduler prosesTimeoutCtBifast dijalankan. Jumlah data: " + processData.size());
        if (processData == null || processData.isEmpty()) {
            MainCHK.tulisLog("[GAP7] Tidak ada data berstatus timeout/retry check status.");
            return;
        }
        ServiceMariaDb threadMariaDb = createMariaDbInstance();
        try {
            for (SpanSp2dStageIn item : processData) {
                MainCHK.tulisLog("[GAP7] Processing Timeout CT Status Inquiry untuk doc: " + item.getDocumentNumber());
                CreditTransferResponse dummyResponse = new CreditTransferResponse();
                dummyResponse.setResponseCode("51");
                dummyResponse.setEndToEndId(item.getReferenceNumber());
                handleCreditTransferTimeout(item, dummyResponse, threadMariaDb);
            }
        } finally {
            try { threadMariaDb.close(); } catch (Exception e) {}
        }
    }

    // [NEW][2026-08-04] GAP 7: Method scheduler untuk memproses ulang data berstatus RRS-000 (Retry Retur FT T24)
    public void prosesRetryRetur(List<SpanSp2dStageIn> processData) {
        MainCHK.tulisLog("[GAP7] Scheduler prosesRetryRetur dijalankan. Jumlah data: " + processData.size());
        if (processData == null || processData.isEmpty()) {
            MainCHK.tulisLog("[GAP7] Tidak ada data berstatus retry retur.");
            return;
        }
        ServiceMariaDb threadMariaDb = createMariaDbInstance();
        try {
            for (SpanSp2dStageIn item : processData) {
                MainCHK.tulisLog("[GAP7] Processing Retry Retur FT untuk doc: " + item.getDocumentNumber());
                CreditTransferResponse dummyResponse = new CreditTransferResponse();
                dummyResponse.setResponseCode("25");
                try {
                    executeTransactionRetur(item, dummyResponse, threadMariaDb);
                } catch (Exception e) {
                    MainCHK.tulisLog("[GAP7] Error saat retry retur doc " + item.getDocumentNumber() + ": " + e.getMessage());
                }
            }
        } finally {
            try { threadMariaDb.close(); } catch (Exception e) {}
        }
    }
}

