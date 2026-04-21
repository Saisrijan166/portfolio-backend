package com.srijan.portfolio.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WallpaperSettingsDto {
    @Size(max = 16)
    private String kind;

    @Size(max = 100)
    private String src;

    @Size(max = 60)
    private String id;

    private Boolean isLight;
}
