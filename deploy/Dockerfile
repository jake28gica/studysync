FROM maven:3.9.4-amazoncorretto-17 AS build
WORKDIR /app
COPY ../mvnw .
COPY ../.mvn .mvn
COPY ../pom.xml .
RUN ./mvnw dependency:go-offline
COPY ../src src
RUN ./mvnw package  -DskipTests

FROM amazoncorretto:17-alpine
WORKDIR /app
RUN apk add --no-cache curl
RUN mkdir -p /tmp/vendor-b
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=build /app/target/studysync-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]