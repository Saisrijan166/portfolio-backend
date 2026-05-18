package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.exception.ApiException;
import lombok.RequiredArgsConstructor;
import com.srijan.portfolio.email.TenantBrandingResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeGeneratorService {

    private final PortfolioService portfolioService;
    private final TenantBrandingResolver tenantBrandingResolver;

    public String generateResumeTex(String username) {
        PortfolioResponse portfolio = portfolioService.getPortfolioByUsername(username);
        if (portfolio == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
        return buildLatex(portfolio, username);
    }

    public byte[] generateResumePdf(String username) {
        PortfolioResponse portfolio = portfolioService.getPortfolioByUsername(username);
        if (portfolio == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }

        String latexSource = buildLatex(portfolio, username);

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("resume_gen_" + username);
            Path texFile = tempDir.resolve("resume.tex");
            Files.writeString(texFile, latexSource);

            // Run pdflatex twice for proper rendering
            for (int i = 0; i < 2; i++) {
                ProcessBuilder pb = new ProcessBuilder(
                        "pdflatex",
                        "-interaction=nonstopmode",
                        "-output-directory=" + tempDir.toAbsolutePath().toString(),
                        texFile.toAbsolutePath().toString()
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    log.error("pdflatex failed with exit code {}: {}", exitCode, output);
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED", "Failed to generate PDF");
                }
            }

            Path pdfFile = tempDir.resolve("resume.pdf");
            if (!Files.exists(pdfFile)) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED", "PDF file not created");
            }

            return Files.readAllBytes(pdfFile);

        } catch (Exception e) {
            log.error("Error generating resume for user {}", username, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF_GENERATION_FAILED", "Failed to generate PDF: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                try (Stream<Path> walk = Files.walk(tempDir)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    log.warn("Failed to clean up temp directory {}", tempDir, e);
                }
            }
        }
    }

    private String buildLatex(PortfolioResponse p, String username) {
        StringBuilder sb = new StringBuilder();

        // ─── Preamble (mirrors original .tex exactly) ───────────────────────
        sb.append("\\documentclass[a4paper,11pt]{article}\n\n")
          .append("\\usepackage{latexsym}\n")
          .append("\\usepackage[empty]{fullpage}\n")
          .append("\\usepackage{titlesec}\n")
          .append("\\usepackage{marvosym}\n")
          .append("\\usepackage[usenames,dvipsnames]{color}\n")
          .append("\\usepackage{verbatim}\n")
          .append("\\usepackage{enumitem}\n")
          .append("\\usepackage[pdftex]{hyperref}\n")
          .append("\\usepackage{fancyhdr}\n")
          .append("\\usepackage{graphicx}\n")
          .append("\\usepackage{fontawesome5}\n")
          .append("\\usepackage{simpleicons}\n")
          .append("\\usepackage[table]{xcolor}\n")
          .append("\\usepackage{tikz}\n\n")
          .append("\\pagestyle{fancy}\n")
          .append("\\fancyhf{}\n")
          .append("\\fancyfoot{}\n")
          .append("\\renewcommand{\\headrulewidth}{0pt}\n")
          .append("\\renewcommand{\\footrulewidth}{0pt}\n\n")
          // Margins — slightly more generous top/bottom than original for whitespace
          .append("\\addtolength{\\oddsidemargin}{-0.530in}\n")
          .append("\\addtolength{\\evensidemargin}{-0.375in}\n")
          .append("\\addtolength{\\textwidth}{1in}\n")
          .append("\\addtolength{\\topmargin}{-.50in}\n")
          .append("\\addtolength{\\textheight}{1.0in}\n\n")
          .append("\\urlstyle{rm}\n")
          .append("\\raggedbottom\n")
          .append("\\raggedright\n")
          .append("\\setlength{\\tabcolsep}{0in}\n\n")
          // Section formatting
          .append("\\titleformat{\\section}{\n")
          .append("  \\vspace{-10pt}\\scshape\\raggedright\\large\n")
          .append("}{}{0em}{}[\\color{black}\\titlerule \\vspace{-6pt}]\n\n")
          // Custom commands — identical to original
          .append("\\newcommand{\\resumeItem}[2]{\n")
          .append("  \\item\\small{\n")
          .append("    \\textbf{#1}{: #2 \\vspace{-2pt}}\n")
          .append("  }\n")
          .append("}\n\n")
          .append("\\newcommand{\\resumeItemWithoutTitle}[1]{\n")
          .append("  \\item\\small{\n")
          .append("    {\\vspace{-2pt}}\n")
          .append("  }\n")
          .append("}\n\n")
          .append("\\newcommand{\\resumeSubheading}[4]{\n")
          .append("  \\vspace{-1pt}\\item\n")
          .append("    \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}\n")
          .append("      \\textbf{#1} & #2 \\\\\n")
          .append("      \\textit{#3} & \\textit{#4} \\\\\n")
          .append("    \\end{tabular*}\\vspace{-5pt}\n")
          .append("}\n\n")
          .append("\\newcommand{\\resumeSubItem}[2]{\\resumeItem{#1}{#2}\\vspace{-3pt}}\n\n")
          .append("\\renewcommand{\\labelitemii}{$\\circ$}\n\n")
          .append("\\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=*]}\n")
          .append("\\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}\n")
          .append("\\newcommand{\\resumeItemListStart}{\\begin{itemize}}\n")
          .append("\\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}\n\n")
          // Color definitions
          .append("\\definecolor{crimson}{rgb}{0.86,0.08,0.24}\n\n");

        // ─── Document begin ──────────────────────────────────────────────────
        sb.append("\\begin{document}\n\n")
          .append("\\definecolor{lightblack}{rgb}{0.18, 0.31, 0.31}\n")
          .append("\\hypersetup{\n")
          .append("    colorlinks=true,\n")
          .append("    linkcolor=black,\n")
          .append("    urlcolor=black,\n")
          .append("    citecolor=black,\n")
          .append("    filecolor=black,\n")
          .append("    hidelinks\n")
          .append("}\n\n");

        // ─── Header ─────────────────────────────────────────────────────────
        String name = (p.getProfile() != null && p.getProfile().getName() != null)
                ? escapeLatex(p.getProfile().getName()) : "Your Name";
        String role = (p.getAbout() != null && p.getAbout().getRoleTitle() != null)
                ? escapeLatex(p.getAbout().getRoleTitle()) : "Professional";

        // Split role title on space so it wraps nicely in the small minipage
        String roleFormatted = role.replace(" ", " \\\\\n");

        sb.append("\\noindent\n")
          .append("\\begin{minipage}{0.69\\textwidth}\n")
          .append("    \\raggedright\n")
          .append("    \\vspace{0.4cm}\n\n")
          .append("{\\LARGE\\bfseries\\sffamily \\textcolor[rgb]{0.86,0.08,0.24}{").append(name).append("}} \\\\\n")
          .append("\\vspace{0.09cm}\n");

        // Email
        if (p.getContact() != null && p.getContact().getPrimaryEmail() != null && !p.getContact().getPrimaryEmail().isBlank()) {
            String email = escapeLatex(p.getContact().getPrimaryEmail());
            sb.append("\\textcolor{lightblack}{\\scalebox{1}{\\faEnvelope[regular]}} \\href{mailto:")
              .append(email).append("}{\\bfseries\\rmfamily\\itshape ").append(email).append("} \\hspace{0.4cm}\n");
        }

        // Portfolio link
        String appUrl  = tenantBrandingResolver.resolve(username).appUrl();
        String portfolioUrl = escapeLatex(appUrl + "/" + username);

        sb.append("\\textcolor{lightblack}{\\scalebox{1.1}{\\faGlobe}} \\href{").append(portfolioUrl)
          .append("}{\\bfseries\\rmfamily\\itshape Portfolio} \\hspace{0.4cm}\n");

        // LinkedIn and GitHub from professional links
        if (p.getContact() != null && p.getContact().getProfessionalLinks() != null) {
            for (ContactLinkDto link : p.getContact().getProfessionalLinks()) {
                if (link.getLabel() == null || link.getUrl() == null) continue;
                String lbl = link.getLabel().toLowerCase();
                String url = escapeLatex(link.getUrl());
                String label = escapeLatex(link.getLabel());
                if (lbl.contains("linkedin")) {
                    sb.append("\\textcolor{lightblack}{\\scalebox{1.1}{\\faLinkedin}} \\href{").append(url)
                      .append("}{\\bfseries\\rmfamily\\itshape ").append(label).append("} \\hspace{0.4cm}\\\\\n");
                } else if (lbl.contains("github")) {
                    sb.append("\\textcolor{lightblack}{\\scalebox{1.1}{\\faGithub}} \\href{").append(url)
                      .append("}{\\bfseries\\rmfamily\\itshape ").append(label).append("}\n");
                }
            }
        }

        sb.append("\n");

        sb.append("\\end{minipage}\n")
          .append("\\hfill\n")
          .append("\\noindent\n")
          .append("\\begin{minipage}{0.085\\textwidth}\n")
          .append("\\vspace{-0.4cm}\n")
          .append("{\\large\\sffamily\\textcolor{gray}{").append(roleFormatted).append("}}\n")
          .append("\\end{minipage}\n\n")
          .append("\\vspace{1pt}\n");

        // ─── Education ──────────────────────────────────────────────────────
        if (p.getEducations() != null && !p.getEducations().isEmpty()) {
            sb.append("%-----------EDUCATION-----------------\n")
              .append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Education}}\n")
              .append("  \\resumeSubHeadingListStart\n");

            for (EducationDto ed : p.getEducations()) {
                String inst = ed.getInstitute()  != null ? escapeLatex(ed.getInstitute())  : "";
                String loc  = ed.getLocation()   != null ? escapeLatex(ed.getLocation())   : "";
                String dur  = ed.getDuration()   != null ? escapeLatex(ed.getDuration())   : "";

                // Build degree string — "Bachelor of CSE; GPA: 8.17"  (mirrors original)
                StringBuilder degBuilder = new StringBuilder();
                if (ed.getDegree() != null && !ed.getDegree().isBlank()) {
                    degBuilder.append(escapeLatex(ed.getDegree()));
                }
                if (ed.getScoreLabel() != null && !ed.getScoreLabel().isBlank()
                        && ed.getScoreValue() != null && !ed.getScoreValue().isBlank()) {
                    degBuilder.append("; ").append(escapeLatex(ed.getScoreLabel()))
                              .append(": ").append(escapeLatex(ed.getScoreValue()));
                }

                sb.append("    \\resumeSubheading\n")
                  .append("      {").append(inst).append("}{").append(loc).append("}\n")
                  .append("      {").append(degBuilder).append("}{").append(dur).append("}\n");
            }
            sb.append("  \\resumeSubHeadingListEnd\n\n");
        }

        // ─── Skills Summary ─────────────────────────────────────────────────
        if (p.getSkills() != null && !p.getSkills().isEmpty()) {
            sb.append("\\definecolor{lightblack}{rgb}{0.41, 0.41, 0.41}\n\n")
              .append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Skills Summary}}\n")
              .append("\\resumeSubHeadingListStart\n");

            Map<String, List<String>> groupedSkills = p.getSkills().stream()
                    .collect(Collectors.groupingBy(
                            s -> s.getDomain() != null ? s.getDomain() : "General",
                            LinkedHashMap::new,
                            Collectors.mapping(SkillDto::getName, Collectors.toList())
                    ));

            for (Map.Entry<String, List<String>> entry : groupedSkills.entrySet()) {
                String domain = escapeLatex(entry.getKey());
                String skills = entry.getValue().stream()
                        .map(this::escapeLatex)
                        .collect(Collectors.joining(", "));
                sb.append("\\resumeSubItem{").append(domain).append("}{ ").append(skills).append("}\n");
            }
            sb.append("\\resumeSubHeadingListEnd\n\n\n")
              .append("\\vspace{-5pt}\n");
        }

        // ─── Experience ─────────────────────────────────────────────────────
        if (p.getExperiences() != null && !p.getExperiences().isEmpty()) {
            sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Experience}}\n")
              .append("\\resumeSubHeadingListStart\n");

            for (ExperienceDto ex : p.getExperiences()) {
                // Title format: "Role – Company"  (mirrors original em-dash style)
                String roleTitle = ex.getRoleTitle() != null ? escapeLatex(ex.getRoleTitle()) : "";
                String company   = ex.getCompany()   != null ? escapeLatex(ex.getCompany())   : "";
                String title     = roleTitle + " -- " + company;
                String dur       = ex.getDuration()  != null ? escapeLatex(ex.getDuration())  : "";

                // Responsibilities — join into prose description
                String desc = "";
                if (ex.getResponsibilities() != null && !ex.getResponsibilities().isEmpty()) {
                    desc = ex.getResponsibilities().stream()
                            .map(this::escapeLatex)
                            .collect(Collectors.joining(" "));
                }

                sb.append("\\resumeSubItem{").append(title).append("}\n")
                  .append("{\\textbf{Duration:} ").append(dur).append(" \\\\\n")
                  .append(desc).append("\n}\n");
            }
            sb.append("\\resumeSubHeadingListEnd\n\n\n");
        }

        // ─── Projects & Research ────────────────────────────────────────────
        if (p.getProjects() != null && !p.getProjects().isEmpty()) {

            List<ProjectDto> regProjects = p.getProjects().stream()
                    .filter(pr -> !pr.isResearch())
                    .collect(Collectors.toList());

            if (!regProjects.isEmpty()) {
                sb.append("%-----------PROJECTS-----------------\n")
                  .append("\\vspace{-5pt}\n")
                  .append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Projects}}\n")
                  .append("\\resumeSubHeadingListStart\n\n");

                for (ProjectDto pr : regProjects) {
                    String projName = pr.getName()    != null ? escapeLatex(pr.getName())    : "";
                    String overview = pr.getOverview() != null ? escapeLatex(pr.getOverview()) : "";
                    String tech     = (pr.getTechStack() != null && !pr.getTechStack().isEmpty())
                            ? pr.getTechStack().stream().map(this::escapeLatex).collect(Collectors.joining(", "))
                            : "";

                    sb.append("\\resumeSubItem{").append(projName).append("}{").append(overview).append("\\\\\n");
                    if (!tech.isBlank()) {
                        sb.append("\\textbf{Tech:} ").append(tech);
                    }
                    sb.append("}\n").append("\\vspace{1pt}\n\n");
                }
                sb.append("\\resumeSubHeadingListEnd\n\n")
                  .append("\\vspace{-7pt}\n");
            }

            // Research section — separate, like original
            List<ProjectDto> resProjects = p.getProjects().stream()
                    .filter(pr -> pr.isResearch())
                    .collect(Collectors.toList());

            if (!resProjects.isEmpty()) {
                sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Research}}\n")
                  .append("\\resumeSubHeadingListStart\n");

                for (ProjectDto pr : resProjects) {
                    String projName = pr.getName()    != null ? escapeLatex(pr.getName())    : "";
                    String overview = pr.getOverview() != null ? escapeLatex(pr.getOverview()) : "";

                    sb.append("\\resumeSubItem{").append(projName).append("}{").append(overview).append("}\n")
                      .append("\\vspace{-2pt}\n\n");
                }
                sb.append("\\resumeSubHeadingListEnd\n\n\n")
                  .append("\\vspace{-6pt}\n");
            }
        }

        // ─── Certifications & Awards ─────────────────────────────────────────
        if (p.getCertificationAchievements() != null && !p.getCertificationAchievements().isEmpty()) {

            List<CertificationAchievementDto> certs = p.getCertificationAchievements().stream()
                    .filter(c -> "certification".equalsIgnoreCase(c.getType()))
                    .collect(Collectors.toList());

            if (!certs.isEmpty()) {
                sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Certifications \\& Awards}}\n")
                  .append("\\resumeSubHeadingListStart\n\n");

                for (CertificationAchievementDto c : certs) {
                    String certTitle = escapeLatex(c.getTitle());
                    // Prefer description; fall back to issuer — mirrors original
                    String certDesc  = (c.getDescription() != null && !c.getDescription().isBlank())
                            ? escapeLatex(c.getDescription())
                            : (c.getIssuer() != null ? escapeLatex(c.getIssuer()) : "");
                    sb.append("\\resumeSubItem{").append(certTitle).append("}{").append(certDesc).append("}\n");
                }
                sb.append("\\resumeSubHeadingListEnd\n\n\n")
                  .append("\\vspace{-6pt}\n");
            }

            // ─── Achievements ────────────────────────────────────────────────
            List<CertificationAchievementDto> achs = p.getCertificationAchievements().stream()
                    .filter(c -> "achievement".equalsIgnoreCase(c.getType()))
                    .collect(Collectors.toList());

            if (!achs.isEmpty()) {
                sb.append("%-----------Achievements-----------------\n")
                  .append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Achievements}}\n")
                  .append("\\begin{description}[font=$\\bullet$]\n");

                for (CertificationAchievementDto a : achs) {
                    sb.append("\\item {").append(escapeLatex(a.getTitle())).append("}\n")
                      .append("\\vspace{-5pt}\n");
                }
                sb.append("\\end{description}\n\n");
            }
        }

        sb.append("\n\\end{document}\n");
        return sb.toString();
    }

    /**
     * Escapes all LaTeX special characters.
     * Order matters — backslash must be handled first.
     */
    private String escapeLatex(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\textbackslash{}")
                .replace("{",  "\\{")
                .replace("}",  "\\}")
                .replace("$",  "\\$")
                .replace("&",  "\\&")
                .replace("#",  "\\#")
                .replace("^",  "\\textasciicircum{}")
                .replace("_",  "\\_")
                .replace("~",  "\\textasciitilde{}")
                .replace("%",  "\\%");
    }
}