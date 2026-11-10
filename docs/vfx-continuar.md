# VFX: onde paramos e como continuar

Roteiro para continuar o trabalho de efeitos visuais em outra sessão (a que tem o Codex e a rede
liberada). Escrito em 09/11, na `game-luta-sprite-fable`.

## Antes de mexer em VFX: as skills

Desde 10/11 o repositório tem duas skills em `.claude/skills/`, carregadas pelo Claude Code:

- **`vfx-luta`** (nossa): o que o dono do jogo já aprovou e reprovou em VFX, os erros que já
  aconteceram, as ferramentas e como gravar e conferir. É o resumo deste documento em forma de
  regra; quando uma decisão nova sair, ela entra lá também.
- **`vfx-2d`** (de terceiros, MIT, copiada sem alteração; ver `ORIGEM.md` na pasta): guia geral
  de VFX 2D, com durações por efeito e os três jeitos de fazer rastro.

**Onde o rastro parou (10/11, fim do dia):** na `game-luta-sprite-fable` está o rastro
procedural com a textura de riscos brancos grossos (`arco_textura_03.png`). O dono achou o
movimento bom e o desenho ainda ruim, e mandou uma referência de véu branco meio transparente com
riscos finos. A troca para esse véu (textura `arco_textura_04.png`, fita desenhada numa malha
só) foi interrompida antes de ser vista no jogo e está só na branch `ccr-97c05bd0-68tbpv`
(commit "Em andamento: rastro do golpe como véu de ar..."). Ele parou para procurar ferramenta
melhor; a ideia em aberto é ele gerar o rastro em vídeo, como fez com os efeitos de dano, e o
jogo encaixar no caminho que já detecta.

**Teste da receita do relatório (10/11, noite; branch `claude/rastro-erosao`, esperando o dono
aprovar no vídeo):** a fita deixou de ser a textura única esticada e virou a receita do
`reports/VFX de luta estilo anime.md`. Ver "Rodada de 10/11 (noite): o rastro pela receita" abaixo.

## Rodada de 10/11 (noite): o rastro pela receita do relatório

Pedido: um teste só do rastro (RastroDoGolpe + EfeitosVideo.fita) pela receita, com a base
escura da Arc System Works, o rastro na frente a 75% e o tempo procedural e o caminho como
estavam. Feito, numa branch da sessão (não na `game-luta-sprite-fable`):

- **Textura** (`tools/sprites/rastro_forma.py` -> `assets/efeitos/rastro_forma`): cinza, em três
  canais. R é a forma, o véu com riscos finos do Codex (`art/vfx/arco_textura_04.png`, o desenho
  vem dele, não do código); G é o ruído de valor que repete no comprimento, esticado no sentido
  do golpe e com o histograma igualado; B é a área da base escura (a forma alargada e borrada).
- **A conta** (`ErosaoDaFita`, Java puro): forma vezes ruído contra um limiar que sobe com o tempo
  e no fundo (erosão: o fundo some primeiro, em rasgos irregulares); uma faixa clara fina na frente
  da erosão; a frente da fita mais clara (a borda que corta); rampa em três faixas chapadas
  (sombra, a cor do membro, branco) a partir do `caminhoCor`, com saturação mínima de 0,3 porque
  a cor medida quase sempre vem cinza-clara; a base escura (o tom da cor, bem escuro, 35%) por
  baixo, apagando junto com o corte. A forma tem o comprimento do caminho e desliza com a frente
  (a "janela que avança") em vez de se espremer no trecho já andado.
- **Por que na CPU e não em shader:** a conta roda numa imagem pequena (256x56, o caminho inteiro)
  e os dois desenhos só esticam a imagem na malha única (`drawImageStrip`, a do 156b937f). Assim o
  PC (Java2D, onde o dono julga) e o celular (OpenGL) mostram os mesmos pixels. O shader GLSL das
  notas ficou como rascunho, sem uso. No OpenGL a textura sobe de novo quando a imagem muda
  (`RenderCanvas.imagemMudou` -> `GlRenderCanvas`, `texSubImage2D`).
- **Tempo:** a frente e o fundo andam por quadro, como antes; a erosão e o ruído (que corre para
  trás, contra o golpe) andam em passos de 4 quadros (15 por segundo). A erosão fica parada até
  30% da vida e sobe até 0,55; o fundo é comido 0,4 a mais; o começo do caminho também (15% dele,
  até 50 px), para a fita não nascer num corte reto. Na pausa do acerto nada se refaz.
- **Custo:** cada imagem custa cerca de 0,75 ms no PC (era 3,4 ms na primeira versão) e é refeita
  a cada quadro em que o golpe anda. No PC o `drawImageStrip` sem OpenGL passou a dividir a faixa
  em colunas pela largura, como o OpenGL já fazia (a fita estreita ia em 16 colunas e derrubava a
  gravação para 41 FPS; o chão largo continua com 16). No celular não foi medido.
- **Conferido:** três golpes de três personagens (soco fraco do p11, chute forte em arco do p14,
  jab fraco do Krüger, que é estocada), quadro a quadro, ao lado da versão de antes. O caminho
  OpenGL do jogo rodou no PC sob tela virtual (Mesa llvmpipe), com a erosão andando: **não é o
  celular**, nenhum aparelho foi visto. `testarMotor`: só as 3 falhas antigas.
- **O que se viu e ficou como estava (é do tempo, que o pedido mandou manter):** no jab do Krüger
  o acerto chega antes de a fita sair, então ela só aparece depois da pausa; no golpe fraco que
  termina logo depois da pausa, a fita é cortada de uma vez quando o golpe acaba.
- **Para o dono decidir no vídeo:** a cor (p11 sai rosa-lilás, p14 lavanda, porque é o tom medido
  com saturação puxada); o véu agora cobre o membro na pausa do acerto mais que a textura antiga;
  a base escura quase não aparece no cenário escuro (é para cenário claro).

## A pesquisa de como os estúdios fazem (10/11)

O dono parou as tentativas ("você está fazendo como você acha que é, não de forma
profissional") e pediu a técnica de verdade. O relatório está em
[`reports/VFX de luta estilo anime.md`](../reports/VFX%20de%20luta%20estilo%20anime.md), com as
notas em `research_notes/VFX de luta estilo anime/`. O essencial:

- **A Arc System Works desenha o rastro.** No Guilty Gear Strive o corte é uma malha no caminho
  da lâmina, do tamanho da área de acerto, com uma animação de **6 desenhos à mão** que avança
  enquanto desaparece e um brilho na borda da frente (aula do chefe de efeitos, Kazuya Ogawa).
  O nosso era uma textura única esticada: por isso nunca chegava.
- **O substituto para não desenhar 50 a 60 animações** vem dos artistas de efeito de outros
  estúdios: uma forma em tons de cinza, um ruído, e um shader que rola a textura, **apaga por
  erosão** (o fundo some primeiro, em bordas irregulares) e colore por **rampa de cor** do
  personagem. Cada efeito vira uma linha de tabela (forma, ruído, rampa, tempos).
- **Faísca**: pilha de camadas curtas (clarão de 2 a 3 quadros, espinhos, detritos); o único
  número completo é de fã (16 quadros no acerto, 8 na defesa).
- **Ferramentas**: nenhuma de VFX roda aqui sem tela de forma confirmada; só Python.
- Quase todo número é ponto de partida, e o shader das notas não foi compilado nem testado em
  aparelho. Próximo passo proposto: um teste só, a fita atual com erosão no fundo e rampa de
  cor, em vídeo ao lado da versão de hoje.
- Três pontos para o dono decidir: a base escura sob o corte (a regra daqui é efeito só de luz),
  efeito grande atrás do personagem (o rastro daqui passa por cima), e o desenho do PC, que não
  tem shader (a gravação que ele aprova precisa de um equivalente).

Skills de terceiros olhadas no mesmo dia e descartadas: `vfx-spell-effects` (SwiftUI/Metal),
`create-game-vfx` (lista genérica para Three.js), `Claude-Code-Game-Studios` (projeto-modelo
inteiro, sem VFX), `rea` (engenharia reversa de código, não abre arte de jogo).

## Em uma frase

Efeito desenhado por código foi tentado de novo e reprovado de novo. O caminho é **imagem gerada
(GPT/Codex) colocada no jogo e julgada com o jogo rodando**. As ferramentas para isso estão
prontas; falta gerar as imagens e escolher o estilo vendo.

## O que já estava decidido (e eu não segui)

- [efeitos-referencia-fighterz.md](efeitos-referencia-fighterz.md): **efeito é luz, sem contorno**.
  Miolo branco, cor saturada, halo macio; o contorno preto fica para os personagens.
- [efeitos-gpt.md](efeitos-gpt.md): a lista de imagens para gerar, em lotes, com a regra de cada
  tipo (LUZ em fundo preto, OBJETO e FUMAÇA em transparente, FOLHA de 4 quadros, TINTA) e o
  pedido de cada uma. O Lote 1 é o acerto, a defesa e o dano.
- [efeitos-de-video.md](efeitos-de-video.md): os efeitos que vêm de vídeo, e o registro de que os
  efeitos desenhados em código já tinham saído do jogo uma vez.
- [revisao-base/direcao.md](revisao-base/direcao.md), regras E1 a E7 e E13: um efeito-âncora e
  todos os outros derivados dele; uma paleta, uma taxa de quadros; tamanho preso ao alcance do
  golpe; duração presa à pausa do golpe; um efeito por atacante.

## O que foi feito nesta rodada

| O quê | Situação |
|---|---|
| Impacto de ar redesenhado em borda dura com miolo de nanquim (`tools/sprites/impacto_ar.py`) | Está no jogo. Contraria o "luz, sem contorno" acima e tem cara de recorte. Fica só até uma imagem substituir |
| Partículas dos poderes por código (fagulhas, estouro, fita) | **Reprovadas ("muito amador") e retiradas.** Confete geométrico, sem luz, brigando com a onda luminosa do Ryūga |
| Imagem parada → efeito do jogo (`tools/sprites/efeito_imagem.py`) | Pronto e testado no jogo com uma imagem de teste |
| Gravar o jogo sem tela por roteiro (`gradle :pc:gravarLuta`, ou `tools/sem-gradle/`) | Pronto e testado |

Para voltar o impacto de ar ao da review (macio, branco, sem contorno):

```
git checkout 10cef7e -- tools/sprites/impacto_ar.py android/app/src/main/assets/efeitos/impacto_ar android/app/src/main/assets/efeitos/impacto_ar_super
```

## Rodada de 10/11: primeiras imagens do Codex e o lugar da faísca

- **Três variações do acerto** geradas no Codex (imagem única, fundo preto, apontando para a
  direita), em `art/vfx/acerto_01.png` a `_03.png` com o pedido ao lado: **01** luz macia sem
  contorno (línguas de fogo e raios longos; a recomendada), **02** estrela recortada em faixas
  duras com anel, **03** branco contido (fina demais: some no jogo). Nenhuma foi gravada como
  `impacto_ar` ainda: falta a escolha. O miolo não vem no meio da imagem; no jogo vai com
  `--origem 0.27,0.51` (01), `0.35,0.49` (02), `0.46,0.5` (03).
- **O lugar** (`CombatEngine.impactX`): colados, o pé desenhado entra no desenho do rival e a
  faísca ficava presa na borda dele, na canela de quem chutava. Agora vai até a ponta desenhada,
  sem passar do meio do corpo do rival.
- **A direção**: a faísca gira na linha do membro que bate (o chute alto sobe, o soco reto fica
  reto, o chute agachado desce). A inclinação é medida no desenho por
  `tools/sprites/angulo_golpe.py` (gravada em `silhueta.angulo` de cada animação de golpe; o
  `caixas_sprites.py --write` refaz junto) e o `EfeitosVideo.tocarDurando` gira o efeito. Só o
  acerto gira; a defesa fica como estava.
- Na gravação, `tecla:L` é o golpe **forte** (J K L = L M H). Com `espera:1.3` o chute às vezes
  não chega (a caminhada varia); `espera:1.8` encosta no rival e acerta sempre.

## Rodada de 10/11 (noite): as referências e o que está no jogo

O usuário mandou as referências principais: dois vídeos de um jogo de luta de anime em 3D com
cara de 2D e um de um jogo de quadrinhos em cel shading. O que se tirou delas: a faísca do golpe
forte cobre o tronco e a cabeça de quem apanha; lanças de luz atravessam na linha do golpe;
vapor branco em dois tons (branco e sombra azul-acinzentada) nos pés; lascas de terra em
zigue-zague na corrida; poeira e pedras no chão; um rastro em arco acompanhando o membro no
balanço.

Estilo escolhido para seguir: a mistura da 01 com a 02 (a estrela recortada com a luz macia).

O que está em cada nome hoje (todas com `tools/sprites/efeito_imagem.py`, imagem e pedido em
`art/vfx/`):

| Nome no jogo | Imagem | Como foi posto |
|---|---|---|
| `impacto_ar` (fraco e médio) | `acerto_04.png` | `--origem 0.29,0.5 --tamanho 160` |
| `impacto_ar_forte` (novo: forte e especial) | `acerto_forte_01.png` | `--origem 0.25,0.49 --tamanho 240` |
| `impacto_ar_super` (Super e Ultra) | `acerto_05.png` | `--origem 0.5,0.5 --tamanho 220` |
| `lanca_luz` (nova: do médio para cima) | `lanca_luz_01.png` | `--origem 0.06,0.5 --tamanho 320` |
| `defesa`, `defesa_3`, `defesa_poder` | `defesa_01.png` | `--origem 0.40,0.48`, tamanho 220 / 200 / 260 |
| `defesa_2` | `defesa_02.png` | `--origem 0.50,0.48 --tamanho 220` |
| `fumaca_pouso` (aterrissagem) | `vapor_pouso_01.png` | `--fundo verde --nuvem --ancora chao --origem 0.505,0.815 --tamanho 240` |
| `fumaca_pulo` (decolagem, quique) | `vapor_pulo_01.png` | `--fundo verde --nuvem --ancora chao --origem 0.5,0.80 --tamanho 220` |
| `poeira_corrida` (largada do dash) | `poeira_corrida_01.png` | `--fundo verde --nuvem --ancora chao --origem 0.82,0.815 --tamanho 260` |
| `impacto_chao` (corpo no chão) | `impacto_chao_01.png` | `--fundo verde --nuvem --ancora chao --origem 0.5,0.815 --tamanho 360` |
| `super_pulo` | `super_pulo_01.png` | `--ancora chao --origem 0.5,0.85 --tamanho 360` |

No código (`GameView.efeitoDeGolpe`): a faísca por força é 0,8 / 1 / 1,3 / 1,4; o forte e o
especial usam `impacto_ar_forte` quando ele existe; a `lanca_luz` sai junto do médio para cima
(0,75 / 1 / 1,2; Super 1,4), por baixo da faísca e girando na mesma inclinação.

Os efeitos de chão vêm em fundo verde (branco some no preto) e com `--nuvem` (nasce do ponto,
abre e continua crescendo enquanto some). A folha passa de 4096 px em fileiras.

Ficou para o usuário conferir: o tamanho e o brilho no celular (a gravação é o desenho do PC).

## Efeitos de dano do usuário (10/11)

O usuário gerou quatro vídeos de dano em fundo preto (`art/vfx/dano/dano_fraco.mp4`, `_medio`,
`_forte`, `_baixo`) para testar. Viraram folhas com o `tools/sprites/efeito_video_preto.py` (os
quadros escolhidos passam o brilho para transparência e seguem para o `efeito_video.py`):

| Nome no jogo | Vídeo | Quando toca | Como foi posto |
|---|---|---|---|
| `impacto_ar_fraco` | `dano_fraco` | golpe fraco | `--quadros 0,1,2,4,44-84:5 --origem 633,377 --escala-mundo 0.25 --fps 12` |
| `impacto_ar` | `dano_medio` | golpe médio (e o que não tiver folha) | `--quadros 0-33:3 --origem 606,384 --escala-mundo 0.25 --fps 8` |
| `impacto_ar_forte` | `dano_forte` | forte e especial | `--quadros 0-33:3 --origem 427,384 --escala-mundo 0.30 --borda 50 --fps 8` |
| `impacto_ar_baixo` | `dano_baixo` | fraco ou médio que pega embaixo (até 35% da altura de quem apanha) | `--quadros 0-36:3 --origem 609,396 --escala-mundo 0.25 --borda 40 --fps 8` |

Saíram as imagens paradas do Codex que estavam nesses nomes (`acerto_04`, `acerto_forte_01`). O
Super continua com a `acerto_05` e a lança de luz, que agora só toca no Super e no Ultra (os
vídeos do forte e do baixo já têm os riscos longos). Cada força tem a folha no tamanho dela
(escala 1; especial 1,15). **Ordem na tela** (pedido do usuário): lutadores, efeito de dano, e o
rastro do golpe por cima de tudo, a 75% de opacidade.

## Próximo: VFX próprio de cada personagem (10/11)

O usuário testou: os efeitos que estão no jogo "funcionam, mas são modestos e iguais para todos; o
certo é termos personalizados para cada um", e os que já existem são para arrancar e trocar pelos
nossos. As referências dele são inspiração, não para copiar. A base é o levantamento dos tipos de
VFX do Tōkon, com a proposta do kit de cada personagem (rastro, faísca, energia, antecipação e
tinta no alvo): [efeitos-referencia-tokon.md](efeitos-referencia-tokon.md). A tabela de formas de
rastro por lutador é palpite para ele corrigir. Falta no motor: uma textura e uma forma de rastro
por personagem (hoje é uma só) e o rastro em vários riscos.

## Rastro do golpe: a linha de ação (10/11)

Pedido do usuário, depois de duas tentativas erradas (uma onda de ar na ponta; um arco escolhido
por regra): **o rastro tem de mostrar de onde o golpe veio e para onde foi**, como nas
referências dele (a garra que desce em arco, o chute que sai do chão, o golpe em giro). "Vamos
construir um motor que detecta o movimento e cria a sua linha de ação."

- **O motor** (`tools/sprites/linha_de_acao.py`): cada desenho do golpe, da guarda ao membro
  esticado, passa por um modelo de pose (MediaPipe Pose, o `pose_landmarker_heavy`). O membro que
  bate é o que tem a ponta na caixa do golpe do desenho e mais andou desde a guarda; a ponta dele
  é seguida para trás, desenho por desenho (o modelo troca esquerda e direita: vale o lado mais
  perto), até a primeira virada brusca ou até parar. A ponta vai até a borda do desenho (o bico
  do tênis, os dedos). Grava `silhueta.caminho` = `{cor, pontos: {quadro: [x, y, jx, jy]}}`: a
  ponta e a junta de trás (cotovelo, joelho) em cada desenho, e a cor do membro na ponta.
  Tentativas que não serviram: seguir o pedaço da ponta por semelhança de imagem (pulava para o
  pé de apoio, o cabelo, a outra mão) e medir só o quadro esticado (não diz de onde veio).
- **Onde tem**: só golpe normal, sem efeito desenhado na arte (`ARTE_COM_EFEITO`, no
  `angulo_golpe.py`) e sem caminho errado (`CAMINHO_ERRADO`, conferido no olho). Hoje cerca de
  95 golpes; os outros ficam sem rastro. `--folha <prefixo>` gera as folhas para conferir.
- **No jogo** (`RastroDoGolpe`, `EfeitosVideo.fita`): a fita nasce no começo do caminho, cresce
  e se apaga pelo fundo (ver "Procedural" abaixo). A textura
  é uma fita reta do Codex (`art/vfx/arco_textura_03.png` -> `tools/sprites/rastro_fita.py` ->
  `assets/efeitos/rastro_fita`), dobrada no caminho em fatias e pintada da cor do membro (matriz
  de cor, o caminho dos filtros dos lutadores). No **balanço** (o membro atravessado no caminho:
  a garra, o chute de lado) a fita é larga, do lado do membro; na **estocada** (o membro na
  direção do caminho: o soco reto, o chute de frente) é um feixe de riscos em volta dele.
- **Exagerado de propósito** (pedido de 10/11, vendo o Tōkon: "os rastros de golpe são
  extremamente exagerados, o corte de ar"): a fita tem de 44 a 160 px de largura conforme a
  força, fica cheia na primeira metade dos 8 a 13 quadros de vida e é desenhada **na frente**
  dos lutadores, a 75% de opacidade (decisão do usuário: cheguei a pôr atrás e ele mandou voltar; começou em 90% e ele pediu menos). A textura passou a ser a
  pincelada seca em vários riscos (`art/vfx/arco_textura_02.png`).
- **Riscos irregulares e pontudos** (correção do usuário no mesmo dia: "as linhas da ponta são
  mais irregulares e pontudas; do jeito que você fez está muito reto"): a textura em uso é a
  `art/vfx/arco_textura_03.png`, com riscos de tamanhos e grossuras diferentes, pontudos nas
  duas pontas e sem corte reto. O rastro passa de 26 a 56 px da ponta do membro, que é onde as
  pontas soltas aparecem; na estocada é um feixe só, com o caminho no meio (antes, duas faixas
  iguais, uma de cada lado, que pareciam trilho); e a textura anda pelo comprimento do caminho.
- **Procedural** (correção do usuário: "ele monta e demonstra o risco; tem de ser procedural:
  iniciou, foi indo e depois vai apagando o fundo"): a fita não aparece montada. A ponta da
  frente sai do começo do caminho e anda por ele de forma contínua, por quadro de jogo (a tela
  mostra os desenhos aos saltos; a fita não), em 3 a 9 quadros, chegando ao fim um quadro depois
  de a ponta do membro ter andado 85% do caminho. A ponta de trás vem atrás com 5, 7 ou 9
  quadros de diferença (fraco, médio, forte) e apaga o fundo até a fita sumir. Antes, cada trecho
  aparecia quando o desenho dele entrava na tela e a fita inteira desbotava de uma vez.
- **Para rodar o motor**: `pip install mediapipe` (no Linux, `apt-get install libegl1 libgles2`);
  o modelo é baixado sozinho para `~/.cache/sokon`. Só a ferramenta usa: o build, os testes e a
  CI leem o que ficou gravado no `character.json`.
- **Em aberto**: o desenho da fita é a primeira versão (os dois riscos da estocada são simples);
  cerca de 50 golpes normais ficaram sem linha (o modelo não achou o corpo, ou achou errado);
  personagem novo: `python3 tools/sprites/linha_de_acao.py <id> --write --folha /tmp/x`, olhar
  e pôr em `CAMINHO_ERRADO` o que estiver errado.

## O estilo ainda está em aberto

Três direções foram postas na mesa; nenhuma foi escolhida. A decisão é para ser tomada **vendo
imagens dentro do jogo**, não por descrição.

1. **Luz de anime** (a que os documentos acima já descrevem e a recomendada): a âncora é a onda e
   o Super do Ryūga, que já estão no jogo. Miolo branco, cor do personagem, brilho macio, poucos
   quadros segurados. Golpe físico no mesmo traço, branco e pequeno.
2. **Mangá**: borda dura, preto e branco, tinta. Foi o que saiu por código e ficou amador; só
   valeria como arte gerada, e ainda assim briga com a onda luminosa.
3. **Contido**: quase nada no golpe físico (como a review deixou) e efeito só nos poderes.

## Passo a passo na sessão com o Codex

O Codex está descrito em [cenarios-com-codex.md](cenarios-com-codex.md) (instalar, entrar com a
conta do ChatGPT, `codex exec`).

### 1. Gerar

Começar pelo **Lote 1** do [efeitos-gpt.md](efeitos-gpt.md): `acerto.png` (a referência do lote),
`acerto_forte.png` e `defesa.png`. Para testar estilo, vale pedir primeiro **uma imagem só**
(sem o trecho FOLHA) em duas ou três variações: é mais rápido e já mostra se a direção presta.

Regras que fazem a imagem servir:

- efeito luminoso em **fundo preto puro**; só o efeito, sem personagem, sem texto;
- **quadrada**, efeito centralizado, com folga nas bordas;
- **apontando para a direita** (o sentido do golpe; o jogo espelha).

Salvar em `art/vfx/` (por exemplo `art/vfx/acerto_01.png`), com o pedido usado ao lado
(`art/vfx/prompt_acerto_01.txt`), como nos cenários.

### 2. Colocar no jogo

```
# uma imagem parada: o estouro (nasce pequena, passa do tamanho, assenta, some) vai nos quadros
python3 tools/sprites/efeito_imagem.py art/vfx/acerto_01.png --nome impacto_ar

# uma FOLHA de 4 quadros lado a lado (o formato do efeitos-gpt.md)
python3 tools/sprites/efeito_imagem.py art/vfx/acerto.png --nome impacto_ar --quadros 4

# só olhar os quadros, sem gravar no jogo
python3 tools/sprites/efeito_imagem.py art/vfx/acerto_01.png --previa /tmp/previa.png
```

Os nomes que a luta toca hoje (`GameView.efeitoDeGolpe`): `impacto_ar` (acerto de golpe e de
projétil), `impacto_ar_super` (Super e Ultra), `defesa`, `defesa_2`, `defesa_3` (golpe
defendido, um de cada vez) e `defesa_poder`. Usar um desses nomes troca o efeito na hora; para
voltar, `git checkout -- android/app/src/main/assets/efeitos/<nome>`.

O que o motor já faz sozinho com a folha:

- **Tamanho**: `--tamanho` é o lado maior em px do mundo no tamanho 1 (128 no acerto forte); o
  jogo desenha o fraco a 0,625, o médio a 0,8125, e o Super a 1,25. Defesa: 0,6 / 0,7 / 0,8.
- **Tempo**: a folha é esticada para a pausa do golpe + 8 quadros (defesa + 4).
- **Lugar**: no ponto do contato, 12 px para dentro de quem apanha, espelhada pela direção.
- **Um por atacante**: o novo tira o anterior.

O que **não** existe e pode fazer falta para o estilo "luz":

- **Mistura aditiva**: a folha é desenhada com transparência comum. O brilho sobre fundo claro
  fica mais apagado do que no fundo preto da imagem. Se a luz parecer fraca no jogo, é aqui.
- **Pintar na cor do personagem** (a TINTA do efeitos-gpt.md): o desenho só muda a transparência.

### 3. Ver no jogo

Com o Gradle funcionando:

```
cd android
gradle :pc:gravarLuta -Proteiro="segura:D espera:1.3 solta:D grava:60:h tecla:L fim"
gradle :pc:gravarLuta -Proteiro="espera:1 grava:90:esp especial:J fim super:3000 grava:150:sup tecla:I fim"
gradle :pc:gravarLuta -Pdupla=p11,p13,player_base,p04 -Proteiro="espera:1 grava:80:b especial:J fim"
#   →  android/pc/build/gravar-luta/<nome>-000.png ...
```

Sem o Gradle (o Maven bloqueado): [tools/sem-gradle/](../tools/sem-gradle/README.md) faz o
mesmo só com `javac`.

O módulo `:pc` já tinha outras gravações sem tela que podem servir: `gradle :pc:testeMovimentos`
(os movimentos e golpes de um lutador), `:pc:revisarAnimacao`, `:pc:capturar` e `:pc:medirLuta`
(`gradle :pc:tasks --group verification` lista todas).

Os passos do roteiro estão no começo de `GravarLuta.java`. O jogador começa em x=1020 e o rival em
x=1580: para um golpe de perto acertar, andar 1,3 s antes. O especial de comando sai melhor com o
Ryūga (`especial:J`); em alguns personagens o mesmo comando solta outro golpe.

Para olhar e mandar:

```
python3 tools/sem-gradle/folha_quadros.py <pasta> h /tmp/h.png 8,12,16,20,24,28 - 3
ffmpeg -framerate 60 -i <pasta>/h-%03d.png -pix_fmt yuv420p /tmp/h.mp4
```

A gravação sai pelo desenho do PC em Java2D. No celular a luta é OpenGL: o que passar aqui ainda
precisa ser visto no aparelho (o APK sai sozinho na release `build-game-luta-sprite-fable` a
cada push).

### 4. Decidir e espalhar

Quando uma imagem de acerto agradar no jogo, ela vira a **âncora** (regra E1): as outras do lote
são geradas anexando ela ("mesmo estilo de traço e de brilho desta imagem") e só então vale gerar
os lotes seguintes do efeitos-gpt.md.

## Fora do VFX, o que ficou pendente

- **Jogar o APK e dizer se os golpes ficaram diferentes de um personagem para outro.** É a
  mudança principal da branch ([balanco/normais-fable.md](balanco/normais-fable.md)) e ainda não
  foi testada na mão.
- **Equilíbrio**: na simulação CPU contra CPU, Krüger e Ryūga ganham demais (79% e 76%) e
  Natisuko e Bonnie perdem demais (21% e 26%). Não foi mexido.
- **Capturas de outros jogos** em `docs/revisao-base/referencias/` (cerca de 90 imagens): decidir
  se ficam no repositório.

## Regras do repositório (valem em qualquer sessão)

- Antes do push: `git fetch` e `git merge` do origin, sem rebase e sem force.
- Não abrir PR sem pedir. Não mexer na `apk-export-sprite-7659823`.
- Nunca commitar `art/keys/<id>/tamanho.json` sem o pacote do personagem.
- APK e build de PC saem por release do GitHub, não por artefato do Actions.
