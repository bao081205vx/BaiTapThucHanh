FROM openjdk:21-jdk-slim
WORKDIR /app
COPY wait-for-it.sh .
RUN chmod +x wait-for-it.sh
COPY target/Tuan2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080