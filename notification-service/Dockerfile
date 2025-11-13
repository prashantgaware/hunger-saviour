FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY notification-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
