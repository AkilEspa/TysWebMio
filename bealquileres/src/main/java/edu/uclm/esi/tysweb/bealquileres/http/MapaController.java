package edu.uclm.esi.tysweb.bealquileres.http;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoSimulador;
import edu.uclm.esi.tysweb.bealquileres.services.MapaService;

@RestController 
@RequestMapping ("/mapa")
@CrossOrigin ("*")
public class MapaController {
    @Autowired 
    private MapaService service;
@GetMapping ("/getVehiculos/{city}")
public List<VehiculoSimulador> getVehiculos(@PathVariable String city) {
    return this.service.getVehiculos(city);
    // Implementación para obtener vehículos en la ciudad especificada
}
}