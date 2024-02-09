FROM openjdk:21-jdk

WORKDIR /app

ADD ./build/libs/inventory-service-0.0.1-SNAPSHOT.jar inventory-service.jar

CMD ["java", "-jar", "inventory-service.jar"]
