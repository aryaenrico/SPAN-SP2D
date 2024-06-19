package com.bsi.service;

import com.bsi.entity.DefaultRequest;
import com.bsi.entity.DefaultResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestClient {
    private static final String REST_URI = "http://localhost:8088/api/v1/affiliatedsupplier";

    private final Client client = ClientBuilder.newClient();

    public RestClient() {
    }

    public static void main(String[] args) {
        RestClient restClient = new RestClient();
        List<String> namafile = new ArrayList<>();
        namafile.add("asd.txt");
        Response response = restClient.processPosting("");
        String responseString = response.readEntity(String.class);
        JsonObject jsonObject = JsonParser.parseString(responseString).getAsJsonObject();
        System.out.println(responseString);
        System.out.println(jsonObject.get("message"));
    }

    public DefaultResponse getJson(int id) {
        return client
                .target(REST_URI)
                .path(String.valueOf(id))
                .request(MediaType.APPLICATION_JSON)
                .get(DefaultResponse.class);
    }

    public Response processPosting(String kodeReferal) {
        return client
                .target(REST_URI + "/process/posting?kodeReferal=" + kodeReferal)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(new DefaultRequest(Collections.singletonList(""), "", "user", "yyyy-MM-dd"), MediaType.APPLICATION_JSON));
    }

    public Response getMasterValidasi(String kodeReferal) {
        return client
                .target(REST_URI + "/master-validasi-file?kodeReferal=" + kodeReferal)
                .request(MediaType.APPLICATION_JSON)
                .get();
    }
}
