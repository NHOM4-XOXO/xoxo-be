# Sử dụng JDK 21 để chạy ứng dụng
FROM eclipse-temurin:21-jdk AS runtime

# Đặt thư mục làm việc
WORKDIR /app

# Copy file jar đã build sẵn từ target vào container
COPY target/*.jar app.jar

# Expose port (ví dụ: 8080)
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
