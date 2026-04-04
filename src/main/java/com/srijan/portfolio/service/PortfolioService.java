package com.srijan.portfolio.service;

import com.srijan.portfolio.config.FlexibleLocalDateDeserializer;
import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.entity.*;
import com.srijan.portfolio.exception.ApiException;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AboutRepository aboutRepository;
    private final ProjectRepository projectRepository;
    private final ExperienceRepository experienceRepository;
    private final CertificationAchievementRepository certificationAchievementRepository;
    private final SkillRepository skillRepository;
    private final EducationRepository educationRepository;
    private final ResumeRepository resumeRepository;
    private final ContactRepository contactRepository;
    private final DesktopWidgetConfigRepository desktopWidgetConfigRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter BOOTSTRAP_TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String sanitize(String value) {
        return value == null ? null : value.trim();
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(this::sanitize)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
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

    private About readAboutSafely(Long userId) {
        try {
            List<About> abouts = aboutRepository.findAllByUserIdOrderByIdAsc(userId);
            return abouts.isEmpty() ? null : abouts.getFirst();
        } catch (Exception exception) {
            log.warn("About table read failed for userId={}", userId, exception);
            return null;
        }
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
        About about = readAboutSafely(userId);
        DesktopWidgetConfig widgets = desktopWidgetConfigRepository.findByUserId(userId).orElse(null);
        Resume resume = resumeRepository.findByUserId(userId).orElse(null);
        Contact contact = contactRepository.findByUserId(userId).orElse(null);
        List<Project> projects = projectRepository.findByUserId(userId);
        List<Experience> experiences = experienceRepository.findByUserId(userId);
        List<CertificationAchievement> certificationAchievements = certificationAchievementRepository.findByUserIdOrderByIdAsc(userId);
        List<Skill> skills = skillRepository.findByUserId(userId);
        List<Education> educations = educationRepository.findByUserId(userId);
        long projectCount = projectRepository.countByUserId(userId);
        long professionalExperienceCount = experienceRepository.countProfessionalByUserId(userId);

        return PortfolioResponse.builder()
                .username(user.getUsername())
                .profile(mapPortfolioIdentity(profile, about))
                .about(mapPortfolioAboutSummary(about))
                .widgets(mapDesktopWidgets(widgets))
                .lastUpdated(resolveLastUpdated(profile, about, widgets, resume, contact, projects, experiences, certificationAchievements, skills, educations))
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
        return mapLegacyProfile(profile);
    }

    @Transactional(readOnly = true)
    public AboutDto getPublicAbout(String username) {
        Optional<User> userOptional = findOptionalUserByUsername(username);
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        About about = readAboutSafely(user.getId());
        return mapAbout(about);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getPublicProjects(String username) {
        if (!publicUserExists(username)) {
            return null;
        }
        return projectRepository.findByUserUsername(sanitize(username))
                .stream().map(this::mapProject).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExperienceDto> getPublicExperience(String username) {
        if (!publicUserExists(username)) {
            return null;
        }
        return experienceRepository.findByUserUsername(sanitize(username))
                .stream().map(this::mapExperience).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificationAchievementDto> getPublicCertificationAchievements(String username) {
        if (!publicUserExists(username)) {
            return null;
        }
        return certificationAchievementRepository.findByUserUsernameOrderByIdAsc(sanitize(username))
                .stream().map(this::mapCertificationAchievement).collect(Collectors.toList());
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
        return educationRepository.findByUserUsername(sanitize(username))
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
        return mapLegacyProfile(profile);
    }

    @Transactional
    public ProfileDto updateProfile(String username, ProfileDto dto) {
        User user = findUserByUsername(username);

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElse(new Profile());

        profile.setUser(user);
        profile.setProfileName(sanitize(dto.getName()));
        profile.setProfileRoleTitle(sanitize(dto.getRoleTitle()));
        profile.setProfileLocation(sanitize(dto.getLocation()));
        profile.setProfileAvailability(sanitize(dto.getAvailability()));
        profile.setOsName(sanitize(dto.getOsName()));
        profile.setAccountType(sanitize(dto.getAccountType()));
        profile.setAccess(sanitize(dto.getAccess()));
        profile.setRoleDescription(sanitize(dto.getRoleDescription()));

        return mapLegacyProfile(profileRepository.save(profile));
    }

    public AboutDto getMyAbout(String username) {
        User user = findUserByUsername(username);
        return mapAbout(readAboutSafely(user.getId()));
    }

    @Transactional
    public AboutDto updateAbout(String username, AboutDto dto) {
        throw new UnsupportedOperationException("Use AboutService for about updates");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN PROJECTS – /api/me/projects
    // ─────────────────────────────────────────────────────────────────────────

    public List<ProjectDto> getMyProjects(String username) {
        findUserByUsername(username);
        return projectRepository.findByUserUsername(username)
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
        projectRepository.delete(p);
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
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN EXPERIENCE – /api/me/experience
    // ─────────────────────────────────────────────────────────────────────────

    public List<ExperienceDto> getMyExperience(String username) {
        findUserByUsername(username);
        return experienceRepository.findByUserUsername(username)
                .stream().map(this::mapExperience).collect(Collectors.toList());
    }

    public List<CertificationAchievementDto> getMyCertificationAchievements(String username) {
        findUserByUsername(username);
        return certificationAchievementRepository.findByUserUsernameOrderByIdAsc(username)
                .stream().map(this::mapCertificationAchievement).collect(Collectors.toList());
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
        experienceRepository.delete(e);
    }

    private Experience buildExperience(Experience e, User user, ExperienceDto dto) {
        Integer currentYear = Year.now().getValue();
        ExperiencePeriod period = normalizeExperiencePeriod(dto);
        if (period.startYear() > currentYear + 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "Start year is not valid");
        }

        e.setUser(user);
        e.setCompany(sanitize(dto.getCompany()));
        e.setRoleTitle(sanitize(dto.getRoleTitle()));
        e.setDuration(period.duration());
        e.setStartMonth(period.startMonth());
        e.setStartYear(period.startYear());
        e.setEndMonth(period.endMonth());
        e.setEndYear(period.endYear());
        e.setCurrent(period.current());
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
        return e;
    }

    @Transactional
    public CertificationAchievementDto createCertificationAchievement(String username, CertificationAchievementDto dto) {
        User user = findUserByUsername(username);
        CertificationAchievement entry = buildCertificationAchievement(new CertificationAchievement(), user, dto);
        return mapCertificationAchievement(certificationAchievementRepository.save(entry));
    }

    @Transactional
    public CertificationAchievementDto updateCertificationAchievement(String username, Long id, CertificationAchievementDto dto) {
        CertificationAchievement entry = certificationAchievementRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Certification or achievement not found"));
        return mapCertificationAchievement(certificationAchievementRepository.save(buildCertificationAchievement(entry, entry.getUser(), dto)));
    }

    @Transactional
    public void deleteCertificationAchievement(String username, Long id) {
        CertificationAchievement entry = certificationAchievementRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Certification or achievement not found"));
        certificationAchievementRepository.delete(entry);
    }

    private CertificationAchievement buildCertificationAchievement(
            CertificationAchievement entry,
            User user,
            CertificationAchievementDto dto
    ) {
        String type = sanitize(dto.getType());
        if (!"certification".equalsIgnoreCase(type) && !"achievement".equalsIgnoreCase(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CERTIFICATION_ACHIEVEMENT_TYPE",
                    "Type must be certification or achievement");
        }

        entry.setUser(user);
        entry.setType(type.toLowerCase(Locale.ROOT));
        entry.setTitle(sanitize(dto.getTitle()));
        entry.setIssuer(sanitize(dto.getIssuer()));
        entry.setIssuedOn(formatIssuedOn(dto.getIssuedOn()));
        entry.setDescription(sanitize(dto.getDescription()));
        entry.setReferenceUrl(sanitize(dto.getReferenceUrl()));
        entry.setImageUrl(sanitize(dto.getImageUrl()));
        return entry;
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
        return educationRepository.findByUserUsername(username)
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
        educationRepository.delete(ed);
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
                : new ArrayList<>();
        contact.setProfessionalLinks(professional);

        List<ContactLink> social = dto.getSocialLinks() != null
                ? dto.getSocialLinks().stream()
                        .map(l -> ContactLink.builder()
                                .label(sanitize(l.getLabel()))
                                .url(sanitize(l.getUrl()))
                                .build())
                        .collect(Collectors.toList())
                : new ArrayList<>();
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
        About about = readAboutSafely(userId);
        List<Project> projects = projectRepository.findByUserId(userId);
        List<Experience> experiences = experienceRepository.findByUserId(userId);
        List<CertificationAchievement> certificationAchievements = certificationAchievementRepository.findByUserIdOrderByIdAsc(userId);
        List<Skill> skills = skillRepository.findByUserId(userId);
        List<Education> educations = educationRepository.findByUserId(userId);
        Resume resume = resumeRepository.findByUserId(userId).orElse(null);
        Contact contact = contactRepository.findByUserId(userId).orElse(null);
        DesktopWidgetConfig widgets = desktopWidgetConfigRepository.findByUserId(userId).orElse(null);

        return PortfolioResponse.builder()
                .username(user.getUsername())
                .profile(mapPortfolioIdentity(profile, about))
                .about(mapPortfolioAboutSummary(about))
                .widgets(mapDesktopWidgets(widgets))
                .projects(projects.stream().map(this::mapProject).collect(Collectors.toList()))
                .experiences(experiences.stream().map(this::mapExperience).collect(Collectors.toList()))
                .certificationAchievements(certificationAchievements.stream().map(this::mapCertificationAchievement).collect(Collectors.toList()))
                .skills(skills.stream().map(this::mapSkill).collect(Collectors.toList()))
                .educations(educations.stream().map(this::mapEducation).collect(Collectors.toList()))
                .resume(mapResume(resume))
                .contact(mapContact(contact))
                .lastUpdated(resolveLastUpdated(profile, about, widgets, resume, contact, projects, experiences, certificationAchievements, skills, educations))
                .build();
    }

    // -- backward-compat bulk update methods (still used internally) ----------

    @Transactional
    public List<ProjectDto> updateProjects(String username, List<ProjectDto> projectDtos) {
        User user = findUserByUsername(username);
        List<Project> existing = projectRepository.findByUserId(user.getId());
        projectRepository.deleteAll(existing);

        List<Project> updated = projectDtos.stream()
                .map(dto -> buildProject(new Project(), user, dto))
                .collect(Collectors.toList());
        return projectRepository.saveAll(updated).stream().map(this::mapProject).collect(Collectors.toList());
    }

    @Transactional
    public List<ExperienceDto> updateExperiences(String username, List<ExperienceDto> dtos) {
        User user = findUserByUsername(username);
        List<Experience> existing = experienceRepository.findByUserId(user.getId());
        experienceRepository.deleteAll(existing);

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
        educationRepository.deleteAll(existing);
        List<Education> updated = dtos.stream().map(dto -> buildEducation(new Education(), user, dto)).collect(Collectors.toList());
        return educationRepository.saveAll(updated).stream().map(this::mapEducation).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────────

    private PortfolioIdentityDto mapPortfolioIdentity(Profile profile, About about) {
        if (profile == null && about == null) return null;

        return PortfolioIdentityDto.builder()
                .name(firstNonBlank(profile != null ? profile.getProfileName() : null, about != null ? about.getName() : null))
                .image(about != null ? about.getImage() : null)
                .build();
    }

    private PortfolioAboutSummaryDto mapPortfolioAboutSummary(About about) {
        if (about == null) {
            return null;
        }

        return PortfolioAboutSummaryDto.builder()
                .name(about.getName())
                .roleTitle(about.getRoleTitle())
                .build();
    }

    private DesktopWidgetsDto mapDesktopWidgets(DesktopWidgetConfig config) {
        if (config == null) {
            return DesktopWidgetsDto.builder()
                    .bottomLeftPrimary(List.of())
                    .bottomLeftSecondary(List.of())
                    .topRight(List.of())
                    .build();
        }

        return DesktopWidgetsDto.builder()
                .bottomLeftPrimary(readStringList(config.getBottomLeftPrimary()))
                .bottomLeftSecondary(readStringList(config.getBottomLeftSecondary()))
                .topRight(readStringList(config.getTopRight()))
                .build();
    }

    private ProfileDto mapLegacyProfile(Profile profile) {
        if (profile == null) return null;

        return ProfileDto.builder()
                .name(profile.getProfileName())
                .roleTitle(profile.getProfileRoleTitle())
                .location(profile.getProfileLocation())
                .availability(profile.getProfileAvailability())
                .osName(profile.getOsName())
                .accountType(profile.getAccountType())
                .access(profile.getAccess())
                .roleDescription(profile.getRoleDescription())
                .build();
    }

    private AboutDto mapAbout(About about) {
        if (about == null) return null;

        List<String> aboutList = null;
        try {
            if (about.getAboutContent() != null) {
                aboutList = objectMapper.readValue(about.getAboutContent(), new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse about/principles", e);
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

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private List<String> readStringList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            log.warn("Failed to parse string list payload", exception);
            return List.of();
        }
    }

    private String resolveLastUpdated(
            Profile profile,
            About about,
            DesktopWidgetConfig widgets,
            Resume resume,
            Contact contact,
            List<Project> projects,
            List<Experience> experiences,
            List<CertificationAchievement> certificationAchievements,
            List<Skill> skills,
            List<Education> educations
    ) {
        return Stream.concat(
                        Stream.of(
                                profile != null ? profile.getUpdatedAt() : null,
                                about != null ? about.getUpdatedAt() : null,
                                widgets != null ? widgets.getUpdatedAt() : null,
                                resume != null ? resume.getUpdatedAt() : null,
                                contact != null ? contact.getUpdatedAt() : null
                        ),
                        Stream.of(
                                projects.stream().map(Project::getUpdatedAt).max(LocalDateTime::compareTo).orElse(null),
                                experiences.stream().map(Experience::getUpdatedAt).max(LocalDateTime::compareTo).orElse(null),
                                certificationAchievements.stream().map(CertificationAchievement::getUpdatedAt).max(LocalDateTime::compareTo).orElse(null),
                                skills.stream().map(Skill::getUpdatedAt).max(LocalDateTime::compareTo).orElse(null),
                                educations.stream().map(Education::getUpdatedAt).max(LocalDateTime::compareTo).orElse(null)
                        )
                )
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .map(BOOTSTRAP_TIMESTAMP_FORMATTER::format)
                .orElse(null);
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
                .startMonth(exp.getStartMonth())
                .startYear(exp.getStartYear())
                .endMonth(exp.getEndMonth())
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

    CertificationAchievementDto mapCertificationAchievement(CertificationAchievement entry) {
        return CertificationAchievementDto.builder()
                .id(entry.getId())
                .type(entry.getType())
                .title(entry.getTitle())
                .issuer(entry.getIssuer())
                .issuedOn(FlexibleLocalDateDeserializer.parseOrNull(entry.getIssuedOn()))
                .description(entry.getDescription())
                .referenceUrl(entry.getReferenceUrl())
                .imageUrl(entry.getImageUrl())
                .build();
    }

    private String formatIssuedOn(LocalDate issuedOn) {
        return issuedOn == null ? null : issuedOn.toString();
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

    private ExperiencePeriod normalizeExperiencePeriod(ExperienceDto dto) {
        ParsedDuration parsedDuration = parseDuration(dto.getDuration());

        Integer startMonth = firstNonNull(dto.getStartMonth(), parsedDuration.startMonth());
        Integer startYear = firstNonNull(dto.getStartYear(), parsedDuration.startYear());
        boolean current = dto.isCurrent() || parsedDuration.current();
        Integer endMonth = current ? null : firstNonNull(dto.getEndMonth(), parsedDuration.endMonth());
        Integer endYear = current ? null : firstNonNull(dto.getEndYear(), parsedDuration.endYear());

        if (startMonth == null || startYear == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "Start duration is required");
        }
        if (startMonth < 1 || startMonth > 12) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "Start month is not valid");
        }
        if (!current && (endMonth == null || endYear == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "End duration is required");
        }
        if (!current && (endMonth < 1 || endMonth > 12)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "End month is not valid");
        }
        if (!current && comparePeriod(startYear, startMonth, endYear, endMonth) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EXPERIENCE_RANGE", "Start duration must be before or equal to end duration");
        }

        return new ExperiencePeriod(
                startMonth,
                startYear,
                current ? null : endMonth,
                current ? null : endYear,
                current,
                formatDuration(startMonth, startYear, current ? null : endMonth, current ? null : endYear, current)
        );
    }

    private ParsedDuration parseDuration(String duration) {
        String value = sanitize(duration);
        if (value == null || value.isBlank()) {
            return ParsedDuration.empty();
        }

        String[] parts = value.split("\\s*[-\u2013\u2014]\\s*");
        if (parts.length != 2) {
            return ParsedDuration.empty();
        }

        ParsedDurationPart start = parseDurationPart(parts[0]);
        if (!start.valid()) {
            return ParsedDuration.empty();
        }

        String endToken = sanitize(parts[1]);
        if (endToken != null && endToken.equalsIgnoreCase("present")) {
            return new ParsedDuration(start.month(), start.year(), null, null, true);
        }

        ParsedDurationPart end = parseDurationPart(parts[1]);
        if (!end.valid()) {
            return ParsedDuration.empty();
        }

        return new ParsedDuration(start.month(), start.year(), end.month(), end.year(), false);
    }

    private ParsedDurationPart parseDurationPart(String rawValue) {
        String value = sanitize(rawValue);
        if (value == null || value.isBlank()) {
            return ParsedDurationPart.invalid();
        }

        String[] tokens = value.split("\\s+");
        if (tokens.length == 2) {
            Integer month = parseMonthToken(tokens[0]);
            Integer year = parseInteger(tokens[1]);
            return month != null && year != null
                    ? new ParsedDurationPart(month, year, true)
                    : ParsedDurationPart.invalid();
        }
        if (tokens.length == 1) {
            Integer year = parseInteger(tokens[0]);
            return year != null ? new ParsedDurationPart(1, year, true) : ParsedDurationPart.invalid();
        }
        return ParsedDurationPart.invalid();
    }

    private Integer parseMonthToken(String rawValue) {
        String normalized = sanitize(rawValue);
        if (normalized == null || normalized.length() < 3) {
            return null;
        }
        try {
            return Month.valueOf(normalized.substring(0, 3).toUpperCase(Locale.ENGLISH)).getValue();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Integer parseInteger(String rawValue) {
        try {
            return Integer.valueOf(rawValue);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int comparePeriod(int startYear, int startMonth, int endYear, int endMonth) {
        int start = startYear * 100 + startMonth;
        int end = endYear * 100 + endMonth;
        return Integer.compare(start, end);
    }

    private String formatDuration(Integer startMonth, Integer startYear, Integer endMonth, Integer endYear, boolean current) {
        return formatMonthYear(startMonth, startYear) + " - " + (current ? "Present" : formatMonthYear(endMonth, endYear));
    }

    private String formatMonthYear(Integer month, Integer year) {
        Month monthEnum = Month.of(month);
        String monthLabel = monthEnum.name().substring(0, 1) + monthEnum.name().substring(1, 3).toLowerCase(Locale.ENGLISH);
        return monthLabel + " " + year;
    }

    private Integer firstNonNull(Integer primary, Integer fallback) {
        return primary != null ? primary : fallback;
    }

    private record ExperiencePeriod(
            Integer startMonth,
            Integer startYear,
            Integer endMonth,
            Integer endYear,
            boolean current,
            String duration
    ) {
    }

    private record ParsedDuration(Integer startMonth, Integer startYear, Integer endMonth, Integer endYear, boolean current) {
        static ParsedDuration empty() {
            return new ParsedDuration(null, null, null, null, false);
        }
    }

    private record ParsedDurationPart(Integer month, Integer year, boolean valid) {
        static ParsedDurationPart invalid() {
            return new ParsedDurationPart(null, null, false);
        }
    }
}
