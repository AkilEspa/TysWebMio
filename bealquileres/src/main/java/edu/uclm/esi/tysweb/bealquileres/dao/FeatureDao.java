package edu.uclm.esi.tysweb.bealquileres.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.tysweb.bealquileres.model.Feature;
import edu.uclm.esi.tysweb.bealquileres.model.Municipio;

public interface FeatureDao extends JpaRepository<Feature, String> {
    Feature findByMunicipioAndNameAndNumber(Municipio municipio, String nombre, Integer numero);

    Feature findByMunicipioAndNameAndNumber(String municipio, String nombre, Integer numero);
}