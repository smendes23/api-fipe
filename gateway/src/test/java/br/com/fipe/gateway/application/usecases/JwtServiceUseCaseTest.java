package br.com.fipe.gateway.application.usecases;

import br.com.fipe.gateway.domain.dto.User;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceUseCaseTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    private JwtServiceUseCase jwtServiceUseCase;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtServiceUseCase = new JwtServiceUseCase(jwtEncoder, jwtDecoder);

        setField(jwtServiceUseCase, "issuer", "test-issuer");
        setField(jwtServiceUseCase, "expiration", 3600000L);

        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        testUser = User.builder()
                .username("testuser")
                .password("password")
                .roles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"))
                .enabled(true)
                .build();
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String expectedToken = "test.jwt.token";
        Jwt mockJwt = mock(Jwt.class);

        when(mockJwt.getTokenValue()).thenReturn(expectedToken);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        String result = jwtServiceUseCase.generateToken(testUser);

        assertNotNull(result);
        assertEquals(expectedToken, result);
        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }

    @Test
    void generateToken_ShouldIncludeCorrectClaims() {
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("test.jwt.token");

        JwtClaimsSet[] capturedClaims = new JwtClaimsSet[1];
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            capturedClaims[0] = parameters.getClaims();
            return mockJwt;
        });

        jwtServiceUseCase.generateToken(testUser);

        assertNotNull(capturedClaims[0]);

        Map<String, Object> claimsMap = capturedClaims[0].getClaims();
        assertEquals("test-issuer", claimsMap.get("iss"));
        assertEquals("testuser", claimsMap.get("sub"));
        assertEquals("ROLE_USER ROLE_ADMIN", claimsMap.get("scope"));

        Instant issuedAt = (Instant) claimsMap.get("iat");
        Instant expiresAt = (Instant) claimsMap.get("exp");
        assertTrue(expiresAt.isAfter(issuedAt));
    }


    @Test
    void generateToken_WithEmptyAuthorities_ShouldHandleCorrectly() {
        User userWithNoRoles = User.builder()
                .username("testuser")
                .password("password")
                .roles(Arrays.asList())
                .enabled(true)
                .build();

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("test.jwt.token");

        JwtClaimsSet[] capturedClaims = new JwtClaimsSet[1];
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            capturedClaims[0] = parameters.getClaims();
            return mockJwt;
        });

        jwtServiceUseCase.generateToken(userWithNoRoles);

        assertNotNull(capturedClaims[0]);
        assertEquals("", capturedClaims[0].getClaim("scope"));
    }

    @Test
    void validateToken_ShouldReturnJwt() {
        String token = "test.jwt.token";
        Jwt expectedJwt = mock(Jwt.class);
        when(jwtDecoder.decode(token)).thenReturn(expectedJwt);

        Jwt result = jwtServiceUseCase.validateToken(token);

        assertNotNull(result);
        assertEquals(expectedJwt, result);
        verify(jwtDecoder).decode(token);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldThrowException() {
        String invalidToken = "invalid.token";
        when(jwtDecoder.decode(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        assertThrows(RuntimeException.class, () -> jwtServiceUseCase.validateToken(invalidToken));
        verify(jwtDecoder).decode(invalidToken);
    }

    @Test
    void getExpirationInSeconds_ShouldReturnCorrectValue() {
        long expectedExpirationInSeconds = 3600L;

        Long result = jwtServiceUseCase.getExpirationInSeconds();

        assertEquals(expectedExpirationInSeconds, result);
    }

    @Test
    void getExpirationInSeconds_WithZeroExpiration_ShouldReturnZero() {
        setField(jwtServiceUseCase, "expiration", 0L);

        Long result = jwtServiceUseCase.getExpirationInSeconds();

        assertEquals(0L, result);
    }

    @Test
    void generateToken_ShouldSetCorrectExpirationTime() {
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("test.jwt.token");

        Instant[] capturedIssuedAt = new Instant[1];
        Instant[] capturedExpiresAt = new Instant[1];

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenAnswer(invocation -> {
            JwtEncoderParameters parameters = invocation.getArgument(0);
            JwtClaimsSet claims = parameters.getClaims();
            capturedIssuedAt[0] = claims.getIssuedAt();
            capturedExpiresAt[0] = claims.getExpiresAt();
            return mockJwt;
        });

        jwtServiceUseCase.generateToken(testUser);

        assertNotNull(capturedIssuedAt[0]);
        assertNotNull(capturedExpiresAt[0]);

        long difference = capturedExpiresAt[0].toEpochMilli() - capturedIssuedAt[0].toEpochMilli();
        assertEquals(3600000L, difference, 1000);
    }
}
