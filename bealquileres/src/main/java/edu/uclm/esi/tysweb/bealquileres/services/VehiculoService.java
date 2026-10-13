package edu.uclm.esi.tysweb.bealquileres.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.uclm.esi.tysweb.bealquileres.dao.MunicipioDao;
import edu.uclm.esi.tysweb.bealquileres.dao.VehiculoDao;
import edu.uclm.esi.tysweb.bealquileres.dto.VehiculoDto;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

@Service 
public class VehiculoService {

    @Autowired 
    private VehiculoDao dao;

    @Autowired 
    private MunicipioDao municipioDao;

    public void createVehicles(Integer numberOfVehicles) {
        List<Vehiculo> vehiculos = new ArrayList<>();
        for (int i=0; i<numberOfVehicles; i++)
            vehiculos.add(new Vehiculo());
        this.dao.saveAll(vehiculos);
    }

    public List<VehiculoDto> getVehiculos() {
        return this.dao.findAll().stream()
            .map(v -> new VehiculoDto(v.getId(), v.getMatricula(),
                v.getLatitud(), v.getLongitud(), v.getDistanciaTotal(), v.getBateria(),
                v.getActivo(), v.getMunicipio()!=null ? v.getMunicipio().getName() : null
                )).toList();
    }
    public synchronized void asignarVehiculos(Integer cantidad, String municipio) {
        Municipio m = this.municipioDao.findByName(municipio);
        if (m == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Municipio no encontrado: " + municipio);

        
        List<Vehiculo> vehiculosDisponibles = this.dao.getVehiculosDisponibles(cantidad);
        if(vehiculosDisponibles.size() < cantidad)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay suficientes vehículos disponibles. Se han asignado " + vehiculosDisponibles.size() + " vehículos.");
        for(Vehiculo v : vehiculosDisponibles) {
            v.setMunicipio(m);
        }
        this.dao.saveAll(vehiculosDisponibles);
    }

}
