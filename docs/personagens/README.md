# Elenco — estilos de luta e plano de sprites

Documento vivo. As fichas são identificadas por código (`p01` … `p16`); os irmãos
p01 e p03 já têm nomes na história (Ryūga Komyo e Kōga Komyo). Os concepts/model sheets
do elenco original p01–p12 ficam em `art/concepts/pNN.png` e são a **referência obrigatória**
para gerar os sprites. Os quatro concepts novos p13–p16 **ainda estão somente no Drive**
e não devem ser considerados importados, animados ou jogáveis.

História: "História Base — Torneio dos Irmãos" (Drive, pasta game-luta-gpt).

## Decisões fechadas

- **Duplas fixas no modo história:** p01+p02, p03+p04, p05+p06, p07+p08, p09+p10,
  p11+p12. No arcade o jogador monta a dupla que quiser.
- **p13+p14 e p15+p16:** pares **propostos** para expansão do elenco; aguardam aprovação
  narrativa e de gameplay. Não são parte das duplas fixas acima até decisão explícita.
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
- **Personagens da história** (concepts no Drive, copiados em `art/concepts/`): Takatoshi
  Komyo aos 45 anos (`takatoshi_45.png`, quando parte para o torneio) e aos 55
  (`takatoshi_55.png`, o Campeão/chefe final), Dafu (`dafu.png`), Ryūga e Kōga aos 8 anos
  (`ryuga_8.png`, `koga_8.png`) e o logo do jogo (`logo.png`). Ainda não são lutadores
  jogáveis; o uso deles no modo história está em `docs/historia/`.

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
| **p11** | p12 (Necro) | Médico grisalho, jaleco com sangue, luvas | **Cirurgião sádico.** Trapper, veneno ao longo do tempo | Bisturis e seringas lançados |
| **p12** (Necro) | p11 | Morto-vivo costurado, bandagens | **Criatura feral.** Contorcido, imprevisível, mordidas, golpes que custam vida, super armor | Força que cresce com a vida perdida |
| **p13** | p14 *(proposta)* | Homem enorme, careca, colete preto rasgado, luvas reforçadas e faixas vermelhas | **Brawler pesado**, punhos esmagadores, ombradas e pressão de curta distância *(proposta)* | Ondas de choque cinéticas curtas *(proposta)* |
| **p14** | p13 *(proposta)* | Homem magro, cabelo preto preso, casaco preto de mangas compridas, faixa vermelha e sorriso malandro | **Trickster de fintas**, esquivas, rasteiras e mudanças de ritmo *(proposta)* | Cortes curtos de ar *(proposta)* |
| **p15** | p16 *(proposta)* | Karateca forte de cabelo preto preso em rabo de cavalo, quimono branco, faixa preta | **Shotokan técnico**, base firme, golpes retos e chutes precisos (no jogo) | Ki curto no S1 (no jogo) |
| **p16** | p15 *(proposta)* | Palhacinha marcial ruiva, roupa vermelha/preta de losangos e meias diferentes | **Rushdown caótico/acrobático**, fingimentos e ataques imprevisíveis *(proposta)* | Estrelas e faíscas de impacto *(proposta)* |

Mecânicas de jogo por personagem (vida, movimento, dano, mecânica única): [mecanicas.md](mecanicas.md) — rascunho.

Fichas completas (golpes, lista de animações e prompts):

- [p01](p01.md)
- [p02](p02.md)
- [p03](p03.md)
- [p04](p04.md)
- [p05](p05.md)
- [p06](p06.md)
- [p07](p07.md)
- [p08](p08.md)
- [p09](p09.md)
- [p10](p10.md)
- [p11](p11.md)
- [p12 — Necro](p12.md)
- [p13](p13.md) — proposta (somente PNG no Drive)
- [p14](p14.md) — proposta (somente PNG no Drive)
- [p15](p15.md) — no jogo com os vídeos que já existem (sem golpes no pulo, reações, intro e vitória)
- [p16](p16.md) — proposta (somente PNG no Drive)

## Personagens novos no Drive

Em `game-luta-gpt/animations/`, as pastas `p13`, `p14` e `p16` contêm somente `pNN.png` cada
uma, e as referências visuais equivalentes estão em `game-luta-gpt/concepts/`. Os documentos
`p13.md`, `p14.md` e `p16.md` registram **propostas de jogabilidade, não recursos
implementados**. A `p15` já tem vídeos e está no jogo com o que eles cobrem ([ficha](p15.md)). Importação das artes, criação de bases, vídeos, dados, hitboxes, poderes,
comandos e balanceamento dependem de etapas futuras e validação.

## Método de animação

Cada animação é um vídeo do Seedance convertido em sprites: regras, imagens iniciais e
ferramenta em [video.md](video.md); prompts de cada golpe na ficha do personagem.

O idle feito por deformação (`tools/sprites/animar.py`, poses-chave do ChatGPT) ficou como
alternativa para quando não houver vídeo.
