#!/bin/bash
# Compila o projeto inteiro com Maven em um Mac e roda os testes.
#
# Uso, no Terminal:
#   bash ~/Documents/ApuradorJifEntrega/apurador-jif/scripts/compilar-no-mac.sh
#
# O que este script faz:
#   1. procura um JDK 21 ou mais novo (se não achar, baixa o Temurin 21 para dentro da pasta);
#   2. baixa o Maven 3.9.9 para _fora-da-entrega/_maven-offline (não instala nada no sistema);
#   3. baixa as bibliotecas do projeto para _fora-da-entrega/_maven-offline/m2;
#   4. compila os módulos e roda os testes (o TI-02 só roda se houver Docker);
#   5. grava a saída em _fora-da-entrega/_logs e empacota as bibliotecas em _fora-da-entrega/_pacotes/maven-offline.tar,
#      para o build poder ser repetido em uma máquina sem acesso ao Maven Central.
#
# Tudo o que o script baixa ou grava fica em ApuradorJifEntrega/_fora-da-entrega, que não faz parte
# da entrega. Nada é alterado fora da pasta ApuradorJifEntrega.

set -u
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
ENTREGA="$(cd "$RAIZ/.." && pwd)"
FORA="$ENTREGA/_fora-da-entrega"
LOGS="$FORA/_logs"
OFF="$FORA/_maven-offline"
PACOTES="$FORA/_pacotes"
MAVEN_VERSAO="3.9.9"
mkdir -p "$LOGS" "$OFF" "$PACOTES"
rm -f "$LOGS/build-mac.fim"
exec > >(tee "$LOGS/build-mac.log") 2>&1

echo "== Apurador JIF: build no Mac =="
date

versao_do_java() {
    "$1/bin/java" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/'
}

usar_se_servir() {
    if [ -n "$1" ] && [ -x "$1/bin/java" ]; then
        local v
        v="$(versao_do_java "$1")"
        if [ "$v" -ge 21 ] 2>/dev/null; then
            export JAVA_HOME="$1"
            return 0
        fi
    fi
    return 1
}

achar_jdk() {
    usar_se_servir "${JAVA_HOME:-}" && return 0
    # O /usr/libexec/java_home só informa; ele não abre a janela de instalação do macOS.
    local h
    h="$(/usr/libexec/java_home -F -v 21+ 2>/dev/null || true)"
    usar_se_servir "$h" && return 0
    local d
    for d in /opt/homebrew/opt/openjdk@21 /usr/local/opt/openjdk@21 /opt/homebrew/opt/openjdk /usr/local/opt/openjdk; do
        usar_se_servir "$d/libexec/openjdk.jdk/Contents/Home" && return 0
    done
    for d in "$OFF"/jdk/*/Contents/Home; do
        usar_se_servir "$d" && return 0
    done
    # O /usr/bin/java do macOS é só um atalho: sem JDK instalado, ele abre uma janela
    # pedindo para instalar o Java e fica parado. Por isso ele não é usado aqui.
    return 1
}

baixar_jdk() {
    local arq
    case "$(uname -m)" in
        arm64) arq=aarch64 ;;
        *) arq=x64 ;;
    esac
    local url="https://api.adoptium.net/v3/binary/latest/21/ga/mac/$arq/jdk/hotspot/normal/eclipse"
    echo "Nenhum JDK 21 encontrado. Baixando o Temurin 21 ($arq) para a pasta _fora-da-entrega/_maven-offline/jdk."
    echo "Nada é instalado no sistema."
    mkdir -p "$OFF/jdk"
    if ! curl -fL --progress-bar "$url" -o "$OFF/jdk/temurin21.tar.gz"; then
        return 1
    fi
    tar -xzf "$OFF/jdk/temurin21.tar.gz" -C "$OFF/jdk" && rm -f "$OFF/jdk/temurin21.tar.gz"
}

echo
echo "== 1. JDK =="
echo "Procurando um JDK 21 ou mais novo..."
if ! achar_jdk; then
    baixar_jdk && achar_jdk
fi
if [ -z "${JAVA_HOME:-}" ] || ! usar_se_servir "$JAVA_HOME"; then
    echo "ERRO: não achei nem consegui baixar um JDK 21."
    echo "Instale o Temurin 21 (https://adoptium.net) e rode o script de novo."
    echo "FALHOU: sem JDK" > "$LOGS/build-mac.fim"
    exit 2
fi
echo "JAVA_HOME=$JAVA_HOME"
"$JAVA_HOME/bin/java" -version

echo
echo "== 2. Maven $MAVEN_VERSAO =="
MVN_HOME="$OFF/apache-maven-$MAVEN_VERSAO"
if [ ! -x "$MVN_HOME/bin/mvn" ]; then
    URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSAO/apache-maven-$MAVEN_VERSAO-bin.tar.gz"
    echo "Baixando $URL"
    if ! curl -fsSL "$URL" -o "$OFF/maven.tar.gz"; then
        echo "ERRO: não consegui baixar o Maven."
        echo "FALHOU: download do Maven" > "$LOGS/build-mac.fim"
        exit 3
    fi
    tar -xzf "$OFF/maven.tar.gz" -C "$OFF"
    rm -f "$OFF/maven.tar.gz"
fi
MVN="$MVN_HOME/bin/mvn -B -ntp -Dmaven.repo.local=$OFF/m2"
$MVN -v

echo
echo "== 3. Bibliotecas (projeto de aquecimento) =="
# Um projeto mínimo com as mesmas dependências e os mesmos plugins do Apurador.
# Ele garante que tudo fique baixado mesmo que algum módulo ainda não compile.
AQ="$OFF/aquecimento"
mkdir -p "$AQ/src/main/java/aq" "$AQ/src/test/java/aq"
cat > "$AQ/pom.xml" <<'POM'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>
    <groupId>aq</groupId>
    <artifactId>aquecimento</artifactId>
    <version>1</version>
    <properties><java.version>21</java.version></properties>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
        <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
        <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.amqp</groupId><artifactId>spring-rabbit-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.testcontainers</groupId><artifactId>rabbitmq</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId><artifactId>maven-failsafe-plugin</artifactId>
                <executions><execution><goals><goal>integration-test</goal><goal>verify</goal></goals></execution></executions>
            </plugin>
        </plugins>
    </build>
</project>
POM
cat > "$AQ/src/main/java/aq/App.java" <<'JAVA'
package aq;

public class App {
    public static void main(String[] args) {
        System.out.println("ok");
    }
}
JAVA
cat > "$AQ/src/test/java/aq/AppTest.java" <<'JAVA'
package aq;

import org.junit.jupiter.api.Test;

class AppTest {
    @Test
    void roda() {
    }
}
JAVA
cat > "$AQ/src/test/java/aq/AppIT.java" <<'JAVA'
package aq;

import org.junit.jupiter.api.Test;

class AppIT {
    @Test
    void roda() {
    }
}
JAVA
(cd "$AQ" && $MVN -q clean install dependency:resolve-plugins)
AQ_STATUS=$?
echo "Aquecimento terminou com código $AQ_STATUS"

echo
echo "== 4. Build do Apurador JIF =="
(cd "$RAIZ" && $MVN -fae clean verify)
BUILD_STATUS=$?
echo "Build terminou com código $BUILD_STATUS"

echo
echo "== 4b. Módulo de integração (TI-02): só compila, sem rodar =="
(cd "$RAIZ" && $MVN -fae -Pintegracao -DskipITs -DskipTests install)
IT_STATUS=$?
echo "Compilação do TI-02 terminou com código $IT_STATUS"
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    echo
    echo "== 4c. Docker encontrado: rodando o TI-02 com Testcontainers =="
    (cd "$RAIZ" && $MVN -Pintegracao -pl integracao verify)
    IT_STATUS=$?
    echo "TI-02 terminou com código $IT_STATUS"
    cp "$RAIZ/integracao/target/ti-02.txt" "$LOGS/ti-02-mac.txt" 2>/dev/null || true
fi

echo
echo "== 5. Pacote para build sem rede =="
rm -f "$PACOTES"/maven-offline.tar "$PACOTES"/maven-offline.tar.parte-*
tar -cf "$PACOTES/maven-offline.tar" -C "$OFF" "apache-maven-$MAVEN_VERSAO" m2
TAMANHO=$(du -m "$PACOTES/maven-offline.tar" | cut -f1)
echo "maven-offline.tar: ${TAMANHO} MB"
if [ "$TAMANHO" -gt 350 ]; then
    (cd "$PACOTES" && split -b 300m maven-offline.tar maven-offline.tar.parte- && rm -f maven-offline.tar)
    echo "Dividido em partes de 300 MB."
fi
ls -l "$PACOTES" | grep maven-offline

echo
echo "== 6. Ambiente =="
{
    echo "data: $(date)"
    sw_vers 2>/dev/null
    echo "arquitetura: $(uname -m)"
    echo "memoria_bytes: $(sysctl -n hw.memsize 2>/dev/null)"
    echo "nucleos: $(sysctl -n hw.ncpu 2>/dev/null)"
    echo "JAVA_HOME: $JAVA_HOME"
    for f in docker kubectl k3d k3s k6 helm node npm brew colima minikube kind multipass; do
        if command -v $f >/dev/null 2>&1; then echo "$f: $(command -v $f)"; else echo "$f: nao instalado"; fi
    done
    docker version --format 'docker servidor: {{.Server.Version}}' 2>&1 | head -1
    docker compose version 2>&1 | head -1
} > "$LOGS/ambiente-mac.txt" 2>&1
cat "$LOGS/ambiente-mac.txt"

echo
if [ "$BUILD_STATUS" -eq 0 ]; then
    echo "PRONTO: o projeto compilou e os testes passaram."
else
    echo "O build terminou com erro. A saída completa está em _fora-da-entrega/_logs/build-mac.log."
fi
echo "aquecimento=$AQ_STATUS build=$BUILD_STATUS integracao=$IT_STATUS" > "$LOGS/build-mac.fim"
echo "Pode voltar ao Claude e avisar que rodou."
