# Motor universal de transição INTRO → IDLE (prova com P02)

## Causa real

`SpriteFighterRenderer.draw()` executava um crossfade exclusivo para
`player_base` (P01). Qualquer outro lutador usava uma troca seca do
último frame de `INTRO` para o primeiro frame de `IDLE`.
As versões anteriores corrigiam isolamento, escala e movimento dentro da
INTRO, mas NÃO validavam a transição real entre dois estados do jogo.

## Implementação

`SpriteIntroTransition.blend(Animation,float)` agora calcula um fade por
tempo do clipe, sem índices específicos e independente do personagem.
O renderer desenha o frame real de INTRO e a primeira pose REAL de IDLE,
cada uma com os roots, pixelScale e worldScale de seus atlas. Nenhum
bitmap é redesenhado, nenhuma duração/hitbox/mecânica é alterada.

O período final de transição usa `min(0.42 s, 16% da duração INTRO)`.
O blend tem curva smoothstep. Quando a INTRO termina, a imagem já é IDLE.
O IDLE subsequente começa de seu primeiro quadro canônico (P02 = 15).
Se uma luta aguarda a INTRO mais longa do oponente, o personagem fica
no primeiro quadro de IDLE até a luta começar.

**Compatibilidade:** P01 continua usando seu caminho previamente aprovado.
Essa exceção antiga será migrada somente após aprovação visual, pois
alterá-la agora violaria a restrição de preservar o P01.
Outros personagens com INTRO e IDLE passam pela mesma política comum.

## Verificações

- `SpriteIntroTransitionTest.java`: utiliza `Canvas` Android
  e os atlas realmente empacotados, comparando a imagem final da INTRO
  com o primeiro frame do IDLE; rejeita uma troca com salto mensurável.
- A mesma política é testada com P02, P03 e P04.
- `render_transicao_intro_idle.py --character p02`: gera GIFs
  **INTRO → IDLE** antes/depois e relatório numérico da passagem.
- Regressões P01/IDLE/DASH permanecem obrigatórias.
- O workflow compara byte a byte os recursos aprovados e confere os
  PNGs dentro do APK publicado.
- Nenhum merge em `main` ou `game-luta-sprite-gpt`.

## Limitação explícita

Reduzir o salto na passagem não equivale a redesenhar poses incompatíveis
de forma convincente. Crossfade pode causar dupla silhueta por alguns
instantes. O relatório automatizado nunca substitui avaliação visual no APK.
Se o cruzamento for feio, a melhoria seguinte deve ser uma ponte real de
poses (derivada de imagens existentes), não maquiagem em testes.
