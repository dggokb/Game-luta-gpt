# Animação por vídeo (Seedance) — padrão

> **Omni Flash 1.1 (Google Flow):** a fórmula de prompt, os pares de quadros de início e fim e
> o jab com quadro final estão em [omni-flash.md](omni-flash.md). As regras abaixo continuam valendo.

Decidido no p01: cada animação é um **vídeo curto** gerado no Seedance a partir de uma
imagem inicial do personagem. O vídeo vira os quadros do jogo com
`tools/sprites/video_para_sprite.py` (recorta o verde, tira sombra e poeira, corrige o
deslizamento, escolhe o loop e coloca tudo na mesma escala). Os prompts de cada
personagem estão na ficha individual correspondente (`p01.md` a `p16.md`).
As fichas `p13.md`–`p16.md` são **propostas**: em 12/10/2026 as pastas
`game-luta-gpt/animations/p13` a `p16` do Drive continham apenas um PNG de cada
personagem, sem vídeo. Não marcar nenhum clipe ✅ antes de produzi-lo, convertê-lo e
validá-lo no jogo.

## Antes do primeiro vídeo de um personagem

1. **Base** (`art/keys/<id>/idle_base.png`): a pose de guarda isolada do concept, em alta
   resolução e fundo transparente. Prompt para o ChatGPT, com o concept anexado:
   ```
   Recorte e isole a pose de luta 3/4 deste model sheet (a figura com os punhos em
   guarda). Não redesenhe e não mude nada: mesmo traço, mesma pintura, mesmas cores,
   mesmas proporções e mesmo nível de detalhe. Corpo inteiro, dos pés à ponta do cabelo.
   Imagem vertical 1024×1536, fundo transparente, personagem centralizado ocupando quase
   toda a altura. Sem chão, sem sombra, sem texto.
   ```
2. **Imagens iniciais** (`art/keys/<id>/inicio_*.png`, geradas a partir da Base): a Base
   sobre verde puro em 1280×720, sempre do mesmo tamanho, com espaço em cima para pulos e
   chutes altos.
   - `inicio_centro.png`: quase tudo (o personagem fica no lugar).
   - `inicio_esquerda.png`: golpes e movimentos que avançam para a direita.
   - `inicio_direita.png`: movimentos que recuam para a esquerda.

## Regras de todo vídeo

- **Formato 16:9**, imagem inicial indicada na tabela do golpe, sem cortar nem dar zoom.
  O vídeo tem de **começar (ou terminar) exatamente na imagem inicial**: é por ela que a
  ferramenta acha o tamanho (ver "Tamanho" abaixo).
- **Um vídeo por animação.** Começa e termina na guarda da imagem inicial.
- **O golpe acontece UMA vez e rápido** (velocidade de jogo de luta); depois ele fica em
  guarda respirando. A ferramenta corta o trecho certo.
- **Sem efeitos:** fogo, energia, poeira, rastro, faíscas e sombra são do jogo. Se o vídeo
  inventar efeito, o golpe fica sujo.
- **Sem adversário:** golpes acertam o ar; reações a golpes vêm de "alguém invisível".
- Gerou estranho (virou de frente, a câmera andou, o cabelo mudou, saiu do quadro)?
  **Gere de novo**, não dá para consertar depois.
- **Câmera parada de verdade:** escrever também "The camera never zooms in or out; the
  character's size stays the same as in the first frame for the whole video". Intro,
  vitória e ultra são os que mais dão zoom (aí o tamanho muda no meio e não há conserto).
- **Golpe fraco é curto e compacto:** "short and compact: only the arm (or leg) moves, the
  body and feet stay in place". Arte com muito movimento num golpe rápido fica acelerada no
  jogo (ver "Ritmo").
- **Andar e correr em loop:** "keeps walking (running) for three seconds as a continuous
  cycle" — o jogo precisa de um ciclo de passada; uma arrancada só não vira loop.
- **Pose do fim:** golpe agachado "ends crouched and holds the crouch for one second";
  intro e vitória podem usar **último quadro = imagem inicial** quando não começam nela.
- **Não precisa de vídeo** para: virar de lado (o jogo espelha o sprite) e pulo para
  frente/trás (o jogo usa o mesmo pulo e move o personagem).

## Tamanho (padrão de inserção)

As imagens iniciais de todos são geradas **na mesma escala do p01**, então a altura de cada
um no jogo sai delas: a guarda do p01 tem **224 px** na célula e os outros ficam
proporcionais (p02 203 px, p04 210 px). O p03 fica igual ao p01 (irmãos, decisão de
história). O jogo ainda aplica o `worldScale` do perfil. Ninguém escolhe escala à mão:

1. **Uma vez por personagem**, depois de criar as imagens iniciais:
   `python3 tools/sprites/tamanho.py calibrar <id> <vídeo_de_agachar.mp4> --escala-de p01`
   grava `art/keys/<id>/tamanho.json` com a escala de cada imagem inicial e a altura (o
   agachado é achado dentro do vídeo de onde foi tirado, mesmo reduzido). O perfil do
   personagem (`tools/sprites/profiles/<id>.json`) leva a mesma proporção.
2. **Personagem inteiro ou vídeo refeito** (o jeito normal):
   `python3 tools/sprites/personagem.py <id> <pasta com os vídeos> [ESTADO ...]`
   Sem estados, monta tudo; com estados (ex.: `CROUCH_LIGHT HIT_AIR`), refaz só esses. Ele
   acha em cada vídeo o trecho da ação e o quadro do impacto, escolhe os quadros na contagem
   do p01 (o tempo e o impacto do p01 servem direto), pula quadros com clarão, arco ou
   poeira, converte e atualiza `characters/<id>/character.json`. Personagem novo nasce com
   os dados do p01 na escala da altura dele; num personagem que já existe só muda a
   animação refeita, e dano, frame data, poderes e tempos ajustados à mão continuam (o tempo
   só volta ao do p01 se a contagem de quadros mudar). O nome do vídeo de cada estado fica
   em `tools/sprites/videos/<id>.json`. Depois: `build_characters.py --write`.
3. **Um clipe à mão** (casos especiais, como o S4 do p04):
   `python3 tools/sprites/video_para_sprite.py video.mp4 --personagem <id> --clipe tools/sprites/clips/<clipe>.json --inicio A --fim B`
   A ferramenta acha a imagem inicial no 1º ou no último quadro (mesmo se o Seedance deu
   zoom), calcula a escala e grava a receita no clipe (`"video"`: arquivo, escala, como foi
   medida, quadros e opções). Para refazer: `video_para_sprite.py video.mp4 --clipe <clipe>`.
4. Vídeo que não começa nem termina numa imagem inicial (intro, vitória...) é medido pela
   guarda do IDLE no meio do vídeo, e só vale se 3 quadros concordarem. Se nada bater, a
   ferramenta para com erro. A saída é gerar de novo a partir da imagem inicial; em último
   caso `--escala N --motivo "..."`, que fica gravado.
5. Se a câmera mudar o zoom no meio do vídeo, a ferramenta recusa: gere de novo.

Os testes conferem que toda receita diz como a escala foi achada.

**Célula:** o `personagem.py` mede o alcance dos quadros e escolhe a célula justa (até
768×512). Cada quadro fica na própria célula. O que passa da célula máxima (o chicote do
p08 esticado no ultra) sai cortado limpo na borda (`aparar_borda` na receita) em vez de
quebrar o build.

## Tempo do golpe

Cada animação de golpe marca o quadro do impacto (`"impactFrame"` no `character.json`).
A animação estica sobre o frame data, e o build recusa o pacote se esse quadro não cair na
janela ativa do golpe; aí é ajustar os `durationsMs` (mais tempo antes ou depois do impacto).

## Auditoria (antes de revisar à mão)

```
python3 tools/sprites/auditoria.py p03 --folhas android/app/build/auditoria
```

Aponta, clipe por clipe, o que está fora do padrão, e grava uma folha de conferência (chão
em verde, raiz em amarelo, quadros marcados em vermelho) só dos clipes com aviso:

| Tipo | O que mede |
|---|---|
| tamanho | pontas do clipe em pose de guarda/agachado com altura diferente da referência; cabeça maior ou menor que a do idle (estimativa: confirmar na folha) |
| pulo | golpe que começa ou termina em guarda com o pé de trás fora do lugar do idle |
| deslize | pé de trás andando aos poucos durante o golpe (giro e rasteira não contam) |
| pose | golpe/reação agachado que fica em pé |
| efeito | cor fora da paleta do personagem num quadro (clarão, poeira, arco, rastro) |
| quebrado | quadro de outro personagem, vazio, cortado na borda ou flutuando |
| receita | clipe sem receita, com escala manual ou usando a arte de outro personagem |
| ritmo | o mesmo estado anima mais rápido/lento que o do p01 no mesmo tempo de jogo; intro, vitória, derrota, provocação e idle fora do tempo do vídeo |
| ciclo | loop que não fecha; corrida com quadro parado em guarda |

A harmonia (`harmony.py`, também nos testes) mede o tronco de cada quadro; no pico de golpe
ela ignora linhas finas (chicote, fita), para um chicote cruzando o corpo não parecer o
corpo pulando. Personagem com roupa ou arma que engana o alinhamento (vestido, capa) é
convertido com `--fixar registro` nos clipes registrados (agachar, pulo, defesa no ar).

É triagem: o "erro" quase sempre é real, o "aviso" precisa de olho na folha. Leva uns 30 s
por personagem. A lista de revisão de cada personagem sai dela (`docs/personagens/revisao.md`).

Correções sem vídeo novo (editam a folha normalizada e gravam na receita, para a
reconversão repetir; depois `build_characters.py --write`):

| Problema | Ferramenta |
|---|---|
| deslize, pulo ao trocar de animação | `ancorar.py p04 HEAVY_STRAIGHT` fixa o pé de apoio no lugar do idle |
| tamanho errado, zoom de câmera no meio | `reescalar.py p02 MEDIUM_KICK --ver` mede pela cabeça quadro a quadro; sem `--ver` aplica (`--fatores` à mão quando a medida erra) |
| poeira, risco, arco, clarão | `limpar.py p02 LIGHT_JAB:3,4,5 --previa pasta` apaga manchas de cor fora da paleta; `CROUCH_HEAVY:4@0-0.2` limpa só nessa faixa da altura do corpo (0 = topo), quando a limpeza inteira comeria parte borrada do corpo |

Não resolvem: efeito por cima do corpo (o arco cobre a perna), pose errada (agachado que
levanta), tamanho em pose ereta (intro/vitória: a cabeça engana). Aí é gerar o vídeo de novo.

## Régua de tamanho (uma animação maior ou menor que outra)

```
python3 tools/sprites/regua.py p02 --relatorio
```

Grava `docs/personagens/regua/<id>.md` (tabela), `.json` e `-trocas.png` (cada troca de
animação sobreposta ao IDLE como o jogo faz). Leva uns 3 min por personagem, sem APK.

**A referência é a imagem inicial** (`art/keys/<id>/inicio_*.png`) no tamanho oficial do
`tamanho.json`: a mesma arte de onde saem todos os vídeos. O **fator** de um clipe é o
tamanho do corpo no jogo dividido pelo dessa referência (100% = certo; aviso fora de ±3%,
erro fora de ±6%). A altura da silhueta não serve de régua: guarda aberta, corrida e
agachado mudam a altura sem mudar o corpo. O fator vem, nesta ordem:

1. a imagem inicial inteira achada num quadro do clipe;
2. a escala que a conversão mediu na câmera do vídeo pela imagem inicial (nota ≥ 0,9), vezes
   o que mudou o tamanho depois (fator óptico, folha do vídeo × folha do jogo);
3. a cabeça da imagem inicial achada nos quadros (normalizada pela do IDLE). A cabeça
   crescendo ou diminuindo ao longo do clipe é zoom de câmera no vídeo.

A troca de animação (ponta do clipe sobre o IDLE q0, raiz no mesmo ponto) separa dois
problemas que parecem iguais no jogo: **tamanho** (mesma pose, corpo maior ou menor:
`reescalar.py`) e **pose** (a ponta do clipe é outra guarda, por exemplo aberta e baixa: a
altura salta na troca mesmo com o corpo certo; escolher outros quadros ou gerar o vídeo
terminando na imagem inicial). Também mostra o corpo fora do lugar (`ancorar.py`).

**Corrigir o tamanho** (sem vídeo novo), depois de ver o relatório:

```
python3 tools/sprites/regua.py p02 --corrigir DASH VICTORY INTRO   # leva a 100%
python3 tools/sprites/build_characters.py --write
python3 tools/sprites/regua.py p02 --relatorio                    # confere
```

Reescala a folha do jogo em volta da raiz (aumenta ou diminui) e grava na receita
(`reescala`, `reguaFatores` e o motivo em `regua`). Fator óptico aplicado depois da medida
da câmera é desfeito. Zoom de câmera vira um fator por quadro (a reta da cabeça ao longo do
clipe). Só corrigir clipes cujo fator foi confirmado por duas medidas; a pose não se corrige
assim.

**Ajustar no olho, no celular** (DEBUG > **AJUSTAR TAMANHOS**):

1. Escolha o personagem (◀ ▶) e a animação na lista. Ela toca em loop com o IDLE em
   transparência atrás, raiz no mesmo ponto, como na troca de animação no jogo
   (PAUSA, VEL ½x/¼x e IDLE ATRÁS ajudam a comparar). O **P01 fica ao lado como referência**,
   no mesmo chão e na mesma escala do jogo, fazendo a mesma animação (ou parado no IDLE; o
   botão P01 troca).
2. −5 / −1 / +1 / +5 mudam o tamanho da folha (1% por toque); 100% desfaz. Cada toque já
   vale no jogo e fica salvo no aparelho. Animações que usam a mesma folha (IDLE e COMBAT,
   agarrão) mudam juntas.
3. **SALVAR NO DRIVE** abre o Compartilhar do Android: escolha Drive. O arquivo
   `ajustes-tamanho-<data>.json` leva os ajustes de todos os personagens.
4. Para gravar no jogo: `python3 tools/sprites/regua.py --aplicar ajustes-tamanho-<data>.json`,
   depois `build_characters.py --write`. A célula cresce sozinha quando o quadro aumentado
   não cabe (margem medida como o import mede). No P01 o fator vai para a receita
   (`ajusteTamanho`) e a calibração gerada multiplica por ele, porque ela normaliza a folha
   pela altura do idle. Se o IDLE mudou mais de 5%, atualize `standingVisualHeight` no
   perfil (o build avisa). Hurtbox, alcance e saída das magias não mudam sozinhos. No APK seguinte o ajuste do aparelho volta a 100% sozinho (a folha mudou de
   tamanho), porque o tamanho já está no desenho.

## Ritmo: animação e movimento do jogo

O motor dá o mesmo tempo a todos (frame data, velocidade de andar e dash, hitstun). A arte
de cada um tem mais ou menos movimento; sem acerto, um dash com corrida de verdade fica
"acelerado" e um andar de passo largo patina. A regra:

| Tipo de animação | Quem manda no tempo | Acerto |
|---|---|---|
| andar, dash, pulo, queda, aterrissagem, agachar, levantar | a arte | `ritmo.py <id>`: mesmo ritmo visual do p01 (andar: distância por quadro; resto: tempo por quadro) |
| idle, intro, vitória, derrota, provocação | o vídeo | `ritmo.py <id>`: tempo do próprio vídeo |
| golpes, especiais, super | o frame data do personagem | `ritmo.py <id>`: frame data esticado pelo ritmo da arte contra o p01 (até 2x mais lento, 1,5x mais rápido); hitstun/blockstun acompanham (vantagem igual), janelas de cancelamento e caixas também, e o quadro de impacto é reposicionado na janela ativa |
| levantar do chão | `fighter.wakeupFrames` do personagem | `ritmo.py <id>`: clipe GETUP no ritmo do p01 e o motor espera o clipe |
| reações ao golpe, defesa, backdash | o motor (hitstun de quem bate, blockstun) | só a auditoria aponta |

O loop (andar, corrida) tem de ser um ciclo do vídeo: começa e termina no mesmo ponto da
passada, sem largada nem freada. Dash de vídeo que é só arrancada vira um loop curto do
trecho do avanço.

## HD direto do vídeo (densidade 2) — piloto p05

Nos vídeos o personagem tem ~420 px de altura (mais nos que deram zoom); a folha 1x usa
~200. Com `--densidade 2` o `video_para_sprite.py` escala para o dobro e grava a folha com o
dobro de px: célula, raiz, `largura`, `raiz` e `aparar_borda` da receita continuam
**lógicos**, o clipe ganha `"pixelScale": 2.0` (como o p01–p04 por super-resolução) e o jogo
desenha no mesmo tamanho. A folha HD fica no máximo com 4096 px de lado (mais linhas, menos
colunas). `--nitidez` aplica unsharp mask leve e tira a franja verde da borda.

Auditoria, ancorar, limpar, reescalar, tamanho e harmonia medem a folha HD reduzida ao
tamanho lógico (os limites em px valem igual) e editam na folha física.

Personagem que já estava no jogo em 1x tem retoques fora da receita. Para não perdê-los:

1. a folha 1x revisada fica como está (`*_video_normalized.png`) e entra em
   `sourceReferences`; a HD vai para `*_video_hd.png` (`source`);
2. a receita guarda `"referencia"` (folha, célula, raiz e colunas da 1x) e
   `tools/sprites/retoque_hd.py` repete na HD, célula a célula, o deslocamento e o que foi
   apagado à mão;
3. quadro trocado à mão por outro do vídeo vai na lista `quadros` (no p05: `fall_down`,
   15 → 16);
4. `--escala-da-receita` reusa a escala gravada (refazer em HD não muda o tamanho).

```
python3 tools/sprites/video_para_sprite.py <vídeo> --clipe tools/sprites/clips/p05_idle.json \
    --densidade 2 --escala-da-receita [--nitidez]
```

Custo por personagem HD (p05): PNG 27 → 84 MB no APK, memória decodificada 151 → 566 MB.

## Vários vídeos (ou quadros à mão) num clipe

Golpe longo gerado em partes (cada complemento começa no último quadro do anterior, no Flow) e
golpe que só vai até a pose (volta pelo inverso) usam `tools/sprites/montagem.py`, com a receita
em `tools/sprites/montagens/<id>.json`: trechos dos vídeos colados (sem perda, na mesma câmera),
os quadros escolhidos e as animações do pacote que usam a folha (frames como `"0-5,4-0"` para a
volta pelo inverso; uma folha pode servir a mais de uma animação). Trecho `"fixo"` tira o
deslocamento lateral (quem anda é o motor, com o avanço do golpe) e `"alinha"` leva o último
trecho de volta à raiz. Exemplo e explicação no p15 ([ficha](p15.md)):

    python3 tools/sprites/montagem.py p15 <pasta com os vídeos> [clipes]

Cabelo comprido de fios finos (p15): `"recorte_suave": true` nas opções (`--recorte-suave` no
`video_para_sprite.py`) deixa as mechas meio transparentes em vez de cortadas em bloco e reabre
os vãos de fundo entre elas. Andar: `"distancia": "video"` mede no vídeo quanto o corpo anda por
quadro, para o pé ficar plantado (use só o trecho dos passos, sem a guarda parada do começo).
Com a velocidade do motor (300 px/s para frente, 220 para trás) o ciclo toca no tempo que a
distância manda; `"velocidade": "video"` faz o lutador andar no chão na velocidade do vídeo
(`fighter.walkSpeed` / `walkBackSpeed` no character.json), e o ciclo toca no tempo do vídeo.
`"tempo": "video"` faz o contrário: deixa o andar do motor e acerta a distância por quadro para o
ciclo tocar no tempo do vídeo (o pé escorrega se o passo do vídeo for mais curto); com
`"acelerar": 2` o ciclo toca 2x mais rápido que o vídeo e o pé escorrega menos.
Andar de um passo só: use `"fixar": "volta"` nas opções, para a folha descontar o avanço da volta
inteira (o mesmo que o motor anda); a reta do tronco não bate com ele e o loop dá um solavanco.
`"balanco"` (0 a 1, padrão 1) diz quanto do vaivém do tronco no passo fica: com 0.5 uma pausa do
vídeo vira uma freada, sem pular quadros.

Personagem novo sem todos os vídeos: o `personagem.py` grava o pacote assim que o movimento
inteiro existe (IDLE, andar, agachar, pulo, dash, backdash); golpe do pulo ou agachado sem vídeo
fica com a pose, e defesas, reações, intro e vitória são opcionais no motor.

## Um vídeo pode virar vários clipes

| Vídeo | Clipes do jogo |
|---|---|
| pulo | subida (`JUMP`), descida (`FALL`), aterrissagem (`LAND`) |
| agachar | abaixar (`CROUCH`), agachado parado, levantar (`RISE`) |
| derrubado | queda (`KNOCKDOWN`), no chão (`GROUNDED`), levantar (`GETUP`) |
| dash | arrancada, corrida em loop (`DASH`), freada |
