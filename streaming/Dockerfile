# syntax=docker/dockerfile:1.6

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY streaming/pom.xml ./streaming/pom.xml
COPY shared-kernel/pom.xml ./shared-kernel/pom.xml
COPY movie-service/pom.xml ./movie-service/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
RUN mvn -f pom.xml -pl streaming -am -B -DskipTests dependency:go-offline
COPY streaming/src ./streaming/src
RUN mvn -f pom.xml -pl streaming -am -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/streaming/target/*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring app.jar
USER spring
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD curl -f http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
CMD ["$JAVA_OPTS"]
