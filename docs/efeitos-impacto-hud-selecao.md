# Efeitos de impacto, HUD e seleção — 12/10/2026

Branch `ccr-60181022-rh9cms`. Apresentação apenas: nenhum efeito escreve no combate.

## Combate
- `ImpactEffects`: faísca de acerto (núcleo quente, estrela laranja/branca, anel de choque, estilhaços com gravidade) e faísca de defesa (arco de barreira e hexágono azul). Pool fixo de 12 explosões e 96 estilhaços, relógio da simulação a 60 Hz, sem alocação por quadro.
- Tamanho pela classe do golpe (L/M/H/especial); golpes pesados ganham dois traços cruzados e empurram a câmera (M = 3, H = 7, especial/ultra = 10).
- `CombatEngine.HitEvent` agora leva `impactX/impactY` (frente da hitbox presa ao corpo do defensor, medida antes do empurrão) e `strength`. Agarrões e ultra usam o peito do defensor.
- Projéteis: brilho radial, cauda de cometa, faíscas soltando e anéis girando; o Super ganha raios. Carga do Super: aura, linhas convergindo, halos tracejados e partículas subindo.

## HUD
- Combo com número em degradê e contorno duplo, salto e tremida por acerto, faixa de dano/escala; o contador da CPU fica à esquerda da coluna de botões de treino.
- Banners com faixa diagonal, degradê dourado e contorno quente.
- Vida: brilho deslizante, pulso vermelho-laranja com ≤25%, pedaço perdido branco que esfria para âmbar. Retrato treme e fica vermelho ao levar golpe.
- Super: energia correndo nas células cheias, halo do nível e "pop" ao subir de nível.
- Botões de toque com corpo de vidro, anel colorido (L azul, M amarelo, H vermelho, COMBO roxo), brilho ao apertar e halo girando quando ASSIST/SUPER/OD estão prontos. Botões de treino em painéis angulares.

## Seleção
- Herói entra deslizando com flash de silhueta branca ao trocar de prévia.
- Placa com código e estilo de luta acima do painel TEAM.
- Cursor com cantoneiras animadas e etiqueta da vaga (P1, TAG P1, P2, TAG P2).
- Confirmação: anel saindo do retrato e explosão branca na vaga do TEAM. Com as duas equipes completas, carimbos "PRONTO!". Entrada na luta com flash branco curto.

## Revisão visual
`VisualReviewTest` desenha quadros reais (Canvas nativo do Robolectric) via `GameView.renderFrame`. O workflow `visual-review.yml` roda os testes, gera o APK e publica JPGs e o resumo do Gradle na branch `visual-review-rh9cms`.
