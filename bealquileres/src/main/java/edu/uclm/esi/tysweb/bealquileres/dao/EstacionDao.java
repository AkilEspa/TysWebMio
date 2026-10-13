package edu.uclm.esi.tysweb.bealquileres.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.tysweb.bealquileres.model.Estacion;


public interface EstacionDao extends JpaRepository<Estacion, String> {


}