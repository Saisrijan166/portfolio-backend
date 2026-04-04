package com.srijan.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.AboutDto;
import com.srijan.portfolio.dto.PrincipleDto;
import com.srijan.portfolio.entity.About;
import com.srijan.portfolio.entity.AboutPrinciple;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.AboutRepository;
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
    private final AboutRepository aboutRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AboutDto getPublicAbout(String username) {
        Optional<User> userOptional = userRepository.findByUsername(sanitize(username));
        if (userOptional.isEmpty()) {
            return null;
        }
        return mapAbout(readAboutSafely(userOptional.get().getId()));
    }

    @Transactional(readOnly = true)
    public AboutDto getMyAbout(String username) {
        User user = findUserByUsername(username);
        return mapAbout(readAboutSafely(user.getId()));
    }

    @Transactional
    public AboutDto updateAbout(String username, AboutDto dto) {
        User user = findUserByUsername(username);
        About about = aboutRepository.findByUserId(user.getId()).orElseGet(About::new);

        about.setUser(user);
        if (dto.getName() != null) {
            about.setName(sanitize(dto.getName()));
        }
        if (dto.getRoleTitle() != null) {
            about.setRoleTitle(sanitize(dto.getRoleTitle()));
        }
        if (dto.getBio() != null) {
            about.setBio(sanitize(dto.getBio()));
        }
        if (dto.getImage() != null) {
            about.setImage(sanitize(dto.getImage()));
        }
        if (dto.getLocation() != null) {
            about.setLocation(sanitize(dto.getLocation()));
        }
        if (dto.getAvailability() != null) {
            about.setAvailability(sanitize(dto.getAvailability()));
        }
        if (dto.getExperienceYears() != null) {
            about.setExperienceYears(sanitize(dto.getExperienceYears()));
        }

        try {
            if (dto.getAbout() != null) {
                about.setAboutContent(objectMapper.writeValueAsString(sanitizeList(dto.getAbout())));
            }
        } catch (Exception exception) {
            log.error("Failed to serialize about data", exception);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ABOUT_SERIALIZATION_FAILED", "Failed to serialize about data");
        }

        if (dto.getPrinciples() != null) {
            if (about.getId() == null) {
                about = aboutRepository.saveAndFlush(about);
            }
            about.getPrinciples().clear();
            aboutRepository.saveAndFlush(about);
            List<PrincipleDto> sanitizedPrinciples = sanitizePrinciples(dto.getPrinciples());
            for (int index = 0; index < sanitizedPrinciples.size(); index++) {
                PrincipleDto principle = sanitizedPrinciples.get(index);
                about.getPrinciples().add(AboutPrinciple.builder()
                        .about(about)
                        .sortOrder(index)
                        .title(principle.getTitle())
                        .description(principle.getDescription())
                        .build());
            }
        }

        return mapAbout(aboutRepository.save(about));
    }

    private About readAboutSafely(Long userId) {
        try {
            List<About> abouts = aboutRepository.findAllByUserIdOrderByIdAsc(userId);
            return abouts.isEmpty() ? null : abouts.getFirst();
        } catch (Exception exception) {
            log.warn("About table read failed for userId={}", userId, exception);
            return null;
        }
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

    private List<PrincipleDto> sanitizePrinciples(List<PrincipleDto> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream()
                .filter(principle -> principle != null)
                .map(principle -> PrincipleDto.builder()
                        .title(sanitize(principle.getTitle()))
                        .description(sanitize(principle.getDescription()))
                        .build())
                .filter(principle -> principle.getTitle() != null && !principle.getTitle().isBlank())
                .filter(principle -> principle.getDescription() != null && !principle.getDescription().isBlank())
                .toList();
    }

    private AboutDto mapAbout(About about) {
        if (about == null) {
            return null;
        }

        List<String> aboutList = null;
        try {
            if (about != null && about.getAboutContent() != null) {
                aboutList = objectMapper.readValue(
                        about.getAboutContent(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
            }
        } catch (Exception exception) {
            log.warn("Failed to parse about data", exception);
        }

        List<PrincipleDto> principlesList = about.getPrinciples().stream()
                .map(principle -> PrincipleDto.builder()
                        .title(principle.getTitle())
                        .description(principle.getDescription())
                        .build())
                .toList();

        return AboutDto.builder()
                .name(about.getName())
                .roleTitle(about.getRoleTitle())
                .bio(about.getBio())
                .image(about.getImage())
                .location(about.getLocation())
                .availability(about.getAvailability())
                .experienceYears(about.getExperienceYears())
                .about(aboutList)
                .principles(principlesList)
                .build();
    }
}
