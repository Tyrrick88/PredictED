FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -Dmaven.test.skip=true dependency:go-offline

COPY backend/src backend/src
RUN mvn -f backend/pom.xml -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system predicted \
    && useradd --system --gid predicted --home-dir /app predicted \
    && mkdir -p /data/uploads \
    && chown -R predicted:predicted /app /data

COPY --from=build --chown=predicted:predicted /workspace/backend/target/predicted-api-*.jar /app/predicted-api.jar

USER predicted
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/predicted-api.jar"]
