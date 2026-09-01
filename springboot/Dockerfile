# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY maven-settings.xml /root/.m2/settings.xml
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

RUN mkdir -p /app/files /app/logs /app/backup
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 3001
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
