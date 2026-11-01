# Identidade dos 16: as cinco sugestões, simuladas uma a uma (01/11)

Pedido: aplicar as sugestões 1 a 5 de [perfis-16.md](perfis-16.md), simulando depois de cada uma;
no fim, rever todas as simulações, rodar as diferenças e manter só o que teve efeito.

**Como foi medido:**
- `BateriaPerfis` com a **escala congelada** da rodada de referência
  ([perfis/escala-base.csv](perfis/escala-base.csv)). Assim as distâncias de rodadas diferentes
  ficam comparáveis: se o elenco se espalha, elas crescem.
- Cada estado do jogo roda com a **mesma bateria final**: 4800 lutas 1x1 com a CPU no nível 8,
  mais outras 4800 com outras sementes para medir o ruído.
  - S0: antes de tudo.
  - S1 a S4: depois de cada passo.
  - S5: o estado final, só com o que ficou.
- **"Mudou de verdade"** quer dizer que a ficha de luta do lutador se moveu mais que 2× o ruído
  dele mesmo.
- Rodadas completas em [perfis/revisao/](perfis/revisao/) (S0–S5 e
  [comparacao.md](perfis/revisao/comparacao.md)).

```
python3 tools/balanco/revisao_perfis.py <pasta com S0..S5> docs/balanco/perfis/escala-base.csv
```

## Resultado

| Passo | O que fez | Lutadores que mudaram o jeito de lutar | Decisão |
|---|---|---|---|
| 1. Movimento por arquétipo | velocidades de andar, dash e backdash próprias para pressão, zoners, agarrador e defensivo | **3 de 16** (p08, p10, p11); pares parecidos 15 → 9 | **fica** |
| 2. Vantagem na defesa por personagem | L/M/H/2H na defesa por arquétipo | **0 de 16** | **removido** (revert) |
| 3. A CPU joga o núcleo com a ferramenta de cada um | estilos onda, contra, rekka, esquiva, ninja, acrobata | **2 de 16** (Ryūga, p05); núcleo 0,96 → 1,07 no jeito de lutar | **fica** |
| 4. Pulo próprio | pulo mais rápido (p10, p05, Necro) ou mais pesado (p09, Krüger) | **0 de 16** | **removido** (revert) |
| 5. S que não é projétil | já era golpe de perto no pacote; a ficha é que lia errado | — (só a medida) | correção da bateria |

## Antes e depois (S0 → S5)

| Medida | S0 (antes) | S5 (final) |
|---|---|---|
| Distância média entre pares: fixa / luta / total | 1,32 / 1,38 / 1,36 | 1,39 / 1,40 / 1,41 |
| Núcleo (Ryūga, Yin-lu, Kōga, p04, p05, p10): fixa / luta / total | 0,94 / 0,94 / 0,95 | 1,00 / 1,04 / 1,02 |
| Pares com total abaixo de 0,9 | 15 de 120 | 10 de 120 |
| Par mais parecido | Yin-lu e p04 (0,47) | Yin-lu e p04 (0,56) |
| Vitórias: desvio / mín / máx | 16,4 / 14,5 / 74,3 | 17,7 / 14,0 / 74,0 |

## Leitura

- **O que a CPU usa muda a luta; o que ela não usa, não muda.**
  - O movimento mudou quem luta de longe (p08 e p11 ficam mais longe) e quem pressiona (p10).
  - O estilo próprio mudou quem tem ferramenta de reação ou de neutro: o Ryūga com a Onda e o
    Aparar, o p05 com o teleporte.
  - A vantagem na defesa e o tempo do pulo não mudaram nada. A CPU pune só 8% do que defende
    (docs/balanco/chao.md) e não pula mais por pular mais rápido.
- **Os dois removidos podem valer contra gente.** Eles só não mudam a CPU. Voltar com eles é
  `git revert` dos reverts (a724f5c3 para a vantagem, bda19d78 para o pulo), de preferência
  junto com a CPU aprendendo a punir (proposta G de chao.md).
- **Balanço:** o Ryūga foi de 62% para 74% de vitórias com o estilo "onda". Junto com o p08 (72%),
  é o mais forte. A Natisuko continua em 14%. Vale um passo de balanço antes de liberar.
- **Yin-lu e p04 continuam o par mais parecido** (0,56). O estilo "contra" da Yin-lu e o
  "esquiva" do p04 mexeram pouco (0,12 e 0,20, perto do ruído).
- **Para conferir na tela:** a animação de andar não acompanha a velocidade. Quem anda mais rápido
  (p10, Kōga, p05) ou mais devagar (p06, p08, p11, p09) pode deslizar o pé.
