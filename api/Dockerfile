# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# Stage 2: Layer extraction
FROM builder AS extractor
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# Stage 3: Runtime
FROM eclipse-temurin:25-jre-alpine
ARG STACKOVERFLOW_API_KEY=
ENV STACKOVERFLOW_API_KEY=$STACKOVERFLOW_API_KEY
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=extractor /app/target/extracted/dependencies/ ./
COPY --from=extractor /app/target/extracted/spring-boot-loader/ ./
COPY --from=extractor /app/target/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/target/extracted/application/ ./
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
