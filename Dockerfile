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

# Install texlive for pdflatex (resume PDF generation)
RUN apt-get update && apt-get install -y --no-install-recommends \
    texlive-latex-base \
    texlive-latex-extra \
    texlive-latex-recommended \
    texlive-fonts-recommended \
    texlive-fonts-extra \
    texlive-font-utils \
    wget unzip \
    && rm -rf /var/lib/apt/lists/*

# Install simpleicons LaTeX package from CTAN (not available in Ubuntu apt repos)
RUN mkdir -p /usr/local/share/texmf/tex/latex/simpleicons && \
    wget -q -O /tmp/simpleicons.zip https://mirrors.ctan.org/fonts/simpleicons.zip && \
    echo "EXPECTED_SHA256_HERE /tmp/simpleicons.zip" | sha256sum -c - && \
    unzip -o /tmp/simpleicons.zip -d /tmp/simpleicons && \
    cp -r /tmp/simpleicons/simpleicons/* /usr/local/share/texmf/tex/latex/simpleicons/ && \
    texhash && \
    rm -rf /tmp/simpleicons /tmp/simpleicons.zip

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