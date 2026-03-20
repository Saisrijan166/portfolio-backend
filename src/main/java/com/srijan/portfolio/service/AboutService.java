package com.srijan.portfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.AboutDto;
import com.srijan.portfolio.dto.PrincipleDto;
import com.srijan.portfolio.entity.Profile;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.ProfileRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AboutService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AboutDto getPublicAbout(String username) {
        Optional<User> userOptional = userRepository.findByUsername(sanitize(username));
        if (userOptional.isEmpty()) {
            return null;
        }
        return mapAbout(profileRepository.findByUserId(userOptional.get().getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public AboutDto getMyAbout(String username) {
        User user = findUserByUsername(username);
        return mapAbout(profileRepository.findByUserId(user.getId()).orElse(null));
    }

    @Transactional
    public AboutDto updateAbout(String username, AboutDto dto) {
        User user = findUserByUsername(username);
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(new Profile());

        profile.setUser(user);
        if (dto.getName() != null) {
            profile.setAboutName(sanitize(dto.getName()));
        }
        if (dto.getRoleTitle() != null) {
            profile.setAboutRoleTitle(sanitize(dto.getRoleTitle()));
        }
        if (dto.getBio() != null) {
            profile.setAboutBio(sanitize(dto.getBio()));
        }
        if (dto.getImage() != null) {
            profile.setAboutImage(sanitize(dto.getImage()));
        }
        if (dto.getLocation() != null) {
            profile.setAboutLocation(sanitize(dto.getLocation()));
        }
        if (dto.getAvailability() != null) {
            profile.setAboutAvailability(sanitize(dto.getAvailability()));
        }
        if (dto.getExperienceYears() != null) {
            profile.setAboutExperienceYears(sanitize(dto.getExperienceYears()));
        }

        try {
            if (dto.getAbout() != null) {
                profile.setAbout(objectMapper.writeValueAsString(sanitizeList(dto.getAbout())));
            }
            if (dto.getPrinciples() != null) {
                profile.setPrinciples(objectMapper.writeValueAsString(dto.getPrinciples()));
            }
        } catch (Exception e) {
            log.error("Failed to serialize about data", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ABOUT_SERIALIZATION_FAILED", "Failed to serialize about data");
        }

        return mapAbout(profileRepository.save(profile));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(sanitize(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String sanitize(String value) {
        return value == null ? null : value.trim();
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::sanitize)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private AboutDto mapAbout(Profile profile) {
        if (profile == null) {
            return null;
        }

        List<String> aboutList = null;
        List<PrincipleDto> principlesList = null;
        try {
            if (profile.getAbout() != null) {
                aboutList = objectMapper.readValue(profile.getAbout(), new TypeReference<List<String>>() {});
            }
            if (profile.getPrinciples() != null) {
                principlesList = objectMapper.readValue(profile.getPrinciples(), new TypeReference<List<PrincipleDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse about data", e);
        }

        return AboutDto.builder()
                .name(firstNonBlank(profile.getAboutName(), profile.getName()))
                .roleTitle(firstNonBlank(profile.getAboutRoleTitle(), profile.getRoleTitle()))
                .bio(firstNonBlank(profile.getAboutBio(), profile.getBio()))
                .image(firstNonBlank(profile.getAboutImage(), profile.getImage()))
                .location(firstNonBlank(profile.getAboutLocation(), profile.getLocation()))
                .availability(firstNonBlank(profile.getAboutAvailability(), profile.getAvailability()))
                .experienceYears(firstNonBlank(profile.getAboutExperienceYears(), profile.getExperienceYears()))
                .about(aboutList)
                .principles(principlesList)
                .build();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }
}
