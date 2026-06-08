# Stage 1: Build the backend application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# Copy root POM and games-backend module
COPY pom.xml .
COPY games-backend/pom.xml games-backend/
COPY games-backend/src games-backend/src

# Package the application skipping tests
RUN mvn -f games-backend/pom.xml clean package -DskipTests

# Stage 2: Minimal JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root system user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy packaged JAR from build stage
COPY --from=build /workspace/games-backend/target/games-backend-1.0-SNAPSHOT.jar app.jar

# Assign ownership to appuser
RUN chown appuser:appgroup app.jar

# Run as non-root user
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
