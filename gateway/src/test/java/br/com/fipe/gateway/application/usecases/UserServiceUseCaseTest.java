package br.com.fipe.gateway.application.usecases;

import br.com.fipe.gateway.domain.dto.User;
import br.com.fipe.gateway.domain.entities.UserEntity;
import br.com.fipe.gateway.domain.repositories.UserRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceUseCase userServiceUseCase;

    @BeforeEach
    void setUp() {
        userServiceUseCase = new UserServiceUseCase(userRepository, passwordEncoder);
    }

    @Test
    void findByUsername_Success() {
        String username = "testuser";
        UserEntity userEntity = UserEntity.builder()
                .username(username)
                .password("encodedPassword")
                .roles("ROLE_USER,ROLE_ADMIN")
                .enabled(true)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(userEntity));

        StepVerifier.create(userServiceUseCase.findByUsername(username))
                .expectNextMatches(userDetails -> {
                    assertThat(userDetails.getUsername()).isEqualTo(username);
                    assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
                    assertThat(userDetails.getAuthorities()).hasSize(2);
                    return true;
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void findByUsername_UserNotFound() {
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());

        StepVerifier.create(userServiceUseCase.findByUsername(username))
                .expectErrorMatches(throwable ->
                        throwable instanceof UsernameNotFoundException &&
                                throwable.getMessage().contains("User not found: " + username)
                )
                .verify();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void authenticate_Success() {
        String username = "testuser";
        String password = "rawPassword";
        String encodedPassword = "encodedPassword";

        UserEntity userEntity = UserEntity.builder()
                .username(username)
                .password(encodedPassword)
                .roles("ROLE_USER")
                .enabled(true)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(userEntity));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        StepVerifier.create(userServiceUseCase.authenticate(username, password))
                .expectNextMatches(user -> {
                    assertThat(user.getUsername()).isEqualTo(username);
                    assertThat(user.getPassword()).isEqualTo(encodedPassword);
                    assertThat(user.getRoles()).containsExactly("ROLE_USER");
                    assertThat(user.isEnabled()).isTrue();
                    return true;
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, encodedPassword);
    }

    @Test
    void authenticate_InvalidPassword() {
        String username = "testuser";
        String password = "wrongPassword";
        String encodedPassword = "encodedPassword";

        UserEntity userEntity = UserEntity.builder()
                .username(username)
                .password(encodedPassword)
                .roles("ROLE_USER")
                .enabled(true)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(userEntity));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);

        StepVerifier.create(userServiceUseCase.authenticate(username, password))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Invalid credentials")
                )
                .verify();

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, encodedPassword);
    }

    @Test
    void authenticate_UserNotFound() {
        String username = "nonexistent";
        String password = "anyPassword";

        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());

        StepVerifier.create(userServiceUseCase.authenticate(username, password))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Invalid credentials")
                )
                .verify();

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void createUser_Success() {
        String username = "newuser";
        String password = "rawPassword";
        String encodedPassword = "encodedPassword";
        List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");

        UserEntity userEntityToSave = UserEntity.builder()
                .username(username)
                .password(encodedPassword)
                .roles("ROLE_USER,ROLE_ADMIN")
                .enabled(true)
                .build();

        UserEntity savedUserEntity = UserEntity.builder()
                .id(1L)
                .username(username)
                .password(encodedPassword)
                .roles("ROLE_USER,ROLE_ADMIN")
                .enabled(true)
                .build();

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(savedUserEntity));

        StepVerifier.create(userServiceUseCase.createUser(username, password, roles))
                .expectNextMatches(user -> {
                    assertThat(user.getUsername()).isEqualTo(username);
                    assertThat(user.getPassword()).isEqualTo(encodedPassword);
                    assertThat(user.getRoles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
                    assertThat(user.isEnabled()).isTrue();
                    return true;
                })
                .verifyComplete();

        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void createUser_WithSingleRole() {
        String username = "simpleuser";
        String password = "password";
        String encodedPassword = "encodedPassword";
        List<String> roles = List.of("ROLE_USER");

        UserEntity savedUserEntity = UserEntity.builder()
                .id(2L)
                .username(username)
                .password(encodedPassword)
                .roles("ROLE_USER")
                .enabled(true)
                .build();

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(savedUserEntity));

        StepVerifier.create(userServiceUseCase.createUser(username, password, roles))
                .expectNextMatches(user -> {
                    assertThat(user.getRoles()).containsExactly("ROLE_USER");
                    return true;
                })
                .verifyComplete();

        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void createUser_RepositoryError() {
        String username = "erroruser";
        String password = "password";
        List<String> roles = List.of("ROLE_USER");
        String errorMessage = "Database error";

        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.error(new RuntimeException(errorMessage)));

        StepVerifier.create(userServiceUseCase.createUser(username, password, roles))
                .expectError(RuntimeException.class)
                .verify();

        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void convertToUser_Success() {
        UserEntity entity = UserEntity.builder()
                .username("testuser")
                .password("password")
                .roles("ROLE_USER,ROLE_ADMIN,ROLE_MODERATOR")
                .enabled(true)
                .build();

        User result = userServiceUseCase.convertToUser(entity);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("password");
        assertThat(result.getRoles()).containsExactly("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void convertToUser_WithEmptyRoles() {
        UserEntity entity = UserEntity.builder()
                .username("testuser")
                .password("password")
                .roles("") // Empty roles string
                .enabled(false)
                .build();

        User result = userServiceUseCase.convertToUser(entity);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("password");
        assertThat(result.getRoles()).containsExactly("");
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    void convertToUser_WithSingleRole() {
        UserEntity entity = UserEntity.builder()
                .username("testuser")
                .password("password")
                .roles("ROLE_USER") // Single role
                .enabled(true)
                .build();

        User result = userServiceUseCase.convertToUser(entity);

        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("password");
        assertThat(result.getRoles()).containsExactly("ROLE_USER");
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void findByUsername_WithDisabledUser() {
        String username = "disableduser";
        UserEntity userEntity = UserEntity.builder()
                .username(username)
                .password("password")
                .roles("ROLE_USER")
                .enabled(false) // Disabled user
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(userEntity));

        StepVerifier.create(userServiceUseCase.findByUsername(username))
                .expectNextMatches(userDetails -> {
                    assertThat(userDetails.getUsername()).isEqualTo(username);
                    assertThat(userDetails.isEnabled()).isFalse();
                    return true;
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void authenticate_WithDisabledUser() {
        String username = "disableduser";
        String password = "password";
        String encodedPassword = "encodedPassword";

        UserEntity userEntity = UserEntity.builder()
                .username(username)
                .password(encodedPassword)
                .roles("ROLE_USER")
                .enabled(false) // Disabled user
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(userEntity));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        StepVerifier.create(userServiceUseCase.authenticate(username, password))
                .expectNextMatches(user -> {
                    assertThat(user.getUsername()).isEqualTo(username);
                    assertThat(user.isEnabled()).isFalse();
                    return true;
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, encodedPassword);
    }
}
