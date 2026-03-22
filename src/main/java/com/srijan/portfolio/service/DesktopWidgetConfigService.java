package com.srijan.portfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.DesktopWidgetsDto;
import com.srijan.portfolio.entity.DesktopWidgetConfig;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.DesktopWidgetConfigRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DesktopWidgetConfigService {

    private final UserRepository userRepository;
    private final DesktopWidgetConfigRepository desktopWidgetConfigRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public DesktopWidgetsDto getPublicWidgets(String username) {
        Optional<User> userOptional = userRepository.findByUsername(sanitize(username));
        if (userOptional.isEmpty()) {
            return null;
        }
        return map(readSafely(userOptional.get().getId()));
    }

    @Transactional(readOnly = true)
    public DesktopWidgetsDto getMyWidgets(String username) {
        User user = findUserByUsername(username);
        return map(readSafely(user.getId()));
    }

    @Transactional
    public DesktopWidgetsDto updateWidgets(String username, DesktopWidgetsDto dto) {
        User user = findUserByUsername(username);
        DesktopWidgetConfig config = readSafely(user.getId());
        if (config == null) {
            config = new DesktopWidgetConfig();
        }

        config.setUser(user);
        config.setBottomLeftPrimary(writeList(dto.getBottomLeftPrimary()));
        config.setBottomLeftSecondary(writeList(dto.getBottomLeftSecondary()));
        config.setTopRight(writeList(dto.getTopRight()));

        return map(desktopWidgetConfigRepository.save(config));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(sanitize(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private DesktopWidgetConfig readSafely(Long userId) {
        try {
            return desktopWidgetConfigRepository.findByUserId(userId).orElse(null);
        } catch (Exception exception) {
            log.warn("Widget config table read failed for userId={}", userId, exception);
            return null;
        }
    }

    private DesktopWidgetsDto map(DesktopWidgetConfig config) {
        if (config == null) {
            return DesktopWidgetsDto.builder()
                    .bottomLeftPrimary(Collections.emptyList())
                    .bottomLeftSecondary(Collections.emptyList())
                    .topRight(Collections.emptyList())
                    .build();
        }

        return DesktopWidgetsDto.builder()
                .bottomLeftPrimary(readList(config.getBottomLeftPrimary()))
                .bottomLeftSecondary(readList(config.getBottomLeftSecondary()))
                .topRight(readList(config.getTopRight()))
                .build();
    }

    private List<String> readList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            log.warn("Failed to parse widget config payload", exception);
            return Collections.emptyList();
        }
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(sanitizeList(values));
        } catch (Exception exception) {
            log.error("Failed to serialize widget config payload", exception);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "WIDGET_CONFIG_SERIALIZATION_FAILED",
                    "Failed to serialize widget configuration"
            );
        }
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream()
                .map(this::sanitize)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(8)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private String sanitize(String value) {
        return value == null ? null : value.trim();
    }
}
