# VFX: onde paramos e como continuar

Roteiro para continuar o trabalho de efeitos visuais em outra sessão (a que tem o Codex e a rede
liberada). Escrito em 09/11, na `game-luta-sprite-fable`.

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
- **No jogo** (`RastroDoGolpe`, `EfeitosVideo.fita`): a fita nasce junto com o movimento (a
  cabeça dela vai para o ponto do desenho que a tela mostra) e some em 5 a 8 quadros. A textura
  é a fita reta do Codex (`art/vfx/arco_textura_01.png` -> `tools/sprites/rastro_fita.py` ->
  `assets/efeitos/rastro_fita`), dobrada no caminho em fatias e pintada da cor do membro (matriz
  de cor, o caminho dos filtros dos lutadores). No **balanço** (o membro atravessado no caminho:
  a garra, o chute de lado) a fita é larga, do lado do membro; na **estocada** (o membro na
  direção do caminho: o soco reto, o chute de frente) são dois riscos finos, um de cada lado.
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
