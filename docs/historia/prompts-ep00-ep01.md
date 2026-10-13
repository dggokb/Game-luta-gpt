# Prompts das imagens: Prólogo e Episódio 1

São 25 imagens: 11 do Prólogo (`ep00`) e 14 do Episódio 1 (`ep01`). Para trocar a arte
provisória, salve cada imagem como **JPG** com o nome da tabela em
`android/app/src/main/assets/historia/<episódio>/` (ex.: `historia/ep01/pag2_q1.jpg`). Não
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

O mensageiro do torneio ainda não tem concept. Gere uma referência antes (prompt no fim da
página) e anexe nos quadros dele.

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

O **selo negro** do convite aparece nos dois episódios: lacre de cera preta brilhante com
um símbolo dourado de duas chamas entrelaçadas. Se o selo do torneio já tiver outro desenho,
troque nos prompts `pag2_q3` (ep00) e `pag3_q3` (ep01).

---

## Prólogo — "O Pai que Partiu" (`historia/ep00/`)

Todos com o **estilo base + complemento de flashback**. Os irmãos têm 8 anos e o pai, 45.

### Página 1 (faixa no alto, dois quadros embaixo)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag1_q1.jpg` | horizontal 1536×1024 (vira faixa larga) | takatoshi_45, ryuga_8, koga_8 | Interior de um dojo tradicional de madeira, de tarde, luz entrando pelas janelas de papel, pergaminho com o kanji 光明 na parede. Plano geral: Takatoshi de pé no centro, braços cruzados, corrigindo os dois filhos em guarda de luta: Kōga à esquerda avançando impaciente, Ryūga à direita parado e atento. Toda a ação numa faixa horizontal no meio da imagem; o terço de cima é só a parede do dojo (espaço para balões). |
| `pag1_q2.jpg` | horizontal 1536×1024 | koga_8, takatoshi_45 | Kōga criança pulando para a frente com um soco reto, gritando, cabelo espetado voando, linhas de velocidade radiais atrás dele; a mão enorme de Takatoshi, com a luva sem dedos, defendendo em primeiro plano à direita. Câmera baixa e dinâmica. Canto de cima à esquerda livre para um balão de grito. |
| `pag1_q3.jpg` | horizontal 1536×1024 | ryuga_8 | Close do rosto de Ryūga criança, sério e concentrado, olhos fixos para a esquerda observando o irmão, uma gota de suor na têmpora. Rosto na metade de baixo da imagem; a metade de cima é sombra suave do dojo (espaço para o balão de pensamento). |

### Página 2 (quatro quadros)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag2_q1.jpg` | horizontal 1536×1024 (vira faixa bem larga) | dafu | Dafu chegando na porta do dojo, sorrindo de olhos fechados, segurando no alto uma bandeja com daifuku (doces de arroz redondos e brancos), a cabaça balançando na cintura. Ele fica no terço esquerdo da imagem, numa faixa horizontal no meio da altura; o resto é a parede de madeira do dojo, vazia (espaço para o balão). |
| `pag2_q2.jpg` | quadrado 1024×1024 | takatoshi_45, mensageiro | No portão de madeira da casa, ao entardecer com o sol baixo e alaranjado atrás, o mensageiro encapuzado estende um envelope com as duas mãos enluvadas para Takatoshi, que está de costas para a câmera em primeiro plano (o dragão azul bordado nas costas do colete bem visível). Silhuetas fortes contra o pôr do sol. Topo da imagem livre (céu) para a narração. |
| `pag2_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | — | Detalhe: envelope de papel grosso cor de marfim sobre uma mesa baixa de madeira escura, fechado por um lacre de cera preta brilhante com um símbolo dourado de duas chamas entrelaçadas. Sem nome escrito. Luz dramática vinda de cima, envelope no centro exato da imagem. |
| `pag2_q4.jpg` | horizontal 1536×1024 (vira faixa larga) | takatoshi_45 | Close extremo do rosto de Takatoshi olhando o envelope fora de quadro, metade do rosto na sombra, expressão grave de quem esperou muito tempo por aquilo. Rosto no terço direito da imagem, numa faixa no meio da altura; à esquerda só sombra (espaço para o balão de pensamento). |

### Página 3 (dois quadros no alto, faixa embaixo)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag3_q1.jpg` | horizontal 1536×1024 | ryuga_8, takatoshi_45 | Dentro de casa, Ryūga criança puxando com as duas mãos a barra do colete longo do pai, olhar preocupado para cima. Só a barra do colete (branca com o dragão azul) e a perna de Takatoshi aparecem à direita. Câmera na altura da criança. Terço de cima livre para o balão. |
| `pag3_q2.jpg` | horizontal 1536×1024 | koga_8 | Kōga criança pulando de alegria com os punhos para o alto, olhos brilhando de admiração, brilhos e estrelinhas em volta, fundo com raios de luz. Ele fica na metade de baixo da imagem; a de cima é livre para o balão de grito. |
| `pag3_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | takatoshi_45, ryuga_8, koga_8, dafu | Takatoshi ajoelhado no centro com uma mão na cabeça de cada filho (Ryūga à esquerda, Kōga à direita), sorriso tranquilo de despedida. Dafu atrás, à direita, de braços cruzados, sorrindo de olhos fechados com uma lágrima escorrendo. Todos numa faixa horizontal no meio da imagem, do peito para cima; o terço de cima é a parede da casa (espaço para dois balões, um de cada lado). |

### Página 4 (quadro único)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag4_q1.jpg` | horizontal 1536×1024 | takatoshi_45, dafu, ryuga_8, koga_8 | Plano geral ao pôr do sol: Takatoshi, pequeno e de costas (dragão do colete visível), caminhando por uma estrada de terra que some no horizonte, com uma trouxa de pano no ombro. Em primeiro plano, de costas para a câmera, no portão de madeira: Dafu com uma criança de cada lado (Ryūga e Kōga) olhando ele ir. Céu enorme laranja e roxo, longas sombras. Canto de cima à esquerda e canto de baixo à direita livres para a narração. |

---

## Episódio 1 — "Duas Cartas" (`historia/ep01/`)

Tempo presente (sem o complemento de flashback). Os irmãos têm 18 anos: são exatamente os
concepts p01 (Ryūga) e p03 (Kōga). Dafu usa o concept `dafu.png`.

### Página 1 (faixa no alto, dois quadros embaixo)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag1_q1.jpg` | horizontal 1536×1024 (vira faixa larga) | p01, p03, dafu | O mesmo dojo de madeira do Prólogo, dez anos depois, um pouco mais gasto, de manhã. Plano geral: Ryūga à esquerda e Kōga à direita, frente a frente em guarda de luta; Dafu sentado no chão ao fundo, no meio, comendo daifuku. Todos numa faixa horizontal no meio da imagem; o terço de cima é a parede do dojo. |
| `pag1_q2.jpg` | horizontal 1536×1024 | p03 | Close de Kōga com um sorriso provocador de canto de boca, estalando os dedos, olhando para a direita, capuz do colete caído nas costas. Rosto na metade de baixo da imagem; a de cima livre para o balão. |
| `pag1_q3.jpg` | horizontal 1536×1024 | p01 | Close de Ryūga, expressão calma e neutra, olhos semicerrados, ajustando a luva, olhando para a esquerda. Rosto na metade de baixo da imagem; a de cima livre para o balão. |

### Página 2 (quadro único)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag2_q1.jpg` | horizontal 1536×1024 | p01, p03, dafu | Splash de abertura de luta: Ryūga (à esquerda) e Kōga (à direita) em guarda, prontos para avançar um no outro, poeira subindo do chão do dojo, linhas de velocidade convergindo para o centro. Dafu no meio, ao fundo, com o braço erguido como juiz, gritando de boca aberta. Câmera baixa, composição simétrica. O quinto de cima é livre para o balão de grito. |

> Depois desta página vem a luta de treino (Ryūga + p02 contra Kōga). A arte não muda nada nela.

### Página 3 (quatro quadros)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag3_q1.jpg` | horizontal 1536×1024 (vira faixa bem larga) | p01, p03, dafu | Depois do treino: Ryūga e Kōga sentados no chão do dojo, suados e ofegantes, rindo um para o outro; Dafu de pé no meio, com as mãos na cintura, dando bronca de brincadeira. Todos numa faixa horizontal no meio da imagem; o resto é chão e parede de madeira. |
| `pag3_q2.jpg` | quadrado 1024×1024 | — | A porta de entrada da casa vista de dentro, de madeira e papel, com a sombra de uma mão batendo do lado de fora projetada no papel. Corredor escuro e silencioso. Centro da imagem livre para a onomatopeia. |
| `pag3_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | — | Detalhe: dois envelopes iguais cor de marfim, lado a lado no chão de madeira da entrada, cada um com o lacre de cera preta com o símbolo dourado de duas chamas entrelaçadas (o mesmo do Prólogo). Cada um tem um nome escrito com pincel (o texto pode ser ilegível). Envelopes no centro exato da imagem. |
| `pag3_q4.jpg` | horizontal 1536×1024 (vira faixa larga) | dafu | Close do rosto de Dafu olhando para baixo, para os envelopes fora de quadro: pela primeira vez os olhos estão abertos, arregalados, o sorriso sumiu, suor frio, sombra cobrindo a testa. Rosto no terço direito da imagem, numa faixa no meio da altura; à esquerda só sombra. |

### Página 4 (duas colunas)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag4_q1.jpg` | quadrado 1024×1024 | p01 | Ryūga de pé lendo a carta aberta nas mãos, do peito para cima, expressão séria e determinada, luz da janela atrás. Ele fica no meio da altura da imagem; o quarto de cima e o quarto de baixo são livres para dois balões. |
| `pag4_q2.jpg` | quadrado 1024×1024 | p03 | Kōga amassando a carta no punho fechado, dentes cerrados, olhar de raiva, veias saltando, energia escura e vermelha subindo em volta, dojo à noite ao fundo. Do peito para cima, no meio da altura; o terço de cima livre para o balão de grito. |

### Página 5 (dois quadros no alto, faixa embaixo)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag5_q1.jpg` | horizontal 1536×1024 | dafu, p01 | Na sala da casa, Dafu à esquerda, sem o sorriso de sempre, olhando preocupado para Ryūga à direita, que responde com um aceno firme de cabeça. Os dois da cintura para cima, no meio da altura; o terço de cima livre para dois balões. |
| `pag5_q2.jpg` | horizontal 1536×1024 | p03 | Kōga segurando a carta aberta e apontando uma linha do texto com o dedo, de lado para a câmera, olhar sério. Ele fica no meio da altura; o quarto de cima e o quarto de baixo são livres para dois balões. |
| `pag5_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | p01, p03 | Ryūga e Kōga de costas um para o outro na porta do dojo, ao pôr do sol, cada um olhando para um lado (Ryūga para a esquerda, Kōga para a direita), mochilas no ombro. Céu laranja e roxo enorme. Os dois numa faixa horizontal no meio da imagem; o terço de cima é céu livre para a narração. |

### Página 6 (quadro único)

| Arquivo | Formato | Anexos | Prompt |
|---|---|---|---|
| `pag6_q1.jpg` | horizontal 1536×1024 | dafu, takatoshi_45, ryuga_8, koga_8 | Noite. Dafu sozinho no dojo vazio e escuro, sentado no chão de costas para a câmera, ombros caídos, iluminado só pela lua que entra pela janela. Nas mãos, uma foto antiga emoldurada: Takatoshi (como no concept de 45 anos) com os dois filhos de 8 anos. Clima de culpa e silêncio. Ele fica no terço esquerdo da imagem; a parte de cima à direita é sombra livre para o balão de pensamento. |

---

## Referência do mensageiro (gerar antes da página 2 do Prólogo)

| Arquivo | Formato | Prompt |
|---|---|---|
| `mensageiro.png` | vertical 1024×1536 | Folha de referência de personagem no mesmo estilo dos concepts anexados (anexe um deles só como referência de traço): mensageiro misterioso, figura alta de manto e capuz pretos, rosto totalmente na sombra, luvas brancas impecáveis, segurando um envelope com lacre preto. Corpo inteiro de frente e de perfil, fundo cinza-claro liso. |
