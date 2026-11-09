#!/bin/bash
# O jogo de verdade sem tela, gravando os quadros de um roteiro (o GravarLuta do módulo :pc).
#   gravar.sh <pasta> [LxA] passo [passo ...]
#   gravar.sh /tmp/esp espera:1 grava:90:esp especial:J fim
#   DUPLA=p11,p13,player_base,p04 gravar.sh /tmp/bisturi 960x540 espera:1 grava:80:b especial:J fim
# Os passos estão em android/pc/src/main/java/com/gamelutagpt/pc/GravarLuta.java.
. "$(dirname "$0")/comum.sh"
pasta="$1"; shift
case "$1" in [0-9]*x[0-9]*) tela="$1"; shift;; *) tela=960x540;; esac
rm -rf "$pasta"; mkdir -p "$pasta"
# Sem o plugin de WEBP as folhas passam pelo Pillow na primeira vez (lento); depois ficam em $B/webp.
cd "$A/pc" && timeout ${TMO:-600} java -Xmx4g $JAVA_OPCOES ${DUPLA:+-Dgameluta.dupla=$DUPLA} ${CARGA:+-Dgameluta.carga=$CARGA} \
  -cp "$OUT" com.gamelutagpt.pc.GravarLuta "$pasta" "$tela" "$@" 2>&1 \
  | grep -v "JAVA_TOOL_OPTIONS\|Missing selection art\|I/Quadros\|não carregou" | tail -6
