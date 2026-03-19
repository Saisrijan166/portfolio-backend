package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.ContactLinkDto;
import com.srijan.portfolio.dto.ProfileDto;
import com.srijan.portfolio.entity.Contact;
import com.srijan.portfolio.entity.ContactLink;
import com.srijan.portfolio.entity.Profile;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.repository.ContactRepository;
import com.srijan.portfolio.repository.ProfileRepository;
import com.srijan.portfolio.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void updateProfilePreservesExistingFieldsAndReturnsContactData() {
        User user = User.builder()
                .id(1L)
                .username("sai")
                .build();

        Profile profile = Profile.builder()
                .id(10L)
                .user(user)
                .name("Sai")
                .roleTitle("Architect")
                .bio("Existing bio")
                .image("https://cdn.example.com/avatar.png")
                .location("Bengaluru")
                .availability("Open")
                .experienceYears("7+ years")
                .osName("Sai OS")
                .accountType("Admin")
                .access("Full")
                .roleDescription("Runs the portfolio")
                .build();

        Contact contact = Contact.builder()
                .id(20L)
                .user(user)
                .primaryEmail("sai@example.com")
                .professionalLinks(List.of(ContactLink.builder().label("LinkedIn").url("https://linkedin.com/in/sai").build()))
                .socialLinks(List.of(ContactLink.builder().label("GitHub").url("https://github.com/sai").build()))
                .build();

        when(userRepository.findByUsername("sai")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contactRepository.findByUserId(1L)).thenReturn(Optional.of(contact));

        ProfileDto payload = ProfileDto.builder()
                .name("Sai Teja")
                .availability("Busy")
                .build();

        ProfileDto result = profileService.updateProfile("sai", payload);

        ArgumentCaptor<Profile> savedProfile = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(savedProfile.capture());

        assertThat(savedProfile.getValue().getName()).isEqualTo("Sai Teja");
        assertThat(savedProfile.getValue().getAvailability()).isEqualTo("Busy");
        assertThat(savedProfile.getValue().getRoleTitle()).isEqualTo("Architect");
        assertThat(savedProfile.getValue().getBio()).isEqualTo("Existing bio");
        assertThat(savedProfile.getValue().getOsName()).isEqualTo("Sai OS");

        assertThat(result.getPrimaryEmail()).isEqualTo("sai@example.com");
        assertThat(result.getProfessionalLinks())
                .extracting(ContactLinkDto::getLabel)
                .containsExactly("LinkedIn");
        assertThat(result.getSocialLinks())
                .extracting(ContactLinkDto::getLabel)
                .containsExactly("GitHub");
    }
}
