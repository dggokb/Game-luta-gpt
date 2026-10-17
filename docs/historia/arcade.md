# Modo Arcade

**ARCADE** no menu inicial: um torneio de **16 duplas** com a mesma chave, as mesmas lutas e o
mesmo final do modo história, mas sem as páginas da história.

```
escolha da dupla → sorteio → OITAVAS → QUARTAS → SEMIFINAL → FINAL (2x2)
                 → a regra escondida: DUELO FINAL (1x1) → TAKATOSHI (em breve) → menu inicial
```

## A dupla

A primeira tela (`MenuArcade`) mostra as **duplas oficiais** que já lutam (as de
`historia/torneio/torneio.json` sem `"pronta": false`: hoje Ryūga · Yin-lu, Kōga · Brucy,
p05 · p06, p07 · p08, p09 · p10 e p11 · p12) e o card **DUPLA LIVRE**.

- **Dupla oficial**: o Arcade começa na hora.
- **Dupla livre**: abre a seleção de personagens só com o lado do jogador (`CharacterSelect.openSolo`:
  "1/2 ESCOLHA SEU LUTADOR", "2/2 ESCOLHA SEU PARCEIRO"); o lado do CPU mostra "SORTEIO DA CHAVE".
  Voltar da seleção volta para a escolha da dupla.

Toque escolhe; controle: direcional move, A escolhe, B volta ao menu inicial.

## A chave

`Arcade.chave` monta as 16 vagas: a dupla do jogador na **vaga 0**, as duplas oficiais que
sobraram e, completando as 15, **duplas livres do CPU**. São 12 lutadores para 32 lugares, então
os lutadores se repetem entre as duplas, mas por igual (cada um em 2 ou 3 duplas): cada dupla
livre do CPU junta os lutadores que menos apareceram até ali. Nenhuma dupla aparece duas vezes
(nem invertida) e ninguém faz dupla consigo mesmo. As 15 duplas são sorteadas nas vagas 1 a 15.

É a mesma `Torneio` da história com 16 vagas (`Torneio.arcade`): quatro rodadas (`OITAVAS DE
FINAL`, `QUARTAS DE FINAL`, `SEMIFINAL`, `FINAL`), sem rival fixo (entre duas duplas do CPU o
vencedor é sorteado) e sem emboscada. A tela da chave (`ChaveTorneio`) usa cards menores para
caber 8 duplas de cada lado (`ChaveTorneio.DEZESSEIS`); o sorteio mostra as 15 duplas caindo
mais rápido que na história.

## As lutas

O Arcade é tocado como as cenas da história (`historia/arcade/arcade.json`, um episódio com
`sorteio`, `luta` + `"torneio": true`, `chave`, `luta` + `"duelo": true` e cartões `titulo`):

- **Cada luta tem 99 segundos**; no TIME OVER vence a dupla com mais vida (regra em
  `docs/combat-engine.md`). **Empate conta como derrota.**
- **Perder repete a luta** (o "continuar" do Arcade). A pausa (◀ MENU ou voltar) tem MENU INICIAL,
  com o aviso "Voltar ao menu encerra o Arcade."
- **A CPU sobe de nível a cada luta** (`NivelCpu`, o mesmo do modo história): começa no NÍVEL
  CPU da pausa (padrão 5) e chega ao 8 na última luta, repartido pelas lutas do Arcade (hoje 5:
  as 4 rodadas e o duelo → 5, 5, 6, 7, 8). Quando o Takatoshi entrar, ele vira a última luta e
  a subida se reparte sozinha. Mudar o nível na pausa muda a base na hora. A cada 2 derrotas
  seguidas a CPU desce 1 nível até o jogador vencer.
- **A regra escondida**: depois da final, a dupla campeã luta entre si, **1x1**: o jogador fica
  com **quem estava em campo quando a final foi vencida** e a CPU com o outro (rótulo DUELO
  FINAL). A campanha guarda isso a cada vitória (`Campanha.venceuCom`, chamado pelo jogo
  antes de `resultadoLuta`); sem registro (save antigo), vale o titular. Isso é só do Arcade: no
  modo história o duelo é sempre do líder (Ryūga ou Kōga).
- **O Campeão, Takatoshi**, ainda não tem personagem: por enquanto o Arcade termina no cartão
  "TAKATOSHI — Em breve: a luta contra o Takatoshi." e volta ao menu inicial. Quando ele existir,
  é só trocar o último passo do `arcade.json` por uma luta 1x1 contra ele.

O Arcade não tem save. Os textos passam pela tradução da história (`historia/idiomas`).

## Prévia no PC

`gradle :pc-preview:runMenu` grava também `arcade_<idioma>_dupla.png`, `_titulo`, `_sorteio`,
`_chave` (depois de duas rodadas), `_campeao` e `_takatoshi` em `build/menu`.

## Código

| Classe | O que faz |
|---|---|
| `historia/Arcade` | duplas oficiais, dupla livre e a chave de 16 |
| `historia/NivelCpu` | o nível da CPU de cada luta (história e Arcade) |
| `historia/MenuArcade` | a tela da escolha da dupla |
| `historia/Torneio` | a chave com 8 (história) ou 16 vagas (`Torneio.arcade`) |
| `historia/ChaveTorneio` | o desenho da chave, com o layout de 8 ou de 16 |
| `historia/Campanha.arcade` | a campanha só com a chave (o duelo usa a dupla do jogador da chave) |
| `CharacterSelect.openSolo` | a seleção só da dupla do jogador |
| `GameView` | `openArcadeMenu`, `startArcade`, `arcadeLevel` |
