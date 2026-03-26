package com.srijan.portfolio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpRequestDto {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
}
