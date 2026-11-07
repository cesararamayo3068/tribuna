FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -ntp package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S tribuna && adduser -S tribuna -G tribuna
WORKDIR /app
COPY --from=build /app/target/tribuna-api-0.1.0.jar app.jar
USER tribuna
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
