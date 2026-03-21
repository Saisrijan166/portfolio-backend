package com.srijan.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactDto {
    @NotBlank
    @Email
    @Size(max = 255)
    private String primaryEmail;

    @Valid
    @Size(max = 10)
    private List<ContactLinkDto> professionalLinks;

    @Valid
    @Size(max = 10)
    private List<ContactLinkDto> socialLinks;
}
