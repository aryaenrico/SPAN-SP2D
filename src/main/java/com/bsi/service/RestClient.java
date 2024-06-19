package com.bsi.service;

import com.bsi.entity.DefaultResponse;
import com.bsi.entity.PostingRequest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

    public Response processPosting(PostingRequest postingRequest) {
        return client
                .target(REST_URI + "/process/posting")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(postingRequest, MediaType.APPLICATION_JSON));
    }

    public Response getMasterValidasi(String kodeReferal) {
        return client
                .target(REST_URI + "/master-validasi-file?kodeReferal=" + kodeReferal)
                .request(MediaType.APPLICATION_JSON)
                .get();
    }
}
