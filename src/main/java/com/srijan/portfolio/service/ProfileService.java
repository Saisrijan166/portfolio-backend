package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.ContactLinkDto;
import com.srijan.portfolio.dto.ProfileDto;
import com.srijan.portfolio.entity.Contact;
import com.srijan.portfolio.entity.Profile;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.ContactRepository;
import com.srijan.portfolio.repository.ProfileRepository;
import com.srijan.portfolio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ContactRepository contactRepository;

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
            profile.setName(sanitize(dto.getName()));
        }
        if (dto.getRoleTitle() != null) {
            profile.setRoleTitle(sanitize(dto.getRoleTitle()));
        }
        if (dto.getBio() != null) {
            profile.setBio(sanitize(dto.getBio()));
        }
        if (dto.getImage() != null) {
            profile.setImage(sanitize(dto.getImage()));
        }
        if (dto.getExperienceYears() != null) {
            profile.setExperienceYears(sanitize(dto.getExperienceYears()));
        }
        if (dto.getLocation() != null) {
            profile.setLocation(sanitize(dto.getLocation()));
        }
        if (dto.getAvailability() != null) {
            profile.setAvailability(sanitize(dto.getAvailability()));
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

    private ProfileDto mapProfile(Profile profile, Contact contact) {
        if (profile == null && contact == null) {
            return null;
        }

        List<ContactLinkDto> professionalLinks = contact != null && contact.getProfessionalLinks() != null
                ? contact.getProfessionalLinks().stream()
                .map(link -> ContactLinkDto.builder()
                        .label(link.getLabel())
                        .url(link.getUrl())
                        .build())
                .toList()
                : List.of();

        List<ContactLinkDto> socialLinks = contact != null && contact.getSocialLinks() != null
                ? contact.getSocialLinks().stream()
                .map(link -> ContactLinkDto.builder()
                        .label(link.getLabel())
                        .url(link.getUrl())
                        .build())
                .toList()
                : List.of();

        return ProfileDto.builder()
                .name(profile != null ? profile.getName() : null)
                .roleTitle(profile != null ? profile.getRoleTitle() : null)
                .bio(profile != null ? profile.getBio() : null)
                .image(profile != null ? profile.getImage() : null)
                .location(profile != null ? profile.getLocation() : null)
                .availability(profile != null ? profile.getAvailability() : null)
                .experienceYears(profile != null ? profile.getExperienceYears() : null)
                .osName(profile != null ? profile.getOsName() : null)
                .accountType(profile != null ? profile.getAccountType() : null)
                .access(profile != null ? profile.getAccess() : null)
                .roleDescription(profile != null ? profile.getRoleDescription() : null)
                .primaryEmail(contact != null ? contact.getPrimaryEmail() : null)
                .professionalLinks(professionalLinks)
                .socialLinks(socialLinks)
                .build();
    }
}
