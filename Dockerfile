FROM maven:3.9.3-eclipse-temurin-20-alpine
WORKDIR /app
COPY . .
EXPOSE 8080
ENTRYPOINT ["mvn","jetty:run"]