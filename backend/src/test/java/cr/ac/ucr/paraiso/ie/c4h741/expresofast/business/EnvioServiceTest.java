package cr.ac.ucr.paraiso.ie.c4h741.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.BitacoraEnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.ConductorRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.EnvioRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.UsuarioRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.data.VehiculoRepository;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Conductor;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Envio;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Vehiculo;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.dto.CambioEstadoDTO;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.dto.EnvioRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.dto.EnvioResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.exception.InvalidStateTransitionException;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


//Pruebas unitarias de EnvioService, aislando por completo la base de datos
//mediante mocks de Mockito sobre los repositorios (JPA).
@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private ConductorRepository conductorRepository;

    @Mock
    private BitacoraEnvioRepository bitacoraEnvioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EnvioService envioService;

    private Vehiculo vehiculo;
    private Conductor conductor;

    @BeforeEach
    void setUp() {
        vehiculo = new Vehiculo();
        vehiculo.setId(1);
        vehiculo.setPlaca("SJO-1234");
        vehiculo.setCapacidadKg(new BigDecimal("500"));

        conductor = new Conductor();
        conductor.setId(1);
        conductor.setNombre("Juan");
        conductor.setApellidos("Perez Solano");
    }

    @Test
    @DisplayName("crear: con datos validos retorna un EnvioResponseDTO en estado PENDIENTE")
    void crearEnvio_DatosValidos_RetornaEnvioDTO() {
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setCodigoRastreo("EXP-0099");
        request.setDireccionDestino("Paraiso, Cartago");
        request.setPesoKg(new BigDecimal("50"));
        request.setCosto(new BigDecimal("5000"));
        request.setVehiculoId(1);
        request.setConductorId(1);

        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> {
            Envio guardado = invocacion.getArgument(0);
            guardado.setId(10);
            return guardado;
        });

        EnvioResponseDTO resultado = envioService.crear(request);

        assertThat(resultado.getId()).isEqualTo(10);
        assertThat(resultado.getEstadoEnvio()).isEqualTo("PENDIENTE");
        assertThat(resultado.getCodigoRastreo()).isEqualTo("EXP-0099");
        assertThat(resultado.getPlacaVehiculo()).isEqualTo("SJO-1234");
        verify(envioRepository, times(1)).save(any(Envio.class));
    }

    @Test
    @DisplayName("crear: si el peso supera la capacidad del vehiculo lanza NegocioException")
    void crearEnvio_VehiculoSinCapacidad_LanzaExcepcion() {
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setCodigoRastreo("EXP-0099");
        request.setDireccionDestino("Paraiso, Cartago");
        request.setPesoKg(new BigDecimal("999")); // supera capacidadKg = 500
        request.setCosto(new BigDecimal("5000"));
        request.setVehiculoId(1);
        request.setConductorId(1);

        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(1)).thenReturn(Optional.of(conductor));

        assertThrows(NegocioException.class, () -> envioService.crear(request));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear: si el vehiculo indicado no existe lanza NegocioException")
    void crearEnvio_VehiculoInexistente_LanzaExcepcion() {
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setVehiculoId(99);
        request.setConductorId(1);
        request.setPesoKg(new BigDecimal("10"));

        when(vehiculoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NegocioException.class, () -> envioService.crear(request));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarEstado: un envio ENTREGADO no puede volver a EN_TRANSITO")
    void actualizarEstado_TransicionInvalida_LanzaExcepcion() {
        Envio envio = new Envio();
        envio.setId(3);
        envio.setCodigoRastreo("EXP-0003");
        envio.setEstadoEnvio("ENTREGADO");

        CambioEstadoDTO request = new CambioEstadoDTO();
        request.setNuevoEstado("EN_TRANSITO");

        when(envioRepository.findById(3)).thenReturn(Optional.of(envio));

        InvalidStateTransitionException excepcion = assertThrows(
                InvalidStateTransitionException.class,
                () -> envioService.actualizarEstado(3, request)
        );

        assertThat(excepcion.getMessage()).contains("EXP-0003");
        verify(bitacoraEnvioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarEstado: si el envio no existe lanza ResourceNotFoundException")
    void actualizarEstado_EnvioInexistente_LanzaExcepcion() {
        CambioEstadoDTO request = new CambioEstadoDTO();
        request.setNuevoEstado("EN_TRANSITO");

        when(envioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> envioService.actualizarEstado(999, request));
    }

    @Test
    @DisplayName("cancelarEnvio: un envio EN_TRANSITO no se puede cancelar")
    void cancelarEnvio_EnvioEnTransito_LanzaExcepcion() {
        Envio envio = new Envio();
        envio.setId(5);
        envio.setCodigoRastreo("EXP-0005");
        envio.setEstadoEnvio("EN_TRANSITO");

        when(envioRepository.findById(5)).thenReturn(Optional.of(envio));

        assertThrows(NegocioException.class, () -> envioService.cancelarEnvio(5));
        verify(envioRepository, never()).save(any());
        verify(bitacoraEnvioRepository, never()).save(any());
    }
}