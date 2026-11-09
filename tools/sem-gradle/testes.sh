#!/bin/bash
# Roda os testes JUnit do motor compilados por montar.sh: todos, ou só as classes pedidas.
#   testes.sh                          todas (uma linha por classe; o que falhar fica em $B/logs)
#   testes.sh MechanicsTest ...        só essas (nome curto ou com.gamelutagpt....)
. "$(dirname "$0")/comum.sh"
mkdir -p "$B/logs"
# Estes três leem arquivos pelo caminho do módulo :app.
NO_APP="UltraDirectorTest CharacterSelectTest NarradorLutaTest"
if [ $# -gt 0 ]; then
  classes=""; for c in "$@"; do case "$c" in *.*) classes="$classes $c";; *) classes="$classes com.gamelutagpt.$c";; esac; done
else
  classes=$(for f in $(grep -l "@Test" $(cat "$B/testes.txt")); do echo "$f" | sed 's#.*/src/test/java/##; s#\.java$##; s#/#.#g'; done)
fi
falhas=0
for c in $classes; do
  pasta="$A/pc"; for n in $NO_APP; do [ "${c##*.}" = "$n" ] && pasta="$A/app"; done
  (cd "$pasta" && timeout ${TMO:-900} java -Xmx3g $JAVA_OPCOES -cp "$OUT:$JUNIT_CP" org.junit.runner.JUnitCore "$c" > "$B/logs/$c.log" 2>&1)
  rc=$?
  [ $rc -ne 0 ] && falhas=$((falhas + 1))
  echo "$c | $([ $rc -eq 0 ] && echo ok || echo FALHOU) | $(grep -E "^OK \(|^Tests run:" "$B/logs/$c.log" | tail -1)"
done
echo "falhas: $falhas"
[ $falhas -eq 0 ]
