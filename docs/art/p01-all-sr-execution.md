# P01 - Super-resolução de todas as animações

Branch: `test/p01-all-sr-20261009`. Fonte: `game-luta-sprite-gpt` no commit `582d1f6`.

Inventário: 39 atlases, 577 frames; dois atlases (96 frames) já integrados. Faltam 37 atlases (481 frames). Estados compartilhados como COMBAT e GROUNDED reutilizam atlas e não duplicam processamento.

| # | Animação | Frames | Status antes da execução |
|---:|---|---:|---|
| 1 | Idle | 76 | Já integrado |
| 2 | Andar para frente | 20 | Já integrado |
| 3 | Andar para trás | 17 | Pendente |
| 4 | Agachar | 8 | Pendente |
| 5 | Levantar | 6 | Pendente |
| 6 | Pular | 6 | Pendente |
| 7 | Cair no ar | 6 | Pendente |
| 8 | Dash | 6 | Pendente |
| 9 | Backdash | 10 | Pendente |
| 10 | Aterrissar | 4 | Pendente |
| 11 | Soco fraco | 11 | Pendente |
| 12 | Chute médio | 14 | Pendente |
| 13 | Golpe forte | 23 | Pendente |
| 14 | Defesa em pé | 8 | Pendente |
| 15 | Defesa agachado | 6 | Pendente |
| 16 | Defesa no ar | 6 | Pendente |
| 17 | Atingido em pé | 9 | Pendente |
| 18 | Atingido agachado | 9 | Pendente |
| 19 | Atingido no ar | 4 | Pendente |
| 20 | Derrubado/no chão | 11 | Pendente |
| 21 | Levantar da queda | 12 | Pendente |
| 22 | Fraco agachado | 11 | Pendente |
| 23 | Médio agachado | 16 | Pendente |
| 24 | Forte agachado | 12 | Pendente |
| 25 | Fraco no ar | 7 | Pendente |
| 26 | Médio no ar | 7 | Pendente |
| 27 | Forte no ar | 7 | Pendente |
| 28 | Forte no ar para baixo | 7 | Pendente |
| 29 | Ultra | 9 | Pendente |
| 30 | Especial de energia | 11 | Pendente |
| 31 | Agarrão e arremesso | 9 | Pendente |
| 32 | Especial S2 | 20 | Pendente |
| 33 | Especial S3 | 21 | Pendente |
| 34 | Especial S4 | 20 | Pendente |
| 35 | Super | 22 | Pendente |
| 36 | Intro | 27 | Pendente |
| 37 | Vitória | 33 | Pendente |
| 38 | Derrota | 33 | Pendente |
| 39 | Provocação | 33 | Pendente |

## Critérios de execução

- Gerar as 37 animações restantes em jobs independentes com o Real-ESRGAN AnimeVideo-v3 e o mesmo modelo oficial dos testes aprovados.
- RGB reconstruído em 4x; alpha diretamente do original com Lanczos; atlas preferencialmente em 2x (fallback seguro 1,5x se 2x não couber).
- Preservar exatamente a ordem dos frames, duração das animações, raiz e escala visual dos personagens; proibir geração de novas poses ou compensação de movimento individual.
- Redistribuir colunas da textura se necessário, mantendo tamanho máximo 4096px e orçamento de 64MiB por atlas.
- Guardar hash de fontes, configurações, modelo e hashes dos masters 4x no manifesto; validar alpha para todos os frames e executar a suíte de testes.
- Só integrar os 37 atlases na branch de teste depois de todas as etapas estarem verdes. Não sobrescrever a branch principal durante o lote.
- Testes automáticos não substituem avaliação visual de tremulação, transições e contornos no jogo.

Workflow: `.github/workflows/p01-all-sr.yml`. O progresso real deve ser consultado no GitHub Actions; tabela acima é o inventário de entrada e não afirma conclusão de trabalho ainda em processamento.
