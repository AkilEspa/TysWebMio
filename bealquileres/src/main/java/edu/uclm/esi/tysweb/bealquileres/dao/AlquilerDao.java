package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uclm.esi.tysweb.bealquileres.model.Alquiler;

public interface AlquilerDao extends JpaRepository<Alquiler, Long> {
    boolean existsByPaymentIntentId(String paymentIntentId);

    List<Alquiler> findByEmailUsuarioOrderByHoraInicioDesc(String emailUsuario);
}
