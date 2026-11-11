FROM eclipse-temurin:21-jdk

WORKDIR /app

ADD ./build/libs/price-streaming-service-0.0.1-SNAPSHOT.jar price-streaming-service.jar

EXPOSE 8083
EXPOSE 5007

CMD ["java", "-Duser.timezone=UTC", "-jar", "price-streaming-service.jar"]