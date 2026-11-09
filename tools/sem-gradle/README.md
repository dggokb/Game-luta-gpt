# O jogo e os testes sem Gradle

Para ambientes em que o Gradle não consegue baixar as dependências (Maven bloqueado). Com o Gradle
funcionando, use ele: `gradle :pc:testarMotor`, `gradle :pc:capturar`, `gradle :pc:gravarLuta`.

Aqui tudo é compilado só com o `javac`, e o que viria do Maven é trocado por algo local:

| No Gradle | Aqui |
|---|---|
| LWJGL (OpenGL) | `GLES30`, `GLUtils` e `DesenhoGl` vazios, gerados por `sem_lwjgl.py`: a luta sai pelo Java2D |
| `org.json` | `java/org/json/`: um org.json mínimo (lê e escreve o que o jogo usa) |
| plugin de WEBP (TwelveMonkeys) | `java/android/graphics/WebpPonte.java`: converte pelo Pillow e guarda o PNG por hash |
| Robolectric | não tem: os testes que usam ficam de fora (`EfeitosVideoTest`, as telas de ajuste) |
| JUnit | o `junit-4*.jar` e o `hamcrest-core` que vêm com o Gradle instalado (ou `JUNIT_CP=a.jar:b.jar`) |

Precisa de JDK 17 ou mais novo, `python3` e Pillow.

```
tools/sem-gradle/montar.sh                    # compila o jogo de PC e os testes do motor
tools/sem-gradle/testes.sh                    # todos os testes (uma linha por classe)
tools/sem-gradle/testes.sh MechanicsTest      # só uma classe
tools/sem-gradle/gravar.sh /tmp/esp espera:1 grava:90:esp especial:J fim
python3 tools/sem-gradle/folha_quadros.py /tmp/esp esp /tmp/esp.png 30,40,50,60 - 4
```

- As classes e os arquivos gerados ficam fora do repositório, em `$SOKON_BUILD` (padrão:
  `/tmp/sokon-sem-gradle`). Depois de mudar código, rode `montar.sh` de novo.
- A primeira gravação é lenta: cada folha WEBP passa pelo Pillow uma vez (fica em
  `$SOKON_BUILD/webp`). As seguintes carregam em poucos segundos.
- Os passos do roteiro de `gravar.sh` estão no começo de
  `android/pc/src/main/java/com/gamelutagpt/pc/GravarLuta.java`. `DUPLA=p11,p13,player_base,p04`
  escolhe os lutadores.
- A gravação é o desenho do PC em Java2D, não o OpenGL do celular: serve para ver golpe, tempo,
  efeito e HUD; o acabamento final se confere no aparelho.
- O `build_characters.py --check` não depende disto (é Python puro).
