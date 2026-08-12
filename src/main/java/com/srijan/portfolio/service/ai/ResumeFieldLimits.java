package com.srijan.portfolio.service.ai;

/**
 * Single source of truth for the limits that resume-AI output must respect.
 *
 * <p>Every value here is the strictest limit that would otherwise <em>block a save</em> — the
 * tighter of the admin UI validation ({@code adminValidation.ts / ADMIN_LIMITS}) and the persisted
 * DTO constraints ({@code ProfileDto}, {@code AboutDto}, {@code SkillDto}, {@code ExperienceDto},
 * {@code EducationDto}, {@code ProjectDto}, {@code CertificationAchievementDto},
 * {@code PrincipleDto}, {@code ContactDto}). Anything above these produces a 400 on save.
 *
 * <p>A few fields additionally carry a {@code *_TARGET}: the admin input's own
 * {@code maxLength}/character counter is tighter than the blocking limit. The AI is asked to aim
 * for the target so the counters read clean, while {@link ResumeLimitEnforcer} only clamps at the
 * hard limit so genuine values are never truncated below what the app accepts.
 */
public final class ResumeFieldLimits {

    private ResumeFieldLimits() {
    }

    // ─── Identity ────────────────────────────────────────────────────────────
    public static final int NAME = 60;
    public static final int EMAIL = 80;
    public static final int PHONE = 30;
    public static final int HEADLINE = 60;
    public static final int LOCATION = 60;
    public static final int LOCATION_TARGET = 20;
    public static final int AVAILABILITY = 30;
    public static final int AVAILABILITY_TARGET = 20;
    public static final int EXPERIENCE_YEARS = 60;
    public static final int EXPERIENCE_YEARS_TARGET = 20;

    // ─── About ───────────────────────────────────────────────────────────────
    public static final int SUMMARY = 300;
    public static final int ABOUT_MAX_PARAGRAPHS = 10;
    /** The admin textarea validates the paragraphs joined by a blank line, as one value. */
    public static final int ABOUT_TOTAL = 1000;
    public static final int ABOUT_PARAGRAPH_TARGET = 300;
    public static final int PRINCIPLES_MAX = 6;
    public static final int PRINCIPLE_TITLE = 30;
    public static final int PRINCIPLE_DESCRIPTION = 125;

    // ─── Skills ──────────────────────────────────────────────────────────────
    public static final int SKILL_NAME = 30;
    public static final int SKILL_DOMAIN = 30;
    public static final int SKILL_META_DESCRIPTION = 100;

    // ─── Experience ──────────────────────────────────────────────────────────
    public static final int EXPERIENCE_COMPANY = 60;
    public static final int EXPERIENCE_ROLE_TITLE = 60;
    public static final int EXPERIENCE_DURATION = 25;
    public static final int EXPERIENCE_LOCATION = 60;
    public static final int RESPONSIBILITIES_MAX = 10;
    public static final int RESPONSIBILITY = 100;
    public static final int ACHIEVEMENTS_MAX = 10;
    public static final int ACHIEVEMENT = 100;
    public static final int EXPERIENCE_SKILLS_MAX = 25;
    public static final int EXPERIENCE_SKILL = 20;

    // ─── Education ───────────────────────────────────────────────────────────
    public static final int EDUCATION_LEVEL = 20;
    public static final int EDUCATION_INSTITUTE = 60;
    public static final int EDUCATION_LOCATION = 60;
    public static final int EDUCATION_DEGREE = 60;
    public static final int EDUCATION_SCORE_LABEL = 20;
    public static final int EDUCATION_SCORE_VALUE = 20;
    public static final int EDUCATION_DURATION = 25;

    // ─── Projects ────────────────────────────────────────────────────────────
    public static final int PROJECT_NAME = 60;
    public static final int PROJECT_TYPE = 60;
    public static final int PROJECT_STATUS = 20;
    public static final int PROJECT_YEAR = 60;
    public static final int PROJECT_YEAR_TARGET = 4;
    public static final int PROJECT_OVERVIEW = 1000;
    public static final int TECH_STACK_MAX = 12;
    public static final int TECH_STACK_ITEM = 20;

    // ─── Certifications & achievements ───────────────────────────────────────
    public static final int CERTIFICATIONS_MAX = 10;
    public static final int CERTIFICATION_TITLE = 30;
    public static final int CERTIFICATION_ISSUER = 60;
    public static final int CERTIFICATION_ISSUER_TARGET = 30;
    public static final int CERTIFICATION_DESCRIPTION = 150;

    // ─── Links ───────────────────────────────────────────────────────────────
    public static final int URL = 255;
    public static final int LINK_LABEL = 60;
    public static final int OTHER_LINKS_MAX = 5;

    // ─── Numeric ranges ──────────────────────────────────────────────────────
    public static final int MIN_YEAR = 1950;
    public static final int MAX_YEAR = 2100;
    public static final int MIN_MONTH = 1;
    public static final int MAX_MONTH = 12;
}
