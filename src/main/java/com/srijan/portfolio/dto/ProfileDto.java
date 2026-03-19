package com.srijan.portfolio.dto;

import jakarta.validation.Valid;
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
public class ProfileDto {
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String roleTitle;

    @Size(max = 2000)
    private String bio;

    @Size(max = 512)
    private String image;

    @Size(max = 255)
    private String location;

    @Size(max = 255)
    private String availability;

    @Size(max = 255)
    private String experienceYears;

    @Size(max = 20)
    private List<String> about;

    @Valid
    @Size(max = 12)
    private List<PrincipleDto> principles;

    @Size(max = 255)
    private String osName;

    @Size(max = 255)
    private String accountType;

    @Size(max = 255)
    private String access;

    @Size(max = 255)
    private String roleDescription;
}
