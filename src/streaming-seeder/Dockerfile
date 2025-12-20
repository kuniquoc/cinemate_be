FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
COPY streaming-signaling/pom.xml ./streaming-signaling/pom.xml
COPY streaming-seeder/pom.xml ./streaming-seeder/pom.xml
COPY shared-kernel/pom.xml ./shared-kernel/pom.xml
COPY movie-service/pom.xml ./movie-service/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
COPY customer-service/pom.xml ./customer-service/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -pl streaming-seeder -am -B -DskipTests dependency:go-offline

COPY shared-kernel/src ./shared-kernel/src
COPY streaming-seeder/src ./streaming-seeder/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f pom.xml -pl streaming-seeder -am -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/streaming-seeder/target/*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring && \
    chown spring:spring app.jar && \
    mkdir -p /var/cinemate/cache && \
    chown -R spring:spring /var/cinemate
USER spring
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
CMD ["$JAVA_OPTS"]
