FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /build

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

RUN groupadd -r spring \
    && useradd -r -g spring spring

COPY --from=build /build/target/backend-1.0.0.jar app.jar

RUN chown spring:spring /app/app.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
