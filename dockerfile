# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Chỉ copy những file cần thiết trước để tận dụng layer cache tốt hơn
COPY pom.xml .
COPY src ./src

# Dùng Maven để build project, skip test để giảm thời gian build
RUN mvn clean package -DskipTests

# Run stage (final image)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy file jar từ builder stage sang
COPY --from=builder /app/target/*.jar app.jar

# Set entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
