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
    @Size(max = 60)
    private String name;

    @Size(max = 60)
    private String roleTitle;

    @Size(max = 60)
    private String location;

    @Size(max = 60)
    private String availability;

    @Email
    @Size(max = 80)
    private String primaryEmail;

    @Valid
    @Size(max = 5)
    private java.util.List<ContactLinkDto> professionalLinks;

    @Valid
    @Size(max = 5)
    private java.util.List<ContactLinkDto> socialLinks;

    @Size(max = 60)
    private String osName;

    @Size(max = 60)
    private String accountType;

    @Size(max = 60)
    private String access;

    @Size(max = 60)
    private String roleDescription;
}
