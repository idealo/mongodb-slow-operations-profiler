FROM tomcat:9-jdk11-openjdk-slim
COPY target/mongodb-slow-operations-profiler.war /tmp
WORKDIR /usr/local/tomcat/webapps/mongodb-slow-operations-profiler/
RUN jar -xfv /tmp/mongodb-slow-operations-profiler.war
RUN chown -R nobody:nogroup /usr/local/tomcat/webapps/
USER nobody:nogroup
