# ---- 前端构建阶段 ----
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# ---- 后端构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY backend/src ./src
# 前端构建产物直接打进 Spring Boot 静态资源目录，单镜像交付
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app -u 1001 app
WORKDIR /app
COPY --from=backend /app/backend/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
