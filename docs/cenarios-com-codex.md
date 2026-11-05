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

O horizonte sai do anel dourado do chão (um círculo): com as bordas de longe e de perto e a meia
largura, resolve-se a altura do horizonte (0,454 na Arena) e daí a profundidade da junção
(1566). Para a Arena, os argumentos com a imagem de 4096 são:

```
--tela-px 2048 --centro 2048 --chao 883.14 --juncao 645.51 --horizonte 0.4542 --ceu-extra 600
```
