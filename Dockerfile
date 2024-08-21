# Etapa 1: Compilación
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM tomcat:10.1-jdk17-corretto
WORKDIR /usr/local/tomcat/webapps/
COPY --from=build /app/target/concurrencia-0.0.1-SNAPSHOT.war ./ROOT.war
EXPOSE 8080