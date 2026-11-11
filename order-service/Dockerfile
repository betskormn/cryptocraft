FROM eclipse-temurin:21-jdk

WORKDIR /app

ADD ./build/libs/order-service-0.0.1-SNAPSHOT.jar order-service.jar

EXPOSE 8084
EXPOSE 5005

CMD ["java", "-Duser.timezone=UTC", "-jar", "order-service.jar"]