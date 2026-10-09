# P09 — teste integrado dos três motores (09/10/2026)

Branch isolada: `test/p09-three-engines-20261009`.
Base: `game-luta-sprite-gpt`; a branch `ccr-60181022-rh9cms` já é ancestral,
portanto os motores de personagens e auditoria **já existem** na nossa base.
Não houve merge, cópia ou substituição desnecessária desses motores.

## O que realmente é testado

1. **Character Pack Engine** — cria um pacote `p09` temporário, registra o bombeiro
   na seleção e compila os arquivos, atlas e metadados Java com
   `build_characters.py --write / --check`. Roda os testes Python e Android.
2. **Auditoria e harmonia** — o mesmo motor da base roda sobre os frames P09
   gerados, verificando continuidade de tronco, escala e chão. O relatório da
   auditoria detalhada guarda todos os avisos para revisão humana.
3. **Refinamento gráfico real** — o Real-ESRGAN AnimeVideo-v3 oficial de hash fixo
   reconstrói cada frame de P09 em 4×, preservando máscara alfa do original,
   e entrega textura 2× para o jogo. Não desenha novos personagens ou poses.

## Arte e limites do protótipo

- Usa **exclusivamente** `art/keys/p09/idle_base.png`, derivado do concept
  `art/concepts/p09.png`. Não usa imagens de outros personagens.
- Gera oito frames provisórios de respiração com deslocamento máximo de 2 px
  no tronco e pés fixos. Um teste de continuidade, **não uma animação final**.
- Todos os outros estados do combate são **placeholders** usando o mesmo
  desenho de P09. Golpes e parâmetros provisórios copiados do P01 apenas
  para testar cadastro, compatibilidade e carregamento do pacote.
- No APK, o P09 aparece como **P09 BOMBEIRO - PILOTO**. Seus movimentos
  ainda NÃO são o moveset, arte e timing finais do personagem.
- O teste não afirma que qualidade visual subjetiva, fluidez de golpes ou
  gameplay estão aprovados. Após o teste será preciso criar as animações
  verdadeiras do P09.

## Como rodar

O workflow `.github/workflows/p09-three-engines.yml` inicia automaticamente
ao atualizar esta branch. Também aceita disparo manual via Actions.

Arte, `character.json`, roster, atlas e recursos Java do P09 são gerados no
**checkout descartável do GitHub Actions**. Nenhum script altera a branch
`game-luta-sprite-gpt`. Nada é automaticamente publicado nela.

Entregáveis do workflow:
- `P09-three-engines-test-APK`: APK de teste com P09 selecionável.
- `P09-three-engines-review`: sprites antes/depois da super-resolução,
  manifestos de hash, relatório de harmonização e resultados de testes.

**Critérios mínimos:** modelo e fonte verificados por SHA256; 8/8 frames
refinados com alfa original; pipeline Character Pack `--write / --check`;
testes unitários; auditoria de harmonia sem erros P09; APK compilado.
Os avisos da auditoria ampla são registrados, sem serem confundidos com
aprovação de placeholders.

Para produção: substituir TODOS os estados placeholder por sprites reais,
auditar transições efetivas, testar no aparelho e só então migrar P09 para
`game-luta-sprite-gpt`.
