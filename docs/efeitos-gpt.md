# Efeitos, poderes e HUD: imagens para gerar no GPT

Hoje quase todos os efeitos do jogo são **desenhados por código** (círculos, estrelas,
linhas e degradês): faíscas de acerto, defesa, poeira, super pulo, auras, os projéteis de
cada personagem, o fogo do número do super, os letreiros (READY, K.O., GUARD BREAK!...) e
boa parte das telas. A única exceção na luta é o raio do Ultra do p01, que já usa imagens
(`ultras/player_base/raio_*.png`). Este documento lista o que dá para trocar por arte do
GPT, com um prompt para cada imagem, e o que continua em código.

Levantamento feito no código em 17/10 (`ImpactEffects`, `EffectsRenderer`,
`ProjetilVisual`, `SuperFlash`, `HudRenderer`, `GameView`, telas do `story-core`).

## Como usar

1. **Uma conversa por lote** no ChatGPT (os lotes estão abaixo). Gere primeiro a imagem
   marcada como **referência** do lote e, nas seguintes, anexe ela e diga: *"mesmo estilo
   de traço e de brilho desta imagem"*. Assim os efeitos saem com cara de família.
2. **Cole a regra do tipo antes de cada prompt** (as regras estão logo abaixo). Cada item
   diz qual regra usar: **LUZ**, **OBJETO** ou **FUMAÇA**, e se é **FOLHA** (animação) e
   **TINTA** (o jogo pinta na cor do personagem).
3. **Tudo virado para a DIREITA** (o golpe vai para a direita, o projétil voa para a
   direita). O jogo espelha quando o lutador está virado para a esquerda.
4. Salve com o **nome exato** do item numa pasta `efeitos` no Drive (os projéteis em
   `efeitos/projeteis`, o HUD em `efeitos/hud`) e me avise. Eu preparo (o preto vira
   transparência, separo os quadros, ajusto tamanho) e ligo no jogo. Cada imagem que
   chega substitui o desenho em código daquele efeito; o resto continua funcionando.
5. Não precisa gerar tudo de uma vez. A ordem dos lotes é a ordem de impacto na luta.

### Regras (cole antes do prompt)

**LUZ** — energia, brilho, faísca, fogo. Fundo **preto puro**, porque brilho com fundo
transparente sai com borda feia no GPT (é o mesmo jeito do raio do p01):

```
Efeito visual para jogo de luta 2D em estilo anime, no nível de Dragon Ball FighterZ e Guilty Gear Strive: desenhado à mão, formas recortadas e afiadas, contorno de energia limpo, cores saturadas e núcleo branco incandescente. Fundo preto puro (#000000), sem nada além do efeito. Sem personagens, sem mãos, sem corpo, sem texto, sem letras, sem marca d'água, sem chão, sem cenário.
```

**OBJETO** — coisas sólidas (kunai, machado, bisturi, molduras do HUD). Fundo
**transparente**:

```
Objeto para jogo de luta 2D em estilo anime, mesmo traço de Dragon Ball FighterZ e Guilty Gear Strive: contorno preto limpo, cel shading, cores saturadas, brilho de metal marcado. Visto exatamente de lado. PNG com fundo transparente: só o objeto, sem sombra, sem chão, sem cenário, sem mãos, sem texto, sem marca d'água.
```

**FUMAÇA** — poeira, fumaça escura, sombra líquida, veneno (coisa escura que sumiria no
preto). Fundo **transparente**:

```
Efeito visual para jogo de luta 2D em estilo anime, no nível de Dragon Ball FighterZ e Guilty Gear Strive: desenhado à mão, formas recortadas, contorno limpo, cel shading. PNG com fundo transparente: só o efeito, sem chão, sem cenário, sem personagens, sem texto, sem marca d'água.
```

**FOLHA** — quando o efeito é animado, acrescente no fim do prompt:

```
4 quadros de animação lado a lado numa faixa horizontal, na ordem do tempo da esquerda para a direita, mesmo tamanho e mesma posição em todos, com espaço vazio entre os quadros.
```

**TINTA** — quando o jogo pinta o efeito na cor de cada personagem, acrescente no fim:

```
Desenhe só em branco e cinza-claro, sem nenhuma cor: o jogo pinta na cor de cada personagem.
```

### Cores de energia de cada personagem

Usadas nos projéteis e auras próprios (as mesmas dos Ultras):

| Id | Energia |
|---|---|
| p01 (Ryūga) | dourada e alaranjada, como o sol |
| p02 (Yin-lu) | azul-celeste, leve e curta, colada nas palmas |
| p03 (Kōga) | dourada e alaranjada, a mesma da família, mas explosiva |
| p04 (Brucy) | laranja-dourada, curta, só no impacto do soco |
| p05 | roxa e escura como um eclipse, rastros de velocidade |
| p06 | roxa e preta, sombras líquidas como tinta |
| p07 | dourada como ouro e moedas |
| p08 | vermelho-escarlate, rastros do chicote como fitas de luz |
| p09 | fogo laranja e vermelho |
| p10 | vapor e água azul-clara |
| p11 | névoa verde de veneno |
| p12 | vermelho-sangue e preto, aura feral |

Atenção: no código, p01 e p05 a p12 ainda usam o mesmo dourado provisório (`#F4B73B`) para
tudo que é tingido. Quando os efeitos **TINTA** entrarem, vale fechar uma cor por
personagem a partir desta tabela.

---

## Lote 1 — Acerto, defesa e dano (o que aparece em todo golpe)

Hoje: `ImpactEffects` (estrela de 9 pontas laranja, núcleo branco, anel achatado,
"lanças" de luz atravessando a tela, faíscas em risquinho). O jogo escala pelo tamanho do
golpe (fraco, médio, forte, especial) e vira na direção do golpe.

### `acerto.png` — referência do lote
LUZ + FOLHA. Substitui a faísca de acerto dos golpes fracos e médios.
```
Faísca de impacto de soco de anime no momento do acerto, vista de lado, com o golpe vindo da esquerda para a direita: estrela de impacto recortada e afiada com pontas desiguais, mais longas para a direita, núcleo branco, bordas amarelo-alaranjadas e vermelho-alaranjadas, um anel de choque achatado em volta e lascas de luz voando para a direita. Os quadros: o clarão nascendo pequeno, a estrela aberta no máximo, se partindo em lascas, sumindo.
```

### `acerto_forte.png`
LUZ + FOLHA. Golpes fortes e especiais (o jogo usa maior).
```
Explosão de impacto de golpe muito forte de anime, vista de lado, com o golpe vindo da esquerda para a direita: clarão branco grande no centro, estrela de impacto enorme e recortada, camadas amarelo, laranja e vermelho, dois anéis de onda de choque, lascas e fagulhas espirrando para a direita, linhas de impacto retas saindo do centro. Os quadros: o clarão estourando, a explosão no máximo, se desfazendo em fagulhas, os últimos restos sumindo.
```

### `lanca_luz.png`
LUZ, imagem única. As "lanças" de luz que atravessam a tela nos acertos (estilo FighterZ).
```
Uma lança de luz fina e muito comprida, horizontal, atravessando a imagem inteira da esquerda para a direita: grossa no ponto de origem à esquerda e afinando até uma ponta agulha à direita, núcleo branco, bordas amarelo-alaranjadas e um halo laranja suave. Imagem bem larga e baixa.
```

### `defesa.png`
LUZ + FOLHA. Defesa de golpe (hoje: barreira azul-ciano com hexágono).
```
Faísca de defesa de anime: um escudo de energia azul-ciano em forma de arco de hexágonos, curvado e virado para a esquerda (de onde vem o golpe), com o centro branco no ponto do impacto, faíscas azul-claras ricocheteando para trás, para a esquerda. Os quadros: o escudo acendendo no impacto, o brilho no máximo com as faíscas, o escudo trincando de leve, sumindo.
```

### `quebra_guarda.png`
LUZ + FOLHA. Quebra de guarda (hoje: defesa + acerto juntos; a ideia original era amarela).
```
Escudo de energia de anime se estilhaçando: um arco de hexágonos azul-ciano e amarelo explodindo em cacos de vidro de luz que voam para a direita, clarão branco e amarelo no centro, rachaduras de luz. Os quadros: o escudo inteiro trincando, explodindo em cacos, os cacos voando, sumindo.
```

### `defesa_perfeita.png`
LUZ + FOLHA + TINTA. Defesa perfeita (hoje: brilho branco no peito).
```
Brilho de defesa perfeita de anime: uma estrela de quatro pontas fina e muito brilhante, com a ponta horizontal bem mais longa que a vertical, um anel fino abrindo em volta e pequenas faíscas em cruz, tudo nítido e limpo, com sensação de "tim!" de metal. Os quadros: a estrela nascendo, no máximo com o anel abrindo, encolhendo, um ponto de luz sumindo.
```

### `contra_golpe.png` (novo: hoje só existe o letreiro)
LUZ + FOLHA. Acerto em contra-golpe (CONTRA-GOLPE! e PUNISH COUNTER!).
```
Faísca de contra-ataque de anime, vista de lado, golpe vindo da esquerda para a direita: estrela de impacto vermelha e amarela com núcleo branco, cercada por um losango de luz vermelha recortado, raios retos em X e fagulhas vermelhas espirrando para a direita, sensação de golpe pego no contrapé. Os quadros: estourando, no máximo, se partindo, sumindo.
```

### `poeira.png`
FUMAÇA + FOLHA. Poeira de queda no chão, quique e saída do super pulo (hoje: 3 elipses bege).
```
Nuvem de poeira e fumaça de anime rolando rente ao chão para os dois lados, bege-claro e cinza-claro com sombreado em cel shading, pedrinhas e terra soltas, base reta embaixo. Os quadros: nascendo pequena e baixa, crescendo para os lados, no máximo e começando a abrir, se desfazendo em fiapos.
```

### `choque_projeteis.png`
LUZ + FOLHA. Dois projéteis se anulando no meio do caminho.
```
Colisão de duas bolas de energia de anime no meio da imagem: um clarão branco no ponto de contato, duas metades de energia se esmagando uma contra a outra, uma vinda da esquerda e outra da direita, anel de onda de choque vertical, fagulhas espirrando para cima e para baixo. Os quadros: o contato, o clarão no máximo, a energia se desfazendo, sumindo.
```

### `quadro_impacto_vermelho.jpg` e `quadro_impacto_vazio.jpg`
Imagem cheia (16:9, sem regra de tipo). O "impact frame" de mangá dos golpes decisivos
(hoje: fundo vermelho ou preto com 70–80 linhas de velocidade e uma estrela). O jogo põe as
silhuetas dos lutadores por cima.
```
Fundo de "impact frame" de mangá de luta, formato 16:9: fundo vermelho intenso com linhas de velocidade radiais vermelho-escuras saindo de um ponto no centro da imagem até as bordas, grossas na borda e finas no centro, e um pequeno clarão branco em forma de estrela de impacto no centro. Sem personagens, sem silhuetas, sem texto, sem marca d'água.
```
```
Fundo de "impact frame" de mangá de luta, formato 16:9: fundo preto com linhas de velocidade radiais brancas saindo de um ponto no centro da imagem até as bordas, grossas na borda e finas no centro, e uma estrela de impacto branca com miolo preto no centro. Sem personagens, sem silhuetas, sem texto, sem marca d'água.
```

---

## Lote 2 — Super, movimento e auras

Hoje: `ImpactEffects.spawnSuperJump`, `spawnSuperGlint`, `EffectsRenderer.drawSuperCharge`,
`drawOverdrive`, `drawMechanicAura`, `drawAirDash`, `drawUltraRush`.

### `aura_super.png` — referência do lote
LUZ + FOLHA + TINTA. Aura de carga do Super e do Ultra.
```
Aura de energia em forma de chamas subindo, como a de um guerreiro de anime se energizando: labaredas em volta de um espaço vazio do tamanho de uma pessoa em pé (o meio é preto, sem corpo), mais intensa embaixo, com faíscas subindo e dois anéis finos inclinados girando em volta. Os quadros: as labaredas tremulando, em loop.
```

### `brilho_super.png`
LUZ, imagem única, TINTA. O brilho de ativação do Super (estrela no peito).
```
Brilho de ativação de super golpe de anime: uma estrela de quatro pontas enorme e fina, a ponta horizontal bem mais longa que a vertical, núcleo branco muito forte, halo redondo suave em volta e um anel fino, com pequenas faíscas saindo em todas as direções.
```

### `super_pulo.png`
LUZ + FOLHA + TINTA. Saída do super pulo (coluna de luz e anéis no chão).
```
Saída de um super pulo de anime: uma coluna de luz vertical estreita subindo do chão até o alto da imagem, dois anéis de onda de choque achatados no chão (elipses bem largas e baixas) abrindo em volta da base, riscos de velocidade subindo. Base reta embaixo. Os quadros: a coluna estourando do chão, no máximo com os anéis abertos, a coluna afinando, sumindo.
```

### `rastro_velocidade.png`
LUZ, imagem única, TINTA. Rastro do air dash e da investida do Ultra.
```
Rastro de velocidade de anime: feixe de linhas horizontais retas e finas de comprimentos diferentes, mais grossas e brilhantes à direita (onde está quem corre) e afinando para a esquerda, com algumas linhas curvas de vento. Imagem larga e baixa.
```

### `overdrive.png`
LUZ + FOLHA. Aura do Overdrive (hoje: oval vermelho com faíscas laranja).
```
Aura de fúria de anime em volta de uma pessoa em pé (o meio é preto, sem corpo): chamas vermelhas e laranja intensas e recortadas subindo, faíscas e brasas voando para cima, contorno vermelho-escuro, muito agressiva. Os quadros: as chamas tremulando, em loop.
```

### Auras de mecânica (uma folha cada)
LUZ + FOLHA, menos o veneno (FUMAÇA + FOLHA). Mesmo formato da `aura_super.png`: em volta
de um espaço vazio do tamanho de uma pessoa em pé.

`aura_armadura.png` (p09, p12 — armadura em golpe)
```
Aura de armadura de anime em volta de uma pessoa em pé (o meio é preto, sem corpo): placas de energia dourada em forma de escudo de metal envolvendo o contorno do corpo, brilho dourado duro, pequenos clarões brancos como reflexo de metal. Os quadros: as placas pulsando, em loop.
```
`aura_contragolpe.png` (p02, p07, p08, p11 — postura de contra-ataque)
```
Postura de contra-ataque de anime em volta de uma pessoa em pé (o meio é preto, sem corpo): um anel fino azul-ciano na altura do peito, um arco branco girando sobre ele e linhas finas de concentração apontando para o centro. Os quadros: o arco girando, em loop.
```
`aura_esquiva.png` (p04, p10 — esquiva)
```
Esquiva de anime em volta de uma pessoa em pé (o meio é preto, sem corpo): um véu de vento branco-azulado translúcido contornando o corpo, com linhas de movimento curvas, leve e rápido. Os quadros: o véu passando, em loop.
```
`aura_veneno.png` (quem foi envenenado pelo p11)
```
Névoa de veneno de anime em volta de uma pessoa em pé (o meio vazio, sem corpo): fumaça verde-escura e verde-limão subindo em fiapos, bolhas verdes estourando, gotas pingando. Os quadros: a névoa subindo, em loop.
```
`aura_furia.png` (p12 — força que cresce com a vida perdida)
```
Aura feral de anime em volta de uma pessoa curvada (o meio é preto, sem corpo): chamas vermelho-sangue com bordas pretas, recortadas como garras, brasas vermelhas subindo, olhar de fera. Os quadros: as chamas tremulando, em loop.
```

---

## Lote 3 — Projéteis e poderes de cada personagem

Hoje: `EffectsRenderer.drawOrb` (p01, p03) e `ProjetilVisual` (os outros). Todos voam para
a **direita** na imagem. Os de energia têm FOLHA (o jogo toca em loop enquanto voa); os
objetos são uma imagem só e o jogo gira quando o objeto gira (shuriken, machados).
Salve em `efeitos/projeteis/`.

### p01 (Ryūga) — `onda_p01.png` — referência do lote
LUZ + FOLHA. A onda do S1 (o jogo aumenta quando ela é carregada).
```
Bola de energia de anime dourada e alaranjada, como o sol, voando para a direita: esfera com núcleo branco incandescente, camadas amarelo e laranja, arcos de energia girando em volta, cauda de cometa comprida para a esquerda com faíscas se soltando. Os quadros: a energia pulsando e os arcos girando, em loop.
```
`onda_super_p01.png` — LUZ + FOLHA. A onda do Super (5 acertos, fica parada no alvo).
```
Bola de energia gigante de anime dourada e alaranjada, como o sol, voando para a direita: esfera enorme com núcleo branco, raios elétricos dourados saindo dela, anéis de energia girando, cauda grossa para a esquerda e faíscas por todo lado, muito mais poderosa que uma onda normal. Os quadros: a esfera pulsando e os raios mudando, em loop.
```

### p02 (Yin-lu) — `palma_p02.png`
LUZ + FOLHA. A palma de ar (alcance curto).
```
Golpe de palma de ar de kung fu em anime, voando para a direita: a forma translúcida de uma mão aberta feita de energia azul-celeste, dedos para cima, palma virada para a direita, com três arcos de onda de ar atrás dela e brilho branco nas bordas. Leve e elegante. Os quadros: a palma se formando, aberta no máximo, se desfazendo em ar, sumindo.
```

### p03 (Kōga) — `onda_p03.png`
LUZ + FOLHA. A onda curta, a mesma energia do irmão, mas explosiva.
```
Bola de energia de anime pequena e instável, dourada e alaranjada, voando para a direita: núcleo branco, energia em chamas recortadas e agressivas em volta, como uma explosão contida, cauda curta e irregular para a esquerda, faíscas espirrando. Os quadros: a energia estalando e mudando de forma, em loop.
```

### p04 (Brucy) — `estouro_p04.png`
LUZ + FOLHA. O soco de impacto (estouro curto na frente do punho).
```
Estouro de energia de anime na frente de um soco, laranja-dourado: uma explosão curta em forma de estrela de 12 pontas com núcleo branco, linhas de impacto retas saindo para a direita, anel de choque, como o impacto de um soco de boxe com energia. Os quadros: estourando, no máximo, se abrindo, sumindo.
```

### p05 — `kunai.png` e `shuriken.png`
OBJETO, imagem única cada.
```
Kunai ninja voando na horizontal, ponta para a direita: lâmina de aço escuro com fio claro e linha central, cabo enrolado em tiras com faixas douradas, argola no fim do cabo.
```
```
Shuriken ninja de quatro pontas, vista exatamente de frente, de aço escuro com fio brilhante e um furo no centro. Bem simétrica (o jogo gira).
```

### p06 — `sombra_p06.png`
FUMAÇA + FOLHA. A sombra rastejante (anda rente ao chão).
```
Sombra líquida de anime rastejando rente ao chão para a direita: uma onda de tinta preta e roxa-escura com a frente alta e a cauda baixa à esquerda, labaredas de sombra roxa subindo do topo, contorno roxo brilhante na frente, base reta embaixo. Os quadros: a sombra ondulando, em loop.
```

### p08 — `estalo_chicote.png`
LUZ + FOLHA. O estalo na ponta do chicote (o chicote em si continua desenhado pelo jogo,
porque estica e volta).
```
Estalo da ponta de um chicote em anime, vermelho-escarlate: um clarão pequeno em estrela de 6 pontas com núcleo branco, fitas de luz vermelha curvas como o rastro da ponta do chicote, faíscas. Os quadros: o estalo nascendo, no máximo, as fitas abrindo, sumindo.
```

### p09 — `machado_p09.png` e `fogo_p09.png`
`machado_p09.png`: OBJETO, imagem única (o jogo gira).
```
Machado de bombeiro visto exatamente de lado, inteiro, na horizontal: lâmina vermelha com fio de aço brilhante e espigão do outro lado, cabo de madeira com faixas amarelas refletivas.
```
`fogo_p09.png`: LUZ + FOLHA. A parede de fogo do Super, correndo pelo chão.
```
Parede de fogo de anime correndo rente ao chão para a direita: labaredas altas na frente, à direita, e mais baixas atrás, em camadas vermelho, laranja e amarelo com núcleo quase branco, brasas subindo, base reta embaixo. Os quadros: as chamas dançando, em loop.
```

### p10 — `machadinha_p10.png` e `vapor_p10.png`
`machadinha_p10.png`: OBJETO, imagem única (o jogo gira).
```
Machadinha de resgate de bombeiro vista exatamente de lado, inteira, na horizontal: cabeça de aço cinza com fio brilhante, cabo de madeira clara com fita vermelha.
```
`vapor_p10.png`: LUZ + FOLHA. O jato de vapor que sai da mão para cima e para a frente.
```
Jato de vapor e água de anime saindo de um ponto à esquerda, embaixo, em diagonal para cima e para a direita: nuvens de vapor branco e azul-claro em bolas que crescem ao longo do jato, gotas de água e linhas de pressão. Os quadros: o jato saindo, no máximo, se abrindo em nuvem, sumindo.
```

### p11 — `bisturi.png`, `seringa.png` e `poca_veneno.png`
`bisturi.png` e `seringa.png`: OBJETO, imagem única cada.
```
Bisturi cirúrgico voando na horizontal, lâmina para a direita: cabo de aço com ranhuras, lâmina brilhante com fio branco e um pequeno brilho em estrela na ponta.
```
```
Seringa de vidro voando na horizontal, agulha para a direita: corpo de vidro transparente cheio de veneno verde-limão brilhante, êmbolo de metal, uma gota verde pingando da agulha.
```
`poca_veneno.png`: FUMAÇA + FOLHA. A poça que a seringa deixa no chão (3 segundos).
```
Poça de veneno de anime no chão, vista de lado bem rasante (uma elipse muito larga e baixa): líquido verde-escuro e verde-limão brilhante, borda clara, bolhas subindo e estourando, um pouco de névoa verde. Os quadros: as bolhas subindo e estourando, em loop.
```

### Raio do Ultra do p06 (falta)
O p06 usa o Ultra de raio, mas não tem as imagens do raio: hoje é tudo desenhado por
código. Use os prompts de [`tools/ultra/README.md`](../tools/ultra/README.md#raio-final)
(corpo, ponta, início, impacto, aura, vento) com a energia **"roxa e preta, sombras
líquidas como tinta"** e acrescente: *"o roxo é brilhante e o preto aparece só como
contorno e veios dentro do roxo"* (preto puro some no fundo preto).

### Ultras do p07 ao p12
As imagens das cinemáticas (corte, palco, olhos, carga, golpe, final) já têm prompt
pronto em [`docs/ultras/prompts.md`](ultras/prompts.md). Hoje esses Ultras usam a arte
provisória desenhada pelo jogo.

---

## Lote 4 — HUD e letreiros

Hoje: `HudRenderer` (barras, fogo do número do super, faixa dos anúncios, contador de
combo) e `GameView` (seta do super pulo). Salve em `efeitos/hud/`.

### `faixa_anuncio.png` — referência do lote
OBJETO. A faixa escura atrás dos letreiros (hoje: paralelogramo escuro com linhas douradas).
```
Faixa de anúncio de jogo de luta, horizontal, bem larga e baixa (proporção 5:1): uma tira inclinada preta e azul-marinho muito escura, com as pontas cortadas em diagonal, filetes dourados em cima e embaixo, pinceladas de tinta nas pontas e leve brilho dourado. Sem texto.
```

### Letreiros
OBJETO, um arquivo por palavra, nome `letreiro_<palavra>.png` (sem acento e sem
pontuação, por exemplo `letreiro_time_over.png`, `letreiro_defesa_perfeita.png`). Use
este prompt trocando a palavra:
```
Letreiro de jogo de luta de anime com a palavra "{PALAVRA}" escrita exatamente assim, em letras maiúsculas grossas e inclinadas para a frente, preenchidas com degradê de branco no topo para amarelo, laranja e vermelho embaixo, contorno preto grosso e um segundo contorno laranja brilhante por fora, leve sombra. Só o letreiro, centralizado, com margem transparente em volta.
```
Confira se a palavra veio escrita certinha; se vier errada, peça de novo.

| Palavra | Quando aparece |
|---|---|
| READY | início da luta |
| GO! | começa a luta |
| K.O. | nocaute |
| TIME OVER | tempo esgotado |
| VITÓRIA / DERROTA / EMPATE | resultado |
| GUARD BREAK! | quebra de guarda |
| DEFESA PERFEITA! | defesa no tempo certo |
| COMBO BREAKER! | escapou do combo |
| CONTRA-GOLPE! / PUNISH COUNTER! | contra-ataque |
| OVERDRIVE! | Overdrive |
| TEAM SUPER! | Super em dupla |
| TECH! / AIR TECH! / PUSHBLOCK! / GUARD CANCEL! / AGARRÃO! | sistema |
| ARMADURA! / VENENO! / ONDA CARREGADA! / ECO DE SOMBRA! / COBRANÇA! / LEGIÃO DE SOMBRAS! | mecânicas de personagem |

Os rótulos de luta da história e do Arcade (CONVITE, QUARTAS DE FINAL, FINAL...) e os
textos das telas continuam em código, porque são traduzidos para inglês e espanhol.

### `digitos_combo.png`
OBJETO. Os números do contador de combo (hoje: texto com degradê).
```
Os números de 0 a 9 numa única linha horizontal, na ordem 0 1 2 3 4 5 6 7 8 9, com espaço vazio entre eles, todos do mesmo tamanho: estilo de contador de combo de jogo de luta de anime, algarismos grossos e inclinados para a frente, degradê de branco no topo para amarelo, laranja e vermelho embaixo, contorno preto grosso e contorno laranja brilhante por fora. Fundo transparente.
```
E o selo `hits.png` (mesmo estilo, a palavra "HITS" menor, em branco com contorno preto).

### `fogo_nivel.png`
LUZ + FOLHA + TINTA. O fogo em volta do número do nível do super (o jogo usa maior em
cada nível e pinta de azul ou vermelho por lado).
```
Chama de anime em forma de gota subindo, com 5 línguas de fogo recortadas e um núcleo branco, pequena e concentrada, base redonda embaixo. Os quadros: as línguas tremulando, em loop.
```

### `seta_chao.png`
OBJETO + TINTA. A seta que mostra quem ficou no chão durante o super pulo do outro.
```
Seta de indicador de jogo de luta apontando para baixo: um triângulo largo com as bordas cortadas em ângulo, preenchido de branco e cinza-claro com brilho, contorno preto grosso e um contorno branco por fora. Desenhe só em branco e cinza, sem cor: o jogo pinta na cor do personagem.
```

---

## Lote 5 — Telas (menu, pausa, história)

Prioridade menor: são peças que aparecem em várias telas.

### `pincel_vermelho.png`
OBJETO. A pincelada vermelha atrás dos títulos (menu, pausa, comandos, cartão do episódio).
```
Uma única pincelada de tinta vermelha (#C8202C) horizontal, larga e baixa, feita com pincel seco de caligrafia japonesa: bordas irregulares, fios de cerdas, respingos nas pontas, a ponta direita afinando. Fundo transparente, sem texto.
```

### `papel_manga.jpg`
Imagem cheia (16:9). O papel das páginas provisórias da história e do "Próximo episódio".
```
Textura de papel de mangá impresso, formato 16:9: papel creme claro (#F1ECE1) levemente envelhecido, com retícula de pontinhos bem sutil e fibras do papel. Só a textura, uniforme, sem desenhos, sem texto, sem marca d'água.
```

### `fundo_menu.jpg`
Imagem cheia (16:9). Fundo do menu inicial e do Arcade (hoje: preto com brilhos azul e
vermelho e fagulhas).
```
Fundo de menu de jogo de luta de anime, formato 16:9, quase todo preto: à esquerda uma névoa de energia azul (#2F6FD6) subindo, à direita uma névoa de energia vermelha (#C8202C), as duas se encontrando no centro escuro, fagulhas azuis e vermelhas subindo, textura de tinta de pincel sutil. O centro fica escuro e limpo, porque o logo e os botões ficam por cima. Sem personagens, sem texto, sem marca d'água.
```

### `hero_p09.png` a `hero_p12.png` — arte da seleção que falta
A arte grande do lado da tela de seleção (`selection/art/tela/hero_pNN`) só existe do p01
ao p08: escolhendo p09 a p12, o lado fica vazio. Uma conversa por personagem, com o
concept dele anexado (`art/concepts/pNN.png`), formato retrato 2:3:
```
Este é o personagem: use o concept anexado como identidade exata (rosto, cabelo, roupa, cores, proporções e traço). Desenhe um único personagem adulto em pose de ação de jogo de luta, em três quartos virado para a DIREITA, enquadrado da cabeça completa até as coxas, com a cabeça inteira dentro da imagem e margem em cima. Ilustração de anime de luta moderno, traço de tinta preto, cel shading, iluminação dramática. PNG com fundo transparente: sem aura, sem cenário, sem chão, sem outras poses, sem retratos, sem diagramas, sem texto, sem marca d'água.
```
Personagens: p09 (bombeiro ruivo enorme, colete vermelho com faixas refletivas), p10
(bombeira loira de rabo de cavalo, mesmo uniforme), p11 (médico grisalho de jaleco
manchado, mãos vazias), p12 (morto-vivo costurado com bandagens, curvado).

### Também falta
- História: páginas do ep03 ao ep07 ainda são provisórias (desenhadas pelo jogo).

---

## O que continua em código (e por quê)

- **Barras**: o preenchimento da vida, da vida vermelha, do dano que escorre e das
  células do super (mudam a cada quadro). A moldura do HUD já é imagem
  (`ui/hud/energy-frame.png`).
- **Textos que mudam**: relógio, números de dano e escala do combo, ASSIST/TROCA, segundos
  do OD, nome e grito do Ultra, menus, balões e legendas (traduzidos).
- **Partículas pequenas**: os risquinhos de faísca, as pedrinhas, a chuva e as pétalas
  (centenas por segundo; imagem não melhora).
- **Câmera e tela**: tremida, zoom, escurecer, clarão branco, câmera lenta, brilho do
  corpo do personagem e o eco de sombra do p06 (feito com o próprio sprite).
- **O chicote do p08**: estica e volta seguindo a mão; só o estalo da ponta vira imagem.

## O que muda no código quando as imagens chegarem

Anotado para eu fazer na integração (não precisa de nada de quem gera):

- Um carregador de folhas de efeito em `ImpactEffects` e `EffectsRenderer` (hoje só o
  raio do Ultra sabe tocar folhas de 4 quadros).
- Pintar imagem na cor do personagem (TINTA): o `RenderCanvas` só sabe mudar a
  transparência; precisa de um parâmetro de cor (Android e PC).
- O preparo reaproveita o `tools/ultra/preparar_raio.py`: preto vira transparência,
  separação dos quadros pelos vãos e limpeza do véu do fundo transparente.
