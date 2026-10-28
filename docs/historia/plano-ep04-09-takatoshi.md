# Plano dos episódios 4 a 9 e do Takatoshi (D03)

Pedido da auditoria (D03, F11): hoje o ep04–ep09 não tem nenhuma imagem (as pastas
`historia/ep04` a `historia/ep09` têm só o `episodio.json`). O jogo mostra os painéis provisórios
montados pela arte provisória. A Arena VIP e a Arena do Templo não existem, e o Arcade termina no
cartão "TAKATOSHI — Em breve". Este documento lista a arte que falta por episódio, em ordem de
prioridade, com um prompt por imagem. Também traz a ficha proposta do Takatoshi como chefe final e
a lista dos vídeos dele.

Nada aqui está no jogo. As falas são as do `episodio.json` de hoje. O que é proposta nova (as
páginas de transição, o ep10, a história do contrato e os golpes do Takatoshi) está marcado
**proposta** e espera aprovação.

## Resumo da arte que falta

| # | Peça | Imagens | Onde entra | Prioridade |
|---|---|---|---|---|
| 1 | ep04 — O Sorteio | 3 páginas | antes das quartas | **P1**: a primeira página do torneio, vista por todo jogador |
| 2 | ep06 — Semifinal | 2 páginas (splash, uma por irmão) | antes da semifinal | **P1**: uma página só por campanha |
| 3 | ep08 — A Final | 1 página | antes da final | **P1**: uma página |
| 4 | ep05 — Fora da Arena | 4 páginas | antes e depois da emboscada | **P2** |
| 5 | ep09 — A Regra Escondida | 3 páginas | antes do duelo | **P2**: o gancho do chefe |
| 6 | Arena VIP (`arena_vip`) | 9 peças de cenário | duelo 1x1 do ep09 (hoje usa o Templo da Lua) | **P2** |
| 7 | ep07 — Sob a Mesma Lua | 8 páginas e as peças do Terraço da Lua | o bônus BROTHERS UNITED | **P3**: o mais longo; o provisório já conta a história |
| 8 | Transições (proposta) | 4 páginas curtas | depois das quartas, da semifinal, da final e do duelo | **P3** |
| 9 | Takatoshi | Base, imagens iniciais, ~40 vídeos, imagens do Ultra | o chefe final | **P4**: o maior trabalho |
| 10 | Arena do Templo (`arena_templo`) | 9 peças de cenário | a luta do Takatoshi | **P4**, junto com ele |
| 11 | ep10 — O Campeão (proposta) | 6 a 8 páginas | depois do ep09 | **P4**, junto com ele |

São 21 páginas no ep04–ep09 (as páginas com `"irmao"` contam duas vezes: uma do Ryūga e uma do
Kōga). Com as transições, 25.

Ordem de trabalho: P1 inteiro (6 páginas, um dia de geração), depois P2 (7 páginas e a VIP).
O ep07 vem depois porque o motor e as páginas provisórias já contam o episódio. O Takatoshi vem
por último, com o cenário e o ep10 dele.

## Como gerar as páginas

Cada página é uma **página pronta** ([motor-paginas.md](motor-paginas.md)):
- uma imagem em pé, 1024×1536, com todos os quadros, separados por divisas brancas;
- balões brancos e lisos;
- recortada por `tools/historia/recortar_pagina.py`.

Os nomes seguem o `episodio.json` de hoje: `pag1`, `pag2_ryuga`, `pag2_koga` e assim por diante.
O recorte troca os painéis provisórios do passo pela página.

Numa conversa só para a história, cole primeiro o **estilo base** de
[prompts-ep00-ep01.md](prompts-ep00-ep01.md#estilo-base) **trocando as duas últimas linhas** pelo
complemento abaixo. O estilo base pede "sem balões e sem molduras", que é a regra dos quadros
avulsos; na página pronta os balões e as divisas fazem parte da imagem.

```
Página de mangá em pé, 1024×1536, com os quadros descritos abaixo, na ordem de leitura
(de cima para baixo, da esquerda para a direita). Divisas BRANCAS e largas entre os quadros;
nenhum desenho (cabelo, braço, efeito, onomatopeia) passa por cima de uma divisa. Balões de
fala brancos e lisos, sem degradê nem textura, com contorno preto fino e o texto em
português dentro, em letras de mangá pretas. Onomatopeias à vontade, desenhadas na arte, sem
passar das divisas. Sem marca d'água, sem número de página.
```

### Concepts para anexar

Anexe **só os de quem aparece na página** (concepts demais misturam as roupas).

| Quem | Arquivo | Pontos que o GPT costuma errar |
|---|---|---|
| Ryūga (18) | `art/concepts/p01.png` | colete azul e branco sem mangas com gola vermelha, regata preta, calça branca com faixa azul |
| Kōga (18) | `art/concepts/p03.png` | colete preto com capuz e forro vermelho, regata branca, calça cargo preta com tiras vermelhas, cabelo espetado |
| Yin-lu | `art/concepts/p02.png` | parceira do Ryūga (ep09) |
| Brucy | `art/concepts/p04.png` | parceiro do Kōga (ep09) |
| Mensageiro | a página 6 do Prólogo (`historia/ep00`) | chapéu e sobretudo pretos, rosto na sombra do chapéu |
| Magnatas | sem concept: descrição no prompt | só silhuetas: o jogo os chama de "???" |
| Agentes mascarados | sem concept: descrição no prompt (**proposta**) | |
| Ilha e arenas | `art/concepts/cenarios/*.jpg` | só como referência de clima e arquitetura |
| Takatoshi (55) | `art/concepts/takatoshi_55.png` | cabelo longo ondulado castanho-escuro com mechas grisalhas, barba cheia, quimono preto sem mangas e rasgado, avental cinza em camadas rasgadas, faixa cinza larga com nó na frente, calça preta larga, luvas pretas enroladas sem dedos, **geta** de madeira |
| Takatoshi (45) | `art/concepts/takatoshi_45.png` | só em flashback (sépia) |
| Dafu | `art/concepts/dafu.png` | coque, bigode fino e comprido, olhos sempre fechados de tanto sorrir, colete mostarda |

**Agentes (proposta, para todos os prompts):** "agentes do torneio: macacão tático preto justo,
colete preto, luvas pretas, máscara branca lisa sem rosto com um dragão vermelho pintado na testa
(o mesmo dragão do selo dos convites), todos iguais". A máscara branca casa com o selo vermelho
do convite e se lê bem em silhueta na noite. Quando os agentes virarem lutadores, este é o concept
a desenhar.

**Magnatas (para todos os prompts):** "magnatas em silhueta preta contra a luz de um camarote
de luxo; só o brilho dos óculos, dos anéis e das taças aparece; nenhum rosto visível".

**Cenas que se repetem:**

| Cena (`cena` no provisório) | Descrição para o prompt |
|---|---|
| `templo` | o salão do templo do torneio no pico da ilha: colunas vermelhas, estandartes vermelhos com o emblema do torneio, braseiros acesos, piso de pedra escura; ver `arena_templo.jpg` |
| `chuva` | o caminho de pedra nos penhascos à noite, chuva forte, lanternas de pedra, mar lá embaixo; ver `caminho_penhascos.jpg` |
| `terraco` | um terraço de pedra silencioso no alto da ilha, à noite, lua cheia enorme, a ilha acesa lá embaixo; ver a seção 5b de [ilha-do-torneio.md](../../tools/stage/ilha-do-torneio.md) |
| `magnatas` | um camarote de luxo escuro com cortinas vermelhas, vidro dando para a ilha à noite; ver `arena_vip.jpg` |

## P1 — ep04, ep06, ep08

### ep04 — O Sorteio

**`pag1`** (faixa-e-dois) — anexar p01, p03 e a página 6 do Prólogo
```
Quadro 1, faixa larga no terço de cima: o grande salão do templo do torneio, visto de longe;
oito duplas de lutadores em fila diante de um altar com braseiros e estandartes vermelhos;
Ryūga (de costas, à esquerda) e Kōga (de costas, à direita) em lados opostos da fila.
Caixa de narração no canto: "O templo do torneio. Oito duplas. Um único desejo."
Quadro 2, embaixo à esquerda: close do mensageiro de chapéu e sobretudo pretos, rosto na
sombra do chapéu, segurando uma urna de madeira preta com o dragão vermelho. Balão:
"Cada convite é um capitão. A chave será sorteada agora."
Quadro 3, embaixo à direita: close do Kōga de perfil, sorriso de canto, olhando para o lado
do Ryūga. Balão: "Nos vemos na final, Ryūga."
```

**`pag2_ryuga`** (splash) — anexar p01
```
Página inteira, um quadro só: Ryūga de corpo inteiro na frente do quadro da chave do torneio
pendurado no salão (oito vagas, o nome dele numa das pontas), punho fechado, olhar firme, a
luz dos braseiros por trás. Balão: "Três lutas até a final. Pai... eu vou chegar até você."
```

**`pag2_koga`** (splash) — anexar p03
```
Página inteira, um quadro só: Kōga de corpo inteiro, de frente, socando a palma da mão
diante do quadro da chave do torneio, o colete aberto voando, sorriso agressivo, a luz dos
braseiros por trás. Balão: "Quem estiver no meu caminho vai cair!"
```

### ep06 — Semifinal

**`pag1_ryuga`** (splash) — anexar p01 e p02
```
Página inteira: Ryūga e Yin-lu entrando na Arena Imperial por um portão de pedra, a arena
cheia e acesa à frente deles; Ryūga olha para trás, para o alto, como se sentisse alguém.
Caixa de narração no alto: "Semifinal." Balão do Ryūga: "Kōga também está avançando. Eu sinto."
```

**`pag1_koga`** (splash) — anexar p03 e p04
```
Página inteira: Kōga e Brucy entrando na Arena Imperial por um portão de pedra, Kōga
apontando para o alto da ilha (o templo lá em cima), Brucy estalando os dedos.
Caixa de narração no alto: "Semifinal." Balão do Kōga: "Ryūga, me espera na final!"
```

### ep08 — A Final

**`pag1`** (duas-colunas) — anexar p01 e p03
```
Quadro 1, coluna da esquerda, de cima a baixo: close do Ryūga no centro da Arena Central,
sério, a lua cheia atrás, luz azul. Balão: "Kōga. Não precisava ser assim."
Quadro 2, coluna da direita: close do Kōga espelhando o Ryūga, cerrando o punho, luz
vermelha. Balão: "Precisava sim! Só um de nós chega ao fim!"
Os dois quadros juntos formam um frente a frente: o Ryūga olha para a direita e o Kōga para a
esquerda.
```

## P2 — ep05, ep09 e a Arena VIP

### ep05 — Fora da Arena

**`pag1`** (faixa-e-dois)
```
Quadro 1, faixa de cima: o caminho de pedra nos penhascos à noite, chuva forte, lanternas
de pedra acesas, duas figuras pequenas (Ryūga e o parceiro) andando de costas para o leitor.
Caixa de narração: "Noite. Longe da arena, no caminho de volta ao alojamento."
Quadro 2, embaixo à esquerda: no alto de uma pedra, quatro silhuetas de lutadores contra um
relâmpago, um deles gritando. Balão espetado: "Ei! A vaga de vocês na semifinal... é nossa!"
Quadro 3, embaixo à direita: close de um sorriso cruel na chuva, rosto meio na sombra.
Balão: "Se a gente derrubar vocês aqui, os organizadores vão ter que trocar a chave!"
```
Os emboscadores mudam a cada jogo (é a dupla que perdeu a outra quarta), então ficam em
silhueta.

**`pag2_ryuga`** (splash) — anexar p01 e p02
```
Página inteira: Ryūga e Yin-lu de costas um para o outro, em guarda, na chuva, a água
escorrendo pelo rosto, as silhuetas dos emboscadores em volta. Balão do Ryūga:
"Vocês perderam na arena. Vão perder aqui também."
```

**`pag2_koga`** (splash) — anexar p03 e p04
```
Página inteira: Kōga avançando contra a chuva com um sorriso largo, Brucy atrás arregaçando
as mangas, as silhuetas dos emboscadores em volta. Balão do Kōga: "Finalmente uma briga de
verdade!"
```

**`pag3`** (duas-colunas) — anexar a página 6 do Prólogo
```
Quadro 1, coluna da esquerda: close do mensageiro de chapéu e sobretudo pretos, num
corredor do templo, sorriso fino. Balão: "Os organizadores assistiram a tudo. Adoraram o
espetáculo."
Quadro 2, coluna da direita: a chave do torneio pendurada no salão vazio, o nome da dupla
do jogador ainda na vaga da semifinal. Caixa de narração: "A chave não muda. A vaga na
semifinal continua sua."
```

### ep09 — A Regra Escondida

**`pag1`** (faixa-e-dois) — anexar a página 6 do Prólogo, p01 e p03
```
Quadro 1, faixa de cima: o salão VIP de luxo, piscinas com luz vermelha, os campeões no
centro do ringue de pedra preta; o mensageiro aplaude devagar, os magnatas em silhueta nos
camarotes. Balão: "Parabéns, campeões. Mas o desejo não pertence a uma dupla."
Quadro 2, embaixo à esquerda: close do mensageiro levantando um dedo. Balão: "Só um de
vocês desafia o Campeão."
Quadro 3, embaixo à direita: os dois parceiros se olhando de lado, de costas um para o outro,
a sombra deles cruzada no piso. Caixa de narração: "A dupla campeã luta entre si. Um contra um."
```
No quadro 3 os parceiros aparecem de costas porque a dupla campeã pode ser outra no Arcade. No
modo história é o Ryūga com a Yin-lu ou o Kōga com o Brucy, e as duas páginas seguintes já
mostram o rosto.

**`pag2_ryuga`** (duas-colunas) — anexar p01 e p02
```
Quadro 1, coluna da esquerda: close da Yin-lu, triste e firme, a luz vermelha da piscina
no rosto. Balão: "Então é isso... um contra o outro."
Quadro 2, coluna da direita: close do Ryūga de olhos baixos, apertando a faixa preta.
Balão: "Desculpa. Eu preciso chegar até o meu pai."
```

**`pag2_koga`** (duas-colunas) — anexar p03 e p04
```
Quadro 1, coluna da esquerda: close do Brucy rindo, estalando o pescoço. Balão: "Hah!
Sempre quis saber quem é o mais forte de nós dois."
Quadro 2, coluna da direita: close do Kōga com um sorriso feroz, punho erguido. Balão:
"Não vou segurar nada!"
```

### Arena VIP

Os prompts das 9 peças já estão na seção 6 de
[ilha-do-torneio.md](../../tools/stage/ilha-do-torneio.md#6-arena-vip-arena_vip-a-final-da-tag-o-duelo-1x1).
Com a VIP pronta, ponha `"cenario": "arena_vip"` no passo `duelo` do ep09 (hoje o duelo cai no
Templo da Lua). Os magnatas assistindo entram depois como `atores` nos camarotes.

## P3 — ep07 e as transições

### ep07 — Sob a Mesma Lua (8 páginas)

Peças do Terraço da Lua: seção 5b de
[ilha-do-torneio.md](../../tools/stage/ilha-do-torneio.md). O cenário provisório usa peças da
Central. Concepts: p01 e p03 em quase todas as páginas.

**`pag1`** (faixa-e-dois)
```
Quadro 1, faixa de cima: um terraço de pedra silencioso no alto da ilha, à noite, lua cheia
enorme, a ilha acesa lá embaixo; Ryūga sentado sozinho na mureta, de costas. Caixa de
narração: "A noite antes da final. Um terraço silencioso da ilha, sob a lua cheia."
Quadro 2, embaixo à esquerda: close do Kōga chegando pelas costas, mãos nos bolsos, meio
sorriso. Balão: "Heh... então você também gosta de ficar olhando para a lua? Eu também. Dá
um relaxamento bom."
Quadro 3, embaixo à direita: close do Ryūga virando o rosto, surpreso. Balão: "Não sabia
que você gostava disso, Kōga..."
```

**`pag2`** (tres-faixas)
```
Faixa 1: em sépia, como lembrança: os dois irmãos com 8 anos treinando lado a lado no
dojo Shūshinkan, o pai de costas observando (anexar ryuga_8, koga_8 e takatoshi_45). Caixa de
narração: "Em silêncio, os dois lembram dos tempos em que treinavam juntos no dojo de Takatoshi."
Faixa 2: close do Ryūga olhando a lua. Balão: "Kōga... você acha que nós dois estamos fazendo
a coisa certa?"
Faixa 3: close do Kōga de olhos fechados. Balão: "Quem é que sabe o que é certo ou errado,
Ryūga? Temos nossas convicções. E eu nunca vou tentar tirar isso de você."
```
A faixa 1 é um flashback: pede "cores sépia, borda preta" só nela (complemento de flashback do
Prólogo).

**`pag3`** (dois-e-faixa)
```
Quadro 1, em cima à esquerda: close do Ryūga sorrindo de leve. Balão: "Eu também não, irmão..."
Quadro 2, em cima à direita: close do Kōga, sorriso de desafio. Balão: "Mas amanhã não vou
pegar leve."
Quadro 3, faixa de baixo: os dois sentados na mureta lado a lado, de costas, contra a lua
cheia. Balão do Ryūga: "Hehe... nem eu."
```

**`pag4`** (duas-colunas)
```
Quadro 1, coluna da esquerda: um camarote de luxo escuro, um magnata em silhueta preta
diante de um vidro, olhando um telão que mostra os dois irmãos no terraço; só o brilho dos
óculos e de uma taça. Balão: "Que cena bonita... Dois irmãos, tão tranquilos na véspera de se
enfrentarem."
Quadro 2, coluna da direita: outro magnata em silhueta, girando um anel no dedo, sorriso
de dentes brancos no escuro. Balão: "Seria uma pena chegarem descansados à final. Vamos
apimentar um pouco nosso espetáculo."
```

**`pag5`** (faixa-e-dois)
```
Quadro 1, faixa de cima: agentes mascarados (macacão tático preto, máscara branca lisa sem
rosto com um dragão vermelho na testa) saltando dos acessos do terraço e cercando os dois
irmãos sob a lua. Caixa de narração: "Agentes mascarados surgem dos acessos e cercam os irmãos."
Quadro 2, embaixo à esquerda: close do Ryūga se levantando, em guarda. Balão: "Kōga...
parece que temos companhia."
Quadro 3, embaixo à direita: close do Kōga estalando os dedos, empolgado. Balão: "Heh... faz
tempo que não lutamos juntos, irmãozinho!"
```

**`pag6`** (splash)
```
Página inteira: Ryūga e Kōga de costas um para o outro, em guarda, no centro do terraço,
a lua cheia enorme atrás deles, os agentes mascarados em volta saltando para o ataque;
energia azul no punho do Ryūga e vermelha no do Kōga. Balão do Ryūga: "Vamos ver se você ainda
consegue me acompanhar!" Balão do Kōga: "Hahaha! Olha quem fala!"
```

**`pag7`** (duas-colunas)
```
Quadro 1, coluna da esquerda: agentes mascarados caídos pelo terraço, máscaras rachadas; os
dois irmãos de pé, ofegantes, suados. Caixa de narração: "Os últimos agentes caem. Os dois
continuam de pé, ofegantes." Balão do Ryūga: "Foi bom lutar ao seu lado novamente."
Quadro 2, coluna da direita: close do Kōga sorrindo, limpando o sangue do lábio. Balão:
"Heh... foi mesmo, irmãozinho."
```

**`pag8`** (duas-colunas)
```
Quadro 1, coluna da esquerda: Kōga indo embora pelo terraço, de costas, acenando sem olhar
para trás. Balão: "Mas amanhã... estaremos de lados opostos."
Quadro 2, coluna da direita: Ryūga sozinho, pequeno no quadro, olhando a lua cheia enorme.
Caixa de narração: "Ryūga observa o irmão partir e volta a olhar para a lua."
```

### Transições (proposta)

Hoje, depois das quartas, da semifinal, da final e do duelo, a história vai direto para a chave
ou para o "Próximo episódio". Uma página curta depois de cada luta fecha a cena no ritmo do
ep02 e do ep03 (que têm página depois da luta). São páginas com `"irmao"`, com o parceiro
**fixo**. As de dupla variável ficam em silhueta.

| Depois de | Página | Conteúdo |
|---|---|---|
| quartas (ep04) | `pag3_ryuga`, `pag3_koga` | o irmão e o parceiro saindo da Arena Costeira, olhando a chave do outro lado |
| semifinal (ep06) | `pag2` (os dois irmãos) | o mensageiro anuncia os finalistas e diz, de passagem: "O desejo é real. Pode trazer de volta até quem já se foi." O Ryūga e o Kōga pensam na mãe. **Proposta** para cumprir a "revelação do meio da temporada" do [README](README.md), que nenhum episódio de hoje traz |
| final (ep08) | `pag2_ryuga`, `pag2_koga` | o irmão vencedor estende a mão ao irmão caído; o outro recusa e vai embora |
| duelo (ep09) | `pag3_ryuga`, `pag3_koga` | o vencedor olha para a escadaria do templo no pico; uma silhueta de cabelo longo espera lá em cima, sob a lua |

O duelo pode ser perdido? Não: a derrota repete a luta. Por isso a página depois dele sempre
mostra o líder vencendo.

## P4 — O Takatoshi, a Arena do Templo e o ep10

### Ficha do chefe (proposta)

**Quem é.** Takatoshi Komyo, 55 anos, fundador do dojo Shūshinkan e pai do Ryūga e do Kōga.
Partiu para o torneio aos 45, dez anos atrás, e não voltou. Venceu o torneio e virou o Campeão: o
último desafio de quem vencer a regra escondida. A história fechada no [README](README.md) diz
"o Campeão é Takatoshi; o segredo do contrato e do Dafu". O roteiro abaixo é **proposta** para o
ep10 e precisa da aprovação da história:

- **O contrato:** o vencedor do torneio ganha o desejo, mas assina um contrato com os organizadores.
  Até ser derrotado, ele fica na ilha como o Campeão que os magnatas pagam para ver.
- **O Takatoshi venceu e não usou o desejo.** Ele quer trazer de volta a mãe dos meninos, mas o
  desejo só se cumpre quando o Campeão cai. Por isso ele luta para perder **para um dos filhos**.
- **O Dafu sabia.** "Sim, Dafu. Você me ajudaria?" (ep00): o Dafu guardou o segredo e mandou os
  convites aos meninos dez anos depois.
- **A luta é para valer.** Ele não entrega a vitória: só um lutador melhor que ele pode cumprir o
  contrato.

**Estilo de luta: a escola inteira.** O Ryūga é o "lado do poder" da escola da família e o Kōga
é o "lado do golpe" ([p01](../personagens/p01.md), [p03](../personagens/p03.md)). O Takatoshi
tem os dois, mais pesados e mais lentos:
- a guarda do concept: a mão da frente aberta e esticada, o punho de trás na altura do peito;
- grande, com a geta batendo no chão a cada passo.

**Arquétipo:** chefe de controle e punição, com uma segunda fase de pressão. Referências: Gouken
e Akuma (SF), Takuma (Art of Fighting), Geese (Fatal Fury) no contra-golpe.

**Números (proposta, a acertar na bateria):**
- `maxLife` 13.000 (o elenco vai de 9.000 a 11.500, com p09 e p13 no topo; um chefe 1x1 sem
  parceiro precisa de mais);
- andar 85% do p01, sem corrida (só um passo largo de dash);
- pulo baixo e pesado;
- SHINKAI POWER.

| Input | Golpe | Descrição |
|---|---|---|
| L | Palma da frente | Palma aberta curta da mão da frente (a guarda do concept) |
| M | Soco de quadril | Soco reto da mão de trás, girando o quadril |
| H | Chute circular alto | Chute circular com a perna de trás, a geta na altura da cabeça |
| → + M | Martelo de cima | Punho descendo de cima (golpe alto, defende em pé) |
| 2L | Palma baixa | Palma curta agachado |
| 2M | Rasteira girando | Rasteira com a mão no chão, girando (a dos dois filhos) |
| 2H | Punho lançador | Soco de baixo para cima que joga o rival no alto |
| jL, jM, jH | Joelho, chute lateral, soco de cima no ar | |
| ↓ + jH | Pisão de geta | Cai com os dois pés; no chão, quica o rival |
| Agarrão | Projeção do mestre | Segura a gola, joelhada e projeta por cima do ombro (as projeções dos dois filhos juntas) |
| S1 ↓↘→ | **Onda da Casa** | A onda do Ryūga, maior e mais lenta; L/M/H muda a velocidade |
| S2 →↓↘ | **Punho do Pico** | Antiaéreo invencível no começo, sobe girando (o do Ryūga com o do Kōga) |
| S3 ↓↙← L/M | **Aparar do Mestre** | O contra-golpe do Ryūga, mas pega golpe alto **e** baixo; devolve com a palma no peito |
| S4 ↓↙← H | **Passo do Trovão** | Passo longo e pisão de geta que faz o chão tremer; derruba quem está perto e em pé sem defender, passa projéteis |
| SUPER | **Herança** | A Onda Máxima do Ryūga e, emendada, a Rajada de Chutes do Kōga: o pai mostra que os dois golpes eram um só |
| ULTRA | **Último Kata do Shūshinkan** | Sequência cinemática (`ULTRA_SEQ` + página de mangá com o toque no tempo): investida, palmas e chutes alternando a escola dos dois filhos, e um raio de energia à queima-roupa no fim |

**Mecânica de chefe (proposta).** Quatro partes, todas sobre o que o motor já tem:

1. **Duas fases.**
   - Até 50% da vida ele luta como o lado do poder: fica longe, solta a Onda, pune com o
     Aparar e com o Punho do Pico.
   - Abaixo de 50%:
     - uma pausa curta: ele aperta a faixa, a página bate com o close dele e a fala "Agora... o
       outro lado da casa.";
     - a SHINKAI liga sozinha, com a barra cheia;
     - a CPU troca para o lado do golpe: anda para a frente, usa Passo do Trovão, rasteira e
       Herança.
   - Custo: um limiar de vida que troca o estilo da CPU (`comboStyle`) e dispara uma página
     (`playScene`). O motor não muda.
2. **Ele lê o jogador.**
   - Quando o jogador solta o mesmo especial 3 vezes seguidas, a próxima vez é respondida com o
     contra certo:
     - Aparar contra golpe de perto;
     - Punho do Pico contra pulo;
     - Passo do Trovão contra projétil.
   - Ensina a variar, o tema do pai.
   - Custo: um contador no `AiController`.
3. **Armadura no Passo do Trovão e na Herança** (a `withNaturalArmor` do p13): golpe fraco não
   para esses dois golpes. A armadura é só nesses golpes, para o chefe não virar um tanque
   chato.
4. **1x1 sem assistência**, como o duelo: CPU nível 8 fixo, sem a descida depois de 2 derrotas.
   A descida de nível fica para decidir no playtest ([balanco/playtest.md](../balanco/playtest.md)).

**Custo estimado (código):**

| Parte | Dias |
|---|---|
| Pacote do personagem (importar vídeos, frame data) | 3–4, como p13 a p16 |
| Duas fases | 1 |
| Leitura | 0,5 |
| Armadura por golpe | 0,5 |
| Luta no ep10 e no `arcade.json` | 0,5 |

### Arte do Takatoshi, em ordem

1. **Base:** o Takatoshi de corpo inteiro na guarda do concept (a pose da direita da folha),
   virado para a direita, fundo cinza liso, recortada em `art/keys/takatoshi/idle_base.png`.
2. **Imagens iniciais:** `inicio_da_base.py takatoshi art/keys/takatoshi/idle_base.png`:
   - o comando gera `inicio_centro`, `inicio_esquerda` e `inicio_direita`;
   - a altura é 0,80, o maior do elenco; conferir se o H cabe na imagem; se o chute passar da
     borda, fazer uma imagem central menor só para o H, como no p08;
   - o cabelo longo pede `--bolso` e `--halo`, como os cabelos de mechas.
3. **Vídeos:** a tabela abaixo, na ordem (o agachar dá a `inicio_agachado`).
4. **Ultra:** as 6 imagens do formato de [ultras/prompts.md](../ultras/prompts.md):
   - `corte` e `palco` da abertura;
   - `olhos`, `carga`, `golpe` e `final` da página de mangá.
5. **Retratos:**
   - rosto da chave do torneio (`historia/torneio/rostos/takatoshi.jpg`, 160×160) e rosto do HUD;
   - os retratos do HUD saem do concept.

### Vídeos no Flick (MiniMax H3 Fast)

Mesma configuração do elenco ([flick-h3-elenco.md](../personagens/flick-h3-elenco.md)):
- H3 Fast, 5 s, 768p;
- a mesma imagem no início e no fim, menos no andar;
- membro do golpe nomeado pelo lado do quadro;
- uma ação contada ("ONE");
- sem nome de golpe de arte marcial.

A **linha de manter** vale para todos (vai no fim de cada prompt):

```
He never holds any object. Keep his face, long wavy dark-brown hair with gray streaks, full beard, torn sleeveless black kimono top, layered torn gray apron, wide gray sash knotted at the front, baggy black pants, black wrapped fingerless gloves, wooden geta sandals, colors, line art and size exactly as in the frames; his size never changes. Clean animation only: no shadow, floor, dust, speed lines, motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

E a **abertura** de todos:

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game animation of the old martial arts master from the start frame, on a flat solid green background. He faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
```

| Ordem | Clipe | Início → fim | Ação (entre a abertura e a linha de manter) |
|---|---|---|---|
| 1 | idle | centro → centro | `[0-5s] He holds his open-hand guard exactly as in the start frame — lead hand open and extended, rear fist at chest height — breathing slowly and heavily in a seamless loop: two slow breaths, his broad chest rising and falling; his long hair and the torn edges of his clothes stir softly.` |
| 2 | andar para frente | esquerda → centro | "Andando para frente como personagem de jogo de luta mantendo sua guarda, passos pesados" |
| 3 | andar para trás | centro → esquerda | "Andando para trás como personagem de jogo de luta mantendo sua guarda, sempre virado para a direita" |
| 4 | agachar | centro → centro | o agachar do [p03.md](../personagens/p03.md) com "his arms kept in his open-hand guard"; o quadro mais baixo vira `inicio_agachado` |
| 5 | dash | esquerda → direita | `[0-1s] He holds his guard. [1-2s] He takes ONE long heavy step forward to the RIGHT, sliding his front foot and pulling the rear foot after it, staying low. [2-5s] He holds the same guard.` |
| 6 | backdash | direita → centro | `[1-2s] He takes ONE short hop backward to the LEFT, both sandals landing flat together.` |
| 7 | pulo | centro → centro | `[1-2.5s] He jumps STRAIGHT UP ONE time, heavy and not very high, knees tucked, and lands in the same place in the same guard.` |
| 8 | L | centro → centro | `[1-1.5s] He thrusts his lead OPEN palm straight forward at chest height, ONE quick palm strike, and pulls it back to the guard.` |
| 9 | M | centro → centro | `[1-2s] He turns his hips and drives ONE straight punch with the fist that is farther back, on the LEFT side of the frame, toward the RIGHT, then returns to the guard.` |
| 10 | H | centro → centro | `[1-2s] He swings the leg farther back, on the LEFT side of the frame, in ONE high circular kick at head height toward the RIGHT, the wooden sandal leading, and sets it back down where it started.` |
| 11 | → + M | centro → centro | `[1-2s] He raises ONE fist high above his head and brings it straight down in front of him like a hammer, at head height of a standing opponent, then returns to the guard.` |
| 12 | 2L | agachado → agachado | `[0.5-1s] From the deep crouch he thrusts ONE short open palm forward at knee height and pulls it back.` |
| 13 | 2M | agachado → agachado | o 2M do [p03.md](../personagens/p03.md) (ONE spin, ONE sweep, the hand on the ground) |
| 14 | 2H | agachado → centro | `[0.5-1.2s] From the deep crouch he rises explosively with ONE uppercut of the fist that is farther back, straight up above his head, then settles into his standing guard.` |
| 15 | jL / jM / jH | centro → centro | três vídeos de pulo: `ONE knee forward` / `ONE straight side kick forward` / `ONE downward punch`, cada um no alto do pulo |
| 16 | ↓ + jH | centro → centro | `In the air he pulls both knees up and stomps down with BOTH wooden sandals together, landing hard in the same place.` |
| 17 | dano em pé, agachado, no ar | centro / agachado | molde dos danos do elenco: `ONE recoil backward, then back to guard` |
| 18 | defesa em pé, agachado, no ar | centro / agachado | `He blocks with BOTH forearms raised in front of his face, absorbs ONE impact, then returns to guard.` |
| 19 | queda, no chão, levantar | centro | molde do elenco: queda de costas; levantar sem pressa, batendo a poeira do quimono |
| 20 | agarrão | centro → centro | `[1-2s] He grabs an invisible collar in front of him with his lead hand, drives ONE knee forward, then [2-3s] throws over his shoulder toward the LEFT with both arms, and returns to guard.` |
| 21 | S1 Onda da Casa | centro → centro | `[0-1.5s] He brings both hands together at his hip, palms open, gathering power, his hair lifting. [1.5-2.5s] He pushes BOTH open palms forward toward the RIGHT in ONE powerful thrust and holds it. [2.5-5s] He returns to his guard.` (a energia é do jogo) |
| 22 | S2 Punho do Pico | centro → centro | `[1-2s] He crouches slightly and leaps straight up with ONE rising uppercut, turning his body once in the air, and lands back in guard.` |
| 23 | S3 Aparar do Mestre | centro → centro | `[0.5-1.5s] He opens both arms in a wide circular parry, ONE circle with the lead hand. [1.5-2s] He strikes ONE open palm straight into the chest height in front of him toward the RIGHT. [2-5s] Guard.` |
| 24 | S4 Passo do Trovão | esquerda → centro | `[1-2s] He takes ONE long heavy step forward to the RIGHT and stamps his front wooden sandal down hard, his whole body sinking with the stomp. [2-5s] Guard.` (o tremor e a poeira são do jogo) |
| 25 | Super Herança (1) | centro → centro | `[0-1.5s] He gathers power with both palms at his hip, hair flying up. [1.5-2.5s] ONE huge double-palm thrust toward the RIGHT, held.` |
| 26 | Super Herança (2) | centro → centro | `[0-3s] He advances to the RIGHT with THREE kicks in a row, alternating legs, the last ONE a high rising kick that ends with his leg straight up. [3-5s] Guard.` |
| 27 | Ultra (2 a 4 vídeos) | centro → centro | no molde do Ultra do p13 e da p15 (`ULTRA_SEQ`): investida, palmas e chutes, raio à queima-roupa; prompts quando a sequência for aprovada |
| 28 | troca de fase | centro → centro | `[0-1s] Guard. [1-3s] He lowers his guard, grips the gray sash with both hands and tightens the knot ONE time, eyes closing. [3-4s] He opens his eyes and sinks into a lower, more aggressive guard: both fists closed, weight forward. [4-5s] Holds it.` |
| 29 | intro | centro → centro | `[0-2s] He stands upright with his arms crossed, eyes closed. [2-3.5s] He opens his eyes, uncrosses his arms and slides into his open-hand guard. [3.5-5s] Holds the guard.` |
| 30 | vitória | centro → centro | `[1-3s] He straightens up, turns his face slightly away, and closes ONE fist in front of his chest. [3-5s] He stands still, eyes closed, hair moving in the wind.` |
| 31 | derrota (de pé) | centro → centro | `[1-3s] He drops to ONE knee, one hand on the ground, breathing hard, and a faint smile appears. [3-5s] Holds.` |
| 32 | provocação | centro → centro | `[1-3s] He beckons ONE time with the fingers of his lead open hand. [3-5s] Guard.` |

São 32 linhas e ~40 vídeos (o pulo no ar e os danos são três cada). No ritmo do Flick, uns
800 créditos, menos de um mês do Flick Pro.

### Arena do Templo

Os prompts das 9 peças já estão na seção 7 de
[ilha-do-torneio.md](../../tools/stage/ilha-do-torneio.md#7-arena-do-templo-arena_templo-o-campeão-takatoshi).
Faltam dois atores do cenário: o Dafu e os magnatas nos terraços, como no Shūshinkan.

### ep10 — O Campeão (proposta de roteiro)

| Passo | Conteúdo |
|---|---|
| título | "O Campeão" |
| `pag1` (faixa-e-dois) | o líder sobe a escadaria do templo sob a lua; a porta redonda dourada se abre; uma silhueta de cabelo longo, de costas, na praça |
| `pag2` (splash) | o Takatoshi se vira: a revelação. Balão: "Dez anos... vocês cresceram." |
| `pag3_ryuga` / `pag3_koga` | o filho reage: o Ryūga ("Pai... por que você não voltou?"), o Kōga ("Você nos abandonou!") |
| `pag4` (duas-colunas) | o Takatoshi abre a guarda do concept: "Me mostre o que a nossa escola virou em você." |
| luta | **O CAMPEÃO**, 1x1, `arena_templo`; a troca de fase toca a página do close no meio da luta |
| `pag5` | depois da luta: o pai caído sorri e conta o contrato e o desejo; o Dafu aparece na escadaria |
| `pag6` | gancho: o desejo se cumpre? a mãe? os magnatas? "Continua..." (fim da temporada 1) |

O Arcade troca o cartão "TAKATOSHI — Em breve" pela luta 1x1 no `arcade.json`
([arcade.md](arcade.md)): sem as páginas, só o rótulo O CAMPEÃO e a luta.

## O que muda no código quando a arte chegar

- Para cada página nova:
  - rodar `recortar_pagina.py --episodio epNN --nome pagX`;
  - o passo provisório do `episodio.json` vira o passo da página pronta, com os quadros e os
    balões que o script gera.
  - O `TorneioTest` confere a ordem dos passos com `"irmao"`.
- `arena_vip`: `"cenario": "arena_vip"` no passo `duelo` do ep09.
- As páginas de transição são passos novos com `"irmao"`.
- A da semifinal é para os dois irmãos.
- Takatoshi:
  - o pacote em `characters/takatoshi`;
  - o ep10 em `temporada.json`;
  - o último passo do `arcade.json`;
  - `NivelCpu` reparte a subida sozinho.
