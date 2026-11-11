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
Script: `art/vfx/effectcraft/impactos_v06_ec.py`; folhas com `--origem 210,260 --escala-mundo
0.70` (fps 20 / 24 / 30). No `efeitoDeGolpe` vêm antes das anteriores, que ficam de reserva; o
golpe baixo continua com `impacto_ar_baixo`. Vídeo:
`android/app/build/sprite-review/impactos_v06_fraco_medio_forte_no_jogo.mp4`.

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
