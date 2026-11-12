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

**Rastro automático desligado (11/11, pedido do dono):** `RastroDoGolpe.LIGADO = false`. O
dono passou a desenhar o rastro à mão em cima da folha de cada golpe (o primeiro é o chute forte
do Ryūga, p01, `art/sprites/source/player_base_heavy_straight_video_normalized.png`). O código, a
textura e o caminho detectado ficam; para voltar, `LIGADO = true`. O teste por erosão continua
guardado na branch `claude/rastro-erosao` (também desligado lá).

## Smear: o membro esticado na própria arte (11/11, teste; branch `claude/smear`)

Ideia do dono, depois de desligar o rastro automático: em vez de efeito por cima, **o próprio
desenho do personagem estica** nos quadros rápidos (o smear do anime; referência dele: a luva e a
manga que viram línguas nas cores da roupa, o punho intacto na frente). Decidido com ele: uma
ferramenta própria; começar só pelo membro que bate, em 1 ou 2 desenhos, com as cores do próprio
personagem, no chute forte (H) do Ryūga adulto (p01); quando a ferramenta estiver pronta, testar
em mais coisas (cabelo, roupa, tudo que se mexe).

- **Ferramenta:** `tools/sprites/smear.py <personagem> <golpe> [--quadros 4,5] [--previa x.png]`,
  depois `build_characters.py --write`. Escreve uma fonte nova ao lado da original
  (`..._smear.png`), aponta o clip para ela e guarda os parâmetros no clip (`smear`); a original
  fica em `sourceReferences`. `--desfazer` volta.
- **Como:** o membro é a parte do desenho perto da reta junta -> ponta do caminho detectado
  (`silhueta.caminho`). Cada ponto do membro deixa uma língua até onde estava no desenho anterior;
  as línguas são faixas de comprimentos diferentes, pontudas, nas cores do próprio membro, sem
  desfoque. Golpe em giro: o leque que o membro varreu. Golpe reto (a ponta anda na direção do
  membro, como este chute que estica do joelho dobrado): arrasto reto, senão as línguas caem dentro
  do corpo e somem. As línguas tomam o lugar do membro e a ponta (o tênis) volta inteira por cima.
- **No H do p01:** desenhos 4 e 5 (o maior salto do pé, 193 px do mundo, e o anterior). Só a folha
  mudou; as caixas de golpe e de corpo são as mesmas.
- **Comprimento:** `--comprimento meio,bordas` (fração de até onde o membro estava; guardado no
  clip). O dono foi de 1,0 a 1,3 ("estica levemente mais um pouco") e fechou em **1,2 no meio do
  feixe e 1,0 nas bordas**, em curva entre os dois.
- **Visto:** gravado no PC ao lado do de hoje, quadro a quadro. `testarMotor`: só as 3 falhas
  antigas. Celular: não visto.
- **Para o dono julgar:** o desenho 5 é o do acerto e o jogo o segura na pausa do acerto, então o
  smear fica parado uns 16 quadros (no anime ele dura 1 ou 2); se incomodar, esticar só o 4.

**Todos os M e H do Ryūga adulto (11/11, pedido do dono: "põe direto", sem vídeo):** smear em M,
6M, 2M, 2H, jH e j2H, com 1,2 no meio e 1,0 nas bordas, nos 2 desenhos depois do salto do golpe. O
efeito que veio do vídeo (clarão, arco, rastro, fumaça, coluna de luz, membro borrado) saiu:
- Onde o efeito estava por cima do corpo, o desenho virou cópia do desenho limpo da mesma pose
  (`smear.py --trocar`, guardado no clip): M 3=2 e 4-7=8; 6M 3-7=8; 2M 9,10=11; jM 7=8; 2H 4-7=8;
  jH 5-10=11; j2H 9=8 e 10-12=13. O smear cobre a passagem. A limpeza por cor (`limpar.py`) foi
  tentada antes e apagava o tronco e o braço junto com o clarão.
- Os caminhos foram medidos de novo na folha limpa (`linha_de_acao.py`; precisa de `pip install
  mediapipe` e `apt-get install libegl1 libgles2`). O jM ficou sem smear: o modelo pega a mão no
  lugar do pé (`CAMINHO_ERRADO`); só a troca tirou o arco. O 2L e o jL continuam em
  `ARTE_COM_EFEITO`.
- No soco M o smear quase não aparece: o braço anda pouco e na direção dele, as línguas caem sobre
  ele. Se o dono quiser mais, é aumentar o `--comprimento` só desse golpe.
- As línguas não passam da margem do quadro (o build exige 8 px): se passariam, o feixe todo
  encolhe na mesma proporção (o martelo j2H encolheu).
- As caixas de golpe e de corpo não mudaram (são as medidas antes, com o efeito).

**Correção e especiais (11/11, depois do dono: "não vi no jM e jH, e nem no S2 e S3"):**
- jH: os desenhos esticados (5 e 6) duravam 9 a 10 ms, menos de um quadro de jogo. Regra que vale
  para todo golpe: **o smear tem de estar no desenho do acerto** (o que o jogo segura na pausa) e
  no anterior. jH passou para 6 e 7.
- jM: o modelo de pose não acha a perna esticada; caminho medido no desenho (`CAMINHO_MANUAL` no
  `linha_de_acao.py`), smear em 6 e 7, o 7 esticando a partir da perna recolhida (`--antes 7=5`).
- S2 (punho ascendente) e S3 (seis chutes): `smear.py --limpar todos` tira o efeito do vídeo
  (faísca, anel, poeira, brilho) guardando só o corpo opaco; caminhos medidos com a pose quadro a
  quadro e guardados em `CAMINHO_MANUAL` (o detector só vê golpes normais). Smear no S2 em 6 e 7;
  no S3 em cada chute esticado (9, 14, 16, 20, 23, 29). Com o caminho, a faísca de dano dos
  especiais passa a nascer na ponta do punho ou do pé.
- As línguas pegam cor só do desenho opaco (o halo meio transparente do vídeo sujava de verde).
- S2 refeito "igual ao 2H" (pedido do dono): os desenhos 6 e 7 viram cópia do 8 (o braço limpo e
  esticado para cima) e esticam a partir do agachamento (5): `--trocar 6=8,7=8 --antes 6=5,7=5`, mais
  `--sem-escuro` (o braço passa na frente da regata preta, que virava língua escura). Um desenho
  trocado passou a usar o ponto do caminho da pose nova, não o do borrão.
- Visto nas folhas, quadro a quadro. `testarMotor`: só as 3 falhas antigas; build PASS. Celular e
  jogo: não vistos nesta correção.
- Visto: as folhas quadro a quadro dos sete golpes, e no jogo (PC) o M e o 2H. Os do ar só na
  folha. `testarMotor`: só as 3 falhas antigas; `build_characters --check`: PASS. Celular: não visto.

## Smear no p14, p15 e p16 (11/11, pedido do dono: "vamos testar agora no p14 ao p16")

M, H e especiais, direto na `game-luta-sprite-fable`, pela receita do Ryūga: `--limpar todos`, o
desenho antes do acerto vira cópia do desenho do acerto (`--trocar`), os dois esticam a partir da
preparação (`--antes`), 1,2 no meio e 1,0 nas bordas. O smear fica no desenho do acerto (o que o jogo
segura na pausa), mesmo quando o membro chega antes: os desenhos entre a chegada e o acerto duram 5
a 11 ms. A preparação é o desenho, antes da chegada, em que a ponta estava mais longe (pose()
quadro a quadro); os pontos ficam em `CAMINHO_MANUAL` (`linha_de_acao.py`), e o membro do acerto é o
que tem a ponta mais longe do tronco dentro da caixa do golpe.

- **Com smear:** p14 M, H, 2M, 2H, jM, jH, j2H, 6M; p15 M, 6M, H, 2M, 2H (com `--sem-escuro`: o
  rabo de cavalo preto passava atrás do braço), jM, jH, j2H, S3; p16 M, H, jM, S2.
- **Quase não aparece** (o membro anda pouco da preparação ao acerto): p15 jH, j2H, S3 e p16 jM. O
  p14 é todo de preto: as línguas saem escuras como a roupa.
- **Ficaram como estavam:** p14 S2 e S3 (o efeito de gelo é o próprio golpe; a limpeza apagava o
  personagem) e S4 (faísca branca opaca grudada no pé); p15 S2 e S4 (o modelo não acha o corpo); p16
  2H (a lâmina branca), jH e j2H (faíscas opacas), 6M (brilho no tênis), 2M e S3 (sem pose) e S4 (os
  desenhos tocam de trás para a frente; a ferramenta ainda não aceita). A limpeza só tira efeito meio
  transparente ou solto: efeito branco opaco grudado no membro precisa de outro jeito.
- Com o caminho novo, a faísca de dano desses golpes nasce na ponta do membro.
- Correção (pedido do dono, "o M não tô vendo o rastro"): o M do p15 é soco duplo e a preparação
  medida era a mão da frente (47 px); passou para a mão de trás recolhida no desenho 4 (138 px).
- p16 refeito (dono: "pegou quase nada, e tem efeitos de desenho nela ainda"): a pose erra nesta
  personagem, então os pontos do caminho foram marcados à mão numa grade (`CAMINHO_MANUAL`). Saíram os
  efeitos opacos do vídeo trocando os desenhos pelo limpo da mesma pose: 2H (a lâmina branca, 6-12=13),
  jH (estouro no punho, 5=6), 6M (brilho no tênis, 6,7=8), 2M (pé borrado, 8,9=10), j2H (estouro no
  joelho, 8,9=10, sem smear: o joelho anda pouco), S2 (mancha vermelha, 5=4, 6=7). Smear novo em 2H,
  jH, 6M, 2M e jM; M, H e S2 com `--sem-escuro` (a luva preta virava língua escura). Saiu de
  `ARTE_COM_EFEITO`. S3 e S4 continuam como estavam.
- "Uma esfera em volta" no 2H do p15 não é do smear: é a aura da esquiva (`drawMechanicAura`,
  `AURA_EVADE`, desde 9/11), que o jogo desenha nos quadros em que o golpe esquiva de ataque aéreo.
- Visto: o desenho do acerto de cada um (folha). `testarMotor`: só as 3 falhas antigas; build
  PASS. Jogo e celular: não vistos.

## Revisão do smear no p13, p14 e p15 (12/11, pedido do dono: "revê do p13 14 15")

- **p14 (roupa preta):** as línguas viravam blocos pretos sólidos. `smear.py --clarear 0.6` puxa as
  línguas escuras para um cinza-azulado e, nesse modo, a perna desenhada fica por cima e as línguas
  atrás (com a roupa clara elas tomam o lugar do membro, como antes). Aplicado aos 8 golpes do p14.
- **p13 (primeira vez):** pontos marcados à mão na grade. Smear em M, H, 2M, 2H, 6M, jH, j2H e S4; as
  pernas com `--clarear`, os braços (pele) com `--sem-escuro`. Os arcos brancos do j2H e do S4 e o
  borrão do 2M saíram trocando pelo desenho limpo. jM (o corpo que cai), S2 (a pedra é o golpe) e S3
  (agarrão) ficam como estavam. Saiu de `ARTE_COM_EFEITO`.
- **p15:** S3 sem o fantasma amarelado do vídeo (5-15 alternam os desenhos limpos 4 e 16; o smear do
  jab é curto); j2H sem o rastro laranja embaixo do pé (5-7=8) e com smear da perna recolhida; S4 com
  smear no chute (pontos à mão).
- Visto nas folhas; `testarMotor`: só as 3 falhas antigas; build PASS. Jogo e celular: não vistos.

**Smear: o segundo desenho a 50% (12/11, pedido do dono).** O smear fica em 2 desenhos: o anterior
ao acerto, que passa rápido, e o do acerto, que o jogo segura na pausa. As línguas do segundo agora
ficam a 50% (`smear.py --opacidade`, padrão "1,0.5" com 2 desenhos; o corpo continua opaco).
Refeito em todos os golpes com smear (p01, p13 a p16), com a configuração guardada em cada clip.
Golpes com 1 desenho (p15 S4, p16 jH) e o S3 do p01 (um desenho por chute) ficam a 100%.
O H do p01 (que estava na versão com nitidez por IA) foi refeito com o pedido do dono: o clipe volta
para a fonte `_smear.png`, `smear.py --opacidade 1,0.5`, e o `sr_hd.py` de novo em cima (pesos
`realesr-animevideov3.pth` conferidos pelo SHA-256; `pip install torch scipy`, CPU, 31 s). O
`smear.py` para com aviso quando o clipe está na versão com nitidez: refazer pede essa sequência.

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

## Teste do EffectCraft para fabricar efeitos de ponto (10/11, noite)

O dono pediu para testar se o [EffectCraft](https://github.com/storytold/effectcraft) (clone de
After Effects em Rust, com linha de comando sem tela) serve para fabricar os efeitos de ponto.
Feito com **um efeito só**, a faísca de defesa. **Nada no jogo foi trocado:** espera a aprovação
dele no vídeo `android/app/build/sprite-review/defesa_atual_x_effectcraft.mp4` (fica só na sessão).

- **Onde fica**: clonado fora do repositório (`/home/user/storytold/effectcraft`); o download de
  releases é bloqueado aqui, então foi compilado: `cargo build --release -p effectcraft-cli`,
  **14 min 17 s** em 4 núcleos, 2,2 GB em `target/`. Compilou sem erro.
- **Confirmado**: renderiza na CPU sem tela (sem DISPLAY nem Wayland); `render-frame --transparent`
  sai PNG RGBA; a render queue (`renderQueue.add` com `format: png, channels: rgba` e
  `renderQueue.render {"wait": true}`) sai a sequência PNG com alfa reto.
- **O efeito**: `art/vfx/effectcraft/defesa_ec.py` monta o projeto (`defesa_ec.ecproj`, ao lado)
  só pela linha de comando (`effectcraft-cli run --commands`) e exporta 8 PNG. Montar e
  renderizar leva **1,2 s**. A forma vem da imagem gerada que já está no jogo (`defesa_01.png`,
  regra 2 da skill): o EffectCraft tira o preto (efeito Unmult), corta em pedaços por máscara e
  anima cada um. Receita do relatório: 8 quadros a 60 por segundo, opaca de 0 a 3 e depois
  75/50/25/0 %, crescendo sempre; camadas anel (nasce no quadro 1), escudo (cresce 92 -> 113 %),
  faíscas (recuam rápido e freiam, esticadas no começo), estrela (gira 30° por quadro, começa em
  0°) e clarão (3 quadros). Só o anel e o clarão são formas do EffectCraft.
- **No jogo**: `tools/sprites/efeito_video.py <pasta dos PNG> --nome defesa_ec --fps 60 --origem
  410,492 --escala-mundo 0.2965 --resolucao 2.0` (a mesma escala e o mesmo contato da `defesa`
  de hoje): folha de 1941×946, 8 quadros. Para gravar, a lista `DEFESAS`/`DEFESA_PODER` do
  `GameView` apontou para `defesa_ec` **só na cópia local**, e voltou. A folha não foi para o
  repositório; o script e o comando acima a refazem.
- **Gravar uma defesa**: `gradle :pc:gravarLuta -Proteiro="tecla:F3 segura:A espera:0.5
  grava:420:e espera:7.5 fim"` (F3 liga a CPU, o jogador recua defendendo). A CPU não repete as
  jogadas, então as defesas caem em quadros diferentes a cada gravação. Nas duas gravações as 4
  defesas foram de **projétil** (`defesa_poder`); a defesa de golpe de perto não apareceu.

Não conferido: a defesa de golpe de perto, o celular (a gravação é o desenho do PC), e o tamanho
contra a `defesa_poder` (ela foi feita com 260; a `defesa_ec` usa a escala da comum, 220).

## Anime VFX do zero no After Effects, refeito no EffectCraft (10/11, noite)

Pedido: "pesquise como fazer anime vfx no Adobe After Effects a partir do zero e faça um
exemplo". **Nada no jogo foi trocado**: o exemplo espera a aprovação dele no vídeo
`android/app/build/sprite-review/acerto_forte_atual_x_anime_effectcraft.mp4` (só na sessão).

**A pesquisa.** A rede da sessão bloqueia os sites de tutorial (School of Motion, Lester Banks,
PremiumBeat, Video Copilot, Medium...); só a busca funcionou, com os resumos das páginas. Os
tutoriais "sem plugin" repetem cinco técnicas:

| Técnica | Como | Fonte (pela busca) |
|---|---|---|
| Tempo em degraus | Posterize Time na camada (12 ou 24 por segundo; o efeito trava a camada num ritmo e só aceita "Hold") | [Adobe, efeitos de tempo](https://helpx.adobe.com/after-effects/desktop/apply-effects-and-animation-presets/list-of-effects/time-effects.html); [Medium, The Inspired Animator](https://medium.com/the-inspired-animator/how-to-apply-a-hand-crafted-look-to-your-animations-in-after-effects-2da1c5dbd282) |
| Raios e linhas de velocidade | Fractal Noise sem escala uniforme (largura ~1, altura ~1400, contraste 800, brilho −100) e Polar Coordinates "Rect to Polar" a 100 %; máscara no centro; Random Seed `time*5` para tremer | [PremiumBeat](https://www.premiumbeat.com/blog/fractal-noise-adobe-after-effects/); [Woodpunch](https://woodpunchsgraphics.com/blogs/tutorials/speed-lines-after-effects-tutorial); [Riztagar](https://riztagar.com/anime-speed-lines/); [note.com/mogra](https://note.com/mogra/n/n7f34e6bd6e5b?hl=en) |
| Explosão de anime | só camadas de forma animadas, com Polar Coordinates por cima; brilho e explosão em camadas separadas; composição grande (2500×2500) | [School of Motion, 30 dias de AE dia 22](https://schoolofmotion.com/blog/cartoon-explosion-after-effects); [Lester Banks, explosão cel](https://lesterbanks.com/2014/09/designing-cel-animation-style-explosion-ae-30-days-ae/) |
| Fumaça e fogo cartoon | Fractal Noise "Turbulent Basic" / "Soft Linear", invertido, contraste 100 -> 125; Turbulent Displace; Levels para borda dura | [Pixflow](https://pixflow.net/blog/how-to-create-a-stunning-cartoon-smoke-effect-in-after-effects/); [AE Juice, fogo](https://aejuice.com/blog/how-to-make-fire-in-after-effects/) |
| Luz | Glow numa camada e uma segunda passada com raio grande e outras cores; clarão oval com pena grande e opacidade no máximo no contato | [Motion Array, eletricidade](https://motionarray.com/learn/premiere-pro/how-to-create-electricity-and-energy-effects-from-scratch-in-adobe-after-effects/); [Creative Bloq, raio](https://www.creativebloq.com/entertainment/vfx/develop-electric-atmosphere-with-lightning-in-adobe-after-effects) |

Formas com Trim Paths, Repeater e Wiggle Paths aparecem em todos como o jeito de fazer riscos,
espinhos e anéis. Nenhuma fonte dá a receita de uma faísca de acerto de jogo de luta: os números
de tempo vêm do nosso relatório.

**O exemplo**: `art/vfx/effectcraft/acerto_anime_ec.py` monta `acerto_anime_ec.ecproj` sem
imagem nenhuma: faísca de acerto forte de 16 quadros (8 desenhos de 2 em 2), clarão, estouro em
raios (ruído + polar + Unmult + Tint, com um disco macio de track matte), riscos em leque
(Repeater, saem rápido e freiam), espinhos em estrela com Wiggle Paths girando 30° por desenho,
anel e uma mancha laranja atrás que some no quadro 8. Cor do branco ao amarelo e ao laranja
(Glow com cores A e B). Monta e renderiza em 3,5 s. No jogo: `efeito_video.py <pasta> --nome
acerto_anime_ec --fps 60 --origem 256,256 --altura 228` (a altura da `impacto_ar_forte`); o
`efeitoDeGolpe` apontou para ela só na cópia local. Roteiro: `segura:D espera:1.8 solta:D
grava:55:g tecla:L fim` (dupla padrão); as duas gravações batem quadro a quadro, acerto no 11.

O que se aprendeu do EffectCraft 0.23.1 nisto:
- **Polar Coordinates com os nomes trocados**: quem faz os raios a partir de listras em pé é a
  opção 1, que ele chama de "Polar to Rect"; no After Effects os tutoriais usam "Rect to Polar".
- **Posterize Time não segura a escala, o giro e a posição da própria camada** (no After Effects
  também não; lá se pré-compõe). O script põe os quadros-chave de dois em dois; toda lista de
  valores tem de terminar em quadro par, senão o último valor fica preso.
- Track matte de alfa, Glow com cores A e B, Repeater, Wiggle Paths e Unmult funcionam como no
  After Effects.

Ficou em aberto para ele julgar: a faísca nova é radial (não aponta a linha do golpe como a
atual), o clarão branco cobre a cabeça do rival nos 2 primeiros desenhos, e o desenho é todo
feito no programa, o que esbarra na regra 2 da skill (forma desenhada por código já reprovada).

## A faísca do golpe forte de hoje refeita no EffectCraft (10/11, noite)

Pedido: "não consegue fazer igual à referência do que está no jogo hoje?". O alvo foi a
`impacto_ar_forte` (do vídeo `art/vfx/dano/dano_forte.mp4`), medida na própria folha quadro a
quadro. `art/vfx/effectcraft/impacto_forte_ec.py` desenha tudo no EffectCraft com caminhos de
vértices (`prop.set` em `contents/group/contents/path/path`):
- estrela de espinhos irregulares (os 10 medidos e 10 menores sorteados) em 5 faixas chapadas,
  da borda vermelho-escura ao núcleo branco; encolhe até sumir no desenho 8;
- cinco jatos pontudos na direção do golpe, nas faixas; pedaços que se soltam da ponta no
  desenho 2 e brasas que voam para a direita e freiam;
- halo laranja (Glow com cores A e B) e uma mancha laranja atrás.

Mesmo ponto, tamanho e ritmo da folha de hoje: `efeito_video.py <pasta> --nome impacto_forte_ec
--fps 8 --origem 175,171 --escala-mundo 0.6667 --resolucao 1.5`. Monta e renderiza em 0,3 s.
Vídeo: `android/app/build/sprite-review/acerto_forte_atual_x_refeito_effectcraft.mp4` (só na
sessão). **Nada no jogo foi trocado**: o `efeitoDeGolpe` apontou para ela só na cópia local.

**Anel de pressão de ar (pedido seguinte):** um anel branco-azulado meio transparente em volta
do amarelo, que nasce no desenho 1, abre rápido freando (raio 72 -> 212 px da folha), ondula
(Turbulent Displace), afina e se desfaz em arcos por erosão: um ruído de borda dura (Fractal
Noise, contraste 320) como máscara de luminância, com o brilho caindo de 100 a −75; dentro, um
disco fraco e macio de pressão. A tela cresceu 70 px à esquerda e 60 em cima e embaixo; o
contato passou a `--origem 245,231` (mesma escala). No jogo, o anel vai do quadro 12 ao 22 da
gravação. Vídeo: `android/app/build/sprite-review/acerto_forte_atual_x_refeito_com_anel_de_ar.mp4`.
O Roughen Edges sozinho não partiu o anel (só deixou a borda áspera e depois apagou de uma vez).

**O anel em 3/4 (pedido seguinte, "é isso, só que na visão 3/4"):** o anel fica de pé,
atravessado na linha do golpe: na tela é uma elipse com 45 % da altura, inclinada −12°, que
avança 49 px da folha na direção do golpe enquanto abre. A metade de trás (com o disco de pressão)
fica atrás dos jatos e da estrela; a metade da frente é uma segunda camada com máscara, por cima
de tudo, e cada metade tem a sua camada de erosão com a mesma semente (os buracos batem na
emenda). No jogo o efeito gira na linha do chute, então o anel fica atravessado no caminho do
golpe. Vídeo: `android/app/build/sprite-review/acerto_forte_atual_x_anel_de_ar_3_4.mp4`.
Aprendido: **na camada de forma do EffectCraft a máscara conta a partir do centro do conteúdo
(0, 0)**, não do canto da camada (a doc de lá fala do contrário para os pontos de efeito).

**Mais pressão de ar que fumaça (pedido seguinte):** o anel grosso, desfocado e ondulando lia
como fumaça. Agora: traço fino e nítido branco (9 -> 1 px da folha) com uma segunda linha mais
fraca logo dentro (Repeater, 2 cópias a 86 × 90 %, a de dentro a 40 %), brilho ciano curto
(Glow com cores A e B), quase sem desfoque e sem ondulação, uma bolha quase transparente só nos
2 primeiros desenhos, e abre e some mais rápido (some no desenho 7). O disco esfumado saiu.
A erosão passou para dentro da camada: **Fractal Noise no modo "Stencil Luma"**. Com track matte
o EffectCraft 0.23.1 abria o buraco também nas camadas de baixo (apagava o brilho laranja, e no
fundo escuro parecia mancha preta). Vídeo:
`android/app/build/sprite-review/acerto_forte_atual_x_pressao_de_ar.mp4` (nesta gravação a
câmera do jogo ficou um pouco mais perto que na de hoje; o acerto é no mesmo quadro).

**Miolo sem contorno à direita e faíscas das pontas (pedidos seguintes):**
- A estrela virou duas camadas: a borda (vermelho-escuro, laranja e amarelo) com uma máscara em
  modo Subtract recortando o lado direito perto do miolo, e o miolo (amarelo-claro e branco)
  inteiro por cima. O miolo branco escorre direto para os jatos, como na referência. Vídeo com
  zoom: `android/app/build/sprite-review/acerto_forte_miolo_sem_contorno.mp4`.
- Faíscas saindo das pontas da estrela (entendido como as pontas dos espinhos; se era a cauda
  dos jatos, é trocar a origem): 2 ou 3 por espinho grande e 1 por pequeno, finas, na direção do
  espinho, rápidas e freando, encolhem e apagam até o desenho 7 ou 8 (`particulas_das_pontas`).
  Vídeo: `android/app/build/sprite-review/acerto_forte_faiscas_das_pontas.mp4`.

Ficou parecido na forma, no tempo e na direção; ficou diferente no traço: a de hoje tem borda
serrilhada e textura de desenho à mão, mais laranja e brasas mais acesas; a refeita é mais
limpa, mais amarela. Aprendido: na forma do EffectCraft, como no After Effects, o primeiro grupo
da lista fica na frente (desenhar do miolo para fora), e o segundo grupo se chama
`contents/group#2`.

## Teste da receita "quadros gerados + acabamento" com o dano_forte (10/11, noite)

Recomendação 1 do relatório `reports/Pipeline profissional de VFX anime.md`, sem desenhar nada no
programa: `art/vfx/effectcraft/dano_forte_ec.py` importa `art/vfx/dano/dano_forte.mp4` no
EffectCraft (ele decodifica mp4 sem tela, igual ao ffmpeg a 1/255), remapeia o tempo por expressão
(12 desenhos: o impacto segurado por dois, depois o vídeo de dois em dois até o quadro 24), troca o
degradê macio por faixas chapadas (Colorama Custom sem interpolar, cortes medidos no vídeo: halo
até ~0,31 de intensidade vira transparente, desenho de 0,36 para cima), tira o preto (Unmult),
refaz o halo limpo (Glow A e B) e come o corpo no fim por erosão (Fractal Noise em Stencil Luma,
contraste 400, brilho de 300 caindo a −50). Monta e renderiza em 1,9 s.

No jogo: `efeito_video.py <pasta> --nome dano_forte_ec --fps 8 --origem 427,384 --escala-mundo
0.30 --borda 50` (o mesmo ponto, escala e ritmo da `impacto_ar_forte`); o nome apontou para ela
só na cópia local. Vídeo: `android/app/build/sprite-review/acerto_forte_hoje_x_receita_dano_forte.mp4`.
Primeiro passe errado: com o corte em 0,10 o halo virou uma mancha laranja chapada, e com
contraste 600 a erosão já comia o desenho 0.

**Com o anel de pressão de ar em 3D (pedido seguinte):** `dano_forte_ec.py --anel` gera
`dano_forte_anel_ec`. A camada do vídeo vira um plano 3D em z = 0 (sem câmera o 3D enquadra igual
ao 2D: diferença zero, conferido) e o anel é uma forma 3D girada (X −10°, Y 66°) que atravessa a
faísca: o arco da frente passa por cima dos jatos e o de trás fica atrás da estrela, sem as duas
camadas com máscara do `impacto_forte_ec`. Fino e nítido, linha dupla (Repeater), bolha só no
começo, brilho ciano, avança na direção do golpe, se parte em arcos por erosão (Fractal Noise em
Stencil Luma). A tela cresce 160 px em cima e embaixo: `efeito_video.py <pasta> --nome
dano_forte_anel_ec --fps 8 --origem 427,543 --escala-mundo 0.30 --borda 50`. No jogo o anel vai
do quadro 15 ao 23 da gravação (acerto no 11). Vídeo:
`android/app/build/sprite-review/acerto_forte_hoje_x_receita_com_anel_3d.mp4`.

**No jogo (pedido do dono, 10/11):** `dano_forte_anel_ec` foi para a `game-luta-sprite-fable`
como a faísca do golpe forte e especial (`GameView.efeitoDeGolpe`, antes da `impacto_ar_forte`,
que fica como reserva se a folha nova faltar). Para voltar: apagar
`android/app/src/main/assets/efeitos/dano_forte_anel_ec` (o jogo cai sozinho na `impacto_ar_forte`).

## Golpe forte: o fim expande e vira lascas (10/11, noite)

Pedido, vendo o Tōkon: "o nosso está diminuindo no final; lá ele se expande dissipando em
partículas". No `dano_forte_ec.py`: a faísca fica ancorada no contato (427,383 do vídeo) e, do
desenho 3 em diante, a escala abre de 100 a 202 % enquanto o vídeo segue até o quadro 11 (antes ia
até o 24, onde já encolheu); a erosão passou a grão esticado na direção do golpe (Fractal Noise
70 × 16 %, contraste 500, brilho de 420 a −170), que quebra a faísca em lascas compridas que se
espalham com a escala. O anel 3D ficou igual. Foi para a branch no mesmo nome
(`dano_forte_anel_ec`). Vídeo: `android/app/build/sprite-review/acerto_forte_final_expandindo_em_lascas.mp4`.

**Revisto no mesmo dia: o dono chamou a expansão acima de gambiarra** (o desenho só esticava e
furava; nada voava). Saiu. A faísca volta a seguir o vídeo com a erosão de antes, e a dissipação
passou a ser de partículas de verdade: `fagulhas()` no `dano_forte_ec.py`, Particle Emitter 2D
com partículas tipo linha (nascem esticadas na direção do voo), estouro para todos os lados dos
desenhos 3 a 5, sem gravidade, freando, amarelo-claro -> laranja, mistura normal (em Add viravam
uma bola branca) e Glow A/B. Testes que não serviram: explodir a camada da faísca no Particle
Playground (vira bolinhas em grade, cara de confete; o motion blur da composição não pega em
partícula de efeito) e o modo Direction Axis (vira um jato estreito). Vídeo:
`android/app/build/sprite-review/acerto_forte_dissipa_em_fagulhas.mp4`.

## Estudo do vídeo de referência do Tōkon e fumaça 3D toon (11/11)

O dono mandou um vídeo de 10 s do Marvel Tōkon (2400×1080, ~24 por segundo, 243 quadros) para
estudo quadro a quadro. **Nenhum quadro entra no repositório** (regra 3); ficou só na sessão.
Medidas:

| Efeito na referência | Quadros | Duração | Como é |
|---|---|---|---|
| Fumaça de pouso/dash | 74-84 | ~455 ms | branca (~#f3f3f3) com UMA sombra cinza (~#c8c8c8), sem contorno; nasce como um bico no chão, vira um volume encaroçado em 2-3 quadros, se parte em tufos que sobem e se enrolam, afina e some transparente |
| Coluna de fumaça | 19-30 | ~500 ms | o mesmo material, subindo em coluna; se abre em arcos ("C") antes de sumir |
| Faísca de acerto | 125-131 | ~290 ms | miolo branco, faixas amarelo/laranja, espinhos em zigue-zague; no fim o miolo some primeiro e sobram lascas em zigue-zague (raios) voando para fora |
| Faísca forte / contra-golpe | 203-212 | ~414 ms | a mesma, com riscos de velocidade ciano atrás do golpe |
| Outros vistos | | | rastro branco meio transparente do chute, escudo de defesa (anel oval ciano), clarão verde de escudo, esfera de gelo, silhueta branca de teletransporte, pedras e arcos de vento no giro do Hulk |

A fumaça é 3D com sombreamento de anime (o jogo é 3D). Refeita pela mesma técnica no EffectCraft:
`art/vfx/effectcraft/fumaca3d_dash_ec.py` — esferas 3D (`layer.new3dPrimitive`) em cachos tipo
couve-flor, luz paralela de cima e da frente com sombra projetada (as dobras escurecem), numa
comp aninhada; na comp final o Colorama corta a luz em duas cores chapadas (luz/sombra, corte em
0,50), erosão de borda dura e transparência no fim; 12 desenhos a 24 por segundo. O 3D roda sem
tela em ~0,06 s por quadro. No jogo: `efeito_video.py <pasta> --nome fumaca3d_dash_ec --fps 24
--origem 600,392 --escala-mundo 0.85`, no lugar da `poeira_corrida` (só na cópia local). Vídeo:
`android/app/build/sprite-review/dash_fumaca_hoje_x_fumaca_3d_toon.mp4`. Espera aprovação.

**Comparada quadro a quadro com o Tōkon e corrigida (11/11):** o dono apontou "você está fazendo
desaparecer e não dissipar" (o mesmo erro do golpe forte). Dissipar, na referência: o volume se
rasga em tufos que se afastam e sobem, cada tufo continua crescendo e se enrola, e só então fica
ralo e transparente; não encolhe nem afina. No script: do desenho 5 em diante as bolas se afastam
do centro do cacho (×1 + 0,55 por desenho), as de cima sobem mais rápido, a escala segue
crescendo, Turbulent Displace em Twist cresce até 90, desfoque leve e opacidade caindo. Tentativa
descartada no caminho: colunas de elipsoides (cara de colar de contas) com Minimax afinando
(sumir, não dissipar). Falta: os tufos do Tōkon viram fitas altas com gancho na ponta; os nossos
ainda são bolas. Vídeos: `fumaca_tokon_x_nossa_quadro_a_quadro.mp4` e
`fumaca_tokon_x_nossa_dissipando.mp4` (em `android/app/build/sprite-review/`, só na sessão).

**Formato de corrida (11/11):** o dono achou o formato estranho para corrida. Na referência, a
fumaça alta que eu copiei (quadros 74-84, 101-110, 166-175) é de impacto e batida na parede; a
poeira de corrida é outra (quadros 159-167, o Loki arrancando, e os passos do Hulk): uma cunha
baixa rente ao chão, fina e pontuda no pé, cada vez mais alta para trás, onde rola por cima, com
a borda de cima serrilhada. Refeito: 20 bolas 3D enfileiradas do pé para trás, raio crescendo com
a distância, achatadas, nascendo em sequência, com a crista e o rolo por cima; o fim dissipa em
tufos que vão cada um para um lado e sobem no seu ritmo. Comparação:
`android/app/build/sprite-review/corrida_tokon_x_nossa_quadro_a_quadro.mp4`.
Ainda diferente: a parte de trás do Tōkon é mais alta e a borda de cima é serrilhada (a nossa é
de bolotas redondas).

## No jogo: impactos fraco, médio e forte redesenhados (`impactos_v06`, 11/11)

O dono mandou os três redesenhados (zip `sokon_vfx_anime_redesenhado_v01`, guardados em
`art/vfx/impactos_v06/{forte,medio,leve}`: 6 quadros de 512 px, pivô de contato 210,260, forte 50
ms, médio 42, leve 33 por quadro; "tinha uma falha no forte"). Acabamento pedido:
- forte: halo laranja e o anel de pressão de ar em 3D (`impacto_v06_forte_ec`);
- médio: spray de ar em círculo, riscos brancos que nascem no meio na batida e correm para as
  pontas (três voltas de 22/20/18 riscos curtos e finos depois do pedido "mais riscos e
  menores"; traço afinando, Repeater, Trim Paths) (`impacto_v06_medio_ec`);
- fraco: só o desenho (`impacto_v06_fraco`).
Depois, "menos pontiagudos e mais finos": riscos de 2,5 a 3 px, ponta redonda, afinando pouco
(taper 20-30 % até 40-45 % da largura).
Depois, "médio e forte um pouco menores": os dois a `--escala-mundo 0.56` (20 % menores; o fraco
ficou em 0.70).
Depois, "um pouco menores os 3, anima mais rápido e 90% de opacidade": médio e forte a
`--escala-mundo 0.48`, fraco a `0.60`; o alfa das folhas multiplicado por 0,9 antes do
`efeito_video.py`; e no `efeitoDeGolpe` os `impacto_v06_*` tocam em 70% da duração normal
(`RITMO_IMPACTO_V06`; o jogo estica cada faísca para a pausa + 8 quadros, então o ritmo da
folha sozinho não mudava nada).
Depois, "ajusta a posição, principalmente do forte": a origem passou a ser o meio do desenho
no auge, não o pivô 210,260 da folha (o forte se abre ~75 px à frente dele e no jogo cobria o
ombro e a cabeça do adversário): forte `--origem 280,250`, médio `200,248`, fraco `214,250`.
O dono reprovou o forte assim ("agora tá saindo lá no meio da perna; antes pegava a ponta mas
tava na posição errada"): voltou para a ponta (`--origem 210,260`) e deixou de girar com a
inclinação do golpe (`efeitoDeGolpe`); inclinado ele subia por cima da cabeça no chute alto.
O dono: "problema não era a inclinação, era posição; tem que nascer do impacto do personagem,
não do adversário". A inclinação voltou e os `impacto_v06_*` agora nascem na ponta desenhada do
golpe de quem bate (`CombatEngine.pontaDoGolpe` → `HitEvent.pontaX/Y`: a caixa do desenho mais à
frente, altura seguindo o ângulo do membro). O impactX/Y de antes ficava preso no corpo de quem
apanha, vinha da caixa de acerto (não do pé desenhado) e ainda andava 12 px: no chute forte (H)
ficava ~9 px à frente e ~19 px abaixo da ponta do pé. Sem a medida do desenho, vale o impactX/Y.
Ainda assim "tá ficando no meio da perna": o golpe acerta num desenho (o H da base no 5, pé a
190 px) e pula para o membro todo esticado, que fica na tela na pausa (o 8, pé a 224 px). A ponta
agora é a do desenho mais esticado dos quadros seguintes (`PONTA_ADIANTE`), enquanto o membro
avança. Recuar a origem (achando que o pé sumia atrás do rival) foi o lado errado.
Ainda errado para o dono; ele pediu trocar a estrela pontuda por "o médio um pouco maior, sem o
spray de ar e com o círculo de pressão de ar": `impacto_v06_forte_medio` (forte_medio no
impactos_v06_ec.py, 1,25× o médio). Aprovado ("o forte ficou melhor assim"): é o forte do jogo
por enquanto, e a estrela pontuda (`impacto_v06_forte_ec`) saiu dos assets. O dono vai desenhar
um hit forte novo.

O hit forte novo chegou (11/11, `art/vfx/impacto_forte_v07`: folha de 3 × 2 quadros de 640 × 640,
centro em 320,320, fundo transparente), "não esquece do círculo de pressão": forte_v07 no
impactos_v06_ec.py (o desenho + o anel 3D, que cresce 0,60/0,54 para ficar do tamanho do anel do
forte_medio no mundo), folha `impacto_v07_forte` a 0,54 (pico do tamanho do forte_medio). Nos
assets; entra no `efeitoDeGolpe` quando o dono aprovar no vídeo (`hit_forte_v07.mp4`).
Reprovado ("aquele não tava bom"); a folha `impacto_v07_forte` saiu dos assets. Veio o v08
(`art/vfx/impacto_forte_v08`: 3 × 2 quadros de 512 × 512 em fundo PRETO com brilho e contorno preto
fino). Recorte: alfa = brilho (o maior de R, G, B) de 150 a 235 — abaixo de 150 o brilho alaranjado
que se espalha pelo quadro todo virava um quadrado translúcido; o contorno preto vira uma fresta
transparente (regra 12). Os quadros recortados estão em `frames/`. forte_v08 no impactos_v06_ec.py
(centro 258,252, anel 0,60/0,50), folha `impacto_v08_forte` a 0,50. Aprovado ("o hit põe no
jogo"): é o forte do jogo; o `impacto_v06_forte_medio` fica de reserva.

"Arruma a posição do forte" (com o rastro desligado o pé aparece): a ponta das caixas do
desenho mais esticado (224 px no H da base) pega o borrão de movimento e passa uns 55 px do tênis;
o efeito nascia no ar. Agora a ponta é a da pose (`caminhoGolpe`, o pé achado pelo
linha_de_acao.py: 187 px) quando o desenho da pancada tem ela; sem ela, as caixas como antes (os
socos M e L da base não têm, e não mudaram).

"Tira mais 10% de opacidade dos 3": as folhas dos impactos saem agora com o alfa a 0,8 do desenho
(antes 0,9) — fraco, médio, forte_medio e o v08.

## Fumaça do dash V04, desenhada pelo dono (11/11)

`art/vfx/fumaca_dash_v04` (8 desenhos diferentes de 768 × 448, 45 ms cada, pé em 710,350, corre
para a direita e a fumaça vai para trás). Folha: `efeito_video.py art/vfx/fumaca_dash_v04/frames
--nome fumaca_dash_v04 --fps 22.2 --origem 710,350 --escala-mundo 0.60` (a 0.40 ficava com uns 60%
do tamanho da `poeira_corrida`). O dono mandou pôr no jogo ("põe no jogo vamos ver"): o
`poeiraDoMotor` toca ela no lugar da `poeira_corrida` (que fica de reserva). Roteiro do dash:
`gradle :pc:gravarLuta -Proteiro="espera:0.5 grava:40:d tecla:D tecla:D fim"`.

**V05 (11/11):** `art/vfx/fumaca_dash_v05`, 10 desenhos de 768 × 448, mesmo pé (710,350), tempos
próprios por quadro (40, 40, 42, 42, 44, 45, 48, 55, 65, 75 ms). Folha `fumaca_dash_v05` com o
mesmo comando da V04 (escala 0,60) e os `duracoesMs` trocados à mão no efeito.json pelos do
LEIA-ME (o efeito_video.py só faz ritmo fixo). Mais alta e mais cheia que a V04. O dono: "acho que
você colocou invertido" → "curvas ao contrário": a folha sai de `frames_curvas_viradas/` (cada
quadro espelhado dentro da própria nuvem: continua nascendo no pé e indo para trás, com as curvas
viradas para quem corre). Aprovado com "deixa 15% menor e põe no jogo": escala 0,51 (era 0,60), e
o `poeiraDoMotor` toca a V05 (a V04 e a `poeira_corrida` ficam de reserva).

## Fumaça do pulo e do super pulo, desenhada pelo dono (11/11)

`art/vfx/super_pulo_v01`: duas imagens de 2 × 2 quadros (`folha_1.png`, `folha_2.png`; o recorte
deixou resíduo de alfa 1 em volta, zerado abaixo de 20). Os 8 quadros em `frames/` estão alinhados
pela base da nuvem (o meio da faixa de baixo em x = 380, o chão em y = 720) e espelhados ("tá
virado"). Folha: `efeito_video.py art/vfx/super_pulo_v01/frames --nome super_pulo_v01 --fps 20
--origem 380,720 --escala-mundo 0.55`. Toca na saída do super pulo (no lugar da `super_pulo`) e,
em teste, no pulo comum a 60% (`PULO_COMUM_ESCALA`, no lugar da `fumaca_pulo`); espelha quando o
lutador olha para a esquerda. "Ele fica atrás do personagem": o EfeitosVideo ganhou a camada de
trás (`atrasDosLutadores()` / `desenharAtras`, desenhada antes dos lutadores) e a fumaça do pulo vai
nela. Depois: "a do super pulo ficou muito grande, e a fumaça dos dois é um pouco atrás dele, não
embaixo": `SUPER_PULO_ESCALA` 0,7 e `PULO_FUMACA_ATRAS` 40 px do mundo para as costas.

## Motores de luz 2 e 3 (11/11), pela pesquisa em `reports/Luz em jogos de luta profissionais.md`

- **2, mistura somada:** `"soma"` no efeito.json (0 cobre, 1 luz pura; `efeito_video.py --soma`).
  O EfeitosVideo pinta cobrindo com alfa (1 − soma) e somando com alfa soma (`Mistura.SOMA`):
  OpenGL com a cor cheia e alfa zero na mistura pré-multiplicada (sem trocar estado), Canvas do
  Android com ADD, PC com `SomaComposite`. Nenhum efeito usa ainda; vídeo `impactos_soma.mp4`
  (normal, 0,5, 1) esperando o dono escolher.
- **3, luz local** (a técnica do Monkey Island SE, por personagem, sem desenhar fora da tela): o
  shader da folha do lutador tira a normal da silhueta borrada do próprio quadro (16 vizinhos do
  alfa, dois anéis de 7% e 3,5% da altura do quadro) e soma a cor da luz onde o corpo está virado
  para ela (n·l), caindo até o raio. A luz vai por lutador e por quadro no pacote do filtro
  (`GlRenderCanvas.luzLocal` → `luzDoQuadro`, `LOCAL`), ligada só em volta da folha
  (`DesenhoGl.luzLocal`; o PC tem a cópia em `pc/src/substituicoes`). O GameView acende uma luz por
  impacto (`acenderLuz`: raio, cor e força por força do golpe; sobe em 15% do efeito e apaga) e
  passa a mais forte sobre cada lutador (`luzNoCorpo`). Só OpenGL (como a contraluz). Desligado
  (`LUZ_DO_IMPACTO`) até o dono aprovar `luz_local_impacto.mp4`.
- **Gravar em OpenGL** (o que o celular mostra; o desenho do PC não tem contraluz nem luz local):
  `xvfb-run -a -s "-screen 0 1920x1080x24" gradle :pc:gravarLuta -Pgl=1 -Proteiro="..."`.
- Os quadros dos vídeos encheram o disco (14 GB) uma vez: apagar a pasta de quadros depois de
  montar cada vídeo, e `rm -rf pc/build/gravar-luta` antes de cada gravação (ela acumula).

**Velocidade dos vídeos de conferência:** o `gravarLuta` grava ~1 quadro a cada 45 ms de jogo
(~22 por segundo: o jogo anda no passo fixo enquanto o desenho do PC é lento), não a 60. Montar o
vídeo a 60 por segundo, um quadro gravado por quadro de vídeo, mostra tudo ~2,7× mais rápido que
no jogo (os vídeos de 11/11 até a fumaça V04 saíram assim; o dono achou a fumaça "muito rápida").
Na velocidade do jogo: repetir cada quadro gravado 45 × 60 / 1000 ≈ 2,7 vezes num vídeo a 60.
Conferido pela fumaça V04: 8 desenhos de 45 ms ocupam 8 quadros gravados.
Script: `art/vfx/effectcraft/impactos_v06_ec.py`; folhas com `--origem 210,260 --escala-mundo
0.70` (fps 20 / 24 / 30). No `efeitoDeGolpe` vêm antes das anteriores, que ficam de reserva; o
golpe baixo continua com `impacto_ar_baixo`. Vídeo:
`android/app/build/sprite-review/impactos_v06_fraco_medio_forte_no_jogo.mp4`.

## Motor de luz 4 (11/11): normal map nos lutadores, tirado do próprio desenho

Pedido (`docs/prompt-fable-normal-map.md`): a luz local do motor 3 só sabe a borda do corpo; dar
aos lutadores uma normal de verdade por quadro, sem dobrar a memória. Vídeos:
`android/app/build/sprite-review/normal_map_antes_depois.mp4` e `normal_map_luz_macia_x_faixas.mp4`.

**No jogo desde 11/11 à noite** (o dono, vendo os vídeos: "coloca tudo na branch fable"): os
mapas do Ryūga e do Kōga (88 folhas). No Modo Teste, o botão **LUZ** (ao lado de EFEITOS) liga e
desliga na hora (`TesteDesempenho.luz`; no PC, `-Pluz=off`). Lutador sem mapa fica como era.

**Decidido pelo dono no aparelho (12/11):** "alto ficou ruim, o leve ficou bom". O nível FORTE
saiu; ficou o SUAVE ({exagero 2,3; sombra 0,5; luz 0,55}). E pediu: "não faço ideia do foco de
luz, então tem que definir nos cenários os pontos de luz".

**Pontos de luz nos cenários (12/11).** As luzes e os fogos que o cenário já desenha (`luzes` e
`fogos` de cada camada do `stage.json`) viram ponto de luz dos lutadores com dois números a mais:

```
"fogos": [ { "x": 0.5, "y": 0.1, "tamanho": 0.42, "lutadores": 0.5, "alcance": 1.2 } ]
"luzes": [ { "x": 0.6, "y": 0.3, "raio": 0.06, "cor": "#FFB45A", "lutadores": 0.5, "alcance": 1.0 } ]
```

- `lutadores` (0 a 2): a força da luz no corpo de quem está perto, do lado dela. Sem ele (ou 0),
  a luz só enfeita o cenário, como antes.
- `alcance`: até onde chega, em alturas de tela (padrão 0,9). O fogo aceita `cor` (padrão laranja);
  a luz usa a `cor` dela.
- O `StageScene` guarda onde cada ponto apareceu na tela no quadro (`pontosDeLuz()`), o `GameView`
  leva ao mundo pela câmera e passa ao shader o mais forte sobre cada lutador (`luzNoCorpo`), pela
  mesma luz local do impacto. Só acende quem tem normal map. O fogo treme junto com a chama.
- Com mapa, a luz local usa a normal exagerada, é mais rasante (acende o lado virado para o ponto,
  não o corpo inteiro) e entra multiplicada pela cor do que ilumina.
- **Primeiro no Templo da Lua** (os braseiros: `lutadores` 0,5, `alcance` 1,2); em 12/11 mais 6
  cenários (ver "Pontos de luz nos outros 11 cenários" logo abaixo). No PC, `-Pcenario=<pasta>`
  grava em outro cenário.
- A direção geral da luz do lugar continua sendo a `contraluz.direcao` do `luzLutadores` (no
  Templo da Lua, `[1, -1]`: de cima, pela direita, o lado da lua).

Vídeo: `android/app/build/sprite-review/pontos_de_luz_off_x_on.mp4` (o Ryūga anda até o braseiro).

**Pontos de luz nos outros 11 cenários (12/11, rodada no Opus).** Pedido: marcar nos 11 cenários
que faltavam as fontes de luz perto de onde os lutadores andam (só camadas até ~600 de
profundidade), começando em `lutadores` 0,5 e `alcance` 1,0; conferir a direção da luz geral.
Só o `stage.json` mudou: nas luzes que já existiam entraram `lutadores` e `alcance`; a única luz
nova (o segundo tubo do andaime) tem `"raio": 0, "reflexo": false`, para acender o lutador sem
desenhar nada a mais no cenário (com raio 0 o `fillRadialGlow` não desenha, nos três desenhos).

| Cenário | Camada (profundidade) | Pontos marcados | `lutadores` / `alcance` |
|---|---|---|---|
| Arena Imperial | `parede` (570) | 6 lanternas de pedra do pé do muro (luzes 0, 12, 21, 29, 37, 49) | 0,5 / 1,0 |
| Beco do Brucy | `predio-grafite` (380) | a luminária de parede (luz 0) | 0,5 / 1,0 |
| | `predio-porta` (380) | a lâmpada da porta e a da esquerda da porta de aço (luzes 1 e 2) | 0,5 / 1,0 |
| Caminho dos Penhascos | `guarda-corpo` (520) | as 2 luminárias da emenda (luzes 0 e 3) | 0,4 / 1,0 |
| | `andaimes` (330) | o tubo aceso (luz 0) e o segundo tubo, novo (x 0,668, y 0,37) | 0,4 / 1,0 |
| Dojo Shūshinkan | `lanternas` (560) | a lanterna do poste | 0,5 / 1,0 |
| | `pilares` (230) | a lanterna de papel do pilar | 0,5 / 1,0 |
| Porto Secreto | `postes` (300) | a luminária do poste | 0,5 / 1,0 |
| Terraço da Lua | `parede` (556) | as 6 lanternas penduradas (luzes 5, 15, 27, 34, 47, 56) | 0,5 / 1,0 |
| Arena Central, Arena Coberta, Arena Costeira, Arena Central (pintura) | — | nenhum: as luzes delas ficam a 1100 ou mais (os braseiros do muro da arena, holofotes, a parede iluminada) | — |
| Dojo Liánwǔguǎn | — | nenhum: é dia (sombra de folhas no chão); as lanternas de pedra do muro (641) não acenderiam ninguém | — |

- **Por que poucas, e não todas as lanternas.** O jogo usa só o ponto mais forte sobre cada
  lutador, sem misturar. Numa fileira de lanternas a 150–220 px uma da outra (o guarda-corpo do
  Caminho, as 16 do pé do muro da Imperial), o lado aceso trocaria toda vez que o lutador
  passasse do meio entre duas, e o `pulsar` de cada uma faria a troca piscar se ele parasse ali.
  Ficou uma fonte a cada trecho (≈ 0,3 a 0,5 de largura de tela), como os braseiros do Templo.
  Se o dono quiser todas, o certo é o motor misturar os dois pontos mais fortes (não feito).
- **Caminho em 0,4**: no ensaio com 0,5 a coxa branca do Ryūga perdeu a sombra do lado aceso.
- **Direção da luz geral**: bate com a arte em todos. Sol à esquerda na Arena Central, na pintura
  e na Imperial (`[-1,-1]`); no Porto o céu do jogo mostra o pôr do sol à esquerda (`[-1,-1]`);
  na Costeira o sol fica um pouco à direita (`[1,-1]`); lua no alto, no meio, no Terraço
  (`[0,-1]`); teto nos dojos e na Arena Coberta (`[0,-1]`). No Caminho dos Penhascos o
  céu tem lua à direita e sol à esquerda, mas quase não aparece no jogo; a contraluz quente
  (`#FFF0D8`, `[-1,-1]`) segue o sol. No Beco não há sol nem lua; fica `[1,-1]`. Nada mudado.
- **O que mais muda entre LUZ OFF e ON é a luz geral do motor 4** (o Kōga, sem ponto perto,
  clareia igual). Por isso, nos 6 cenários com ponto, há também o vídeo de três colunas: OFF,
  ON sem pontos (o `stage.json` de antes) e ON com pontos.

Vídeos (Ryūga x Kōga, o Ryūga andando para a direita; 60 quadros a 17,5 por segundo, a
velocidade do jogo): `android/app/build/sprite-review/pontos_de_luz/<cenário>_luz_off_x_on.mp4`
(os 11) e `<cenário>_off_sem_com_pontos.mp4` (os 6 com ponto). As gravações de cada coluna são
rodadas diferentes: a câmera e o passo não batem quadro a quadro.

Testes: `:stage-core:test` passa; `:pc:testarMotor` 687 testes, só as 3 falhas antigas.

**Não visto:** nada no celular; só Ryūga e Kōga (os únicos com normal map); só andando (sem
golpe, pulo nem queda); o roteiro não leva o Ryūga a todos os pontos de cada cenário.

**O que o botão liga, e por quê (12/11).** A primeira versão só usava o mapa na luz do impacto
(motor 3), e o dono testou no aparelho: "não vi diferença nenhuma". Ele tinha razão: gravado com
a duração real, LUZ OFF e LUZ ligada são indistinguíveis, porque a luz do impacto dura o tempo da
faísca (280 ms no golpe forte) e fica embaixo dela. Os vídeos de antes tinham a luz esticada 6
vezes (`normal_map_antes_depois.mp4`): não valem como prova do que se vê no jogo. Ele escolheu
então usar o mapa na **luz do cenário**, que vale o tempo todo:

- A luz do lugar (a direção e a cor da "contraluz" do `luzLutadores`, a mesma que já fazia o fio
  na borda; com focos de luz ela segue o foco mais forte) acende o que está virado para ela, e o
  lado oposto escurece puxando para a cor do lugar (metade do fator dos pés). O que olha de frente
  para a tela fica como estava.
- **A inclinação da normal é exagerada** (2,3× no SUAVE, 3,2× no FORTE). O mapa guarda formas
  grandes, quase de frente (18° de inclinação média no miolo): sem exagero a mudança sobre a
  arte, que já tem a própria sombra pintada, não se vê. Foi o que aconteceu no primeiro ensaio.
- A luz entra quase toda multiplicada pela cor do que ilumina (10% somados, 90% multiplicados):
  somando mais, a roupa preta virava cinza.
- Os dois níveis testados: {exagero, sombra, luz} = SUAVE {2,3; 0,5; 0,55} e FORTE {3,2; 0,75;
  0,85}. No PC, `-Pvolume=exagero,sombra,luz` afina sem recompilar.
- Luz em dois tons chapados (faixas) foi testada e descartada: com a normal exagerada vira mancha.
- Cintilação: no idle do Ryūga, a mudança média entre quadros seguidos é a mesma com e sem volume
  (5,4 contra 5,7 em 255).
- O mapa agora vai para a placa de vídeo quando a folha é desenhada (não só quando uma luz
  acende), e a cópia da memória comum é solta nessa hora, como o bitmap da folha.

Vídeo com as configurações reais do jogo, sem nada esticado:
`android/app/build/sprite-review/luz_cenario_off_x_forte.mp4`; a mesma pose com e sem, nos dois
níveis: `luz_cenario_mesma_pose.png`. Por fazer, se ele quiser: os braseiros e lanternas do
cenário como luz de ponto no lutador que passa perto (as posições já estão no `stage.json`, em
`luzes` e `fogos` das camadas).

**1. De onde vem a normal.** Sete jeitos nos mesmos quadros (Ryūga, Kōga, p08, p12, p13, p15):

| Jeito | O que deu |
|---|---|
| Silhueta borrada (o motor 3) | só a borda; o miolo é chapado |
| Luminância como relevo (Laigter / SpriteIlluminator) | não serve em arte pintada: o traço vira sulco e a cor da roupa vira degrau (a "cara de metal" do Sandfox) |
| Silhueta estufada (Lumo) | um travesseiro: braço na frente do tronco não aparece |
| Estufada por regiões de tinta | cada linha vira vinco; treme mais e não acerta o que está na frente |
| Depth Anything V2 (profundidade → normal) | degraus duros entre as partes e inclinação exagerada |
| **Metric3D v2 ViT-small** (estima a normal direto; ONNX, na CPU) | **volume de verdade**: perna na frente do tronco, braço, peito, rosto, dobra da calça, em formas grandes |
| Sapiens 0.3b (só corpo humano) | mais detalhe (rosto, tecido), porém ruidoso, 14 s por quadro (20× mais lento) e precisa do PyTorch |

Ficou o Metric3D, com três arrumações na ferramenta (`tools/sprites/normal_map.py`):

- **No tempo.** O modelo olha um quadro por vez e a normal treme de um para o outro: no idle do
  Ryūga, 5,2° onde o desenho não mudou (a silhueta treme 1,5°). Misturando cada quadro com os
  dois vizinhos de cada lado só onde a cor é a mesma, o mapa final fica em 2,5° (Kōga: 3,3° contra
  2,2° da silhueta).
- **Só as formas grandes**: borrão de 1,2% da altura.
- **A borda vem da silhueta** (até 3% da altura do contorno). Numa medida de 0 a 1 (1 = a borda
  inteira aponta para fora do corpo), o modelo sozinho dá 0,65 e o mapa final 0,99.

**2. Memória** (medido: a dupla da gravação tem 1.359 MB em 125 folhas, sem compressão):

| Mapa | Erro contra a normal inteira | Diferença na luz (níveis de 255) | Memória |
|---|---|---|---|
| 1/2 do lado, 2 canais de 8 bits | 0,6° | 0,35 média, 2,9 no pior 1% | 1/8 da folha |
| **1/4, 2 canais de 8 bits** | 1,7° | 1,0 média, 7 no pior 1% | **1/32: +42 MB na luta (3,1% de 1.359, pela conta)** |
| 1/8, 2 canais de 8 bits | 4,1° | 2,5 média, 15 no pior 1% | 1/128: +11 MB |
| 1/16 | 8,8° | 5,2 média, 30 no pior 1% | não vale |

Ficou 1/4 com x e y em 8 bits (`GL_RG8`; o z o shader refaz). As caixas da folha são múltiplas de
4, então o mapa tem o mesmo desenho de caixas dividido por 4 e usa o mesmo uv. Ryūga: 689 MB de
folhas, 21,5 MB de mapas. 4 ou 5 bits por canal economizariam pouco e pioram; compressão de
textura (EAC RG11) cortaria pela metade de novo, mas pede um codificador a mais: não fiz.
O mapa fica na memória comum (2 bytes por ponto) e só sobe para a placa quando uma luz acende
um quadro daquela folha.

**No APK**: o arquivo é um WebP cinza com o x na metade esquerda e o y na direita. Com x e y em
vermelho e verde o WebP com perda errava 28° (ele guarda mal a cor); em cinza, qualidade 90, erra
1,4° e pesa 6,2% do arquivo da folha (Ryūga: 2,7 MB sobre 43,6; para o elenco inteiro a conta dá
perto de +22 MB sobre 352). Sem perda seriam 17%.

**3. No shader** (`GlRenderCanvas`): com mapa, a luz local lê a normal dele (uma leitura, meio
ponto do mapa para dentro do retângulo do quadro, para não misturar com o quadro vizinho) no
lugar dos 16 vizinhos da silhueta; sem mapa, vale a silhueta como antes. O mesmo programa e o
mesmo lote: o número da textura do mapa vai no pacote do filtro do quadro, que já existia. O
texto do shader é montado duas vezes do mesmo modelo: o novo (`FRAGMENTOS`) e o de antes do motor
4 (`FRAGMENTOS_ANTIGO`, idêntico byte a byte ao que estava no jogo). **Se a placa do aparelho
recusar o novo, o jogo fica com o antigo** (aviso no log "shader com normal map recusado") e a
luz usa a silhueta em todos. Os dois passam no `glslangValidator` como GLSL ES 3.00. O mapa de uma
folha carrega junto com ela quando o OPENGL do Modo Teste está ligado (medido no PC: +32,5 MB em
87 folhas para Ryūga e Kōga, na linha "normais ... MB" do log de carregamento).

**Luz em faixas na luz do impacto** (testada nos vídeos de 11/11, fora do jogo desde 12/11): com a conta de luz do motor 3 o
ganho do mapa é discreto (a luz deixa de lavar o braço por igual e passa a seguir o músculo e o
rosto). Uma conta mais rasante, em dois tons chapados de borda curta (a sombra de anime), mostra
mais a forma. É escolha de visual, dele.

**Gravar** (os mapas têm de estar ao lado das folhas, em `res/drawable-nodpi`):

```
python3 tools/sprites/normal_map.py player_base p03      # ~0,8 s por quadro; o elenco (12.174 quadros) ~2,7 h
cd android && rm -rf pc/build/gravar-luta
xvfb-run -a -s "-screen 0 1920x1080x24" gradle :pc:gravarLuta -Pgl=1 \
    -Pgraficos=ALTO -Presolucao=1920x1080 -Proteiro="segura:D espera:1.8 solta:D grava:44:g tecla:L fim"
```

`-Pluz=off` começa desligado, `-Pcenario=<pasta>` troca o cenário, `-Pvolume=exagero,sombra,luz` afina o
volume, `-PluzDura=N` estica a luz do impacto e `-PluzFixa=1` a deixa na força cheia,
`-PnormalAlterna=1` liga e desliga o mapa a cada quadro (dois quadros seguidos com a mesma pose,
com e sem), `-Pbotoes=1` mostra os botões do Modo Teste e `-Pgraficos=ALTO` impede o nível
gráfico de baixar no meio. Em 1080p o PC grava um quadro a cada ~57 ms: montar a 17,5 por segundo
(não a 22). Com o processador ocupado o jogo anda lento e o golpe erra.

**Folha refeita, mapa refeito.** O mapa é do desenho: quem refaz uma folha (vídeo novo, SR HD,
smear) roda `normal_map.py <folha> --refazer` em seguida. Se a folha mudar de tamanho e o mapa
ficar o antigo, o jogo ignora o mapa (aviso no log) e o `build_characters.py` o trata como saída
velha: o `--check` acusa e o `--write` apaga (aí é refazer com `normal_map.py`). Se só o desenho
mudar, ninguém tem como saber. O `build_characters.py` conhece o `<folha>_n.webp` desde 12/11:
antes disso o `--check` acusava os 88 mapas como órfãos e o `--write` os apagaria (erro meu ao
mandar os mapas; avisado por outra sessão). Foi o que houve no mesmo dia com `player_base_idle` e
`player_base_heavy_straight` (refeitas em SR HD): os dois mapas foram refeitos.

**O que não foi feito ou visto:** nada rodou no celular (nem o shader, nem a memória, nem o
custo); só Ryūga e Kōga têm mapa (88 folhas); no PC, os 12 cenários foram vistos com o volume (os outros 11 só em 12/11, andando, sem golpe); a
licença dos pesos do Metric3D (código BSD de 2 cláusulas; a conversão ONNX está como CC0) precisa
ser conferida antes de um lançamento comercial.

## Motor de luz e sombra definitivo (12/11, branch `claude/motor-luz-definitivo`)

Pedido do dono, depois dos vídeos de 12/11 ("a luz tá muito estourada"): parar de ajustar em cima
do que existe e fazer um motor de luz e sombra definitivo, mesmo removendo o de hoje. Em três
fases, parando no fim de cada uma: estudo, proposta de uma página, protótipo em vídeo (Ryūga e
Kōga em templo_lua, dojo_yinlu e beco_brucy, motor de hoje x motor novo). **Nada vai para a
`game-luta-sprite-fable` até ele aprovar pelo vídeo.**

**Fase 1, estudo (feita em 12/11).** Relatório: `reports/Motor de luz e sombra.md`; notas por
frente em `research_notes/Motor de luz e sombra/` (princípios da pintura, jogos de luta, 2D com
normal map, 2D com luz pintada, contas de shader, e a medida no nosso jogo).

- **O que as fontes dizem, em uma linha:** personagem desenhado não é "aceso" com luz somada; a
  cor pintada é multiplicada por uma luz perto de 1, a tinta do cenário é pouca e ajustada à mão,
  e o branco puro fica para brilho e efeito. A regra mais direta é do Guilty Gear Xrd REVELATOR:
  luz de golpe e de cenário **substitui** a luz ambiente pela distância, não soma, porque somar
  "would make them too bright and reduce visibility".
- **Medido no nosso jogo** (PC, OpenGL, 1080p, parados, caixa em volta do lutador): ligar a luz
  leva a área cortada em branco do Ryūga de 1,4% para 11,8% da caixa no Templo da Lua (perto de um
  terço do corpo), 1,7% → 10,5% no Dojo Liánwǔguǎn, 1,2% → 5,9% no Beco.
- **Por que estoura (duas causas):** (1) o lutador já chega claro demais antes de qualquer luz: à
  noite o fundo tem luma mediana 36 a 55 e o Ryūga, com a luz desligada, p90 de 146 a 164; o
  `brilho` dos 12 cenários vai de 0,86 a 1,03, inclusive nos de noite. (2) contraluz, volume e luz
  local **somam** por cima, com corte em 1 a cada passo; a roupa branca só pode cortar.
- **O que não tem fonte:** o rig de luz da luta no SF6 (site oficial bloqueado), qualquer dado
  técnico do DBFZ, tinta por cenário no BlazBlue, o método do Streets of Rage 4, normal map no
  Children of Morta, números de sombra no chão e de contraluz, custo por conta em GPU de celular.
- Medir de novo: `gravarLuta` com `-Proteiro="espera:1.5 grava:8:g espera:0.6 fim"`, com e sem
  `-Pluz=off`, e contar na caixa do lutador os pontos com algum canal ≥ 250.

Fases 2 (proposta) e 3 (protótipo): por fazer, esperando o dono ler o estudo.

## No jogo: golpe forte = V4 com acabamento (`acerto_forte_05_ec`, 11/11)

Pedido do dono: "aplica os effects nele e põe". `art/vfx/effectcraft/acerto_forte_05_ec.py` corta a
folha V4 (ordem `0,1,2,2,3,4,5,6,7`, folga de 150 px em cima e embaixo), importa como sequência no
EffectCraft e põe por cima o halo laranja (Glow A e B) e o anel de pressão de ar em 3D do
`dano_forte` (convertido para a escala da V4, ×0,30/0,43); a faísca vira plano 3D para o anel
atravessá-la. Sem faixas de cor nem erosão (o desenho já tem as cores e já se dissipa).
`efeito_video.py <pasta> --nome acerto_forte_05_ec --fps 24 --origem 255,483 --escala-mundo 0.43`.
No `GameView.efeitoDeGolpe` ela vem antes da `dano_forte_anel_ec` (que fica de reserva). Vídeo:
`android/app/build/sprite-review/acerto_forte_v4_com_acabamento_no_jogo.mp4`.

## V4 da faísca do golpe forte (`acerto_forte_05.png`, 11/11) — a mais perto do Tōkon

Folha do dono 3120 × 1300, 4 × 2 células de 780 × 650, fundo transparente, opaca: miolo branco
grande e recortado no contato (255, 333 da célula), braço longo para a frente com ponta laranja,
respingos, e no fim lascas tortas em zigue-zague que voam e somem. Ordem `0,1,2,2,3,4,5,6,7`,
`efeito_video.py <pasta> --nome acerto_forte_05 --fps 24 --origem 255,333 --escala-mundo 0.43`.
Sobreposta ao Tōkon: no auge (127) o miolo e o leque quase se encaixam um no outro, e no 129 as
lascas têm a mesma forma, cor e direção. Diferença: a nossa já vem inclinada ~15° para cima e o
jogo ainda gira na linha do chute, então no chute alto ela sobe bastante. Vídeo:
`android/app/build/sprite-review/acerto_forte_v4_tokon_e_jogo.mp4`.

## V3 da faísca direcional (`acerto_forte_04.png`, 11/11)

Mesma família da `acerto_forte_03`, mais macia: miolo branco maior, braços amarelos meio
transparentes (alfa mediano 45), lascas do fim que se dissolvem. Mesmo corte e mesmos números
(`--origem 125,223 --escala-mundo 0.66`). No jogo: mais luz e menos forma; o miolo cobre melhor o
contato, mas o amarelo some contra o cenário claro e o fim quase não se lê. Vídeo:
`android/app/build/sprite-review/acerto_forte_v3_tokon_e_jogo.mp4`. Esperando o dono.

## Faísca do golpe forte direcional (`acerto_forte_03.png`, 11/11)

Segunda folha do dono (4 × 2, células de 443 px, fundo transparente, sem contorno): miolo branco
no contato (x 125, y 223 da célula) e braços amarelos com ponta laranja apontando para a frente,
que no fim viram lascas e somem. Ordem `0,1,2,2,3,4,5,6,7` e
`efeito_video.py <pasta> --nome acerto_forte_03 --fps 24 --origem 125,223 --escala-mundo 0.66`.
No jogo gira na linha do chute e sai para a frente do golpe. Sobreposta ao Tōkon: direção,
tempo e as lascas do fim (quadro 129) batem bem; no auge a dele é mais cheia (miolo branco
largo e corpo laranja), a nossa é um leque mais estreito. Vídeo:
`android/app/build/sprite-review/acerto_forte_03_sobreposto_no_tokon.mp4`. Esperando o dono.

## Faísca do golpe forte desenhada (`acerto_forte_02.png`, 11/11)

O dono mandou uma folha de 8 desenhos (4 × 2, células de 443 px, fundo transparente, borda
escura opaca): risco do golpe, estrela pequena, explosão grande de miolo branco, anel que se
quebra e lascas que voam e somem. Guardada em `art/vfx/acerto_forte_02.png`. Para o jogo: cortar
as 8 células e montar a ordem `0,1,2,2,2,3,4,5,6,7` (segura a explosão grande como o Tōkon nos
quadros 126-128), sem recorte por preto (a imagem já tem alfa), e
`efeito_video.py <pasta> --nome acerto_forte_02 --fps 24 --origem 229,227 --escala-mundo 0.55`.
Sobreposta ao Tōkon quadro a quadro, bate em fase, tamanho e tempo (estrela no 125, explosão
cheia 126-128, lascas 129-130, some no 131). Diferenças: a nossa é redonda e simétrica (a dele
estica para o lado do golpe), tem borda escura (a dele não) e as lascas são menores. Vídeo:
`android/app/build/sprite-review/acerto_forte_02_sobreposto_no_tokon.mp4`. Esperando o dono.

## Pesquisa "como fazer anime VFX" com a rede aberta (11/11)

Lidos por inteiro:
- **Estúdio japonês de vídeo de pachinko (Matrix, zenn.dev):** faísca de impacto em 3 fases:
  発生 (nasce: ponto pequeno e muito claro), メイン (o auge, onde a impressão se decide) e 余韻
  (o resto, apagando quieto). Peças feitas em branco e preto e coloridas depois, cada uma com a
  sua cor (a grande na cor principal, as pequenas com o matiz deslocado): fonte de luz, brilho
  largo, linhas grossas saindo da fonte, linhas finas voando, anel de choque, partículas de luz.
  Começa pelas peças do meio e grandes. Cada peça com um tempo de vida diferente; "as partículas
  que perdem velocidade somem por último". Junta em Screen/Add e, por cima, uma névoa colorida
  fraca (4-Color Gradient + Turbulent Displace em Overlay). Comp quadrada, o dobro da tela.
  https://zenn.dev/matrixsoftware/articles/da59a47433fec0
- **Flash FX (energia e impacto desenhados à mão):** o miolo é branco e a cor vem do brilho;
  desenha primeiro a parte maior; fórmula "acumula, estoura grande, dissipa", a dissipação
  animada direto, quadro a quadro, como fumaça. http://flashfx.blogspot.com/2014/01/10-energy-strikes.html
- **ParticleFX Studio:** biblioteca grátis de efeitos anime (soco forte, faísca, defesa, dash,
  poeira...), folha PNG com fundo transparente, termos permitem jogo comercial sem crédito (não
  pode redistribuir a biblioteca; pacotes da CodeManu pedem crédito; uso automatizado só pelo
  servidor MCP deles). Visto: são estouros genéricos de partículas, abaixo do Tōkon e do nosso
  `dano_forte`. Não adotado.

Conclusão: a fórmula profissional (fases, peças com vidas diferentes, cor por peça, brilho)
coincide com o relatório; o que separa o Tōkon é o desenho à mão de cada quadro, não a técnica.

## No jogo hoje: golpe forte sem fagulhas (11/11)

O dono mandou tirar as fagulhas ("tira esse treco"). A folha `dano_forte_anel_ec` voltou à versão
do commit `5bf7c27`: o vídeo `dano_forte` com acabamento (faixas, impacto segurado, halo, erosão)
e o anel de pressão de ar em 3D, sem partículas. O script `dano_forte_ec.py` seguiu com as
tentativas (lascas); para refazer exatamente a folha do jogo, usar o script desse commit.

## Golpe forte: dissipa em lascas de raio, medido no Tōkon (11/11)

Na referência (quadros 124-133, ~290 ms) a faísca não encolhe: quando o miolo apaga, os braços
pontudos se soltam e viram raios em zigue-zague (amarelo com borda e ponta laranja) que seguem
voando na direção em que apontavam, afinam e somem. No `dano_forte_ec.py --anel`: `lascas()`
substitui as fagulhas — um traço por braço do vídeo (13 direções medidas), quebrado em raio pelo
operador Zig Zag (cantos vivos), taper nas duas pontas, segundo traço laranja mais largo por
baixo, nascendo na metade do braço no desenho 4 e voando para fora freando, afinando em 4
desenhos; o vídeo apaga junto (opacidade 80, 25, 0 nos desenhos 4 a 6). Gravado no jogo só na
cópia local (`dano_forte_lascas_ec`). Comparação no tempo:
`android/app/build/sprite-review/acerto_forte_tokon_x_nosso_lascas.mp4`. Diferença que sobra: as
nossas lascas são mais grossas que as dele e somem um quadro antes.

## Defesa em 3D, medida no Tōkon (11/11)

`art/vfx/effectcraft/defesa3d_ec.py`, medida na referência (quadros 77-84, ~250 ms a 24 por
segundo): clarão pequeno ciano e branco no contato (q0), abre num disco oval de energia em pé, de
frente para quem bate (q1-q3: sólido com máscara oval, Gradient Ramp radial vezes Fractal Noise
fervendo, cortado em 4 faixas de azul a branco pelo Colorama, 3D girado 68° em Y), depois só o
anel fino (elipse 3D com traço) que se abre e apaga (q4-q7), com Glow ciano por cima. Comparação:
`android/app/build/sprite-review/defesa_tokon_x_nossa_quadro_a_quadro.mp4`. No jogo seria
`efeito_video.py <pasta> --nome defesa3d_ec --fps 24 --origem 300,300 --escala-mundo 0.55` no
lugar das `defesa*`. Não testada no jogo.

## Fumaça de pouso em 3D toon (11/11)

`art/vfx/effectcraft/fumaca3d_pouso_ec.py`, medida na queda do Hulk da referência (quadros
165-177): duas colunas altas (~400 px numa tela de 2400) que nascem como bicos finos nos pés e
voam para os lados ~700 px em 2 ou 3 quadros, viram volume encaroçado e se rasgam em tufos que
sobem e ficam ralos (~450 ms). Mesma técnica da fumaça do dash (esferas 3D, luz com sombra,
Colorama em duas cores). No jogo seria `efeito_video.py <pasta> --nome fumaca3d_pouso_ec --fps
24 --origem 1200,952 --escala-mundo 0.6` no lugar da `fumaca_pouso`. Comparação:
`android/app/build/sprite-review/pouso_tokon_x_nossa_quadro_a_quadro.mp4`. Ainda diferente: as
colunas do Tōkon são mais estreitas e com ganchos que se enrolam embaixo; as nossas são mais
largas e quadradas. Não testada no jogo.

## Fumaça do dash (10/11, noite): tentativa no programa não aprovada

Pedido: "fazer um novo efeito de fumaça do pé no dash"; o dono acha a `poeira_corrida` de hoje
feia. Duas tentativas, nenhuma no jogo:
1. a mesma `poeira_corrida_01.png` animada em partes (revelar a partir do pé, riscos que somem
   cedo, nuvem que rola e se desfaz por erosão de borda dura) — o movimento ficou bom, mas o
   desenho é o que ele acha feio;
2. fumaça cel "metaball" feita no EffectCraft (`art/vfx/effectcraft/poeira_dash_ec.py`: tufos +
   Gaussian Blur + Channel Threshold no alfa + duas cores + erosão) — três passes, todos com cara
   de programa (pão, bolinhas, faixa lisa). Pela regra 2 revista, não serve.

Roteiro que grava o dash: `gradle :pc:gravarLuta -Proteiro="espera:0.5 grava:40:d tecla:D tecla:D fim"`
(a poeira aparece do quadro 8 ao 20). Próximo passo: arte gerada nova (imagem com 4 a 6 desenhos
lado a lado, ou vídeo em fundo verde), e o EffectCraft só no acabamento, como no `dano_forte`.

## Regra 2 revista (10/11, noite)

Depois de ver a faísca do golpe forte refeita no EffectCraft, o dono revisou a regra do desenho
por código: **"só aceitar código de ficar nível profissional"**. A skill `vfx-luta` (regra 2) foi
atualizada: imagem ou vídeo gerado continua o caminho padrão; desenho feito no programa só vale
seguindo técnica profissional, comparado lado a lado no jogo, e quem julga o nível é ele.

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
