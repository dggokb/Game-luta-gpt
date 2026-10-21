# Comandos por personagem (15/10)

Cada personagem tem os próprios comandos de S1 a S4 (proposta aprovada em
`auditoria-golpes.md`). No jogo, a lista fica na pausa: **PAUSA → COMANDOS**, com ◀ ▶ para trocar
de personagem. Ela mostra os botões do que o jogador está usando (toque, teclado ou controle).

Setas com o personagem virado para a direita. Qualquer botão (L, M ou H) solta o especial; no S1
o botão escolhe a força da energia.

| # | S1 | S2 | S3 | S4 |
|---|---|---|---|---|
| p01 | Onda ↓↘→ | Punho ascendente →↓↘ | Aparar ↓↓ | Chute giratório ↓↙← |
| p02 | Palma ↓↘→ | Chute da garça [↓] ↑ | Contra-golpe ←↙↓ | Chute rodopiante [←] → |
| p03 | Onda curta ↓↘→ | Punho em sequência →↓↘ | Avanço em três ↓↙← | Mergulho, no ar: ↓↘→ |
| p04 | Soco de impacto ↓↘→ | Cabeçada [←] → | Esquiva ↓↓ | Ombrada ←↙↓↘→ |
| p05 | Kunai ↓↘→ | Shuriken ↓↙← | Selo e mudança ↓↓ | Corte de mão ←↙↓↘→ |
| p06 | Sombra rastejante ↓↙← | Eco de sombra ↓↓ | Puxar →↘↓↙← | Passo-sombra ↓↘→ |
| p07 | Tapa de luva ↓↘→ | Chute ascendente →↓↘ | Contra-golpe ↓↙← | Chute giratório baixo ↓↓ |
| p08 | Chicote ←↙↓↘→ | Giro ascendente [↓] ↑ | Contra-golpe ↓↙← | Salto-agulha ↓↘→ |
| p09 | Machado →↘↓↙← | Cotovelada →↓↘ | Escudo ↓↓ | Ombrada [←] → |
| p10 | Machadinho ↓↙← | Jato de vapor →↓↘ | Esquiva acrobática ↓↓ | Voadora em parafuso ↓↘→ |
| p11 | Bisturi ↓↘→ | Seringa ↓↙← | Bloqueio de nervos →↘↓↙← | Passo cirúrgico [←] → |
| p12 | Investida [←] → | Salto de garras [↓] ↑ | Fúria ↓↓ | Contorção ↓↙← |
| p15 | Seiken Hadō ↓↘→ | Shōryū de caratê →↓↘ | Rajada de punhos ↓↓ | Voadora e machado ↓↙← |

Super: botão SUPER. Ultra: ↓ + SUPER. Agarrão: botão de agarrar (ou L + M juntos).

## Como o jogo lê

- **Diagonal do meio é opcional:** ↓↘→ vale como ↓→, e a meia-lua ←↙↓↘→ vale como ←↓→. A
  diagonal no fim conta (→↓↘).
- **Tempo:** o comando inteiro em 15 quadros (¼ s), mais 6 para cada passo além de dois (a
  meia-lua tem 21); o botão até 15 quadros depois da última direção.
- **Segurar** (`[←] →`, `[↓] ↑`): segure por meio segundo (30 quadros) e aperte a outra direção
  com o botão. A carga dura 10 quadros depois de soltar. Diagonais contam (↙ carrega trás e baixo).
  Com a carga para baixo pronta, o pulo espera 3 quadros: o botão que chega junto vira o
  especial em vez do pulo.
- **↓↓:** solte (ou mude de direção) entre os dois ↓. Para emendar golpes agachados (2L → 2M),
  mantenha o ↓ segurado: soltar e apertar de novo dentro de ¼ s vira o especial de ↓↓.
- **Quando dois comandos batem:** ganha o de mais passos (um movimento ganha de um segurar);
  empatado, o que terminou por último; e então S2 a S4 antes do S1.
- **No chão / no ar:** os especiais saem no chão, menos o Mergulho do p03, que só sai no ar
  (parado na preparação, desce na diagonal até o chão e sobra pouca recuperação no pouso). O S1
  sai nos dois.

## Onde mudar

`tools/sprites/mecanicas.json`, bloco `comandos` de cada personagem (nome e comando em setas,
`"[←] →"` para segurar, `"noAr": true` para só no ar). Depois:

    python3 tools/sprites/mecanicas.py --write
    python3 tools/sprites/build_characters.py --write

Os tempos (carga, janela, espera do pulo, pouso do mergulho) ficam em `CombatConfig`.
