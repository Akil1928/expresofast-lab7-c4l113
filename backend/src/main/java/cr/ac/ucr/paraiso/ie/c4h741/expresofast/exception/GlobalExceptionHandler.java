package cr.ac.ucr.paraiso.ie.c4h741.expresofast.exception;

import cr.ac.ucr.paraiso.ie.c4h741.expresofast.business.NegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 - Errores de validacion de campos (@NotBlank, @Pattern, @Positive, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de validacion");
        body.put("detalles", errores);

        return ResponseEntity.badRequest().body(body);
    }

    // 400 - JSON mal formado o no legible en el body de la peticion
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es un JSON valido");
    }

    // 404 - Recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(ResourceNotFoundException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 400 - Reglas de negocio incumplidas (peso, vehiculo/conductor invalido, etc.)
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarNegocio(NegocioException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 - Transicion de estado invalida (reto autonomo)
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> manejarTransicionInvalida(InvalidStateTransitionException ex) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 401 - Credenciales incorrectas en login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> manejarCredencialesInvalidas(BadCredentialsException ex) {
        return construirRespuesta(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    // 403 - Usuario autenticado pero sin el rol requerido
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccessDeniedException ex) {
        return construirRespuesta(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta accion");
    }

    // 500 - Cualquier otro error inesperado (sin exponer el stacktrace)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorGenerico(Exception ex) {
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado en el servidor");
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}