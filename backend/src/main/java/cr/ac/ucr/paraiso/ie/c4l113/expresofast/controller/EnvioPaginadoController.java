package cr.ac.ucr.paraiso.ie.c4l113.expresofast.controller;

import cr.ac.ucr.paraiso.ie.c4l113.expresofast.business.EnvioService;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioDTO;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioRequestDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioResponseDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.CambioEstadoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioRegistroDTO;
import java.util.Map;
import java.util.List;

/**
 * Controlador dedicado a la paginación relacional y a la exposición
 * del Stored Procedure SP_OBTENER_ENVIOS_POR_ESTADO (Laboratorio 9).
 * Se mantiene separado de EnvioController (/api/envios) para no alterar
 * las rutas ya consumidas por el cliente web de los laboratorios previos.
 */



@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/envios")
public class EnvioPaginadoController {

    private final EnvioService envioService;

    public EnvioPaginadoController(EnvioService envioService) {
        this.envioService = envioService;
    }

    /**
     * GET /api/v1/envios?page=0&size=5&sortBy=fechaCreacion&direction=desc&busqueda=...&estado=...
     */
    @GetMapping
    public ResponseEntity<Page<EnvioDTO>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado) {

        Page<EnvioDTO> resultado = envioService.listarPaginado(page, size, sortBy, direction, busqueda, estado);
        return ResponseEntity.ok(resultado);
    }
        /**
     * GET /api/v1/envios/check-tracking/{numeroTracking}
     * Validador asincrono de Angular: indica si el numero de rastreo ya existe.
     */
    @GetMapping("/check-tracking/{numeroTracking}")
    public ResponseEntity<Map<String, Boolean>> verificarTracking(@PathVariable String numeroTracking) {
        boolean existe = envioService.existeTracking(numeroTracking);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    /**
     * POST /api/v1/envios/avanzado
     * Registra un envio junto con sus paquetes asociados (Lab 11).
     */
    @PostMapping("/avanzado")
    public ResponseEntity<EnvioDTO> registrarAvanzado(@Valid @RequestBody EnvioRegistroDTO request) {
        EnvioDTO creado = envioService.registrarEnvioAvanzado(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * GET /api/v1/envios/procedimiento/{estado}
     */
    @GetMapping("/procedimiento/{estado}")
    public ResponseEntity<List<EnvioDTO>> listarViaStoredProcedure(@PathVariable String estado) {
        List<EnvioDTO> resultado = envioService.listarViaStoredProcedure(estado);
        return ResponseEntity.ok(resultado);
    }

        /**
     * GET /api/v1/envios/todos — listado completo sin paginar (Angular EnvioListComponent).
     */
    @GetMapping("/todos")
    public ResponseEntity<List<EnvioDTO>> listarTodos() {
        return ResponseEntity.ok(envioService.listarTodos());
    }

    /**
     * GET /api/v1/envios/rastreo/{codigo} — Angular EnvioTrackingComponent.
     */
    @GetMapping("/rastreo/{codigo}")
    public ResponseEntity<EnvioDTO> buscarPorRastreo(@PathVariable String codigo) {
        return ResponseEntity.ok(envioService.buscarPorCodigoRastreo(codigo));
    }

    /**
     * POST /api/v1/envios — registro de nuevo envio desde Angular EnvioFormComponent.
     */
    @PostMapping
    public ResponseEntity<EnvioResponseDTO> crear(@Valid @RequestBody EnvioRequestDTO request) {
        EnvioResponseDTO creado = envioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * PATCH /api/v1/envios/{id}/estado — actualizacion de estado desde Angular EnvioListComponent.
     */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<EnvioDTO> actualizarEstado(@PathVariable Integer id,
                                                       @Valid @RequestBody CambioEstadoDTO request) {
        EnvioDTO actualizado = envioService.actualizarEstadoSimple(id, request.getNuevoEstado());
        return ResponseEntity.ok(actualizado);
    }
}