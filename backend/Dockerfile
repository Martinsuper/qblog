# 多阶段构建：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r qblog && useradd -r -g qblog qblog \
    && mkdir -p /app/logs /app/backups /app/uploads \
    && chown qblog:qblog /app/logs /app/backups /app/uploads \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && apt-get clean
ENV BACKUP_DIRECTORY=/app/backups
USER qblog
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]