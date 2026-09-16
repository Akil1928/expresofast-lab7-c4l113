package cr.ac.ucr.paraiso.ie.c4h741.expresofast.business;

import cr.ac.ucr.paraiso.ie.c4h741.expresofast.dto.AuthRequestDTO;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.dto.AuthResponseDTO;
import cr.ac.ucr.paraiso.ie.c4h741.expresofast.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de AuthService, mockeando el AuthenticationManager de Spring
 * Security para no depender de la base de datos ni del UserDetailsService real.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // jwtExpirationMs se inyecta con @Value en produccion; en la prueba lo fijamos a mano
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 86400000L);
    }

    @Test
    @DisplayName("login: con credenciales validas retorna token, username y roles")
    void login_CredencialesValidas_RetornaAuthResponseDTO() {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setUsername("admin");
        request.setPassword("admin123");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("token.jwt.simulado");

        AuthResponseDTO resultado = authService.login(request);

        assertThat(resultado.getToken()).isEqualTo("token.jwt.simulado");
        assertThat(resultado.getUsername()).isEqualTo("admin");
        assertThat(resultado.getRoles()).containsExactly("ROLE_ADMIN");
        assertThat(resultado.getExpirationTime()).isEqualTo(86400000L);
    }

    @Test
    @DisplayName("login: con credenciales invalidas propaga BadCredentialsException")
    void login_CredencialesInvalidas_PropagaBadCredentialsException() {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setUsername("admin");
        request.setPassword("claveMala");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}