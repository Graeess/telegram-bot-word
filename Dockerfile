
FROM openjdk:17-jdk-slim


RUN apt-get update && apt-get install -y maven


WORKDIR /app


COPY pom.xml .


RUN mvn dependency:go-offline


COPY src ./src


RUN mvn clean package -DskipTests

EXPOSE 8080


CMD ["java", "-jar", "target/telegram-bot-word-0.0.1-SNAPSHOT.jar"]

