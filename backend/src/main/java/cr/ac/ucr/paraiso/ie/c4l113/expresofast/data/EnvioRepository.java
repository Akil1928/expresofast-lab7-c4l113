package cr.ac.ucr.paraiso.ie.c4l113.expresofast.data;

import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.Procedure;
@Repository
public interface EnvioRepository extends JpaRepository<Envio, Integer> {

            /**
     * Invoca el procedimiento almacenado SP_OBTENER_ENVIOS_POR_ESTADO.
     */
    @Procedure(name = "Envio.obtenerEnviosPorEstado")
    List<Envio> obtenerEnviosPorEstado(@Param("pEstado") String pEstado);

    /**
     * Paginación relacional física por estado.
     */
    Page<Envio> findByEstadoEnvio(String estadoEnvio, Pageable pageable);
    /**
     * Busca un envio por su codigo de rastreo unico (usado por EnvioTrackingComponent en Angular).
     */
    Optional<Envio> findByCodigoRastreo(String codigoRastreo);
    /**
     * Paginación relacional con búsqueda por término (dirección o código de rastreo).
     */
    @Query("""
            SELECT e FROM Envio e
            WHERE (:busqueda IS NULL OR
                   LOWER(e.codigoRastreo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
                   LOWER(e.direccionDestino) LIKE LOWER(CONCAT('%', :busqueda, '%')))
            AND (:estado IS NULL OR e.estadoEnvio = :estado)
            """)
    Page<Envio> buscarPaginado(@Param("busqueda") String busqueda,
                                @Param("estado") String estado,
                                Pageable pageable);
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
