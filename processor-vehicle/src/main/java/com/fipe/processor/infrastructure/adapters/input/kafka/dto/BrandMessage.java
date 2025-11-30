package com.fipe.processor.infrastructure.adapters.input.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for brand messages received from Kafka.
 * 
 * This class represents the message structure for brand data
 * published by API-1 and consumed by API-2.
 * 
 * @author FIPE Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandMessage {

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
}
