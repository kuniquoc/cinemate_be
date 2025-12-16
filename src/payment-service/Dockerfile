# ############## STAGE 1: BUILD ##############
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent pom.xml first
COPY pom.xml ./pom.xml

# Copy all service pom.xml files to resolve dependencies
COPY shared-kernel/pom.xml ./shared-kernel/pom.xml
COPY movie-service/pom.xml ./movie-service/pom.xml
COPY streaming-signaling/pom.xml ./streaming-signaling/pom.xml
COPY streaming-seeder/pom.xml ./streaming-seeder/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
COPY customer-service/pom.xml ./customer-service/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml

# Download dependencies for payment service and its dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -pl payment-service -am -B -DskipTests dependency:go-offline

# Copy shared-kernel source (if exists)
COPY shared-kernel/src ./shared-kernel/src

# Copy payment service source
COPY payment-service/src ./payment-service/src

# Build payment service with all dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -pl payment-service -am -B -DskipTests package


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
COPY --from=build /app/payment-service/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

# HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
#     CMD curl -f http://localhost:8585/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["$JAVA_OPTS"]
