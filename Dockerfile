# Build from remote source code using git clone, Maven and run with Tomcat
FROM alpine/git:2.47.2 AS cloner
RUN cd /root && git clone https://github.com/idealo/mongodb-slow-operations-profiler.git

FROM maven:3.9.12-eclipse-temurin-17 AS builder
COPY --from=cloner /root/mongodb-slow-operations-profiler/ /usr/src/app/
WORKDIR /usr/src/app/
RUN mvn -B clean package

FROM tomcat:9-jdk17-openjdk-slim
COPY --from=builder /usr/src/app/target/mongodb-slow-operations-profiler.war /tmp
WORKDIR /usr/local/tomcat/webapps/mongodb-slow-operations-profiler/
RUN jar -xfv /tmp/mongodb-slow-operations-profiler.war
RUN chown -R nobody:nogroup /usr/local/tomcat/webapps/
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/ || exit 1
USER nobody:nogroup