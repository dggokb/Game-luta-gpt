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

- **Sorteio**: as duplas dos irmãos ficam nas pontas (Ryūga na vaga 0, Kōga na vaga 7);
  as outras 6 são embaralhadas nas vagas 1 a 6 (a cada partida a chave muda).
- **Quartas → semifinal → final.** Na final, sempre o outro irmão.
- **O rival sempre avança.** As lutas entre duas duplas do CPU têm o vencedor sorteado.
- Derrota do jogador repete a luta (o padrão das lutas da história). Com
  `"repetirSeDerrota": false`, a dupla sai da chave e as lutas seguintes são puladas.

### A emboscada (fora da arena)

Depois que o jogador passa das quartas, a dupla que **perdeu a outra quarta do lado dele**
não aceita a derrota: acha que, derrotando o jogador, fica com a vaga dele na semifinal. Eles
emboscam o jogador à noite, longe da arena. Não é regra do torneio, mas os organizadores
(os magnatas) assistem e adoram o espetáculo; a chave não muda.

- É sempre uma dupla com golpes: só as duplas prontas ficam no lado do jogador.
- O rótulo da luta é **EMBOSCADA**; perder repete a luta, como as outras da história.
- **Cenário**: o **Caminho dos Penhascos** (`caminho_penhascos`): uma passarela de metal
  sobre um abismo da ilha, à noite, com pontes suspensas, ruínas e andaimes
  ([../../tools/stage/ilha-do-torneio.md](../../tools/stage/ilha-do-torneio.md)).

### O desafio no porto (ep03)

Antes do sorteio, uma dupla desafia o jogador no cais (rótulo **DESAFIO**, 2x2 no
`porto_secreto`). A história não diz, mas essa dupla é **do lado do rival na chave** que o
ep04 vai sortear: assim o jogador nunca a reencontra no caminho dele (quartas, emboscada,
semifinal) e o porto aumenta o número de duplas diferentes que ele enfrenta.

- `Campanha.adversarioPorto()`: refaz a chave da semente sem mostrá-la (o sorteio do ep04 dá a
  mesma) e escolhe, pela semente, uma das duplas com golpes (`pronta`) da metade do rival
  (`Torneio.prontasDoLadoDoRival`). Hoje, com p13 e p15 sem golpes no lado do rival, sobra uma:
  p05, p07, p09 ou p11, cada uma em ~1/4 das partidas. O save não muda: a semente refaz tudo.
- A luta tem `"porto": true` (o `"cpu"` do passo só vale sem campanha).
- As páginas do desafio dependem do adversário: `"adversario": "p05"` (ou uma lista) faz o
  passo tocar só contra essa dupla. As do Ryūga são fixas (chegada) e as do adversário vêm
  depois delas.

| Adversário no porto | Ryūga | Kōga |
|---|---|---|
| p05 + p06 | `pag5_ryuga_p05` (a dupla mascarada cerca os dois: "Hum.... parece que podemos tentar algo antes do torneio...") | provisória |
| p07 + p08 | `pag5_ryuga_p07` (no meio da multidão, a dama de cabelo rosa esbarra na Yin-lu e o velho de chapéu encara o Ryūga: "Parece que temos passatempo....") | provisória |
| p09, p11 (p13, p15 quando tiverem golpes) | provisória (`pag3_q*`) | provisória |

Quando chegar a página de um adversário: grave como `pagN_<irmao>_<dupla>.jpg`, ponha o passo
com `"irmao"` e `"adversario"` antes da luta e tire o id da lista `"adversario"` da página
provisória do mesmo irmão. O `TorneioTest.ep03TemAsPaginasDoAdversarioDoPorto` confere a ordem.

### A regra escondida

Depois da FINAL (2x2), a dupla campeã luta entre si, **1x1** (Ryūga × p02 ou Kōga × p04).
Quem vencer desafia o Campeão (o chefe final, ainda por fazer).

Lutas do jogador na temporada: o treino do ep01, o convite do ep02, o desafio no porto
(ep03), as quartas, a emboscada, a semifinal, a final e o duelo 1x1.

Na tela da chave: a dupla do jogador tem borda **azul**, a do rival **vermelha**; quem
perdeu fica apagado e riscado; o caminho de quem avançou fica dourado; embaixo, a próxima
luta do jogador (ou os campeões).

## A Ilha do Torneio (cenários)

O torneio é numa ilha de penhascos sobre o mar; cada rodada tem a sua arena, subindo a ilha
do porto até o templo do pico. Mapa, concepts (`art/concepts/cenarios/`) e prompts de cada
cenário: [../../tools/stage/ilha-do-torneio.md](../../tools/stage/ilha-do-torneio.md).

| Luta | Cenário |
|---|---|
| DESAFIO (ep03, A Chegada) | Porto Secreto (`porto_secreto`), **pronto** |
| QUARTAS DE FINAL (ep04) | Arena Costeira (`arena_costeira`), **pronto** |
| EMBOSCADA (ep05) | Caminho dos Penhascos (`caminho_penhascos`), **pronto** |
| SEMIFINAL (ep06) | Arena Imperial (`arena_imperial`), **pronto** |
| FINAL (2x2, ep07) | Arena Central (`arena_central`), **pronto** |
| DUELO FINAL (1x1, a "Final da Tag", ep08) | Arena VIP (`arena_vip`); por enquanto, a Arena Central |
| O Campeão, Takatoshi | Arena do Templo (`arena_templo`) |

O Porto Secreto, a Arena Costeira, o Caminho dos Penhascos, a Arena Imperial e a Arena
Central já estão no jogo (no ep03, nas quartas do ep04, na emboscada do ep05, na semifinal
do ep06 e na final do ep07, e no sorteio de cenários do Arcade). A Arena VIP e a Arena do
Templo ainda não existem: o duelo 1x1 fica na Arena Central, logo depois da final
(`"cenario": "arena_central"` no passo do ep08).

## Passos novos no `episodio.json`

| Passo | O que faz |
|---|---|
| `{ "tipo": "escolha" }` | a tela ESCOLHA SEU CAMINHO |
| `{ "tipo": "sorteio" }` | sorteia a chave e mostra as duplas caindo nas vagas, uma a uma |
| `{ "tipo": "chave" }` | a chave com os resultados até aqui e a próxima luta |
| `{ "tipo": "luta", "torneio": true }` | luta contra o próximo adversário da chave; o rótulo é o nome da rodada (QUARTAS DE FINAL, SEMIFINAL, FINAL). No fim, a rodada inteira se resolve |
| `{ "tipo": "luta", "emboscada": true }` | a emboscada fora da arena: o jogador contra a dupla que perdeu a outra quarta do lado dele (rótulo EMBOSCADA). Só toca entre as quartas e a semifinal |
| `{ "tipo": "luta", "duelo": true }` | o duelo 1x1 da regra escondida: o líder da dupla do jogador (o irmão escolhido, Ryūga ou Kōga) contra o parceiro, seja quem for que estava em campo ao vencer a final (rótulo DUELO FINAL; no Arcade é quem venceu a final, veja [arcade.md](arcade.md)). Só toca se a dupla do jogador for campeã |
| `"dupla": "jogador", "cpu": "rival"` | numa luta comum: a dupla fixa do irmão escolhido contra a do outro (com `"solo": true`, só os irmãos 1x1, como no treino do ep01) |
| `"irmao": "ryuga"` ou `"koga"` | em qualquer passo: só toca na campanha desse irmão (páginas diferentes para cada lado) |
| `{ "tipo": "luta", "porto": true }` | o desafio no porto: contra uma dupla do lado do rival na chave que ainda vai ser sorteada |
| `"adversario": "p05"` (ou lista) | em qualquer passo: só toca quando o adversário do porto é essa dupla (id do `torneio.json`) |

Se o sorteio for pulado (PULAR), a chave é sorteada na primeira luta do torneio. Sem a
campanha (cenas tocadas sem a história), escolha, sorteio, chave, lutas do torneio e passos
com `"irmao"`, `"emboscada"` ou `"duelo"` são pulados.

## Os episódios hoje (rascunho)

| Episódio | Conteúdo |
|---|---|
| ep01 — Duas Cartas | escolha do irmão, páginas prontas (começo e fim de cada irmão), treino jogador × rival |
| ep02 — O Parceiro | antes do torneio, cada irmão vai convencer o parceiro: 4 páginas prontas de cada irmão (Ryūga vai atrás da Yin-lu, Kōga vai atrás do Brucy), **luta 1x1 CONVITE** (Ryūga/P01 × Yin-lu/p02 ou Kōga/p03 × Brucy/p04; perdendo ou ganhando, a história segue; a do Ryūga é no Dojo da Yin-Lu, `dojo_yinlu`, e a do Kōga no Beco do Brucy, `beco_brucy`), o parceiro aceita (`pag5_ryuga` ou `pag5_koga`, página pronta de cada irmão) |
| ep03 — A Chegada | os irmãos chegam de barco à Ilha do Torneio, no Porto Secreto. **Ryūga: 4 páginas prontas** (`pag1_ryuga` a `pag4_ryuga`: o cais do continente e a Yin-lu desconfiada da ilha, "conhecemos um sim", a revelação do Kōga, a viagem e a ilha (legenda "Ilha do torneio: local desconhecido"), a chegada no meio da multidão, os dois em alerta). Kōga: página do porto e página dele com o Brucy (provisórias). Depois, o desafio no cais (veja **O desafio no porto** abaixo; para os dois, por enquanto, segue o rascunho): a dupla do p07 + p08 (gananciosos, "uma dupla a menos na chave seria ótimo para os nossos negócios") desafia o jogador no cais, **luta 2x2 DESAFIO** no `porto_secreto`, o mensageiro encerra a briga, página do irmão vendo o outro irmão chegar. Antes do sorteio: a chave ainda não existe |
| ep04 — O Sorteio | página do templo, **sorteio**, página do irmão escolhido, **quartas**, chave |
| ep05 — Fora da Arena | chave, a emboscada à noite, página do irmão, **emboscada**, os organizadores adoraram |
| ep06 — Semifinal | chave, página do irmão escolhido, **semifinal**, chave |
| ep07 — A Final | chave, os irmãos frente a frente, **final 2x2** contra o rival, chave com os campeões |
| ep08 — A Regra Escondida | o anúncio da regra, a conversa com o parceiro, **duelo 1x1**, "Próximo episódio: O Campeão" |

**Nível da CPU** (`NivelCpu`): começa no NÍVEL CPU da pausa (padrão 5) na primeira luta e
chega ao 8 na última, repartido pelas lutas da campanha do irmão escolhido. Hoje são 8 (treino
do ep01, convite, desafio, quartas, emboscada, semifinal, final e duelo): 5, 5, 5, 6, 6, 7, 7, 8. O
treino-tutorial do Prólogo não conta (não tem IA). O Arcade usa a mesma regra
([arcade.md](arcade.md)). A cada 2 derrotas seguidas a CPU desce 1 nível (nunca abaixo de 1); a
vitória zera a contagem e a CPU volta ao nível da luta (`Campanha.registrarResultado`).

As páginas do ep03 ao ep08 são provisórias (painéis montados pelo jogo, com a arte
provisória); viram páginas prontas pelo [motor de páginas](motor-paginas.md) (o ep02 já está todo em páginas prontas). Depois do
ep08 a temporada acaba e o jogo volta ao menu inicial (o chefe final, o Campeão, ainda não
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

A mesma chave, com 16 vagas, é a do modo Arcade ([arcade.md](arcade.md)).

A campanha vive enquanto a história toca: voltar ao menu (ou fechar o app) perde a escolha
e a chave. Ainda não há save.

## Revisar no PC

```bash
cd android
gradle :pc-preview:runHistoria                          # campanha do Ryūga
gradle :pc-preview:runHistoria -Pirmao=koga -Psorteio=3 # do Kōga, com outra chave
gradle :story-core:test                                 # TorneioTest: sorteio, rodadas, emboscada, temporada inteira
```

Na linha de comando sem gradle: `java -Dirmao=koga -Dsorteio=3 ... HistoriaPreview`.
