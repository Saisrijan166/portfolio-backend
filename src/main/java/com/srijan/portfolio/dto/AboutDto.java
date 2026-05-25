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
@NoArgsConstructor
@AllArgsConstructor
public class AboutDto {
    @Size(max = 60)
    private String name;

    @Size(max = 60)
    private String roleTitle;

    @Size(max = 300)
    private String bio;

    @Size(max = 255)
    private String image;

    @Size(max = 60)
    private String location;

    @Size(max = 60)
    private String availability;

    @Size(max = 60)
    private String experienceYears;

    @Size(max = 10)
    private List<@Size(max = 2500) String> about;

    @Valid
    @Size(max = 6)
    private List<PrincipleDto> principles;
}
