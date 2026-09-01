package com.bsi.entity.mock;


import java.io.File;
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
import com.bsi.entity.span.PathPropertiesBifast;
import com.bsi.entity.t24.FundsTransferSoapResponse;
import com.bsi.service.BifastClient;
import com.bsi.service.ProsesRetur;
import com.bsi.service.ServiceMariaDb;
import com.bsi.service.T24Client;
import com.bsi.utility.RequestIdGenerator;
import com.bsi.utility.Utillity;

public class ProcessBifast {

    // [IMPROVEMENT][2026-08-11] Path & PropName dinamis dengan fallback ke Environment Variables
    public String pathProp;
    public String propName;
    private int numThread;
    private BifastConfig config;
    private BifastClient client;
    public T24Config configT24;
    private T24Client t24Client;
    private PathPropertiesBifast pathPropertiesBifast;

    // Parameterized Constructor (BifastConfig, String, String)
    public ProcessBifast(BifastConfig config , PathPropertiesBifast pathPropertiesBifast) {
        this.config = config;
        this.pathPropertiesBifast = pathPropertiesBifast;
        initClientsAndConfig();
    }

    // [FIX][2026-08-12] Inisialisasi client & config di dalam method constructor setelah pathProp & propName terisi
    private void initClientsAndConfig() {

        this.pathProp = this.pathPropertiesBifast.getPathProp();
        this.propName = this.pathPropertiesBifast.getPropName();
        this.client = new BifastClient(this.config);
        String configFilePath = pathPropertiesBifast.getPathProp() + (pathPropertiesBifast.getPathProp().endsWith("/") || pathPropertiesBifast.getPathProp().endsWith("\\") ? "" : File.separator) + pathPropertiesBifast.getPropName() + ".properties";
        this.configT24 = T24Config.fromProperties(configFilePath);
        this.numThread = config.getNumThread();
        this.t24Client = new T24Client(this.configT24);
    }


    // 2026-08-04] Factory method untuk membuat dedicated instance ServiceMariaDb (1 koneksi DB terisolasi per Worker Thread)
    private ServiceMariaDb createMariaDbInstance() {
        if (this.pathProp == null || this.propName == null) {
            MainCHK.tulisLog("[ERROR][2026-08-12] pathProp atau propName null saat createMariaDbInstance! Using default fallback.");
            return new ServiceMariaDb(config.getPathPropertiesBifast().getPathProp(), config.getPathPropertiesBifast().getPropName());
        }
        return new ServiceMariaDb(this.pathProp, this.propName);
    }

    // [2026-08-04] Multi-Threading dengan 3 Dedicated Worker Threads & 3 Instance ServiceMariaDb Terpisah.
    public String paymentBifast(List<SpanSp2dStageIn> processData) {
        if (processData == null || processData.isEmpty()) {
            MainCHK.tulisLog("Tidak ada data BI-FAST untuk diproses.");
            return "00";
        }

        int numThreads = this.numThread;
        int totalData = processData.size();
        long startTime = System.currentTimeMillis();

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

        //  Graceful Shutdown Hook untuk memastikan worker thread aktif selesai jika JVM menerima signal (SIGTERM/SIGINT)
        Thread shutdownHook = new Thread(() -> {
            if (!executor.isTerminated()) {
                MainCHK.tulisLog("[SHUTDOWN-HOOK] Signal shutdown terdeteksi! Menunggu worker threads menyelesaikan transaksi aktif...");
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        for (int i = 1; i <= numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                MainCHK.tulisLog("[WORKER-" + threadId + "] Worker thread dimulai (Membuka koneksi MariaDB dedicated)...");
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
            MainCHK.tulisLog("Error:updated saat menunggu worker threads: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            // Unregister shutdown hook jika proses selesai dengan normal
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); } catch (Exception e) {}
        }

        // [IMPROVEMENT][2026-08-11] Cetak Operational Summary Metrics ke Log
        long duration = System.currentTimeMillis() - startTime;
        MainCHK.tulisLog("=======================================================");
        MainCHK.tulisLog("[SUMMARY][2026-08-11] BI-FAST BATCH PROCESSING COMPLETED");
        MainCHK.tulisLog(" Total Processed   : " + totalData);
        MainCHK.tulisLog(" Total Duration    : " + duration + " ms (" + String.format("%.2f", duration / 1000.0) + " s)");
        MainCHK.tulisLog("=======================================================");

        return "00";
    }

    //  Ekstraksi pemrosesan single item dengan instance mariaDb dedicated per-thread (Tanpa Synchronized)
    public void processSingleItem(SpanSp2dStageIn item, Map<String, BifastRcMapping> mappingRcAe, ServiceMariaDb mariaDb) {
        System.out.println("========================================");
        System.out.println("Processing : " + item.getBeneficiaryAccount());

        // ACCOUNT INQUIRY
        AccountInquiryRequest accountInquiryRequest = buildAccountInquiryRequest(item);
        AccountInquiryResponse accountInquiryResponse = null;

        try {
            mariaDb.updateSp2dstagein(item);
        } catch (SQLException e) {
            MainCHK.tulisLog("Error saat update status pst pada table stage in " + e.getMessage());
        }

        try {
            accountInquiryRequest.setBankCode(mariaDb.getBankCode(item.getBeneficiaryBankCode()));
        } catch (SQLException e) {
            MainCHK.tulisLog("Gagal saat mapping bank code untuk document number :" + item.getDocumentNumber());
            return;
        }

        //Wrap call AE dengan try-catch ApiClientException.
        try {
            accountInquiryResponse = client.accountInquiry(accountInquiryRequest);
        } catch (ApiClientException aeEx) {
           if (aeEx.isNetworkTimeout()) {
               // tidak ada response dalam rentang waktu yang disepakati)
               MainCHK.tulisLog("Timeout saat Account Inquiry untuk doc: " + item.getDocumentNumber() + " - " + aeEx.getMessage());
           try {
                String documentNumberOnTable = mariaDb.getDataSp2dBifast(item.getDocumentNumber());
                if (documentNumberOnTable == null){
                    mariaDb.insertDataSp2dBfast(item);
                }
                 mariaDb.handleAccountInquiryError(item, "BLANK", mappingRcAe);
        }catch (SQLException sqlEx) {
              MainCHK.tulisLog("Error update DB saat AE timeout: " + sqlEx.getMessage());
          }
        } else {
        // Pengecekan spesifik untuk HTTP Status Code 404 (Not Found) dan 500 (Internal Server Error)
          int httpCode = aeEx.getHttpStatus();
        if (httpCode == 404) {
               MainCHK.tulisLog(" Response HTTP 404 (Not Found) saat Account Inquiry untuk doc: " + item.getDocumentNumber() + " - " + aeEx.getMessage());
        try {
             String documentNumberOnTable = mariaDb.getDataSp2dBifast(item.getDocumentNumber());
                if (documentNumberOnTable == null){
                    mariaDb.insertDataSp2dBfast(item);
                }
            mariaDb.handleAccountInquiryError(item, "404", mappingRcAe);
        } catch (SQLException sqlEx) {
            MainCHK.tulisLog("Error update DB saat AE HTTP 404: " + sqlEx.getMessage());
        }
    } else if (httpCode == 508) {
        MainCHK.tulisLog("Response HTTP 508 saat Account Inquiry untuk doc: " + item.getDocumentNumber() + " - " + aeEx.getMessage());
        try {
             String documentNumberOnTable = mariaDb.getDataSp2dBifast(item.getDocumentNumber());
                if (documentNumberOnTable == null){
                    mariaDb.insertDataSp2dBfast(item);
                }
            mariaDb.handleAccountInquiryError(item, "508", mappingRcAe);
        } catch (SQLException sqlEx) {
            MainCHK.tulisLog("Error update DB saat AE HTTP 508: " + sqlEx.getMessage());
        }
        }
            return;
    }
    }

        String responseCode = Utillity.safe(accountInquiryResponse.getResponseCode());

        boolean isAccountValid = accountInquiryResponse != null && accountInquiryResponse.isSuccess();

        boolean isNameMatched = isAccountValid 
                && item.getBeneficiaryName() != null 
                && accountInquiryResponse.getCreditorName() != null 
                && item.getBeneficiaryName().trim().equalsIgnoreCase(accountInquiryResponse.getCreditorName().trim());

        boolean accountNoutFound = false;
        boolean beneficiaryBankIsnotAvailable = false;

        String mappingRcSpan = "";

        if (accountInquiryResponse.getResponseCode().equals("25")) {
            accountNoutFound = isRc25ForAccountnotFound(Utillity.safe(accountInquiryResponse.getResponseMessage()),mappingRcAe);
            if (!accountNoutFound) {
                beneficiaryBankIsnotAvailable = true;
            }
        }
    
        mappingRcSpan = getSpanRc(responseCode,accountInquiryResponse.getResponseMessage(),mappingRcAe);
       
        boolean isReturCode = accountNoutFound || "78".equals(responseCode);
        boolean isFallbackSkn = "99".equals(responseCode) || !isNameMatched;



        MainCHK.tulisLog("Account Inquiry Rsponse Code :  " + accountInquiryResponse.getResponseCode());
        

        // Case 1 : Ae dan ct sukses
        if (accountInquiryResponse.isSuccess() && isAccountValid && isNameMatched) {
            executeCreditTransferFlow(item, mariaDb);
        }
        // case 2 : AE retur 
        else if (isReturCode) {
            MainCHK.tulisLog("Proses Retur Validasi Account Inquiry dengan response code : "+ accountInquiryResponse.getResponseCode());
            executeAccountInquiryReturFlow(item, accountInquiryResponse, mariaDb , mappingRcSpan);
        }
        else if (beneficiaryBankIsnotAvailable) {
            try {
                String documentNumberOnTableSp2dBifast =mariaDb.getDataSp2dBifast(item.getDocumentNumber());
            if (documentNumberOnTableSp2dBifast == null){
                  mariaDb.insertDataSp2dBfast(item);
                }
                  mariaDb.handleAccountInquiryErrorRcSpan(item, mappingRcSpan);
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

    public String getBifastDescription(String responseCode,String responseMessage,Map<String, BifastRcMapping> rcMapping) {
     String key;
        if ("25".equals(responseCode)) {
           String errorCode = Utillity.extractBifastDescription(responseMessage);
             key = responseCode + "|" + errorCode;
        } else {
         key = responseCode;
        }
        return rcMapping.get(key).bifast_description;
     }

    public String getSpanRc(String responseCode,String responseMessage,Map<String, BifastRcMapping> rcMapping) {
     String key;
        if ("25".equals(responseCode)) {
           String errorCode = Utillity.extractBifastDescription(responseMessage);
             key = responseCode + "|" + errorCode;
        } else {
         key = responseCode;
        }
        return rcMapping.get(key).span_rc;
     }

    private void executeCreditTransferFlow(SpanSp2dStageIn item, ServiceMariaDb mariaDb) {
        MainCHK.tulisLog("Initiate Proses Credit Transfer");
        CreditTransferRequest ctRequest = buildCreditTransferRequest(item);

        try {
            ctRequest.setBankCode(mariaDb.getBankCode(item.getBeneficiaryBankCode()));
        } catch (SQLException e) {
            MainCHK.tulisLog("Gagal saat mapping bank code untuk document number :" + item.getDocumentNumber());
            return;
        }
        try {
            CreditTransferResponse ctResponse = null;
            try {
                ctResponse = client.creditTransfer(ctRequest);
            } catch (ApiClientException e) {
                if (e.isNetworkTimeout()) {
                    MainCHK.tulisLog("Network timeout saat Credit Transfer untuk doc: " + item.getDocumentNumber() + " - " + e.getMessage());
                    ctResponse = new CreditTransferResponse();
                    ctResponse.setResponseCode("51");
                    handleCreditTransferTimeout(item, ctResponse, mariaDb);
                    return;
                } else {
                    MainCHK.tulisLog("API Client Error saat Credit Transfer: " + e.getMessage());
                    return;
                }
            }
            if (ctResponse == null) {
                MainCHK.tulisLog("ct Response null setelah call CT untuk doc: " + item.getDocumentNumber());
                return;
            }

            MainCHK.tulisLog("Proses ct selesai dengan Response Code : " + ctResponse.getResponseCode());
          
            if (ctResponse.isSuccess()) {
                mariaDb.postingMessageAfterCt(item, ctResponse);
                mariaDb.prosesAck(item);
            } else {
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
        if (timeoutFromCi) {
            mariaDb.handleResponseTimeoutFromCi(item ,ctResponse);
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
        //  Failed Response dari BI-FAST (RC 25) -> Retur FT
        else if (failedResponseFromCi) {

            SpanSp2dStageIn dataClone = item;
            dataClone.setReturnCode(ctResponse.getResponseCode());
            dataClone.setReferenceNumber(ctResponse.getReferenceId());

            mariaDb.updateErrorDataForReturProcess(dataClone);
            executeTransactionRetur(dataClone, mariaDb);
        }
        else if (isBeneficiaryDormant) {
            MainCHK.tulisLog("Failed CT: Account Inactive/Dormant (RC 78) untuk doc: " + item.getDocumentNumber() + ". Melakukan proses retur FT.");
            SpanSp2dStageIn dataClone = item;
            dataClone.setReturnCode(ctResponse.getResponseCode());
            dataClone.setReferenceNumber(ctResponse.getReferenceId());

            executeTransactionRetur(dataClone, mariaDb);
        }
    }

    private void executeAccountInquiryReturFlow(SpanSp2dStageIn item, AccountInquiryResponse inquiryResponse, ServiceMariaDb mariaDb,String rcSpan) {
        MainCHK.tulisLog("Account Inquiry retur process initiated");

        String debitAccount = Utillity.safe(item.getAgentBankAccountNumber());
        String creditAccount="";

        String transactionType = Utillity.getTransactionType(item, mariaDb.getSpanconfig());

        switch (transactionType.trim().toUpperCase()) {
            case "BO2":
               creditAccount  = mariaDb.getSpanconfig().getAcctRrRpkbunGaji();
                break;
            case "BO1":
                creditAccount = mariaDb.getSpanconfig().getAcctRrRpkbunNonGaji();
                break;
            default:
                creditAccount = mariaDb.getSpanconfig().getAcctRrReksusSbsn();
        }

        ProsesRetur prosesRetur = new ProsesRetur(configT24);
        FundsTransferSoapResponse resp = prosesRetur.returProcess(item, debitAccount, creditAccount, transactionType);
    
        if (resp != null && resp.isSuccess()) {
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
                mariaDb.insertReturDatainPostingTable(dataRetur, resp);
                String documentNumberOnTableSp2dBifast =mariaDb.getDataSp2dBifast(item.getDocumentNumber());
                 if (documentNumberOnTableSp2dBifast == null){
                      mariaDb.insertDataSp2dBfast(item);
                }
                mariaDb.handleAccountInquiryErrorRcSpan(item, rcSpan);
                mariaDb.updateSp2dstageinAEerror(item , inquiryResponse.getResponseCode());
                mariaDb.prosesAckRetur(item.getDocumentNumber());
            } catch (Exception e) {
                MainCHK.tulisLog("Error insert data retur AE pada tabel posting: " + e.getMessage());
            }
        } else {
            MainCHK.tulisLog("Gagal retur AE, update status agar diproses scheduler retry");
            try {
                mariaDb.updateStatusForRetryRetur(item,"SR011");
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
        inquiryRequest.setCreditorAccountId(item.getBeneficiaryAccount());
        return inquiryRequest;
    }

    private PaymenStatusRequest buildPaymentStatusRequest(SpanSp2dStageIn item){
       PaymenStatusRequest paymenStatusRequest = new PaymenStatusRequest();
        paymenStatusRequest.setRequestId(RequestIdGenerator.generateRequestID());
        paymenStatusRequest.setChannelType("02");
        paymenStatusRequest.setBicSendSys("BSMDIDJA");
        paymenStatusRequest.setOriginator("O");
        return paymenStatusRequest;
    }

    private CreditTransferRequest buildCreditTransferRequest(SpanSp2dStageIn item) {
        CreditTransferRequest ctRequest = new CreditTransferRequest();
        ctRequest.setRequestId(RequestIdGenerator.generateRequestID());
        ctRequest.setRequestDate(RequestIdGenerator.currentRequestDate());
        ctRequest.setChannelType("99");
        ctRequest.setCategoryPurposeCode("03");
        ctRequest.setInterbankSettlementAmount(item.getAmount().toString());
        ctRequest.setChargeBearerCode("DEBT");
        ctRequest.setDebitorAccountId(item.getAgentBankAccountNumber());
        ctRequest.setDebitorAccountType("OTHR");
        ctRequest.setCreditorAccountId(item.getBeneficiaryAccount());
        ctRequest.setCreditorAccountType("SVGS");
        ctRequest.setCreditorName(item.getBeneficiaryName());
        ctRequest.setPaymentInformation(Utillity.constructPaymentInformationBifast(item));
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
            MainCHK.tulisLog("Memproses Status Payment Request untuk doc : " + item.getDocumentNumber() + " [code=" + item.getReturnCode() + "]");
            PaymenStatusRequest tiRequest = buildPaymentStatusRequest(item);
            try{
                tiRequest.setEndToEndId(mariaDb.getDataEndToEndId(item.getDocumentNumber()));
            } catch (SQLException e ){
                MainCHK.tulisLog("Error saat get data end to end id untuk kebutuhan psr untuk doc : "+ item.getDocumentNumber());
            }
            PaymentStatusResponse tiResponse = null;

            try {
                tiResponse = client.transactionInquiry(tiRequest);
            } catch (ApiClientException e) {
                if (e.isNetworkTimeout()) {
                    MainCHK.tulisLog("Network timeout saat get status inquiry: " + e.getMessage());
                    
                    int counterUpdate = mariaDb.getNumRetryStatus(item) + 1;
                        if (counterUpdate == 5){
                         CreditTransferResponse ctResponse = new CreditTransferResponse();
                        ctResponse.setResponseCode("000");
                        ctResponse.setReferenceId(item.getReferenceNumber());
                        mariaDb.postingMessageAfterCt(item, ctResponse);
                        mariaDb.prosesAck(item);
                        } else if (counterUpdate == 1 ) {
                        SpanSp2dStageIn data =item;
                        data.setReferenceNumber(cTransferResponse.getReferenceId());
                        mariaDb.initiateRetryCheckStatus(data);
                        }
                        MainCHK.tulisLog("Increment counter PSR");
                        mariaDb.increaseCounterCheckstatusBifast(item, counterUpdate);

                } else {
                    MainCHK.tulisLog("API Exception saat get status inquiry: " + e.getMessage());
                }
            } catch (Exception e) {
                MainCHK.tulisLog("Error memanggil API Status Inquiry: " + e.getMessage());
            }

            if (tiResponse != null) {
                MainCHK.tulisLog("Menerima Response Get Status: Code=" + tiResponse.getResponseCode() + ", Message=" + tiResponse.getResponseMessage());

                if (tiResponse.isSuccess()) {
                    MainCHK.tulisLog("Get status sukses. Melakukan posting & ACK sukses untuk doc: " + item.getDocumentNumber());
                    try {
                        CreditTransferResponse ctResponse = new CreditTransferResponse();
                        ctResponse.setResponseCode("000");
                        // harus di cek
                        ctResponse.setReferenceId(item.getReferenceNumber());
                        mariaDb.postingMessageAfterCt(item, ctResponse);
                        mariaDb.prosesAck(item);
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
                        ctResponse.setReferenceId(item.getReferenceNumber());
                        mariaDb.postingMessageAfterCt(item, ctResponse);
                        mariaDb.prosesAck(item);
                        } else if (counterUpdate == 1 ) {
                          SpanSp2dStageIn data =item;
                          data.setReferenceNumber(cTransferResponse.getReferenceId());
                          mariaDb.initiateRetryCheckStatus(data);
                        }
                        MainCHK.tulisLog("Increment counter");
                        mariaDb.increaseCounterCheckstatusBifast(item, counterUpdate);
                         // 2026-08-06
                       
                    } catch (SQLException e) {
                        MainCHK.tulisLog("Error update counter retry status: " + e.getMessage());
                    }
                } else {
                    MainCHK.tulisLog("Get status GAGAL/NOT FOUND [ResponseCode=" + tiResponse.getResponseCode() + ", Message=" + tiResponse.getResponseMessage() + "]. Melakukan proses retur & ACK gagal.");
                    try {
                        executeTransactionRetur(item, mariaDb);
                    } catch (SQLException e) {
                        MainCHK.tulisLog("Error proses retur/ACK gagal setelah get status: " + e.getMessage());
                    }
                }
            } else {
                MainCHK.tulisLog("Status PSR tidak mendapat respon / Network Timeout. Increament retry counter.");
                try {
                    int counterUpdate = mariaDb.getNumRetryStatus(item) + 1;
                    MainCHK.tulisLog("Counter retry check status sekarang: " + counterUpdate);
                    if (counterUpdate == 5){
                        CreditTransferResponse ctResponse = new CreditTransferResponse();
                        ctResponse.setResponseCode("000");
                        ctResponse.setReferenceId(item.getReferenceNumber());
                        mariaDb.postingMessageAfterCt(item, ctResponse);
                        mariaDb.prosesAck(item);
                        }else if (counterUpdate == 1) {
                          SpanSp2dStageIn data =item;
                          data.setReferenceNumber(cTransferResponse.getReferenceId());
                          mariaDb.initiateRetryCheckStatus(data);
                        }
                        MainCHK.tulisLog("Increment counter");
                        mariaDb.increaseCounterCheckstatusBifast(item, counterUpdate);
                } catch (SQLException e) {
                    MainCHK.tulisLog("Error update counter retry status: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            MainCHK.tulisLog("Error fatal saat penanganan timeout CT: " + e.getMessage());
        }
    }

    private void executeTransactionRetur(SpanSp2dStageIn item, ServiceMariaDb mariaDb) throws SQLException {
        MainCHK.tulisLog("Initiate proses retur ke core banking untuk data sp2d dengan dokumen number "+item.getDocumentNumber());
        String debitAccount = mariaDb.getSpanconfig().getAcctIaKewajibanBifast();
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
        
          ProsesRetur prosesRetur = new ProsesRetur(configT24);
          FundsTransferSoapResponse resp = prosesRetur.returProcess(item, debitAccount, creditAccount, transactionType);

        if (resp != null && resp.isSuccess()) {
            MainCHK.tulisLog("Sukses retur pada T24, Transaction ID: " + resp.getTransactionId() + "Untuk dokumen number " + item.getDocumentNumber());
            SpanSp2dStageIn dataRetur = cloneItemForRetur(item);

            dataRetur.setBeneficiaryAccount(creditAccount);
            dataRetur.setAgentBankAccountNumber(debitAccount);

            // confirm value nya apa
            dataRetur.setAgentBankAccountName("IA KEWAJIBAN BIFAST");
            dataRetur.setDescription("Retur Transaksi");

            mariaDb.insertPostingCtFailure(item);
            mariaDb.insertReturDatainPostingTable(dataRetur, resp);
            MainCHK.tulisLog("Data Status : " +item.getStatus());
            mariaDb.prosesAckRetur(item.getDocumentNumber());

            switch (item.getStatus().trim().toUpperCase()) {
                 case "RRS-000":
                     mariaDb.finalizeSuccessRecordsAfterRetyRetur(item);
                     break; 
                case "RMR-000":
                     mariaDb.finalizeSuccessRecordsAfterReturManual(item);
                     break;
                case "PST-000":
                     mariaDb.finalizeSuccessRecords(item);
                     break;
                 default:
                     mariaDb.finalizeSuccessRecordsAfterGetStatus(item);
             }
            
        } else {
            MainCHK.tulisLog("Gagal retur CT pada Core T24! dengan response dari core"+ resp.status.toString() + "untuk document number : "+item.getDocumentNumber());
            if (resp == null) {
                mariaDb.updateStatusForRetryRetur(item ,"SR019");
            } else {
                if (resp.status != null && resp.status.messages != null && !resp.status.messages.isEmpty()) {
                    if (resp.status.transactionId != null && resp.status.successIndicator.equalsIgnoreCase("T24Error")){
                        mariaDb.updateStatusForRetryRetur(item ,"SR011");
                    }else if (resp.status.messages.get(0).equalsIgnoreCase("TAFJERR-1060: Session Expiration") && resp.status.successIndicator.equalsIgnoreCase("T24Error")){
                        mariaDb.updateStatusForRetryRetur(item ,"SR019");
                    }

                } else {
                    MainCHK.tulisLog("Retur gagal tanpa error message (kemungkinan timeout partial) untuk doc: " + item.getDocumentNumber());
                    mariaDb.updateStatusForRetryRetur(item , "SR019");
                }
            }
        }
    }

    private Boolean isRc25ForAccountnotFound(String responseMessage, Map<String ,BifastRcMapping> map) {
        String bifastDescription  = getBifastDescription("25", responseMessage, map);
        if (Utillity.safe(bifastDescription).matches(".*U17[0-9X].*")){
            MainCHK.tulisLog("Masuk sini ");
           return false;
        }
        return true;
    }

    // Method scheduler untuk memproses ulang data berstatus RGS-000 (PSR process)
    public void prosesTimeoutCtBifast(List<SpanSp2dStageIn> processData) {
        MainCHK.tulisLog("Scheduler get payment status dijalankan. Jumlah data: " + processData.size());
        if (processData == null || processData.isEmpty()) {
            MainCHK.tulisLog("Tidak ada data berstatus timeout/retry check status.");
            return;
        }

        int numThreads = this.numThread;
        int totalData=processData.size();
        
        Queue<SpanSp2dStageIn> taskQueue = new ConcurrentLinkedQueue<>(processData);
        AtomicInteger processedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i=1; i<=processData.size(); i++){
            final int thread = i;
            executor.submit(()->{
               MainCHK.tulisLog("[WORKER-" + thread + "] Worker thread dimulai (Membuka koneksi MariaDB dedicated)...");
               ServiceMariaDb threadMariaDb = createMariaDbInstance();
               try{
                 SpanSp2dStageIn item;
                 while((item = taskQueue.poll())!= null){
                    int count =processedCount.incrementAndGet();
                    MainCHK.tulisLog("[WORKER-" + thread + "] Memproses document: " + item.getDocumentNumber() + " (" + count + "/" + totalData + ")");
                    CreditTransferResponse ctResponse  = new CreditTransferResponse();
                    ctResponse.setResponseCode(item.getReturnCode());
                    try {
                     handleCreditTransferTimeout(item, ctResponse, threadMariaDb);
                    } catch (Exception e){
                        MainCHK.tulisLog("Error saat pemanggilan awal proses retur" + e.getMessage());
                    }
                 }
                }finally{
                    try{
                     threadMariaDb.close();
                    }catch(Exception e ){
                     MainCHK.tulisLog("[WORKER-" + thread + "] Error closing DB connection: " + e.getMessage()); 
                    }
                }
                     MainCHK.tulisLog("[WORKER-" + thread + "] Worker thread selesai (Koneksi DB ditutup).");
            });

            executor.shutdown();
            try {
                executor.awaitTermination(2, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                MainCHK.tulisLog("[MULTI-THREAD] Interrupted saat menunggu worker threads: " + e.getMessage());
                Thread.currentThread().interrupt();
            }

        }
        
    }

    //  Method scheduler untuk memproses ulang data berstatus RRS-000 (Retry Retur FT T24)
    public void prosesRetryRetur(List<SpanSp2dStageIn> processData) {
        MainCHK.tulisLog("Scheduler prosesRetryRetur dijalankan. Jumlah data: " + processData.size());
        if (processData == null || processData.isEmpty()) {
            MainCHK.tulisLog("Tidak ada data berstatus retry retur.");
            return;
        }

        int numThreads = this.numThread;
        int totalData=processData.size();
        
        Queue<SpanSp2dStageIn> taskQueue = new ConcurrentLinkedQueue<>(processData);
        AtomicInteger processedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i=1 ; i <= numThreads ; i++){
            final int thread = i;
            executor.submit(()->{
              MainCHK.tulisLog("[WORKER-" + thread + "] Worker thread dimulai (Membuka koneksi MariaDB dedicated)...");
               ServiceMariaDb threadMariaDb = createMariaDbInstance();
               try{
                 SpanSp2dStageIn item;
                 while((item = taskQueue.poll())!= null){
                    int count =processedCount.incrementAndGet();
                    MainCHK.tulisLog("[WORKER-" + thread + "] Memproses document: " + item.getDocumentNumber() + " (" + count + "/" + totalData + ")");
                    try {
                     executeTransactionRetur(item,threadMariaDb);
                    } catch (SQLException e){
                        MainCHK.tulisLog("Error saat pemanggilan awal proses retur" + e.getMessage());
                    }
                 }
                }finally{
                    try{
                     threadMariaDb.close();
                    }catch(Exception e ){
                     MainCHK.tulisLog("[WORKER-" + thread + "] Error closing DB connection: " + e.getMessage()); 
                    }
                }
                     MainCHK.tulisLog("[WORKER-" + thread + "] Worker thread selesai (Koneksi DB ditutup).");
            });
        }
        executor.shutdown();
         try {
            executor.awaitTermination(2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            MainCHK.tulisLog("[MULTI-THREAD] Interrupted saat menunggu worker threads: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    public void prosesRetur(List<SpanSp2dStageIn> processData) {
        MainCHK.tulisLog("Scheduler proses Retur dijalankan. Jumlah data: " + processData.size());
        if (processData == null || processData.isEmpty()) {
            MainCHK.tulisLog("Tidak ada data berstatus retry retur.");
            return;
        }

        int numThreads = this.numThread;
        int totalData=processData.size();
        
        Queue<SpanSp2dStageIn> taskQueue = new ConcurrentLinkedQueue<>(processData);
        AtomicInteger processedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i=1 ; i <= numThreads ; i++){
            final int thread = i;
            executor.submit(()->{
              MainCHK.tulisLog("[WORKER-" + thread + "] Worker thread dimulai (Membuka koneksi MariaDB dedicated)...");
               ServiceMariaDb threadMariaDb = createMariaDbInstance();
               try{
                 SpanSp2dStageIn item;
                 while((item = taskQueue.poll())!= null){
                    int count =processedCount.incrementAndGet();
                    MainCHK.tulisLog("[WORKER-" + thread + "] Memproses document: " + item.getDocumentNumber() + " (" + count + "/" + totalData + ")");

                    try {
                     executeTransactionRetur(item, threadMariaDb);
                    } catch (SQLException e){
                        MainCHK.tulisLog("Error saat pemanggilan awal proses retur" + e.getMessage());
                    }
                 }
                }finally{
                    try{
                     threadMariaDb.close();
                    }catch(Exception e ){
                     MainCHK.tulisLog("[WORKER-" + thread + "] Error closing DB connection: " + e.getMessage()); 
                    }
                }
                     MainCHK.tulisLog("[WORKER-" + thread + "] Worker thread selesai (Koneksi DB ditutup).");
            });
        }
        executor.shutdown();
         try {
            executor.awaitTermination(2, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            MainCHK.tulisLog("[MULTI-THREAD] Interrupted saat menunggu worker threads: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}