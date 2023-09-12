FROM maven:3.9.3-eclipse-temurin-20-alpine as build

WORKDIR /app
COPY src /app/src
COPY pom.xml /app

RUN mvn clean package -DskipTests

FROM eclipse-temurin:20-jdk-alpine

COPY --from=build /app/target/*-fat.jar rinha-backend-java.jar 

#EXPOSE 8080
#EXPOSE 8085
ENTRYPOINT [ "java","--enable-preview", "-jar", "rinha-backend-java.jar"]

