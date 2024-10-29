package com.bsi.service;

import com.bsi.MainCHK;
import com.bsi.entity.DefaultResponse;
import com.bsi.entity.PostingRequest;
import com.bsi.entity.ResponseToken;
import com.bsi.entity.TokenRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestClient {
    private final String REST_URI;
    private final String URL_POSTING;
    private final String URL_VALIDASI;
    private final String URL_TOKEN;
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final Client client = ClientBuilder.newClient();
    private String TOKEN_MAGIC;

    public RestClient(String REST_URI, String clientId, String clientSecret) {
        this.REST_URI = REST_URI;
        this.CLIENT_ID = clientId;
        this.CLIENT_SECRET = clientSecret;
        URL_POSTING = "/magic-affiliated-supplier/api/v1/process/posting";
        URL_VALIDASI = "/magic-affiliated-supplier/api/v1/master-validasi-file?kodeReferal=";
        URL_TOKEN = "/magic-security/auth/get-token";
        TOKEN_MAGIC = "-";
    }

    public static void main(String[] args) {
        RestClient restClient = new RestClient("http://10.0.117.100:8082", "span-auth", "5tQS8bqHa5zd4FjVle68eDMbbLsEWD20");
        Response response = restClient.getMasterValidasi("BSIRST20241024140040");
        String responseString = response.readEntity(String.class);
        JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
        MainCHK.tulisLog("response: " + jsonObject.toString());
        restClient.getMasterValidasi("BSIRST20241024140040");
//new TokenRequest("span-auth", "5tQS8bqHa5zd4FjVle68eDMbbLsEWD20")
    }

    public DefaultResponse getJson(int id) {
        return client
                .target(REST_URI)
                .path(String.valueOf(id))
                .request(MediaType.APPLICATION_JSON)
                .get(DefaultResponse.class);
    }

    public int processPosting(PostingRequest postingRequest) {
        if (TOKEN_MAGIC.equals("-")) {
            getMagicToken();
        }
        MainCHK.tulisLog("request URL: " + REST_URI + URL_POSTING);
        Gson gson = new Gson();
        String jsonPayload = gson.toJson(postingRequest);
        try {
            URL url = new URL(REST_URI + URL_POSTING);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + TOKEN_MAGIC);
            conn.setDoOutput(true);
            // Write the JSON payload to the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // Get the response
            int responseCode = conn.getResponseCode();
            MainCHK.tulisLog("Response Code: " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            MainCHK.tulisLog("Response: " + response.toString());
            return responseCode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getString(PostingRequest postingRequest) {
        JsonObject obj = new JsonObject();
        obj.addProperty("referenceNumber", postingRequest.getReferenceNumber());
        obj.addProperty("documentNumber", postingRequest.getDocumentNumber());
        obj.addProperty("documentDate", postingRequest.getDocumentDate());
        obj.addProperty("beneficiaryAccount", postingRequest.getBeneficiaryAccount());
        obj.addProperty("amount", postingRequest.getAmount());
        obj.addProperty("agentBankAccountNumber", postingRequest.getAgentBankAccountNumber());
        obj.addProperty("applicationAreaMessageIdentifier", postingRequest.getApplicationAreaMessageIdentifier());
        return obj.toString();
    }

    public Response getMasterValidasi(String kodeReferal) {
        if (TOKEN_MAGIC.equals("-")) {
            getMagicToken();
        }
        MainCHK.tulisLog("request URL: " + REST_URI + URL_VALIDASI + kodeReferal);
        return client
                .target(REST_URI + URL_VALIDASI + kodeReferal)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TOKEN_MAGIC)
                .get();
    }

    public void getMagicToken() {
        MainCHK.tulisLog("request URL: " + REST_URI + URL_TOKEN);
        Gson gson = new Gson();
        String jsonPayload = gson.toJson(new TokenRequest(CLIENT_ID, CLIENT_SECRET));
        MainCHK.tulisLog("Response: " + jsonPayload);
        try {
            URL url = new URL(REST_URI + URL_TOKEN);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            // Write the JSON payload to the request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // Get the response
            int responseCode = conn.getResponseCode();
            MainCHK.tulisLog("Response Code: " + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            MainCHK.tulisLog("Response: " + response.toString());
            ResponseToken responseToken = gson.fromJson(response.toString(), ResponseToken.class);
            MainCHK.tulisLog("getStatusCode: " + responseToken.getStatusCode());
            if (responseToken.getStatusCodeValue() == 200) {
                TOKEN_MAGIC = responseToken.getBody().getAccessToken();
                MainCHK.tulisLog("getAccessToken: " + responseToken.getBody().getAccessToken());
                return;
            }
            TOKEN_MAGIC = "-";
            throw new RuntimeException("Failed to get token");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
