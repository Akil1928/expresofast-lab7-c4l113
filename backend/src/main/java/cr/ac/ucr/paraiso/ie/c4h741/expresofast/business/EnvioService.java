package cr.ac.ucr.paraiso.ie.c4h741.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.ConductorRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.EnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Conductor;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Envio;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Vehiculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnvioService {

    private static final List<String> ESTADOS_VALIDOS =
            List.of("PENDIENTE", "EN_TRANSITO", "ENTREGADO", "CANCELADO");

    private final EnvioRepository envioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConductorRepository conductorRepository;

    // Inyeccion de dependencias por constructor
    public EnvioService(EnvioRepository envioRepository,
                         VehiculoRepository vehiculoRepository,
                         ConductorRepository conductorRepository) {
        this.envioRepository = envioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
    }

    @Transactional(readOnly = true)
    public List<Envio> listarOptimizado() {
        return envioRepository.findAllOptimizado();
    }

    @Transactional
    public Envio crear(Envio envio) {
        if (envio.getVehiculo() == null || envio.getVehiculo().getId() == null) {
            throw new NegocioException("Debe indicar el vehiculo asignado al envio.");
        }
        if (envio.getConductor() == null || envio.getConductor().getId() == null) {
            throw new NegocioException("Debe indicar el conductor asignado al envio.");
        }

        Vehiculo vehiculo = vehiculoRepository.findById(envio.getVehiculo().getId())
                .orElseThrow(() -> new NegocioException("El vehiculo indicado no existe."));

        Conductor conductor = conductorRepository.findById(envio.getConductor().getId())
                .orElseThrow(() -> new NegocioException("El conductor indicado no existe."));

        // Regla de negocio: el peso del envio no puede superar la capacidad del vehiculo
        if (envio.getPesoKg() == null || vehiculo.getCapacidadKg() == null
                || envio.getPesoKg().compareTo(vehiculo.getCapacidadKg()) > 0) {
            throw new NegocioException(
                    "El peso del envio (" + envio.getPesoKg() + " kg) supera la capacidad del vehiculo "
                            + vehiculo.getPlaca() + " (" + vehiculo.getCapacidadKg() + " kg).");
        }

        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);

        if (envio.getEstadoEnvio() == null || envio.getEstadoEnvio().isBlank()) {
            envio.setEstadoEnvio("PENDIENTE");
        } else {
            validarEstado(envio.getEstadoEnvio());
        }

        return envioRepository.save(envio);
    }

    /**
     * Actualiza el estado de un envio aprovechando el Dirty Checking de JPA:
     * al modificar la entidad gestionada dentro de la transaccion,
     * Hibernate detecta el cambio y genera el UPDATE automaticamente al hacer commit.
     */
    @Transactional
    public Envio actualizarEstado(Integer envioId, String nuevoEstado) {
        validarEstado(nuevoEstado);

        Envio envio = envioRepository.findById(envioId)
                .orElseThrow(() -> new NegocioException("El envio con id " + envioId + " no existe."));

        envio.setEstadoEnvio(nuevoEstado);
        // No es necesario llamar a save(): Dirty Checking actualiza al finalizar la transaccion.
        return envio;
    }

    @Transactional
    public int actualizarEstadoMasivoPorVehiculo(Integer vehiculoId, String nuevoEstado) {
        validarEstado(nuevoEstado);

        if (!vehiculoRepository.existsById(vehiculoId)) {
            throw new NegocioException("El vehiculo con id " + vehiculoId + " no existe.");
        }

        return envioRepository.actualizarEstadoPorVehiculo(vehiculoId, nuevoEstado);
    }

    private void validarEstado(String estado) {
        if (estado == null || !ESTADOS_VALIDOS.contains(estado.toUpperCase())) {
            throw new NegocioException("Estado de envio invalido: " + estado
                    + ". Valores permitidos: " + ESTADOS_VALIDOS);
        }
    }
}
