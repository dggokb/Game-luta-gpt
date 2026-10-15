# Menu inicial

O app abre no **menu inicial**: o logo de *SŌKON: BLOODLINES* (`historia/logo.jpg`) sobre
preto, com o brilho azul do dragão à esquerda, o vermelho do tigre à direita e fagulhas
subindo, e duas opções em faixas de mangá:

| Opção | O que faz |
|---|---|
| **MODO HISTÓRIA** | O jogo de verdade. Toca a temporada desde o Prólogo (ep00: páginas 1–4, o treino no Dojo Shūshinkan, páginas 5–11, "Próximo episódio"), a escolha do irmão (Ryūga ou Kōga) e o torneio do ep01 ao ep06 ([historia/torneio.md](historia/torneio.md)). Sem os botões de teste (IA, DEBUG, VIDA, DUPLA, HISTÓRIA, TUTORIAL) e sem o DEBUG. No fim da temporada volta ao menu. |
| **MODO TESTE** | O que o app abria antes: a seleção de dupla e a luta de teste no Templo da Lua (`DEFAULT_STAGE`), com os botões de teste, o DEBUG (AJUSTAR TAMANHOS / AJUSTAR CAIXAS), o modo batalha (IA ON) etc. A seleção abre com a última dupla escolhida no teste. |

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
episódio se perde (ainda não há save). Na pausa tudo para (página, luta, treino, conversa).
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
