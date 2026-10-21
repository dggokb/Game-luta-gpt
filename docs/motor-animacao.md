# Motor de animação (motion-core)

O movimento por cima dos quadros desenhados. A arte continua vindo dos vídeos (Seedance/Omni);
o motor mexe no desenho na hora de pintar: achata e estica, inclina, mistura a troca de clipe e
deixa rastro. **Só apresentação**: o motor de combate, o frame data e as caixas não mudam (a
ferramenta de revisão confere que o estado da luta sai igual com o motor ligado e desligado).

## O que ele faz

| Efeito | Quando | Padrão |
|---|---|---|
| Achatar | ao aterrissar (mais forte quanto mais rápido caía), volta com um balanço | até 9% da altura |
| Esticar | ao sair do chão | 6% |
| Inclinação de impacto | apanhando, o tronco cede para o lado do empurrão (depois do hitstop) | 3° fraco a 8° especial; 2° defendendo |
| Inclinação de corrida | dash e air dash para a frente; para trás no backdash | 4° |
| Estalo | quando o golpe próprio fica ativo, um tranco curto de esticar na horizontal | 1,5% a 4,5% |
| Transição | troca de clipe fora de golpes, reações, decolagem e aterrissagem: o quadro de antes some em 2 quadros | 35% |
| Rastro | golpe forte, especial, Super e dashes: até 3 cópias azuladas onde o lutador estava | 35%, vivem 8 quadros |

Tudo com os pés parados (a raiz é o pivô) e o "para a frente" segue para onde o lutador olha.

**No jogo:** o botão **ANIM** (no lugar do antigo DUPLA, abaixo de VIDA P1/CPU) e a tecla **F4**
passam por 100% → 150% → 50% → desligado.

## Como funciona

- **Molas de forma fechada** (`Mola`): o valor em qualquer quadro sai de uma conta, sem simular
  os quadros de antes. Quatro tipos prontos: `RAPIDA`, `PADRAO`, `PESADA`, `ELASTICA`.
- **Canal** (`Canal`): um valor animado (achatar, esticar, inclinar) = alvo + soma de molas, uma
  por evento. Mudar o alvo no meio do caminho não reinicia nada: soma uma mola com a diferença e o
  valor segue contínuo.
- **Relógio próprio**: só anda fora do hitstop e da pausa do Super, então o tranco de um golpe
  começa quando a luta volta a andar. Tudo é conta do quadro desse relógio: o mesmo jogo dá a
  mesma animação (serve para rollback).
- **Leitura → Pose**: a cada passo o jogo entrega uma `Leitura` (no chão, velocidade, clipe e
  quadro, golpe ativo e peso, dash, apanhou/defendeu e para que lado, congelado) e o motor
  devolve uma `Pose` (escala X/Y, inclinação, quadro de antes com opacidade, cópias do rastro).
- **Ajustes** (`Ajustes`): cada efeito liga e desliga sozinho, e `intensidade` escala todos.

## Onde fica

| Parte | Arquivo |
|---|---|
| Motor (Java puro, sem Android) | `android/motion-core/` (`Mola`, `Canal`, `Leitura`, `Pose`, `Ajustes`, `MotorAnimacao`) |
| Testes do motor | `android/motion-core/src/test/` — `gradle :motion-core:test` |
| Ponte com a luta | `app/.../AnimacaoLutador.java` (lê o `CombatFighter` e os `HitEvent` do passo) |
| Desenho | `SpriteFighterRenderer`: `draw` aplica a pose em volta dos pés e a transição; `drawRastro` desenha as cópias |
| Ganchos no jogo | `GameView`: `updateFighterSprite` chama `renderer.animacao.passo(...)`; `drawFighter` chama `renderer.drawRastro(c)`; `startIntro` zera; botão ANIM e F4 |
| Revisão | `gradle :pc:revisarAnimacao` (abaixo) |

## Revisar no PC

```bash
cd android
gradle :pc:revisarAnimacao
# pc/build/revisao-animacao/{desligado,ligado}/0000.png ... + estado.txt de cada um
ffmpeg -framerate 60 -i pc/build/revisao-animacao/desligado/%04d.png \
       -framerate 60 -i pc/build/revisao-animacao/ligado/%04d.png \
       -filter_complex hstack -pix_fmt yuv420p lado-a-lado.mp4
```

O roteiro (`RevisaoAnimacao`) joga 380 quadros: pulo parado, dash, L > M > H no CPU, backdash e
pulo para a frente. Os dois `estado.txt` têm de ser iguais (o motor não mexe na luta).
`-Pmodos=desligado,ligado` escolhe as pastas (um nome que começa com "ligado" liga o motor).

## Levar para a sprite-gpt

Os commits desta branch estão separados: os de "Versão leve" só enxugam o jogo; o do motor só
acrescenta o motor. Para levar:

1. Copie `android/motion-core/` inteiro e inclua no `android/settings.gradle`
   (`include ':motion-core'`).
2. Dependência em `android/app/build.gradle` e `android/pc/build.gradle`:
   `implementation project(':motion-core')`.
3. Copie `AnimacaoLutador.java` para `app/src/main/java/com/gamelutagpt/`.
4. `SpriteFighterRenderer`: o campo `animacao`, o `animacao.zerar()` no `setCharacter`, o `draw`
   com `aplicarPose`/`drawClip`/transição, o `drawRastro` e o filtro `rastro`. (O `drawClip` é o
   corpo antigo do `draw` com o clipe como parâmetro.)
5. `GameView`: no fim de `updateFighterSprite`, `renderer.animacao.passo(f, renderer.motion,
   engine, fighterVisualY(f))`; no `drawFighter`, `renderer.drawRastro(c)` antes do `c.save()`;
   no `startIntro`, zerar os dois renderers.
6. Opcional (botão/tecla): `Control.ANIMACAO` no `ControlsLayout`, `Action.ANIMACAO` (F4) no
   `HardwareInput`, `animacaoLabel`/`animacaoLigada` no `HudRenderer.State` e o botão no
   `drawTestButtons`. Na sprite-gpt o lugar do DUPLA está ocupado: escolha outro ou use só o F4.
7. `gradle :motion-core:test :pc:testarMotor` e `gradle :pc:revisarAnimacao` para conferir.

Com outros personagens nada muda: o motor só lê o clipe e o estado da luta, sem nada por
personagem.

## Próximos passos possíveis

- Ajustar os números no celular (ANIM 50/100/150%) e fixar o padrão.
- Quadros intermediários fora do jogo nos golpes de 3 a 4 quadros (fluxo óptico, como no
  `tools/sprites/animar.py`).
- Peso por personagem (um `Ajustes` no pack: o tanque achata menos, o leve mais).
- Molas também na câmera (soco e volta do zoom) no lugar do decaimento fixo.
