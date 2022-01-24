FROM alpine/git AS cloner
RUN cd /root && git clone https://github.com/idealo/mongodb-slow-operations-profiler.git

FROM maven:3.8.4-openjdk-11-slim AS builder
COPY --from=cloner /root/mongodb-slow-operations-profiler/ /usr/src/app/
WORKDIR /usr/src/app/
RUN mvn package

FROM tomcat:9-jdk11-openjdk-slim
COPY --from=builder /usr/src/app/target/mongodb-slow-operations-profiler.war /tmp
WORKDIR /usr/local/tomcat/webapps/mongodb-slow-operations-profiler/
RUN jar -xfv /tmp/mongodb-slow-operations-profiler.war
RUN chown -R nobody:nogroup /usr/local/tomcat/webapps/
USER nobody:nogroup