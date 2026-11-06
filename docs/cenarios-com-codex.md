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
8. **A ponta do estádio como peça própria** (`prompt_6_ponta.txt`, `4_ponta_verde.png`): o
   panorama volta a ser o do concept (sem volta pintada, `--parede-ate 700`) e o Codex pinta uma
   peça nova, sobre verde: o fim da arquibancada curvando para a câmera, com um pilar grande,
   bandeira e lanternas na barreira. Recortada pela chave verde, ampliada (Real-ESRGAN) e
   reduzida, vira `ponta.png`; o fim de dentro some num degradê de 420 px (senão a peça acabava
   num corte reto no meio do chão). `pontas()` no `animar_coberta.py` põe a peça no chão
   (`"noChao"`) em profundidade 300, 100 além de cada ponta da arena (`PONTA_PROF`,
   `PONTA_FORA`, `PONTA_LARGURA` = 700 de largura no mundo), a da direita espelhada (`"virar"`).
   Perto da câmera, ela anda mais que a parede: no repouso fica fora da tela (igual ao concept);
   com a câmera no canto, entra a arquibancada fechando o estádio.
