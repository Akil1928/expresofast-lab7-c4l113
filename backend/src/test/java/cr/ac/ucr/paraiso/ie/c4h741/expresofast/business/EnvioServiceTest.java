package cr.ac.ucr.paraiso.ie.c4h741.expresofast.business;

import java.util.List;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        @ParameterizedTest
    @CsvSource({
            "10.0, 5.0, 3000.0",
            "20.0, 15.0, 6500.0",
            "2.5, 100.0, 20875.0",
            "1.0, 0.0, 650.0"
    })
    @DisplayName("calcularTarifa: calcula correctamente segun peso y distancia")
    void calcularTarifa_CasosVariados_CalculaCorrectamente(
            double pesoKg, double distanciaKm, double tarifaEsperada) {
        double tarifaCalculada = envioService.calcularTarifa(pesoKg, distanciaKm);
        assertEquals(tarifaEsperada, tarifaCalculada, 0.01);
    }

    @Test
    @DisplayName("calcularTarifa: peso cero o negativo lanza NegocioException")
    void calcularTarifa_PesoInvalido_LanzaExcepcion() {
        assertThrows(NegocioException.class, () -> envioService.calcularTarifa(0, 10));
        assertThrows(NegocioException.class, () -> envioService.calcularTarifa(-5, 10));
    }
        @Test
    @DisplayName("crear: si el conductor indicado no existe lanza NegocioException")
    void crearEnvio_ConductorInexistente_LanzaExcepcion() {
        EnvioRequestDTO request = new EnvioRequestDTO();
        request.setVehiculoId(1);
        request.setConductorId(99);
        request.setPesoKg(new BigDecimal("10"));

        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NegocioException.class, () -> envioService.crear(request));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("listarOptimizado: retorna la lista de envios mapeada a DTO")
    void listarOptimizado_RetornaListaMapeada() {
        Envio envio = new Envio();
        envio.setId(1);
        envio.setCodigoRastreo("EXP-0001");
        envio.setEstadoEnvio("PENDIENTE");
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);

        when(envioRepository.findAllOptimizado()).thenReturn(List.of(envio));

        List<EnvioResponseDTO> resultado = envioService.listarOptimizado();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigoRastreo()).isEqualTo("EXP-0001");
        assertThat(resultado.get(0).getPlacaVehiculo()).isEqualTo("SJO-1234");
    }

    @Test
    @DisplayName("actualizarEstado: con un estado invalido (no en la lista permitida) lanza NegocioException")
    void actualizarEstado_EstadoInvalido_LanzaExcepcion() {
        CambioEstadoDTO request = new CambioEstadoDTO();
        request.setNuevoEstado("ESTADO_INEXISTENTE");

        assertThrows(NegocioException.class, () -> envioService.actualizarEstado(1, request));
        verify(envioRepository, never()).findById(any());
    }

    @Test
    @DisplayName("actualizarEstado: con una transicion valida actualiza el envio y registra bitacora")
    void actualizarEstado_TransicionValida_ActualizaYRegistraBitacora() {
        Envio envio = new Envio();
        envio.setId(2);
        envio.setCodigoRastreo("EXP-0002");
        envio.setEstadoEnvio("PENDIENTE");
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);

        cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Usuario usuario =
                new cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Usuario();
        usuario.setUsername("admin");
        usuario.setNombreCompleto("Javier Moreno Sibaja");

        CambioEstadoDTO request = new CambioEstadoDTO();
        request.setNuevoEstado("EN_TRANSITO");
        request.setObservaciones("Sale de bodega");

        when(envioRepository.findById(2)).thenReturn(Optional.of(envio));
        when(usuarioRepository.findByUsername(any())).thenReturn(Optional.of(usuario));

        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null, List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        EnvioResponseDTO resultado = envioService.actualizarEstado(2, request);

        assertThat(resultado.getEstadoEnvio()).isEqualTo("EN_TRANSITO");
        verify(bitacoraEnvioRepository, times(1)).save(any());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("obtenerBitacora: si el envio no existe lanza ResourceNotFoundException")
    void obtenerBitacora_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.existsById(999)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> envioService.obtenerBitacora(999));
    }

    @Test
    @DisplayName("actualizarEstadoMasivoPorVehiculo: si el vehiculo no existe lanza NegocioException")
    void actualizarEstadoMasivoPorVehiculo_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.existsById(99)).thenReturn(false);

        assertThrows(NegocioException.class,
                () -> envioService.actualizarEstadoMasivoPorVehiculo(99, "EN_TRANSITO"));
    }

    @Test
    @DisplayName("actualizarEstadoMasivoPorVehiculo: con datos validos retorna la cantidad de filas actualizadas")
    void actualizarEstadoMasivoPorVehiculo_DatosValidos_RetornaFilasActualizadas() {
        when(vehiculoRepository.existsById(1)).thenReturn(true);
        when(envioRepository.actualizarEstadoPorVehiculo(1, "EN_TRANSITO")).thenReturn(3);

        int resultado = envioService.actualizarEstadoMasivoPorVehiculo(1, "EN_TRANSITO");

        assertThat(resultado).isEqualTo(3);
    }

    @Test
    @DisplayName("cancelarEnvio: con un envio PENDIENTE lo cancela correctamente")
    void cancelarEnvio_EnvioPendiente_CancelaCorrectamente() {
        Envio envio = new Envio();
        envio.setId(7);
        envio.setCodigoRastreo("EXP-0007");
        envio.setEstadoEnvio("PENDIENTE");
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);

        cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Usuario usuario =
                new cr.ac.ucr.paraiso.ie.c4h741.expresofast.domain.Usuario();
        usuario.setUsername("admin");

        when(envioRepository.findById(7)).thenReturn(Optional.of(envio));
        when(usuarioRepository.findByUsername(any())).thenReturn(Optional.of(usuario));

        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null, List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        EnvioResponseDTO resultado = envioService.cancelarEnvio(7);

        assertThat(resultado.getEstadoEnvio()).isEqualTo("CANCELADO");
        verify(bitacoraEnvioRepository, times(1)).save(any());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("cancelarEnvio: si el envio no existe lanza ResourceNotFoundException")
    void cancelarEnvio_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> envioService.cancelarEnvio(999));
    }
}