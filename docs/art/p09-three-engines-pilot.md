# P09 e P12 — teste integrado dos três motores (09/10/2026)

Branch isolada: `test/p09-three-engines-20261009`.
Base: `game-luta-sprite-gpt`; a branch `ccr-60181022-rh9cms` já é ancestral,
portanto os motores de personagens e auditoria **já existem** na nossa base.
Não houve merge, cópia ou substituição desnecessária desses motores.

## O que realmente é testado

1. **Character Pack Engine** — cria pacotes temporários `p09` e `p12`, registra o bombeiro e o zumbi
   na seleção e compila os arquivos, atlas e metadados Java com
   `build_characters.py --write / --check`. Roda os testes Python e Android.
2. **Auditoria e harmonia** — o mesmo motor da base roda sobre os frames P09 e P12
   gerados, verificando continuidade de tronco, escala e chão. O relatório da
   auditoria detalhada guarda todos os avisos para revisão humana.
3. **Refinamento gráfico real** — o Real-ESRGAN AnimeVideo-v3 oficial de hash fixo
   reconstrói cada frame de ambos os personagens em 4×, preservando máscara alfa do original,
   e entrega textura 2× para o jogo. Não desenha novos personagens ou poses.

## Arte e limites do protótipo

- Para P09 usa **exclusivamente** `art/keys/p09/idle_base.png`, derivado do
  concept `art/concepts/p09.png`; para P12 usa **exclusivamente**
  `art/keys/p12/idle_base.png`, derivado de `art/concepts/p12.png`. Não reaproveita
  a arte de outros lutadores.
- Gera oito frames provisórios de respiração **por personagem** com deslocamento máximo de 2 px
  no tronco e pés fixos. Um teste de continuidade, **não uma animação final**.
- Todos os outros estados do combate são **placeholders** usando o mesmo
  desenho de P09. Golpes e parâmetros provisórios copiados do P01 apenas
  para testar cadastro, compatibilidade e carregamento do pacote.
- No APK, os lutadores aparecem como **P09 BOMBEIRO - PILOTO** e
  **P12 ZUMBI - PILOTO**; ambos selecionáveis, sem movesets finais.
- O teste não afirma que qualidade visual subjetiva, fluidez de golpes ou
  gameplay estão aprovados. Após o teste será preciso criar as animações
  verdadeiras do P09.

## Como rodar

O workflow `.github/workflows/p09-three-engines.yml` inicia automaticamente
ao atualizar esta branch. Também aceita disparo manual via Actions.

Arte, `character.json`, roster, atlases e recursos Java de P09 e P12 são
gerados inicialmente no **checkout do GitHub Actions**. Se e somente se os
testes e APK passarem, os arquivos necessários são commitados automaticamente
**na própria branch de teste**. Nenhum arquivo é publicado em
`game-luta-sprite-gpt` ou `main`.

Entregáveis do workflow:
- `P09-P12-three-engines-test-APK`: APK de teste com P09 e P12 selecionáveis.
- `P09-P12-three-engines-review`: sprites antes/depois da super-resolução,
  manifestos de hash, relatório de harmonização e resultados de testes.

**Critérios mínimos:** modelo e fonte verificados por SHA256; 8/8 frames por personagem
refinados com alfa original; pipeline Character Pack `--write / --check`;
testes unitários; auditoria de harmonia sem erros P09/P12; APK compilado.
Os avisos da auditoria ampla são registrados, sem serem confundidos com
aprovação de placeholders.

Para produção: substituir TODOS os estados placeholder por sprites reais,
auditar transições efetivas, testar no aparelho e só então migrar P09/P12 para
`game-luta-sprite-gpt`.
