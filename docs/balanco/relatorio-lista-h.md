# Relatório da lista H: decisões sobre a lista G

Este relatório cobre as oito decisões do usuário. Cada etapa tem um commit isolado, medido com as mesmas sementes da anterior e registrado em [ajustes.md](ajustes.md). A base é `5dedf956`, o fim da lista G, e o final é `285e7f88`.

**Testes:**

- suíte do app: 583 de 583;
- `build_characters.py --check`: passa.

**Ruído das medidas:** ±8 por personagem na matriz e ±13 numa dupla do T21 (56 lutas por dupla).

**Relatórios das frentes:**

- [h1-breaker.md](h1-breaker.md)
- [h4-personagens.md](h4-personagens.md)
- [h5-dhc-assists-energy.md](h5-dhc-assists-energy.md)
- colisões e o A/B do p09: entradas H8a–H2 em [ajustes.md](ajustes.md)

## Commits, na ordem

| # | Commit | Item | O que muda |
|---|---|---|---|
| H8a | `bfca8884` | 8 colisões | Ryūga agachado apanha pelo corpo desenhado. Era o único dos 16 com a caixa genérica de ±36 px |
| H8b | `d50fe9ef` | 8 colisões | A Investida do p12 bate pelo bote desenhado; não atravessa mais o rival |
| FPS | `354378e4` | (seu teste no celular) | No LEVE, as folhas decodificam a 3/4 (~57% da memória de vídeo) |
| H8c | `4a8f35c2` | 8 colisões | Membro na diagonal vira duas caixas: o canto vazio do retângulo não acerta mais |
| H3 | `e6e4b2d8` | 3 p06 | O impacto do 2M cai no primeiro quadro ativo. Só muda o tempo do clipe; dano e quadros iguais |
| H2 | `cb632c64` | 2 p09 | **EXPERIMENTO, lado B:** 2M com `alcanceMaximo` 190 (era 220) |
| H1 | `db6bc913` | 1 Breaker | Atordoamento 20 → 4 com a mesma distância; o buffer do atacante é limpo na quebra |
| H5a | `393e5a65` | 5 DHC | O parceiro desliza até o lugar do Super, sem aparecer longe nem dentro do rival |
| H5 | `58cd1600` | 6, 7 | Só testes: segurança dos assists e rendimento do ENERGY |
| H4-p16 | `e1e33d59` | 4 | M e H da p16 cancelam em especial e Super |
| H4-p15a | `7413fe14` | 4 | Repel da p15 livre 6 quadros depois de desfazer a bola (antes, 39) |
| H4-p15b | `8c25c96d` | 4 | Super da p15 defendido empurra 10 px (antes, 60): −83 → −9 |
| H4-Ryūga | `285e7f88` | 4 | O Aparar vira aparar de verdade: só bate no que leu |

**Não aplicados**, medidos e com a recomendação de cada um no item correspondente abaixo:

- piso de 70% no DHC;
- atordoamento 2 no Breaker;
- pisão do p14 no neutro da CPU;
- correção dos outros 16 golpes com o impacto fora da janela.

## Os 16 personagens

Força contra o elenco na matriz 1x1 16 × 16: nível 8, 10 sementes por célula e os dois lados somados.

| personagem | lista G | lista H | Δ | causa principal |
|---|---|---|---|---|
| p08 | 78% | 75% | −3 | ruído |
| p04 | 64% | 65% | +1 | — |
| p11 | 63% | 61% | −2 | — |
| p09 | 64% | 60% | −4 | experimento H2 (2M 190): o 2M cai de 22% para 14% do dano dele |
| Ryūga | 61% | 60% | −1 | Aparar sem golpe comum; agachado agora apanha pelo desenho (H8a) |
| Kōga | 58% | 60% | +2 | — |
| p05 | 51% | 59% | +8 | provável H8c: os rivais perderam acertos fantasma contra ele (não isolado por golpe) |
| p10 | 63% | 57% | −6 | dentro do ruído |
| p12 | 47% | 57% | +10 | H8b: a Investida passou a acertar |
| p06 | 51% | 53% | +2 | H3 dentro do ruído; você aceitou os 51% |
| p13 | 55% | 50% | −5 | ruído |
| Yin-lu | 45% | 50% | +5 | — |
| p14 | 31% | 36% | +5 | — |
| p07 | 32% | 23% | −9 | ver abaixo |
| p16 | 24% | 19% | −5 | ruído: o ganho do cancelamento não aparece no 1x1 contra CPU |
| p15 | 13% | 14% | +1 | — |

**Faixa:** 13–78% → 14–75%, desvio padrão 16,8 → 17,0. **Viés de lado:** o lado esquerdo vence 49%.

O p07 cai 9 pontos, no limite do ruído. A principal suspeita é o H8c: com o membro diagonal em duas caixas, golpes longos do p07 que acertavam pelo canto vazio deixaram de acertar. Ainda não isolei por golpe.

## Duplas oficiais (T21)

2x2 contra as outras sete duplas oficiais, 8 lutas por adversário. A tabela mostra o T21 em cada commit, para separar as causas.

| dupla | base | depois de H8a + H8b + FPS | H8c | H3 | H2 (p09) | H1 (Breaker) | H5a…H4 (final) |
|---|---|---|---|---|---|---|---|
| Ryūga + Yin-lu | 64% | 54% | 48% | 52% | 55% | 46% | 46% |
| Kōga + p04 | 55% | 55% | 63% | 61% | 64% | 55% | 57% |
| p05 + p06 | 36% | 39% | 45% | 50% | 46% | 45% | 45% |
| p07 + p08 | 63% | 63% | 55% | 55% | 59% | 63% | 63% |
| p09 + p10 | 79% | 75% | 71% | 73% | 63% | 68% | 66% |
| p11 + p12 | 64% | 66% | 71% | 70% | 73% | 71% | 73% |
| p13 + p14 | 39% | 38% | 39% | 43% | 48% | 36% | 38% |
| p15 + p16 | 2% | 5% | 5% | 5% | 5% | 9% | 5% |

**Leitura:**

- **Ryūga + Yin-lu (64 → 46%)** não cai por uma etapa só. Somam três:
  - o Ryūga agachado passou a apanhar pelo corpo desenhado (H8a), que é mais largo que a caixa genérica antiga;
  - a H8c (−6);
  - o Breaker 4 (−9).
  - Cada passo está dentro do ruído de uma dupla; a soma, não. As duas primeiras causas são correções de geometria que você pediu, e não compensei com buff.
- **p09 + p10 (79 → 66%)** é o efeito esperado do experimento H2 (73 → 63%).
- **p15 + p16 (2 → 5%)** continua no fundo, mesmo com as quatro ferramentas corrigidas. Na frente H4 o conjunto chegava a 9%, mas aqui a diferença fica dentro do ruído. A p15 é a âncora: com outros parceiros ela fica em 9–16%.
- **p11 + p12 (64 → 73%)** sobe com a Investida do p12 (H8b).

## Cada decisão

1. **Breaker (H1):**
   - Atordoamento 4 com a mesma distância e a CPU nova:
     - troco garantido no roteiro: 57% → 3%;
     - com as CPUs livres: garantido 68% → 1%, oportunista 20% → 60%.
   - Ainda entra garantido o L e o 2L na ponta de quem tem L longo: p12, p11, p06 e Ryūga.
   - **Próximo experimento proposto:** atordoamento 2 (troco garantido de 1–3% e quem quebra ainda com saldo positivo). Não aplicado.
2. **p09 (H2):**
   - Experimento B aplicado e marcado: matriz 64 → 60%, 2x2 66 → 62%, dupla 79 → 66%.
   - Para voltar ao A, basta reverter o `cb632c64`.
   - Consolidar é decisão sua, agora com a matriz completa.
3. **p06 (H3):** os 23 px vinham do tempo do clipe, não do alcance. Corrigido só o tempo; o alcance real foi para 306 px e a força ficou em 53%, dentro do ruído.
4. **Personagens fracos (H4):**
   - **Ferramentas corrigidas:** cancelamentos da p16, repel e Super defendido da p15, e o Aparar do Ryūga, sem aumento de dano.
   - **Por que p15 + p16 dava 2%:** a dupla junta as duas personagens mais fracas, e as três ferramentas quebradas eram parte disso.
   - **p14:** colocar o pisão no neutro da CPU piorou o p14, então ficou de fora.
   - **p07:** nenhuma ferramenta deixa de cumprir a função.
   - **Ainda abaixo de 25%:** p15, p16 e p07.
5. **DHC (H5a):**
   - A entrada de 420 px foi corrigida: desliza, sem teleporte e sem parar dentro do rival.
   - O piso de 70% foi medido sozinho e rende só +1,7% por barra; não aplicado.
   - **Proposta:** soltar o DHC quando o rival estiver saindo do combo, onde hoje ele sai tarde (p05+p06, p13+p14, p15+p16).
6. **Assists:** segurança, canto, cruzamento e defesa agachada testados; nenhum precisou de correção.
7. **ENERGY:**
   - Não alterado.
   - Vence 69% no 1x1 e 55% no 2x2.
   - A barra sobra no fim da luta em todos os tipos: é a CPU que não gasta.
8. **Colisões (H8a–H8c):**
   - Fantasmas acima de 50 px: 113 → 85.
   - Kōga L/M/H: nunca pegam agachado, nem antes nem depois; o desenho também não encosta.
   - Nenhum H passou a pegar agachado.
   - **Achado para depois:** mais 16 golpes têm o impacto um quadro antes da janela ativa. Corrigir aumenta o alcance real de quase todos, então é decisão ampla.

## Para você decidir

1. **p09:** consolidar o B (190) ou voltar ao A (220).
2. **Breaker:** testar o atordoamento 2.
3. **DHC:** soltar o DHC na saída do combo.
4. **Os 16 golpes com o impacto fora da janela:** corrigir todos (mexe no alcance real do elenco) ou um por um.
5. **Ryūga + Yin-lu (46%):** aceitar, porque as quedas vêm de correções de geometria, ou investigar ferramentas da dupla.
6. **p07 (23%):** isolar quais golpes perderam acertos com a H8c, para confirmar que eram só fantasmas.
7. **p15 e p16:** próximas ferramentas da H4:
   - normais lentos da p15: passar quadros do início para a recuperação;
   - Super da p15 sem combo;
   - balão e Super lentos da p16.
