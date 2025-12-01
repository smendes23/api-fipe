package br.com.fipe.gateway.application.ports.input;

import br.com.fipe.gateway.domain.dto.User;
import br.com.fipe.gateway.domain.entities.UserEntity;
import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

public interface UserServicePort {
    Mono<UserDetails> findByUsername(String username);
    Mono<User> authenticate(String username, String password);
    Mono<User> createUser(String username, String password, List<String> roles);
    User convertToUser(UserEntity entity);
}
