# Portfolio SaaS Platform — Backend

Spring Boot REST API powering the Portfolio SaaS platform. Handles multi-tenant authentication, portfolio content management, AI-assisted resume tooling, and transactional email — serving the [portfolio-frontend](https://github.com/Saisrijan166/portfolio-frontend) client.

**Live:** https://portfoliooss.vercel.app

## Features

- Multi-tenant account, profile, and content management (about, experience, education, skills, certifications, projects, appearance/widgets)
- Authentication & security, including a PIN-gated superadmin module for platform administration
- AI-assisted resume parsing, scoring, and section summarization
- Automated LaTeX resume generation and download
- Public portfolio and rating endpoints for visitor-facing pages
- Transactional email system with templating (welcome emails, password reset, notifications)
- Scheduled/cron jobs and a keep-warm endpoint to avoid cold starts
- Dockerized build (includes TeX Live for resume PDF generation)

## Tech Stack

- Java, Spring Boot
- PostgreSQL
- Maven
- Docker

## Architecture

This is the backend half of a two-repo system:

- **Backend** (this repo) — REST API, business logic, persistence, AI tooling
- **[portfolio-frontend](https://github.com/Saisrijan166/portfolio-frontend)** — Next.js client consuming this API

## Getting Started

```bash
./mvnw spring-boot:run
```

Copy `.env.example` to `.env` and configure the required variables (database connection, mail, AI provider keys, etc.) before running. A PostgreSQL instance is required.

