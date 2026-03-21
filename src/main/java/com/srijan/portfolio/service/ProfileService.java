package com.srijan.portfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.ContactLinkDto;
import com.srijan.portfolio.dto.ProfileDto;
import com.srijan.portfolio.entity.Contact;
import com.srijan.portfolio.entity.Profile;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.ContactRepository;
import com.srijan.portfolio.repository.ProfileRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProfileDto getPublicProfile(String username) {
        Optional<User> userOptional = userRepository.findByUsername(sanitize(username));
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        return mapProfile(
                profileRepository.findByUserId(user.getId()).orElse(null),
                contactRepository.findByUserId(user.getId()).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public ProfileDto getMyProfile(String username) {
        User user = findUserByUsername(username);
        return mapProfile(
                profileRepository.findByUserId(user.getId()).orElse(null),
                contactRepository.findByUserId(user.getId()).orElse(null)
        );
    }

    @Transactional
    public ProfileDto updateProfile(String username, ProfileDto dto) {
        User user = findUserByUsername(username);
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(new Profile());

        profile.setUser(user);
        if (dto.getName() != null) {
            profile.setProfileName(sanitize(dto.getName()));
        }
        if (dto.getRoleTitle() != null) {
            profile.setProfileRoleTitle(sanitize(dto.getRoleTitle()));
        }
        if (dto.getLocation() != null) {
            profile.setProfileLocation(sanitize(dto.getLocation()));
        }
        if (dto.getAvailability() != null) {
            profile.setProfileAvailability(sanitize(dto.getAvailability()));
        }
        if (dto.getOsName() != null) {
            profile.setOsName(sanitize(dto.getOsName()));
        }
        if (dto.getAccountType() != null) {
            profile.setAccountType(sanitize(dto.getAccountType()));
        }
        if (dto.getAccess() != null) {
            profile.setAccess(sanitize(dto.getAccess()));
        }
        if (dto.getRoleDescription() != null) {
            profile.setRoleDescription(sanitize(dto.getRoleDescription()));
        }
        if (dto.getPrimaryEmail() != null) {
            profile.setProfilePrimaryEmail(sanitize(dto.getPrimaryEmail()));
        }
        if (dto.getProfessionalLinks() != null) {
            profile.setProfileProfessionalLinks(writeLinks(dto.getProfessionalLinks()));
        }
        if (dto.getSocialLinks() != null) {
            profile.setProfileSocialLinks(writeLinks(dto.getSocialLinks()));
        }

        Profile saved = profileRepository.save(profile);
        Contact contact = contactRepository.findByUserId(user.getId()).orElse(null);
        return mapProfile(saved, contact);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(sanitize(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String sanitize(String value) {
        return value == null ? null : value.trim();
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

    private boolean hasProfileOwnedContacts(Profile profile) {
        return profile != null && (
                hasText(profile.getProfilePrimaryEmail())
                        || hasText(profile.getProfileProfessionalLinks())
                        || hasText(profile.getProfileSocialLinks())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String writeLinks(List<ContactLinkDto> links) {
        try {
            return objectMapper.writeValueAsString(links);
        } catch (Exception exception) {
            log.error("Failed to serialize profile contact links", exception);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PROFILE_CONTACT_SERIALIZATION_FAILED",
                    "Failed to serialize profile contact links"
            );
        }
    }

    private List<ContactLinkDto> readLinks(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ContactLinkDto>>() {});
        } catch (Exception exception) {
            log.warn("Failed to parse profile contact links", exception);
            return List.of();
        }
    }

    private ProfileDto mapProfile(Profile profile, Contact contact) {
        if (profile == null && contact == null) {
            return null;
        }

        boolean useProfileOwnedContacts = hasProfileOwnedContacts(profile);
        List<ContactLinkDto> professionalLinks = useProfileOwnedContacts
                ? readLinks(profile.getProfileProfessionalLinks())
                : mapGlobalLinks(contact != null ? contact.getProfessionalLinks() : null);

        List<ContactLinkDto> socialLinks = useProfileOwnedContacts
                ? readLinks(profile.getProfileSocialLinks())
                : mapGlobalLinks(contact != null ? contact.getSocialLinks() : null);

        String primaryEmail = useProfileOwnedContacts
                ? firstNonBlank(profile.getProfilePrimaryEmail(), null)
                : contact != null ? contact.getPrimaryEmail() : null;

        return ProfileDto.builder()
                .name(profile != null ? profile.getProfileName() : null)
                .roleTitle(profile != null ? profile.getProfileRoleTitle() : null)
                .location(profile != null ? profile.getProfileLocation() : null)
                .availability(profile != null ? profile.getProfileAvailability() : null)
                .osName(profile != null ? profile.getOsName() : null)
                .accountType(profile != null ? profile.getAccountType() : null)
                .access(profile != null ? profile.getAccess() : null)
                .roleDescription(profile != null ? profile.getRoleDescription() : null)
                .primaryEmail(primaryEmail)
                .professionalLinks(professionalLinks)
                .socialLinks(socialLinks)
                .build();
    }

    private List<ContactLinkDto> mapGlobalLinks(List<com.srijan.portfolio.entity.ContactLink> links) {
        if (links == null) {
            return List.of();
        }
        return links.stream()
                .map(link -> ContactLinkDto.builder()
                        .label(link.getLabel())
                        .url(link.getUrl())
                        .build())
                .toList();
    }
}
