# Estágio 1: Build da aplicação
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Baixa as dependências primeiro para aproveitar cache de camadas
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte e gera o JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Estágio 2: Runtime enxuto (Ubuntu/glibc para compatibilidade com webp-imageio e AWT)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Cria usuário não-root e diretório de uploads com permissão adequada
RUN groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8080 9090

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]

