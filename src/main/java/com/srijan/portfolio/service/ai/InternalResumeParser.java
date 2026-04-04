package com.srijan.portfolio.service.ai;

import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.dto.ResumeParseResponseDto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Internal resume parser — final fallback when all AI providers fail.
 * Uses regex and heuristic-based extraction. No AI generation.
 * Focuses on maximum accuracy: leaves fields empty rather than guessing.
 */
@Component
public class InternalResumeParser {

    private static final Logger log = LoggerFactory.getLogger(InternalResumeParser.class);

    // ─── Regex patterns ──────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{1,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}(?:\\s*(?:ext|x)\\.?\\s*\\d+)?");

    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?linkedin\\.com/in/[\\w\\-]+/?", Pattern.CASE_INSENSITIVE);

    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?github\\.com/[\\w\\-]+/?", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-]+(\\.[\\w\\-]+)+[/\\w\\-.?=&#%]*");

    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile(
            "(\\d{4})\\s*[-–—to]+\\s*(\\d{4}|[Pp]resent|[Cc]urrent|[Nn]ow)");

    private static final Pattern MONTH_YEAR_PATTERN = Pattern.compile(
            "(?:(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\.?\\s*)(\\d{4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SINGLE_YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern BULLET_PREFIX_PATTERN = Pattern.compile("^[\\-*►▪●○◆→•\\s]+");
    private static final Pattern SCORE_PATTERN = Pattern.compile(
            "(?:GPA|CGPA|Percentage|Score|Grade)[:\\s]*([\\d.]+(?:/[\\d.]+)?%?)",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> ROLE_KEYWORDS = List.of(
            "developer", "engineer", "manager", "analyst", "consultant", "intern", "designer",
            "architect", "lead", "specialist", "administrator", "tester", "founder", "freelancer",
            "associate", "coordinator", "executive", "researcher", "programmer");

    private static final List<String> ORGANIZATION_KEYWORDS = List.of(
            "inc", "llc", "ltd", "pvt", "private", "corp", "corporation", "company", "solutions",
            "systems", "technologies", "technology", "tech", "labs", "lab", "software", "services",
            "studio", "agency", "group", "university", "college", "school", "institute");

    private static final List<String> DEGREE_KEYWORDS = List.of(
            "bachelor", "master", "b.tech", "m.tech", "b.e.", "m.e.", "b.sc", "m.sc", "bca",
            "mca", "mba", "phd", "doctorate", "diploma", "associate", "computer science",
            "engineering", "degree", "graduation");

    // ─── Section detection map (normalized key → keyword list) ────────────────
    private static final Map<String, List<String>> SECTION_KEYWORDS = new LinkedHashMap<>();

    static {
        SECTION_KEYWORDS.put("summary", List.of(
                "summary", "objective", "profile", "about", "about me",
                "professional summary", "career objective", "introduction",
                "personal statement", "career summary", "professional profile"));
        SECTION_KEYWORDS.put("experience", List.of(
                "experience", "work experience", "professional experience", "employment",
                "work history", "career history", "positions held", "employment history",
                "relevant experience", "professional background"));
        SECTION_KEYWORDS.put("education", List.of(
                "education", "academic", "academics", "qualifications", "educational background",
                "academic background", "academic qualifications", "schooling"));
        SECTION_KEYWORDS.put("skills", List.of(
                "skills", "technical skills", "core competencies", "competencies",
                "technologies", "tech stack", "tools", "proficiencies",
                "areas of expertise", "key skills", "professional skills",
                "technical proficiencies", "programming languages", "frameworks"));
        SECTION_KEYWORDS.put("projects", List.of(
                "projects", "personal projects", "academic projects", "portfolio",
                "key projects", "notable projects", "side projects", "selected projects"));
        SECTION_KEYWORDS.put("certifications", List.of(
                "certifications", "certificates", "courses", "training",
                "professional development", "licenses", "credentials",
                "professional certifications", "online courses"));
        SECTION_KEYWORDS.put("achievements", List.of(
                "achievements", "accomplishments", "awards", "honors",
                "recognition", "distinctions", "accolades"));
        SECTION_KEYWORDS.put("languages", List.of(
                "languages", "language proficiency", "spoken languages"));
        SECTION_KEYWORDS.put("interests", List.of(
                "interests", "hobbies", "activities", "extracurricular"));
        SECTION_KEYWORDS.put("volunteer", List.of(
                "volunteer", "volunteering", "community service", "volunteer experience"));
        SECTION_KEYWORDS.put("publications", List.of(
                "publications", "papers", "research papers", "articles"));
        SECTION_KEYWORDS.put("references", List.of(
                "references", "referees"));
    }

    private static final Map<String, Integer> MONTH_MAP = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("feb", 2), Map.entry("mar", 3),
            Map.entry("apr", 4), Map.entry("may", 5), Map.entry("jun", 6),
            Map.entry("jul", 7), Map.entry("aug", 8), Map.entry("sep", 9),
            Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dec", 12));

    // ─── Main parse method ───────────────────────────────────────────────────

    public ResumeParseResponseDto parse(String text) {
        if (text == null || text.isBlank()) {
            return ResumeParseResponseDto.builder().provider("internal").build();
        }

        String[] lines = text.split("\\n");
        Map<String, List<String>> sections = identifySections(lines);

        log.debug("Identified sections: {}", sections.keySet());

        return ResumeParseResponseDto.builder()
                .name(extractName(lines))
                .email(extractFirst(text, EMAIL_PATTERN))
                .phone(extractPhone(text))
                .location(extractLocation(lines))
                .headline(extractHeadline(lines))
                .summary(extractSectionText(sections, "summary"))
                .about(new ArrayList<>())
                .skills(extractSkills(sections))
                .experience(extractExperiences(sections))
                .education(extractEducation(sections))
                .projects(extractProjects(sections))
                .certifications(extractCertifications(sections))
                .linkedinUrl(extractFirst(text, LINKEDIN_PATTERN))
                .githubUrl(extractFirst(text, GITHUB_PATTERN))
                .websiteUrl(extractWebsite(text))
                .otherLinks(extractOtherLinks(text))
                .principles(new ArrayList<>())
                .provider("internal")
                .build();
    }

    // ─── Section identification ──────────────────────────────────────────────

    private Map<String, List<String>> identifySections(String[] lines) {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentSection = "header";
        List<String> currentLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                currentLines.add("");
                continue;
            }

            String sectionKey = detectSectionHeader(trimmed);
            if (sectionKey != null) {
                sections.put(currentSection, currentLines);
                currentSection = sectionKey;
                currentLines = new ArrayList<>();
            } else {
                currentLines.add(trimmed);
            }
        }
        sections.put(currentSection, currentLines);
        return sections;
    }

    private String detectSectionHeader(String line) {
        // Section headers are typically short, often uppercase or decorated
        if (line.length() > 80)
            return null;

        String lower = line.toLowerCase()
                .replaceAll("[:\\-–—|_*#=]+", "")
                .trim();

        if (lower.isEmpty())
            return null;

        for (Map.Entry<String, List<String>> entry : SECTION_KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.equals(kw) || lower.startsWith(kw + " ") || lower.endsWith(" " + kw)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    // ─── Extraction methods ──────────────────────────────────────────────────

    private String extractName(String[] lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;
            if (trimmed.length() < 2 || trimmed.length() > 60)
                continue;
            if (EMAIL_PATTERN.matcher(trimmed).find())
                continue;
            if (URL_PATTERN.matcher(trimmed).find())
                continue;

            // Skip phone-only lines
            String digitsOnly = trimmed.replaceAll("\\D", "");
            if (digitsOnly.length() >= 7 && trimmed.length() < 20)
                continue;

            // Skip lines that look like section headers
            String lower = trimmed.toLowerCase();
            if (lower.startsWith("resume") || lower.startsWith("curriculum") || lower.startsWith("cv"))
                continue;

            // Skip lines that are section headers
            if (detectSectionHeader(trimmed) != null)
                continue;

            return trimmed;
        }
        return null;
    }

    private String extractHeadline(String[] lines) {
        // Headline is typically line 2 or 3, after name and before contact info
        boolean nameFound = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;

            // Skip short/long lines
            if (trimmed.length() < 3 || trimmed.length() > 80)
                continue;

            // Skip contact info
            if (EMAIL_PATTERN.matcher(trimmed).find())
                continue;
            if (URL_PATTERN.matcher(trimmed).find())
                continue;
            String digitsOnly = trimmed.replaceAll("\\D", "");
            if (digitsOnly.length() >= 7 && trimmed.length() < 20)
                continue;

            // Skip section headers
            if (detectSectionHeader(trimmed) != null)
                continue;

            if (!nameFound) {
                nameFound = true;
                continue; // skip name line
            }

            // This line is likely the headline/role
            // Verify it looks like a role (not a location or address)
            String lower = trimmed.toLowerCase();
            if (lower.contains(",") && !lower.contains("developer") && !lower.contains("engineer")
                    && !lower.contains("designer") && !lower.contains("manager")
                    && !lower.contains("analyst") && !lower.contains("intern")) {
                continue; // likely a location
            }

            return trimmed;
        }
        return null;
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            String phone = matcher.group().trim();
            String digits = phone.replaceAll("\\D", "");
            if (digits.length() >= 7 && digits.length() <= 15) {
                return phone;
            }
        }
        return null;
    }

    private String extractLocation(String[] lines) {
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            String line = lines[i].trim();
            if (line.isEmpty())
                continue;
            if (EMAIL_PATTERN.matcher(line).find())
                continue;
            if (URL_PATTERN.matcher(line).find())
                continue;

            // Check for "Address:" or "Location:" prefix
            String lower = line.toLowerCase();
            if (lower.startsWith("address:") || lower.startsWith("location:")) {
                return line.substring(line.indexOf(':') + 1).trim();
            }

            // Look for pipe/bullet separated parts containing comma patterns
            String[] parts = line.split("[|•·▪►]");
            for (String part : parts) {
                String p = part.trim();
                if (p.contains(",") && p.length() < 60 && !p.contains("@")) {
                    // Verify it looks like a location, not a skill list
                    String[] commaParts = p.split(",");
                    if (commaParts.length <= 3) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private String extractFirst(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private String extractSectionText(Map<String, List<String>> sections, String sectionKey) {
        List<String> lines = sections.get(sectionKey);
        if (lines != null && !lines.isEmpty()) {
            return lines.stream()
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.joining(" "))
                    .trim();
        }
        return null;
    }

    // ─── Skills extraction ───────────────────────────────────────────────────

    private List<String> extractSkills(Map<String, List<String>> sections) {
        List<String> allSkills = new ArrayList<>();

        List<String> lines = sections.get("skills");
        if (lines == null || lines.isEmpty())
            return allSkills;

        for (String line : lines) {
            if (line.isEmpty())
                continue;

            // Remove label prefixes like "Languages:", "Frameworks:", etc.
            String cleaned = line.replaceFirst("(?i)^(?:languages|frameworks|tools|databases|" +
                    "technologies|libraries|platforms|operating systems|methodologies|" +
                    "programming|frontend|backend|devops|cloud|testing)[:\\s-]*", "");

            // Skills are often comma, pipe, bullet, or semicolon separated
            String[] parts = cleaned.split("[,|•·;/]");
            for (String part : parts) {
                String skill = part.replaceAll("^[\\-*►▪●○◆→\\s]+", "").trim();
                if (!skill.isEmpty() && skill.length() < 60 && skill.length() > 1) {
                    allSkills.add(skill);
                }
            }
        }

        return allSkills.stream().distinct().collect(Collectors.toList());
    }

    // ─── Experience extraction ───────────────────────────────────────────────

    private List<ParsedExperience> extractExperiences(Map<String, List<String>> sections) {
        List<String> lines = sections.get("experience");
        if (lines == null || lines.isEmpty())
            return new ArrayList<>();

        List<ParsedExperience> experiences = new ArrayList<>();
        List<String> currentBlock = new ArrayList<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                if (!currentBlock.isEmpty()) {
                    ParsedExperience exp = parseExperienceBlock(currentBlock);
                    if (exp != null)
                        experiences.add(exp);
                    currentBlock.clear();
                }
                continue;
            }

            // Check if this line starts a new entry (has date pattern, not a bullet)
            boolean isBullet = line.startsWith("-") || line.startsWith("•") || line.startsWith("*")
                    || line.startsWith("►") || line.startsWith("→");
            boolean hasDate = YEAR_RANGE_PATTERN.matcher(line).find() || MONTH_YEAR_PATTERN.matcher(line).find();

            if (hasDate && !isBullet && !currentBlock.isEmpty() && currentBlock.stream().anyMatch(this::containsDateToken)) {
                ParsedExperience exp = parseExperienceBlock(currentBlock);
                if (exp != null)
                    experiences.add(exp);
                currentBlock.clear();
            }

            currentBlock.add(line);
        }

        if (!currentBlock.isEmpty()) {
            ParsedExperience exp = parseExperienceBlock(currentBlock);
            if (exp != null)
                experiences.add(exp);
        }

        return experiences;
    }

    private ParsedExperience parseExperienceBlock(List<String> lines) {
        if (lines.isEmpty())
            return null;

        ParsedExperience.ParsedExperienceBuilder builder = ParsedExperience.builder()
                .responsibilities(new ArrayList<>())
                .achievements(new ArrayList<>())
                .skills(new ArrayList<>())
                .current(false);

        List<String> responsibilities = new ArrayList<>();
        List<String> headerLines = new ArrayList<>();
        boolean reachedBullets = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            applyExperienceDates(builder, line);

            boolean isBullet = BULLET_PREFIX_PATTERN.matcher(line).find()
                    && (rawLine.stripLeading().startsWith("-")
                    || rawLine.stripLeading().startsWith("•")
                    || rawLine.stripLeading().startsWith("*")
                    || rawLine.stripLeading().startsWith("►")
                    || rawLine.stripLeading().startsWith("→"));

            String cleaned = stripDecorations(line);
            if (cleaned.isEmpty()) {
                continue;
            }

            if (isBullet) {
                reachedBullets = true;
                responsibilities.add(cleaned);
                continue;
            }

            if (!reachedBullets && headerLines.size() < 3) {
                headerLines.add(line);
                continue;
            }

            responsibilities.add(cleaned);
        }

        populateExperienceHeader(builder, headerLines);
        builder.responsibilities(responsibilities);
        ParsedExperience exp = builder.build();

        // Only return if we extracted meaningful data
        if (exp.getRoleTitle() != null || exp.getCompany() != null) {
            return exp;
        }
        return null;
    }

    // ─── Education extraction ────────────────────────────────────────────────

    private List<ParsedEducation> extractEducation(Map<String, List<String>> sections) {
        List<String> lines = sections.get("education");
        if (lines == null || lines.isEmpty())
            return new ArrayList<>();

        List<ParsedEducation> educations = new ArrayList<>();
        List<String> block = new ArrayList<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                if (!block.isEmpty()) {
                    ParsedEducation edu = parseEducationBlock(block);
                    if (edu != null)
                        educations.add(edu);
                    block.clear();
                }
            } else {
                block.add(line);
            }
        }

        if (!block.isEmpty()) {
            ParsedEducation edu = parseEducationBlock(block);
            if (edu != null)
                educations.add(edu);
        }

        return educations;
    }

    private ParsedEducation parseEducationBlock(List<String> lines) {
        if (lines.isEmpty())
            return null;

        ParsedEducation.ParsedEducationBuilder builder = ParsedEducation.builder();

        String allText = String.join(" ", lines).toLowerCase();

        // Detect degree/level first to determine which line is what
        String level = detectEducationLevel(allText);
        builder.level(level);

        List<String> leftovers = new ArrayList<>();
        String degree = null;
        String institute = null;
        String location = null;
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            applyEducationDates(builder, line);
            applyEducationScore(builder, line);

            for (String token : splitHeaderTokens(line)) {
                String cleaned = stripDecorations(stripDatesAndScores(token));
                if (cleaned.isEmpty()) {
                    continue;
                }

                if (location == null && looksLikeLocation(cleaned)) {
                    location = cleaned;
                    continue;
                }

                if (degree == null && looksLikeDegree(cleaned)) {
                    degree = cleaned;
                    continue;
                }

                if (institute == null && looksLikeInstitute(cleaned)) {
                    institute = cleaned;
                    continue;
                }

                leftovers.add(cleaned);
            }
        }

        for (String candidate : leftovers) {
            if (degree == null && looksLikeDegree(candidate)) {
                degree = candidate;
                continue;
            }
            if (institute == null) {
                institute = candidate;
                continue;
            }
            if (degree == null) {
                degree = candidate;
            }
        }

        builder.degree(degree);
        builder.institute(institute);
        builder.location(location);
        return builder.build();
    }

    private String detectEducationLevel(String text) {
        if (text.contains("phd") || text.contains("doctorate") || text.contains("doctor of"))
            return "PhD";
        if (text.contains("master") || text.contains("m.s.") || text.contains("mba")
                || text.contains("m.tech") || text.contains("m.sc") || text.contains("m.a."))
            return "Masters";
        if (text.contains("bachelor") || text.contains("b.s.") || text.contains("b.tech")
                || text.contains("b.e.") || text.contains("b.sc") || text.contains("b.a.")
                || text.contains("bca") || text.contains("bcom"))
            return "Bachelors";
        if (text.contains("diploma") || text.contains("associate"))
            return "Diploma";
        if (text.contains("high school") || text.contains("secondary") || text.contains("12th")
                || text.contains("10th") || text.contains("higher secondary"))
            return "High School";
        return null;
    }

    // ─── Projects extraction ─────────────────────────────────────────────────

    private List<ParsedProject> extractProjects(Map<String, List<String>> sections) {
        List<String> lines = sections.get("projects");
        if (lines == null || lines.isEmpty())
            return new ArrayList<>();

        List<ParsedProject> projects = new ArrayList<>();
        List<String> block = new ArrayList<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                if (!block.isEmpty()) {
                    ParsedProject proj = parseProjectBlock(block);
                    if (proj != null)
                        projects.add(proj);
                    block.clear();
                }
            } else {
                block.add(line);
            }
        }

        if (!block.isEmpty()) {
            ParsedProject proj = parseProjectBlock(block);
            if (proj != null)
                projects.add(proj);
        }

        return projects;
    }

    private ParsedProject parseProjectBlock(List<String> lines) {
        if (lines.isEmpty())
            return null;

        ParsedProject.ParsedProjectBuilder builder = ParsedProject.builder()
                .type("Personal")
                .status("Completed")
                .techStack(new ArrayList<>());

        // First line is project name
        String name = lines.get(0).replaceAll("^[\\-*►▪●○◆→•]+\\s*", "").trim();
        // Remove trailing URLs from name
        name = name.replaceAll("\\s*https?://\\S+", "").trim();
        builder.name(name);

        StringBuilder desc = new StringBuilder();
        List<String> techStack = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String cleaned = line.replaceAll("^[\\-*►▪●○◆→•]+\\s*", "").trim();
            String lower = cleaned.toLowerCase();

            // Check for tech stack indicators
            if (lower.startsWith("tech") || lower.startsWith("built with") || lower.startsWith("stack")
                    || lower.startsWith("tools:") || lower.startsWith("using:")) {
                String techPart = cleaned.replaceFirst(
                        "(?i)^(?:technologies?|tech stack|built with|stack|tools|using)[:\\s]*", "");
                for (String t : techPart.split("[,|•·;]")) {
                    String tech = t.trim();
                    if (!tech.isEmpty() && tech.length() < 50)
                        techStack.add(tech);
                }
                continue;
            }

            // Extract URLs
            Matcher urlMatcher = URL_PATTERN.matcher(cleaned);
            while (urlMatcher.find()) {
                String url = urlMatcher.group();
                if (url.contains("github.com")) {
                    builder.sourceLink(url);
                } else {
                    builder.liveLink(url);
                }
            }

            // Accumulate description
            String descLine = cleaned.replaceAll("https?://\\S+", "").trim();
            if (!descLine.isEmpty() && desc.length() < 500) {
                if (desc.length() > 0)
                    desc.append(" ");
                desc.append(descLine);
            }
        }

        builder.techStack(techStack);
        builder.overview(desc.toString().trim());

        // Extract year from any line
        for (String line : lines) {
            Matcher yearMatcher = SINGLE_YEAR_PATTERN.matcher(line);
            if (yearMatcher.find()) {
                builder.year(yearMatcher.group());
                break;
            }
        }

        return builder.build();
    }

    // ─── Certifications extraction ───────────────────────────────────────────

    private List<String> extractCertifications(Map<String, List<String>> sections) {
        List<String> certs = new ArrayList<>();

        // Check both certifications and achievements sections
        for (String key : List.of("certifications", "achievements")) {
            List<String> lines = sections.get(key);
            if (lines == null)
                continue;

            for (String line : lines) {
                if (line.isEmpty())
                    continue;
                String cleaned = line.replaceAll("^[\\-*►▪●○◆→•\\s]+", "").trim();
                if (!cleaned.isEmpty() && cleaned.length() > 2) {
                    certs.add(cleaned);
                }
            }
        }

        return certs;
    }

    // ─── Link extraction ─────────────────────────────────────────────────────

    private String extractWebsite(String text) {
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            if (!url.contains("linkedin.com") && !url.contains("github.com")
                    && !url.contains("twitter.com") && !url.contains("facebook.com")
                    && !url.contains("instagram.com") && !url.contains("mailto:")) {
                return url;
            }
        }
        return null;
    }

    private List<ParsedLink> extractOtherLinks(String text) {
        List<ParsedLink> links = new ArrayList<>();
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        Set<String> seen = new HashSet<>();

        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            if (seen.contains(url))
                continue;
            seen.add(url);

            if (url.contains("linkedin.com") || url.contains("github.com"))
                continue;

            String label = detectLinkLabel(url);
            links.add(ParsedLink.builder().label(label).url(url).build());
        }

        return links;
    }

    private String detectLinkLabel(String url) {
        if (url.contains("twitter.com") || url.contains("x.com"))
            return "Twitter";
        if (url.contains("medium.com"))
            return "Medium";
        if (url.contains("stackoverflow.com"))
            return "Stack Overflow";
        if (url.contains("behance.net"))
            return "Behance";
        if (url.contains("dribbble.com"))
            return "Dribbble";
        if (url.contains("leetcode.com"))
            return "LeetCode";
        if (url.contains("hackerrank.com"))
            return "HackerRank";
        if (url.contains("codepen.io"))
            return "CodePen";
        if (url.contains("youtube.com"))
            return "YouTube";
        if (url.contains("dev.to"))
            return "Dev.to";
        if (url.contains("hashnode."))
            return "Hashnode";
        if (url.contains("kaggle.com"))
            return "Kaggle";
        if (url.contains("codeforces.com"))
            return "Codeforces";
        if (url.contains("figma.com"))
            return "Figma";
        if (url.contains("notion."))
            return "Notion";
        return "Website";
    }

    private void applyExperienceDates(ParsedExperience.ParsedExperienceBuilder builder, String line) {
        Matcher yearRange = YEAR_RANGE_PATTERN.matcher(line);
        boolean hasYearRange = yearRange.find();
        if (hasYearRange) {
            try {
                builder.startYear(Integer.parseInt(yearRange.group(1)));
                String end = yearRange.group(2);
                if (end.matches("\\d{4}")) {
                    builder.endYear(Integer.parseInt(end));
                } else {
                    builder.current(true);
                }
            } catch (NumberFormatException ignored) {
            }
            if (builder.build().getDuration() == null) {
                builder.duration(yearRange.group(0));
            }
        }

        Matcher monthMatcher = MONTH_YEAR_PATTERN.matcher(line);
        Integer startMonth = null;
        Integer endMonth = null;
        Integer startYear = builder.build().getStartYear();
        Integer endYear = builder.build().getEndYear();
        while (monthMatcher.find()) {
            String monthStr = monthMatcher.group(1).toLowerCase().substring(0, 3);
            Integer month = MONTH_MAP.get(monthStr);
            int year = Integer.parseInt(monthMatcher.group(2));
            if (startMonth == null) {
                startMonth = month;
                startYear = year;
            } else {
                endMonth = month;
                endYear = year;
            }
        }

        if (startMonth != null) {
            builder.startMonth(startMonth);
            builder.startYear(startYear);
        }
        if (endMonth != null) {
            builder.endMonth(endMonth);
            builder.endYear(endYear);
        }
        if (builder.build().getDuration() == null && (startMonth != null || hasYearRange)) {
            builder.duration(line.trim());
        }
    }

    private void populateExperienceHeader(ParsedExperience.ParsedExperienceBuilder builder, List<String> headerLines) {
        String role = null;
        String company = null;
        String location = null;
        List<String> leftovers = new ArrayList<>();

        for (String line : headerLines) {
            String cleanedLine = stripDecorations(stripDatesAndScores(line));
            if (cleanedLine.isEmpty()) {
                continue;
            }

            if (builder.build().getDuration() == null && containsDateToken(line)) {
                builder.duration(line.trim());
            }

            if (cleanedLine.matches("(?i).+\\s+(?:at|@)\\s+.+")) {
                String[] parts = cleanedLine.split("(?i)\\s+(?:at|@)\\s+", 2);
                role = firstNonBlank(role, parts[0].trim());
                company = firstNonBlank(company, parts[1].trim());
                continue;
            }

            List<String> tokens = splitHeaderTokens(cleanedLine).stream()
                    .map(this::stripDecorations)
                    .filter(token -> !token.isBlank())
                    .toList();

            if (tokens.size() >= 2) {
                String first = tokens.get(0);
                String second = tokens.get(1);

                if (looksLikeLocation(second)) {
                    role = firstNonBlank(role, first);
                    location = firstNonBlank(location, second);
                } else if (looksLikeOrganization(first) && looksLikeRole(second)) {
                    company = firstNonBlank(company, first);
                    role = firstNonBlank(role, second);
                } else {
                    role = firstNonBlank(role, first);
                    company = firstNonBlank(company, second);
                }

                if (tokens.size() >= 3 && location == null && looksLikeLocation(tokens.get(2))) {
                    location = tokens.get(2);
                }
                continue;
            }

            if (looksLikeLocation(cleanedLine) && location == null) {
                location = cleanedLine;
                continue;
            }

            leftovers.add(cleanedLine);
        }

        for (String candidate : leftovers) {
            if (role == null && looksLikeRole(candidate)) {
                role = candidate;
                continue;
            }
            if (company == null && looksLikeOrganization(candidate)) {
                company = candidate;
                continue;
            }
            if (role == null) {
                role = candidate;
                continue;
            }
            if (company == null) {
                company = candidate;
                continue;
            }
            if (location == null && looksLikeLocation(candidate)) {
                location = candidate;
            }
        }

        builder.roleTitle(role);
        builder.company(company);
        builder.location(location);
    }

    private void applyEducationDates(ParsedEducation.ParsedEducationBuilder builder, String line) {
        Matcher yearRange = YEAR_RANGE_PATTERN.matcher(line);
        if (yearRange.find()) {
            try {
                builder.startYear(Integer.parseInt(yearRange.group(1)));
                String end = yearRange.group(2);
                if (end.matches("\\d{4}")) {
                    builder.endYear(Integer.parseInt(end));
                }
            } catch (NumberFormatException ignored) {
            }
            builder.duration(yearRange.group(0));
        }
    }

    private void applyEducationScore(ParsedEducation.ParsedEducationBuilder builder, String line) {
        Matcher scoreMatcher = SCORE_PATTERN.matcher(line);
        if (scoreMatcher.find()) {
            String labelPart = scoreMatcher.group(0).split("[:\\s]")[0];
            builder.scoreLabel(labelPart);
            builder.scoreValue(scoreMatcher.group(1));
        }
    }

    private List<String> splitHeaderTokens(String value) {
        return Arrays.stream(value.split("\\s*[|•·▪]+\\s*|\\s+[–—-]\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String stripDecorations(String value) {
        return BULLET_PREFIX_PATTERN.matcher(value)
                .replaceFirst("")
                .replaceAll("[|•·▪,\\-–—]+$", "")
                .trim();
    }

    private String stripDatesAndScores(String value) {
        return SCORE_PATTERN.matcher(
                MONTH_YEAR_PATTERN.matcher(
                        YEAR_RANGE_PATTERN.matcher(value).replaceAll("")
                ).replaceAll("")
        ).replaceAll("").replaceAll("\\b(19|20)\\d{2}\\b", "").trim();
    }

    private boolean containsDateToken(String value) {
        return YEAR_RANGE_PATTERN.matcher(value).find()
                || MONTH_YEAR_PATTERN.matcher(value).find()
                || SINGLE_YEAR_PATTERN.matcher(value).find();
    }

    private boolean looksLikeRole(String value) {
        String lower = value.toLowerCase();
        return ROLE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private boolean looksLikeOrganization(String value) {
        String lower = value.toLowerCase();
        if (ORGANIZATION_KEYWORDS.stream().anyMatch(lower::contains)) {
            return true;
        }
        return !looksLikeRole(value) && value.split("\\s+").length >= 2 && Character.isUpperCase(value.charAt(0));
    }

    private boolean looksLikeDegree(String value) {
        String lower = value.toLowerCase();
        return DEGREE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private boolean looksLikeInstitute(String value) {
        String lower = value.toLowerCase();
        return ORGANIZATION_KEYWORDS.stream().anyMatch(lower::contains)
                || lower.contains("university")
                || lower.contains("college")
                || lower.contains("school")
                || lower.contains("institute");
    }

    private boolean looksLikeLocation(String value) {
        String lower = value.toLowerCase();
        return (value.contains(",") && value.length() < 80 && !EMAIL_PATTERN.matcher(value).find())
                || lower.equals("remote")
                || lower.startsWith("remote ")
                || lower.endsWith(" remote");
    }

    private String firstNonBlank(String existing, String candidate) {
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return candidate == null || candidate.isBlank() ? existing : candidate;
    }
}
