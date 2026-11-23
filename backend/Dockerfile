# Multi-stage Dockerfile for Backend (Java 25 LTS / Spring Boot 3.5.x)
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Cache das dependências Maven
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Compilação da aplicação
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime enxuto e seguro
FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app

# Usuário não-root para conformidade OWASP e segurança de execução
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

USER appuser
EXPOSE 8082

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
