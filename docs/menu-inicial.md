# Menu inicial

O app abre no **menu inicial**: o logo de *SŌKON: BLOODLINES* (`historia/logo.jpg`) sobre
preto, com o brilho azul do dragão à esquerda, o vermelho do tigre à direita e fagulhas
subindo, e três opções em faixas de mangá:

| Opção | O que faz |
|---|---|
| **MODO HISTÓRIA** | O jogo de verdade. Toca a temporada desde o Prólogo (ep00: páginas 1–4, o treino no Dojo Shūshinkan, páginas 5–11, "Próximo episódio"), a escolha do irmão (Ryūga ou Kōga) e o torneio do ep01 ao ep09 ([historia/torneio.md](historia/torneio.md)). Sem os botões de teste (IA, DEBUG, VIDA, DUPLA, HISTÓRIA, TUTORIAL) e sem o DEBUG. No fim da temporada volta ao menu. |
| **ARCADE** | Torneio de 16 duplas com dupla oficial ou livre, o duelo 1x1 da dupla campeã e o Takatoshi (em breve): [historia/arcade.md](historia/arcade.md). |
| **MODO TESTE** | O que o app abria antes: a seleção de dupla e a luta de teste no cenário escolhido no painel da seleção (começa no Templo da Lua, `DEFAULT_STAGE`), com os botões de teste, o DEBUG (AJUSTAR TAMANHOS / AJUSTAR CAIXAS), o modo batalha (IA ON) etc. A seleção abre com a última dupla escolhida no teste. |

## Save e capítulos do modo história

O modo história grava sozinho (no aparelho, `SharedPreferences` "historia_v1"), no começo e no
fim de cada episódio:

- Na primeira vez, MODO HISTÓRIA começa direto no Prólogo.
- Depois disso, MODO HISTÓRIA abre a tela **MODO HISTÓRIA** com **CONTINUAR** (do começo
  do episódio onde o jogador parou, com o irmão escolhido e a chave do torneio como estavam
  ali) e os **capítulos**: os já terminados (CONCLUÍDO) e o seguinte (ATUAL) podem ser
  escolhidos; os outros aparecem **BLOQUEADOS** (o toque só faz o card tremer).
- Escolher um capítulo antigo toca a temporada a partir dele; os capítulos já liberados
  continuam liberados. Terminada a temporada, a tela mostra TEMPORADA COMPLETA no lugar do
  CONTINUAR.
- Sair no meio de um episódio (MENU INICIAL na pausa, ou fechar o app) volta ao começo dele.
- O save guarda a semente da chave e, para cada episódio, o irmão e as rodadas já
  disputadas; a chave é refeita igual ao carregar (`ProgressoHistoria`, `Campanha.restaurar`).
- **Novo sorteio**: escolher na tela de capítulos um capítulo até o "O Sorteio" (ep04),
  inclusive, troca a semente e sai outro torneio (`ProgressoHistoria.novoSorteio`). O
  CONTINUAR mantém a chave. Do próprio "O Sorteio", o desafio do porto (ep03) já foi
  jogado: a semente nova é uma que dá o mesmo adversário de lá, para ele continuar do lado
  do rival.
- O save tem versão (`ProgressoHistoria.VERSAO`). Quando um episódio novo entra no meio da
  temporada, os saves antigos precisam ser convertidos na leitura: a versão 1 é de antes do
  ep02 "O Parceiro", a 2, de antes do ep03 "A Chegada", e a 3, de antes do ep07 "Sob a
  Mesma Lua"; em todas, os episódios dali em
  diante estão uma posição antes (`inserirEpisodio`). Quem parou no começo do episódio que
  agora vem depois do novo joga o novo. Sem isso, o CONTINUAR caía no episódio errado com
  a campanha de outro (a emboscada sumia e as rodadas chegavam adiantadas).
- Os capítulos ficam em linhas de 5 cards.

Controle na tela: direcional move, A escolhe, B (ou voltar) volta ao menu inicial.

## Controles

- **Toque**: tocar numa opção escolhe. A opção pisca, a tela escurece e o modo começa.
- **Controle / teclado**: ▲▼ (ou ◀▶) move o cursor, **A**, ENTER ou START escolhe.
- A tecla **voltar** do Android no menu inicial fecha o app (é a tela raiz).

## Voltar ao menu

No **modo história**:

- nas páginas, o botão **◀ MENU** (canto de cima à esquerda, espelhando o PULAR);
- nas lutas e no treino, o botão do topo da luta vira **◀ MENU** (no teste ele é o ◀ SELEÇÃO);
- a tecla **voltar** (ou ESC, ou START no controle) em qualquer ponto.

Todos abrem a **pausa**: **CONTINUAR** ou **MENU INICIAL**, com o aviso de que o progresso do
episódio se perde (ainda não há save). Na pausa tudo para (página, luta, treino, conversa, cinemática).
Voltar de novo (ou B) continua. Com controle, B nas páginas continua pulando, como antes.

No **modo teste** a tecla voltar na luta também abre a pausa (sem o aviso), e dali dá para
voltar ao menu. Na seleção de dupla, voltar continua sendo o "voltar" da seleção; cancelar a
seleção vai para a luta de teste. A pausa não abre no meio de um ultra (Página Final e raio).

## Idiomas

Os textos do menu e da pausa passam pela mesma tradução da história
(`assets/historia/idiomas/<código>.json`, chave = o texto em português). A lista fica em
`MenuInicial.TEXTOS` e `MenuPausa.TEXTOS`; o teste `traducoesCobremOsEpisodiosEOMenu` falha se
algum texto do menu ou de qualquer episódio da temporada ficar sem tradução.

## Onde fica no código

| Arquivo | O que faz |
|---|---|
| `android/story-core/.../MenuInicial.java` | o menu: logo, opções, cursor, toque, animações |
| `android/story-core/.../MenuPausa.java` | a pausa (CONTINUAR / MENU INICIAL) |
| `android/story-core/.../MenuCapitulos.java` | CONTINUAR e os capítulos do modo história (trancados os não liberados) |
| `android/story-core/.../ProgressoHistoria.java` | o save do modo história (episódio atual, concluídos, campanha por episódio) |
| `android/story-core/.../BotaoMenu.java` | o desenho das opções (faixa inclinada, cursor, brilho) |
| `android/story-core/.../PaginaHistoria.java` | o botão ◀ MENU das páginas (`setBotaoMenu`, `menuAt`) |
| `android/app/.../GameView.java` | abre o menu no primeiro lançamento, `startStoryMode`, `startTestMode`, `openPause`, `openMainMenu` |
| `android/pc-preview/.../MenuPreview.java` | grava o menu e a pausa em PNG |

`MenuInicial` e `MenuPausa` são Java puro (como o resto do `story-core`): o mesmo código
desenha no celular e no PC.

## Revisar no PC

```bash
cd android
gradle :pc-preview:runMenu                # build/menu/menu_pt_*.png
gradle :pc-preview:runMenu -Pidioma=en    # o mesmo em inglês
gradle :story-core:test                   # MenuInicialTest e as traduções
```
