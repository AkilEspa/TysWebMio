package edu.uclm.esi.tysweb.bealquileres.http;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.services.VehiculoService;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/vehiculos")
@Tag(
    name = "Vehículos",
    description = "Operaciones relacionadas con la gestión de vehículos"
)
public class VehiculoController {

    @Autowired 
    private VehiculoService service;

    @PostMapping("/createVehicles")
    public void createVehicles(@RequestBody Integer numberOfVehicles) {
        if (numberOfVehicles>1000) 
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Demasiados vehículos. Se admiten hasta 5");

        this.service.createVehicles(numberOfVehicles);
    }

    @GetMapping("/getVehiculos")
    private List<VehiculoDto> getVehiculos() {
        return this.service.getVehiculos();
    }
    
    @PostMapping ("/asignarVehiculos")
    public void asignarVehiculos(@RequestBody AsignarVehiculoRequestDto request) {

        if(request.cantidad() < 1 || request.cantidad()>100) 
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demasiados vehículos. Se admiten entre 1 y 100");

        this.service.asignarVehiculos(request.cantidad(), request.municipio());
    }
        
}