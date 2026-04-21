package com.srijan.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactMessageRequest {

    @NotBlank
    @Size(max = 100)
    private String subject;

    @NotBlank
    @Size(max = 600)
    private String message;
}
