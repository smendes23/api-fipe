package br.com.fipe.gateway.domain.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Brand entity representing a vehicle brand in the domain model.
 * 
 * This is a DDD entity that represents a vehicle brand (e.g., Ford, Chevrolet).
 * It follows the Single Responsibility Principle by focusing only on brand data.
 * 
 * @author FIPE Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("brands")
public class Brand {

    /**
     * Brand code (FIPE identifier).
     */
    @Id
    private Long id;

    private String code;

    /**
     * Brand name.
     */
    private String name;

    /**
     * Creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Factory method to create a new Brand instance.
     * Implements the Factory Pattern for object creation.
     * 
     * @param code the brand code
     * @param name the brand name
     * @return a new Brand instance
     */
    public static Brand create(String code, String name) {
        return Brand.builder()
                .code(code)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Validates the brand entity.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return code != null && !code.isBlank() 
                && name != null && !name.isBlank();
    }
}
