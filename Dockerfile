FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package \
    && JAR_FILE="$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)" \
    && cp "$JAR_FILE" /workspace/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
