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
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.domain.Paquete;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioRegistroDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.PaqueteDTO;
import java.math.BigDecimal;

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

        /**
     * Registra un envio junto con todos sus paquetes asociados en una sola
     * transaccion (Lab 11). El peso total del envio se calcula como la suma
     * de los pesos de cada paquete, respetando la regla de capacidad del vehiculo.
     */
    @Transactional
    public EnvioDTO registrarEnvioAvanzado(EnvioRegistroDTO request) {
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new NegocioException("El vehiculo indicado no existe."));

        Conductor conductor = conductorRepository.findById(request.getConductorId())
                .orElseThrow(() -> new NegocioException("El conductor indicado no existe."));

        if (envioRepository.existsByCodigoRastreo(request.getNumeroTracking())) {
            throw new NegocioException(
                    "El numero de rastreo " + request.getNumeroTracking() + " ya esta en uso.");
        }

        if (!request.getFechaEntregaEstimada().isAfter(request.getFechaDespacho())) {
            throw new NegocioException(
                    "La fecha de entrega estimada debe ser posterior a la fecha de despacho.");
        }

        BigDecimal pesoTotal = request.getPaquetes().stream()
                .map(PaqueteDTO::getPesoKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (vehiculo.getCapacidadKg() == null || pesoTotal.compareTo(vehiculo.getCapacidadKg()) > 0) {
            throw new NegocioException(
                    "El peso total de los paquetes (" + pesoTotal + " kg) supera la capacidad del vehiculo "
                            + vehiculo.getPlaca() + " (" + vehiculo.getCapacidadKg() + " kg).");
        }

        Envio envio = new Envio();
        envio.setCodigoRastreo(request.getNumeroTracking());
        envio.setDireccionDestino(request.getDireccionDestino());
        envio.setPesoKg(pesoTotal);
        envio.setCosto(request.getCosto());
        envio.setEstadoEnvio("PENDIENTE");
        envio.setFechaDespacho(request.getFechaDespacho());
        envio.setFechaEntregaEstimada(request.getFechaEntregaEstimada());
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);

        for (PaqueteDTO paqueteDTO : request.getPaquetes()) {
            Paquete paquete = new Paquete();
            paquete.setDescripcion(paqueteDTO.getDescripcion());
            paquete.setPesoKg(paqueteDTO.getPesoKg());
            envio.agregarPaquete(paquete);
        }

        Envio guardado = envioRepository.save(envio);
        // cascade = CascadeType.ALL en Envio.paquetes persiste los paquetes automaticamente.

        return toEnvioDTO(guardado);
    }

    /**
     * Verifica si un numero de rastreo ya existe (Lab 11, validador asincrono de Angular).
     */
    @Transactional(readOnly = true)
    public boolean existeTracking(String numeroTracking) {
        return envioRepository.existsByCodigoRastreo(numeroTracking);
    }

        /**
     * Paginación relacional física a nivel de SQL. Permite filtrar por
     * término de búsqueda (código de rastreo o dirección) y/o por estado,
     * ordenando dinámicamente según sortBy/dir.
     */
    @Transactional(readOnly = true)
    public Page<EnvioDTO> listarPaginado(int page, int size, String sortBy, String dir,
                                          String busqueda, String estado) {
        String campoOrden = (sortBy == null || sortBy.isBlank()) ? "fechaCreacion" : sortBy;
        Sort.Direction direccion = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direccion, campoOrden));

        String busquedaNormalizada = (busqueda == null || busqueda.isBlank()) ? null : busqueda.trim();
        String estadoNormalizado = (estado == null || estado.isBlank()) ? null : estado.toUpperCase();

        Page<Envio> resultado = envioRepository.buscarPaginado(busquedaNormalizada, estadoNormalizado, pageable);
        return resultado.map(this::toEnvioDTO);
    }

    /**
     * Invoca el procedimiento almacenado relacional SP_OBTENER_ENVIOS_POR_ESTADO.
     */
    @Transactional(readOnly = true)
    public List<EnvioDTO> listarViaStoredProcedure(String estado) {
        validarEstado(estado);
        return envioRepository.obtenerEnviosPorEstado(estado.toUpperCase()).stream()
                .map(this::toEnvioDTO)
                .toList();
    }
        /**
     * Listado completo sin paginar, consumido por EnvioListComponent en Angular.
     */
    @Transactional(readOnly = true)
    public List<EnvioDTO> listarTodos() {
        return envioRepository.findAll().stream()
                .map(this::toEnvioDTO)
                .toList();
    }

    /**
     * Busqueda por codigo de rastreo, consumida por EnvioTrackingComponent.
     */
    @Transactional(readOnly = true)
    public EnvioDTO buscarPorCodigoRastreo(String codigoRastreo) {
        Envio envio = envioRepository.findByCodigoRastreo(codigoRastreo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un envio con el codigo de rastreo " + codigoRastreo));
        return toEnvioDTO(envio);
    }

    /**
     * Actualizacion de estado simplificada para el cliente Angular (Lab 10),
     * que no maneja JWT/usuario autenticado. No registra bitacora de auditoria
     * (esa funcionalidad se conserva intacta para el cliente Vanilla JS del Lab 6-9).
     */
    @Transactional
    public EnvioDTO actualizarEstadoSimple(Integer envioId, String nuevoEstado) {
        String estado = nuevoEstado.toUpperCase();
        validarEstado(estado);

        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new ResourceNotFoundException("El envio con id " + envioId + " no existe."));

        validarTransicion(envio.getEstadoEnvio(), estado, envio.getCodigoRastreo());
        envio.setEstadoEnvio(estado);
        // Dirty Checking persiste el cambio al finalizar la transaccion.

        return toEnvioDTO(envio);
    }

    private EnvioDTO toEnvioDTO(Envio envio) {
        return new EnvioDTO(
                envio.getId(),
                envio.getCodigoRastreo(),
                envio.getDireccionDestino(),
                envio.getCosto(),
                envio.getEstadoEnvio(),
                envio.getFechaCreacion()
        );
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