# Stage 1: Build the backend with Maven
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy root POM first for dependency caching
COPY pom.xml .
COPY games-backend/pom.xml games-backend/
RUN mvn -f games-backend/pom.xml dependency:go-offline -q || true

# Copy source and build
COPY games-backend/src games-backend/src
RUN mvn -f games-backend/pom.xml clean package -DskipTests

# Stage 2: Minimal runtime
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy fat JAR from build stage
COPY --from=build /workspace/games-backend/target/games-backend-1.0-SNAPSHOT.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# JAVA_OPTS injected by Render (e.g. -Xmx512m -Xms256m); PORT is read from
# application.properties which uses ${PORT:8080}
ENTRYPOINT java $JAVA_OPTS -jar app.jar
