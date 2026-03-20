package com.srijan.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileDto {
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String roleTitle;

    @Size(max = 255)
    private String location;

    @Size(max = 255)
    private String availability;

    @Email
    @Size(max = 255)
    private String primaryEmail;

    @Valid
    @Size(max = 10)
    private java.util.List<ContactLinkDto> professionalLinks;

    @Valid
    @Size(max = 10)
    private java.util.List<ContactLinkDto> socialLinks;

    @Size(max = 255)
    private String osName;

    @Size(max = 255)
    private String accountType;

    @Size(max = 255)
    private String access;

    @Size(max = 255)
    private String roleDescription;
}
