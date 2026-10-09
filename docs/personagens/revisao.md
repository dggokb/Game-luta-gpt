# Revisão dos personagens no jogo

Saída de `python3 tools/sprites/auditoria.py player_base p03 p02 p04`, conferida nas folhas.
Cada item: **corrigir** (visto na folha, está errado), **conferir** (pode ser, olhar no jogo)
ou **falso** (a auditoria errou; fica registrado para calibrar a ferramenta).

Como corrigir cada tipo (ferramentas em `docs/personagens/video.md`, "Auditoria"):

| Tipo | Correção |
|---|---|
| tamanho | `reescalar.py` (golpe); em pose ereta, gerar de novo a partir da imagem inicial |
| pulo / deslize | `ancorar.py` |
| pose agachado | gerar de novo (o vídeo levanta o personagem) |
| efeito | `limpar.py`; efeito por cima do corpo, gerar de novo |
| arte de outro personagem | falta o vídeo |

## Situação (1ª rodada de correção)

Corrigido sem vídeo novo:
- pé de apoio fixado: p01 S3; p03 ULTRA, S1, M, H; p02 S4, S3, H, 2L, S1; p04 H, S1, S3, LL;
- tamanho: p02 M e TAUNT (20% menores), ULTRA (zoom de câmera nos quadros 1-7);
- efeitos apagados: p02 LL, M, S3, HIT_AIR, arremesso, rastro do 2M; p04 LL, 2H e vitória (clarão);
- p04 DASH: quadro só de poeira trocado pelo anterior.

Auditoria depois: p01 1 erro (falso), p03 3, p02 3, p04 2 (antes 2, 6, 8, 5).

Falta vídeo novo (gerar a partir da imagem inicial):
- p03: CROUCH_LIGHT e HIT_CROUCH (levantam), pulo médio e agarrão (usam a arte do p01);
- p02: HIT_CROUCH (levanta), fim do 2M em pé, H (arco por cima da perna), INTRO e VICTORY
  (zoom de câmera em pose ereta);
- p04: fim do 2M em pé, INTRO (zoom de câmera), DASH (corrida pouco aproveitável);
- p01: HIT_CROUCH, se o tamanho incomodar no jogo (a cabeça mede 8-18% menor).

## p01 (player_base)

| Clipe | Achado | Veredito |
|---|---|---|
| SPECIAL_S3 | termina com o pé de trás 32 px atrás do idle (q20) | corrigir |
| HIT_CROUCH | cabeça 18% menor que a do agachado | corrigir |
| CROUCH_HEAVY | pé de trás anda 24 px e volta | falso: é a troca de base de agachado para em pé do upper |
| ULTRA_BEAM q2, HIT_AIR q3, SPECIAL_S2 q4 | cor fora da paleta | falso: luz do golpe no corpo |

## p03

| Clipe | Achado | Veredito |
|---|---|---|
| CROUCH_LIGHT | fica em pé (q1-9) e o pé anda 22 px | corrigir (gerar de novo) |
| HIT_CROUCH | fica em pé (q2-7) | corrigir (gerar de novo) |
| ULTRA_BEAM | começa já no golpe; pé anda 69 px e termina 16 px fora do idle | corrigir |
| SPECIAL_ENERGY | pé de trás anda 22 px e não volta | corrigir |
| MEDIUM_KICK | pé de trás anda 30 px e volta | corrigir |
| HEAVY_STRAIGHT | pé anda 32 px, termina 12 px fora | corrigir |
| SPECIAL_S3 | pé anda 61 px, termina 16 px fora | conferir (golpe longo com passos) |
| JUMP_MEDIUM, THROW_GRAB/TOSS | usa a arte do p01 | falta o vídeo |
| CROUCH_MEDIUM | cabeça 18% menor; pé anda 12 px | falso: rasteira girando |
| JUMP_HEAVY_DOWN q6 | cor fora da paleta | falso: luz no cabelo |

## p02

| Clipe | Achado | Veredito |
|---|---|---|
| MEDIUM_KICK | cabeça 24% maior (escala manual, nota 0,54) | corrigir |
| TAUNT | cabeça 20% maior (escala pela guarda, nota 0,62) | corrigir |
| ULTRA_BEAM | cabeça 27% maior nos quadros do meio | conferir |
| HIT_CROUCH | fica em pé (q1-6) | corrigir (gerar de novo) |
| CROUCH_MEDIUM | último quadro em pé; rastro escuro da rasteira (q5-6) | corrigir |
| SPECIAL_S4 | termina 28 px à frente do idle | corrigir |
| SPECIAL_S3 | termina 23 px atrás do idle; poeira q13 | corrigir |
| HEAVY_STRAIGHT | arco do chute (q7-8); termina 14 px fora | corrigir |
| CROUCH_LIGHT | pé anda 32 px e volta | corrigir (tremida da conversão) |
| SPECIAL_ENERGY | pé anda 16 px e não volta | corrigir |
| LIGHT_JAB q3-5, MEDIUM_KICK q3-4 | poeira no chão | corrigir |
| HIT_AIR q1, THROW_TOSS q2 | risco/arco branco | corrigir |
| INTRO, VICTORY | cabeça 16-18% maior | falso: em pé ereto; as pontas batem com o idle |
| LAND q1, ULTRA_BEAM | cor / pé anda 16 px | conferir |

## p04

| Clipe | Achado | Veredito |
|---|---|---|
| INTRO | cabeça 16% maior (escala manual) | corrigir |
| CROUCH_MEDIUM | fica em pé no fim (q11-13); pé anda 36 px | corrigir |
| HEAVY_STRAIGHT | pé anda 28 px; termina 16 px fora | corrigir |
| SPECIAL_ENERGY | pé anda 35 px, termina 20 px fora | corrigir |
| SPECIAL_S3 | pé anda 30 px e não volta | corrigir |
| DASH q3-4 | quadro de poeira | corrigir |
| LIGHT_JAB | clarão no punho q2; termina 14 px fora | corrigir |
| CROUCH_HEAVY q4, VICTORY q11 | clarão no punho | corrigir |
| ULTRA_BEAM q3 | aura amarela | conferir (pode ser de propósito) |
| SUPER_WAVE q5,8 | energia na mão | conferir (o projétil já é desenhado à parte) |
| SPECIAL_S4 q4,12 | rastro de poeira | conferir |
| THROW_GRAB, ULTRA_BEAM | pé anda 15-20 px e volta | conferir |

## Calibração da auditoria (4 personagens)

- Tamanho por pose e pose agachado: todos os achados conferidos eram reais.
- Cabeça: acerta os erros grandes (p03 antigo +53%, p02 M +24%); em pose ereta (intro,
  vitória) dá falso até ~18%. Por isso abaixo de 14% não avisa.
- Pulo/deslize: depois de ignorar troca de pé (> 15 px num quadro), os achados são reais;
  sobra falso em troca de base (agachado → em pé).
- Efeito por cor fora da paleta: pega poeira, arco, risco e até quadro de outro personagem;
  luz do golpe no cabelo/roupa ainda dá aviso falso.
