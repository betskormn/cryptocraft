FROM eclipse-temurin:21-jdk

WORKDIR /app

ADD ./build/libs/pricing-service-0.0.1-SNAPSHOT.jar pricing-service.jar

EXPOSE 8082
EXPOSE 5005

CMD ["java", "-Duser.timezone=UTC", "-jar", "pricing-service.jar"]