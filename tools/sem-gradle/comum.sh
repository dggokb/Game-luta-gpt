# Caminhos comuns do modo sem Gradle (incluído pelos outros scripts).
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="${REPO:-$(cd "$AQUI/../.." && pwd)}"
A="$REPO/android"
# Onde ficam as classes compiladas e os arquivos gerados (fora do repositório).
B="${SOKON_BUILD:-${TMPDIR:-/tmp}/sokon-sem-gradle}"
GEN="$B/gerado"; OUT="$B/classes"
# JUnit 4 e Hamcrest: os que vêm com o Gradle instalado servem.
if [ -z "$JUNIT_CP" ]; then
  junit=$(find /opt "$HOME/.gradle" "$HOME/.m2" /usr/share -name 'junit-4*.jar' 2>/dev/null | head -1)
  hamcrest=$(find /opt "$HOME/.gradle" "$HOME/.m2" /usr/share -name 'hamcrest-core-*.jar' 2>/dev/null | head -1)
  JUNIT_CP="$junit:$hamcrest"
fi
SP="$GEN:$AQUI/java:$A/pc/src/substituicoes/java:$A/app/src/main/java:$A/pc/src/main/java:$A/render-core/src/main/java:$A/ultra-core/src/main/java:$A/story-core/src/main/java:$A/stage-core/src/main/java:$A/motion-core/src/main/java:$A/app/src/test/java"
JAVA_OPCOES="-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Dgameluta.repo=$REPO -Dgameluta.semSom=true -Dgameluta.webpcache=$B/webp"
