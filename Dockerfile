# ---------- Stage 1: Build backend ----------
FROM eclipse-temurin:21-jdk AS build-backend

WORKDIR /build
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw && ./mvnw package -DskipTests -B

# ---------- Stage 2: Build frontend ----------
FROM node:20-alpine AS build-frontend

WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# ---------- Stage 3: Runtime ----------
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

COPY --from=build-backend /build/target/*.jar app.jar
COPY --from=build-frontend /build/dist /app/static

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
