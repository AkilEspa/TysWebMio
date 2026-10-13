package edu.uclm.esi.tysweb.beusu.service;

import java.util.LinkedHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class IdeeClient {

    private final RestClient restClient;

    public IdeeClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api-features.idee.es")
                .build();
    }

    @SuppressWarnings("unchecked")
    public Boolean existe(String municipio) {
        try {
            LinkedHashMap<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/address/items")
                            .queryParam("component_AddressAreaName", municipio)
                            .queryParam("f", "json")
                            .queryParam("offset", 0)
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(LinkedHashMap.class);

            if (response == null)
                return false;

            Object oNumberMatched = response.get("numberMatched");
            if (oNumberMatched == null)
                return false;

            Integer numberMatched = Integer.parseInt(oNumberMatched.toString());

            return numberMatched > 0;

        } catch (Exception exception) {
            return false;
        }
    }
}