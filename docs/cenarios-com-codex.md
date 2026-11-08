# Cenários a partir de um concept, com o Codex

Processo da Arena Central (pintura), `stages/arena_pintura`. As imagens são geradas pelo Codex,
o agente da OpenAI, entrando com a conta do ChatGPT Plus. Não há cobrança à parte: a cota é a do
plano. Os prompts usados ficam em `art/stages/arena_pintura/prompt_*.txt`.

## Por que assim

Uma pintura só (o concept) tem luz, perspectiva e traço que combinam. Pedir cada parte do zero
gera imagens parecidas, mas que não encaixam (as torres "retas"). O Codex **edita** a imagem: a
parte original volta idêntica, pixel por pixel, e só a área cinza é pintada. Então tudo sai da
mesma pintura.

## Preparar o Codex no ambiente

```
npm install -g @openai/codex
codex login --device-auth      # mostra um link e um código; entrar com a conta do ChatGPT
```

A conta precisa da opção "Ativar login com código de dispositivo para Codex" (chatgpt.com,
Configurações → Segurança). O login fica no ambiente até ele ser reciclado.

Cada pedido:

```
codex exec --skip-git-repo-check --sandbox workspace-write --image canvas.png - < prompt.txt
```

O Codex salva a imagem pedida na pasta. Ele devolve no tamanho da entrada (1672×941 com o
concept). Dois pedidos podem rodar ao mesmo tempo.

## Passos

1. **Limpar** (`prompt_1_limpar.txt`): o concept (uma tela do jogo com HUD) sem os lutadores e
   sem o HUD → `1_concept_limpo.webp`. Como o concept é uma tela do jogo, o enquadramento já é
   o da câmera de repouso: a largura da imagem é a largura da tela, e o pé dos lutadores fica
   em 552/720.
2. **Estender para os lados** (`prompt_2_estender_lado.txt`): a metade da borda numa tela do
   mesmo tamanho, a outra metade cinza. Um pedido por lado, e o panorama fica com 2 telas.
3. **Emendas** (`prompt_3_emenda.txt`): uma faixa cinza em cima de cada emenda, com os dois lados
   visíveis. O que sobrar de diferença de tom sai com `cv2.seamlessClone` (Poisson) só na faixa.
4. **Chão embaixo** (`prompt_4_chao_embaixo.txt`): a câmera aberta (zoom 0,78) vê mais chão. O
   pedaço de baixo do panorama com cinza embaixo, em duas metades, e depois um pedido no centro
   para ligar as duas (o anel fechando sem emenda).
5. **Nitidez**: Real-ESRGAN x2 (ONNX, CPU) e reduzir para 4096 de largura (o limite de textura)
   → `3_panorama_4096.webp`.
6. **Montar**: `tools/stage/cenario_pintura.py` com o enquadramento do concept
   (`--tela-px`, `--centro`, `--chao`, `--juncao`, `--horizonte`). Ele corta na linha do pé da
   arquibancada: a parede vai para o fundo e o chão vira projeção (ver
   [stage-engine.md](stage-engine.md)).
7. **Paralaxe do fundo** (`prompt_5_sem_arena.txt`, `prompt_6_arena_verde.txt`): o topo do
   panorama (sem o chão) com a arena inteira removida, e o Codex pinta o vale e as montanhas que
   ficavam atrás → `4_fundo_sem_arena_4096.webp`. Para recortar a arena, a mesma imagem com o céu
   e as montanhas em verde chapado. O Codex repinta a arena um pouco deslocada, então o verde só
   serve de molde: ele é alinhado à pintura original (SIFT + polinômio de 3º grau + fluxo óptico)
   e vira a máscara `5_mascara_arena_4096.png`. Embaixo de onde há céu em cada coluna, tudo é
   arena. A parede vira `parede.png` com transparência na profundidade da junção, e o fundo vai
   para 9000 (anda ~1/6 do que andam os lutadores). A imagem gerada vem com a 1ª linha clara:
   o script troca as 6 primeiras linhas pela 7ª antes de espelhar o céu.

O horizonte sai do anel dourado do chão (um círculo): com as bordas de longe e de perto e a meia
largura, resolve-se a altura do horizonte (0,454 na Arena) e daí a profundidade da junção
(1566). Para a Arena, os argumentos com a imagem de 4096 são:

```
--tela-px 2048 --centro 2048 --chao 883.14 --juncao 645.51 --horizonte 0.4542 --ceu-extra 600 \
--fundo art/stages/arena_pintura/4_fundo_sem_arena_4096.webp \
--mascara art/stages/arena_pintura/5_mascara_arena_4096.png
```

## Três planos de fundo, animação, luz e câmera

8. **Céu separado das montanhas** (`prompt_7_ceu_verde.txt`, `prompt_8_so_ceu.txt`): o Codex
   pinta o céu em verde (molde) e, à parte, só o céu sem terra. `tools/stage/alinhar_mascara.py`
   alinha o molde à pintura e dá `6_mascara_ceu_4096.png`. O `cenario_pintura.py` com `--ceu` e
   `--mascara-ceu` monta três planos: **céu** (30000), **montanhas** (`--prof-fundo`, 7000) e a
   **arena**. O céu é todo o pintado à parte (pedaços do céu da pintura davam dois sóis e
   fantasmas das torres); em cima ele é continuado espelhando a faixa de cima, sem esticar (o
   esticado virava riscos verticais). Céu e montanhas **não** ficam apoiados na arena: cada um
   anda pela sua profundidade, e o super pulo tem paralaxe.
   As montanhas (`prompt_10_montanhas_verde.txt`): o Codex repinta o fundo com o céu em verde e
   as montanhas vêm desse verde mesmo, recortadas pela própria cor (`--montanhas-verde`), sem
   molde alinhado: o recorte e a pintura são a mesma imagem e não sobra halo. A 1ª linha (clara)
   fica transparente e as cores são casadas com as da pintura.
9. **Animação** (`tools/stage/animar_pintura.py`): solta as bandeiras grandes e compridas (nas
   torres o buraco fica transparente, aparece o fundo; caixas baixas, como holofote, lanterna,
   painel de lava e enfeite do pavilhão, ficam pintadas), põe chama animada nos braseiros
   (`BRASEIROS`: borda da bacia e altura da chama, em pixels da `parede.png`, marcados à mão com
   uma régua; refazer se a parede mudar), pulsa os holofotes, mexe a plateia com flashes e corre
   a água das cachoeiras.
10. **Luz e câmera**: sombra comprida dos lutadores na direção do sol do conceito (atrás à
    esquerda) e `camera.zoom` 0,8 (só neste cenário a câmera da luta abre 20%, mais perto do
    enquadramento do conceito).
11. **Chão mais largo** (`prompt_9_chao_lado.txt`): com a câmera aberta, o chão perto da tela
    passava da borda da pintura e aparecia a cópia espelhada (o degrau da beira dobrado). O Codex
    continua o chão 520 px para cada lado (`8_chao_largo.webp`) e o `cenario_pintura.py` tira o
    chão dele (`--chao-largo`), com no máximo 4096 de largura. Sem a malha (Canvas), o chão
    projetado vai em faixas de 1 px (com 2 as linhas curvas saíam serrilhadas).
12. **Sem cara de recorte**: a borda da arena é limpa da cor do fundo antigo e recebe um pouco da
    cor do céu e das montanhas que ficam atrás dela ("light wrap", 5 px), com o alfa 1 px mais
    macio. As montanhas somem numa névoa quente perto do pé da arquibancada (perspectiva aérea:
    entre elas e a arena tem ar). Atrás das construções, uma sombra roxa difusa e fraca. Uma
    luz de contorno nítida foi tentada e saiu: virava um risco laranja em volta, o próprio
    recorte. No ar, `poeira` dourada contra o sol baixo. O pulo simples não mexe no zoom
    (`camera.zoomNoPulo: false`). As cachoeiras são achadas pelo contraste com a rocha em volta
    (a névoa clareia o pé das montanhas).
13. **Arena mais perto** (`--aproximar 0.7`): a parede vai para 70% da profundidade da junção
    (1566 → 1097) e cresce 1,25x em volta do ponto de fuga (o meio da tela na altura do
    horizonte), como um plano que chega perto da câmera; o chão acaba no pé dela. O anel
    vermelho (z ≈ 500) continua com chão atrás. A borda com a cor do fundo e a névoa nas
    montanhas acompanham a arena aproximada.
14. **Plano do meio** (`prompt_11_meio_verde.txt`, `--meio-verde`): entre a arena e as montanhas
    não havia nada (a plateia colada em montanhas lá longe). O Codex pinta, sobre verde chapado,
    colinas com mata de outono, pinheiros, pagodes e uma torre de vigia na névoa
    (`10_meio_verde_2x.webp`, já com o Real-ESRGAN x2). Recortado pelo próprio verde, vira a
    camada `meio` a 2000 (`--prof-meio`), com a silhueta 45 px acima da plateia na tela de
    repouso (`--meio-acima`), largura da tela, repetida espelhada. A borda da arena também pega a
    cor dele.
15. **Chão sem o degrau da esquerda** (`prompt_12_sem_meio_fio.txt`): o chão alargado tinha um
    meio-fio curvo à esquerda que, com a câmera no canto, virava faixas escuras esticadas. O
    Codex repinta a faixa como piso liso e ela volta para o `8_chao_largo.webp` (Real-ESRGAN,
    mistura suave na borda). O chão ganha mais 35% embaixo (espelhado) e o motor repete a última
    linha se a câmera vir um chão ainda mais perto.

Os argumentos completos da Arena hoje:

```
--tela-px 2048 --centro 2048 --chao 883.14 --juncao 645.51 --horizonte 0.4542 --ceu-extra 600 \
--aproximar 0.7 --fundo 4_fundo_sem_arena_4096.webp --mascara 5_mascara_arena_4096.png \
--ceu 7_ceu_4096.webp --mascara-ceu 6_mascara_ceu_4096.png --montanhas-verde 9_montanhas_verde_4096.png \
--meio-verde 10_meio_verde_2x.webp --prof-fundo 4000 --prof-ceu 12000 --zoom 0.8 --chao-largo 8_chao_largo.webp
```

(as imagens em `art/stages/arena_pintura/`; depois, `tools/stage/animar_pintura.py`).

## Arena Coberta (`stages/arena_coberta`)

Do concept da arena coberta com o telão (`art/stages/arena_coberta/0_concept.png`), pelo mesmo
caminho, mais curto (não tem céu nem montanhas):

1. **Limpar** (`prompt_1_limpar.txt`): sem os lutadores, o juiz e o HUD; o telão com a tela vazia
   (azul-escura, com linhas); faixas cinza em cima (teto) e embaixo (chão perto) completadas →
   `1_limpo.png`.
2. **Estender** (`prompt_2_estender_esq.txt`, `prompt_2_estender_dir.txt`) e **emendas**
   (`prompt_3_emenda.txt`, misturadas com borda suave) → `2_panorama_codex.webp`; Real-ESRGAN x2 e
   4096 de largura → `2_panorama_4096.webp`.
3. **Montar**: `cenario_pintura.py` com `--nitida` (parede sem desfoque, só um véu de névoa) e
   `--cor-acima` (em cima do teto pintado é escuro, não espelhado: o telão e os holofotes viravam
   de cabeça para baixo; o alto da pintura entra nesse escuro aos poucos). `--nome` e `--extra`
   (um JSON que substitui chaves do stage.json: luz e sombra de cima, poeira fraca) completam:

```
python3 tools/stage/cenario_pintura.py art/stages/arena_coberta/2_panorama_4096.webp \
  android/app/src/main/assets/stages/arena_coberta --tela-px 2048 --centro 2048 --chao 798 \
  --juncao 520 --parede-ate 1150 --horizonte 0.3786 --ceu-extra 500 --nitida --cor-acima '#0E0E1A' --nevoa '#2A2030' \
  --zoom 0.8 --zoom-repouso 0.948 --nome 'Arena Coberta' --extra art/stages/arena_coberta/stage_extra.json
python3 tools/stage/animar_coberta.py
```

   **Enquadrar pela câmera da luta** (`--zoom-repouso`): o concept é uma tela da luta, e a
   câmera da luta na largada (zoom 0,948 com o `camera.zoom` 0,8) não é a de repouso do motor
   (1,24). Medido pela de repouso, o chão e o fundo ficavam em proporções erradas na luta (o
   telão 30% maior, a barreira baixa). Com `camera.zoomRepouso` o stage.json descreve as
   camadas nessa câmera, e as medidas saem direto do concept: a largura da tela do celular é
   2048 px do panorama (`--tela-px`), e `--chao` é a linha dos pés no panorama. Para conferir,
   capturar no formato do celular (1870×841) e pôr lado a lado com o concept.
   O horizonte (0,379) sai do emblema do chão, um círculo: com a borda de longe, a de perto e a
   meia largura, resolve-se a altura do horizonte e a profundidade (o meio do emblema fica
   embaixo dos lutadores). A junção chão/barreira fica em z ≈ 1735.
4. **Animação** (`tools/stage/animar_coberta.py`): a tela do telão vira o `"telao"` da camada (a
   luta ao vivo e o replay do `TelaoLuta`), a plateia pula com flashes (sem o telão e a treliça),
   fachos de luz saindo de 10 spots da fileira de cima, varrendo devagar (duas imagens com os
   fachos em ângulos diferentes, ±7°, e `"onda"` em fase oposta: barato, duas imagens por quadro),
   cada spot do teto ganha um brilho pulsando em cima da lâmpada (achados no panorama original,
   sem o escurecimento de cima, e um por lâmpada), e o chão encerado reflete os lutadores.
5. **Os lados fechando** (desfeito; ver o passo 8) (estádio): a pintura estendida curva a barreira e a arquibancada para
   perto da câmera nas pontas, mas a parede era um plano só, na profundidade da junção do meio; a
   base curva virava chão esticado. `--parede-ate 700` deixa as linhas de baixo na parede, e o
   `animar_coberta.py` acha a base da barreira coluna a coluna e faz das pontas as camadas
   `parede-esq` e `parede-dir` com `"curva"` (cada coluna na profundidade da sua base, sem a
   parte de baixo: ali aparece o chão projetado). No meio reto, a parede plana continua.
6. **Chão fosco (testado e desfeito: o chão fica o encerado pintado, igual ao concept;
   `CHAO_FOSCO` no `animar_coberta.py`)** (`prompt_4_chao_fosco.txt`): o chão pintado era encerado, com os reflexos das
   lanternas e luzes em riscos verticais; no chão projetado, com a câmera no canto, eles abriam
   em leque. O chão (menos o emblema, até a base da barreira) vira cinza e o Codex repinta o
   mesmo piso fosco, com a pintura original junto como referência (`--image` duas vezes: o
   tamanho, a cor e as lajotas saem iguais). Primeiro o centro, depois cada lado continuando o
   centro; nas emendas, o lado é casado com o centro linha a linha (a cor) e misturado em 80 px.
   Tirar os reflexos pela imagem (filtros) não deu: os riscos são dourados e estourados.
   Os reflexos das lanternas voltam desenhados pelo motor (`"reflexoComprimento"` no `chao`):
   cada lanterna da barreira (achada na pintura, uma por lanterna, também nas pontas curvas) vira
   uma luz com reflexo, e o motor desenha o risco no chão, sempre vertical embaixo dela, com a
   câmera em qualquer lugar. No modo leve, sem reflexos.
7. **O estádio fechando nos cantos** (desfeito; ver o passo 8) (`prompt_5_volta_esq.txt`, `prompt_5_volta_dir.txt`): a
   câmera não passa muito das bordas da pintura, então a volta do estádio tem de estar nela. As
   faixas das pontas (560 px de cada lado no panorama de 3344) viram cinza e o Codex pinta a
   arquibancada, os pilares e a barreira com as lanternas virando para a câmera até o canto de
   baixo (o lado esquerdo com o direito pronto, espelhado, como referência). A base da barreira
   nas pontas é marcada à mão (`PONTAS_ESQ`, `PONTAS_DIR` no `animar_coberta.py`); as camadas
   curvas chegam no máximo a 700 de profundidade (`PROF_MIN`: mais perto, a ponta andava bem mais
   que o meio e abria um degrau). No chão projetado, onde a pintura tem barreira, entra o chão da
   pintura de antes da volta (`chao_sem_volta.jpg`), senão a barreira era esticada como chão.
   Agora `--parede-ate 1150`.
   **Desfeito em parte:** com cada coluna da ponta na sua profundidade, a ponta andava bem mais
   que o meio e, no canto, a pintura se desmontava (pedaços de arquibancada soltos sobre o chão).
   As pontas voltam ao plano da parede (só desenhadas depois do chão). Medido nas capturas: a
   parede, lá no fundo, anda só ~90 px da pintura entre o repouso e o canto; a volta pintada fora
   da tela de repouso quase não aparece. Para o estádio "fechar" na tela, a volta tem de estar
   dentro do enquadramento de repouso (as bordas da tela do concept).
8. **As paredes dos lados (o estádio fechando de verdade)**: a peça da ponta (`ponta.png`, uma
   arquibancada pintada sobre verde e posta no chão perto da câmera) foi testada e desfeita: ela
   aparecia na frente de tudo, sem ligação com o fundo. Agora cada lado é uma parede de verdade:
   um plano em x fixo do mundo, do fundo (profundidade 1734,7, encontrando a parede na borda da
   tela de repouso) até perto da câmera (60), com `"lateral"` no motor, que põe a perspectiva
   (`docs/stage-engine.md`). A imagem (`lateral_dir.jpg`, e a espelhada `lateral_esq.jpg`) é a
   fachada desenrolada: o trecho da própria parede do fundo do pilar com a bandeira até antes do
   telão (`LATERAL_TRECHO` no `animar_coberta.py`), repetido, do teto até a base da barreira,
   15% mais escuro. No repouso as paredes ficam fora da tela (igual ao concept); com a câmera
   no canto ou afastada (lutadores longe), a arquibancada vem do fundo até a câmera e a arena
   fecha. O chão continua o encerado aprovado (o mesmo `chao.jpg` da comparação com o concept).
   **Ajustes:** a parede reta (paralela ao olhar) convergia forte e parecia torta no canto;
   agora ela abre na diagonal (`LATERAL_ABRE` = 1200 no mundo, `"abre"` no motor), como a
   arquibancada fazendo a volta.
9. **O chão aprovado sem o leque**: os riscos de reflexo pintados no chão encerado abriam em
   leque com a câmera andando ou afastada (o chão é projetado em perspectiva). O chão vira duas
   partes (`reflexos_do_chao()` no `animar_coberta.py`): o piso fosco do Codex (`chao_fosco.jpg`,
   mesmas lajotas, na cor do encerado linha a linha) no chão projetado, e os reflexos (o que o
   encerado aprovado, `chao_encerado.jpg`, tem a mais) numa camada (`reflexos_chao.webp`) na
   profundidade da parede, desenhada logo depois do chão. Com a câmera parada a soma é o chão
   aprovado; com a câmera andando, cada risco fica reto embaixo da sua lanterna.

### Arena Coberta refeita: a arena oval inteira num desenho só

Tudo das laterais acima (paredes montadas, peças da ponta, volta pintada) foi desfeito: emenda e
recorte sempre apareciam. O cenário passou a ser **um desenho só, feito inteiro pelo Codex**:

1. `prompt_8a_arena_larga.txt`: o concept em tamanho menor no meio de uma tela cinza (o guia da
   composição) e o concept inteiro como referência de estilo. O Codex pinta a arena oval inteira
   na escala do concept (o meio é a vista da largada; o oval só fecha longe, nas pontas), com a
   arquitetura de estádio: barreira contínua, arquibancada em rampa dividida por escadas, nenhum
   pilar na frente do povo, bandeiras no anel do teto e nos camarotes.
2. `prompt_8_arena_oval.txt`: o mesmo desenho repintado inteiro, só sem os pilares na barreira →
   `6_arena_oval_aprovada.png` (e `6_visao_jogo_aprovada.png`, a prévia com HUD e lutadores,
   `prompt_9_visao_jogo.txt`).
3. Real-ESRGAN x2 e 4096 de largura → `7_panorama_oval_4096.webp`.
4. O mesmo desenho com o chão **sem os riscos de luz** (o Codex repinta só o chão), casado na cor
   com o aprovado linha a linha → `7_chao_fosco_4096.webp`, usado como `--chao-largo`. Pintados,
   os riscos abriam em leque com a câmera andando; agora o motor desenha o reflexo de cada
   lanterna da barreira (`reflexoImagem`: um brilho curto e macio, `risco.png`), sempre reto.
5. Montagem e animação:

```
D=android/app/src/main/assets/stages/arena_coberta; A=art/stages/arena_coberta
python3 tools/stage/cenario_pintura.py $A/7_panorama_oval_4096.webp $D --tela-px 2048 --centro 2048 \
  --chao 1288 --juncao 1010 --parede-ate 1190 --horizonte 0.3786 --ceu-extra 300 --nitida \
  --cor-acima '#0E0E1A' --nevoa '#2A2030' --zoom 0.8 --zoom-repouso 0.948 --nome 'Arena Coberta' \
  --extra $A/stage_extra.json --chao-largo $A/7_chao_fosco_4096.webp
python3 tools/stage/animar_coberta.py
```

## Dojo da Yin Lu (`stages/dojo_yinlu`)

O concept aprovado (o pátio com o portão do 連武館, `art/stages/dojo_yinlu/0_concept.png`) sem os dois
pilares vermelhos dos lados (pilar com meio telhado não fazia sentido, e perto da câmera ele não
funciona no canto). Tentativas descartadas na mesma pasta: galerias nos lados (2_, 4_), colunata (5_),
só dois pilares (6_), concepts novos (7_).

1. `prompt_1_limpar.txt`: o concept sem lutadores, HUD e botões → `1_limpo.png`.
2. `prompt_7_sem_pilar.txt`: sem os pilares, bandeiras, lanternas penduradas e beirais → `8_limpo_sem_pilar.png`.
3. `prompt_8_estender_esq/dir.txt`: metade da tela e metade cinza, o muro, a mureta e o pátio
   continuam retos (o Codex devolve a metade dada idêntica); `prompt_9_emenda.txt` fecha a emenda
   da direita → `8_panorama_codex.png` (2 telas).
4. Real-ESRGAN x2 e 4096 → `9_panorama_4096.webp`.
5. Montagem (escala da tela do concept, horizonte pelo emblema do chão, junção no começo do pátio):

```
D=android/app/src/main/assets/stages/dojo_yinlu; A=art/stages/dojo_yinlu
python3 tools/stage/cenario_pintura.py $A/9_panorama_4096.webp $D --tela-px 2048 --centro 2039 \
  --chao 801 --anel-longe 772 --anel-meia 139 --juncao 664 --ceu-extra 300 --nitida \
  --cor-acima '#1F6FE0' --nevoa '#CFE3F5' --zoom 0.8 --zoom-repouso 0.948 \
  --nome 'Dojo Liánwǔguǎn' --extra $A/stage_extra.json
```

(e tirar a `poeira` que o script põe; quem põe a poeira é a animação).
6. Animação (`tools/stage/animar_yinlu.py`): as quatro lanternas de pedra e as duas do portão
   brilham e tremulam (`luzes` da parede), raios de sol entre as árvores em duas imagens que cruzam
   uma na outra (`"onda"` em fase oposta) e poeira dourada no ar; as folhas caindo e a luz do sol nos
   lutadores vêm do `stage_extra.json`.
   As árvores balançam com o vento (`"vento"` do motor): o script separa as copas da parede
   (`arvores.png`, a parede fica com o céu borrado atrás), acha os troncos (alcance 0) e as pontas
   (alcance 1) e agrupa os galhos. A parede original fica em `art/stages/dojo_yinlu/parede_com_arvores.jpg`
   (rodar de novo parte dela). As copas do pátio de dentro, vistas pelo portão, são outra camada
   (`arvores_dentro.png`, vento mais fraco; o templo e as portas não mexem). As lanternas do
   portão ficam num poste e não balançam; as borlas vermelhas embaixo delas sim (recortadas pela
   cor, `borla_1/2.png`, com `"pendulos"`; atrás, o poste continuado de cima para baixo).
7. **Planos** (`tools/stage/planos_yinlu.py`, chamado pelo `animar_yinlu.py`): num plano só, o céu,
   as árvores e o muro andavam juntos (parecia uma foto), e no super pulo aparecia o céu esticado
   em cima das copas cortadas. O Codex trabalha em duas metades do topo do panorama (2248 px cada,
   400 de sobra, reduzidas para 1672 de largura) e cada metade volta com Real-ESRGAN x2; o
   `tools/stage/juntar_metades.py` junta as duas por um caminho que passa pelo céu (degradê deixava
   folhas fantasmas, corte reto cortava galho):
   - `prompt_10_alto.txt`: a metade embaixo e cinza em cima → as copas e o céu continuados para cima
     → `10_alto_4096.webp` (565 linhas acima da pintura; da linha 0 para baixo, a pintura mesma);
   - `prompt_11_sem_muro.txt` → `11_sem_muro_4096.webp`: as árvores sem o muro, com tronco e chão;
   - `prompt_12_so_ceu.txt` → `12_so_ceu_4096.webp`: só o céu, com as nuvens inteiras;
   - `prompt_13_muro_verde.txt`: o muro e o portão sobre verde; `alinhar_mascara.py` dá
     `13_mascara_muro_4096.png`.

   Ficam quatro planos: **céu** (degradê, 30000), **nuvens** (do "só céu", separadas do azul pela
   saturação, a 3500, andando com `"deriva": 20`), **árvores** (a pintura onde aparecem, o "sem
   muro" atrás do muro e do portão, a 1000, com o vento) e a **parede** (`parede.png`: os mesmos
   pixels de antes, com o alfa do molde). A parede continua nas linhas de antes (as lanternas, as
   borlas e as árvores de dentro do portão não mudaram de lugar).

   Mais dois planos: o **pátio de dentro** visto pelo portão (`prompt_14_patio.txt`: o recorte em
   volta do portão sem o portão, o pátio pintado mais largo que o vão; alinhado por SIFT ao que se
   via e com as cores casadas → `14_patio.webp`; o vão sai do molde `prompt_15_vao_verde.txt` →
   `15_mascara_vao.png`), a 850, com o vento das copas de dentro; e a **mata ao longe**
   (`prompt_16_mata.txt`: uma fileira de árvores distantes sobre magenta → `16_mata.png`), a 1800,
   com névoa puxando para o azul do céu, aparecendo nos vãos entre as copas.
   Mata, árvores e pátio ficam com `"apoio"`: no super pulo, soltos, subiam atrás do muro (a mata
   aparecia demais; no vão do portão aparecia a grama embaixo do pátio); presos ao muro (apoio na
   profundidade da parede), ficava parado demais. As árvores apoiam em 820 (~metade do movimento) e
   a mata em 1000 (~40%); o pátio fica preso ao muro (só 5 px de pintura embaixo do vão). Na
   horizontal a paralaxe continua; céu e nuvens, soltos.

Os fachos da Arena Coberta e os raios do dojo ficam meia unidade na frente da parede: atrás dela
(mais fundo), a parede opaca os cobria.

## Arena Imperial (`stages/arena_imperial`)

Refeita como uma pintura só (antes era montada em peças recortadas: céu, mar, penhascos,
arquibancadas, palácio, árvores, pórticos, telão; o telão escorregava pelo cenário e tudo tinha
cara de recorte). Arte em `art/stages/arena_imperial_pintura/`.

1. `0_concept.png`: o print do jogo. `prompt_1_limpar.txt` → `1_limpo.png` (sem lutadores, HUD,
   botões; telão vazio; céu e chão completados). `prompt_2_sem_bandeiras_ceu.txt`: as duas
   bandeiras que ficaram soltas no céu saem → `2_sem_bandeiras.png`.
2. **Inteira** (`prompt_5_inteira.txt`): estendida em metades, cada lado saía com outra
   perspectiva no chão e a emenda não fechava; o print vai pequeno no meio de uma tela cinza e o
   Codex pinta a arena toda de uma vez, com um horizonte e um ponto de fuga → `3_inteira.png`;
   Real-ESRGAN x2 e 4096 → `4_panorama_4096.webp`.
3. Chão sem os riscos de luz (`prompt_11_chao_fosco.txt`, casado com o aprovado linha a linha →
   `14_chao_fosco_4096.webp`, o `--chao-largo`): o motor desenha o reflexo de cada lanterna da
   mureta (pintados, os riscos abriam em leque com a câmera afastada).
4. Planos: `prompt_6_arena_verde.txt` (céu e montanhas em verde → molde `10_mascara_arena_4096.png`),
   `prompt_7_sem_arena.txt` (só o céu e as montanhas, com o vale atrás → `12_sem_arena_4096.webp`),
   `prompt_8_ceu_magenta.txt` (o céu dele em magenta → molde `15_mascara_montanhas_4096.png`) e
   `prompt_9_so_ceu.txt` (só o céu → `13_so_ceu_4096.webp`).

```
D=android/app/src/main/assets/stages/arena_imperial; A=art/stages/arena_imperial_pintura
python3 tools/stage/cenario_pintura.py $A/4_panorama_4096.webp $D --tela-px 2048 --centro 2048 \
  --chao 1387 --anel-longe 1347 --anel-meia 202 --juncao 1262 --ceu-extra 300 --nitida \
  --cor-acima '#2A1A3A' --nevoa '#3A2030' --zoom 0.8 --zoom-repouso 0.948 --nome 'Arena Imperial' \
  --extra $A/stage_extra.json --chao-largo $A/14_chao_fosco_4096.webp
python3 tools/stage/animar_imperial.py
```

5. Plano do meio (`prompt_16_meio.txt`): entre as montanhas e a arena não havia nada (o vale
   escuro atrás dos telhados). O Codex pinta, sobre magenta e no estilo da arena, colinas com
   bordos vermelhos e pinheiros, pagodes, um portal e uma cachoeira na névoa → `16_meio.png`
   (Real-ESRGAN x2 → `17_meio_2x.webp`), a 2000, do alto das colinas na linha 740 do panorama
   até atrás da arena, repetido espelhado e com `"apoio"` em 1100 (no super pulo anda menos).

6. Nuvens andando: são laranja e vermelhas sobre um céu azul-roxo (da mesma família de cor do
   pôr do sol: não dá para usar a chave de branco da Yin Lu). O alfa sai da "temperatura" de cada
   pixel (vermelho menos azul) e a camada `nuvens` (10000, `"deriva": 45`) usa os pixels da
   pintura; atrás, o céu limpo do Codex (`prompt_20_ceu_limpo.txt` → `22_ceu_limpo_4096.webp`, o
   degradê e o sol), casado em cor com o que aparece entre as nuvens. Perto do horizonte as nuvens
   são uma faixa contínua em volta do sol: recortadas, o sol sumia atrás delas e um buraco escuro
   andava junto; dali para baixo (`HORIZONTE_FIXO`) o céu fica a pintura, parado. O molde verde
   do Codex (`prompt_19_nuvens_verde.txt`) foi tentado e saiu: ele repinta as nuvens com outras
   formas e o molde não bate.

O `animar_imperial.py` separa céu (30000), nuvens (10000), montanhas (6000), plano do meio (2000) e arena (`parede.webp`, com o alfa do
molde; no que aparece em repouso, céu e montanhas são os pixels da pintura), põe o telão com a luta
ao vivo, a plateia dos dois prédios, as lanternas (as da mureta com reflexo no chão), a água das
duas cachoeiras e o vento nas folhas do bordo vermelho. Parede e montanhas em WebP sem perda (o
PNG da parede passava de 9 MB). `lanterna.png` e `chao_placas.jpg` da arena antiga ficam: o Terraço
da Lua usa.

## Terraço da Lua (`stages/terraco_lua`)

O cenário do ep07 (Sob a Mesma Lua: o Ryūga e o Kōga na véspera da final, depois os agentes). Era
provisório (peças da Central e da Imperial). Arte em `art/stages/terraco_lua_pintura/`.

1. Concepts na vista do jogo, no estilo da Imperial (`prompt_1_concept_a/b.txt`); pedido "mais
   alto": o terraço no topo de um penhasco, acima das nuvens (`prompt_2_alto_a.txt`) → aprovado o
   A, `2_alto_a.png`.
2. Inteira (`prompt_3_inteira.txt`: o concept pequeno no meio, cinza em volta) → `3_inteira.png`;
   Real-ESRGAN x2 → `7_panorama_4096.webp`.
3. Molde do terraço (`prompt_4_terraco_verde.txt` → `8_mascara_terraco_4096.png`), céu sem as
   nuvens (`prompt_7_ceu_limpo.txt` → `10_ceu_limpo_4096.webp`, saiu alinhado), sem o terraço
   (`prompt_5_sem_terraco.txt` → `11_sem_terraco_4096.webp`: o Codex mudou o horizonte de lugar;
   só o mar de nuvens dele entra, deslocado para o horizonte da pintura, atrás da mureta) e o chão
   sem os riscos de luz (`prompt_6_chao_fosco.txt` → `12_chao_fosco_4096.webp`).

```
D=android/app/src/main/assets/stages/terraco_lua; A=art/stages/terraco_lua_pintura
python3 tools/stage/cenario_pintura.py $A/7_panorama_4096.webp $D --tela-px 2048 --centro 2048 \
  --chao 1358 --anel-longe 1322 --anel-meia 162 --juncao 1258 --horizonte 0.5149 --ceu-extra 300 \
  --nitida --cor-acima '#060A1E' --nevoa '#101830' --zoom 0.8 --zoom-repouso 0.948 \
  --nome 'Terraço da Lua' --extra $A/stage_extra.json --chao-largo $A/12_chao_fosco_4096.webp
python3 tools/stage/animar_terraco.py
```

(o horizonte é o ponto de fuga das juntas do piso, linha 1068; o anel do mosaico dava 966). O
`animar_terraco.py` separa o céu (30000, com o brilho da lua) numa imagem só com o mar, o mar
de nuvens, as ilhas, o estádio e o templo, as nuvens (12000, as da pintura mais claras que o céu
limpo, `"deriva": 30`) e o terraço (`parede.webp`, mureta e postes com o alfa do molde); acende
as lanternas (as da mureta com reflexo no chão, pelo motor).

O céu e o mar ficam juntos porque o horizonte está no infinito: não há paralaxe entre eles. Em
duas camadas (céu a 30000, mar e ilhas a 6000, mesmo com `"apoio"` no céu) a emenda logo acima do
horizonte aparecia como uma linha e as ilhas saíam cortadas, e na horizontal as ilhas
escorregavam sobre o céu. Na imagem única, em cima vai o céu limpo do Codex e, do horizonte para
baixo e nas ilhas que sobem acima dele (onde a pintura difere do céu limpo e não é nuvem), a
pintura. A imagem continua 850 linhas abaixo da mureta (`FUNDO_MAIS`, o mar de nuvens do
`11_sem_terraco`): no super pulo o terraço desce mais que o céu e abria uma faixa preta embaixo.
O céu limpo do Codex ainda tinha a mureta e os postes embaixo: eles saem (`tirar_postes`, o céu
ao lado deslocado na horizontal entra no lugar; um inpaint deixava borrões laranja), atrás da
mureta entra o mar sem terraço, e as nuvens perdem a faixa dos postes (a borda dourada deles
passava no recorte por brilho). Sem isso, no super pulo aparecia um poste solto no ar.
