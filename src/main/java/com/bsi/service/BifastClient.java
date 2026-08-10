package com.bsi.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bsi.MainCHK;
import com.bsi.config.BifastConfig;
import com.bsi.entity.api.ApiClientException;
import com.bsi.entity.bifast.accountinquiry.AccountInquiryRequest;
import com.bsi.entity.bifast.accountinquiry.AccountInquiryResponse;
import com.bsi.entity.bifast.credittransfer.CreditTransferRequest;
import com.bsi.entity.bifast.credittransfer.CreditTransferResponse;
import com.bsi.entity.bifast.transactioninquiry.PaymenStatusRequest;
import com.bsi.entity.bifast.transactioninquiry.PaymentStatusResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BifastClient {
    private final BifastConfig config;
    private final ObjectMapper mapper;

    public static final Logger log = LoggerFactory.getLogger(BifastClient.class);

    public BifastClient (BifastConfig config){
        this.config = config;
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    public AccountInquiryResponse accountInquiry(AccountInquiryRequest request){
        return post(config.getEndpointAe(), request, AccountInquiryResponse.class);
    }

    public CreditTransferResponse creditTransfer(CreditTransferRequest request){
        return post (config.getEndpointCT(), request,CreditTransferResponse.class);
    }

    public PaymentStatusResponse transactionInquiry(PaymenStatusRequest request){
    if (config.getEndpointTi() == null || config.getEndpointTi().trim().isEmpty()) {
        log.warn("Endpoint Transaction Inquiry belum dikonfigurasi/kontrak API ESB belum tersedia");
        throw new ApiClientException("Endpoint Transaction Inquiry belum dikonfigurasi di properties", -1, null);
    }
    return post(config.getEndpointTi(), request, com.bsi.entity.bifast.transactioninquiry.PaymentStatusResponse.class);
}


private static void disableSslVerification() {
    try {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(
                        X509Certificate[] certs,
                        String authType) {
                }

                @Override
                public void checkServerTrusted(
                        X509Certificate[] certs,
                        String authType) {
                }
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());

        HttpsURLConnection.setDefaultSSLSocketFactory(
                sc.getSocketFactory());

        HttpsURLConnection.setDefaultHostnameVerifier(
                (hostname, session) -> true);

    } catch (Exception e) {
       throw new RuntimeException("Gagal disable SSL verification",e);
    }
}



private <T> T post(String path, Object requestBody, Class<T> responseType) { 
    String url =  path;
    HttpURLConnection conn = null;
    try {

        disableSslVerification();
        MainCHK.tulisLog("[WARNING] SSL Certificate Validation Disabled (DEV ONLY)");
        String jsonRequest = mapper.writeValueAsString(requestBody);
        log.info("POST {} request: {}", url, jsonRequest);

        // Logging payload request integrasi BI-FAST
        MainCHK.tulisLog("Payload Request [" + url + "]: " + jsonRequest);

        conn = (HttpURLConnection) new URL (url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(config.getConnectTimeoutMs());
        conn.setReadTimeout(config.getreadTimeoutms());
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("x-Gateway-APIKey", config.getapiKey());
    
        byte[] payload = jsonRequest.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
            os.flush();
        }

        int status = conn.getResponseCode();
        String responseBody = readBody(status >= 200 && status < 300
                ? conn.getInputStream() : conn.getErrorStream());

        log.info("POST {} status={} response: {}", url, status, responseBody);
        // [LOG][2026-08-03] Requirement: Logging payload response integrasi BI-FAST via MainCHK.tulisLog
        MainCHK.tulisLog("Payload Response [" + url + "] status=" + status + ": " + responseBody);

        if (status < 200 || status >= 300) {
            throw new ApiClientException(
                    "HTTP error dari server: " + status, status, responseBody);
        }
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new ApiClientException("Response body kosong dari server", status, responseBody);
        }
        return mapper.readValue(responseBody, responseType);

    } catch (IOException e) {
        // [LOG][2026-08-03] Requirement: Logging error IO/Timeout integrasi BI-FAST via MainCHK.tulisLog
        MainCHK.tulisLog("Payload Response [" + url + "] ERROR: " + e.getMessage());
        throw new ApiClientException("Gagal memanggil API " + url + ": " + e.getMessage(), e);
        // 
    } finally {
        if (conn != null) {
            conn.disconnect();
        }
    }
}


private String readBody(InputStream is ) throws IOException{
    if (is == null){
        return null;
    }
    StringBuilder sb = new StringBuilder();
    try(BufferedReader br = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))){
      String line;
      while ((line = br.readLine()) != null){
          sb.append(line);
      }
    } 
    return sb.toString();
}
    
}
