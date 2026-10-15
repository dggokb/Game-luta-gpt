# Torneio do modo história: escolha do irmão, sorteio e chave

A partir do Episódio 1 o modo história segue a estrutura do torneio da "História Base":
chave eliminatória com **8 duplas fixas**, os irmãos em **lados opostos** e só se encontram
na final.

## A escolha do irmão

Depois do Prólogo (o "Próximo episódio" do ep00), antes do título do Episódio 1, aparece
**ESCOLHA SEU CAMINHO**: **Ryūga** (p01, o Dragão, azul, à esquerda) ou **Kōga** (p03, o
Tigre, vermelho, à direita). Só esses dois: a história é deles. Toque no lado escolhe; com
controle, ◀▶ e A. A escolha não se pula (o PULAR some nessa tela).

Daí em diante o jogador luta sempre com a **dupla fixa** do irmão escolhido:
Ryūga + p02 ou Kōga + p04. O treino do Prólogo vem antes da escolha e continua sendo com o
Ryūga criança.

## As duplas fixas

Ficam em `android/app/src/main/assets/historia/torneio/torneio.json`:

| Dupla | Lutadores | Observação |
|---|---|---|
| ryuga | p01 (player_base) + p02 | fixa na vaga 0 (ponta esquerda) |
| koga | p03 + p04 | fixa na vaga 7 (ponta direita) |
| p05 | p05 + p06 | sorteada |
| p07 | p07 + p08 | sorteada |
| p09 | p09 + p10 | sorteada |
| p11 | p11 + p12 | sorteada |
| p13 | p13 + p14 | sorteada, `"pronta": false` |
| p15 | p15 + p16 | sorteada, `"pronta": false` |

`"pronta": false` = dupla que ainda não tem golpes (p13–p16). O sorteio põe essas duplas no
**lado do rival**, então elas só aparecem nas lutas CPU × CPU da chave e nunca cruzam com o
jogador. Quando ganharem golpes, é só tirar o `"pronta": false`.

## A chave (com repescagem)

Cada lado da chave (o **Lado do Dragão** à esquerda, o **Lado do Tigre** à direita) tem 4
duplas e o mesmo formato:

```
 quartas         rodada 2                       rodada 3            rodada 4
 A × B  ─┬─ vencedores ─▶ SEMIFINAL  ─┐
 C × D  ─┴─ perdedores ─▶ REPESCAGEM ─┴─▶ FINAL DA CHAVE ─▶  FINAL (o campeão de cada lado)
```

- **Sorteio**: as duplas dos irmãos ficam nas pontas (Ryūga na vaga 0, Kōga na vaga 7);
  as outras 6 são embaralhadas nas vagas 1 a 6 (a cada partida a chave muda).
- **Repescagem**: os dois perdedores das quartas de cada lado lutam entre si. O vencedor
  enfrenta o vencedor da semifinal na **final da chave**. Quem ganha a final da chave vai
  para a **FINAL** contra o campeão do outro lado (sempre o outro irmão).
- **É justo**: os dois caminhos têm 3 lutas até a final. Quem vence faz quartas, semifinal
  e final da chave; quem perde faz quartas, repescagem e final da chave.
- Por isso a **derrota nas quartas não repete a luta**: manda para a repescagem. Nas outras
  rodadas a derrota repete (o padrão das lutas da história). Com
  `"repetirSeDerrota": false`, a dupla sai da chave e as lutas seguintes são puladas.
- **O rival sempre vence** as lutas dele até a final. As lutas entre duas duplas do CPU têm
  o vencedor sorteado.
- Depois da FINAL (2x2) vem a **regra escondida**: a dupla campeã luta entre si, **1x1**
  (Ryūga × p02 ou Kōga × p04). Quem vencer desafia o Campeão (o chefe final, ainda por fazer).

Lutas do jogador na temporada: o treino do ep01, 4 do torneio (quartas, semifinal ou
repescagem, final da chave, final) e o duelo 1x1.

Na tela da chave cada confronto é uma caixa (quartas, semifinal, repescagem, final da chave,
final e, em cima, os campeões). A dupla do jogador tem borda **azul**, a do rival
**vermelha**; quem venceu ganha a faixa dourada, quem perdeu fica apagado e riscado; as
linhas douradas levam os vencedores, as vermelhas levam os perdedores das quartas à
repescagem. Embaixo, a próxima luta do jogador (ou os campeões).

## Passos novos no `episodio.json`

| Passo | O que faz |
|---|---|
| `{ "tipo": "escolha" }` | a tela ESCOLHA SEU CAMINHO |
| `{ "tipo": "sorteio" }` | sorteia a chave e mostra as duplas caindo nas vagas, uma a uma |
| `{ "tipo": "chave" }` | a chave com os resultados até aqui e a próxima luta |
| `{ "tipo": "luta", "torneio": true }` | luta contra o próximo adversário da chave; o rótulo é o nome da rodada (QUARTAS DE FINAL, SEMIFINAL, REPESCAGEM, FINAL DA CHAVE, FINAL). No fim, a rodada inteira se resolve |
| `{ "tipo": "luta", "duelo": true }` | o duelo 1x1 da regra escondida: o capitão da dupla do jogador contra o próprio parceiro (rótulo DUELO FINAL). Só toca se a dupla do jogador for campeã |
| `"caminho": "direto"` ou `"repescagem"` | em qualquer passo: só toca para quem venceu as quartas ou para quem perdeu e foi para a repescagem |
| `"dupla": "jogador", "cpu": "rival"` | numa luta comum: a dupla fixa do irmão escolhido contra a do outro (o treino do ep01) |
| `"irmao": "ryuga"` ou `"koga"` | em qualquer passo: só toca na campanha desse irmão (páginas diferentes para cada lado) |

Se o sorteio for pulado (PULAR), a chave é sorteada na primeira luta do torneio. Sem a
campanha (cenas tocadas sem a história), escolha, sorteio, chave, lutas do torneio e passos
com `"irmao"`, `"caminho"` ou `"duelo"` são pulados.

## Os episódios hoje (rascunho)

| Episódio | Conteúdo |
|---|---|
| ep01 — Duas Cartas | escolha do irmão, páginas provisórias, treino jogador × rival |
| ep02 — O Sorteio | página do templo, **sorteio**, página do irmão escolhido, **quartas**, chave |
| ep03 — Segunda Rodada | chave, página do caminho (semifinal ou repescagem) e do irmão, **semifinal ou repescagem**, chave |
| ep04 — Final da Chave | chave, página, **final da chave**, chave |
| ep05 — A Final | chave, os irmãos frente a frente, **final 2x2** contra o rival, chave com os campeões |
| ep06 — A Regra Escondida | o anúncio da regra, a conversa com o parceiro, **duelo 1x1**, "Próximo episódio: O Campeão" |

As páginas do ep01 ao ep04 são provisórias (painéis montados pelo jogo, com a arte
provisória); viram páginas prontas pelo [motor de páginas](motor-paginas.md). Depois do
ep06 a temporada acaba e o jogo volta ao menu inicial (o chefe final, o Campeão, ainda não
existe).

## Retratos

`historia/torneio/rostos/pNN.jpg` (160×160), recortados dos concepts por
`python3 tools/historia/rostos_torneio.py` (os recortes de p01–p12 são os do HUD; p13–p16
foram marcados à mão). Rode de novo quando um concept mudar.

## Código

| Arquivo | O que faz |
|---|---|
| `story-core/.../Torneio.java` | sorteio, rodadas, rival sempre avança, CPU × CPU sorteado |
| `story-core/.../Campanha.java` | irmão escolhido, a chave, resolve as duplas de cada luta |
| `story-core/.../EscolhaIrmao.java` | a tela ESCOLHA SEU CAMINHO |
| `story-core/.../ChaveTorneio.java` | o desenho do sorteio e da chave |
| `story-core/.../PaginaHistoria.java` | toca os passos novos (`setCampanha`, `lados`, `rotulo`) |
| `app/.../GameView.java` | cria a campanha no começo da história e monta as duplas de cada luta |

A campanha vive enquanto a história toca: voltar ao menu (ou fechar o app) perde a escolha
e a chave. Ainda não há save.

## Revisar no PC

```bash
cd android
gradle :pc-preview:runHistoria                          # campanha do Ryūga
gradle :pc-preview:runHistoria -Pirmao=koga -Psorteio=3 # do Kōga, com outra chave
gradle :pc-preview:runHistoria -Prepescagem=true        # perde as quartas e segue pela repescagem
gradle :story-core:test                                 # TorneioTest: sorteio, rodadas, repescagem, temporada inteira
```

Na linha de comando sem gradle: `java -Dirmao=koga -Dsorteio=3 ... HistoriaPreview`.
