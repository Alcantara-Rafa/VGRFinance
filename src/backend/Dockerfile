# Imagem de produção da API, usada pelo Render.
# Duas etapas: a primeira compila, a segunda carrega só o resultado.

# Etapa 1: compilar com Maven + Java 21
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build
# Copiar o pom primeiro permite ao Docker reaproveitar as bibliotecas baixadas
# quando só o código mudou.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Etapa 2: rodar só com o Java (imagem bem menor)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Usuário comum: o programa não precisa de poderes de administrador.
RUN useradd --system --no-create-home vgr
USER vgr
COPY --from=build /build/target/*.jar app.jar
ENV TZ=America/Sao_Paulo
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
