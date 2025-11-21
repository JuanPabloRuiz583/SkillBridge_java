# Etapa de build
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copia todos os arquivos do projeto
COPY . .

# Dá permissão de execução ao Gradle wrapper e compila o projeto
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar

# Etapa de execução
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copia o .jar do build
COPY --from=builder /app/build/libs/*.jar app.jar

# Expõe a porta padrão
EXPOSE 8080

# Comando para rodar o app
ENTRYPOINT ["java", "-jar", "app.jar"]