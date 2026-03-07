package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.entity.*;
import com.srijan.portfolio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

        private final UserRepository userRepository;
        private final ProfileRepository profileRepository;
        private final ProjectRepository projectRepository;
        private final ExperienceRepository experienceRepository;
        private final SkillRepository skillRepository;
        private final EducationRepository educationRepository;
        private final ResumeRepository resumeRepository;
        private final ContactRepository contactRepository;
        private final ObjectMapper objectMapper;

        public PortfolioResponse getPortfolioByUsername(String username) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found: " + username));

                Long userId = user.getId();

                Profile profile = profileRepository.findByUserId(userId).orElse(null);
                List<Project> projects = projectRepository.findByUserIdAndDeletedFalse(userId);
                List<Experience> experiences = experienceRepository.findByUserIdAndDeletedFalse(userId);
                List<Skill> skills = skillRepository.findByUserId(userId);
                List<Education> educations = educationRepository.findByUserIdAndDeletedFalse(userId);
                Resume resume = resumeRepository.findByUserId(userId).orElse(null);
                Contact contact = contactRepository.findByUserId(userId).orElse(null);

                return PortfolioResponse.builder()
                                .username(user.getUsername())
                                .profile(mapProfile(profile))
                                .projects(projects.stream().map(this::mapProject).collect(Collectors.toList()))
                                .experiences(experiences.stream().map(this::mapExperience).collect(Collectors.toList()))
                                .skills(skills.stream().map(this::mapSkill).collect(Collectors.toList()))
                                .educations(educations.stream().map(this::mapEducation).collect(Collectors.toList()))
                                .resume(mapResume(resume))
                                .contact(mapContact(contact))
                                .build();
        }

        public ProfileDto updateProfile(String username, ProfileDto dto) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found: " + username));

                Profile profile = profileRepository.findByUserId(user.getId())
                                .orElse(new Profile());

                profile.setUser(user);
                profile.setName(dto.getName());
                profile.setRoleTitle(dto.getRoleTitle());
                profile.setBio(dto.getBio());
                profile.setImage(dto.getImage());
                profile.setLocation(dto.getLocation());
                profile.setAvailability(dto.getAvailability());
                profile.setExperienceYears(dto.getExperienceYears());

                profile.setOsName(dto.getOsName());
                profile.setAccountType(dto.getAccountType());
                profile.setAccess(dto.getAccess());
                profile.setRoleDescription(dto.getRoleDescription());

                try {
                        if (dto.getAbout() != null) {
                                profile.setAbout(objectMapper.writeValueAsString(dto.getAbout()));
                        } else {
                                profile.setAbout(null);
                        }
                        if (dto.getPrinciples() != null) {
                                profile.setPrinciples(objectMapper.writeValueAsString(dto.getPrinciples()));
                        } else {
                                profile.setPrinciples(null);
                        }
                } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize profile data", e);
                }

                profile = profileRepository.save(profile);

                return mapProfile(profile);
        }

        public ResumeDto updateResume(String username, ResumeDto dto) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Resume resume = resumeRepository.findByUserId(user.getId())
                                .orElse(new Resume());

                resume.setUser(user);
                resume.setResumeUrl(dto.getResumeUrl());
                resume.setLastUpdated(dto.getLastUpdated());

                return mapResume(resumeRepository.save(resume));
        }

        public ContactDto updateContact(String username, ContactDto dto) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Contact contact = contactRepository.findByUserId(user.getId())
                                .orElse(new Contact());

                contact.setUser(user);
                contact.setPrimaryEmail(dto.getPrimaryEmail());

                List<ContactLink> professional = dto.getProfessionalLinks().stream()
                                .map(l -> ContactLink.builder().label(l.getLabel()).url(l.getUrl()).build())
                                .collect(Collectors.toList());
                contact.setProfessionalLinks(professional);

                List<ContactLink> social = dto.getSocialLinks().stream()
                                .map(l -> ContactLink.builder().label(l.getLabel()).url(l.getUrl()).build())
                                .collect(Collectors.toList());
                contact.setSocialLinks(social);

                return mapContact(contactRepository.save(contact));
        }

        public List<ProjectDto> updateProjects(String username, List<ProjectDto> projectDtos) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Soft delete all existing projects
                List<Project> existing = projectRepository.findByUserIdAndDeletedFalse(user.getId());
                existing.forEach(p -> p.setDeleted(true));
                projectRepository.saveAll(existing);

                List<Project> updated = projectDtos.stream().map(dto -> {
                        Project p = new Project();
                        p.setUser(user);
                        p.setName(dto.getName());
                        p.setType(dto.getType());
                        p.setStatus(dto.getStatus());
                        p.setYear(dto.getYear());
                        p.setOverview(dto.getOverview());
                        p.setTechStack(dto.getTechStack());
                        p.setLiveLink(dto.getLiveLink());
                        p.setSourceLink(dto.getSourceLink());
                        p.setPdfLink(dto.getPdfLink());
                        p.setMediaVideo(dto.getMediaVideo());
                        p.setScreenshots(dto.getScreenshots());
                        p.setResearch(dto.isResearch());
                        p.setDeleted(false);
                        return p;
                }).collect(Collectors.toList());

                return projectRepository.saveAll(updated).stream().map(this::mapProject).collect(Collectors.toList());
        }

        public List<ExperienceDto> updateExperiences(String username, List<ExperienceDto> dtos) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<Experience> existing = experienceRepository.findByUserIdAndDeletedFalse(user.getId());
                existing.forEach(e -> e.setDeleted(true));
                experienceRepository.saveAll(existing);

                List<Experience> updated = dtos.stream().map(dto -> {
                        Experience e = new Experience();
                        e.setUser(user);
                        e.setCompany(dto.getCompany());
                        e.setRoleTitle(dto.getRoleTitle());
                        e.setDuration(dto.getDuration());
                        e.setStartYear(dto.getStartYear());
                        e.setEndYear(dto.getEndYear());
                        e.setCurrent(dto.isCurrent());
                        e.setResponsibilities(dto.getResponsibilities());
                        e.setAchievements(dto.getAchievements());
                        e.setSkills(dto.getSkills());
                        e.setAcademic(dto.isAcademic());
                        e.setLevel(dto.getLevel());
                        e.setInstitute(dto.getInstitute());
                        e.setLocation(dto.getLocation());
                        e.setDegree(dto.getDegree());
                        e.setScoreLabel(dto.getScoreLabel());
                        e.setScoreValue(dto.getScoreValue());
                        e.setDeleted(false);
                        return e;
                }).collect(Collectors.toList());

                return experienceRepository.saveAll(updated).stream().map(this::mapExperience)
                                .collect(Collectors.toList());
        }

        public List<SkillDto> updateSkills(String username, List<SkillDto> dtos) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<Skill> existing = skillRepository.findByUserId(user.getId());
                skillRepository.deleteAll(existing);

                List<Skill> updated = dtos.stream().map(dto -> {
                        Skill s = new Skill();
                        s.setUser(user);
                        s.setDomain(dto.getDomain());
                        s.setName(dto.getName());
                        s.setLevel(dto.getLevel());
                        s.setMetaSkill(dto.isMetaSkill());
                        s.setMetaDescription(dto.getMetaDescription());
                        return s;
                }).collect(Collectors.toList());

                return skillRepository.saveAll(updated).stream().map(this::mapSkill).collect(Collectors.toList());
        }

        public List<EducationDto> updateEducations(String username, List<EducationDto> dtos) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<Education> existing = educationRepository.findByUserId(user.getId());
                existing.forEach(e -> e.setDeleted(true));
                educationRepository.saveAll(existing); // apply soft-delete block

                List<Education> updated = dtos.stream().map(dto -> {
                        Education e = new Education();
                        e.setUser(user);
                        e.setLevel(dto.getLevel());
                        e.setInstitute(dto.getInstitute());
                        e.setLocation(dto.getLocation());
                        e.setDegree(dto.getDegree());
                        e.setScoreLabel(dto.getScoreLabel());
                        e.setScoreValue(dto.getScoreValue());
                        e.setDuration(dto.getDuration());
                        e.setDeleted(false);
                        return e;
                }).collect(Collectors.toList());

                return educationRepository.saveAll(updated).stream().map(this::mapEducation)
                                .collect(Collectors.toList());
        }

        private ProfileDto mapProfile(Profile profile) {
                if (profile == null)
                        return null;

                List<String> aboutList = null;
                List<PrincipleDto> principlesList = null;
                try {
                        if (profile.getAbout() != null) {
                                aboutList = objectMapper.readValue(profile.getAbout(),
                                                new TypeReference<List<String>>() {
                                                });
                        }
                        if (profile.getPrinciples() != null) {
                                principlesList = objectMapper.readValue(profile.getPrinciples(),
                                                new TypeReference<List<PrincipleDto>>() {
                                                });
                        }
                } catch (Exception e) {
                        System.err.println("Failed to parse about/principles: " + e.getMessage());
                }

                return ProfileDto.builder()
                                .name(profile.getName())
                                .roleTitle(profile.getRoleTitle())
                                .bio(profile.getBio())
                                .image(profile.getImage())
                                .location(profile.getLocation())
                                .availability(profile.getAvailability())
                                .experienceYears(profile.getExperienceYears())
                                .osName(profile.getOsName())
                                .accountType(profile.getAccountType())
                                .access(profile.getAccess())
                                .roleDescription(profile.getRoleDescription())
                                .about(aboutList)
                                .principles(principlesList)
                                .build();
        }

        private ProjectDto mapProject(Project project) {
                return ProjectDto.builder()
                                .id(project.getId())
                                .name(project.getName())
                                .type(project.getType())
                                .status(project.getStatus())
                                .year(project.getYear())
                                .overview(project.getOverview())
                                .techStack(project.getTechStack())
                                .liveLink(project.getLiveLink())
                                .sourceLink(project.getSourceLink())
                                .pdfLink(project.getPdfLink())
                                .mediaVideo(project.getMediaVideo())
                                .screenshots(project.getScreenshots())
                                .isResearch(project.isResearch())
                                .build();
        }

        private ExperienceDto mapExperience(Experience exp) {
                return ExperienceDto.builder()
                                .id(exp.getId())
                                .company(exp.getCompany())
                                .roleTitle(exp.getRoleTitle())
                                .duration(exp.getDuration())
                                .startYear(exp.getStartYear())
                                .endYear(exp.getEndYear())
                                .isCurrent(exp.isCurrent())
                                .responsibilities(exp.getResponsibilities())
                                .achievements(exp.getAchievements())
                                .skills(exp.getSkills())
                                .isAcademic(exp.isAcademic())
                                .level(exp.getLevel())
                                .institute(exp.getInstitute())
                                .location(exp.getLocation())
                                .degree(exp.getDegree())
                                .scoreLabel(exp.getScoreLabel())
                                .scoreValue(exp.getScoreValue())
                                .build();
        }

        private SkillDto mapSkill(Skill skill) {
                return SkillDto.builder()
                                .id(skill.getId())
                                .domain(skill.getDomain())
                                .name(skill.getName())
                                .level(skill.getLevel())
                                .isMetaSkill(skill.isMetaSkill())
                                .metaDescription(skill.getMetaDescription())
                                .build();
        }

        private EducationDto mapEducation(Education education) {
                if (education == null)
                        return null;
                return EducationDto.builder()
                                .id(education.getId())
                                .level(education.getLevel())
                                .institute(education.getInstitute())
                                .location(education.getLocation())
                                .degree(education.getDegree())
                                .scoreLabel(education.getScoreLabel())
                                .scoreValue(education.getScoreValue())
                                .duration(education.getDuration())
                                .deleted(education.isDeleted())
                                .build();
        }

        private ResumeDto mapResume(Resume resume) {
                if (resume == null)
                        return null;
                return ResumeDto.builder()
                                .resumeUrl(resume.getResumeUrl())
                                .lastUpdated(resume.getLastUpdated())
                                .build();
        }

        private ContactDto mapContact(Contact contact) {
                if (contact == null)
                        return null;

                List<ContactLinkDto> professional = contact.getProfessionalLinks() != null
                                ? contact.getProfessionalLinks().stream()
                                                .map(l -> new ContactLinkDto(l.getLabel(), l.getUrl()))
                                                .collect(Collectors.toList())
                                : List.of();

                List<ContactLinkDto> social = contact.getSocialLinks() != null
                                ? contact.getSocialLinks().stream()
                                                .map(l -> new ContactLinkDto(l.getLabel(), l.getUrl()))
                                                .collect(Collectors.toList())
                                : List.of();

                return ContactDto.builder()
                                .primaryEmail(contact.getPrimaryEmail())
                                .professionalLinks(professional)
                                .socialLinks(social)
                                .build();
        }
}
