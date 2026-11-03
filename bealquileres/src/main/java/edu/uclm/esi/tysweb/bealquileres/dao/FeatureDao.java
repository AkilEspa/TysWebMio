package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.tysweb.bealquileres.model.Feature;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;

public interface FeatureDao extends JpaRepository<Feature, String> {
    Feature findByMunicipioAndNameAndNumber(Municipio municipio, String nombre, String numero);

    List<Feature> findAllByMunicipioOrderByName(Municipio municipio);

    List<Feature> findAllByMunicipioAndCapacidadIsNotNullOrderByNameAsc(Municipio municipio);
}