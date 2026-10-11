# P02 — bancada de animações (11/10/2026)

Objetivo: controlar apenas **P02** e lutar contra **P01** (id técnico `player_base`) sob controle da CPU.

- P02 é o único integrante da equipe do jogador, sem TAG/parceiro.
- P01 fica exclusivamente no lado do adversário; seus sprites INTRO/VICTORY e correções automáticas continuam ativos.
- Tela de seleção de personagens é omitida no primeiro lançamento. Botão antes chamado DUPLA aparece como **REINICIAR P02** e reinicia o mesmo confronto.
- Os demais personagens e sprites do projeto são preservados e não sofreram alterações.
- Modo isolado em `P02TrainingMatch.ENABLED`, para reversão simples quando os testes terminarem.
- A lista de produção em `characters/roster.json` e o código gerado `GeneratedCharacters.java` permanecem intactos; o foco de testes é aplicado na construção do `GameView`, não no compilador de personagens.
- Regression test: `com.gamelutagpt.P02TrainingMatchTest`.

Limitações: esta mudança apenas define o confronto P02 vs P01. Não implica que as animações do P02 já foram validadas ou refinadas.
