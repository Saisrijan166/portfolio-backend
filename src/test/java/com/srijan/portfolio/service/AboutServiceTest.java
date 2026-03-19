package com.srijan.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srijan.portfolio.dto.AboutDto;
import com.srijan.portfolio.dto.PrincipleDto;
import com.srijan.portfolio.entity.Profile;
import com.srijan.portfolio.entity.User;
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
class AboutServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private AboutService aboutService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void updateAboutPreservesProfileFieldsWhenPayloadOmitsThem() {
        aboutService = new AboutService(userRepository, profileRepository, objectMapper);

        User user = User.builder()
                .id(1L)
                .username("sai")
                .build();

        Profile profile = Profile.builder()
                .id(10L)
                .user(user)
                .name("Sai")
                .roleTitle("Platform Engineer")
                .bio("Existing bio")
                .image("https://cdn.example.com/avatar.png")
                .location("Bengaluru")
                .availability("Open")
                .experienceYears("6+ years")
                .about("[\"Existing about\"]")
                .principles("[{\"title\":\"Ship\",\"description\":\"Fast\"}]")
                .build();

        when(userRepository.findByUsername("sai")).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AboutDto payload = AboutDto.builder()
                .about(List.of("Updated paragraph"))
                .principles(List.of(PrincipleDto.builder().title("Quality").description("High").build()))
                .build();

        AboutDto result = aboutService.updateAbout("sai", payload);

        ArgumentCaptor<Profile> savedProfile = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(savedProfile.capture());

        assertThat(savedProfile.getValue().getName()).isEqualTo("Sai");
        assertThat(savedProfile.getValue().getRoleTitle()).isEqualTo("Platform Engineer");
        assertThat(savedProfile.getValue().getBio()).isEqualTo("Existing bio");
        assertThat(savedProfile.getValue().getImage()).isEqualTo("https://cdn.example.com/avatar.png");
        assertThat(savedProfile.getValue().getLocation()).isEqualTo("Bengaluru");
        assertThat(savedProfile.getValue().getAvailability()).isEqualTo("Open");
        assertThat(savedProfile.getValue().getExperienceYears()).isEqualTo("6+ years");
        assertThat(result.getAbout()).containsExactly("Updated paragraph");
        assertThat(result.getPrinciples()).hasSize(1);
        assertThat(result.getPrinciples().getFirst().getTitle()).isEqualTo("Quality");
    }
}
