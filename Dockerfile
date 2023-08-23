FROM maven:3.9.3-eclipse-temurin-17-alpine as build

WORKDIR /app
COPY src /app/src
COPY pom.xml /app

RUN mvn clean package -DskipTests

FROM jetty:12.0.0-jdk17-alpine

COPY --from=build --chown=jetty:jetty /app/target/rinha-backend-jetty-servlet.war $JETTY_BASE/webapps/ROOT.war 

EXPOSE 8080
CMD ["java","-jar","/usr/local/jetty/start.jar","--add-modules=ee10-deploy,ee10-webapp,ee10-annotations"]