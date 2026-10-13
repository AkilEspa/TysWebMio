package edu.uclm.esi.tysweb.simulador.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uclm.esi.tysweb.simulador.model.Feature;

public interface FeatureDao extends JpaRepository<Feature, String> {

    @Query(
        value = """
            SELECT *
            FROM feature
            WHERE municipio_id = :municipioId
            ORDER BY RAND()
            LIMIT 1
            """,
        nativeQuery = true
    )
    Feature findRandomByMunicipio(@Param("municipioId") String municipioId);

    @Query("""
        SELECT AVG(f.latitude), AVG(f.longitude)
        FROM Feature f
        WHERE f.municipio.id = :municipioId
        """)
    Object[] findCenterByMunicipioId(@Param("municipioId") String municipioId);
}
