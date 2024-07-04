package com.bsi.service;

import com.bsi.MainCHK;
import com.bsi.entity.DefaultResponse;
import com.bsi.entity.PostingRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RestClient {
    private final String REST_URI;

    private final Client client = ClientBuilder.newClient();

    public RestClient(String REST_URI) {
        this.REST_URI = REST_URI;
    }

    public static void main(String[] args) {
//        RestClient restClient = new RestClient("http://localhost:8088/api/v1/affiliatedsupplier");
//        List<String> namafile = new ArrayList<>();
//        namafile.add("asd.txt");
//        Response response = restClient.processPosting(null);
//        String responseString = response.readEntity(String.class);
//        JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
//        System.out.println(responseString);
//        System.out.println(jsonObject.get("message"));
    }

    public DefaultResponse getJson(int id) {
        return client
                .target(REST_URI)
                .path(String.valueOf(id))
                .request(MediaType.APPLICATION_JSON)
                .get(DefaultResponse.class);
    }

    public void processPosting(PostingRequest postingRequest) {
        MainCHK.tulisLog("request URL: " + REST_URI + "/process/posting");
        Gson gson = new Gson();
        String jsonPayload = gson.toJson(postingRequest);
        try {
            URL url = new URL(REST_URI + "/process/posting");
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

//            String rawData = getString(postingRequest);
//            String type = "application/json";
//            String encodedData = URLEncoder.encode(rawData, "UTF-8");
//            URL url = new URL("http://www.example.com/page.php");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setDoOutput(true);
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", type);
//            conn.setRequestProperty("Content-Length", String.valueOf(encodedData.length()));
//            OutputStream outputStream = conn.getOutputStream();
//            outputStream.write(encodedData.getBytes());
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
//        String rawData = "referenceNumber=" + postingRequest.getReferenceNumber();
//        rawData += "&documentNumber=" + postingRequest.getDocumentNumber();
//        rawData += "&documentDate=" + postingRequest.getDocumentDate();
//        rawData += "&beneficiaryAccount=" + postingRequest.getBeneficiaryAccount();
//        rawData += "&amount=" + postingRequest.getAmount();
//        rawData += "&agentBankAccountNumber=" + postingRequest.getAgentBankAccountNumber();
//        rawData += "&applicationAreaMessageIdentifier=" + postingRequest.getApplicationAreaMessageIdentifier();
        return obj.toString();
    }

    public Response processPosting2(PostingRequest postingRequest) {
        MainCHK.tulisLog("request URL: " + REST_URI + "/process/posting");
        return client
                .target(REST_URI + "/process/posting")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(postingRequest, MediaType.APPLICATION_JSON));
    }

    public Response getMasterValidasi(String kodeReferal) {
        MainCHK.tulisLog("request URL: " + REST_URI + "/master-validasi-file?kodeReferal=" + kodeReferal);
        return client
                .target(REST_URI + "/master-validasi-file?kodeReferal=" + kodeReferal)
                .request(MediaType.APPLICATION_JSON)
                .get();
    }
}
