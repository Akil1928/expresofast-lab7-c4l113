package cr.ac.ucr.paraiso.ie.c4l113_c4h741.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4l113_c4h741.expresofast.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Integer> {

    /**
     * Recupera todos los envios junto con Vehiculo, EmpresaLogistica y Conductor
     * en un unico viaje a la base de datos, evitando el fallo N+1 SELECT.
     */
    @Query("""
            SELECT e FROM Envio e
            JOIN FETCH e.vehiculo v
            JOIN FETCH v.empresa
            JOIN FETCH e.conductor
            """)
    List<Envio> findAllOptimizado();

    /**
     * Actualiza de forma masiva el estado de todos los envios
     * asociados a un vehiculo especifico.
     * clearAutomatically = true limpia el contexto de persistencia
     * para evitar datos desactualizados en cache tras el UPDATE masivo.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Envio e
            SET e.estadoEnvio = :estado
            WHERE e.vehiculo.id = :vehiculoId
            """)
    int actualizarEstadoPorVehiculo(@Param("vehiculoId") Integer vehiculoId,
                                     @Param("estado") String estado);
}
