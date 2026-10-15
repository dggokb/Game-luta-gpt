# Prompts das imagens: Prólogo e Episódio 1

Faltam 10 imagens, todas do Episódio 1 (`ep01`, as páginas depois da luta): o Prólogo já está completo em páginas prontas. Se o Episódio 1 também for feito em páginas inteiras (o padrão agora, veja [motor-paginas.md](motor-paginas.md)), use os prompts abaixo só como roteiro das cenas. Para trocar a arte
provisória, salve cada imagem como **JPG** com o nome da tabela em
`android/app/src/main/assets/historia/<episódio>/` (ex.: `historia/ep01/pag7_q1.jpg`). Não
precisa recortar: o jogo enquadra a imagem no painel sozinho. Se o recorte cortar uma parte
importante, ajuste o `"foco"` do painel no `episodio.json` (0 = mostra o topo, 1 = mostra a base).

Para ver o resultado no PC sem abrir o jogo: `cd android && gradle :pc-preview:runHistoria`
(grava as páginas montadas em `android/build/historia/`).

## Concepts de referência (anexar no ChatGPT)

Todos estão em `art/concepts/` (cópias dos concepts do Drive, pasta `game-luta-gpt/concepts`).
Anexe **só os concepts de quem aparece no quadro**: anexar concepts demais faz o GPT misturar
roupas.

| Quem | Arquivo | Quando | Pontos que o GPT costuma errar |
|---|---|---|---|
| Ryūga criança (8 anos) | `ryuga_8.png` | Prólogo | cabelo castanho bagunçado, regata preta, calça branca larga, luvas pretas com vermelho, tênis cano alto preto/branco/vermelho. **Não é dogi** |
| Kōga criança (8 anos) | `koga_8.png` | Prólogo | cabelo espetado castanho-escuro, regata preta, calça cargo preta larga, mesmas luvas, tênis preto/vermelho |
| Takatoshi (45 anos) | `takatoshi_45.png` | Prólogo | cabelo castanho bagunçado, cavanhaque, colete longo sem mangas branco com azul-marinho e vermelho e um dragão azul bordado nas costas e na barra, gola vermelha, quimono preto por baixo, faixas vermelha e azul com borlas, luvas sem dedos, tênis cano alto |
| Ryūga (18 anos) | `p01.png` | Episódio 1 | colete azul e branco sem mangas com gola vermelha, regata preta, calça branca com faixa azul, faixa preta com pontas vermelhas |
| Kōga (18 anos) | `p03.png` | Episódio 1 | colete preto sem mangas com capuz e forro vermelho, regata branca, calça cargo preta com tiras vermelhas |
| Dafu | `dafu.png` | os dois | corpulento e forte, coque no alto da cabeça, bigode fino e comprido caído, olhos sempre fechados de tanto sorrir, colete mostarda com nuvens douradas, camisa branca, faixa vermelha, cabaça pendurada na cintura, calça escura larga, faixas nos tornozelos |
| Takatoshi (55 anos) | `takatoshi_55.png` | **não aparece agora**: é o Campeão do fim da temporada (cabelo longo e barba, quimono preto rasgado, geta). Não usar no Prólogo |

O mensageiro do torneio aparece na página 6 do Prólogo (chapéu e sobretudo pretos): anexe essa
página quando ele aparecer.

## Como gerar no ChatGPT

1. **Uma conversa só para a história.** Cole o estilo base na primeira mensagem e peça para
   ele valer para todas as imagens; mesmo assim, repita o estilo se o traço começar a mudar.
2. **Anexe os concepts** da tabela acima em cada quadro.
3. **No Prólogo, cole também o complemento de flashback.**
4. **Tamanho:** use o formato da tabela (horizontal = 1536×1024, quadrado = 1024×1024).
   Os painéis muito largos (faixas) são um recorte do meio da imagem: o prompt já pede para
   a ação ficar na faixa central.
5. **Sem texto na imagem.** Os balões, a narração e as onomatopeias são colocados pelo
   jogo. O prompt pede espaço livre onde os balões entram.

### Estilo base

```
Quadro de mangá colorido no estilo de anime de luta dos anos 90 (Yu Yu Hakusho, Fatal Fury
anime, Street Fighter II V): traço de tinta preto e grosso, cores chapadas e vibrantes,
sombras duras, iluminação dramática, muito contraste, retícula leve nas sombras.
Personagens idênticos aos concepts anexados: mesmo rosto, cabelo, roupa, cores e proporções.
Sem texto, sem letras, sem balões de fala, sem onomatopeias, sem molduras de quadrinho,
sem bordas, sem marca d'água. A imagem ocupa o quadro inteiro.
```

### Complemento de flashback (só no Prólogo)

```
Lembrança de dez anos atrás: cores desbotadas puxando para o sépia e o âmbar, luz quente e
suave, leve granulado de papel antigo.
```

O **convite do torneio** é como na página 6 do Prólogo: envelope preto com um lacre de cera
vermelha com um dragão, entregue pelo mensageiro de chapéu e sobretudo pretos. Use as páginas
6 e 7 do Prólogo como referência nos prompts das cartas (`pag7_q3` do Episódio 1).

---

## Prólogo — "O Pai que Partiu" (`historia/ep00/`)

**Completo em páginas prontas** (arte de vocês, `pag1.jpg` a `pag11.jpg`), lido pelo motor de
páginas ([motor-paginas.md](motor-paginas.md)). Não precisa de mais nenhuma imagem.

---

## Episódio 1 — "Duas Cartas" (`historia/ep01/`)

Tempo presente (sem o complemento de flashback). Os irmãos têm 18 anos: são exatamente os
concepts p01 (Ryūga) e p03 (Kōga). Dafu usa o concept `dafu.png`.

### Páginas 1 a 6 (antes da luta): prontas

Páginas de mangá prontas (`pag1.jpg` a `pag6.jpg`), lidas pelo motor de páginas: dez anos
depois, Ryūga treina sozinho no dojo, Dafu observa, Kōga volta e provoca o irmão, e os dois
partem para a luta. Não precisam de mais nenhuma imagem.

> Depois delas vem a luta de treino (Ryūga contra Kōga). A arte não muda nada nela.

### Página 7 (quatro quadros)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag7_q1.jpg` | horizontal 1536×1024 (vira faixa bem larga) | p01, p03, dafu | Depois do treino: Ryūga e Kōga sentados no chão do dojo, suados e ofegantes, rindo um para o outro; Dafu de pé no meio, com as mãos na cintura, dando bronca de brincadeira. Todos numa faixa horizontal no meio da imagem; o resto é chão e parede de madeira. |
| `pag7_q2.jpg` | quadrado 1024×1024 | — | A porta de entrada da casa vista de dentro, de madeira e papel, com a sombra de uma mão batendo do lado de fora projetada no papel. Corredor escuro e silencioso. Centro da imagem livre para a onomatopeia. |
| `pag7_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | — | Detalhe: dois envelopes iguais, lado a lado no chão de madeira da entrada, pretos, cada um com o lacre de cera vermelha com o dragão (o mesmo do convite do Prólogo). Cada um tem um nome escrito com pincel (o texto pode ser ilegível). Envelopes no centro exato da imagem. |
| `pag7_q4.jpg` | horizontal 1536×1024 (vira faixa larga) | dafu | Close do rosto de Dafu olhando para baixo, para os envelopes fora de quadro: pela primeira vez os olhos estão abertos, arregalados, o sorriso sumiu, suor frio, sombra cobrindo a testa. Rosto no terço direito da imagem, numa faixa no meio da altura; à esquerda só sombra. |

### Página 8 (duas colunas)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag8_q1.jpg` | quadrado 1024×1024 | p01 | Ryūga de pé lendo a carta aberta nas mãos, do peito para cima, expressão séria e determinada, luz da janela atrás. Ele fica no meio da altura da imagem; o quarto de cima e o quarto de baixo são livres para dois balões. |
| `pag8_q2.jpg` | quadrado 1024×1024 | p03 | Kōga amassando a carta no punho fechado, dentes cerrados, olhar de raiva, veias saltando, energia escura e vermelha subindo em volta, dojo à noite ao fundo. Do peito para cima, no meio da altura; o terço de cima livre para o balão de grito. |

### Página 9 (dois quadros no alto, faixa embaixo)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag9_q1.jpg` | horizontal 1536×1024 | dafu, p01 | Na sala da casa, Dafu à esquerda, sem o sorriso de sempre, olhando preocupado para Ryūga à direita, que responde com um aceno firme de cabeça. Os dois da cintura para cima, no meio da altura; o terço de cima livre para dois balões. |
| `pag9_q2.jpg` | horizontal 1536×1024 | p03 | Kōga segurando a carta aberta e apontando uma linha do texto com o dedo, de lado para a câmera, olhar sério. Ele fica no meio da altura; o quarto de cima e o quarto de baixo são livres para dois balões. |
| `pag9_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | p01, p03 | Ryūga e Kōga de costas um para o outro na porta do dojo, ao pôr do sol, cada um olhando para um lado (Ryūga para a esquerda, Kōga para a direita), mochilas no ombro. Céu laranja e roxo enorme. Os dois numa faixa horizontal no meio da imagem; o terço de cima é céu livre para a narração. |

### Página 10 (quadro único)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag10_q1.jpg` | horizontal 1536×1024 | dafu, takatoshi_45, ryuga_8, koga_8 | Noite. Dafu sozinho no dojo vazio e escuro, sentado no chão de costas para a câmera, ombros caídos, iluminado só pela lua que entra pela janela. Nas mãos, uma foto antiga emoldurada: Takatoshi (como no concept de 45 anos) com os dois filhos de 8 anos. Clima de culpa e silêncio. Ele fica no terço esquerdo da imagem; a parte de cima à direita é sombra livre para o balão de pensamento. |

---
