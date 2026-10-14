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

## A chave

```
 vaga 0  RYŪGA ─┐                                        ┌─ vaga 4
 vaga 1  ...   ─┴─ semifinal ─┐              ┌─ semifinal ─┴─ vaga 5
                              ├─── FINAL ────┤
 vaga 2  ...   ─┬─ semifinal ─┘              └─ semifinal ─┬─ vaga 6
 vaga 3  ...   ─┘                                        └─ vaga 7  KŌGA
```

- **Sorteio**: as duplas dos irmãos ficam nas pontas; as outras 6 são embaralhadas nas
  vagas 1 a 6 (a cada partida a chave muda).
- **Quartas → semifinal → final**: 3 lutas do jogador. Na final, sempre o outro irmão.
- **O rival sempre avança.** As lutas entre duas duplas do CPU têm o vencedor sorteado.
- Derrota do jogador repete a luta (o padrão das lutas da história). Com
  `"repetirSeDerrota": false`, a dupla do jogador sai da chave e as lutas seguintes do
  torneio são puladas.

Na tela da chave: a dupla do jogador tem borda **azul**, a do rival **vermelha**; quem
perdeu fica apagado e riscado; o caminho de quem avançou fica dourado; embaixo, a próxima
luta do jogador (ou os campeões).

## Passos novos no `episodio.json`

| Passo | O que faz |
|---|---|
| `{ "tipo": "escolha" }` | a tela ESCOLHA SEU CAMINHO |
| `{ "tipo": "sorteio" }` | sorteia a chave e mostra as duplas caindo nas vagas, uma a uma |
| `{ "tipo": "chave" }` | a chave com os resultados até aqui e a próxima luta |
| `{ "tipo": "luta", "torneio": true }` | luta contra o próximo adversário da chave; o rótulo é o nome da rodada (QUARTAS DE FINAL, SEMIFINAL, FINAL). No fim, a rodada inteira se resolve |
| `"dupla": "jogador", "cpu": "rival"` | numa luta comum: a dupla fixa do irmão escolhido contra a do outro (o treino do ep01) |
| `"irmao": "ryuga"` ou `"koga"` | em qualquer passo: só toca na campanha desse irmão (páginas diferentes para cada lado) |

Se o sorteio for pulado (PULAR), a chave é sorteada na primeira luta do torneio. Sem a
campanha (cenas tocadas sem a história), escolha, sorteio, chave, lutas do torneio e passos
com `"irmao"` são pulados.

## Os episódios hoje (rascunho)

| Episódio | Conteúdo |
|---|---|
| ep01 — Duas Cartas | escolha do irmão, páginas provisórias, treino jogador × rival |
| ep02 — O Sorteio | página do templo, **sorteio**, página do irmão escolhido, **quartas**, chave |
| ep03 — Semifinal | chave, página do irmão escolhido, **semifinal**, chave |
| ep04 — A Final | chave, os irmãos frente a frente, **final** contra o rival, chave com os campeões, "Próximo episódio: A Regra Escondida" |

As páginas do ep01 ao ep04 são provisórias (painéis montados pelo jogo, com a arte
provisória); viram páginas prontas pelo [motor de páginas](motor-paginas.md). Depois do
ep04 a temporada acaba e o jogo volta ao menu inicial (a regra escondida e o chefe final
ainda não existem).

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
gradle :story-core:test                                 # TorneioTest: sorteio, rodadas, temporada inteira
```

Na linha de comando sem gradle: `java -Dirmao=koga -Dsorteio=3 ... HistoriaPreview`.
