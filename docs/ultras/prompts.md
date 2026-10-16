# Ultras cinematográficos: imagens para gerar

Gerado por `tools/ultra/gerar_prompts.py` a partir de `tools/ultra/personagens.json` (mude lá e
rode de novo). O nome de cada Ultra fica no `ultra.json` do personagem
(`android/app/src/main/assets/ultras/<id>/ultra.json`, campo `"nome"`): troque à vontade.

## Como usar

1. **Uma conversa por personagem** no ChatGPT. Anexe o concept dele (`art/concepts/<concept>.png`,
   também no Drive) logo na primeira mensagem e diga: *"Este é o personagem. Vou pedir várias
   imagens dele, sempre com esta mesma aparência."*
2. Gere **primeiro o `corte`**. Se ficar bom, anexe o corte junto com o concept nos pedidos seguintes:
   ajuda o personagem a sair igual em todas.
3. Cole cada prompt como está. O formato vem no começo de cada item (retrato, quadrado ou
   paisagem): se o ChatGPT perguntar, escolha esse.
4. Salve com o **nome exato** de cada item (`corte.png`, `palco.png`, `olhos.png`, `carga.png`,
   `golpe.png`, `atingido.png`, `final.png`) numa pasta com o id do personagem, por exemplo
   `game-luta-gpt/ultras/p02/` no Drive, e me avise. Eu recorto e coloco no jogo
   (`tools/ultra/preparar_ultra.py`).
5. O **corte** precisa de fundo transparente. Se vier com fundo, peça *"refaça com fundo
   transparente (PNG)"*; se ainda assim vier com fundo branco liso, tudo bem: a ferramenta remove.

Não precisa gerar tudo de uma vez: cada imagem que chega substitui a arte provisória daquele
pedaço, e o resto continua funcionando.

## Onde cada imagem aparece

| Imagem | Onde | Formato |
|---|---|---|
| `corte` | Abertura: o personagem atravessa a tela com o nome do Ultra | retrato 2:3, fundo transparente |
| `palco` | Abertura: o fundo do Ultra | paisagem 3:2 |
| `olhos` | Página de mangá: faixa de cima com o nome | paisagem 3:2 (vira uma faixa bem fina) |
| `carga` | Página de mangá: o personagem carregando | quadrado |
| `golpe` | Página de mangá: o golpe vindo na câmera | paisagem 3:2 (vira faixa larga) |
| `atingido` | Página de mangá: o adversário sendo atingido | paisagem 3:2 (vira faixa larga) |
| `final` | Página de mangá: o golpe final, tela cheia (é onde entra o toque no tempo certo) | paisagem 16:9 |

## player_base — Explosão Solar
Concept para anexar: `art/concepts/p01.png`. Energia: dourada e alaranjada, como o sol.
A página de mangá deste Ultra já tem as 5 imagens; faltam só as da abertura.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador jovem de cabelo castanho espetado, colete azul e branco sem mangas, regata preta, faixa preta e vermelha na cintura, luvas pretas e vermelhas, tênis azul, vermelho e branco. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: de pé em base larga, punhos fechados, aura dourada subindo, olhar sério e determinado. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (dourada e alaranjada, como o sol). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um céu de entardecer em chamas douradas atrás de um templo de pedra, sol gigante no horizonte, pedras flutuando. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (dourada e alaranjada, como o sol). Formato horizontal.
```

## p02 — Dança da Garça
Concept para anexar: `art/concepts/p02.png`. Energia: azul-celeste, leve e curta, colada nas palmas.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutadora de kung fu de cabelo prateado curto com tiara preta, túnica sem mangas azul-petróleo com detalhes dourados, faixa laranja comprida na cintura, calça larga azul-marinho, luvas sem dedos pretas, tênis. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: em guarda baixa de garça, uma perna levemente erguida, mãos abertas em bico de garça, faixa laranja voando. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (azul-celeste, leve e curta, colada nas palmas). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um lago de águas calmas ao luar, com uma garça de energia azul gigante abrindo as asas no céu e pétalas voando. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (azul-celeste, leve e curta, colada nas palmas). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de lutadora de kung fu de cabelo prateado curto com tiara preta, túnica sem mangas azul-petróleo com detalhes dourados, faixa laranja comprida na cintura, calça larga azul-marinho, luvas sem dedos pretas, tênis, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (azul-celeste, leve e curta, colada nas palmas). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutadora de kung fu de cabelo prateado curto com tiara preta, túnica sem mangas azul-petróleo com detalhes dourados, faixa laranja comprida na cintura, calça larga azul-marinho, luvas sem dedos pretas, tênis. Cena: de pé numa perna só em pose de garça, braços abertos como asas, energia azul-celeste girando em espiral em volta dela, pétalas e água subindo. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutadora de kung fu de cabelo prateado curto com tiara preta, túnica sem mangas azul-petróleo com detalhes dourados, faixa laranja comprida na cintura, calça larga azul-marinho, luvas sem dedos pretas, tênis. Cena: a palma aberta dela vindo direto na direção da câmera em escorço exagerado, os dedos em bico de garça, o rosto concentrado ao fundo, rastro de energia azul. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (azul-celeste, leve e curta, colada nas palmas) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutadora de kung fu de cabelo prateado curto com tiara preta, túnica sem mangas azul-petróleo com detalhes dourados, faixa laranja comprida na cintura, calça larga azul-marinho, luvas sem dedos pretas, tênis. Cena: de perfil olhando para a direita, empurrando as duas palmas abertas para a frente numa base larga, uma explosão curta de energia azul-celeste em forma de garça acertando à queima-roupa, a faixa laranja voando para trás. Cenário: um lago de águas calmas ao luar, com uma garça de energia azul gigante abrindo as asas no céu e pétalas voando. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p03 — Fúria da Casa
Concept para anexar: `art/concepts/p03.png`. Energia: dourada e alaranjada, a mesma da família, mas explosiva.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador jovem de cabelo preto espetado, colete preto e vermelho com capuz, regata branca, calça cargo preta com fivelas vermelhas, luvas pretas, tênis vermelho e branco, expressão de raiva. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: inclinado para a frente, punho de trás puxado até o quadril, gritando, aura dourada explodindo. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (dourada e alaranjada, a mesma da família, mas explosiva). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: uma rua destruída à noite pegando fogo dourado, com a silhueta de um dojo de família em ruínas ao fundo. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (dourada e alaranjada, a mesma da família, mas explosiva). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de lutador jovem de cabelo preto espetado, colete preto e vermelho com capuz, regata branca, calça cargo preta com fivelas vermelhas, luvas pretas, tênis vermelho e branco, expressão de raiva, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (dourada e alaranjada, a mesma da família, mas explosiva). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador jovem de cabelo preto espetado, colete preto e vermelho com capuz, regata branca, calça cargo preta com fivelas vermelhas, luvas pretas, tênis vermelho e branco, expressão de raiva. Cena: curvado para a frente gritando de raiva, punhos cerrados, a aura dourada explodindo para cima como labaredas, chão rachando em volta. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador jovem de cabelo preto espetado, colete preto e vermelho com capuz, regata branca, calça cargo preta com fivelas vermelhas, luvas pretas, tênis vermelho e branco, expressão de raiva. Cena: o punho dele vindo direto na direção da câmera em escorço exagerado, o rosto furioso ao fundo, rastro de energia dourada e pedaços de chão voando. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (dourada e alaranjada, a mesma da família, mas explosiva) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador jovem de cabelo preto espetado, colete preto e vermelho com capuz, regata branca, calça cargo preta com fivelas vermelhas, luvas pretas, tênis vermelho e branco, expressão de raiva. Cena: de perfil olhando para a direita, dando UM soco gigante à queima-roupa com o corpo todo, o braço esticado, uma explosão de energia dourada saindo do punho e rasgando a imagem. Cenário: uma rua destruída à noite pegando fogo dourado, com a silhueta de um dojo de família em ruínas ao fundo. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p04 — Soco de Rua
Concept para anexar: `art/concepts/p04.png`. Energia: laranja-dourada, curta, só no impacto do soco.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador de rua musculoso de regata amarela, bandagens nas mãos, faixa vermelha na cabeça, calça cargo azul-marinho, tênis, sorriso confiante e debochado. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: em guarda de boxe, punhos na altura do queixo, sorrindo de lado, poeira no chão. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (laranja-dourada, curta, só no impacto do soco). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um beco de cidade à noite com luzes de neon laranja, grafite nas paredes, fumaça saindo de bueiros e chuva fina. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (laranja-dourada, curta, só no impacto do soco). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de lutador de rua musculoso de regata amarela, bandagens nas mãos, faixa vermelha na cabeça, calça cargo azul-marinho, tênis, sorriso confiante e debochado, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (laranja-dourada, curta, só no impacto do soco). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador de rua musculoso de regata amarela, bandagens nas mãos, faixa vermelha na cabeça, calça cargo azul-marinho, tênis, sorriso confiante e debochado. Cena: estalando os dedos e o pescoço com um sorriso debochado, energia laranja faiscando nos punhos enfaixados, poeira levantando do chão. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador de rua musculoso de regata amarela, bandagens nas mãos, faixa vermelha na cabeça, calça cargo azul-marinho, tênis, sorriso confiante e debochado. Cena: o punho enfaixado dele vindo direto na direção da câmera em escorço exagerado, sorriso de canto ao fundo, onda de choque laranja. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (laranja-dourada, curta, só no impacto do soco) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador de rua musculoso de regata amarela, bandagens nas mãos, faixa vermelha na cabeça, calça cargo azul-marinho, tênis, sorriso confiante e debochado. Cena: de perfil olhando para a direita, acertando UM soco de direita gigante à queima-roupa depois de uma cabeçada, a onda de choque laranja explodindo e estilhaçando o asfalto. Cenário: um beco de cidade à noite com luzes de neon laranja, grafite nas paredes, fumaça saindo de bueiros e chuva fina. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p05 — Eclipse
Concept para anexar: `art/concepts/p05.png`. Energia: roxa e escura como um eclipse, rastros de velocidade.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: ninja de máscara preta no rosto, cabelo preto espetado, roupa preta justa com tiras rasgadas e lenço longo vermelho-escuro voando, katana embainhada nas costas (nunca na mão), mãos vazias. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: agachado baixo, uma mão no chão, olhos brilhando acima da máscara, lenço voando. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (roxa e escura como um eclipse, rastros de velocidade). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um eclipse total no céu noturno, com um anel de luz roxa em volta do sol escuro, telhados japoneses e folhas voando. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (roxa e escura como um eclipse, rastros de velocidade). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de ninja de máscara preta no rosto, cabelo preto espetado, roupa preta justa com tiras rasgadas e lenço longo vermelho-escuro voando, katana embainhada nas costas (nunca na mão), mãos vazias, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (roxa e escura como um eclipse, rastros de velocidade). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: ninja de máscara preta no rosto, cabelo preto espetado, roupa preta justa com tiras rasgadas e lenço longo vermelho-escuro voando, katana embainhada nas costas (nunca na mão), mãos vazias. Cena: agachado baixo de costas para um eclipse gigante, olhos brilhando em roxo acima da máscara, sombras e rastros de velocidade girando em volta dele. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: ninja de máscara preta no rosto, cabelo preto espetado, roupa preta justa com tiras rasgadas e lenço longo vermelho-escuro voando, katana embainhada nas costas (nunca na mão), mãos vazias. Cena: a mão aberta dele em golpe de faca (mão vazia, sem arma) vindo direto na direção da câmera em escorço exagerado, olhos frios acima da máscara ao fundo, rastros roxos de velocidade. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (roxa e escura como um eclipse, rastros de velocidade) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: ninja de máscara preta no rosto, cabelo preto espetado, roupa preta justa com tiras rasgadas e lenço longo vermelho-escuro voando, katana embainhada nas costas (nunca na mão), mãos vazias. Cena: de perfil olhando para a direita, aparecendo do nada na frente do adversário e acertando UM golpe de mão vazia à queima-roupa, um anel de eclipse roxo explodindo atrás do impacto, a katana embainhada continua nas costas. Cenário: um eclipse total no céu noturno, com um anel de luz roxa em volta do sol escuro, telhados japoneses e folhas voando. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p06 — Abismo
Concept para anexar: `art/concepts/p06.png`. Energia: roxa e preta, sombras líquidas como tinta.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador misterioso de máscara cinza que cobre o rosto (só os olhos aparecem), cabelo roxo, streetwear preto e roxo largo com tiras soltas. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: de pé torto e relaxado, uma mão estendida de lado com os dedos soltos, sombras escorrendo do chão. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (roxa e preta, sombras líquidas como tinta). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um vazio escuro e infinito com uma espiral de sombra roxa no centro, mãos de sombra subindo do chão. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (roxa e preta, sombras líquidas como tinta). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de lutador misterioso de máscara cinza que cobre o rosto (só os olhos aparecem), cabelo roxo, streetwear preto e roxo largo com tiras soltas, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (roxa e preta, sombras líquidas como tinta). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador misterioso de máscara cinza que cobre o rosto (só os olhos aparecem), cabelo roxo, streetwear preto e roxo largo com tiras soltas. Cena: de pé torto com as duas mãos abertas erguidas devagar, sombras roxas e pretas escorrendo do chão e subindo pelo corpo, olhos brilhando atrás da máscara. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador misterioso de máscara cinza que cobre o rosto (só os olhos aparecem), cabelo roxo, streetwear preto e roxo largo com tiras soltas. Cena: as duas palmas abertas dele vindo na direção da câmera em escorço exagerado, sombra roxa líquida saindo das mãos, olhos atrás da máscara ao fundo. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (roxa e preta, sombras líquidas como tinta) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: lutador misterioso de máscara cinza que cobre o rosto (só os olhos aparecem), cabelo roxo, streetwear preto e roxo largo com tiras soltas. Cena: de perfil olhando para a direita, as duas palmas empurradas para a frente disparando um raio contínuo de sombra roxa e preta que atravessa a imagem e engole o horizonte. Cenário: um vazio escuro e infinito com uma espiral de sombra roxa no centro, mãos de sombra subindo do chão. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p07 — Herança
Concept para anexar: `art/concepts/p07.png`. Energia: dourada como ouro e moedas.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: mafioso grisalho elegante de terno risca de giz, chapéu fedora, sobretudo comprido aberto, luvas, sapato social, bigode. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: de pé ereto, tocando a aba do chapéu com dois dedos, sobretudo voando, sorriso frio. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (dourada como ouro e moedas). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um salão de cassino antigo à noite, com moedas de ouro voando, lustres de cristal e cédulas caindo como chuva. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (dourada como ouro e moedas). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de mafioso grisalho elegante de terno risca de giz, chapéu fedora, sobretudo comprido aberto, luvas, sapato social, bigode, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (dourada como ouro e moedas). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: mafioso grisalho elegante de terno risca de giz, chapéu fedora, sobretudo comprido aberto, luvas, sapato social, bigode. Cena: de pé ereto tocando a aba do chapéu com dois dedos, sobretudo voando para trás, moedas de ouro girando em volta dele, a sombra do chapéu escondendo os olhos. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: mafioso grisalho elegante de terno risca de giz, chapéu fedora, sobretudo comprido aberto, luvas, sapato social, bigode. Cena: a sola do sapato social dele vindo direto na direção da câmera num chute reto em escorço exagerado, o chapéu e o sorriso frio ao fundo, rastro dourado. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (dourada como ouro e moedas) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: mafioso grisalho elegante de terno risca de giz, chapéu fedora, sobretudo comprido aberto, luvas, sapato social, bigode. Cena: de perfil olhando para a direita, dando UM chute reto gigante com a perna de trás, o sobretudo abrindo como asas, uma explosão dourada de moedas no impacto, o chapéu continua na cabeça. Cenário: um salão de cassino antigo à noite, com moedas de ouro voando, lustres de cristal e cédulas caindo como chuva. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p08 — Sinfonia Escarlate
Concept para anexar: `art/concepts/p08.png`. Energia: vermelho-escarlate, rastros do chicote como fitas de luz.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: dama elegante de cabelo rosa comprido, vestido vermelho e preto longo com fenda, luvas pretas, salto-agulha, chicote trançado vermelho na mão. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: de pé elegante, queixo erguido, chicote enrolado na mão, cabelo e vestido voando. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (vermelho-escarlate, rastros do chicote como fitas de luz). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um teatro de ópera vazio iluminado por um holofote vermelho, cortinas escarlates rasgadas e pétalas de rosa voando. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (vermelho-escarlate, rastros do chicote como fitas de luz). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de dama elegante de cabelo rosa comprido, vestido vermelho e preto longo com fenda, luvas pretas, salto-agulha, chicote trançado vermelho na mão, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (vermelho-escarlate, rastros do chicote como fitas de luz). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: dama elegante de cabelo rosa comprido, vestido vermelho e preto longo com fenda, luvas pretas, salto-agulha, chicote trançado vermelho na mão. Cena: girando no lugar com o chicote formando um círculo de luz escarlate em volta dela, cabelo rosa e vestido voando, olhar frio e confiante. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: dama elegante de cabelo rosa comprido, vestido vermelho e preto longo com fenda, luvas pretas, salto-agulha, chicote trançado vermelho na mão. Cena: a ponta do chicote dela vindo direto na direção da câmera em escorço exagerado, ela ao fundo com o braço esticado, rastro escarlate em espiral. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (vermelho-escarlate, rastros do chicote como fitas de luz) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: dama elegante de cabelo rosa comprido, vestido vermelho e preto longo com fenda, luvas pretas, salto-agulha, chicote trançado vermelho na mão. Cena: de perfil olhando para a direita, estalando UMA chicotada final gigante que corta a imagem de lado a lado, um rastro escarlate enorme e pétalas de rosa explodindo. Cenário: um teatro de ópera vazio iluminado por um holofote vermelho, cortinas escarlates rasgadas e pétalas de rosa voando. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p09 — Resgate Final
Concept para anexar: `art/concepts/p09.png`. Energia: fogo laranja e vermelho.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeiro ruivo enorme e musculoso, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso confiante. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: de pé largo, braços abertos prontos para agarrar, chamas atrás dele, sorriso confiante. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (fogo laranja e vermelho). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um prédio em chamas à noite, com fumaça, faíscas e uma escada de bombeiro ao fundo. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (fogo laranja e vermelho). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de bombeiro ruivo enorme e musculoso, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso confiante, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (fogo laranja e vermelho). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeiro ruivo enorme e musculoso, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso confiante. Cena: de pé largo, peito estufado, braços abertos, chamas laranja enormes subindo atrás dele, brasas voando. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeiro ruivo enorme e musculoso, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso confiante. Cena: as duas mãos enluvadas dele vindo direto na direção da câmera para agarrar, em escorço exagerado, o sorriso ao fundo, fogo em volta. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (fogo laranja e vermelho) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeiro ruivo enorme e musculoso, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso confiante. Cena: de perfil olhando para a direita, saltando alto e batendo o adversário (uma silhueta escura) no chão com uma pancada enorme, uma coluna de fogo e destroços explodindo do chão. Cenário: um prédio em chamas à noite, com fumaça, faíscas e uma escada de bombeiro ao fundo. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p10 — Resgate Aéreo
Concept para anexar: `art/concepts/p10.png`. Energia: vapor e água azul-clara.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeira loira atlética de rabo de cavalo, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso provocador. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: no ar em pleno salto, uma perna erguida, rabo de cavalo voando, vapor em volta. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (vapor e água azul-clara). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um céu azul com nuvens de vapor e jatos de água de mangueira formando arcos, gotas brilhando ao sol. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (vapor e água azul-clara). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de bombeira loira atlética de rabo de cavalo, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso provocador, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (vapor e água azul-clara). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeira loira atlética de rabo de cavalo, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso provocador. Cena: pulando no lugar provocando, vapor azul-claro saindo das botas, jatos de água girando em volta dela, rabo de cavalo voando. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeira loira atlética de rabo de cavalo, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso provocador. Cena: a bota dela vindo direto na direção da câmera num chute voador em escorço exagerado, ela ao fundo sorrindo, rastro de vapor e gotas. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (vapor e água azul-clara) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: bombeira loira atlética de rabo de cavalo, colete vermelho de bombeiro com faixas refletivas amarelas, calça de bombeiro, botas, luvas, sorriso provocador. Cena: de perfil olhando para a direita, descendo do alto com UM chute de machado gigante, uma explosão de vapor e água azul-clara no impacto. Cenário: um céu azul com nuvens de vapor e jatos de água de mangueira formando arcos, gotas brilhando ao sol. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p11 — Autópsia
Concept para anexar: `art/concepts/p11.png`. Energia: névoa verde de veneno.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: médico grisalho e magro de jaleco branco comprido manchado de sangue, luvas cirúrgicas, expressão fria e sorriso sádico, mãos vazias (sem bisturi nem seringa na mão). Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: de pé ereto e calmo, ajeitando as luvas, jaleco balançando, névoa verde no chão. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (névoa verde de veneno). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: uma sala de cirurgia escura com a luz da mesa de operação acesa, azulejos verdes, névoa verde venenosa no chão. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (névoa verde de veneno). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de médico grisalho e magro de jaleco branco comprido manchado de sangue, luvas cirúrgicas, expressão fria e sorriso sádico, mãos vazias (sem bisturi nem seringa na mão), olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (névoa verde de veneno). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: médico grisalho e magro de jaleco branco comprido manchado de sangue, luvas cirúrgicas, expressão fria e sorriso sádico, mãos vazias (sem bisturi nem seringa na mão). Cena: de pé calmo ajeitando as luvas cirúrgicas, uma névoa verde venenosa subindo do chão em volta dele, a luz da sala de cirurgia por trás. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: médico grisalho e magro de jaleco branco comprido manchado de sangue, luvas cirúrgicas, expressão fria e sorriso sádico, mãos vazias (sem bisturi nem seringa na mão). Cena: os dedos esticados dele em golpe de lança (mão vazia) vindo direto na direção da câmera em escorço exagerado, olhar frio ao fundo, rastro verde. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (névoa verde de veneno) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: médico grisalho e magro de jaleco branco comprido manchado de sangue, luvas cirúrgicas, expressão fria e sorriso sádico, mãos vazias (sem bisturi nem seringa na mão). Cena: de perfil olhando para a direita, acertando UMA estocada de dedos à queima-roupa num ponto exato do peito do adversário (uma silhueta escura), uma explosão de veneno verde saindo do ponto, mãos vazias. Cenário: uma sala de cirurgia escura com a luz da mesa de operação acesa, azulejos verdes, névoa verde venenosa no chão. Câmera dramática de cinema, a imagem mais épica do Ultra.
```

## p12 — Último Suspiro
Concept para anexar: `art/concepts/p12.png`. Energia: vermelho-sangue e preto, aura feral.

**`corte.png`** (retrato 2:3, fundo transparente)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: criatura morta-viva de pele cinza costurada, bandagens soltas pelo corpo, postura curvada e contorcida, olhos brilhando, boca aberta mostrando os dentes. Corpo inteiro, pose heroica de quem vai soltar o golpe mais forte: curvado de quatro apoios, rugindo para cima, bandagens voando, aura vermelha. Virado para a DIREITA (de três quartos), olhando para a direita. Em volta do corpo, a energia do personagem (vermelho-sangue e preto, aura feral). FUNDO TRANSPARENTE (PNG): sem chão, sem cenário, sem sombra no chão.
```

**`palco.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Cenário de fundo, SEM nenhum personagem e sem pessoas: um cemitério em ruínas sob uma lua vermelha gigante, névoa vermelha e corvos voando. Composição centralizada e dramática, com linhas de energia irradiando do centro, na cor da energia do Ultra (vermelho-sangue e preto, aura feral). Formato horizontal.
```

**`olhos.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Close extremo nos olhos de criatura morta-viva de pele cinza costurada, bandagens soltas pelo corpo, postura curvada e contorcida, olhos brilhando, boca aberta mostrando os dentes, olhar furioso e determinado. Os dois olhos ocupam uma faixa horizontal fina no centro exato da imagem; acima e abaixo, só sombra. Na íris, o reflexo da energia do personagem (vermelho-sangue e preto, aura feral). Fundo escuro.
```

**`carga.png`** (quadrado)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: criatura morta-viva de pele cinza costurada, bandagens soltas pelo corpo, postura curvada e contorcida, olhos brilhando, boca aberta mostrando os dentes. Cena: curvado rugindo para o céu, costuras se abrindo, aura vermelho-sangue explodindo em volta, bandagens voando. Corpo inteiro, de frente para a câmera, câmera de baixo para cima.
```

**`golpe.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: criatura morta-viva de pele cinza costurada, bandagens soltas pelo corpo, postura curvada e contorcida, olhos brilhando, boca aberta mostrando os dentes. Cena: a garra aberta dele vindo direto na direção da câmera em escorço exagerado, o rosto rugindo ao fundo, rastro vermelho-sangue. Composição horizontal com o golpe no centro, linhas de velocidade radiais.
```

**`atingido.png`** (paisagem 3:2)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Um lutador genérico de roupa escura, em silhueta e sem rosto definido, sendo atingido por um golpe e arremessado para a direita, corpo dobrado para trás, estrela de impacto branca no peito, a energia do golpe (vermelho-sangue e preto, aura feral) explodindo no ponto do impacto, saliva e suor voando, linhas de velocidade horizontais. Composição horizontal com o corpo no centro.
```

**`final.png`** (paisagem 16:9)

```
Ilustração de anime de luta moderno, no nível de Dragon Ball FighterZ, Guilty Gear Strive e Garou: Mark of the Wolves. Traço de tinta grosso e preto, cores vibrantes e saturadas, sombras duras com cel shading, iluminação dramática de cinema, muito contraste, linhas de velocidade e energia desenhadas à mão. O personagem é EXATAMENTE o do concept anexado: mesmo rosto, cabelo, roupa, cores e proporções. Sem texto, sem letras, sem balões de fala, sem molduras de quadrinho, sem marca d'água, sem logotipo. Personagem: criatura morta-viva de pele cinza costurada, bandagens soltas pelo corpo, postura curvada e contorcida, olhos brilhando, boca aberta mostrando os dentes. Cena: de perfil olhando para a direita, dando UMA pancada gigante com os dois braços de cima para baixo em modo berserker, uma explosão vermelho-sangue e o chão afundando. Cenário: um cemitério em ruínas sob uma lua vermelha gigante, névoa vermelha e corvos voando. Câmera dramática de cinema, a imagem mais épica do Ultra.
```
