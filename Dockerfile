# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy the maven wrapper and project definition files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make the wrapper executable and download dependencies
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copy the project source
COPY src ./src

# Build the jar
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install texlive for pdflatex (resume PDF generation).
# Icons come from fontawesome5, which ships in texlive-fonts-extra. There is deliberately no
# build-time download here: a previous step fetched the simpleicons package from
# mirrors.ctan.org, which 307-redirects to a randomly chosen mirror, so the build broke
# whenever it landed on one with an untrusted certificate (wget exit 5). That package was
# only ever \usepackage'd and never used, so it was removed rather than made resilient.
RUN apt-get update && apt-get install -y --no-install-recommends \
    texlive-latex-base \
    texlive-latex-extra \
    texlive-latex-recommended \
    texlive-fonts-recommended \
    texlive-fonts-extra \
    texlive-font-utils \
    && rm -rf /var/lib/apt/lists/*

# Expose the application port
EXPOSE 8080

# Copy the built jar to the runtime image
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-Djava.net.preferIPv4Stack=true", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]