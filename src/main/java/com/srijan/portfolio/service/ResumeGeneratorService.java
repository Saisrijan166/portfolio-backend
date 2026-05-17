package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.exception.ApiException;
import lombok.RequiredArgsConstructor;
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

    public byte[] generateResumePdf(String username) {
        PortfolioResponse portfolio = portfolioService.getPortfolioByUsername(username);
        if (portfolio == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }

        String latexSource = buildLatex(portfolio);
                
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("resume_gen_" + username);
            Path texFile = tempDir.resolve("resume.tex");
            Files.writeString(texFile, latexSource);

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

    private String buildLatex(PortfolioResponse p) {
        StringBuilder sb = new StringBuilder();
        
        // Preamble
        sb.append("\\documentclass[a4paper,20pt]{article}\n")
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
          .append("\\pagestyle{fancy}\n")
          .append("\\fancyhf{}\n")
          .append("\\fancyfoot{}\n")
          .append("\\renewcommand{\\headrulewidth}{0pt}\n")
          .append("\\renewcommand{\\footrulewidth}{0pt}\n")
          .append("\\addtolength{\\oddsidemargin}{-0.530in}\n")
          .append("\\addtolength{\\evensidemargin}{-0.375in}\n")
          .append("\\addtolength{\\textwidth}{1in}\n")
          .append("\\addtolength{\\topmargin}{-.65in}\n")
          .append("\\addtolength{\\textheight}{1.3in}\n")
          .append("\\urlstyle{rm}\n")
          .append("\\raggedbottom\n")
          .append("\\raggedright\n")
          .append("\\setlength{\\tabcolsep}{0in}\n")
          .append("\\titleformat{\\section}{\\vspace{-10pt}\\scshape\\raggedright\\large}{}{0em}{}[\\color{black}\\titlerule \\vspace{-6pt}]\n")
          .append("\\newcommand{\\resumeItem}[2]{\\item\\small{\\textbf{#1}{: #2 \\vspace{-2pt}}}}\n")
          .append("\\newcommand{\\resumeItemWithoutTitle}[1]{\\item\\small{{\\vspace{-2pt}}}}\n")
          .append("\\newcommand{\\resumeSubheading}[4]{\\vspace{-1pt}\\item\\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}\\textbf{#1} & #2 \\\\ \\textit{#3} & \\textit{#4} \\\\ \\end{tabular*}\\vspace{-5pt}}\n")
          .append("\\newcommand{\\resumeSubItem}[2]{\\resumeItem{#1}{#2}\\vspace{-3pt}}\n")
          .append("\\renewcommand{\\labelitemii}{$\\circ$}\n")
          .append("\\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=*]}\n")
          .append("\\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}\n")
          .append("\\newcommand{\\resumeItemListStart}{\\begin{itemize}}\n")
          .append("\\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}\n")
          .append("\\usepackage[table]{xcolor}\n")
          .append("\\definecolor{crimson}{rgb}{0.86,0.08,0.24}\n")
          .append("\\usepackage{tikz}\n")
          .append("\\begin{document}\n")
          .append("\\definecolor{lightblack}{rgb}{0.18, 0.31, 0.31}\n")
          .append("\\hypersetup{colorlinks=true,linkcolor=black,urlcolor=black,citecolor=black,filecolor=black,hidelinks}\n");

        // Header
        String name = p.getProfile() != null && p.getProfile().getName() != null ? escapeLatex(p.getProfile().getName()) : "Your Name";
        String role = p.getAbout() != null && p.getAbout().getRoleTitle() != null ? escapeLatex(p.getAbout().getRoleTitle()) : "Professional";
        
        sb.append("\\noindent\n")
          .append("\\begin{minipage}{0.69\\textwidth}\n")
          .append("    \\raggedright\n")
          .append("    \\vspace{0cm}\n")
          .append("{\\LARGE\\bfseries\\sffamily \\textcolor[rgb]{0.86,0.08,0.24} {").append(name).append(" }}\n")
          .append("\\\\\n")
          .append("\\vspace{0.09cm}\n");
          
        sb.append("    \\hspace{0.1cm}{}");
        
        if (p.getContact() != null) {
            if (p.getContact().getPrimaryEmail() != null && !p.getContact().getPrimaryEmail().isBlank()) {
                String email = escapeLatex(p.getContact().getPrimaryEmail());
                sb.append("\\textcolor{lightblack}{\\scalebox{1}{\\faEnvelope[regular]}} \\href{mailto:").append(email).append("}{\\bfseries\\rmfamily\\itshape ").append(email).append("} \\hspace{0.4cm}\n");
            }
            // we skip phone if missing since ContactDto might not have it directly mapped to a strong field, wait ContactDto has professionalLinks/socialLinks. Let's look for phone or linkedin.
            boolean hasLinkedIn = false;
            boolean hasGithub = false;
            String portfolioLink = "Portfolio";
            
            if (p.getContact().getProfessionalLinks() != null) {
                for (ContactLinkDto link : p.getContact().getProfessionalLinks()) {
                    String url = escapeLatex(link.getUrl());
                    String label = escapeLatex(link.getLabel());
                    if (label.toLowerCase().contains("linkedin")) {
                        hasLinkedIn = true;
                        sb.append("\\textcolor{lightblack}{\\scalebox{1.1}{\\faLinkedin}} \\href{").append(url).append("}{\\bfseries\\rmfamily\\itshape ").append(label).append("}\n");
                    } else if (label.toLowerCase().contains("github")) {
                        hasGithub = true;
                        sb.append("\\hspace{0.5cm}\\textcolor{lightblack}{\\scalebox{1.1}{\\faGithub}}  \\href{").append(url).append("}{\\bfseries\\rmfamily\\itshape ").append(label).append("} \\\\\n");
                    }
                }
            }
            
            // if we need more links, we can add them here
        }
        
        sb.append("\\end{minipage}\n")
          .append("\\hfill\n")
          .append("\\noindent\n")
          .append("\\begin{minipage}{0.085\\textwidth}\n")
          .append("\\vspace{-0.4cm}\n")
          .append("{\\large\\sffamily\\textcolor{gray}{").append(role).append("}}\n")
          .append("\\end{minipage}\n")
          .append("\\vspace{1pt}\n");

        // Education
        if (p.getEducations() != null && !p.getEducations().isEmpty()) {
            sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Education}}\n")
              .append("\\resumeSubHeadingListStart\n");
            for (EducationDto ed : p.getEducations()) {
                String inst = escapeLatex(ed.getInstitute());
                String loc = escapeLatex(ed.getLocation());
                String deg = escapeLatex(ed.getDegree());
                if (ed.getScoreLabel() != null && !ed.getScoreLabel().isBlank()) {
                    deg += "; " + escapeLatex(ed.getScoreLabel()) + ": " + escapeLatex(ed.getScoreValue());
                }
                String dur = escapeLatex(ed.getDuration());
                
                sb.append("  \\resumeSubheading\n")
                  .append("    {").append(inst != null ? inst : "").append("}{").append(loc != null ? loc : "").append("}\n")
                  .append("    {").append(deg != null ? deg : "").append("}{").append(dur != null ? dur : "").append("}\n");
            }
            sb.append("\\resumeSubHeadingListEnd\n");
        }

        // Skills
        if (p.getSkills() != null && !p.getSkills().isEmpty()) {
            sb.append("\\definecolor{lightblack}{rgb}{0.41, 0.41, 0.41}\n")
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
            sb.append("\\resumeSubHeadingListEnd\n\\vspace{-5pt}\n");
        }

        // Experience
        if (p.getExperiences() != null && !p.getExperiences().isEmpty()) {
            sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Experience}}\n")
              .append("\\resumeSubHeadingListStart\n");
            for (ExperienceDto ex : p.getExperiences()) {
                String title = escapeLatex(ex.getRoleTitle()) + " - " + escapeLatex(ex.getCompany());
                String dur = escapeLatex(ex.getDuration());
                String desc = ex.getResponsibilities() != null && !ex.getResponsibilities().isEmpty() 
                                ? escapeLatex(String.join(", ", ex.getResponsibilities())) 
                                : "";
                
                sb.append("\\resumeSubItem{").append(title).append("}\n")
                  .append("{\\textbf{Duration:} ").append(dur).append(" \\\\\n")
                  .append(desc).append("\n}\n");
            }
            sb.append("\\resumeSubHeadingListEnd\n\\vspace{-6pt}\n");
        }

        // Projects
        if (p.getProjects() != null && !p.getProjects().isEmpty()) {
            List<ProjectDto> regProjects = p.getProjects().stream().filter(pr -> !pr.isResearch()).collect(Collectors.toList());
            if (!regProjects.isEmpty()) {
                sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Projects}}\n")
                  .append("\\resumeSubHeadingListStart\n");
                for (ProjectDto pr : regProjects) {
                    String namePr = escapeLatex(pr.getName());
                    String desc = escapeLatex(pr.getOverview());
                    String tech = pr.getTechStack() != null ? escapeLatex(String.join(", ", pr.getTechStack())) : "";
                    
                    sb.append("\\resumeSubItem{").append(namePr).append("}{").append(desc).append("\\\\\n")
                      .append("\\textbf{Tech:} ").append(tech).append("}\n")
                      .append("\\vspace{1pt}\n");
                }
                sb.append("\\resumeSubHeadingListEnd\n\\vspace{-7pt}\n");
            }

            List<ProjectDto> resProjects = p.getProjects().stream().filter(pr -> pr.isResearch()).collect(Collectors.toList());
            if (!resProjects.isEmpty()) {
                sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Research}}\n")
                  .append("\\resumeSubHeadingListStart\n");
                for (ProjectDto pr : resProjects) {
                    String namePr = escapeLatex(pr.getName());
                    String desc = escapeLatex(pr.getOverview());
                    
                    sb.append("\\resumeSubItem{").append(namePr).append("}{").append(desc).append("}\n")
                      .append("\\vspace{-2pt}\n");
                }
                sb.append("\\resumeSubHeadingListEnd\n\\vspace{-6pt}\n");
            }
        }

        // Certifications & Achievements
        if (p.getCertificationAchievements() != null && !p.getCertificationAchievements().isEmpty()) {
            List<CertificationAchievementDto> certs = p.getCertificationAchievements().stream().filter(c -> "certification".equalsIgnoreCase(c.getType())).collect(Collectors.toList());
            if (!certs.isEmpty()) {
                sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Certifications}}\n")
                  .append("\\resumeSubHeadingListStart\n");
                for (CertificationAchievementDto c : certs) {
                    sb.append("\\resumeSubItem{").append(escapeLatex(c.getTitle())).append("}{").append(escapeLatex(c.getDescription() != null ? c.getDescription() : c.getIssuer())).append("}\n");
                }
                sb.append("\\resumeSubHeadingListEnd\n\\vspace{-6pt}\n");
            }

            List<CertificationAchievementDto> achs = p.getCertificationAchievements().stream().filter(c -> "achievement".equalsIgnoreCase(c.getType())).collect(Collectors.toList());
            if (!achs.isEmpty()) {
                sb.append("\\section{\\textcolor[rgb]{0.86,0.08,0.24}{Achievements}}\n")
                  .append("\\begin{description}[font=\\bullet]\n");
                for (CertificationAchievementDto a : achs) {
                    sb.append("\\item {").append(escapeLatex(a.getTitle())).append("}\n\\vspace{-5pt}\n");
                }
                sb.append("\\end{description}\n");
            }
        }

        sb.append("\\end{document}\n");
        return sb.toString();
    }

    private String escapeLatex(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\textbackslash{}")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("$", "\\$")
                .replace("&", "\\&")
                .replace("#", "\\#")
                .replace("^", "\\textasciicircum{}")
                .replace("_", "\\_")
                .replace("~", "\\textasciitilde{}")
                .replace("%", "\\%");
    }
}
