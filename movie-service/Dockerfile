# ############## STAGE 1: BUILD ##############
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./pom.xml

COPY movie-service/pom.xml ./movie-service/pom.xml

RUN mvn -f pom.xml -pl movie-service -am -B -DskipTests dependency:go-offline

COPY movie-service/src ./movie-service/src

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