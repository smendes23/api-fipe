package br.com.fipe.gateway.application.usecases;

import br.com.fipe.gateway.application.ports.UserServicePort;
import br.com.fipe.gateway.domain.dto.User;
import br.com.fipe.gateway.domain.entities.UserEntity;
import br.com.fipe.gateway.domain.repositories.UserRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceUseCase implements UserServicePort {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToUser)
                .cast(UserDetails.class)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("User not found: " + username)));
    }

    public Mono<User> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .map(this::convertToUser)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")));
    }

    public Mono<User> createUser(String username, String password, List<String> roles) {
        UserEntity entity = UserEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles(String.join(",", roles))
                .enabled(true)
                .build();

        return userRepository.save(entity)
                .map(this::convertToUser);
    }

    public User convertToUser(UserEntity entity) {
        List<String> roles = Arrays.asList(entity.getRoles().split(","));
        return User.builder()
                .username(entity.getUsername())
                .password(entity.getPassword())
                .roles(roles)
                .enabled(entity.getEnabled())
                .build();
    }
}
