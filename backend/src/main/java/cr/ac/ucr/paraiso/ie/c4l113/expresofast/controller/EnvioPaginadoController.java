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

import java.util.List;

/**
 * Controlador dedicado a la paginación relacional y a la exposición
 * del Stored Procedure SP_OBTENER_ENVIOS_POR_ESTADO (Laboratorio 9).
 * Se mantiene separado de EnvioController (/api/envios) para no alterar
 * las rutas ya consumidas por el cliente web de los laboratorios previos.
 */
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
     * GET /api/v1/envios/procedimiento/{estado}
     */
    @GetMapping("/procedimiento/{estado}")
    public ResponseEntity<List<EnvioDTO>> listarViaStoredProcedure(@PathVariable String estado) {
        List<EnvioDTO> resultado = envioService.listarViaStoredProcedure(estado);
        return ResponseEntity.ok(resultado);
    }
}