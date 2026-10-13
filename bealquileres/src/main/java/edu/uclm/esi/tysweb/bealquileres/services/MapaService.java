package edu.uclm.esi.tysweb.bealquileres.services;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoSimulador;

@Service 
public class MapaService {
    @Autowired 
    private SimuladorClient simuladorClient;
    public List<VehiculoSimulador> getVehiculos(String city) {
       
        return this.simuladorClient.getVehiculos(city);
    }
}
