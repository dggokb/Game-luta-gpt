# Prompts das imagens: Prólogo e Episódio 1

São 25 imagens: 11 do Prólogo (`ep00`) e 14 do Episódio 1 (`ep01`), mais as fichas de personagem. Para trocar a arte
provisória, salve cada imagem como **JPG** com o nome da tabela em
`android/app/src/main/assets/historia/<episódio>/` (ex.: `historia/ep01/pag2_q1.jpg`). Não
precisa recortar: o jogo enquadra a imagem no painel sozinho. Se o recorte cortar uma parte
importante, ajuste o `"foco"` do painel no `episodio.json` (0 = mostra o topo, 1 = mostra a base).

Para ver o resultado no PC sem abrir o jogo: `cd android && gradle :pc-preview:runHistoria`
(grava as páginas montadas em `android/build/historia/`).

## Como gerar no ChatGPT

1. **Uma conversa só para a história.** Anexe os concepts `art/concepts/p01.png` (Ryūga) e
   `art/concepts/p03.png` (Kōga) logo na primeira mensagem.
2. **Gere as fichas** (seção abaixo) antes dos quadros. Anexe a ficha certa sempre que o
   personagem aparecer, para ele sair igual em todos os quadros.
3. **Cole o estilo base antes de cada prompt.** Nos quadros do Prólogo, cole também o
   complemento de flashback.
4. **Tamanho:** use o formato da tabela (horizontal = 1536×1024, quadrado = 1024×1024).
   Os painéis muito largos (faixas) são um recorte do meio da imagem: o prompt já pede para
   a ação ficar na faixa central.
5. **Sem texto na imagem.** Os balões, a narração e as onomatopeias são colocados pelo
   jogo. O prompt pede espaço livre onde os balões entram.

### Estilo base (cole antes de cada prompt)

```
Quadro de mangá colorido no estilo de anime de luta dos anos 90 (Yu Yu Hakusho, Fatal Fury
anime, Street Fighter II V): traço de tinta preto e grosso, cores chapadas e vibrantes,
sombras duras, iluminação dramática, muito contraste, retícula leve nas sombras.
Personagens fiéis às fichas anexadas (mesmo rosto, cabelo, roupa e proporções).
Sem texto, sem letras, sem balões de fala, sem onomatopeias, sem molduras de quadrinho,
sem bordas, sem marca d'água. A imagem ocupa o quadro inteiro.
```

### Complemento de flashback (só no Prólogo)

```
Lembrança de dez anos atrás: cores desbotadas puxando para o sépia e o âmbar, luz quente e
suave, leve granulado de papel antigo.
```

## Fichas de personagem (gerar primeiro)

Takatoshi e Dafu ainda não têm concept: as descrições abaixo são **proposta** para validar.
Se vocês já tiverem outra ideia de visual, troque a descrição antes de gerar.

| Ficha | Formato | Prompt |
|---|---|---|
| `ficha_takatoshi.png` | vertical 1024×1536 | Folha de referência de personagem: Takatoshi Komyo, mestre de artes marciais japonês de uns 40 anos, alto, forte e sereno, ombros largos, cabelo preto longo preso num rabo baixo, barba curta bem aparada, olhar firme e calmo. Dogi azul-índigo sem mangas com faixa preta, bandagens vermelhas nos antebraços, calça larga escura. Ele é a soma dos dois filhos (anexos p01 e p03): traços do rosto dos dois. Corpo inteiro de frente e de perfil, pose neutra em pé, fundo branco liso, iluminação uniforme. |
| `ficha_dafu.png` | vertical 1024×1536 | Folha de referência de personagem: Dafu, homem chinês corpulento e bem-humorado de uns 40 anos, barriga grande mas braços fortes, rosto redondo com sorriso largo, cabeça raspada, bigode fino e caído. Túnica chinesa tradicional (changshan) cor de mostarda com botões de nó, calça preta e sapatos de pano. Corpo inteiro de frente e de perfil, pose neutra em pé, fundo branco liso, iluminação uniforme. |
| `ficha_dafu_hoje.png` | vertical 1024×1536 | A mesma ficha do Dafu (anexe `ficha_dafu.png`), dez anos mais velho: têmporas grisalhas no bigode, algumas rugas de riso, um pouco mais pesado. Mesma roupa, um pouco gasta, com um avental de cozinha por cima. |
| `ficha_irmaos_criancas.png` | horizontal 1536×1024 | Folha de referência: os irmãos gêmeos Ryūga e Kōga aos 7 anos (anexos p01 e p03 são eles adolescentes). Ryūga: mesmo cabelo castanho do p01, expressão séria e atenta, dogi branco de treino com faixa azul. Kōga: mesmo cabelo espetado do p03, sorriso travesso e cheio de energia, dogi branco de treino com faixa vermelha, curativo no joelho. Os dois de corpo inteiro, de frente, lado a lado, fundo branco liso. |
| `mensageiro.png` (só referência) | vertical 1024×1536 | Folha de referência: mensageiro misterioso, figura alta de manto e capuz pretos, rosto totalmente na sombra, luvas brancas impecáveis. Corpo inteiro, fundo branco liso. |

O **selo negro** do convite aparece nos dois episódios: lacre de cera preta brilhante com
um símbolo dourado de duas chamas entrelaçadas. Se o selo do torneio já tiver outro desenho,
troque nos prompts `pag2_q3` (ep00) e `pag3_q3` (ep01).

---

## Prólogo — "O Pai que Partiu" (`historia/ep00/`)

Todos com o **estilo base + complemento de flashback**.

### Página 1 (faixa no alto, dois quadros embaixo)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag1_q1.jpg` | horizontal 1536×1024 (vira faixa larga) | Interior de um dojo tradicional de madeira, de tarde, luz entrando pelas janelas de papel, pergaminho com o kanji 光明 na parede. Plano geral: Takatoshi (ficha) de pé no centro, braços cruzados, corrigindo os dois filhos de 7 anos (ficha das crianças) em guarda de luta: Kōga à esquerda avançando impaciente, Ryūga à direita parado e atento. Toda a ação numa faixa horizontal no meio da imagem; o terço de cima é só a parede do dojo (espaço para balões). |
| `pag1_q2.jpg` | horizontal 1536×1024 | Kōga criança (ficha) pulando para a frente com um soco reto, gritando, cabelo voando, linhas de velocidade radiais atrás dele, a palma enorme de Takatoshi defendendo em primeiro plano à direita. Câmera baixa e dinâmica. Deixe o canto de cima à esquerda livre para um balão de grito. |
| `pag1_q3.jpg` | horizontal 1536×1024 | Close do rosto de Ryūga criança (ficha), sério e concentrado, olhos fixos para a esquerda observando o irmão, uma gota de suor na têmpora. Rosto na metade de baixo da imagem; a metade de cima é sombra suave do dojo (espaço para o balão de pensamento). |

### Página 2 (quatro quadros)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag2_q1.jpg` | horizontal 1536×1024 (vira faixa bem larga) | Dafu (ficha) chegando na porta do dojo, rindo de boca aberta, segurando no alto uma bandeja com daifuku (doce de arroz redondo e branco). Ele fica no terço esquerdo da imagem, numa faixa horizontal no meio da altura; o resto é a parede de madeira do dojo, vazia (espaço para o balão). |
| `pag2_q2.jpg` | quadrado 1024×1024 | No portão de madeira da casa, ao entardecer com o sol baixo e alaranjado atrás, o mensageiro encapuzado (referência) estende um envelope com as duas mãos enluvadas para Takatoshi (ficha), que está de costas para a câmera em primeiro plano. Silhuetas fortes contra o pôr do sol. Topo da imagem livre (céu) para a narração. |
| `pag2_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | Detalhe: envelope de papel grosso cor de marfim sobre uma mesa baixa de madeira escura, fechado por um lacre de cera preta brilhante com um símbolo dourado de duas chamas entrelaçadas. Sem nome escrito. Luz dramática vinda de cima, envelope no centro exato da imagem. |
| `pag2_q4.jpg` | horizontal 1536×1024 (vira faixa larga) | Close extremo do rosto de Takatoshi (ficha) olhando o envelope fora de quadro, metade do rosto na sombra, expressão grave de quem esperou muito tempo por aquilo. Rosto no terço direito da imagem, numa faixa no meio da altura; à esquerda só sombra (espaço para o balão de pensamento). |

### Página 3 (dois quadros no alto, faixa embaixo)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag3_q1.jpg` | horizontal 1536×1024 | Dentro de casa, Ryūga criança (ficha) puxando a manga do dogi do pai com as duas mãos, olhar preocupado para cima. Só a manga e o braço de Takatoshi aparecem à direita. Câmera na altura da criança. Terço de cima livre para o balão. |
| `pag3_q2.jpg` | horizontal 1536×1024 | Kōga criança (ficha) pulando de alegria com os punhos para o alto, olhos brilhando de admiração, brilhos e estrelinhas em volta, fundo com raios de luz. Ele fica na metade de baixo da imagem; a de cima é livre para o balão de grito. |
| `pag3_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | Takatoshi (ficha) ajoelhado no centro com uma mão na cabeça de cada filho (Ryūga à esquerda, Kōga à direita, ficha das crianças), sorriso tranquilo de despedida. Dafu (ficha) atrás, à direita, de braços cruzados e olhos marejados. Todos numa faixa horizontal no meio da imagem, do peito para cima; o terço de cima é a parede da casa (espaço para dois balões, um em cada lado). |

### Página 4 (quadro único)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag4_q1.jpg` | horizontal 1536×1024 | Plano geral ao pôr do sol: Takatoshi (ficha), pequeno e de costas, caminhando por uma estrada de terra que some no horizonte, com uma trouxa de pano no ombro. Em primeiro plano, de costas para a câmera, no portão de madeira: Dafu com uma criança de cada lado (Ryūga e Kōga, ficha das crianças) olhando ele ir. Céu enorme laranja e roxo, longas sombras. Canto de cima à esquerda e canto de baixo à direita livres para a narração. |

---

## Episódio 1 — "Duas Cartas" (`historia/ep01/`)

Tempo presente (sem o complemento de flashback). Ryūga e Kōga aos 17 anos são exatamente os
concepts p01 e p03; Dafu usa a `ficha_dafu_hoje.png`.

### Página 1 (faixa no alto, dois quadros embaixo)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag1_q1.jpg` | horizontal 1536×1024 (vira faixa larga) | O mesmo dojo de madeira do Prólogo, dez anos depois, um pouco mais gasto, de manhã. Plano geral: Ryūga (p01) à esquerda e Kōga (p03) à direita, frente a frente em guarda de luta; Dafu (ficha hoje) sentado no chão ao fundo, no meio, comendo daifuku. Todos numa faixa horizontal no meio da imagem; o terço de cima é a parede do dojo. |
| `pag1_q2.jpg` | horizontal 1536×1024 | Close de Kōga (p03) com um sorriso provocador de canto de boca, estalando os dedos, olhando para a direita. Rosto na metade de baixo da imagem; a de cima livre para o balão. |
| `pag1_q3.jpg` | horizontal 1536×1024 | Close de Ryūga (p01), expressão calma e neutra, olhos semicerrados, ajustando a luva, olhando para a esquerda. Rosto na metade de baixo da imagem; a de cima livre para o balão. |

### Página 2 (quadro único)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag2_q1.jpg` | horizontal 1536×1024 | Splash de abertura de luta: Ryūga (p01, à esquerda) e Kōga (p03, à direita) em guarda, prontos para avançar um no outro, poeira subindo do chão do dojo, linhas de velocidade convergindo para o centro. Dafu (ficha hoje) no meio, ao fundo, com o braço erguido como juiz, gritando. Câmera baixa, composição simétrica. O quinto de cima é livre para o balão de grito. |

> Depois desta página vem a luta de treino (Ryūga + p02 contra Kōga). A arte não muda nada nela.

### Página 3 (quatro quadros)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag3_q1.jpg` | horizontal 1536×1024 (vira faixa bem larga) | Depois do treino: Ryūga (p01) e Kōga (p03) sentados no chão do dojo, suados e ofegantes, rindo um para o outro; Dafu (ficha hoje) de pé no meio, com as mãos na cintura, dando bronca de brincadeira. Todos numa faixa horizontal no meio da imagem; o resto é chão e parede de madeira. |
| `pag3_q2.jpg` | quadrado 1024×1024 | A porta de entrada da casa vista de dentro, de madeira e papel, com a sombra de uma mão batendo do lado de fora projetada no papel. Corredor escuro e silencioso. Centro da imagem livre para a onomatopeia. |
| `pag3_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | Detalhe: dois envelopes iguais cor de marfim, lado a lado no chão de madeira da entrada, cada um com o lacre de cera preta com o símbolo dourado de duas chamas entrelaçadas (o mesmo do Prólogo). Um tem o nome de Ryūga e o outro o de Kōga, escritos com pincel (o texto pode ser ilegível). Envelopes no centro exato da imagem. |
| `pag3_q4.jpg` | horizontal 1536×1024 (vira faixa larga) | Close do rosto de Dafu (ficha hoje) olhando para baixo, para os envelopes fora de quadro: o sorriso de sempre sumiu, olhos arregalados, suor frio, sombra cobrindo a metade de cima do rosto. Rosto no terço direito da imagem, numa faixa no meio da altura; à esquerda só sombra. |

### Página 4 (duas colunas)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag4_q1.jpg` | quadrado 1024×1024 | Ryūga (p01) de pé lendo a carta aberta nas mãos, do peito para cima, expressão séria e determinada, luz da janela atrás. Ele fica no meio da altura da imagem; o quarto de cima e o quarto de baixo são livres para dois balões. |
| `pag4_q2.jpg` | quadrado 1024×1024 | Kōga (p03) amassando a carta no punho fechado, dentes cerrados, olhar de raiva, veias saltando, energia escura e vermelha subindo em volta, dojo à noite ao fundo. Do peito para cima, no meio da altura; o terço de cima livre para o balão de grito. |

### Página 5 (dois quadros no alto, faixa embaixo)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag5_q1.jpg` | horizontal 1536×1024 | Na sala da casa, Dafu (ficha hoje) à esquerda, olhando preocupado para Ryūga (p01) à direita, que responde com um aceno firme de cabeça. Os dois da cintura para cima, no meio da altura; o terço de cima livre para dois balões. |
| `pag5_q2.jpg` | horizontal 1536×1024 | Kōga (p03) segurando a carta aberta e apontando uma linha do texto com o dedo, de lado para a câmera, olhar sério. Ele fica no meio da altura; o quarto de cima e o quarto de baixo são livres para dois balões. |
| `pag5_q3.jpg` | horizontal 1536×1024 (vira faixa larga) | Ryūga (p01) e Kōga (p03) de costas um para o outro na porta do dojo, ao pôr do sol, cada um olhando para um lado (Ryūga para a esquerda, Kōga para a direita), mochilas no ombro. Céu laranja e roxo enorme. Os dois numa faixa horizontal no meio da imagem; o terço de cima é céu livre para a narração. |

### Página 6 (quadro único)

| Arquivo | Formato | Prompt |
|---|---|---|
| `pag6_q1.jpg` | horizontal 1536×1024 | Noite. Dafu (ficha hoje) sozinho no dojo vazio e escuro, sentado no chão de costas para a câmera, ombros caídos, iluminado só pela lua que entra pela janela. Nas mãos, uma foto antiga emoldurada: Takatoshi com os dois filhos pequenos. Clima de culpa e silêncio. Ele fica no terço esquerdo da imagem; a parte de cima à direita é sombra livre para o balão de pensamento. |
