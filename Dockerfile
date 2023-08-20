FROM maven:3.9.3-eclipse-temurin-20-alpine as build

WORKDIR /app
COPY src /app/src
COPY pom.xml /app

RUN mvn clean package -DskipTests assembly:single

FROM eclipse-temurin:20-jdk-alpine

COPY --from=build /app/target/rinha-backend-jetty-servlet-jar-with-dependencies.jar rinha-backend-jetty-servlet-jar-with-dependencies.jar 

EXPOSE 8080
ENTRYPOINT [ "java", "--enable-preview", "-jar", "rinha-backend-jetty-servlet-jar-with-dependencies.jar" ]