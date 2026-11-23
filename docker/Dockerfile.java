# Imagem do Dispatcher ou do worker. O módulo é escolhido pelo argumento MODULO.
#   docker build -f docker/Dockerfile.java --build-arg MODULO=dispatcher -t apurador/dispatcher:1.0.0 .
#   docker build -f docker/Dockerfile.java --build-arg MODULO=worker     -t apurador/worker:1.0.0 .
# O contexto do build é a raiz do repositório, porque o módulo depende de core, cluster e persistencia.

FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULO
WORKDIR /src
COPY pom.xml .
COPY core/pom.xml core/
COPY cluster/pom.xml cluster/
COPY persistencia/pom.xml persistencia/
COPY dispatcher/pom.xml dispatcher/
COPY worker/pom.xml worker/
# Baixa as dependências numa camada própria: mudar o código não baixa tudo de novo.
RUN mvn -B -ntp -q -pl ${MODULO} -am dependency:go-offline -DexcludeArtifactIds=core,cluster,persistencia || true
COPY . .
RUN mvn -B -ntp -q -pl ${MODULO} -am package -DskipTests \
 && cp ${MODULO}/target/${MODULO}-*-exec.jar /src/app.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 apurador
USER apurador
WORKDIR /app
COPY --from=build /src/app.jar app.jar
EXPOSE 8080 8081
# MaxRAMPercentage: a JVM respeita o limite de memória do container.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
