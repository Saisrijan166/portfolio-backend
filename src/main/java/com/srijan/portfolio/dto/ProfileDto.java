package com.srijan.portfolio.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileDto {
    private String name;
    private String roleTitle;
    private String bio;
    private String image;
    private String location;
    private String availability;
    private String experienceYears;

    // new fields
    private List<String> about;
    private List<PrincipleDto> principles;
    private String osName;
    private String accountType;
    private String access;
    private String roleDescription;
}
