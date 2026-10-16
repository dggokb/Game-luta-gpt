# Cenário: Dojo da Yin-Lu (`dojo_yinlu`) — prompts

Segundo cenário do motor em camadas (`docs/stage-engine.md` e `tools/stage/README.md` no
branch `art/cenarios-anime-20261011`). Baseado nas páginas de quadrinho da Yin-Lu: o portão
de madeira do 連武館 com lanternas de papel, muros brancos com telhado de telhas escuras,
árvores, terraços e degraus de pedra, pilares vermelhos, estandarte bordô com lótus branca,
arranha-céus ao longe e um dia de céu azul com nuvens.

Pasta final: `android/app/src/main/assets/stages/dojo_yinlu/`.

## Estilo (colar antes de cada prompt)

```
Arte de cenário para jogo de luta 2D em estilo anime, mesmo traço de um mangá de luta
moderno (estilo SNK / King of Fighters), dia ensolarado de céu azul com nuvens brancas,
luz do sol quente vindo da esquerda, sombras suaves, cores vivas e saturadas, contorno
limpo e sombreamento em cel-shading. Arquitetura de dojo tradicional chinês/japonês de
madeira escura, telhas cinza-azuladas, paredes de reboco branco e pedra cinza-clara.
Emblema recorrente: flor de lótus branca estilizada dentro de um círculo.
Sem personagens, sem pessoas, sem animais, sem texto além do indicado, sem marca d'água.
```

## Prompts

| Arquivo | Formato | Fundo | Profundidade |
| --- | --- | --- | --- |
| `ceu.jpg` | horizontal 1536×1024 | opaco | 100000 |
| `montanhas.png` | horizontal | transparente | 9000 |
| `cidade.png` | horizontal | transparente | 4200 |
| `arvores.png` | horizontal | transparente | 1800 |
| `portao.png` | horizontal | transparente | 1000 |
| `muro.png` | horizontal | transparente | 1050 |
| `terraco.png` | horizontal | transparente | 560 (fim do piso) |
| `pilar.png` | vertical 1024×1536 | transparente | 300 |
| `lanterna_pedra.png` | vertical | transparente | 120 |
| `chao.jpg` | horizontal | opaco | piso |
| `chao_lajotas.jpg` | horizontal | opaco | piso (repetições) |

### `ceu.jpg`
```
Céu diurno de verão: azul intenso no topo clareando para azul-claro perto do horizonte,
grandes nuvens cúmulos brancas e fofas na metade de baixo, algumas nuvens finas no alto.
Só o céu: sem prédios, sem montanhas, sem árvores, sem chão, sem sol visível.
```

### `montanhas.png`
```
Silhueta de morros e montanhas distantes cobertos de floresta, azul-esverdeado claro e
desbotado pela distância (perspectiva atmosférica), com névoa clara no pé. Ocupa só a
metade de baixo da imagem. As bordas esquerda e direita terminam na mesma altura, para a
imagem poder se repetir. PNG com fundo transparente, sem céu.
```

### `cidade.png`
```
Skyline distante de uma metrópole moderna asiática vista de longe: arranha-céus de vidro
de alturas variadas, azul-acinzentados e levemente desbotados pela distância, alguns
guindastes, e entre eles telhados de pagodes antigos. Os prédios são mais altos à direita
e à esquerda e mais baixos no centro. A base é uma linha reta na parte de baixo. Sem céu:
tudo acima dos prédios é transparente. PNG com fundo transparente.
```

### `arvores.png`
```
Faixa contínua de copas de árvores vista de frente: pinheiros orientais, bordos verdes e
árvores frondosas de folhas verde-vivo iluminadas pelo sol da esquerda, alturas variadas,
sem troncos aparecendo embaixo (a base é uma faixa reta e densa de folhagem). As bordas
esquerda e direita continuam iguais, para se repetir lado a lado. PNG com fundo
transparente.
```

### `portao.png`
```
Portão de entrada de um dojo tradicional, vista frontal e simétrica, sem perspectiva:
portal de madeira escura com telhado de telhas cinza-azuladas de beirais curvos, duas
portas grossas de madeira com cravos de ferro abertas para dentro, mostrando lá atrás um
pátio de pedra e o salão principal do dojo com telhado de dois andares. Acima das portas,
uma placa de madeira horizontal com os ideogramas 連武館 entalhados. Duas lanternas de
papel cilíndricas cor de pêssego penduradas nos cantos do portal, cada uma com uma lótus
vermelha pintada. Base de pedra reta embaixo. Só o portão: os lados são transparentes.
PNG com fundo transparente.
```
Se os ideogramas vierem tortos, gere de novo pedindo "placa de madeira lisa, sem escrita".

### `muro.png`
```
Trecho de muro de dojo tradicional, vista frontal, em linha reta: paredes de reboco
branco divididas em painéis por postes de madeira escura, base de blocos de pedra
cinza-clara, coberto no alto por um telhadinho de telhas cinza-azuladas com beiral.
Iluminação do sol vindo da esquerda. As bordas esquerda e direita continuam iguais, para
o muro se repetir lado a lado. Base reta embaixo. PNG com fundo transparente.
```

### `terraco.png`
```
Borda de um terraço de pedra de templo, vista frontal, em linha reta: balaustrada baixa
de pedra cinza-clara com colunas quadradas e corrimão de pedra, sobre um muro de arrimo
de blocos de pedra com três degraus largos embaixo, alguns tufos de grama e pequenos
arbustos verdes nas juntas. As bordas esquerda e direita continuam iguais, para se
repetir lado a lado. PNG com fundo transparente.
```

### `pilar.png`
```
Vista frontal de um pilar redondo de madeira laqueada vermelha, alto, à esquerda da
imagem, sobre uma base de pedra quadrada, com um pedaço de viga de madeira escura e
telhas no topo. Pendurado na viga, ao lado do pilar, um estandarte de tecido bordô
comprido com uma flor de lótus branca estilizada dentro de um círculo branco. À direita,
uma lanterna de papel cor de pêssego pendurada, com uma lótus vermelha pintada. O pilar
toca a borda de baixo da imagem. PNG com fundo transparente.
```

### `lanterna_pedra.png`
```
Lanterna de pedra tradicional (tōrō) de granito cinza, vista frontal, sobre uma base de
pedra, com uma pequena janela aberta no compartimento de luz (vazio, sem chama), musgo
verde nas bordas, ao lado de duas pedras arredondadas e um arbusto baixo. A base toca a
borda de baixo da imagem. PNG com fundo transparente.
```

### `chao.jpg`
```
Piso de pátio de dojo visto exatamente de cima (vista ortográfica, sem perspectiva, sem
horizonte): grandes lajotas retangulares de pedra cinza-bege, irregulares e gastas pelo
uso, com juntas escuras e um pouco de musgo e folhas soltas aqui e ali. No centro, um
grande círculo de pedra mais clara com uma flor de lótus entalhada em baixo-relevo.
Iluminação uniforme, sem sombras de objetos. As bordas esquerda e direita continuam o
padrão das lajotas.
```

### `chao_lajotas.jpg`
```
O mesmo piso do prompt anterior (lajotas de pedra cinza-bege vistas exatamente de cima,
vista ortográfica), mas sem o círculo e sem a lótus no centro: só lajotas, do mesmo
tamanho, cobrindo toda a imagem. As quatro bordas continuam o padrão, para repetir.
```

## Dicas

- Fundo transparente: na API, `background: "transparent"`; no ChatGPT, peça "PNG com fundo
  transparente". Se vier com fundo, gere de novo.
- O piso tem que vir visto de cima; o jogo é que põe em perspectiva.
- As lajotas de `chao.jpg` e `chao_lajotas.jpg` precisam ter o mesmo tamanho.
- Brilho das lanternas, neblina e folhas caindo são feitos pelo jogo (no `stage.json`,
  `petalas` com cor verde de folha), então não precisam estar na arte.
