# ========================
# Stage 1: Build the app
# ========================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml trước để cache dependency
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source và build
COPY src ./src
RUN mvn clean package -DskipTests

# ========================
# Stage 2: Run the app
# ========================
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy jar từ stage build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8585

ENTRYPOINT ["java", "-jar", "app.jar"]
