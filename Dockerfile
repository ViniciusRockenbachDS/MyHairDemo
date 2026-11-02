# =============================================================================
# MyHair — demonstração (site + API num só serviço, para o Render)
# 1) monta o site React  2) compila a API Java  3) imagem final só com o necessário
# =============================================================================

# ---------- 1. site
FROM node:20-alpine AS site
WORKDIR /site
COPY frontend/package*.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
# mesma origem da API (endereços relativos) + modo demonstração
ENV VITE_API_URL="" VITE_DEMO=true
RUN npm run build

# ---------- 2. API
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /api
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
RUN mvn -q -B -DskipTests package

# ---------- 3. imagem final
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=api /api/target/*.jar app.jar
COPY --from=site /site/dist ./static
ENV SPRING_PROFILES_ACTIVE=demo \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
