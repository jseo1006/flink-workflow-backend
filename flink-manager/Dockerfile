FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY flink-manager/pom.xml flink-manager/
COPY . .
RUN mvn clean package -pl flink-manager -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/flink-manager/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
