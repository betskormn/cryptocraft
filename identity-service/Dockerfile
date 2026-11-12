FROM eclipse-temurin:21-jdk

WORKDIR /app

ADD ./build/libs/identity-service-0.0.1-SNAPSHOT.jar identity-service.jar

EXPOSE 8086
EXPOSE 5005

CMD ["java", "-Duser.timezone=UTC", "-jar", "identity-service.jar"]