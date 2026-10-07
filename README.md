# Game Luta Sprite GPT

Branch `game-luta-sprite-gpt`. Versão `0.76-sprite-gpt-pack-v2` (fonte única: `versionName` em `android/app/build.gradle`).

## Character Pack Engine

A v0.54 incorpora o motor de pacotes da Astra v0.53 preservando a identidade GPT.
Cada personagem possui `characters/<id>/character.json`; o roster fica em
`characters/roster.json`. Animações e golpes L/M/H em pé são declarativos, e
novos IDs de animação não exigem condições novas no renderer.

O pipeline trabalha em staging, descobre todos os clips/pacotes, gera atlas,
metadados Java e relatórios, e só publica se tudo passar. Startup, janela ativa e
recovery compartilham o relógio definido no manifesto do golpe.

## Canonical Anatomy

`worldScale=1.0` não é suficiente para provar que duas artes foram desenhadas no
mesmo tamanho. A v0.54 adiciona `scaleMode: canonical-anatomy`: um frame comparável
do golpe é medido contra a pose canônica revisada do personagem usando altura e
bandas de silhueta. A mediana dos candidatos define a escala; dispersão excessiva
reprova a importação.

O Heavy de 9 frames é a regressão oficial. Ele agora usa canvas 320×256 e root
128/238. Um teste Android compara o tamanho visual do início do Heavy com o Idle e
falha se o golpe voltar a crescer.

## Build

```bash
python3 -m pip install -r tools/sprites/requirements.txt
python3 tools/sprites/build_characters.py --write
python3 -m unittest discover -s tools/sprites/tests -v
python3 tools/sprites/build_characters.py --check
cd android
gradle testDebugUnitTest assembleDebug --stacktrace
```

`--write` é o único passo que altera saídas versionadas. O Gradle só roda `--check`
(aviso se faltar Python localmente; erro na CI com `-PrequireSpriteCheck=true`).

Aplicativo **Game Luta Sprite GPT**, pacote `com.gamelutagpt`.
Preview técnico: `android/app/build/sprite-review/index.html`.

## Segundo personagem de validação — v0.55

O roster de produção usa `player_base` e `player_two`. O segundo pack reutiliza deliberadamente o mesmo perfil, os mesmos atlases e os mesmos movimentos para isolar o teste da arquitetura. O botão TROCA muda o renderer para `player_two`, e o HUD exibe o `displayName` do Character Pack ativo.

A suíte valida a troca real e executa L, M e H no segundo pack. O workflow da implementação v0.55 passou pipeline, testes Python, testes Java/Canvas, build, instalação e smoke test no emulador Android.

## Player Two com arte própria — v0.56

O segundo Character Pack deixou de reutilizar os atlas do personagem base. `player_two` agora possui perfil próprio (384×256, root 192/246, worldScale 1.0), Idle de 8 frames, Movement de 16 frames, Jab de 3, Medium Kick de 3 e Heavy Straight de 9. Idle e Movement usam masters revisados em `prepared-grid`; Jab, Medium e Heavy entram pelo `canonical-anatomy` do próprio personagem. O renderer e a lógica de troca não recebem condições específicas para o novo lutador.

Validação final da v0.56 deve rodar sobre os outputs `player_two_*` já materializados na branch, sem depender do auto-commit de regeneração do CI.

## Pack schema 2 — v0.63

Cada `character.json` agora é a fonte única do lutador (bloco `fighter`: vida, cor,
auto-combo, energia, Super e hurtbox) e declara os nove inputs com `totalMs`, janela
ativa e alcance. O NPC usa o próprio pack para dano e frame data, a câmera enquadra a
altura real da arte e um único cache de atlas é compartilhado na partida. Detalhes em
`docs/sprite-standard.md`.

## Empurrão e arquitetura — v0.65

Os corpos não se sobrepõem mais (caixa de empurrão no pack) e o `GameView` foi
dividido em sistemas menores (IA, regras de combate, comandos, câmera, controles,
HUD, cenário e efeitos), a maioria testável sem Android. Tabela em
`docs/sprite-standard.md`.

## 2H com arte própria — v0.66

O lançador agachado (2H) do `player_base` usa a nova folha de 4 frames pelo motor de
sprites, com o gancho sincronizado à janela de acerto. Comportamento de lançador mantido.

## Harmonia dos sprites — v0.67

Auditoria automática (`tools/sprites/harmony.py` + teste) de alinhamento, contato com o
chão, picos em golpes e escala. Corrigidos: tamanho e chão das reações do lutador base,
alinhamento do movimento e do Heavy do Player Two, artefato magenta, respiração do idle
e garras vazadas do Brutamonte.

## jL com arte própria — v0.68

O soco fraco aéreo do `player_base` usa a nova folha de 4 frames, com escala pela cabeça
e registro alinhado ao pulo.

## Aéreos completos — v0.69

jM e jH do `player_base` com arte própria, alinhados ao pulo e ao jL. Novo modo de
separação por componente para folhas em que as poses se sobrepõem na horizontal.

## Defesas e queda — v0.70

Defesa em pé, agachado e **no ar** (regra nova: segurar para trás pulando) e sequência de
queda do `player_base` com arte própria, todas na mesma escala do corpo.

## Dano e levantar — v0.71

Dano em pé, agachado e no ar (frame escolhido pela física do lançamento) e o levantar do
`player_base` com arte própria; o levantar começa espelhado para sair do mesmo lado em que
o lutador caiu. O atlas ampliado `player_base_missing` foi removido.

## Motor de Dano e Combos V2 — v0.72

O combate saiu do `GameView` para um motor Java puro em passo fixo de 60 fps: buffer de
input, máquina de estados (startup/active/recovery), cancelamentos declarados por golpe
(hit/block/whiff), hitstun/blockstun/hitstop reais, sessão de combo com escala de dano,
hitstun decay, juggle points e pushback. O frame data vive nos Character Packs (schema 3)
e é validado no build e no carregamento. Botão DEBUG mostra caixas, estados e a tabela de
vantagem de cada golpe. Detalhes, rotas e critérios de aceite em `docs/combat-engine.md`.

## Botões de teste de vida — v0.73

Abaixo de DEBUG: **VIDA P1** enche a vida dos dois lutadores do time (um KO volta a
lutar) e **VIDA CPU** enche a do oponente, sem precisar reiniciar o app.

## Recuo mais lento — v0.74

Andar para trás (220) é mais lento que andar para frente (300), no chão e no ar, para que
recuar não seja uma fuga igual ao avanço do adversário.

## Ultra "Página Final" — v0.75

**↓ + SUPER** com 3 barras. A regra fica no motor: ativação que congela o oponente
(como o Super), investida e confirmação. A investida pode ser defendida (blockstun e
recuperação punível) e erra se o oponente estiver longe. Quando acerta, o motor espera e
o `GameView` toca a cinemática: uma página de mangá montada em 5 painéis com a arte do
personagem. Tocar quando o anel fecha dá bônus no golpe final (PERFEITO +25%, BOM +10%).
O dano entra pelo motor com a escala da sessão de combo e, no fim, o oponente é arremessado
e cai derrubado.

- `android/ultra-core/`: motor da cinemática em Java puro (roda no Android e no PC), com
  testes em `gradle :ultra-core:test`.
- `android/pc-preview/`: visualizador no PC, `gradle :pc-preview:run`.
- `android/app/src/main/assets/ultras/<id do personagem>/`: `ultra.json` (nome, cores,
  dano, onomatopeias), as 5 imagens e os sons opcionais.
- `tools/ultra/`: prompts fixos e o script que prepara a arte gerada por IA. O guia
  completo está em [tools/ultra/README.md](tools/ultra/README.md).

## Cenário com falso 3D — v0.76

O fundo virou **camadas com profundidade** projetadas pela câmera da luta: o que está longe
anda e cresce menos, o piso é desenhado em perspectiva ("line scroll") e luzes, fogo,
água, neblina, pétalas em 3D e o reflexo dos lutadores no chão molhado são animados pelo
jogo. A luta continua 2D. Primeiro cenário: **Templo da Lua**, com arte provisória
esperando a arte do GPT.

- `android/stage-core/`: motor de cenário em Java puro, com testes (`gradle :stage-core:test`).
- `android/render-core/`: base de desenho compartilhada pelo cenário e pelo ultra (é o que
  cada plataforma implementa no port para PC).
- `android/app/src/main/assets/stages/templo_lua/`: `stage.json` e as camadas.
- Visualizador no PC com câmera de luta simulada: `gradle :pc-preview:runStage --args="app/src/main/assets/stages/templo_lua"`.
- O jogo agora desenha pela GPU no Android 8+ (`lockHardwareCanvas`), com reserva por software.
- Como criar um cenário e os prompts das camadas: [tools/stage/README.md](tools/stage/README.md).
  Como funciona: `docs/stage-engine.md`. Cinemática do ultra por dentro: `docs/ultra-pagina-final.md`.

## Raio final do ultra — v0.77

Quando a página do ultra quebra, a luta volta com o personagem disparando o **raio amarelo**
do painel final, no estilo Marvel vs Capcom: o feixe atravessa a tela, acerta 20 vezes
seguidas com o contador de hits subindo e o dano entrando a cada acerto, e termina numa
explosão que arremessa o oponente. O bônus do toque no tempo certo vale também para o raio.

- Regra no motor (`CombatEngine`, fase `ULTRA_BEAM`) e desenho em Java puro (`RaioFinal`, no
  `ultra-core`), com a arte gerada no GPT em `assets/ultras/player_base/raio_*.png`.
- Prompts e o script que prepara as imagens (fundo preto vira transparência):
  [tools/ultra/README.md](tools/ultra/README.md#raio-final). Por dentro: `docs/ultra-pagina-final.md`.
- Ver no PC: `gradle :pc-preview:runRaio`.

## Raio estilo Kamehameha — v0.78

O raio final do ultra ganhou carga e animação de verdade, como um Kamehameha de Marvel vs
Capcom: o personagem tem uma **folha de pose própria** (carga com as mãos na cintura,
disparo, sustentação com a roupa batendo no vento e recuperação), a **aura** em chamas
acende atrás dele, o **vento** gira para dentro da esfera que cresce nas mãos e a
**poeira** levanta do chão; no "HA!" vem a onda de choque, a câmera abre, o raio ondula
com anéis correndo por ele e vento nas bordas, o recuo arrasta o pé soltando poeira e
nuvens rolam embaixo do feixe. Toda a arte é do GPT.

- Pose do personagem pelo pipeline de sprites (`specialAnimations.ULTRA`), com
  `tools/sprites/separar_folha.py` para folhas desenhadas em grade apertada.
- Efeitos preparados por `tools/ultra/preparar_raio.py --aura --vento --poeira`.
- Prompts e passo a passo: [tools/ultra/README.md](tools/ultra/README.md#raio-final).

## Time no motor: Assist, Assist → Tag e barra do time — v0.79

Primeiro passo do roadmap de mecânicas: a equipe passa a participar do combate.

- **TAG** chama o parceiro como **assist**: ele entra atrás de você, faz o golpe dele (o
  Lutador base lança o projétil; o Lutador Teste 2 dá o soco forte) e sai. Recarga de 4 s.
- **TAG de novo** enquanto o assist está em campo: **Assist → Tag**. O assist fica como o
  novo personagem e o anterior sai, sem interromper o combo.
- **↓ + TAG**: a troca direta de antes.
- **Barra do time**: uma barra só para a dupla; o que o assist ganha vai para ela e ela
  acompanha quem está em campo.
- A regra do time saiu do `GameView` e foi para o motor (`TeamSystem`), em frames e
  determinística, base para rollback e para as próximas mecânicas de equipe (guard
  cancel, DHC). O golpe do assist é declarado no pack (`fighter.assist.move`).
- Detalhes: `docs/combat-engine.md` (seção Time).

## Agarrão e tech — v0.80

- **L + M** agarra: não dá para bloquear, então quem só defende tem que reagir. Na tela, o
  **ponto "L+M" entre os botões L e M** aperta os dois com um dedo só (ou use dois dedos).
- Quem é agarrado tem uma janela curta para apertar **L + M** e fazer o **tech**: ninguém
  toma dano e os dois se afastam.
- Agarrão que erra deixa o atacante aberto; durante e logo depois de hitstun/blockstun não
  dá para ser agarrado (agarrão não vira combo).
- A CPU também agarra quando está colada e faz tech em parte dos agarrões.
- Regras e tempos: `docs/combat-engine.md` (seção Agarrão e tech).

## Defesa ativa: pushblock e Guard Cancel Tag — v0.81

- **M + H bloqueando** (ou o ponto **"M+H"** entre os botões M e H): **pushblock**. Empurra
  o atacante para longe e encurta o bloqueio, por 1/4 de barra. Fora do bloqueio é o H.
- **TAG bloqueando** (no chão, assist pronto): **Guard Cancel Tag**. O parceiro entra no
  seu lugar atacando, invulnerável, e o oponente congela no clarão. Custa 1 barra; o
  botão mostra "CANCEL" quando dá.
- Regras e tempos: `docs/combat-engine.md` (seção Defesa ativa).

## Air dash — v0.82

- **→ →** no ar: **air dash** (mantém a altura); **← ←**: **back air dash**. Um por pulo.
- Dá para soltar golpe aéreo no meio do dash: o impulso continua.
- Detalhes: `docs/combat-engine.md` (seção Air dash).

## Wall bounce e ground bounce — v0.83

- **H dentro de um combo** joga o oponente na parede (borda da tela ou da arena); ele
  quica de volta e dá para continuar o combo. O H solto no neutro continua normal.
- **jH num oponente no ar** crava ele no chão e ele quica para cima.
- Um de cada por combo; depois de quicar, se ninguém continuar, ele cai derrubado.
- Rota de exemplo: L → M → H (parede) → dash → M → 2H → super pulo → jL → jM → jH (chão) → Super.
- Detalhes: `docs/combat-engine.md` (seção Wall bounce e ground bounce).

**Arte que falta** (agarrão, pushblock, air dash, batida na parede, especial e Super, o
Lutador Teste 2 e efeitos), com os prompts: [docs/sprites-pendentes.md](docs/sprites-pendentes.md).
