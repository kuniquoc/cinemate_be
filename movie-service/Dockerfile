# ############## STAGE 1: BUILD ##############
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom.xml first
COPY pom.xml ./pom.xml

# Copy all service pom.xml files to resolve dependencies
COPY shared-kernel/pom.xml ./shared-kernel/pom.xml
COPY movie-service/pom.xml ./movie-service/pom.xml
COPY streaming/pom.xml ./streaming/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml

# Download dependencies for movie-service and its dependencies
RUN mvn -f pom.xml -pl movie-service -am -B -DskipTests dependency:go-offline

# Copy shared-kernel source (if exists)
COPY shared-kernel/src ./shared-kernel/src

# Copy movie-service source
COPY movie-service/src ./movie-service/src

# Build movie-service with all dependencies
RUN mvn -f pom.xml -pl movie-service -am -B -DskipTests package


# ----------------------------------------------------
# ############## STAGE 2: RUNTIME ##############
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk update \
    && apk add --no-cache curl ffmpeg \
    && addgroup -S spring \
    && adduser -S spring -G spring \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# Copy the final JAR from the build stage
COPY --from=build /app/movie-service/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8080


HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["$JAVA_OPTS"]