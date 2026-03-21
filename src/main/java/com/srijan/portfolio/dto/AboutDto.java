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
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String roleTitle;

    @Size(max = 500)
    private String bio;

    @Size(max = 512)
    private String image;

    @Size(max = 50)
    private String location;

    @Size(max = 50)
    private String availability;

    @Size(max = 50)
    private String experienceYears;

    @Size(max = 20)
    private List<@Size(max = 3000) String> about;

    @Valid
    @Size(max = 12)
    private List<PrincipleDto> principles;
}
