package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.entity.*;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

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

    private Optional<User> findOptionalUserByUsername(String username) {
        return userRepository.findByUsername(sanitize(username));
    }

    public boolean publicUserExists(String username) {
        return findOptionalUserByUsername(username).isPresent();
    }

    private User findUserByUsername(String username) {
        return findOptionalUserByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC PORTFOLIO BOOTSTRAP
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioBootstrap(String username) {
        Optional<User> userOptional = findOptionalUserByUsername(username);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        Long userId = user.getId();

        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        long projectCount = projectRepository.countByUserIdAndDeletedFalse(userId);
        long professionalExperienceCount = experienceRepository.countProfessionalByUserId(userId);

        return PortfolioResponse.builder()
                .username(user.getUsername())
                .profile(mapProfile(profile))
                .projectCount(projectCount)
                .experienceCount(professionalExperienceCount)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC – /api/public/portfolio/{username}/*
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProfileDto getPublicProfile(String username) {
        Optional<User> userOptional = findOptionalUserByUsername(username);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return mapProfile(profile);
    }

    @Transactional(readOnly = true)
    public AboutDto getPublicAbout(String username) {
        Optional<User> userOptional = findOptionalUserByUsername(username);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return mapAbout(profile);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getPublicProjects(String username) {
        if (!publicUserExists(username)) {
            return null;
        }
        return projectRepository.findByUserUsernameAndDeletedFalse(sanitize(username))
                .stream().map(this::mapProject).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExperienceDto> getPublicExperience(String username) {
        if (!publicUserExists(username)) {
            return null;
        }
        return experienceRepository.findByUserUsernameAndDeletedFalse(sanitize(username))
                .stream().map(this::mapExperience).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SkillDto> getPublicSkills(String username) {
        if (!publicUserExists(username)) {
            return null;
        }
        return skillRepository.findByUserUsername(sanitize(username))
                .stream().map(this::mapSkill).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EducationDto> getPublicEducation(String username) {
        if (!publicUserExists(username)) {
            return null;
        }
        return educationRepository.findByUserUsernameAndDeletedFalse(sanitize(username))
                .stream().map(this::mapEducation).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeDto getPublicResume(String username) {
        Optional<User> userOptional = findOptionalUserByUsername(username);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        Resume resume = resumeRepository.findByUserId(user.getId()).orElse(null);
        return mapResume(resume);
    }

    @Transactional(readOnly = true)
    public ContactDto getPublicContact(String username) {
        Optional<User> userOptional = findOptionalUserByUsername(username);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        Contact contact = contactRepository.findByUserId(user.getId()).orElse(null);
        return mapContact(contact);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN PROFILE – /api/me/profile
    // ─────────────────────────────────────────────────────────────────────────

    public ProfileDto getMyProfile(String username) {
        User user = findUserByUsername(username);
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return mapProfile(profile);
    }

    @Transactional
    public ProfileDto updateProfile(String username, ProfileDto dto) {
        User user = findUserByUsername(username);

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElse(new Profile());

        profile.setUser(user);
        profile.setName(sanitize(dto.getName()));
        profile.setRoleTitle(sanitize(dto.getRoleTitle()));
        profile.setBio(sanitize(dto.getBio()));
        profile.setImage(sanitize(dto.getImage()));
        profile.setLocation(sanitize(dto.getLocation()));
        profile.setAvailability(sanitize(dto.getAvailability()));
        profile.setExperienceYears(sanitize(dto.getExperienceYears()));
        profile.setOsName(sanitize(dto.getOsName()));
        profile.setAccountType(sanitize(dto.getAccountType()));
        profile.setAccess(sanitize(dto.getAccess()));
        profile.setRoleDescription(sanitize(dto.getRoleDescription()));

        return mapProfile(profileRepository.save(profile));
    }

    public AboutDto getMyAbout(String username) {
        User user = findUserByUsername(username);
        Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        return mapAbout(profile);
    }

    @Transactional
    public AboutDto updateAbout(String username, AboutDto dto) {
        User user = findUserByUsername(username);
        Profile profile = profileRepository.findByUserId(user.getId())
                .orElse(new Profile());

        profile.setUser(user);

        try {
            profile.setAbout(dto.getAbout() != null
                    ? objectMapper.writeValueAsString(dto.getAbout()) : null);
            profile.setPrinciples(dto.getPrinciples() != null
                    ? objectMapper.writeValueAsString(dto.getPrinciples()) : null);
        } catch (Exception e) {
            log.error("Failed to serialize about data", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ABOUT_SERIALIZATION_FAILED", "Failed to serialize about data");
        }

        return mapAbout(profileRepository.save(profile));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN PROJECTS – /api/me/projects
    // ─────────────────────────────────────────────────────────────────────────

    public List<ProjectDto> getMyProjects(String username) {
        findUserByUsername(username);
        return projectRepository.findByUserUsernameAndDeletedFalse(username)
                .stream().map(this::mapProject).collect(Collectors.toList());
    }

    @Transactional
    public ProjectDto createProject(String username, ProjectDto dto) {
        User user = findUserByUsername(username);
        Project p = buildProject(new Project(), user, dto);
        return mapProject(projectRepository.save(p));
    }

    @Transactional
    public ProjectDto updateProject(String username, Long id, ProjectDto dto) {
        Project p = projectRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return mapProject(projectRepository.save(buildProject(p, p.getUser(), dto)));
    }

    @Transactional
    public void deleteProject(String username, Long id) {
        Project p = projectRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        p.setDeleted(true);
        projectRepository.save(p);
    }

    private Project buildProject(Project p, User user, ProjectDto dto) {
        p.setUser(user);
        p.setName(sanitize(dto.getName()));
        p.setType(sanitize(dto.getType()));
        p.setStatus(sanitize(dto.getStatus()));
        p.setYear(sanitize(dto.getYear()));
        p.setOverview(sanitize(dto.getOverview()));
        p.setTechStack(sanitizeList(dto.getTechStack()));
        p.setLiveLink(sanitize(dto.getLiveLink()));
        p.setSourceLink(sanitize(dto.getSourceLink()));
        p.setPdfLink(sanitize(dto.getPdfLink()));
        p.setMediaVideo(sanitize(dto.getMediaVideo()));
        p.setScreenshots(sanitizeList(dto.getScreenshots()));
        p.setResearch(dto.isResearch());
        p.setDeleted(false);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN EXPERIENCE – /api/me/experience
    // ─────────────────────────────────────────────────────────────────────────

    public List<ExperienceDto> getMyExperience(String username) {
        findUserByUsername(username);
        return experienceRepository.findByUserUsernameAndDeletedFalse(username)
                .stream().map(this::mapExperience).collect(Collectors.toList());
    }

    @Transactional
    public ExperienceDto createExperience(String username, ExperienceDto dto) {
        User user = findUserByUsername(username);
        Experience e = buildExperience(new Experience(), user, dto);
        return mapExperience(experienceRepository.save(e));
    }

    @Transactional
    public ExperienceDto updateExperience(String username, Long id, ExperienceDto dto) {
        Experience e = experienceRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
        return mapExperience(experienceRepository.save(buildExperience(e, e.getUser(), dto)));
    }

    @Transactional
    public void deleteExperience(String username, Long id) {
        Experience e = experienceRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
        e.setDeleted(true);
        experienceRepository.save(e);
    }

    private Experience buildExperience(Experience e, User user, ExperienceDto dto) {
        Integer currentYear = Year.now().getValue();
        if (dto.getStartYear() != null && dto.getEndYear() != null && dto.getStartYear() > dto.getEndYear() && !dto.isCurrent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "Start year must be before or equal to end year");
        }
        if (dto.getStartYear() != null && dto.getStartYear() > currentYear + 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "Start year is not valid");
        }

        e.setUser(user);
        e.setCompany(sanitize(dto.getCompany()));
        e.setRoleTitle(sanitize(dto.getRoleTitle()));
        e.setDuration(sanitize(dto.getDuration()));
        e.setStartYear(dto.getStartYear());
        e.setEndYear(dto.getEndYear());
        e.setCurrent(dto.isCurrent());
        e.setResponsibilities(sanitizeList(dto.getResponsibilities()));
        e.setAchievements(sanitizeList(dto.getAchievements()));
        e.setSkills(sanitizeList(dto.getSkills()));
        e.setAcademic(dto.isAcademic());
        e.setLevel(sanitize(dto.getLevel()));
        e.setInstitute(sanitize(dto.getInstitute()));
        e.setLocation(sanitize(dto.getLocation()));
        e.setDegree(sanitize(dto.getDegree()));
        e.setScoreLabel(sanitize(dto.getScoreLabel()));
        e.setScoreValue(sanitize(dto.getScoreValue()));
        e.setDeleted(false);
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN SKILLS – /api/me/skills
    // ─────────────────────────────────────────────────────────────────────────

    public List<SkillDto> getMySkills(String username) {
        findUserByUsername(username);
        return skillRepository.findByUserUsername(username)
                .stream().map(this::mapSkill).collect(Collectors.toList());
    }

    @Transactional
    public SkillDto createSkill(String username, SkillDto dto) {
        User user = findUserByUsername(username);
        Skill s = buildSkill(new Skill(), user, dto);
        return mapSkill(skillRepository.save(s));
    }

    @Transactional
    public SkillDto updateSkill(String username, Long id, SkillDto dto) {
        Skill s = skillRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        return mapSkill(skillRepository.save(buildSkill(s, s.getUser(), dto)));
    }

    @Transactional
    public void deleteSkill(String username, Long id) {
        Skill s = skillRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        skillRepository.delete(s);
    }

    private Skill buildSkill(Skill s, User user, SkillDto dto) {
        s.setUser(user);
        s.setDomain(sanitize(dto.getDomain()));
        s.setName(sanitize(dto.getName()));
        s.setLevel(dto.getLevel());
        s.setMetaSkill(dto.isMetaSkill());
        s.setMetaDescription(sanitize(dto.getMetaDescription()));
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN EDUCATION – /api/me/education
    // ─────────────────────────────────────────────────────────────────────────

    public List<EducationDto> getMyEducation(String username) {
        findUserByUsername(username);
        return educationRepository.findByUserUsernameAndDeletedFalse(username)
                .stream().map(this::mapEducation).collect(Collectors.toList());
    }

    @Transactional
    public EducationDto createEducation(String username, EducationDto dto) {
        User user = findUserByUsername(username);
        Education ed = buildEducation(new Education(), user, dto);
        return mapEducation(educationRepository.save(ed));
    }

    @Transactional
    public EducationDto updateEducation(String username, Long id, EducationDto dto) {
        Education ed = educationRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Education not found"));
        return mapEducation(educationRepository.save(buildEducation(ed, ed.getUser(), dto)));
    }

    @Transactional
    public void deleteEducation(String username, Long id) {
        Education ed = educationRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Education not found"));
        ed.setDeleted(true);
        educationRepository.save(ed);
    }

    private Education buildEducation(Education ed, User user, EducationDto dto) {
        ed.setUser(user);
        ed.setLevel(sanitize(dto.getLevel()));
        ed.setInstitute(sanitize(dto.getInstitute()));
        ed.setLocation(sanitize(dto.getLocation()));
        ed.setDegree(sanitize(dto.getDegree()));
        ed.setScoreLabel(sanitize(dto.getScoreLabel()));
        ed.setScoreValue(sanitize(dto.getScoreValue()));
        ed.setDuration(sanitize(dto.getDuration()));
        ed.setDeleted(false);
        return ed;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN RESUME – /api/me/resume
    // ─────────────────────────────────────────────────────────────────────────

    public ResumeDto getMyResume(String username) {
        User user = findUserByUsername(username);
        Resume resume = resumeRepository.findByUserId(user.getId()).orElse(null);
        return mapResume(resume);
    }

    @Transactional
    public ResumeDto updateResume(String username, ResumeDto dto) {
        User user = findUserByUsername(username);
        Resume resume = resumeRepository.findByUserId(user.getId()).orElse(new Resume());
        resume.setUser(user);
        resume.setResumeUrl(sanitize(dto.getResumeUrl()));
        resume.setLastUpdated(dto.getLastUpdated() == null || dto.getLastUpdated().isBlank()
                ? java.time.OffsetDateTime.now().toString()
                : sanitize(dto.getLastUpdated()));
        return mapResume(resumeRepository.save(resume));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN CONTACT – /api/me/contact
    // ─────────────────────────────────────────────────────────────────────────

    public ContactDto getMyContact(String username) {
        User user = findUserByUsername(username);
        Contact contact = contactRepository.findByUserId(user.getId()).orElse(null);
        return mapContact(contact);
    }

    @Transactional
    public ContactDto updateContact(String username, ContactDto dto) {
        User user = findUserByUsername(username);
        Contact contact = contactRepository.findByUserId(user.getId()).orElse(new Contact());
        contact.setUser(user);
        contact.setPrimaryEmail(sanitize(dto.getPrimaryEmail()));

        List<ContactLink> professional = dto.getProfessionalLinks() != null
                ? dto.getProfessionalLinks().stream()
                        .map(l -> ContactLink.builder()
                                .label(sanitize(l.getLabel()))
                                .url(sanitize(l.getUrl()))
                                .build())
                        .collect(Collectors.toList())
                : List.of();
        contact.setProfessionalLinks(professional);

        List<ContactLink> social = dto.getSocialLinks() != null
                ? dto.getSocialLinks().stream()
                        .map(l -> ContactLink.builder()
                                .label(sanitize(l.getLabel()))
                                .url(sanitize(l.getUrl()))
                                .build())
                        .collect(Collectors.toList())
                : List.of();
        contact.setSocialLinks(social);

        return mapContact(contactRepository.save(contact));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY – kept for backward compatibility (getPortfolioByUsername)
    // ─────────────────────────────────────────────────────────────────────────

    public PortfolioResponse getPortfolioByUsername(String username) {
        User user = findUserByUsername(username);
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

    // -- backward-compat bulk update methods (still used internally) ----------

    @Transactional
    public List<ProjectDto> updateProjects(String username, List<ProjectDto> projectDtos) {
        User user = findUserByUsername(username);
        List<Project> existing = projectRepository.findByUserIdAndDeletedFalse(user.getId());
        existing.forEach(p -> p.setDeleted(true));
        projectRepository.saveAll(existing);

        List<Project> updated = projectDtos.stream()
                .map(dto -> buildProject(new Project(), user, dto))
                .collect(Collectors.toList());
        return projectRepository.saveAll(updated).stream().map(this::mapProject).collect(Collectors.toList());
    }

    @Transactional
    public List<ExperienceDto> updateExperiences(String username, List<ExperienceDto> dtos) {
        User user = findUserByUsername(username);
        List<Experience> existing = experienceRepository.findByUserIdAndDeletedFalse(user.getId());
        existing.forEach(e -> e.setDeleted(true));
        experienceRepository.saveAll(existing);

        List<Experience> updated = dtos.stream()
                .map(dto -> buildExperience(new Experience(), user, dto))
                .collect(Collectors.toList());
        return experienceRepository.saveAll(updated).stream().map(this::mapExperience).collect(Collectors.toList());
    }

    @Transactional
    public List<SkillDto> updateSkills(String username, List<SkillDto> dtos) {
        User user = findUserByUsername(username);
        skillRepository.deleteAll(skillRepository.findByUserId(user.getId()));
        List<Skill> updated = dtos.stream().map(dto -> buildSkill(new Skill(), user, dto)).collect(Collectors.toList());
        return skillRepository.saveAll(updated).stream().map(this::mapSkill).collect(Collectors.toList());
    }

    @Transactional
    public List<EducationDto> updateEducations(String username, List<EducationDto> dtos) {
        User user = findUserByUsername(username);
        List<Education> existing = educationRepository.findByUserId(user.getId());
        existing.forEach(e -> e.setDeleted(true));
        educationRepository.saveAll(existing);
        List<Education> updated = dtos.stream().map(dto -> buildEducation(new Education(), user, dto)).collect(Collectors.toList());
        return educationRepository.saveAll(updated).stream().map(this::mapEducation).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ProfileDto mapProfile(Profile profile) {
        if (profile == null) return null;

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
                .build();
    }

    private AboutDto mapAbout(Profile profile) {
        if (profile == null) return null;

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
            log.warn("Failed to parse about/principles", e);
        }

        return AboutDto.builder()
                .about(aboutList)
                .principles(principlesList)
                .build();
    }

    ProjectDto mapProject(Project project) {
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

    ExperienceDto mapExperience(Experience exp) {
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

    SkillDto mapSkill(Skill skill) {
        return SkillDto.builder()
                .id(skill.getId())
                .domain(skill.getDomain())
                .name(skill.getName())
                .level(skill.getLevel())
                .isMetaSkill(skill.isMetaSkill())
                .metaDescription(skill.getMetaDescription())
                .build();
    }

    EducationDto mapEducation(Education education) {
        if (education == null) return null;
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

    ResumeDto mapResume(Resume resume) {
        if (resume == null) return null;
        return ResumeDto.builder()
                .resumeUrl(resume.getResumeUrl())
                .lastUpdated(resume.getLastUpdated())
                .build();
    }

    ContactDto mapContact(Contact contact) {
        if (contact == null) return null;

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
