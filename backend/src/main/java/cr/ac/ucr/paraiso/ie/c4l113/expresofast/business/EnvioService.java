package cr.ac.ucr.paraiso.ie.c4l113.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4l113.expresofast.data.BitacoraEnvioRepository;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.data.ConductorRepository;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.data.EnvioRepository;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.data.UsuarioRepository;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.BitacoraEnvio;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.Conductor;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.Envio;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.Usuario;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.Vehiculo;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.BitacoraResponseDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.CambioEstadoDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioRequestDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioResponseDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.exception.InvalidStateTransitionException;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class EnvioService {

    private static final List<String> ESTADOS_VALIDOS =
            List.of("PENDIENTE", "EN_TRANSITO", "ENTREGADO", "CANCELADO");
    //tarifa: base + costo por kg + costo por km
    private static final double TARIFA_BASE = 500.0;
    private static final double TARIFA_POR_KG = 150.0;
    private static final double TARIFA_POR_KM = 200.0;

    // Estados finales: una vez alcanzados, no pueden volver a PENDIENTE ni EN_TRANSITO
    private static final Set<String> ESTADOS_FINALES = Set.of("ENTREGADO", "CANCELADO");
    private static final Set<String> ESTADOS_INICIALES = Set.of("PENDIENTE", "EN_TRANSITO");

    private final EnvioRepository envioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConductorRepository conductorRepository;
    private final BitacoraEnvioRepository bitacoraEnvioRepository;
    private final UsuarioRepository usuarioRepository;

    public EnvioService(EnvioRepository envioRepository,
                         VehiculoRepository vehiculoRepository,
                         ConductorRepository conductorRepository,
                         BitacoraEnvioRepository bitacoraEnvioRepository,
                         UsuarioRepository usuarioRepository) {
        this.envioRepository = envioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
        this.bitacoraEnvioRepository = bitacoraEnvioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<EnvioResponseDTO> listarOptimizado() {
        return envioRepository.findAllOptimizado().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public EnvioResponseDTO crear(EnvioRequestDTO request) {
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new NegocioException("El vehiculo indicado no existe."));

        Conductor conductor = conductorRepository.findById(request.getConductorId())
                .orElseThrow(() -> new NegocioException("El conductor indicado no existe."));

        // Regla de negocio: el peso del envio no puede superar la capacidad del vehiculo
        if (vehiculo.getCapacidadKg() == null
                || request.getPesoKg().compareTo(vehiculo.getCapacidadKg()) > 0) {
            throw new NegocioException(
                    "El peso del envio (" + request.getPesoKg() + " kg) supera la capacidad del vehiculo "
                            + vehiculo.getPlaca() + " (" + vehiculo.getCapacidadKg() + " kg).");
        }

        Envio envio = new Envio();
        envio.setCodigoRastreo(request.getCodigoRastreo());
        envio.setDireccionDestino(request.getDireccionDestino());
        envio.setPesoKg(request.getPesoKg());
        envio.setCosto(request.getCosto());
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);
        envio.setEstadoEnvio("PENDIENTE");

        Envio guardado = envioRepository.save(envio);
        return toResponseDTO(guardado);
    }

    /**
     * Actualiza el estado de un envio, valida la transicion, y registra
     * automaticamente el cambio en la bitacora de auditoria con el usuario autenticado.
     */
    @Transactional
    public EnvioResponseDTO actualizarEstado(Integer envioId, CambioEstadoDTO request) {
        String nuevoEstado = request.getNuevoEstado().toUpperCase();
        validarEstado(nuevoEstado);

        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new ResourceNotFoundException("El envio con id " + envioId + " no existe."));

        String estadoAnterior = envio.getEstadoEnvio();
        validarTransicion(estadoAnterior, nuevoEstado, envio.getCodigoRastreo());

        Usuario usuarioActual = obtenerUsuarioAutenticado();

        envio.setEstadoEnvio(nuevoEstado);
        // Dirty Checking actualiza el Envio al finalizar la transaccion.

        BitacoraEnvio bitacora = new BitacoraEnvio();
        bitacora.setEnvio(envio);
        bitacora.setEstadoAnterior(estadoAnterior);
        bitacora.setEstadoNuevo(nuevoEstado);
        bitacora.setUsuario(usuarioActual);
        bitacora.setObservaciones(request.getObservaciones());
        bitacoraEnvioRepository.save(bitacora);

        return toResponseDTO(envio);
    }

    @Transactional
    public int actualizarEstadoMasivoPorVehiculo(Integer vehiculoId, String nuevoEstado) {
        validarEstado(nuevoEstado);

        if (!vehiculoRepository.existsById(vehiculoId)) {
            throw new NegocioException("El vehiculo con id " + vehiculoId + " no existe.");
        }

        return envioRepository.actualizarEstadoPorVehiculo(vehiculoId, nuevoEstado);
    }

    @Transactional(readOnly = true)
    public List<BitacoraResponseDTO> obtenerBitacora(Integer envioId) {
        if (!envioRepository.existsById(envioId)) {
            throw new ResourceNotFoundException("El envio con id " + envioId + " no existe.");
        }

        return bitacoraEnvioRepository.findByEnvioIdOrderByFechaCambioDesc(envioId).stream()
                .map(b -> new BitacoraResponseDTO(
                        b.getId(),
                        b.getEstadoAnterior(),
                        b.getEstadoNuevo(),
                        b.getFechaCambio(),
                        b.getUsuario().getNombreCompleto(),
                        b.getObservaciones()
                ))
                .toList();
    }

    private void validarEstado(String estado) {
        if (estado == null || !ESTADOS_VALIDOS.contains(estado.toUpperCase())) {
            throw new NegocioException("Estado de envio invalido: " + estado
                    + ". Valores permitidos: " + ESTADOS_VALIDOS);
        }
    }

    /**
     * Reto autonomo: un envio en estado final (ENTREGADO o CANCELADO)
     * no puede volver a un estado inicial (PENDIENTE o EN_TRANSITO).
     */
    private void validarTransicion(String estadoActual, String nuevoEstado, String codigoRastreo) {
        if (ESTADOS_FINALES.contains(estadoActual) && ESTADOS_INICIALES.contains(nuevoEstado)) {
            throw new InvalidStateTransitionException(
                    "Transicion de estado no permitida para el envio " + codigoRastreo);
        }
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    private EnvioResponseDTO toResponseDTO(Envio envio) {
        return new EnvioResponseDTO(
                envio.getId(),
                envio.getCodigoRastreo(),
                envio.getDireccionDestino(),
                envio.getPesoKg(),
                envio.getCosto(),
                envio.getEstadoEnvio(),
                envio.getVehiculo() != null ? envio.getVehiculo().getPlaca() : null,
                envio.getConductor() != null
                        ? envio.getConductor().getNombre() + " " + envio.getConductor().getApellidos()
                        : null
        );
    }
   
     //Cancela un envio. Un envio EN_TRANSITO no se puede cancelar directamente
     //(debe completarse la entrega o el conductor debe reportar una incidencia).
    @Transactional
    public EnvioResponseDTO cancelarEnvio(Integer envioId) {
        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new ResourceNotFoundException("El envio con id " + envioId + " no existe."));

        if ("EN_TRANSITO".equals(envio.getEstadoEnvio())) {
            throw new NegocioException(
                    "No se puede cancelar el envio " + envio.getCodigoRastreo()
                            + " porque ya esta en transito. Contacte al conductor asignado.");
        }

        String estadoAnterior = envio.getEstadoEnvio();
        validarTransicion(estadoAnterior, "CANCELADO", envio.getCodigoRastreo());

        Usuario usuarioActual = obtenerUsuarioAutenticado();

        envio.setEstadoEnvio("CANCELADO");

        BitacoraEnvio bitacora = new BitacoraEnvio();
        bitacora.setEnvio(envio);
        bitacora.setEstadoAnterior(estadoAnterior);
        bitacora.setEstadoNuevo("CANCELADO");
        bitacora.setUsuario(usuarioActual);
        bitacora.setObservaciones("Cancelado por el usuario.");
        bitacoraEnvioRepository.save(bitacora);

        return toResponseDTO(envio);
    }
      //calcula la tarifa de un envio basado en el peso y la distancia a recorrer
     //formula: tarifa = (pesoKg * TARIFA_POR_KG) + (distanciaKm * TARIFA_POR_KM) + TARIFA_BASE
  
    public double calcularTarifa(double pesoKg, double distanciaKm) {
        if (pesoKg <= 0) {
            throw new NegocioException("El peso debe ser mayor a cero para calcular la tarifa.");
        }
        if (distanciaKm < 0) {
            throw new NegocioException("La distancia no puede ser negativa.");
        }
        return (pesoKg * TARIFA_POR_KG) + (distanciaKm * TARIFA_POR_KM) + TARIFA_BASE;
    }
}