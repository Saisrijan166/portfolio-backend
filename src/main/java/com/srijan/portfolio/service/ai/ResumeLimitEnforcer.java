package com.srijan.portfolio.service.ai;

import com.srijan.portfolio.config.FlexibleLocalDateDeserializer;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Clamps parsed resume data to {@link ResumeFieldLimits} so a save can never be rejected by the
 * admin UI validation or the persisted DTO constraints.
 *
 * <p>The prompt asks the model to stay inside the limits; this is the deterministic guarantee
 * behind it, and it also covers the non-AI paths (internal parser fallback, previously cached
 * payloads). It never invents or reinterprets content: values are trimmed at a word boundary,
 * over-long lists keep their leading items, and unusable URLs/dates/enums are normalised.
 */
@Component
public class ResumeLimitEnforcer {

    private static final Logger log = LoggerFactory.getLogger(ResumeLimitEnforcer.class);

    private static final String PARAGRAPH_SEPARATOR = "\n\n";
    /** Below this the remaining about-budget cannot hold a readable paragraph, so stop instead. */
    private static final int MIN_ABOUT_PARAGRAPH = 80;
    private static final String ACHIEVEMENT_TYPE = "achievement";
    private static final String CERTIFICATION_TYPE = "certification";

    public ResumeParseResponseDto enforce(ResumeParseResponseDto resume) {
        if (resume == null) {
            return null;
        }

        return ResumeParseResponseDto.builder()
                .name(clamp(resume.getName(), ResumeFieldLimits.NAME))
                .email(clamp(resume.getEmail(), ResumeFieldLimits.EMAIL))
                .phone(clamp(resume.getPhone(), ResumeFieldLimits.PHONE))
                .location(clamp(resume.getLocation(), ResumeFieldLimits.LOCATION))
                .headline(clamp(resume.getHeadline(), ResumeFieldLimits.HEADLINE))
                .summary(clamp(resume.getSummary(), ResumeFieldLimits.SUMMARY))
                .about(enforceAbout(resume.getAbout()))
                .availability(clamp(resume.getAvailability(), ResumeFieldLimits.AVAILABILITY))
                .experienceYears(clamp(resume.getExperienceYears(), ResumeFieldLimits.EXPERIENCE_YEARS))
                .skills(enforceSkills(resume.getSkills()))
                .experience(enforceExperience(resume.getExperience()))
                .education(enforceEducation(resume.getEducation()))
                .projects(enforceProjects(resume.getProjects()))
                .certificationAchievements(enforceCertifications(resume.getCertificationAchievements()))
                .linkedinUrl(clampUrl(resume.getLinkedinUrl()))
                .githubUrl(clampUrl(resume.getGithubUrl()))
                .websiteUrl(clampUrl(resume.getWebsiteUrl()))
                .otherLinks(enforceLinks(resume.getOtherLinks()))
                .principles(enforcePrinciples(resume.getPrinciples()))
                .provider(resume.getProvider())
                .build();
    }

    // ─── Sections ────────────────────────────────────────────────────────────

    /**
     * The admin textarea validates the paragraphs joined by a blank line as a single value, so the
     * combined length is the binding limit, not the per-paragraph one.
     */
    private List<String> enforceAbout(List<String> paragraphs) {
        List<String> result = new ArrayList<>();
        if (paragraphs == null) {
            return result;
        }

        int used = 0;
        for (String paragraph : capCount(paragraphs, ResumeFieldLimits.ABOUT_MAX_PARAGRAPHS, "about paragraphs")) {
            String trimmed = trimToNull(paragraph);
            if (trimmed == null) {
                continue;
            }

            int separator = result.isEmpty() ? 0 : PARAGRAPH_SEPARATOR.length();
            int remaining = ResumeFieldLimits.ABOUT_TOTAL - used - separator;
            if (remaining < Math.min(MIN_ABOUT_PARAGRAPH, trimmed.length())) {
                log.debug("resume.limits.about_truncated dropped_from_index={}", result.size());
                break;
            }

            String fitted = clamp(trimmed, remaining);
            if (fitted == null) {
                break;
            }

            result.add(fitted);
            used += separator + fitted.length();
        }

        return result;
    }

    private List<ResumeParseResponseDto.ParsedSkill> enforceSkills(List<ResumeParseResponseDto.ParsedSkill> skills) {
        List<ResumeParseResponseDto.ParsedSkill> result = new ArrayList<>();
        if (skills == null) {
            return result;
        }

        for (ResumeParseResponseDto.ParsedSkill skill : skills) {
            if (skill == null) {
                continue;
            }
            result.add(ResumeParseResponseDto.ParsedSkill.builder()
                    .name(clamp(skill.getName(), ResumeFieldLimits.SKILL_NAME))
                    .domain(clamp(skill.getDomain(), ResumeFieldLimits.SKILL_DOMAIN))
                    .metaSkill(skill.isMetaSkill())
                    .metaDescription(clamp(skill.getMetaDescription(), ResumeFieldLimits.SKILL_META_DESCRIPTION))
                    .build());
        }

        return result;
    }

    private List<ResumeParseResponseDto.ParsedExperience> enforceExperience(List<ResumeParseResponseDto.ParsedExperience> experience) {
        List<ResumeParseResponseDto.ParsedExperience> result = new ArrayList<>();
        if (experience == null) {
            return result;
        }

        for (ResumeParseResponseDto.ParsedExperience item : experience) {
            if (item == null) {
                continue;
            }

            Integer startYear = clampYear(item.getStartYear());
            Integer endYear = clampYear(item.getEndYear());

            result.add(ResumeParseResponseDto.ParsedExperience.builder()
                    .company(clamp(item.getCompany(), ResumeFieldLimits.EXPERIENCE_COMPANY))
                    .roleTitle(clamp(item.getRoleTitle(), ResumeFieldLimits.EXPERIENCE_ROLE_TITLE))
                    .duration(clamp(
                            firstNonBlank(item.getDuration(), buildDuration(startYear, endYear, item.isCurrent())),
                            ResumeFieldLimits.EXPERIENCE_DURATION))
                    .startMonth(clampMonth(item.getStartMonth()))
                    .startYear(startYear)
                    .endMonth(clampMonth(item.getEndMonth()))
                    .endYear(endYear)
                    .current(item.isCurrent())
                    .responsibilities(clampList(
                            item.getResponsibilities(),
                            ResumeFieldLimits.RESPONSIBILITIES_MAX,
                            ResumeFieldLimits.RESPONSIBILITY,
                            "responsibilities"))
                    .achievements(clampList(
                            item.getAchievements(),
                            ResumeFieldLimits.ACHIEVEMENTS_MAX,
                            ResumeFieldLimits.ACHIEVEMENT,
                            "achievements"))
                    .skills(clampList(
                            item.getSkills(),
                            ResumeFieldLimits.EXPERIENCE_SKILLS_MAX,
                            ResumeFieldLimits.EXPERIENCE_SKILL,
                            "experience skills"))
                    .location(clamp(item.getLocation(), ResumeFieldLimits.EXPERIENCE_LOCATION))
                    .build());
        }

        return result;
    }

    private List<ResumeParseResponseDto.ParsedEducation> enforceEducation(List<ResumeParseResponseDto.ParsedEducation> education) {
        List<ResumeParseResponseDto.ParsedEducation> result = new ArrayList<>();
        if (education == null) {
            return result;
        }

        for (ResumeParseResponseDto.ParsedEducation item : education) {
            if (item == null) {
                continue;
            }

            Integer startYear = clampYear(item.getStartYear());
            Integer endYear = clampYear(item.getEndYear());

            result.add(ResumeParseResponseDto.ParsedEducation.builder()
                    .level(clamp(item.getLevel(), ResumeFieldLimits.EDUCATION_LEVEL))
                    .institute(clamp(item.getInstitute(), ResumeFieldLimits.EDUCATION_INSTITUTE))
                    .location(clamp(item.getLocation(), ResumeFieldLimits.EDUCATION_LOCATION))
                    .degree(clamp(item.getDegree(), ResumeFieldLimits.EDUCATION_DEGREE))
                    .scoreLabel(clamp(item.getScoreLabel(), ResumeFieldLimits.EDUCATION_SCORE_LABEL))
                    .scoreValue(clamp(item.getScoreValue(), ResumeFieldLimits.EDUCATION_SCORE_VALUE))
                    // duration is @NotBlank on EducationDto — derive it from the years when absent
                    // so a single missing value cannot reject the whole education section.
                    .duration(clamp(
                            firstNonBlank(item.getDuration(), buildDuration(startYear, endYear, false)),
                            ResumeFieldLimits.EDUCATION_DURATION))
                    .startYear(startYear)
                    .endYear(endYear)
                    .build());
        }

        return result;
    }

    private List<ResumeParseResponseDto.ParsedProject> enforceProjects(List<ResumeParseResponseDto.ParsedProject> projects) {
        List<ResumeParseResponseDto.ParsedProject> result = new ArrayList<>();
        if (projects == null) {
            return result;
        }

        for (ResumeParseResponseDto.ParsedProject item : projects) {
            if (item == null) {
                continue;
            }
            result.add(ResumeParseResponseDto.ParsedProject.builder()
                    .name(clamp(item.getName(), ResumeFieldLimits.PROJECT_NAME))
                    .type(clamp(item.getType(), ResumeFieldLimits.PROJECT_TYPE))
                    .status(clamp(item.getStatus(), ResumeFieldLimits.PROJECT_STATUS))
                    .year(clamp(item.getYear(), ResumeFieldLimits.PROJECT_YEAR))
                    .overview(clamp(item.getOverview(), ResumeFieldLimits.PROJECT_OVERVIEW))
                    .techStack(clampList(
                            item.getTechStack(),
                            ResumeFieldLimits.TECH_STACK_MAX,
                            ResumeFieldLimits.TECH_STACK_ITEM,
                            "tech stack"))
                    .liveLink(clampUrl(item.getLiveLink()))
                    .sourceLink(clampUrl(item.getSourceLink()))
                    .build());
        }

        return result;
    }

    private List<ResumeParseResponseDto.ParsedCertificationAchievement> enforceCertifications(
            List<ResumeParseResponseDto.ParsedCertificationAchievement> certifications) {
        List<ResumeParseResponseDto.ParsedCertificationAchievement> result = new ArrayList<>();
        if (certifications == null) {
            return result;
        }

        for (ResumeParseResponseDto.ParsedCertificationAchievement item
                : capCount(certifications, ResumeFieldLimits.CERTIFICATIONS_MAX, "certifications")) {
            if (item == null) {
                continue;
            }
            result.add(ResumeParseResponseDto.ParsedCertificationAchievement.builder()
                    .type(normalizeCertificationType(item.getType()))
                    .title(clamp(item.getTitle(), ResumeFieldLimits.CERTIFICATION_TITLE))
                    .issuer(clamp(item.getIssuer(), ResumeFieldLimits.CERTIFICATION_ISSUER))
                    .issuedOn(normalizeIssuedOn(item.getIssuedOn()))
                    .description(clamp(item.getDescription(), ResumeFieldLimits.CERTIFICATION_DESCRIPTION))
                    .referenceUrl(clampUrl(item.getReferenceUrl()))
                    .imageUrl(clampUrl(item.getImageUrl()))
                    .build());
        }

        return result;
    }

    private List<ResumeParseResponseDto.ParsedLink> enforceLinks(List<ResumeParseResponseDto.ParsedLink> links) {
        List<ResumeParseResponseDto.ParsedLink> result = new ArrayList<>();
        if (links == null) {
            return result;
        }

        for (ResumeParseResponseDto.ParsedLink link
                : capCount(links, ResumeFieldLimits.OTHER_LINKS_MAX, "other links")) {
            if (link == null) {
                continue;
            }
            result.add(ResumeParseResponseDto.ParsedLink.builder()
                    .label(clamp(link.getLabel(), ResumeFieldLimits.LINK_LABEL))
                    .url(clampUrl(link.getUrl()))
                    .build());
        }

        return result;
    }

    private List<ResumeParseResponseDto.ParsedPrinciple> enforcePrinciples(List<ResumeParseResponseDto.ParsedPrinciple> principles) {
        List<ResumeParseResponseDto.ParsedPrinciple> result = new ArrayList<>();
        if (principles == null) {
            return result;
        }

        for (ResumeParseResponseDto.ParsedPrinciple principle
                : capCount(principles, ResumeFieldLimits.PRINCIPLES_MAX, "principles")) {
            if (principle == null) {
                continue;
            }
            result.add(ResumeParseResponseDto.ParsedPrinciple.builder()
                    .title(clamp(principle.getTitle(), ResumeFieldLimits.PRINCIPLE_TITLE))
                    .description(clamp(principle.getDescription(), ResumeFieldLimits.PRINCIPLE_DESCRIPTION))
                    .build());
        }

        return result;
    }

    // ─── Primitives ──────────────────────────────────────────────────────────

    /**
     * Trims to {@code max} characters, preferring the last word boundary so a clamped value still
     * reads as a whole phrase.
     */
    private String clamp(String value, int max) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() <= max) {
            return trimmed;
        }
        if (max <= 0) {
            return null;
        }

        String cut = trimmed.substring(0, max);
        int lastSpace = cut.lastIndexOf(' ');
        // Only fall back to the word boundary when it keeps most of the allowance, otherwise a
        // single long token would collapse the value to almost nothing.
        if (lastSpace >= max / 2) {
            cut = cut.substring(0, lastSpace);
        }

        String result = trimToNull(stripTrailingPunctuation(cut));
        if (result == null) {
            return null;
        }

        log.debug("resume.limits.clamped max={} from={} to={}", max, trimmed.length(), result.length());
        return result;
    }

    /**
     * Every persisted URL field is pattern-checked against {@code ^https?://...} (plus
     * {@code mailto:} for link rows), so a bare host has to be given a scheme before it is clamped.
     */
    private String clampUrl(String url) {
        String trimmed = trimToNull(url);
        if (trimmed == null) {
            return null;
        }

        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("mailto:")) {
            trimmed = "https://" + trimmed;
        }

        // Truncated at a word boundary a URL becomes meaningless, so cut it exactly.
        return trimmed.length() <= ResumeFieldLimits.URL
                ? trimmed
                : trimmed.substring(0, ResumeFieldLimits.URL);
    }

    private List<String> clampList(List<String> values, int maxItems, int maxItemLength, String label) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }

        for (String value : capCount(values, maxItems, label)) {
            String clamped = clamp(value, maxItemLength);
            if (clamped != null) {
                result.add(clamped);
            }
        }

        return result;
    }

    private <T> List<T> capCount(List<T> values, int maxItems, String label) {
        if (values == null) {
            return List.of();
        }
        if (values.size() <= maxItems) {
            return values;
        }

        log.debug("resume.limits.list_capped label={} max={} from={}", label, maxItems, values.size());
        return values.subList(0, maxItems);
    }

    private Integer clampMonth(Integer month) {
        if (month == null) {
            return null;
        }
        return Math.min(Math.max(month, ResumeFieldLimits.MIN_MONTH), ResumeFieldLimits.MAX_MONTH);
    }

    private Integer clampYear(Integer year) {
        if (year == null) {
            return null;
        }
        return Math.min(Math.max(year, ResumeFieldLimits.MIN_YEAR), ResumeFieldLimits.MAX_YEAR);
    }

    private String buildDuration(Integer startYear, Integer endYear, boolean current) {
        if (startYear == null && endYear == null) {
            return null;
        }
        if (startYear == null) {
            return String.valueOf(endYear);
        }
        if (current) {
            return startYear + " - Present";
        }
        if (endYear == null || endYear.equals(startYear)) {
            return String.valueOf(startYear);
        }
        return startYear + " - " + endYear;
    }

    private String normalizeCertificationType(String type) {
        String trimmed = trimToNull(type);
        if (trimmed != null && ACHIEVEMENT_TYPE.equalsIgnoreCase(trimmed)) {
            return ACHIEVEMENT_TYPE;
        }
        return CERTIFICATION_TYPE;
    }

    /**
     * {@code CertificationAchievementDto.issuedOn} is a {@code LocalDate}; anything the flexible
     * deserializer cannot read would fail the request, so drop it instead.
     */
    private String normalizeIssuedOn(String issuedOn) {
        String trimmed = trimToNull(issuedOn);
        if (trimmed == null) {
            return null;
        }

        LocalDate parsed = FlexibleLocalDateDeserializer.parseOrNull(trimmed);
        if (parsed == null) {
            log.debug("resume.limits.issued_on_dropped reason=unparseable");
            return null;
        }
        return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String stripTrailingPunctuation(String value) {
        int end = value.length();
        while (end > 0) {
            char character = value.charAt(end - 1);
            if (Character.isLetterOrDigit(character) || character == ')' || character == '%') {
                break;
            }
            end--;
        }
        return value.substring(0, end);
    }

    private String firstNonBlank(String first, String second) {
        String trimmed = trimToNull(first);
        return trimmed != null ? trimmed : trimToNull(second);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
