package edu.uclm.esi.tysweb.bealquileres.services;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoSimulador;
@Service 
public class SimuladorClient {
    private final RestClient restClient;
    public SimuladorClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }
    public List<VehiculoSimulador> getVehiculos(String city) {
       
        List<VehiculoSimulador> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/simulador/getVehiculos/")
                    .pathSegment(city)
                    .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<VehiculoSimulador>>() {});
        if (response == null)
            return List.of();

        return response;

    }
}
