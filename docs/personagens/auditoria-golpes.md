# Auditoria dos golpes: ficha × jogo (14/10)

Compara a "Lista de golpes" de cada ficha (`pNN.md`) com o que o jogo faz hoje
(`characters/<id>/character.json`, motor e animações). Os golpes normais (L, M, H, agachados e
no ar) ficam de fora: aqui entram os especiais (S1 a S4), Super, Ultra e agarrão.

✅ bate com a ficha · ⚠️ parcial · ❌ não bate

## O problema geral (vale para todos)

1. **Os comandos são iguais para os 12.** S1 = ↓→ (por isso a meia-lua ↓↘→ sempre sai o S1),
   S2 = →↓↘, S3 = ↓← com L ou M, S4 = ↓← com H. S3 e S4 têm o mesmo movimento e só o botão muda.
2. **O S1 é a mesma bola de energia nos 12**, só muda a cor e a velocidade. Kunai, machado,
   chicote, bisturi e sombra rasteira aparecem como bola. Até o p07 (tapa) e a p12 (investida),
   que pela ficha não têm projétil, soltam a bola.
3. **Todo especial é um golpe só.** A animação vem de um vídeo com vários movimentos (deflete e
   responde, três investidas, salto e voadora...), mas o jogo estica esse vídeo sobre **um**
   acerto genérico. É o "faz um monte de movimento e sai um golpe normal".
4. **O Super é igual em 11 dos 12:** a animação de sequência de golpes toca e no fim sai uma onda
   de energia. A sequência não acerta nada. Só o p07 mudou (capangas).
5. **O Ultra é igual nos 12:** investe, entra a cinemática e sai um raio. A cinemática só tem
   arte de verdade no p01; nos outros é provisória. Pela ficha só p01 e p06 têm raio.

## Personagem por personagem

### p01 — irmão calmo (poder)
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Onda, projétil reto | bola + carga (segurar) | ✅ |
| S2 | Punho ascendente | uppercut, 1 acerto, derruba | ✅ |
| S3 | Aparar: deflete e responde com um soco | golpe comum, não deflete nada | ❌ |
| S4 | Chute giratório que avança | gira no lugar | ⚠️ falta avançar |
| Super | Onda Máxima, projétil grande multi-hit | onda grande, 1 acerto | ⚠️ falta multi-hit |
| Ultra | Raio da Casa | investida + cinemática + raio (arte própria) | ✅ |

### p02 — kung fu (garça)
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Palma de energia curta | bola de alcance curto | ⚠️ visual de bola |
| S2 | Chute da garça (anti-aéreo) | chute, 1 acerto, derruba | ✅ |
| S3 | Contra-golpe com palma | contra-golpe alto | ✅ |
| S4 | Chute rodopiante que avança no ar | gira no lugar | ⚠️ falta avançar |
| Super | Rajada da Garça (sequência de palmas e chutes) | animação de sequência + **onda** | ❌ |
| Ultra | Dança da Garça (enxurrada de golpes) | sequência do vídeo, 6 golpes nos quadros da animação | ✅ |

### p03 — irmão explosivo (golpe)
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Onda curta e rápida | bola de alcance curto | ✅ |
| S2 | Uppercut que avança e acerta **várias vezes** | avança, **1 acerto** | ⚠️ falta multi-hit |
| S3 | Rekka: 3 investidas, cada uma com novo comando | cada comando toca o vídeo das **3** investidas e dá 1 acerto | ❌ |
| S4 | Chute em mergulho **no ar** | sai no chão | ❌ |
| Super | Rajada de Chutes avançando, chute final que lança | animação + **onda** | ❌ |
| Ultra | Fúria da Casa (sequência + soco de energia) | sequência do vídeo, 9 golpes nos quadros da animação | ✅ |

### p04 — lutador de rua
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Soco com estouro curto na frente do punho | bola de alcance curto | ⚠️ visual de bola |
| S2 | Cabeçada em avanço | avança, 1 acerto | ✅ |
| S3 | Esquiva e contra-soco, cancela em soco | esquiva alta + cancela em L/M/H | ✅ |
| S4 | Ombrada (investida baixa) | no lugar | ⚠️ falta avançar |
| Super | Chuva de Socos | animação + **onda** | ❌ |
| Ultra | Soco de Rua (sequência + cabeçada) | sequência do vídeo, 6 golpes nos quadros da animação | ✅ |

### p05 — ninja
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Kunai reto e rápido | **bola** | ❌ |
| S2 | Shuriken em diagonal (projétil) | **golpe corpo a corpo** | ❌ |
| S3 | Selo e teleporte | some e reaparece atrás | ✅ |
| S4 | Corte de mão que avança | avança | ✅ |
| Super | Dança das Lâminas | animação + **onda** | ❌ |
| Ultra | Eclipse (some e ataca em enxurrada) | sequência do vídeo, 7 golpes nos quadros da animação | ✅ |
| Extra | wall jump | wall jump com animação | ✅ |

### p06 — sombra e ilusão
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Sombra rastejante, **rasteira e lenta** | **bola** na altura do peito | ❌ |
| S2 | Clone que avança com palma | eco de sombra (decidido no lugar do clone) | ⚠️ decisão |
| S3 | Puxar | alcança longe e puxa | ✅ |
| S4 | Passo-sombra (desliza e palma) | no lugar | ⚠️ falta avançar |
| Super | Legião de Sombras (clones atacando) | animação + **onda** | ❌ |
| Ultra | Abismo (raio de sombra e puxa) | raio genérico, cinemática provisória | ⚠️ |

### p07 — cavalheiro pesado
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Tapa de luva (**sem projétil**) | **bola** | ❌ |
| S2 | Chute ascendente (anti-aéreo) | chute, 1 acerto | ✅ |
| S3 | Contra-golpe com calcanhar | contra-golpe (lê tudo) | ✅ |
| S4 | Chute giratório **baixo** | golpe alto (defende em pé) | ⚠️ falta ser baixo |
| Super | Estala, capangas entram, ele fecha com sequência pesada | capangas; a sequência dele não acerta | ⚠️ |
| Ultra | Herança (enxurrada de chutes e tapas) | sequência do vídeo, 4 golpes nos quadros da animação | ✅ |

### p08 — chicote
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Chicotada longa em linha reta | **bola** | ❌ |
| S2 | Giro ascendente (anti-aéreo) | 1 acerto | ✅ |
| S3 | Contra-golpe com joelhada | contra-golpe (lê golpe baixo) | ✅ |
| S4 | Salto-agulha (salta à frente e cai agachada) | no lugar | ⚠️ falta avançar |
| Super | Tempestade de Chicotes | animação + **onda** | ❌ |
| Ultra | Sinfonia Escarlate (chutes, tapas, chicotada final) | sequência do vídeo, 5 golpes nos quadros da animação | ✅ |

### p09 — bombeiro grappler
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Machado girando | **bola** | ❌ |
| S2 | Cotovelada ascendente | 1 acerto | ✅ |
| S3 | Escudo (aguenta e responde com cabeçada) | armadura de 1 golpe | ✅ |
| S4 | Investida de ombro | avança + armadura | ✅ |
| Agarrão | Carregar e jogar | agarrão mais forte e longo | ✅ |
| Super | Tempestade de Fogo (soca o chão, fogo) | animação + **onda** | ❌ |
| Ultra | Resgate Final (**agarra**, salta e arremessa) | **raio** | ❌ |

### p10 — bombeira ágil
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Machadinho arremessado | **bola** | ❌ |
| S2 | Jato de vapor (anti-aéreo) | 1 acerto, sem vapor | ⚠️ falta o efeito |
| S3 | Esquiva acrobática e chute | esquiva alta | ✅ |
| S4 | Voadora em parafuso que avança | avança e atravessa | ✅ |
| Extra | — | 2 air dashes | ✅ |
| Super | Cortina de Vapor (chutes acrobáticos) | animação + **onda** | ❌ |
| Ultra | Resgate Aéreo (combo aéreo) | sequência do vídeo, 5 golpes nos quadros da animação | ✅ |

### p11 — cirurgião
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Bisturi reto e rápido | **bola** | ❌ |
| S2 | Seringa arremessada no chão à frente, deixa veneno | **golpe corpo a corpo** que envenena | ❌ |
| S3 | Bloqueio de nervos (contra-golpe) | contra-golpe + veneno | ✅ |
| S4 | Passo cirúrgico (desliza e estoca) | avança | ✅ |
| Super | Cirurgia (5 golpes exatos + estocada) | animação + **onda** | ❌ |
| Ultra | Autópsia (chave no pulso, golpes, arremesso) | **raio** | ❌ |

### p12 — criatura feral
| | Ficha | Jogo hoje | |
|---|---|---|---|
| S1 | Investida de quatro apoios (**sem projétil**), custa vida | **bola** (custa vida) | ❌ |
| S2 | Salto de garras (anti-aéreo), custa vida | 1 acerto, custa vida | ✅ |
| S3 | Fúria: aguenta e responde | armadura + custa vida | ✅ |
| S4 | Contorção que avança de lado | no lugar | ⚠️ falta avançar |
| Extra | Força cresce com a vida perdida | fúria | ✅ |
| Super | Frenesi (garras, mordida, joelhada, cabeçada) | animação + **onda** | ❌ |
| Ultra | Último Suspiro (berserker total) | sequência do vídeo, 4 golpes nos quadros da animação | ✅ |

## Contagem

| | ✅ | ⚠️ | ❌ |
|---|---|---|---|
| S1 | 2 | 2 | 8 |
| S2 | 7 | 3 | 2 |
| S3 | 10 | 0 | 2 |
| S4 | 4 | 7 | 1 |
| Super | 0 | 2 | 10 |
| Ultra | 1 | 1 | 10 |

## O que precisa (quase tudo é código; os vídeos já existem)

As animações dos Supers, Ultras e especiais **já mostram** o golpe da ficha (vieram dos prompts
da ficha). O que falta é o jogo fazer o que a animação mostra:

1. **S1 por tipo:** projétil com visual próprio (kunai, shuriken, machado girando, bisturi,
   chicote, sombra rasteira, estouro no punho, palma) e S1 corpo a corpo no p07 e na p12.
2. **Golpes de vários acertos:** cada acerto onde a animação bate (rekka do p03, uppercut do
   p03, Supers de sequência).
3. **Super por tipo:** sequência corpo a corpo que avança (a maioria), onda multi-hit (p01),
   capangas (p07), soco no chão com fogo (p09), clones (p06).
4. **Ultra por tipo:** investida que vira sequência de golpes (a maioria), agarrão (p09, p11) e
   raio (p01, p06).
5. **Ajustes pequenos:** avanço nos S4 que andam (p01, p02, p04, p06, p08, p12), S4 baixo do p07,
   S4 do p03 no ar, S2 projétil do p05 e da p11, vapor da p10.
6. **Comandos:** decidir se cada personagem tem comandos próprios ou se fica o padrão de jogo de
   luta (todos iguais), mas com S3 e S4 em movimentos diferentes.

## Comandos por personagem (proposta, 14/10)

Decidido: cada personagem tem comandos próprios (o jogo vai para PC). Setas com o personagem
virado para a direita. `[←] →` = segura ← um instante e aperta →; `[↓] ↑` = segura ↓ e aperta ↑.
Super e Ultra continuam no botão SUPER (Ultra = ↓ + SUPER).

| # | S1 | S2 | S3 | S4 |
|---|---|---|---|---|
| p01 | Onda ↓↘→ | Punho ascendente →↓↘ | Aparar ↓↓ | Chute giratório ↓↙← |
| p02 | Palma ↓↘→ | Chute da garça [↓] ↑ | Contra-golpe ←↙↓ | Chute rodopiante [←] → |
| p03 | Onda curta ↓↘→ | Punho em sequência →↓↘ | Avanço em três ↓↙← (repetir até 3×) | Mergulho, no ar: ↓↘→ |
| p04 | Soco de impacto ↓↘→ | Cabeçada [←] → | Esquiva ↓↓ | Ombrada ←↙↓↘→ |
| p05 | Kunai ↓↘→ | Shuriken ↓↙← | Selo e mudança ↓↓ | Corte de mão ←↙↓↘→ |
| p06 | Sombra rastejante ↓↙← | Eco de sombra ↓↓ | Puxar →↘↓↙← | Passo-sombra ↓↘→ |
| p07 | Tapa de luva ↓↘→ | Chute ascendente →↓↘ | Contra-golpe ↓↙← | Chute giratório baixo ↓↓ |
| p08 | Chicote ←↙↓↘→ | Giro ascendente [↓] ↑ | Contra-golpe ↓↙← | Salto-agulha ↓↘→ |
| p09 | Machado →↘↓↙← | Cotovelada →↓↘ | Escudo ↓↓ | Ombrada [←] → |
| p10 | Machadinho ↓↙← | Jato de vapor →↓↘ | Esquiva acrobática ↓↓ | Voadora em parafuso ↓↘→ |
| p11 | Bisturi ↓↘→ | Seringa ↓↙← | Bloqueio de nervos →↘↓↙← | Passo cirúrgico [←] → |
| p12 | Investida [←] → | Salto de garras [↓] ↑ | Fúria ↓↓ | Contorção ↓↙← |

Ideia: quem é "de carga" (p02 garça, p04 boxe, p12 fera) segura para trás ou para baixo; o
grappler (p09) tem meias-luas longas; os irmãos (p01, p03) usam os comandos clássicos.

## Ultras refeitos (15/10)

Os Ultras do p02, p03, p04, p05, p07, p08, p10 e p12 deixaram de ser o raio. Agora cada um
segue o próprio vídeo (`tools/sprites/ultras.json` + `ultras.py`): a ativação toca o começo do
vídeo, a investida segura a pose até encostar e, se pega, a sequência roda inteira. Cada pancada
do vídeo é um golpe no jogo, no quadro em que ela acerta, com uma parte do dano, e o último
golpe lança. Não tem mais cinemática nem raio para esses oito. Errou ou foi defendido: recupera
como antes.

- p01 e p06 continuam com o raio (é o golpe deles).
- p09 e p11: o vídeo mostra o rival junto (agarrão), e ele ficaria colado na arte. Faltam
  vídeos novos sem o rival; até lá eles continuam com o raio.

## Golpes com várias pancadas (15/10)

Os golpes agora batem onde a animação bate (`tools/sprites/pancadas.json` → `mecanicas.py`):
cada pancada do desenho é um acerto, no quadro em que ela encosta; o dano do golpe se divide
entre elas, as do meio só seguram o rival até a próxima e a última aplica o efeito do golpe
(derrubar, lançar). O golpe mantém a vantagem que tinha com um acerto só.

| | Pancadas |
|---|---|
| S2 | p03 uppercut 3, p07 chutes 3, p08 chicotadas 2 |
| S3 | p03 rekka: as 3 investidas num comando só, 3 pancadas |
| S4 | p01 chutes giratórios 3, p02 2, p03 2, p05 2, p12 2 |
| Super corpo a corpo (sem onda) | p02 5, p03 3, p04 3, p05 3, p06 6, p08 4 (chicote, alcance longo), p10 4, p12 4 |

Avançam como na animação: S4 do p01, p02, p04, p06, p08 e p12, e os Supers corpo a corpo.
Continuam com onda: p01 (Onda), p09 (fogo pelo chão) e p11 (arremesso); o p07 chama os capangas.
