# Elenco

Índice dos personagens e das regras de animação. Cada ficha (`pNN.md`) tem o mesmo formato: quem é
e como luta, o que está no jogo hoje (gerado dos dados), combos, o que falta e os prompts de vídeo.

## Decisões fechadas

- **Duplas fixas no modo história:** p01+p02, p03+p04, p05+p06, p07+p08, p09+p10, p11+p12. No
  arcade o jogador monta a dupla que quiser. p13+p14 e p15+p16 são pares **propostos**.
- **Sem armas na mão.** Kunai, shuriken, machado, bisturi, seringa etc. só existem como projéteis
  ou poderes lançados. Exceção única: **p08 luta com o chicote na mão** (longo alcance).
- **Todos têm algum poder.** O tipo e a quantidade dependem do estilo de cada um.
- **Escola da família (p01, p03 e o pai):** mesma base, mesmos comandos, execução oposta. O p01
  puxa para o **poder** (Ryu, Takuma, Ken); o p03 para o **golpe** (Ryu, Robert, Kim); o pai (boss)
  é a junção dos dois. **A energia da família tem a mesma cor nos dois**: a diferença é a forma de
  usar, não a cor.
- **Cada um luta diferente:** golpe igual entre personagens não serve (proposta dos golpes novos de
  p02 a p12 em [golpes-refeitos.md](golpes-refeitos.md)).
- **Qualidade:** a do concept, sem readaptar traço ou proporção; animação de anime viva.
- **Personagens da história** (concepts em `art/concepts/`): Takatoshi Komyo aos 45 e aos 55 anos
  (o Campeão), Dafu, Ryūga e Kōga aos 8 anos ([ryuga_8](ryuga_8.md), [koga_8](koga_8.md), no
  tutorial) e o logo. O uso deles na história está em `docs/historia/`.

## Fichas

| Ficha | Quem | Estilo de luta | Molde |
|---|---|---|---|
| [p01](p01.md) | **Ryūga** (dupla: p02) | Escola da família, lado do poder: shoto técnico, calmo, contra-ataque | Equilibrado |
| [p02](p02.md) | **Yin-lu** (dupla: p01) | Kung fu ágil da garça: mão aberta, chutes altos, rasteiras, contra-golpe | Rushdown / contra-golpe |
| [p03](p03.md) | **Kōga** (dupla: p04) | Escola da família, lado do golpe: rushdown energético, movido pela vingança | Rushdown |
| [p04](p04.md) | p04 (dupla: p03) | Lutador de rua: jabs, ganchos, cabeçada, esquivas que cancelam em soco | Brawler |
| [p05](p05.md) | p05 (dupla: p06) | Ninjutsu: ágil, teleporte em fumaça, mixup, wall jump | Mixup aéreo |
| [p06](p06.md) | p06 (dupla: p05) | Sombra e ilusão: palmas, clones de sombra, puxar à distância | Zoner / trapper |
| [p07](p07.md) | p07 (dupla: p08) | Cavalheiro pesado (savate): chutes de sapato social, palmadas, contra-ataque | Brawler pesado |
| [p08](p08.md) | p08 (dupla: p07) | Chicote de longo alcance (a única com arma na mão) | Zoner |
| [p09](p09.md) | p09 (dupla: p10) | Grappler bombeiro: agarrões de "resgate", tanque | Grappler |
| [p10](p10.md) | p10 (dupla: p09) | Bombeira ágil: chutes, acrobacias, rushdown | Rushdown acrobático |
| [p11](p11.md) | p11 (dupla: p12) | Cirurgião sádico: trapper, veneno ao longo do tempo | Zoner / trapper |
| [p12](p12.md) | **Necro** (dupla: p11) | Criatura feral: imprevisível, mordidas, golpes que custam vida, fúria | Berserker |
| [p13](p13.md) | **Krüger Stahl** (proposta: p14) | Colosso de couraça: punhos esmagadores, ombradas, pressão de perto | Colosso de couraça |
| [p14](p14.md) | p14 (proposta: p13) | Malandro dos chutes: fintas, esquivas, rasteiras, muda o ritmo | Trickster de chutes |
| [p15](p15.md) | **Natisuko** (proposta: p16) | Karatê técnico: base firme, golpes retos, chutes precisos | Equilibrado técnico |
| [p16](p16.md) | **Bonnie Scarlet** (proposta: p15) | Palhaça marcial: rushdown caótico, acrobático, fingimentos | Rushdown caótico |

## Documentos de referência

- [video.md](video.md): **como fazer uma animação hoje** (imagens iniciais, H3, prompt, regras do
  dono, conversão, montagem e o que rodar depois).
- [poderes.md](poderes.md): **os prompts dos poderes, projéteis e objetos lançados** de cada um
  (vídeo em fundo preto ou verde, imagem para objeto) e a cor da energia de cada personagem.
- [mecanicas.md](mecanicas.md): vida, movimento, dano e a mecânica única de cada um.
- [comandos.md](comandos.md): os comandos dos especiais e dos golpes de comando e de corrida.
- [combos-assinatura.md](combos-assinatura.md): o jeito de combar de cada um e os combos de
  assinatura conferidos no motor.
- [golpes-refeitos.md](golpes-refeitos.md): a proposta dos golpes novos de p02 a p12.
- `regua/`: medidas de tamanho (régua) e trocas de quadros de p02 e p15.

## Manter as fichas em dia

A parte "No jogo hoje" de cada ficha sai dos dados do jogo. Depois de mexer num golpe ou numa
animação: `python3 tools/sprites/doc_personagem.py <id> --write` (ou sem `<id>`, todos).
