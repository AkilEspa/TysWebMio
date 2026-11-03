package edu.uclm.esi.tysweb.bealquileres.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import edu.uclm.esi.tysweb.bealquileres.model.Vehiculo;

public interface VehiculoDao extends JpaRepository<Vehiculo, Long> {

    Integer countByMunicipioIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query (value="""
            select * from vehiculo v where v.municipio_id is null limit :cantidad
            """, nativeQuery=true)
    List<Vehiculo> getVehiculosDisponibles(Integer cantidad);

    List<Vehiculo> findByPaymentIntentId(String paymentIntentId);

    @Query("""
            select v from Vehiculo v
            where v.activo = true
              and v.municipio is not null
              and not exists (
                select a.id from Alquiler a
                where a.vehiculo = v and a.horaFin > :now
              )
            """)
    List<Vehiculo> findVehiculosAlquilables(@Param("now") LocalDateTime now);

}