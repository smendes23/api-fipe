package com.fipe.processor.domain.entities;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("brands")
public class Brand {

    @Id
    private Long id;

    private String code;

    private String name;

    private LocalDateTime createdAt;

    public static Brand create(String code, String name) {
        return Brand.builder()
                .code(code)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public boolean isValid() {
        return code != null && !code.isBlank() 
                && name != null && !name.isBlank();
    }
}
