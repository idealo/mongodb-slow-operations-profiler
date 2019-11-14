FROM alpine/git AS cloner
RUN cd /root && git clone https://github.com/idealo/mongodb-slow-operations-profiler.git

FROM maven:3-ibmjava-8-alpine AS builder
COPY --from=cloner /root/mongodb-slow-operations-profiler/ /usr/src/app/
WORKDIR /usr/src/app/
RUN mvn package

FROM tomcat:9-jdk11-openjdk-slim
COPY --from=builder /usr/src/app/target/mongodb-slow-operations-profiler.war /usr/local/tomcat/webapps/
RUN chown -R nobody /usr/local/tomcat/webapps/
USER nobody