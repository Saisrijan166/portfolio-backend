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
 * Focuses on maximum accuracy for data that CAN be extracted from text.
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

    private static final Pattern SINGLE_YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

    // Section header keywords
    private static final List<String> EXPERIENCE_KEYWORDS = List.of(
            "experience", "work experience", "professional experience", "employment",
            "work history", "career history", "positions held");

    private static final List<String> EDUCATION_KEYWORDS = List.of(
            "education", "academic", "academics", "qualifications", "educational background");

    private static final List<String> SKILLS_KEYWORDS = List.of(
            "skills", "technical skills", "core competencies", "competencies",
            "technologies", "tech stack", "tools", "proficiencies");

    private static final List<String> PROJECT_KEYWORDS = List.of(
            "projects", "personal projects", "academic projects", "portfolio");

    private static final List<String> CERTIFICATION_KEYWORDS = List.of(
            "certifications", "certificates", "courses", "training",
            "professional development", "licenses");

    private static final List<String> SUMMARY_KEYWORDS = List.of(
            "summary", "objective", "profile", "about", "about me",
            "professional summary", "career objective", "introduction");

    // ─── Main parse method ───────────────────────────────────────────────────

    public ResumeParseResponseDto parse(String text) {
        if (text == null || text.isBlank()) {
            return ResumeParseResponseDto.builder().provider("internal").build();
        }

        String[] lines = text.split("\\n");
        Map<String, List<String>> sections = identifySections(lines);

        ResumeParseResponseDto result = ResumeParseResponseDto.builder()
                .name(extractName(lines))
                .email(extractFirst(text, EMAIL_PATTERN))
                .phone(extractPhone(text))
                .location(extractLocation(lines))
                .headline(null)
                .summary(extractSectionText(sections, SUMMARY_KEYWORDS))
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

        return result;
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
        String lower = line.toLowerCase()
                .replaceAll("[:\\-–—|_*#]", "")
                .trim();

        if (matchesAny(lower, EXPERIENCE_KEYWORDS))
            return "experience";
        if (matchesAny(lower, EDUCATION_KEYWORDS))
            return "education";
        if (matchesAny(lower, SKILLS_KEYWORDS))
            return "skills";
        if (matchesAny(lower, PROJECT_KEYWORDS))
            return "projects";
        if (matchesAny(lower, CERTIFICATION_KEYWORDS))
            return "certifications";
        if (matchesAny(lower, SUMMARY_KEYWORDS))
            return "summary";
        return null;
    }

    private boolean matchesAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.equals(kw) || text.startsWith(kw + " ") || text.endsWith(" " + kw)) {
                return true;
            }
        }
        return false;
    }

    // ─── Extraction methods ──────────────────────────────────────────────────

    private String extractName(String[] lines) {
        // Name is usually the first non-empty, non-URL, non-email line
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;
            if (EMAIL_PATTERN.matcher(trimmed).find())
                continue;
            if (PHONE_PATTERN.matcher(trimmed).find() && trimmed.length() < 20)
                continue;
            if (URL_PATTERN.matcher(trimmed).find())
                continue;
            if (trimmed.length() > 60)
                continue;

            // Skip lines that look like section headers
            String lower = trimmed.toLowerCase();
            if (lower.startsWith("resume") || lower.startsWith("curriculum"))
                continue;

            return trimmed;
        }
        return null;
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            String phone = matcher.group().trim();
            // Validate: must have at least 7 digits
            String digits = phone.replaceAll("\\D", "");
            if (digits.length() >= 7 && digits.length() <= 15) {
                return phone;
            }
        }
        return null;
    }

    private String extractLocation(String[] lines) {
        // Look in first few lines for location-like patterns
        for (int i = 0; i < Math.min(8, lines.length); i++) {
            String line = lines[i].trim();
            if (line.isEmpty())
                continue;

            // Common location separators
            if (line.contains(",") && line.length() < 80 && !EMAIL_PATTERN.matcher(line).find()) {
                // Could be "City, State" or "City, Country"
                String[] parts = line.split("[|•·]");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.contains(",") && p.length() < 50 && !p.contains("@")) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private String extractFirst(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String result = matcher.group().trim();
            if (!result.startsWith("http")) {
                return result;
            }
            return result;
        }
        return null;
    }

    private String extractSectionText(Map<String, List<String>> sections, List<String> keywords) {
        for (String key : keywords) {
            List<String> lines = sections.get(key);
            if (lines != null && !lines.isEmpty()) {
                return lines.stream()
                        .filter(l -> !l.isEmpty())
                        .collect(Collectors.joining(" "))
                        .trim();
            }
        }
        return null;
    }

    // ─── Skills extraction ───────────────────────────────────────────────────

    private List<String> extractSkills(Map<String, List<String>> sections) {
        List<String> allSkills = new ArrayList<>();

        for (String key : SKILLS_KEYWORDS) {
            List<String> lines = sections.get("skills");
            if (lines == null)
                continue;

            for (String line : lines) {
                if (line.isEmpty())
                    continue;
                // Skills are often comma, pipe, or bullet separated
                String[] parts = line.split("[,|•·;]");
                for (String part : parts) {
                    String skill = part.replaceAll("^[\\-*►▪●○◆→\\s]+", "").trim();
                    if (!skill.isEmpty() && skill.length() < 60) {
                        allSkills.add(skill);
                    }
                }
            }
            break; // only process once
        }

        return allSkills.stream().distinct().collect(Collectors.toList());
    }

    // ─── Experience extraction ───────────────────────────────────────────────

    private List<ParsedExperience> extractExperiences(Map<String, List<String>> sections) {
        List<String> lines = sections.get("experience");
        if (lines == null || lines.isEmpty())
            return new ArrayList<>();

        List<ParsedExperience> experiences = new ArrayList<>();
        ParsedExperience current = null;
        List<String> currentResp = new ArrayList<>();

        for (String line : lines) {
            if (line.isEmpty()) {
                if (current != null) {
                    current.setResponsibilities(new ArrayList<>(currentResp));
                    experiences.add(current);
                    current = null;
                    currentResp.clear();
                }
                continue;
            }

            // Check if this line looks like a new job entry (has year pattern)
            Matcher yearMatcher = YEAR_RANGE_PATTERN.matcher(line);
            boolean hasYear = yearMatcher.find() || SINGLE_YEAR_PATTERN.matcher(line).find();

            if (hasYear && (current == null || !line.startsWith("-") && !line.startsWith("•"))) {
                if (current != null) {
                    current.setResponsibilities(new ArrayList<>(currentResp));
                    experiences.add(current);
                    currentResp.clear();
                }

                current = ParsedExperience.builder()
                        .responsibilities(new ArrayList<>())
                        .achievements(new ArrayList<>())
                        .skills(new ArrayList<>())
                        .build();

                // Try to extract company and role from this and surrounding context
                parseExperienceHeader(line, current);
            } else if (current != null) {
                String cleaned = line.replaceAll("^[\\-*►▪●○◆→•\\s]+", "").trim();
                if (!cleaned.isEmpty()) {
                    currentResp.add(cleaned);
                }
            }
        }

        if (current != null) {
            current.setResponsibilities(new ArrayList<>(currentResp));
            experiences.add(current);
        }

        return experiences;
    }

    private void parseExperienceHeader(String line, ParsedExperience exp) {
        // Extract dates
        Matcher yearRange = YEAR_RANGE_PATTERN.matcher(line);
        if (yearRange.find()) {
            try {
                exp.setStartYear(Integer.parseInt(yearRange.group(1)));
                String end = yearRange.group(2);
                if (end.matches("\\d{4}")) {
                    exp.setEndYear(Integer.parseInt(end));
                } else {
                    exp.setCurrent(true);
                }
            } catch (NumberFormatException ignored) {
            }

            exp.setDuration(yearRange.group(0));
        }

        // Remove the date portion to get company/role
        String remainder = line.replaceAll(YEAR_RANGE_PATTERN.pattern(), "")
                .replaceAll("\\d{4}", "")
                .replaceAll("[|•·,\\-–—]+$", "")
                .replaceAll("^[|•·,\\-–—]+", "")
                .trim();

        if (!remainder.isEmpty()) {
            // Try to split by common separators
            String[] parts = remainder.split("[|•·–—]");
            if (parts.length >= 2) {
                exp.setRoleTitle(parts[0].trim());
                exp.setCompany(parts[1].trim());
            } else {
                exp.setRoleTitle(remainder);
            }
        }
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

        ParsedEducation edu = ParsedEducation.builder().build();

        // First line usually has the degree/institute
        String first = lines.get(0);
        edu.setDegree(first);

        if (lines.size() > 1) {
            edu.setInstitute(lines.get(1));
        }

        // Look for years in any line
        for (String line : lines) {
            Matcher yearRange = YEAR_RANGE_PATTERN.matcher(line);
            if (yearRange.find()) {
                try {
                    edu.setStartYear(Integer.parseInt(yearRange.group(1)));
                    String end = yearRange.group(2);
                    if (end.matches("\\d{4}")) {
                        edu.setEndYear(Integer.parseInt(end));
                    }
                } catch (NumberFormatException ignored) {
                }
                edu.setDuration(yearRange.group(0));
                break;
            }
        }

        // Detect level
        String allText = String.join(" ", lines).toLowerCase();
        if (allText.contains("phd") || allText.contains("doctorate")) {
            edu.setLevel("PhD");
        } else if (allText.contains("master") || allText.contains("m.s.") || allText.contains("mba")
                || allText.contains("m.tech")) {
            edu.setLevel("Masters");
        } else if (allText.contains("bachelor") || allText.contains("b.s.") || allText.contains("b.tech")
                || allText.contains("b.e.")) {
            edu.setLevel("Bachelors");
        } else if (allText.contains("diploma")) {
            edu.setLevel("Diploma");
        } else if (allText.contains("high school") || allText.contains("secondary")) {
            edu.setLevel("High School");
        }

        // Look for GPA/percentage
        Pattern scorePattern = Pattern.compile("(?:GPA|CGPA|Percentage|Score)[:\\s]*([\\d.]+(?:/[\\d.]+)?%?)",
                Pattern.CASE_INSENSITIVE);
        for (String line : lines) {
            Matcher scoreMatcher = scorePattern.matcher(line);
            if (scoreMatcher.find()) {
                String labelPart = scoreMatcher.group(0).split("[:\\s]")[0];
                edu.setScoreLabel(labelPart);
                edu.setScoreValue(scoreMatcher.group(1));
                break;
            }
        }

        return edu;
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

        ParsedProject proj = ParsedProject.builder()
                .type("Personal")
                .status("Completed")
                .techStack(new ArrayList<>())
                .build();

        proj.setName(lines.get(0).replaceAll("^[\\-*►▪●○◆→•]+\\s*", "").trim());

        StringBuilder desc = new StringBuilder();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String cleaned = line.replaceAll("^[\\-*►▪●○◆→•]+\\s*", "").trim();

            // Check for tech stack indicators
            String lower = cleaned.toLowerCase();
            if (lower.startsWith("tech") || lower.startsWith("built with") || lower.startsWith("stack")) {
                String techPart = cleaned.replaceFirst("(?i)^(?:technologies?|tech stack|built with|stack)[:\\s]*", "");
                String[] techs = techPart.split("[,|•·;]");
                for (String t : techs) {
                    String tech = t.trim();
                    if (!tech.isEmpty())
                        proj.getTechStack().add(tech);
                }
            } else {
                // Extract URLs
                Matcher urlMatcher = URL_PATTERN.matcher(cleaned);
                while (urlMatcher.find()) {
                    String url = urlMatcher.group();
                    if (url.contains("github.com")) {
                        proj.setSourceLink(url);
                    } else {
                        proj.setLiveLink(url);
                    }
                }

                if (!cleaned.isEmpty() && desc.length() < 500) {
                    if (desc.length() > 0)
                        desc.append(" ");
                    desc.append(cleaned);
                }
            }
        }

        proj.setOverview(desc.toString().trim());

        // Extract year
        for (String line : lines) {
            Matcher yearMatcher = SINGLE_YEAR_PATTERN.matcher(line);
            if (yearMatcher.find()) {
                proj.setYear(yearMatcher.group());
                break;
            }
        }

        return proj;
    }

    // ─── Certifications extraction ───────────────────────────────────────────

    private List<String> extractCertifications(Map<String, List<String>> sections) {
        List<String> lines = sections.get("certifications");
        if (lines == null || lines.isEmpty())
            return new ArrayList<>();

        return lines.stream()
                .filter(l -> !l.isEmpty())
                .map(l -> l.replaceAll("^[\\-*►▪●○◆→•\\s]+", "").trim())
                .filter(l -> !l.isEmpty())
                .collect(Collectors.toList());
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

            String label = "Website";
            if (url.contains("twitter.com") || url.contains("x.com"))
                label = "Twitter";
            else if (url.contains("medium.com"))
                label = "Medium";
            else if (url.contains("stackoverflow.com"))
                label = "Stack Overflow";
            else if (url.contains("behance.net"))
                label = "Behance";
            else if (url.contains("dribbble.com"))
                label = "Dribbble";
            else if (url.contains("leetcode.com"))
                label = "LeetCode";
            else if (url.contains("hackerrank.com"))
                label = "HackerRank";
            else if (url.contains("codepen.io"))
                label = "CodePen";
            else if (url.contains("youtube.com"))
                label = "YouTube";
            else if (url.contains("dev.to"))
                label = "Dev.to";
            else if (url.contains("hashnode."))
                label = "Hashnode";
            else if (url.contains("kaggle.com"))
                label = "Kaggle";

            links.add(ParsedLink.builder().label(label).url(url).build());
        }

        return links;
    }
}
