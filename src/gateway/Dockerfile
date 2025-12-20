# ############## STAGE 1: BUILD ##############
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom.xml first
COPY pom.xml ./pom.xml

# Copy all service pom.xml files to resolve dependencies
COPY shared-kernel/pom.xml ./shared-kernel/pom.xml
COPY movie-service/pom.xml ./movie-service/pom.xml
COPY streaming-seeder/pom.xml ./streaming-seeder/pom.xml
COPY streaming-signaling/pom.xml ./streaming-signaling/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
COPY customer-service/pom.xml ./customer-service/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml

# Download dependencies for gateway and its dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -pl gateway -am -B -DskipTests dependency:go-offline

# Copy shared-kernel source (if exists)
COPY shared-kernel/src ./shared-kernel/src

# Copy gateway source
COPY gateway/src ./gateway/src

# Build gateway with all dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -pl gateway -am -B -DskipTests package


# ----------------------------------------------------
# ############## STAGE 2: RUNTIME ##############
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk update \
    && apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# Copy the final JAR from the build stage
COPY --from=build /app/gateway/target/*.jar app.jar

RUN chown spring:spring app.jar \
    && mkdir -p /tmp/logs \
    && chown -R spring:spring /tmp/logs

USER spring

EXPOSE 8080

ENV LOGGING_FILE_PATH=/tmp/logs

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["$JAVA_OPTS"]