package com.srijan.portfolio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppearanceSettingsDto {
    @Size(max = 40)
    private String theme;

    @Valid
    private WallpaperSettingsDto wallpaper;

    @Valid
    private WallpaperSettingsDto mobileWallpaper;
}
