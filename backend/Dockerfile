FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src src

RUN ./gradlew --no-daemon clean bootJar -x test

RUN mkdir -p /out && \
    JAR_FILE="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" && \
    test -n "$JAR_FILE" && \
    cp "$JAR_FILE" /out/app.jar

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /out/app.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
