package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

public interface VehiculoDao extends JpaRepository<Vehiculo, String> {

    Integer countByMunicipioIsNull();

    @Query (value="""
            select * from vehiculo v where v.municipio_id is null limit :cantidad
            """, nativeQuery=true)
    List<Vehiculo> getVehiculosDisponibles(Integer cantidad);

}