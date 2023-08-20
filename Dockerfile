FROM maven:3.9.3-eclipse-temurin-20-alpine as build

WORKDIR /app
COPY src /app/src
COPY pom.xml /app

RUN mvn clean package -DskipTests

FROM eclipse-temurin:20-jdk-alpine

COPY --from=build /app/target/rinha-backend-jetty-servlet*.jar rinha-backend-jetty-servlet.jar

EXPOSE 8080
ENTRYPOINT [ "java", "-XX:+UseParallelGC", "-XX:MaxRAMPercentage=75", "--enable-preview", "-jar", "rinha-backend-jetty-servlet.jar" ]