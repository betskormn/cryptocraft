FROM eclipse-temurin:21-jdk

WORKDIR /app

ADD ./build/libs/inventory-service-0.0.1-SNAPSHOT.jar inventory-service.jar

EXPOSE 8081
EXPOSE 5005

CMD ["java", "-Duser.timezone=UTC", "-jar", "inventory-service.jar"]