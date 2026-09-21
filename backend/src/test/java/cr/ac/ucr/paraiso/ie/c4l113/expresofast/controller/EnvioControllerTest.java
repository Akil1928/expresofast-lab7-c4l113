package cr.ac.ucr.paraiso.ie.c4l113.expresofast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.business.EnvioService;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioRequestDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.EnvioResponseDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.exception.ResourceNotFoundException;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.security.JwtAuthenticationFilter;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;


  //pruebas de corte de controlador (Slice Testing) para EnvioController.
 //sse mockea EnvioService: aqui solo se valida el contrato HTTP (codigos y JSON),
 ////no la logica de negocio (eso ya se prueba en EnvioServiceTest).
 


@WebMvcTest(EnvioController.class)
@AutoConfigureMockMvc(addFilters = false)
class EnvioControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnvioService envioService;

    //se mockean los filtros/beans de seguridad para que el contexto de
    //@WebMvcTest levante sin necesitar la base de datos ni el filtro JWT real.
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listarOptimizados_Exitoso_Retorna200YListaDeEnvios() throws Exception {
        EnvioResponseDTO envio = new EnvioResponseDTO(
                1, "EXP-0001", "Paraiso, Cartago", new BigDecimal("10"),
                new BigDecimal("3000"), "PENDIENTE", "SJO-1234", "Juan Perez Solano"
        );

        when(envioService.listarOptimizado()).thenReturn(List.of(envio));

        mockMvc.perform(get("/api/envios/optimizados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoRastreo").value("EXP-0001"))
                .andExpect(jsonPath("$[0].estadoEnvio").value("PENDIENTE"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void crear_PayloadInvalido_Retorna400ConErroresDeValidacion() throws Exception {
        //payload con campos requeridos vacios/nulos: violan @NotBlank y @Positive
        EnvioRequestDTO payloadInvalido = new EnvioRequestDTO();
        payloadInvalido.setCodigoRastreo(""); //viola @NotBlank y @Pattern

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error de validacion"))
                .andExpect(jsonPath("$.detalles").exists());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void obtenerBitacora_EnvioNoEncontrado_Retorna404() throws Exception {
        when(envioService.obtenerBitacora(eq(999)))
                .thenThrow(new ResourceNotFoundException("El envio con id 999 no existe."));

        mockMvc.perform(get("/api/envios/999/bitacora"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El envio con id 999 no existe."));
    }
}