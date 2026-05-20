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