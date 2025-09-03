FROM openjdk:21-jdk

WORKDIR /app

ADD ./build/libs/inventory-service-0.0.1-SNAPSHOT.jar inventory-service.jar

EXPOSE 5005

CMD ["java", "-Duser.timezone=UTC -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005", "-jar", "inventory-service.jar"]
