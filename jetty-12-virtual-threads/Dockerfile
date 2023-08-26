FROM maven:3.9.3-eclipse-temurin-20-alpine as build

ENV JETTY_HOME /usr/local/jetty
ENV JETTY_BASE /var/lib/jetty

RUN set -xe ; \
    
    mkdir -p $JETTY_HOME; \
    mkdir -p $JETTY_BASE; \
    
    cd $JETTY_BASE; \
     
    wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-home/12.0.0/jetty-home-12.0.0.tar.gz; \
    tar -xzvf jetty-home-12.0.0.tar.gz; \
    mv jetty-home-12.0.0/* $JETTY_HOME; \
    
    java -jar $JETTY_HOME/start.jar --add-modules=server,http,ext,resources,ee10-deploy,ee10-webapp,ee10-annotations,threadpool-virtual-preview,logging/slf4j; \
     
    rm -rf jetty-home-12.0.0.tar.gz; \
    rm -rf jetty-home-12.0.0; 

COPY ./jetty-config/threadpool-virtual-preview.ini ${JETTY_BASE}/start.d

WORKDIR /app
COPY src /app/src
COPY pom.xml /app

RUN mvn clean package -DskipTests

RUN set -xe ; \
    cp /app/target/rinha-backend-jetty-servlet.war $JETTY_BASE/webapps/ROOT.war; \
    rm -rf /app;

WORKDIR $JETTY_BASE

EXPOSE 8080
CMD ["java",  "-XX:+UseParallelGC", "-XX:MaxRAMPercentage=75","--enable-preview","-jar","/usr/local/jetty/start.jar"] 
