package br.com.fipe.gateway.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity {

    @Id
    private Long id;
    @Column("username")
    private String username;

    @Column("password")
    private String password;

    @Column("roles")
    private String roles;

    @Column("enabled")
    private Boolean enabled;
}
