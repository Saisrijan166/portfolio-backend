package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.AppearanceSettingsDto;
import com.srijan.portfolio.dto.WallpaperSettingsDto;
import com.srijan.portfolio.entity.AppearanceConfig;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.AppearanceConfigRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppearanceConfigService {

    private final UserRepository userRepository;
    private final AppearanceConfigRepository appearanceConfigRepository;

    @Transactional(readOnly = true)
    public AppearanceSettingsDto getPublicAppearance(String username) {
        Optional<User> userOptional = userRepository.findByUsername(sanitize(username));
        if (userOptional.isEmpty()) {
            return null;
        }

        return map(appearanceConfigRepository.findByUserId(userOptional.get().getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public AppearanceSettingsDto getMyAppearance(String username) {
        User user = findUserByUsername(username);
        return map(appearanceConfigRepository.findByUserId(user.getId()).orElse(null));
    }

    @Transactional
    public AppearanceSettingsDto updateAppearance(String username, AppearanceSettingsDto dto) {
        User user = findUserByUsername(username);
        AppearanceConfig config = appearanceConfigRepository.findByUserId(user.getId()).orElse(new AppearanceConfig());

        config.setUser(user);
        config.setTheme(sanitize(dto.getTheme()));
        config.setWallpaperKind(sanitize(dto.getWallpaper() != null ? dto.getWallpaper().getKind() : null));
        config.setWallpaperSrc(sanitize(dto.getWallpaper() != null ? dto.getWallpaper().getSrc() : null));
        config.setWallpaperId(sanitize(dto.getWallpaper() != null ? dto.getWallpaper().getId() : null));
        config.setWallpaperIsLight(dto.getWallpaper() != null ? dto.getWallpaper().getIsLight() : null);

        return map(appearanceConfigRepository.save(config));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(sanitize(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private AppearanceSettingsDto map(AppearanceConfig config) {
        if (config == null) {
            return null;
        }

        return AppearanceSettingsDto.builder()
                .theme(sanitize(config.getTheme()))
                .wallpaper(mapWallpaper(config))
                .build();
    }

    private WallpaperSettingsDto mapWallpaper(AppearanceConfig config) {
        String kind = sanitize(config.getWallpaperKind());
        String src = sanitize(config.getWallpaperSrc());
        String id = sanitize(config.getWallpaperId());
        Boolean isLight = config.getWallpaperIsLight();

        if (kind == null && src == null && id == null && isLight == null) {
            return null;
        }

        return WallpaperSettingsDto.builder()
                .kind(kind)
                .src(src)
                .id(id)
                .isLight(isLight)
                .build();
    }

    private String sanitize(String value) {
        return value == null ? null : value.trim();
    }
}
