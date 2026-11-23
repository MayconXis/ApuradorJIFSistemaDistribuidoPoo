#!/bin/bash
# Compila e roda os testes do core e do cluster só com o JDK e o JUnit Console Launcher,
# sem Maven e sem rede. Foi assim que as saídas em docs/03-relatorio/saida-dos-testes
# foram geradas. Com Maven instalado, o equivalente é: mvn -pl core,cluster -am test
#
# Uso: scripts/testar-sem-maven.sh [caminho do junit-platform-console-standalone.jar]
set -euo pipefail
cd "$(dirname "$0")/.."
JUNIT="${1:-/usr/share/java/junit-platform-console-standalone.jar}"
SAIDA="${TMPDIR:-/tmp}/apurador-testes"
mkdir -p "$SAIDA/core" "$SAIDA/core-test" "$SAIDA/cluster" "$SAIDA/cluster-test" docs/03-relatorio/saida-dos-testes
JAVAC="javac -encoding UTF-8 --release 21 -Xlint:all"
JAVA="java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"
OPCOES="--scan-class-path --details=tree --details-theme=ascii --disable-ansi-colors --disable-banner"

echo "== core =="
$JAVAC -d "$SAIDA/core" $(find core/src/main/java -name "*.java")
$JAVAC -cp "$SAIDA/core:$JUNIT" -d "$SAIDA/core-test" $(find core/src/test/java -name "*.java")
$JAVA -jar "$JUNIT" execute --class-path "$SAIDA/core:$SAIDA/core-test" $OPCOES | tee docs/03-relatorio/saida-dos-testes/core.txt

echo "== cluster =="
$JAVAC -cp "$SAIDA/core" -d "$SAIDA/cluster" $(find cluster/src/main/java -name "*.java")
$JAVAC -cp "$SAIDA/core:$SAIDA/cluster:$JUNIT" -d "$SAIDA/cluster-test" $(find cluster/src/test/java -name "*.java")
$JAVA -jar "$JUNIT" execute --class-path "$SAIDA/core:$SAIDA/cluster:$SAIDA/cluster-test" $OPCOES | tee docs/03-relatorio/saida-dos-testes/cluster.txt
