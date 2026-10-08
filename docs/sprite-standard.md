# Padrão de personagens e sprites — GPT v0.63

A escala pertence ao **personagem**, nunca a um golpe isolado. As imagens e os
metadados são preparados antes do APK; não há JSON, recorte automático ou
normalização de imagens durante a partida.

## Arquivos que o autor edita

| Arquivo/pasta | Responsabilidade |
| --- | --- |
| `characters/<id>/character.json` | Identidade visual, regras do lutador, animações e golpes (schema 2) |
| `characters/roster.json` | Personagem visual dos dois slots do time |
| `tools/sprites/profiles/<id>.json` | Tamanho, raiz, escala e exigências de qualidade |
| `tools/sprites/clips/*.json` | Fonte, segmentação, quantidade de frames e destinos |
| `art/sprites/source/` | Fontes de arte, preservadas sem sobrescrever |

O build descobre todos os `character.json` e todos os clips. Não existe uma lista
manual de personagens no renderer. `GeneratedCharacters.java`,
`GeneratedSpriteLayouts.java`, `SpriteStates.java`, os atlas em `drawable-nodpi` e os
relatórios em `tools/sprites/reports` são saídas geradas e versionadas. Não editar à mão.

## Fluxo de trabalho

```bash
python3 -m pip install -r tools/sprites/requirements.txt
python3 tools/sprites/build_characters.py --write
python3 -m unittest discover -s tools/sprites/tests -v
python3 tools/sprites/build_characters.py --check
cd android
gradle testDebugUnitTest assembleDebug
```

O `preBuild` do Gradle apenas **verifica** (`--check`) as saídas versionadas; ele nunca
reescreve arquivos rastreados. Sem Python 3.9+/Pillow o build local emite um aviso e
continua (as saídas já estão no repositório); a CI usa `-PrequireSpriteCheck=true` e
falha se a verificação não puder rodar. Java 17, Gradle 8.7 e Android SDK 35 continuam
pré-requisitos. O APK não precisa de Python.

O importador trabalha em uma pasta temporária. Somente depois de validar todos os
pacotes publica as saídas; um erro de configuração não deixa metade dos atlas
atualizada. `--check` compara todas as saídas declaradas, sem modificar os arquivos
versionados. A CI rejeita saídas desatualizadas antes do build e nunca faz commit.
Saídas órfãs (PNG/relatório sem clip) reprovam o `--check`; o `--write` as remove.
Arte em `art/sprites/source/` que nenhum clip ou perfil referencia reprova os dois modos.

## Novo personagem

1. Crie um perfil visual em `tools/sprites/profiles/`, usando `player_base.json`
   como referência. Defina as dimensões, a raiz e a escala próprias do lutador.
2. Adicione as fontes e os clips correspondentes, com IDs e saídas únicos.
3. Copie `characters/player_base/character.json` para
   `characters/<novo_id>/character.json`. O campo `id` deve coincidir com a pasta.
   Ajuste nome, perfil, `artFacing`, o bloco `fighter`, atlas, animações e os nove golpes.
4. Substitua um dos IDs de `characters/roster.json` para testar no time. O primeiro
   slot deve ser igual a `defaultCharacter`. O cadastro é gerado automaticamente.
5. Gere, revise a prévia, execute os testes e confira o APK no Android.

O pacote é a fonte única do lutador: visual, vida, cor do HUD, auto-combo, projétil de
energia (dano, alcance, velocidade, comando), Super, hurtbox e o frame data de todos os
golpes normais. Física geral (velocidade de andar, pulo, gravidade), regras de medidor,
estados de knockdown e a IA continuam no código; uma mecânica inédita ainda exige
programação.

### Bloco `fighter`

```json
"artFacing": "right",
"fighter": {
  "color": "#F4B73B",
  "maxLife": 10000,
  "autoCombo": ["L", "M", "H"],
  "body": {"halfWidth": 34, "standHeight": 145, "crouchHeight": 90,
           "pushHalfWidth": 30, "pushHeight": 120},
  "energy": {"damage": 850, "range": 720, "speed": 760, "command": [3, 1],
             "spawnX": 62, "spawnY": 82, "crouchSpawnY": 65},
  "super": {"damage": 3200, "range": 1450, "speed": 1180,
            "spawnX": 78, "spawnY": 86, "airSpawnY": 82},
  "assist": {"move": "S"}
}
```

`assist.move` é o golpe que o lutador faz quando o parceiro o chama com TAG: `"S"` (o
projétil de `energy`) ou um normal de chão (`L`, `M`, `H`, `2L`, `2M`, `2H`), com o frame
data e a animação do próprio golpe. Sem `assist`, vale `"S"` para quem tem projétil e
`"H"` para quem não tem.

`spawnX`/`spawnY` são o ponto de lançamento do projétil (à frente da raiz e altura acima
do chão); `crouchSpawnY` e `airSpawnY` são opcionais e valem `spawnY` quando omitidos.
As alturas precisam ficar dentro da hurtbox. O nome exibido é sempre `displayName`; o
HUD mostra o slot do time (`PLAYER 1`, `PLAYER 2`, `CPU`) + `displayName`. `hudName`
foi removido e reprova o build.

`body` é a hurtbox de gameplay em unidades do mundo, independente do PNG.
`pushHalfWidth`/`pushHeight` formam a caixa de empurrão: os corpos não se sobrepõem,
andar contra o oponente empurra os dois (metade para cada; contra a parede o outro
cede tudo) e quem passa acima de `pushHeight` atravessa (super pulo sobre o
Brutamonte). A caixa de empurrão fica dentro da hurtbox, então nunca bloqueia golpe. `energy` e
`super` são opcionais (sem eles o lutador não usa o recurso). `artFacing` diz para onde
a arte olha; o renderer espelha a partir dele, sem flips no `GameView`.

## Estados e animações

Estados semânticos obrigatórios: `IDLE`, `COMBAT`, `WALK_FORWARD`, `WALK_BACK`,
`CROUCH`, `RISE`, `JUMP`, `FALL`, `DASH`, `BACKDASH` e `LAND`.

Estados opcionais conhecidos: `DEFENSE_STAND`, `DEFENSE_CROUCH`, `DEFENSE_AIR`, `HIT_STAND`,
`HIT_CROUCH`, `HIT_AIR`, `KNOCKDOWN`, `GROUNDED` e `GETUP` (os três últimos só em
conjunto). O vocabulário vive em `build_characters.py` e é gerado em
`SpriteStates.java`. Qualquer outra animação precisa ser usada por um golpe ou por
`specialAnimations`; um nome fora do vocabulário e sem uso (ex.: `HIT_STAN`) reprova o
build em vez de cair silenciosamente no fallback.

Cada animação contém `atlas`, `frames` (índices a partir de zero), `loop` e uma das
formas de avanço:

- `durationsMs`: um tempo positivo por frame;
- `distancePerFrame`: distância percorrida para avançar, com `loop: true`.

Caminhada acompanha deslocamento físico, inclusive ao virar para o outro lado.
Colisão com a borda não deve produzir passos no lugar. Ataques usam o relógio de
combate; reiniciar o mesmo golpe reinicia o frame, sem herdar a recuperação anterior.
Os IDs de animação de ataque são livres: não é necessário alterar um enum Java.
`specialAnimations` pode ligar `S` (energia), `SUPER` e `ULTRA` a animações one-shot; o tempo
do especial é esticado sobre a animação. `ULTRA` é a folha de 9 poses do raio final do
ultra; o jogo escolhe o quadro pela fase do raio (veja `docs/ultra-pagina-final.md`).

`specialMoves` declara especiais de comando (`S2` a `S9`): os mesmos campos de um golpe de
`moves` (animação, dano, frame data, cancelamentos) mais `command` (direções relativas,
como a energia: 1 frente, 3 baixo, 5 trás) e `buttons` (opcional, padrão `["L","M","H"]`).
Ex.: `"S2": {"command": [1, 3, 2], ...}` é → ↓ ↘ + botão. São golpes de chão; o comando
mais longo vence, e dois especiais não podem dividir comando e botão. Golpes normais
cancelam neles quando o `cancelInto` os lista.

Folhas do GPT desenhadas numa grade apertada (uma mão ou tira entra na célula vizinha)
passam antes por `tools/sprites/separar_folha.py`, que devolve cada pedaço ao quadro dono e
monta uma faixa com espaço entre os quadros. Quando os pés de uma pose larga não ficam na
mesma linha, o clip pode aumentar a faixa usada para achar os pés com `footBandRatio`
(padrão do perfil: 0,025).

## Novo golpe usando um comando existente

Adicione o atlas/clip e uma animação ao manifesto. Vincule-a a L, M ou H em `moves`.
Exemplo de animação de quatro frames e vínculo L:

```json
"CUSTOM_PUNCH": {
  "atlas": "novo_personagem_soco",
  "frames": [0, 1, 2, 3],
  "durationsMs": [40, 30, 30, 60],
  "loop": false
}
```

```json
"L": {
  "animation": "CUSTOM_PUNCH",
  "damage": 300,
  "reach": 84,
  "startupFrames": 2,
  "activeFrames": 4,
  "recoveryFrames": 4,
  "hitstunFrames": 12,
  "blockstunFrames": 9,
  "hitstopFrames": 5,
  "cancelWindows": {"hit": [2, 8], "block": [2, 8], "whiff": null},
  "cancelInto": ["L", "M", "2L"],
  "pushbackOnHit": 24,
  "pushbackOnBlock": 30
}
```

O pack declara **todos** os nove inputs: `L`, `M`, `H`, `2L`, `2M`, `2H`, `jL`, `jM`,
`jH`. Cada golpe tem `animation` **ou** `pose` (nunca os dois). Sem arte própria, o
golpe declara a postura mantida: `"pose": "CROUCH"` para 2X e `"pose": "AIR"` para jX.
Não há empréstimo implícito de L/M/H.

Desde o schema 3 (v0.72) o tempo de gameplay vem do frame data a 60 fps
(`startupFrames + activeFrames + recoveryFrames`), não da arte: a animação é esticada
para caber, mantendo a proporção entre frames. Retocar durações de frame muda só o
visual. `totalMs`/`activeStartMs`/`activeEndMs` foram removidos e reprovam o build.
Dano só acontece nos frames ativos, no máximo `maxHits` vezes por execução. Os demais
campos (hitstun, cancelamentos, pushback, juggle, lançamento, hitboxes por frame) estão
descritos em `docs/combat-engine.md`.

`reach` é a distância da raiz do atacante até a ponta do golpe; a meia-largura da
hurtbox do alvo é somada na colisão, então um lutador largo é atingido mais cedo.
(Valores antigos centro-a-centro = `reach` + 34.) `hitHeight` (opcional) é a altura
do golpe acima do chão: padrão 42 para 2X e 78 para o resto. Sem `hitboxes`, o golpe usa
uma caixa da raiz até `reach`, em `hitHeight` ± 19,5, em todos os frames ativos.

## Geometria, qualidade e raízes

Os valores abaixo descrevem a **célula canônica de autoria** (`report['layout']`). O
atlas que vai no APK é **empacotado**: o pipeline recorta a borda transparente comum a
todas as células, mantendo 8 px de margem em volta de qualquer pixel visível e a raiz
dentro da célula, e grava a nova geometria em `report['packed']`. Os pixels não mudam de
posição em relação à raiz; a memória decodificada cai de ~35.5 para ~26.4 MiB. O Java
gerado e o renderer usam sempre a geometria empacotada.

`standingVisualHeight` do perfil precisa coincidir (±5%) com a altura medida do Idle.

Personagem base: célula 256×256, raiz preferida X=128/Y=238, worldScale=1.0,
referência em pé de 228 px, fundo transparente e margem mínima de 8 px.
Golpes largos recebem mais espaço transparente, sem encolher o corpo. O chute
médio usa 384×256. O forte usa 320×256 e, após a validação anatômica da v0.54,
raiz local X=128/Y=238. Essas diferenças são metadados de canvas, sem correção
de escala por golpe em runtime.

### Escala anatômica canônica

Para fontes novas que contenham ao menos uma pose comparável à guarda canônica,
use `scaleMode: canonical-anatomy` e `anatomyReferenceFrame`. O perfil aponta para
um mestre revisado do personagem e define bandas normalizadas de silhueta. O
importador mede altura e largura nessas bandas, calcula candidatos de escala e usa
a mediana. Se a dispersão ultrapassar `maxScaleSpreadRatio`, o frame escolhido não
é confiável e o import é rejeitado.

Isso impede a regressão em que `worldScale=1.0` era mantido, mas um golpe desenhado
maior aparecia maior no jogo. O Heavy de 9 frames é o teste de regressão oficial:
o quadro 0 precisa permanecer visualmente compatível com o Idle. Clips sem uma
pose comparável devem usar um mestre `prepared-grid` revisado em vez de forçar a
detecção automática.

Três modos de importação estão disponíveis:

- `horizontal-alpha-components`: separação horizontal por transparência;
- `grid-alpha-components`: projeções X/Y e ordem por linha;
- `prepared-grid`: mestre previamente normalizado, com colunas explícitas e raiz
  `authored`; valida dimensões, quantidade, pixels visíveis e margens, e copia sem
  reamostrar ou reposicionar poses aéreas.

Médio e forte são normalizados das fontes de alta resolução com `ground-feet`.
Idle, movimentos e jab preservam os mestres normalizados revisados da v0.51. Seus
originais não normalizados não foram reconstruídos. Todos os cinco agora têm
configuração, relatório e validação no build. O idle mantém 1024×512, oito células
256×256; não retorna ao atlas legado de baixa resolução.

Os relatórios registram layout, validações e hashes SHA-256 de fonte/config/perfil.
Dados duplicados de perfil foram removidos do Java manual. Atlas do time são
carregados antes da partida; a troca de personagem não decodifica os mesmos bitmaps
novamente.

## Prévia e validação

Abra `android/app/build/sprite-review/index.html` após importar. A página funciona
offline, incorpora as imagens e permite selecionar personagem/animação, reproduzir,
pausar, avançar frame, espelhar e ajustar zoom. Exibe raiz, chão e indicação da
janela ativa. Caminhada na prévia usa tempo simulado; no jogo usa distância real.

O build rejeita, entre outros: ID duplicado, saída duplicada, fonte/atlas ausente,
frame inexistente, estado obrigatório ausente, duração inválida, janela ativa fora
da animação, perfil incompatível e roster inválido. Testes incluem cadastrar um
segundo personagem e um golpe com ID novo, regeneração determinística, saídas
obsoletas e falhas sem publicação parcial.

Testes Android exercitam comandos, liberação do direcional, salto/pouso, orientação,
pausa, escala, recortes e renderização de animação personalizada. Os testes de
combate verificam startup sem dano, janela ativa com dano único e recovery sem
acerto tardio, nos dois sentidos. A CI instala o APK no emulador e testa abertura e
retorno do segundo plano.

## Limites que ainda exigem revisão

A segmentação alpha usa projeções, não reconhecimento de poses. A grade ainda usa
intervalos X globais: linhas desalinhadas, efeitos separados, sombras ou poses que
se encostam podem gerar agrupamento incorreto. Uma contagem correta não garante
anatomia ou agrupamento corretos.

`ground-feet` estima o apoio pela faixa inferior de pixels e usa o centro do
bounding box se não detectar pés. Não usar esse modo para saltos, tecido/efeito
abaixo dos pés ou deslocamento intencional da raiz. Nesses casos, usar mestre
normalizado com raiz authored e revisar a imagem. Referências de escala devem ser
poses comparáveis em pé; não normalizar um agachamento à altura de um lutador em pé.

Margens são avaliadas acima do alpha 10: pixels mais fracos exigem inspeção visual.
`maxUpscale` impede ampliação proibida, mas não detecta blur já presente. Hitboxes
não devem ser inferidas automaticamente de pixels opacos.

Abertura no emulador não comprova qualidade final ou latência no celular. Movimento,
multitoque e fluidez ainda devem ser testados no aparelho. Física a 120 Hz não
promete entrada em 8,3 ms; consumo da fila e desenho compartilham o loop de 60 FPS.


## Atualização GPT v0.54

A branch GPT incorporou o Character Pack Engine da Astra v0.53 e adicionou a
validação `canonical-anatomy`. O Heavy atual produz 320×256, root 128/238 e
`worldScale=1.0`; a escala da fonte é calculada pela assinatura anatômica, não pela
mediana dos nove bounding boxes. O relatório grava assinatura canônica, assinatura
da fonte, candidatos de escala e dispersão.

O teste Python rejeita um frame de referência incompatível. O teste Android mede
o tamanho visual do início do Heavy contra o Idle e falha se o golpe voltar a
crescer.


## Validação com segundo personagem — GPT v0.55

O roster de produção contém `player_base` e `player_two`. Neste teste, `player_two` compartilha intencionalmente perfil, atlas, estados e L/M/H com o personagem base para testar somente cadastro, geração, preload, troca em runtime e seleção declarativa dos golpes.

O HUD mostra o `displayName` do pack ativo. O teste Android executa a animação completa de troca, confirma que o renderer passou de `player_base` para `player_two` e executa L, M e H pelo manifesto do segundo personagem. O teste Python confirma o roster versionado e a equivalência de movimentos. Workflow 37293247005: PASS completo.


## Player Two com arte própria — GPT v0.56

`player_two` passou a usar um perfil independente: célula-base 384×256, root 192/246 e worldScale 1.0. O Idle (8 frames) é a referência anatômica canônica do personagem. O Movement (16 frames) foi preparado como master authored porque mistura caminhada, crouch, salto e dash; isso evita re-grounding automático de poses aéreas. Jab (3), Medium Kick (3) e Heavy Straight (9) usam as fontes geradas em alta resolução e `canonical-anatomy` do Player Two.

Este é o teste de independência do Character Pack Engine: o roster continua `player_base` + `player_two`, mas o segundo personagem agora carrega perfil e atlas próprios sem adicionar condição especial ao renderer. O teste de runtime continua fazendo a troca real e executando L/M/H após a entrada do segundo pack.


## NPC pelo mesmo Character Pack Engine — GPT v0.57

O roster agora declara `opponentCharacter`. O oponente deixa de depender do boneco vetorial provisório e recebe um `SpriteFighterRenderer` próprio, alimentado pelos mesmos `character.json`, perfil, atlas e estados semânticos usados pelos lutadores do time. O pack de validação `monster_npc`/“Brutamonte” usa geometria própria e é maior que os jogadores para validar escala independente.

A IA continua responsável por decisão e física; desde a v0.63 dano, alcance e frame data
do NPC também vêm do pack. O renderer do NPC converte deslocamento, direção, salto, crouch, dash/backdash e L/M/H em estados do pack. Não há condição de desenho específica para o monstro. Trocar o NPC visual exige somente alterar `characters/roster.json -> opponentCharacter` para outro pack válido.

(Histórico: o commit automático de saídas pela CI foi removido na v0.63.)


## Brutamonte com arte final de teste — GPT v0.58

O pack temporário 32×32 foi substituído pela arte detalhada aprovada do Brutamonte. (Correção: o estado final usa um único atlas `monster_npc_pack` de 16 células 256×256 com `worldScale` 1.4; o atlas separado de ações e as células 160×160 foram descartados e removidos na v0.63.) O renderer e a IA continuam genéricos; esta alteração troca apenas assets e metadados do pack.


## Player Base — conjunto de combate completo — GPT v0.59

O `player_base` recebeu o primeiro passe visual dos estados de combate que ainda
usavam poses genéricas: defesa em pé/agachado, hit em pé/agachado/aéreo,
knockdown, derrubado, levantar e L/M/H agachado e aéreo. Todos os 14 estados
ficam no atlas transparente `player_base_missing`, com célula 256×256 e raiz
canônica 128/238.

O runtime prioriza essas animações quando o pack as oferece e mantém fallback
para personagens que ainda não possuem os estados novos. Assim `player_two`
e outros packs continuam válidos sem duplicar imediatamente o mesmo conjunto.
A rotação vetorial antiga de knockdown também fica desativada quando
`KNOCKDOWN`, `GROUNDED` e `GETUP` estão presentes no pack.

Este atlas é um primeiro passe de teste visual. As regras de dano, launcher,
ground slam, blockstun e ataques 2L/2M/2H permanecem as já existentes; a v0.59
conecta os sprites a esses estados sem alterar a física do combate.


## Correção do mapeamento dos golpes direcionais — GPT v0.60

A v0.59 conectou prematuramente os frames 8–13 do atlas `player_base_missing`
aos ataques 2L/2M/2H e L/M/H aéreos. Visualmente esses frames não representam
corretamente cada golpe e, por isso, o mapeamento foi removido.

A v0.60 mantém somente os oito estados reativos aprovados desse atlas:
defesa em pé/agachado, hit em pé/agachado/aéreo, knockdown, derrubado e levantar.
Os ataques agachados voltam ao fallback de agachamento e os ataques aéreos ao
fallback de salto/queda, exatamente como antes da v0.59, até existirem sprites
dedicados e revisados para cada golpe.


## Primeiro golpe direcional pelo motor — 2L — GPT v0.61

O `player_base` agora possui um atlas dedicado `player_base_crouch_light` para o
2L. A fonte é um master revisado 4×256×256, transparente, com root authored
128/238 e sem redução do limiar global de qualidade: cada frame supera os 10 mil
pixels opacos exigidos pelo perfil do personagem.

O Character Pack Engine passa a aceitar opcionalmente `2L`, `2M` e `2H` em
`moves`, mantendo L/M/H obrigatórios. O runtime consulta o move declarativo para
ataques no chão; assim o 2L usa diretamente a animação e a janela ativa definidas
no `character.json`. 2M/2H continuam no fallback legado até receberem masters
próprios. Ataques aéreos continuam fora desse vínculo e não reutilizam sprites em
pé por engano.


## Segundo golpe direcional pelo motor — 2M — GPT v0.62

O 2M do `player_base` usa o atlas dedicado `player_base_crouch_medium`, com
quatro frames e célula 384×256. O root permanece 128/238 e o worldScale continua
1.0, preservando o mesmo tamanho corporal do 2L enquanto a célula mais larga dá
espaço para a perna estendida sem cortar nem encolher o personagem.

O modo `prepared-grid` passa a aceitar opcionalmente `frameWidth`,
`frameHeight`, `rootX` e `rootY` por clip, sempre respeitando os limites do
perfil. Sem esses campos, o comportamento anterior continua usando a célula e a
raiz base do personagem. O 2M mantém dano 500, alcance 150 e a regra existente de
knockdown; apenas sua animação/timing visual passa a vir do Character Pack Engine.


## Pack schema 2, fonte única e hurtbox — GPT v0.63

- **Fonte única do lutador.** `FighterProfile` saiu do `GameView`. Vida, cor, nome do
  HUD, auto-combo, energia, Super e hurtbox vêm do bloco `fighter`. O oponente usa as
  regras do próprio pack (antes herdava as do Player 2).
- **Frame data independente da arte.** Todo golpe tem `totalMs`; a animação é esticada
  para caber. Os nove inputs são obrigatórios; inputs sem arte declaram `pose`. Os
  valores que antes estavam fixos no código foram migrados sem mudança de gameplay
  para o jogador (janela legada = 36%–64% do `totalMs`).
- **NPC pelo pack.** Dano, janela ativa, alcance e duração dos golpes do Brutamonte
  agora valem. 2L/2M/2H têm animações próprias (`LOW_CLAW`, `LOW_LUNGE`,
  `RISING_SMASH`) em vez de reaproveitar L/M/H em pé, e o Super/energia tocam `ROAR`.
  Reações (hit/knockdown) usam os estados opcionais quando o pack os tiver.
- **Hurtbox e câmera.** Colisão usa `fighter.body` (Brutamonte: 78 × 290/240). A câmera
  enquadra a altura opaca real medida nos relatórios (`visualStandHeight` /
  `visualCrouchHeight`), não a célula do PNG.
- **Memória.** Um único `SpriteAtlasCache` por partida; os atlas do time e do NPC são
  decodificados uma vez (antes os da equipe eram decodificados duas vezes).
- **Espelhamento.** `artFacing` + renderer; knockdown vetorial cai para trás para
  ambos os lados.
- **Pipeline.** Gradle só verifica; CI sem commit automático e com `contents: read`;
  órfãos e fontes não referenciadas reprovam o build. Versão única em
  `versionName`, exibida no HUD via `BuildConfig`.

Limites conhecidos: os frames 9–11 do atlas do Brutamonte encostam na borda da célula
(o perfil usa `minMargin: 0`), então garras/tecido aparecem cortados; corrigir exige
reexportar a arte com células maiores. O NPC ainda reaproveita frames entre estados
(ex.: o frame 8 serve de agachar, pouso e início de ataque).

### Complementos — v0.64

- A IA escolhe golpes só entre os que alcançam o jogador (`reach` do pack + meia-largura
  do alvo), mantendo os pesos de sorteio; fora do alcance ela se aproxima em vez de
  golpear o ar.
- Projéteis saem do ponto declarado em `energy`/`super` (o Brutamonte lança mais alto).
- Atlas empacotados (ver "Geometria") e `standingVisualHeight` validado contra o Idle.
- Nomes unificados: `displayName` + slot do time; `hudName` removido.

## Empurrão entre corpos e arquitetura — GPT v0.65

- **Caixa de empurrão** por personagem (`body.pushHalfWidth`, `body.pushHeight`),
  resolvida a cada passo de física por `CombatRules.resolvePush`.
- **`GameView` dividido** (3.705 → ~2.600 linhas). Ele mantém a simulação (estado
  dos lutadores, golpes, defesa, Super, troca, projéteis) e o loop; o resto saiu:

| Classe | Papel | Testável na JVM |
| --- | --- | --- |
| `OpponentAi` | Decisões da CPU; executa por `Actions`, lê um `Situation` | sim |
| `CombatRules` | Colisão de golpes/projéteis, empurrão, alcance, medidor | sim |
| `CommandBuffer` | Reconhecimento de comandos (↓→ etc.) | sim |
| `CameraRig` | Enquadramento, zoom e Super Jump | sim |
| `ControlsLayout` | Geometria dos controles e hit-test do toque | sim |
| `Arena` | Dimensões do mundo e da tela virtual | sim |
| `HudRenderer` | HUD, painel sobre o oponente, D-pad e botões (lê `State`) | Robolectric |
| `StageRenderer` / `EffectsRenderer` | Cenário; projéteis e overlays do Super | Robolectric |
| `SpriteFighterRenderer` / `SpriteAtlasCache` | Sprites e atlas compartilhados | Robolectric |

O painel sobre o oponente passou a mostrar o `displayName` do pack (antes dizia
"NPC TESTE"/"PLAYER 2").

## Terceiro golpe direcional pelo motor — 2H (lançador) — GPT v0.66

O `player_base` ganhou o atlas `player_base_crouch_heavy`: 4 frames (agachado em guarda,
carga, gancho para cima, guarda de recuperação), importados da folha original por
`horizontal-alpha-components` + `ground-feet` + `canonical-anatomy`.

- **Referência de escala por clip.** O clip pode declarar `anatomyReference` próprio
  (fonte, frame, colunas, `frameWidth`/`frameHeight`, `bands`), herdando o resto do
  perfil. O 2H compara seu frame 0 (agachado) com o frame 0 do 2M aprovado, usando altura
  e largura da cabeça; a faixa do peito fica de fora porque os punhos cobrem o rosto.
  Escala 0,507 (candidatos 0,526/0,488; dispersão 7,6%), coerente com idle × guarda final
  (0,511). Comparar com o Idle em pé foi rejeitado pelo próprio validador.
- **Frame data inalterado.** `totalMs` 400, ativo 144–256 ms, dano 800, alcance 148 e a
  regra de lançador (sobe o oponente e abre o super pulo de perseguição) continuam.
  Durações 70/70/130/130 ms: o frame do gancho fica na tela durante toda a janela ativa
  (testado).
- O uppercut precisa de célula canônica 256×320 (raiz 128/273); o atlas empacotado fica
  219×282.

Observações: `hitHeight` continua o padrão de golpe agachado (42) e a hurtbox durante o
golpe é a de agachado, como antes; se o gancho deve acertar mais alto (anti-aéreo) ou o
corpo deve ficar vulnerável em pé no frame 3, isso é um ajuste de balanceamento no pack.

## Auditoria de harmonia — GPT v0.67

`tools/sprites/harmony.py` mede todos os frames de todos os packs (rode o script para o
relatório) e `tests/test_harmony.py` reprova o build se voltar a acontecer:

- **Registro:** em poses que não são golpe, o tronco fica a até 20 px do tronco do Idle
  (sem "teleporte" lateral ao trocar de estado).
- **Chão:** poses no chão encostam no chão (±4 px).
- **Picos em golpes:** um frame que sai e volta mais de 40 px enquanto os vizinhos
  concordam é tranco.
- **Escala:** a defesa em pé tem ao menos 85% da altura do Idle.

Na v0.66 a auditoria encontrava 15 problemas; todos foram corrigidos por
transformações declarativas no clip (a arte-fonte não é editada):

| Pack | Problema | Correção (clip) |
| --- | --- | --- |
| `player_base` reações | desenhadas a ~70% do corpo; defesa agachada, caído e levantar flutuando 6–22 px | `transform.scale` 1,425 (anatomia vs Idle), `groundFrames`, célula 384×256, frames 8–13 sem uso removidos |
| `player_two` movimento | tronco a 42–106 px da raiz em passada, agachar, pulo, queda, dash e backdash | `transform.offsets` por frame |
| `player_two` agachar 9 | brilho magenta (artefato) no punho | `transform.cleanup` (repinta com os vizinhos) |
| `player_two` Heavy | frame 7 saltava +67 px e voltava | `frameShift` |
| `player_two` Idle | respiração fora de ordem (227→216→226…) | frames em vai-e-volta |
| Brutamonte | garras do frame 10 vazavam para as células 9 e 11 (pedaços soltos; garra cortada) e ciscos | `transform.regroupComponents` (cada pedaço volta ao dono; ciscos < 16 px removidos); margem mínima volta a 8 px |
| Brutamonte pulo/queda | passos de caminhada no ar e bote do golpe como queda | JUMP 8→13, FALL 12; Idle em vai-e-volta |

Operações do `transform` (clips `prepared-grid`): `keepFrames`, `scale` (ampliar exige
`allowUpscale` + `reason`), `outputFrameWidth/Height/RootX/RootY`, `columns`,
`offsets`, `groundFrames`, `cleanup`, `regroupComponents`. Clips importados por
componentes aceitam `frameShift`. Tudo fica registrado no relatório do atlas.

Limite: as reações do `player_base` foram ampliadas a partir de arte pequena e ficam
mais suaves que o resto; o ideal é redesenhá-las na resolução do Idle.

## Primeiro golpe aéreo pelo motor — jL — GPT v0.68

O `player_base` ganhou o atlas `player_base_jump_light` (4 frames: guarda no ar, jab
esticado ×2, recolhe). Importado por componentes com `canonical-anatomy` contra o Idle
usando só a faixa da cabeça (`anatomyReference.bands: [[0, 0.12]]`): a altura de um corpo
no ar não é comparável à de um corpo em pé. Escala 0,416 (candidatos 0,409/0,423).

Registro aéreo: `ground-feet` prende a raiz no pé mais baixo, o que é errado no ar. O
`frameShift` de cada frame alinha o tronco (−13 px) e a altura dos pés (12 px acima da
raiz) com os frames JUMP/FALL aprovados, então trocar de pulo para golpe não dá salto.

Frame data do jL inalterado (160 ms, ativo 58–102 ms, dano 300); durações 50/30/30/50
mantêm os dois frames de soco esticado durante toda a janela ativa (testado). jM e jH
continuam com `pose: "AIR"`.

## Aéreos completos — jM e jH — GPT v0.69

O `player_base` agora tem arte própria para os três golpes aéreos:

| Input | Atlas | Frames | Escala | Frame data (inalterado) | Durações |
| --- | --- | --- | --- | --- | --- |
| jM | `player_base_jump_medium` | guarda, carga, soco esticado, recolhe | 0,399 | 260 ms, ativo 94–166 | 50/40/90/80 |
| jH | `player_base_jump_heavy` | guarda, braços erguidos, martelada, recolhe | 0,402 | 400 ms, ativo 144–256 | 70/70/130/130 |

Em ambos o frame do golpe fica na tela durante toda a janela ativa (testado). Mesmo
registro aéreo do jL (tronco −13 px, pés 12 px acima da raiz) e escala pela cabeça
contra o Idle. O jH continua sendo a martelada que, no super pulo, derruba o oponente.

**Novo modo de separação `alpha-components`:** na folha do jM o punho esticado passa por
cima da pose seguinte (sem encostar), então a projeção por colunas juntava os dois
frames. O modo rotula corpos conectados (8-vizinhança), ordena da esquerda para a
direita e recorta cada frame só com os próprios pixels; pedaços pequenos juntam-se ao
corpo que os contém e ciscos são descartados.

## Defesa em pé, agachado e no ar + queda — GPT v0.70

Quatro folhas novas do `player_base`, todas na **mesma escala** (0,368, medida pela
guarda em pé contra o Idle; agachado e ar usam `scaleMode: "fixed"` com justificativa
para o conjunto não variar de tamanho):

| Estado | Atlas | Frames usados |
| --- | --- | --- |
| `DEFENSE_STAND` | `player_base_defense_stand` | guarda erguida → impacto → recupera → guarda |
| `DEFENSE_CROUCH` | `player_base_defense_crouch` | idem, agachado |
| `DEFENSE_AIR` (novo) | `player_base_defense_air` | idem, no ar (registro aéreo dos golpes jX) |
| `KNOCKDOWN` | `player_base_fall` | lançado para trás → bate no chão |
| `GROUNDED` | `player_base_fall` | deitado → assentado |
| `HIT_AIR` | `player_base_fall` | lançado para trás |

**Clips de guarda:** o primeiro frame da animação é a guarda sustentada (enquanto o golpe
se aproxima); os demais tocam esticados sobre o blockstun (`Animation.guardTime`).

**Defesa no ar (regra nova):** no ar, segurar trás, trás-baixo ou trás-cima ativa
`GUARD_AIR`. Bloqueia golpes, projéteis e Super (golpes baixos não alcançam um corpo no
ar). Durante o bloqueio no ar o empurrão e a gravidade continuam; o HUD mostra
"PRONTO/DEFENDENDO/BLOQUEIO NO AR".

**Importador:** `scaleMode: "fixed"` (exige `scaleReason`), raiz `bbox-bottom-center`
para corpos deitados e `minOpaquePixels` por clip. O atlas antigo `player_base_missing`
agora só serve `HIT_STAND`, `HIT_CROUCH` e `GETUP` (frames 2, 3 e 7 da fonte), ainda
ampliados de arte pequena — são os próximos a redesenhar.

## Dano em pé, agachado e no ar + levantar — GPT v0.71

Quatro folhas novas do `player_base`, na mesma escala entre si (0,353: a guarda de
`player_base_hit_stand` medida contra o Idle por `canonical-anatomy`; as outras três
usam `scaleMode: "fixed"` com justificativa):

| Estado | Atlas | Frames usados |
| --- | --- | --- |
| `HIT_STAND` | `player_base_hit_stand` | impacto → recuo máximo → recupera (frames 1–3) |
| `HIT_CROUCH` | `player_base_hit_crouch` | idem, agachado (frames 1–3) |
| `HIT_AIR` | `player_base_hit_air` | lançado subindo → topo → caindo (frames 0–3) |
| `GETUP` | `player_base_getup` | deitado → apoia → ajoelha → de pé (frames 0–3) |

**Levantar do lado certo:** na folha original o corpo deitado tem a cabeça para a frente,
mas a queda (`player_base_fall`) termina com a cabeça para trás. Os frames 0 e 1 do
levantar são espelhados (`mirrorFrames: [0, 1]`), então o lutador começa a levantar na
mesma posição em que caiu e só vira de frente ao ajoelhar (frame 2). Um teste compara o
lado da cabeça do último frame da queda com o primeiro do levantar.

**`HIT_AIR` pela física:** o frame do lançamento sai da velocidade vertical, não do
relógio (`GameView.launchPoseTime`): subindo rápido → frame 0, subindo → 1, caindo → 3;
`groundSlam` usa o frame do topo.

**Harmonia:** reações (`HIT_STAND`, `HIT_CROUCH`) têm tolerância de torso de ±28 px
(o corpo dobrado é a pose; deslocar o frame faria o pé deslizar). Demais estados seguem
±20.

**Importador:** `mirrorFrames` (lista de índices espelhados na horizontal antes de medir
raiz e escala). O atlas ampliado `player_base_missing` foi removido: nenhum estado o usa.
