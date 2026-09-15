package cr.ac.ucr.paraiso.ie.c4h741.expresofast.controller;

import cr.ac.ucr.paraiso.ie.c4h741.expresofast.business.EnvioService;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.business.NegocioException;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Envio;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/envios")
@CrossOrigin(origins = "*")
public class EnvioController {

    private final EnvioService envioService;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    /**
     * GET /api/envios/optimizados
     * Retorna los envios cargados con JOIN FETCH (Vehiculo, EmpresaLogistica, Conductor)
     * en un unico viaje a la base de datos.
     */
    @GetMapping("/optimizados")
    public ResponseEntity<List<Envio>> listarOptimizados() {
        return ResponseEntity.ok(envioService.listarOptimizado());
    }

    /**
     * POST /api/envios
     * Registra un nuevo envio express.
     */
    @PostMapping
    public ResponseEntity<Envio> crear(@RequestBody Envio envio) {
        Envio creado = envioService.crear(envio);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * PATCH /api/envios/{id}/estado
     * Actualiza el estado del envio aprovechando Dirty Checking.
     * Cuerpo esperado: { "estado": "EN_TRANSITO" }
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<Envio> actualizarEstado(@PathVariable Integer id,
                                                   @Valid @RequestBody EstadoEnvioRequest request) {
        Envio actualizado = envioService.actualizarEstado(id, request.getEstado().toUpperCase());
        return ResponseEntity.ok(actualizado);
    }

    /**
     * PATCH /api/envios/vehiculo/{vehiculoId}/estado
     * Actualizacion masiva de estado para todos los envios de un vehiculo.
     */
    @PatchMapping("/vehiculo/{vehiculoId}/estado")
    public ResponseEntity<Map<String, Object>> actualizarEstadoMasivo(@PathVariable Integer vehiculoId,
                                                                       @Valid @RequestBody EstadoEnvioRequest request) {
        int filasActualizadas = envioService.actualizarEstadoMasivoPorVehiculo(
                vehiculoId, request.getEstado().toUpperCase());
        return ResponseEntity.ok(Map.of("filasActualizadas", filasActualizadas));
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, String>> manejarNegocioException(NegocioException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
