# Elenco — estilos de luta e plano de sprites

Documento vivo. Os nomes ainda não existem: cada lutador é chamado pelo número do concept
(`p01` … `p12`). Os concepts/model sheets ficam em `art/concepts/pNN.png` e são a
**referência obrigatória** de toda folha de sprite daquele personagem.

História: "História Base — Torneio dos Irmãos" (Drive, pasta game-luta-gpt).

## Decisões fechadas

- **Duplas fixas no modo história:** p01+p02, p03+p04, p05+p06, p07+p08, p09+p10,
  p11+p12. No arcade o jogador monta a dupla que quiser.
- **Sem armas na mão.** Kunai, shuriken, machado, bisturi, seringa etc. só existem
  como **projéteis/poderes lançados**. Exceção única: **p08 luta com o chicote na mão**
  (longo alcance, estilo Dhalsim) — normais fortes e lançados; detalhes na mecânica.
- **Todos têm algum poder.** O tipo e a quantidade dependem do estilo de cada um.
- **Escola da família (p01, p03 e o pai):** mesma base, mesmos comandos, execução oposta.
  - p01 puxa para o **poder** (referências: Ryu, Takuma, Ken).
  - p03 puxa para o **golpe** (referências: Ryu, Robert, Kim).
  - O pai (boss) é a junção completa dos dois.
  - **A energia da família tem a mesma cor nos dois.** Nada de "Ryu azul / Ken vermelho":
    a diferença é a forma de usar, não a cor. (Cor da energia: a definir junto com os efeitos.)
- **Ordem de produção dos sprites definitivos:** p01 e p03 primeiro, depois os outros.
- **Qualidade:** exatamente a do concept, sem readaptar traço ou proporção; animação de
  anime viva, com muitos frames. Base de cada personagem no ChatGPT (uma conversa por
  personagem, com o concept anexado); animações em vídeo no Seedance (veja Método abaixo).
- O ajudante (pai adotivo, gordão) e o pai ainda não têm concept: ficam para depois.

## Elenco

| # | Dupla | Concept | Estilo de luta | Poderes / projéteis |
|---|---|---|---|---|
| **p01** | p02 | Colete azul/branco sem manga, regata preta, faixa preta e vermelha, luvas pretas e vermelhas, tênis azul/vermelho/branco, cabelo castanho | **Escola da família — lado do poder.** Shoto técnico, calmo, paciente, contra-ataque | Projétil reto, anti-aéreo, super projétil, ultra em raio |
| **p02** | p01 | Kung fu, túnica azul-petróleo com dourado, faixa laranja, cabelo prateado, tiara | **Kung fu ágil** (garça/louva-a-deus): mão aberta, chutes altos, rasteiras, contra-golpe. Apaixonada pelo p01 e inspirada nele, conhecem-se desde pequenos | Palmas de energia curta |
| **p03** | p04 | Colete preto/vermelho com capuz, regata branca, calça cargo preta com fivelas vermelhas, cabelo espetado | **Escola da família — lado do golpe.** Rushdown, energético, movido pela vingança | Projétil curto, uppercut que avança, ultra em sequência de golpes |
| **p04** | p03 | Regata amarela, bandagens, faixa vermelha, cargo azul-marinho, sorriso confiante | **Lutador de rua.** Jabs, ganchos, cabeçada, esquivas que cancelam em soco | Soco com impacto de energia, pouco poder |
| **p05** | p06 | Ninja de máscara, cabelo preto espetado, katana nas costas (só visual) | **Ninjutsu.** Ágil, teleporte em fumaça, mixup, wall jump | Kunai, shuriken |
| **p06** | p05 | Máscara cinza, cabelo roxo, streetwear preto/roxo | **Sombra e ilusão.** Palmas, clones de sombra, puxar à distância. Zoner/trapper estranho | Sombras, clones |
| **p07** | p08 | Mafioso grisalho, terno risca de giz, chapéu, sobretudo | **Cavalheiro pesado** (savate/bartitsu): chutes de sapato social, palmadas, contra-ataque. Ganancioso | Capangas no Super |
| **p08** | p07 | Dama de vestido vermelho/preto, cabelo rosa, chicote | **Chicote — longo alcance** (Dhalsim). Única com arma na mão. Gananciosa | Chicote nos normais fortes e lançado |
| **p09** | p10 | Bombeiro ruivo, colete vermelho com faixas refletivas | **Grappler bombeiro.** Agarrões de "resgate", tanque | Machado lançado (giratório), fogo |
| **p10** | p09 | Bombeira loira, mesmo uniforme | **Bombeira ágil.** Chutes, acrobacias, rushdown | Machado lançado, vapor/água |
| **p11** | p12 | Médico grisalho, jaleco com sangue, luvas | **Cirurgião sádico.** Trapper, veneno ao longo do tempo | Bisturis e seringas lançados |
| **p12** | p11 | Morto-vivo costurado, bandagens | **Criatura feral.** Contorcido, imprevisível, mordidas, golpes que custam vida, super armor | Força que cresce com a vida perdida |

Fichas completas (golpes, lista de animações e prompts):

- [p01](p01.md)
- [p02](p02.md)
- [p03](p03.md)
- [p04](p04.md)

## Método de animação

Cada animação é um vídeo do Seedance convertido em sprites: regras, imagens iniciais e
ferramenta em [video.md](video.md); prompts de cada golpe na ficha do personagem.

O idle feito por deformação (`tools/sprites/animar.py`, poses-chave do ChatGPT) ficou como
alternativa para quando não houver vídeo.
