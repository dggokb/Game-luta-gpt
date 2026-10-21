# Motor de animação (motion-core)

O movimento por cima dos quadros desenhados. A arte continua vindo dos vídeos (Seedance/Omni);
o motor mexe no desenho na hora de pintar: achata e estica, inclina, mistura a troca de clipe e
deixa rastro. **Só apresentação**: o motor de combate, o frame data e as caixas não mudam (a
ferramenta de revisão confere que o estado da luta sai igual com o motor ligado e desligado).

## O que ele faz

Organizado pelos 12 princípios da animação (o que cada efeito resolve):

| Princípio | Efeito | Quando | Padrão |
|---|---|---|---|
| Achatar e esticar | Achatar | ao aterrissar (mais forte quanto mais rápido caía), volta firme | até 3,5% da altura |
| | Esticar | ao sair do chão | 2,5% |
| | Volume | com `volume` ligado, achatar e esticar mantêm a área do desenho (mais cartum, mais borracha) | desligado |
| | Virada | ao virar de lado, o desenho sai estreito e abre de volta (lê como um giro) | 12% da largura |
| | Quique | o corpo caído bate no chão: achata mais e sobe um pouco, sem nunca entrar no chão | 5%, 6 px |
| Antecipação | Preparação | na preparação do golpe (os quadros de startup que ele já tem) o corpo recua e abaixa | 1° fraco a 3° especial; 1% |
| | Agachar para pular | nos quadros de preparação do pulo (o jump squat do jogo, abaixo) o corpo agacha e sai esticando | 3% |
| Continuidade | Seguir o golpe | quando o golpe fica ativo, o corpo segue para a frente e assenta | 1° fraco a 3° especial |
| | Acertar trava, errar passa | acertou (ou foi defendido): o corpo para no rival e recua um pouco; errou: quando a janela fecha no vazio, passa do ponto e volta devagar | recua 1° a 3°; passa 1° a 3,5° |
| | Arrasto | dash: o tronco fica para trás na largada e passa da conta na freada (cisalhamento com os pés parados) | 1,5° na largada, 3° na freada |
| Aceleração | Molas | tudo entra e sai por molas, nunca linear | — |
| Arcos | Arco do pulo | no ar, a cabeça vai para onde ele anda subindo e volta descendo; o pulo da corrida inclina mais. Pulo parado e super pulo reto não inclinam | 2,5° (3,75° na corrida) |
| | Esticar no ar | estica conforme a velocidade vertical (normal no topo do pulo) | até 1,5% |
| Ação secundária | Poeira | aterrissagem: um sopro de cada lado dos pés (personagem pesado: a nuvem inteira); largada e freada do dash e do backdash: o pé deslizando. A queda do knockdown já tinha a dela | — |
| | Defesa pesada | defender golpe forte arrasta o pé para trás duas vezes no empurrão | forte ou mais |
| | Sombra de contato | embaixo dos dois: encolhe e clareia com a altura e abre com o achatar da aterrissagem | some até 35% a 420 px |
| | Cansaço | vida abaixo de 25%, parado: respira fundo, ombros caídos e o idle 20% mais lento | 0,6%, 1,5° |
| Tempo | Mola por peso | o golpe fraco volta ligeiro, o forte mais devagar (apanhando e batendo), todas sem balançar | 4 molas |
| | Peso do personagem | `fighter.pesoAnimacao` no pack: o pesado achata, inclina e arrasta menos e levanta mais poeira | 1 |
| Exagero | Smear | golpes fortes: nos 2 primeiros quadros ativos, cópias esticadas por baixo do desenho (o punho/pé "borra") | 18% |
| | Inclinação de impacto | apanhando, o tronco cede para o lado do empurrão (depois do hitstop) | 2° fraco a 5,5° especial; 2° defendendo |
| | Onde pegou | pela altura do ponto de contato: na cabeça o tronco vai para trás; no corpo dobra para a frente e encolhe; nas pernas os pés vão e o tronco fica (arrasto) | 70% / 35% da altura |
| | Variação | cada reação sai ±20% diferente (igual no mesmo jogo): golpes repetidos não parecem um loop | ±20% |
| | Inclinação de corrida | dash e air dash para a frente; para trás no backdash | 3° |
| | Estalo | quando o golpe próprio fica ativo, um tranco curto de esticar na horizontal | 0,8% a 2,2% |
| Encenação | Soco de câmera | o zoom do golpe (ImpactFeel) entra seco e volta firme por mola | ~1/2 s |
| | Tranco da tela | o tremor do golpe tem direção: a tela vai no sentido do golpe e volta por mola (o tremor aleatório ficou menor) | 4 / 9 / 14 px |
| | Câmera adiantada | a câmera se adianta para o lado de quem corre e volta ao parar | 30 px |
| Pose a pose | Transição | troca de clipe fora de golpes, reações, decolagem e aterrissagem: o quadro de antes some em 2 quadros | 35% |
| | Rastro | só nos dois para a frente e dois para trás (dash, backdash e air dash) e no pulo que sai da corrida; o super pulo e os golpes não deixam (`rastroNosGolpes` liga nos golpes) | até 3 cópias azuladas, 35%, vivem 8 quadros |
| Direto | Quadros intermediários | fora do jogo: `tools/sprites/intermediarios.py` (abaixo) | — |

**Firme, não borracha.** As molas do corpo e da câmera voltam sem balançar (amortecimento
0,75–0,9) e o achatar/esticar é pequeno: o desenho é pintado, e muito achatar ou um corpo que
vai e volta passando do ponto parece borracha, em qualquer intensidade (o ANIM 50% diminui o
tamanho, não o balanço). Para um estilo mais cartum: `volume = true`, `Mola.ELASTICA` em
`molaForma`/`molaArrasto`/`molaPorForca` e os valores maiores (achatar 9%, esticar 6%).

**Só nos golpes normais.** Antecipação, continuidade, acertar trava/errar passa, estalo e smear
valem para L, M, H (em pé, agachado e no ar). Os especiais, a energia e o Super já têm o
movimento no desenho: com o motor por cima, o corpo ia e voltava a cada acerto. O tranco da
tela também é só dos normais (e do KO), um por vez.

Tudo com os pés parados (a raiz é o pivô) e o "para a frente" segue para onde o lutador olha.
Nada disso muda o frame data, as caixas ou a física: a antecipação usa só os quadros de
preparação que o golpe já tem.

## Mudanças de jogabilidade (motor de combate)

Duas regras clássicas dos jogos de luta, no `CombatConfig` (0 desliga cada uma):

- **Preparação do pulo** (`jumpSquatFrames = 3`): o pulo do chão agacha 3 quadros antes de
  sair. Nesses quadros não sai golpe, dash nem andar (o golpe apertado espera no buffer); a
  corrida segue no embalo e vira o pulo da corrida mesmo soltando a frente. A direção é a que
  ainda segura para cima no fim, ou a do comando. Apanhar ou ser agarrado cancela o pulo. **O
  pulo que cancela um golpe sai na hora**: as rotas de combo (lançador → super pulo, jM → pulo)
  não mudam.
- **Aterrissagem do pulo vazio** (`jumpLandingFrames = 3`): quem aterrissa de um pulo sem golpe
  fica 3 quadros sem golpe, pulo, dash nem andar (defender pode; o golpe espera no buffer). O
  golpe aéreo que aterrissa continua livre na hora (pulo com golpe → combo no chão).

No desenho, a preparação usa o começo do clipe de agachar com o achatar do motor; a
aterrissagem já tinha o clipe dela.

## O que não dá para resolver por código

- **Pés patinando na caminhada.** O clipe de andar do p01 veio de um vídeo com a câmera
  acompanhando o personagem: dentro do quadro o pé de apoio quase não anda para trás (≈1 px por
  quadro; às vezes até vai para a frente), enquanto o jogo anda 12,5 px por quadro do clipe.
  Então o pé desliza na velocidade da caminhada. Mexer no `distancePerFrame` não resolve (a
  arte não tem o deslocamento) e "prender" o pé faria o corpo andar aos trancos. A solução é um
  clipe novo de andar no lugar (como numa esteira: o pé de apoio indo para trás).
- **Reações desenhadas por altura** (hit alto e baixo, e um segundo hit em pé): o motor imita
  com inclinação e arrasto, mas o desenho certo é arte nova.

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
  quadro, golpe na preparação ou ativo e peso dele, dash, embalo, apanhou/defendeu e para que
  lado, peso do personagem, congelado) e o motor devolve uma `Pose` (escala X/Y, inclinação,
  arrasto, quique, smear, quadro de antes com opacidade, cópias do rastro e os eventos de
  poeira do passo).
- **Ajustes** (`Ajustes`): cada efeito liga e desliga sozinho, e `intensidade` escala todos.

## Onde fica

| Parte | Arquivo |
|---|---|
| Motor (Java puro, sem Android) | `android/motion-core/` (`Mola`, `Canal`, `Leitura`, `Pose`, `Ajustes`, `MotorAnimacao`) |
| Testes do motor | `android/motion-core/src/test/` — `gradle :motion-core:test` |
| Ponte com a luta | `app/.../AnimacaoLutador.java` (lê o `CombatFighter` e os `HitEvent` do passo) |
| Desenho | `SpriteFighterRenderer`: `draw` aplica a pose em volta dos pés (com o arrasto), o smear e a transição; `drawRastro` desenha as cópias |
| Ganchos no jogo | `GameView`: `updateFighterSprite` chama `renderer.animacao.passo(...)`; `poeiraDoMotor` solta as partículas; `drawFighter` chama `renderer.drawRastro(c)`; `startIntro` zera; botão ANIM e F4 |
| Soco de câmera | `ImpactFeel`: o zoom é um `Canal` com a mola `ZOOM_SPRING`; o tranco da tela, `kick`/`KICK_SPRING` |
| Câmera adiantada | `CameraRig.updateLead`/`lead()`, chamado no `updateFightCamera` |
| Sombra de contato | `GameView.drawContactShadow` |
| Preparação do pulo e aterrissagem | `CombatEngine.startJump`/`jumpFromSquat`, `land` e `CombatFighter.busyOnGround()`; testes em `PreparacaoDoPuloTest` |
| Peso no pack | `fighter.pesoAnimacao` (0,5 a 2) em `characters/<id>/character.json` → `Fighter.animationWeight` (gerado pelo `build_characters.py`) |
| Quadros intermediários | `tools/sprites/intermediarios.py` |
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

O roteiro (`RevisaoAnimacao`) joga 940 quadros: pulo parado, dash, L > M > H no CPU (que cai e
quica), backdash, pulo para a frente, pulo saído da corrida, super pulo, pulo por cima do CPU
(os dois viram), o 2H, andar, o cansaço com pouca vida e três especiais (S1, S2 e S4).
Com o motor ligado sai também `pose.txt`: a inclinação, o arrasto e a escala de quem joga em
cada quadro (para achar balanço sem olhar as imagens). Os dois `estado.txt` têm de ser iguais (o motor não mexe na luta).
`-Pmodos=desligado,ligado` escolhe as pastas (um nome que começa com "ligado" liga o motor).

## Quadros intermediários (fora do jogo)

```bash
pip install -r tools/sprites/requirements-animar.txt
python3 tools/sprites/intermediarios.py auditar            # maiores saltos entre quadros
python3 tools/sprites/intermediarios.py gerar JUMP_HEAVY --entre 1
# android/app/build/sprite-review/intermediarios/: o quadro do meio, a tira antes|meio|depois
# e os GIFs do clipe original e com o quadro novo (o tempo do golpe não muda)
```

A auditoria mede em pixels do mundo quanto o desenho anda de um quadro para o seguinte e a
**confiança** do fluxo (quanto do desenho vai e volta pelo fluxo óptico). Salto grande com
confiança alta é candidato a quadro do meio; com confiança baixa é giro ou pose nova, e o meio
sai com dupla exposição: ali o certo é desenhar.

**O que deu no p01:** os clipes vieram de vídeo e já são densos. O maior salto é 50 px (pulo
H, 1 > 2) e todos os acima de 40 px têm confiança abaixo de 50%: são o corpo girando ou o
chute voltando, e o meio sai com dupla exposição. Nenhum quadro do p01 foi trocado. A
ferramenta fica para personagens com poucos quadros (desenhados à mão ou do ChatGPT).

Para aplicar um candidato aprovado: o PNG vai como célula nova na folha-fonte do clipe
(`art/sprites/source/...`, `expectedFrames` + 1), a lista `frames`/`durationsMs` da animação
ganha o quadro (a duração do de antes é dividida, como no GIF) e `build_characters.py --write`
refaz a folha. No p01, as calibrações por quadro (`P01SpriteCalibration`) também andam uma
posição depois do quadro novo.

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
7. `GameView.poeiraDoMotor` (as partículas do motor) chamado depois dos dois `updateFighterSprite`;
   `drawContactShadow` antes dos corpos; o `clipStep` (idle mais lento cansado) e o agachar da
   preparação do pulo no `updateFighterSprite`; o `lead()` e o `kickX()` na câmera.
   A preparação do pulo e a aterrissagem são do motor de combate: levar junto é decisão de
   jogabilidade (com `jumpSquatFrames`/`jumpLandingFrames` em 0 o jogo fica como antes).
8. `ImpactFeel` com o `Canal` no zoom (opcional: sem ele o soco volta pelo decaimento antigo).
9. O `withAnimationWeight`/`animationWeight` no `CharacterDefinition.Fighter` e o
   `pesoAnimacao` no `build_characters.py`; aí cada pack pode ganhar o peso dele (o tanque
   1,3–1,5, o leve 0,8) e `--write` refaz o `GeneratedCharacters`.
10. `tools/sprites/intermediarios.py` e o teste dele.
11. `gradle :motion-core:test :pc:testarMotor` e `gradle :pc:revisarAnimacao` para conferir.

Com outros personagens nada muda: o motor só lê o clipe e o estado da luta, sem nada por
personagem.

## Próximos passos possíveis

- Ajustar os números no celular (ANIM 50/100/150%) e fixar o padrão.
- Dar o `pesoAnimacao` de cada personagem quando o motor for para a sprite-gpt.
