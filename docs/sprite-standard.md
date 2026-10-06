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
  "hudName": "PLAYER 1",
  "color": "#F4B73B",
  "maxLife": 10000,
  "autoCombo": ["L", "M", "H"],
  "body": {"halfWidth": 34, "standHeight": 145, "crouchHeight": 90},
  "energy": {"damage": 850, "range": 720, "speed": 760, "command": [3, 1]},
  "super": {"damage": 3200, "range": 1450, "speed": 1180}
}
```

`body` é a hurtbox de gameplay em unidades do mundo, independente do PNG. `energy` e
`super` são opcionais (sem eles o lutador não usa o recurso). `artFacing` diz para onde
a arte olha; o renderer espelha a partir dele, sem flips no `GameView`.

## Estados e animações

Estados semânticos obrigatórios: `IDLE`, `COMBAT`, `WALK_FORWARD`, `WALK_BACK`,
`CROUCH`, `RISE`, `JUMP`, `FALL`, `DASH`, `BACKDASH` e `LAND`.

Estados opcionais conhecidos: `DEFENSE_STAND`, `DEFENSE_CROUCH`, `HIT_STAND`,
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
`specialAnimations` pode ligar `S` (energia) e `SUPER` a animações one-shot; o tempo do
especial é esticado sobre a animação.

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
  "totalMs": 160,
  "activeStartMs": 40,
  "activeEndMs": 100,
  "reach": 84
}
```

O pack declara **todos** os nove inputs: `L`, `M`, `H`, `2L`, `2M`, `2H`, `jL`, `jM`,
`jH`. Cada golpe tem `animation` **ou** `pose` (nunca os dois). Sem arte própria, o
golpe declara a postura mantida: `"pose": "CROUCH"` para 2X e `"pose": "AIR"` para jX.
Não há empréstimo implícito de L/M/H.

O tempo de gameplay vem de `totalMs`, não da arte: a animação é esticada para caber
nele, mantendo a proporção entre frames. Retocar durações de frame muda só o visual.
Dano só é permitido em `activeStartMs <= tempo < activeEndMs`, uma vez por execução.
Startup e recovery não acertam.

`reach` é a distância da raiz do atacante até a ponta do golpe; a meia-largura da
hurtbox do alvo é somada na colisão, então um lutador largo é atingido mais cedo.
(Valores antigos centro-a-centro = `reach` + 34.) `hitHeight` (opcional) é a altura
do golpe acima do chão: padrão 42 para 2X e 78 para o resto.

Ainda não há comandos extras declarativos, cancel windows ou hitboxes por frame.

## Geometria, qualidade e raízes

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
