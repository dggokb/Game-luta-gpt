# Medida no nosso jogo (12/11): quanto a luz de hoje estoura

Não é pesquisa de fonte externa: é medida feita nesta sessão, no jogo, para o relatório ter um
número nosso ao lado dos princípios. Vale como dado de primeira mão.

## Como foi medido

- Gravação em OpenGL no PC, 1080p, gráficos ALTO, Ryūga (esquerda) e Kōga (direita) parados, 8
  quadros por caso: `gradle :pc:gravarLuta -Pgl=1 -Pcenario=<pasta> [-Pluz=off]`.
- "Luz desligada" (`-Pluz=off`, o botão LUZ OFF do Modo Teste) tira o motor 4 (o volume pelo
  normal map contra a luz geral do cenário), a luz do impacto e os pontos de luz. **A tinta do
  cenário (`luzLutadores`: cor, força, brilho, pés) e a contraluz continuam nos dois casos.**
- Medido numa caixa fixa em volta de cada lutador (a caixa inclui o fundo atrás dele, igual nos
  dois casos; o corpo ocupa perto de um terço dela). Luma = 0,299 R + 0,587 G + 0,114 B, de 0 a 255.
- "Canal ≥ 250": parte da caixa em que pelo menos um canal de cor bateu no teto (cortou).

## Números

| Cenário | Fundo (luma mediana / p90) | Lutador | Luz | Luma p90 | Luma p99 | Luma ≥ 235 | Canal ≥ 250 |
|---|---|---|---|---|---|---|---|
| Templo da Lua (noite) | 55 / 139 | Ryūga | off | 164 | 230 | 0,9% | 1,4% |
| | | Ryūga | on | 211 | 255 | 6,5% | 11,8% |
| | | Kōga | off | 136 | 194 | 0,2% | 0,6% |
| | | Kōga | on | 141 | 240 | 1,2% | 3,1% |
| Dojo Liánwǔguǎn (dia) | 108 / 170 | Ryūga | off | 201 | 254 | 4,2% | 1,7% |
| | | Ryūga | on | 229 | 255 | 9,5% | 10,5% |
| | | Kōga | off | 166 | 217 | 0,5% | 0,6% |
| | | Kōga | on | 166 | 236 | 1,0% | 1,7% |
| Beco do Brucy (noite, neon, chuva) | 36 / 102 | Ryūga | off | 146 | 207 | 0,4% | 1,2% |
| | | Ryūga | on | 175 | 234 | 1,0% | 5,9% |
| | | Kōga | off | 113 | 167 | 0,1% | 0,4% |
| | | Kōga | on | 118 | 214 | 0,5% | 1,7% |

## O que os números dizem

1. **Ligar a luz multiplica por 5 a 8 a área que bate no teto** no Ryūga (roupa branca): no
   Templo, de 1,4% para 11,8% da caixa, ou seja, perto de um terço do corpo cortado em branco. É a
   "luz muito estourada" do dono, medida.
2. **O lutador já chega claro demais antes de qualquer luz.** No Templo da Lua (noite) o fundo
   tem luma mediana 55; o Ryūga com a luz desligada já tem p90 de 164 e p99 de 230. No Beco
   (mediana do fundo 36), p90 de 146. A arte dos lutadores é pintada como "dia normal" e a tinta
   do cenário quase não a escurece: o `brilho` dos 12 cenários vai de 0,86 a 1,03 e a `forca` da
   cor de 0,1 a 0,35, inclusive nos de noite (Templo 0,90; Beco 0,86; Terraço 0,92).
3. **A luz entra por cima disso, somando, e o shader corta em 1.** Hoje a conta é: cor da arte ×
   matriz da tinta, × degradê dos pés, + contraluz (somada), + volume (o lado aceso soma
   `cor × força × (0,10 + 0,90 × arte)`), + luz local (somada), com `clamp(…, 0, 1)` em cada passo.
   Não há teto antes do corte, nem a arte "reserva" margem: a roupa branca (perto de 1,0) só pode
   cortar.
4. **O que muda entre desligado e ligado é quase todo a luz geral** (o volume do motor 4): o Kōga,
   longe de qualquer ponto de luz, clareia junto (já anotado no docs em 12/11).

## O que cada cenário declara hoje (para a proposta)

| Cenário | `luzLutadores` cor / força / brilho | pés | contraluz cor / força / direção | sombra | reflexo | pontos |
|---|---|---|---|---|---|---|
| arena_central | #FFA090 / 0,20 / 0,97 | #E8D0C8 | #FF9A50 / 0,9 / [-1,-1] | 7 focos, alfa 0,72 | — | 0 |
| arena_coberta | #FFD49A / 0,26 / 1,00 | #E8D0B0 | #FFC070 / 0,9 / [0,-1] | alfa 0,55 | 0,22 | 0 |
| arena_costeira | #A8B4E6 / 0,25 / 0,94 | #D4DAEA | #BFE0FF / 0,75 / [1,-1] | padrão | 0,18 | 0 |
| arena_imperial | #FFA070 / 0,22 / 0,98 | #E8D0C0 | #FF9A40 / 0,9 / [-1,-1] | alfa 0,45 | 0,25 | 6 |
| arena_pintura | #FF9A70 / 0,34 / 0,95 | #D8B8A8 | #FFB060 / 1,0 / [-1,-1] | alfa 0,62 | — | 0 |
| beco_brucy | #9CA8D8 / 0,35 / 0,86 | #B8B8D0 | #60A8FF / 0,9 / [1,-1] | padrão | 0,35 | 3 |
| caminho_penhascos | #FFE4DC / 0,10 / 1,00 | #E8E0DC | #FFF0D8 / 0,6 / [-1,-1] | padrão | 0,08 | 4 |
| dojo_shinshinkan | #FFC890 / 0,25 / 0,95 | #E0D0C0 | #FFC070 / 0,75 / [0,-1] | padrão | 0,32 | 2 |
| dojo_yinlu | #FFF0D4 / 0,10 / 1,03 | #E8E4DC | #FFE8C0 / 0,6 / [1,-1] | alfa 0,50 | — | 0 |
| porto_secreto | #FFD8A8 / 0,20 / 0,93 | #E0D0C4 | #FFB060 / 0,85 / [-1,-1] | padrão | 0,28 | 1 |
| templo_lua | #C8A8F0 / 0,30 / 0,90 | #C8C0DC | #D0B8FF / 0,8 / [1,-1] | padrão | 0,22 | 1 |
| terraco_lua | #A8C4FF / 0,30 / 0,92 | #C0C8E0 | #C8DCFF / 0,85 / [0,-1] | alfa 0,40 | 0,30 | 6 |

## Limites da medida

- Só no PC (o desenho em OpenGL é o mesmo do celular, mas a tela do aparelho não foi vista).
- Só parados, só Ryūga e Kōga, 8 quadros por caso.
- A caixa inclui o fundo: a porcentagem sobre o corpo é perto de três vezes a da caixa (conta
  aproximada, não medida com máscara).
