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

## Rastro do golpe (10/11)

Pedido do usuário. O rastro é uma onda de ar na ponta do braço ou da perna quando o golpe estica,
virada para onde o membro aponta. É o mesmo desenho que a arte do Ryūga (player_base) já traz em
9 dos 11 normais (a meia-lua na frente do punho): a imagem foi gerada a partir desses quadros
(`art/vfx/ref_rastro_ryuga.png`) para os outros 15 lutadores ficarem iguais a ele.

| Nome no jogo | Imagem | Como foi posto |
|---|---|---|
| `rastro_golpe` (fraco e médio) | `rastro_02.png` (a onda com riscos de ar) | `--sopro --origem 0.84,0.48 --tamanho 170` |
| `rastro_forte` (forte) | `rastro_01.png` (a meia-lua grande) | `--sopro --origem 0.52,0.5 --tamanho 170` |

- **Onde sai** (`tools/sprites/angulo_golpe.py`, `silhueta.rastro` de cada animação: x, y e graus
  da ponta por quadro): só golpe normal, e só onde a arte não tem efeito desenhado
  (`ARTE_COM_EFEITO`, conferido no olho) nem a medida sai errada (`MEDIDA_ERRADA`). Hoje são
  cerca de 125 golpes; especiais e Super ficam com os efeitos deles.
- **A medida é conferida por dois caminhos** (a linha do membro e a ponta mais longe do corpo):
  quadro em que os dois não batem fica sem ponta e sem inclinação. Isso também limpou a
  inclinação da faísca, que em alguns golpes pegava o pé de apoio ou a mão aberta.
- **Quando sai** (`GameView.rastroDoGolpe`): no primeiro quadro em que a tela mostra o membro
  esticado, dentro da janela ativa ou até 3 quadros depois; dura de 6 a 10 quadros e anda junto
  com quem bate (o golpe no pulo). Fraco em 0,75.
- A regra antiga (direcao.md: golpe sem linhas de velocidade, para não atrapalhar a leitura da
  distância) foi respeitada no que dá: o rastro é curto e colado na ponta, não uma linha atrás
  do membro. Se atrapalhar no jogo, é tirar as duas pastas de `assets/efeitos`.
- **O arco do balanço** (o que o usuário queria: a onda na ponta não era o pedido). No médio e
  no forte o rastro é um arco de ar por onde o membro passou, como nas referências. A textura é
  uma fita reta gerada no Codex (`art/vfx/arco_textura_01.png`); o
  `tools/sprites/rastro_arco.py` curva ela num arco de círculo e anima (a ponta nova varre o arco
  em dois quadros e a fita se desfaz pela ponta velha). Folhas: `rastro_arco` (95°),
  `rastro_arco_forte` (`--abertura 130 --grossura 0.36`) e as duas `_desce` (`--desce`). O
  `silhueta.rastro` ganhou o comprimento do membro (o raio) e o sentido (+1 subiu, -1 desceu,
  0 reto); o jogo põe o centro do arco na raiz do membro, escala pelo raio e gira na inclinação.
  O sentido é uma regra, não uma medida: o golpe inclinado para baixo desceu, os outros subiram
  (o desenho tem poucos quadros entre a guarda e o membro esticado para medir o caminho). O
  fraco e o rasteiro continuam com a onda na ponta.
- Personagem novo ou arte nova: `python3 tools/sprites/angulo_golpe.py <id> --write`, olhar o
  quadro do impacto de cada golpe e pôr em `ARTE_COM_EFEITO` ou `MEDIDA_ERRADA` o que não servir.

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
