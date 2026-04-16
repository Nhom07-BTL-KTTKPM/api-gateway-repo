FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY shared-library-repo/shared-library /workspace/shared-library
COPY api-gateway-repo/api-gateway /workspace/api-gateway

RUN cd /workspace/shared-library && chmod +x mvnw && ./mvnw -DskipTests clean install
RUN cd /workspace/api-gateway && chmod +x mvnw && ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/api-gateway/target/api-gateway-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
