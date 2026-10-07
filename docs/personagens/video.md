# Animação por vídeo (Seedance) — padrão

Decidido no p01: cada animação é um **vídeo curto** gerado no Seedance a partir de uma
imagem inicial do personagem. O vídeo vira os quadros do jogo com
`tools/sprites/video_para_sprite.py` (recorta o verde, tira sombra e poeira, corrige o
deslizamento, escolhe o loop e coloca tudo na mesma escala). Os prompts de cada
personagem estão na ficha dele (`p01.md`, `p03.md`).

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

- **Formato 16:9**, imagem inicial indicada na tabela do golpe. Nunca trocar o tamanho
  do personagem: a ferramenta mede a escala no 1º quadro.
- **Um vídeo por animação.** Começa e termina na guarda da imagem inicial.
- **O golpe acontece UMA vez e rápido** (velocidade de jogo de luta); depois ele fica em
  guarda respirando. A ferramenta corta o trecho certo.
- **Sem efeitos:** fogo, energia, poeira, rastro, faíscas e sombra são do jogo. Se o vídeo
  inventar efeito, o golpe fica sujo.
- **Sem adversário:** golpes acertam o ar; reações a golpes vêm de "alguém invisível".
- Gerou estranho (virou de frente, a câmera andou, o cabelo mudou, saiu do quadro)?
  **Gere de novo**, não dá para consertar depois.
- **Não precisa de vídeo** para: virar de lado (o jogo espelha o sprite) e pulo para
  frente/trás (o jogo usa o mesmo pulo e move o personagem).

## Um vídeo pode virar vários clipes

| Vídeo | Clipes do jogo |
|---|---|
| pulo | subida (`JUMP`), descida (`FALL`), aterrissagem (`LAND`) |
| agachar | abaixar (`CROUCH`), agachado parado, levantar (`RISE`) |
| derrubado | queda (`KNOCKDOWN`), no chão (`GROUNDED`), levantar (`GETUP`) |
| dash | arrancada, corrida em loop (`DASH`), freada |
