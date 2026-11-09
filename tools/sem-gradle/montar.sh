#!/bin/bash
# Compila o jogo de PC e os testes JUnit do motor só com javac, sem Gradle e sem Maven.
# Para ambientes em que o Gradle não baixa as dependências. Ver README.md desta pasta.
set -e
. "$(dirname "$0")/comum.sh"
mkdir -p "$GEN/com/gamelutagpt" "$OUT"
python3 "$AQUI/sem_lwjgl.py" "$A" "$GEN"
# A calibração do P01 e o BuildConfig, que no Gradle são gerados no build.
(cd "$REPO" && python3 tools/sprites/generate_p01_calibration.py --output "$GEN/com/gamelutagpt/P01SpriteCalibration.java" >/dev/null)
VN=$(grep -o "versionName\s*'[^']*'" "$A/app/build.gradle" | sed "s/.*'\(.*\)'/\1/")
cat > "$GEN/com/gamelutagpt/BuildConfig.java" <<EOT
package com.gamelutagpt;
public final class BuildConfig {
    public static final boolean DEBUG = true;
    public static final String APPLICATION_ID = "com.gamelutagpt";
    public static final String BUILD_TYPE = "pc";
    public static final int VERSION_CODE = 1;
    public static final String VERSION_NAME = "$VN";
    private BuildConfig() {}
}
EOT
# Os testes que não precisam do Robolectric (nem das duas telas de ajuste, que usam o Android de verdade).
grep -L "org.robolectric" $(find "$A/app/src/test/java" -name "*.java") | grep -v "AjusteTamanhoTest\|AjusteCaixasTest" > "$B/testes.txt"
rm -rf "$OUT"; mkdir -p "$OUT"
javac -nowarn -encoding UTF-8 -Xlint:-options -proc:none -Xmaxerrs 200 -d "$OUT" -cp "$JUNIT_CP" -sourcepath "$SP" \
  @"$B/testes.txt" "$A"/pc/src/main/java/com/gamelutagpt/pc/{CapturaPc,GravarLuta,MedirLuta}.java 2> "$B/javac.log" \
  || { grep "error:" "$B/javac.log" | head -20; echo "falhou: $B/javac.log"; exit 1; }
echo "compilado em $OUT ($(wc -l < "$B/testes.txt") arquivos de teste)"
