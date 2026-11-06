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
9. **Animação** (`tools/stage/animar_pintura.py`): solta as bandeiras grandes (nas torres o
   buraco fica transparente, aparece o fundo), põe chama animada nos braseiros (`BRASEIROS`,
   marcados à mão e ajustados pela cor), pulsa os holofotes, mexe a plateia com flashes e corre
   a água das cachoeiras.
10. **Luz e câmera**: sombra comprida dos lutadores na direção do sol do conceito (atrás à
    esquerda) e `camera.zoom` 0,8 (só neste cenário a câmera da luta abre 20%, mais perto do
    enquadramento do conceito).
11. **Chão mais largo** (`prompt_9_chao_lado.txt`): com a câmera aberta, o chão perto da tela
    passava da borda da pintura e aparecia a cópia espelhada (o degrau da beira dobrado). O Codex
    continua o chão 520 px para cada lado (`8_chao_largo.webp`) e o `cenario_pintura.py` tira o
    chão dele (`--chao-largo`), com no máximo 4096 de largura. Sem a malha (Canvas), o chão
    projetado vai em faixas de 1 px (com 2 as linhas curvas saíam serrilhadas).
12. **Relevo e ar**: a borda da arena (o recorte) é limpa da cor do fundo antigo; atrás das
    construções vai uma sombra roxa difusa, deslocada para longe do sol, que escurece as
    montanhas em volta delas (a arena deixa de parecer colada), e as bordas viradas para o sol
    ganham uma luz de contorno quente e fraca. No ar, `poeira` dourada contra o sol baixo. O
    pulo simples não mexe no zoom (`camera.zoomNoPulo: false`).
