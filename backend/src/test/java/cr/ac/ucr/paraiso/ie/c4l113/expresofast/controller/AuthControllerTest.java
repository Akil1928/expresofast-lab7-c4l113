package cr.ac.ucr.paraiso.ie.c4l113.expresofast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.business.AuthService;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.AuthRequestDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.dto.AuthResponseDTO;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.security.JwtAuthenticationFilter;
import cr.ac.ucr.paraiso.ie.c4l113.expresofast.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

//pruebas de corte de controlador para AuthController: login exitoso y fallido.
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void login_CredencialesCorrectas_Retorna200ConToken() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setUsername("admin");
        request.setPassword("admin123");

        AuthResponseDTO respuestaEsperada = new AuthResponseDTO(
                "token.jwt.simulado", "admin", List.of("ROLE_ADMIN"), 86400000L
        );

        when(authService.login(any(AuthRequestDTO.class))).thenReturn(respuestaEsperada);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token.jwt.simulado"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void login_CredencialesIncorrectas_Retorna401() throws Exception {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setUsername("admin");
        request.setPassword("claveMala");

        when(authService.login(any(AuthRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario o contraseña incorrectos"));
    }
}