FROM openjdk:8-jre-alpine
RUN mkdir -p /opt/RestEndpoint
WORKDIR /opt/RestEndpoint
COPY RestEndpoint/target/RestEndpoint-0.0.1-SNAPSHOT.jar /opt/RestEndpoint
CMD ["java", "-jar", "RestEndpoint-0.0.1-SNAPSHOT.jar"]
