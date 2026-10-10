# Motor de validação escalável dos sprites — elenco inteiro

**Motivo:** o P01 provou que corrigir manualmente cada frame, voltar ao celular,
comparar IDLE e repetir é caro demais para 12 personagens.

## O que realmente causou os erros anteriores

- A altura do retângulo do sprite **não** mede tamanho do corpo quando o braço
  é levantado, um lutador se agacha ou muda a pose. Não aplicar "bbox igual ao
  idle" indiscriminadamente: isso encolheu o DASH e o 2H.
- O valor do root X ou um simples offset no PNG **não** garante que a animação
  fique parada **na tela**, pois o enquadramento acompanha os dois lutadores.
  Na v0.100 a câmera é congelada no KO; não se tenta mascarar a câmera com dx
  de sprite.
- No superpulo, FALL era escalado independentemente em cada um dos seis
  quadros conforme sua altura (que cresce conforme o lutador estica a postura).
  O mesmo corpo passava a encolher na descida. Na v0.100 a escala corporal de
  FALL é fixa de ponta a ponta.
- O jogo pode ter ritmo de 60 FPS, mas o celular renderizar a 30 FPS. A
  validação deve garantir visualização dos quadros (ou indicar o limite).

## Motor reutilizável existente

1. `tamanho.py`: mede a escala do vídeo com referência original e cabeça.
2. `auditoria.py`: aponta corpo pequeno/grande, deslize, ancoragem, frames
   partidos, efeitos, ritmo, loops e discrepâncias entre personagens.
3. `ancorar.py`: desloca os quadros pela posição do pé de apoio quando
   esse pé realmente deveria estar parado. NÃO usar indiscriminadamente
   em DASH/WALK, que mudam de posição de propósito.
4. `reescalar.py`: detecta a escala pela cabeça (mais confiável que bbox).
   No primeiro ciclo usar `--ver`; redimensionamento do PNG deve ser uma
   exceção porque introduz perda gráfica; preferir escala no renderizador.
5. `refinar_elenco.py`: novo orquestrador para correr pelos packs e **filtrar
   apenas estados anômalos**. Não mascara imagens sem fonte como sucesso.

## Procedimento operacional

Instale `pip install -r tools/sprites/requirements-animar.txt`, depois:

```bash
# Um personagem — gerar relatório legível
python3 tools/sprites/refinar_elenco.py player_base --saida android/app/build/sprite-review/p01.json

# Todos os packs atualmente presentes, sejam 8, 10 ou 12
python3 tools/sprites/refinar_elenco.py --todos --saida android/app/build/sprite-review/elenco.json

# Para medir uma anomalia sem escrever PNG nem desregular um personagem
python3 tools/sprites/ancorar.py p02 MEDIUM_KICK --ver
python3 tools/sprites/reescalar.py p02 MEDIUM_KICK --ver
```

O JSON por lutador inclui `AUDITADO`, `PENDENTE_FONTE_OU_LAYOUT` ou
`FALHA_AUDITORIA`, quantidades por severidade, frames problemáticos e
rota de correção (ancorar pé, rever cabeça/escala, corrigir arte, ritmo
ou mudança intencional).

A auditoria pode executar remotamente por **GitHub Actions → Audit Sprite
Roster → Run workflow** na branch experimental. Os resultados ficam em
artefato `Sprite-Roster-Audit`. Não altera a branch nem a arte.

## Critérios de aceitação automáticos

- **Corpo:** usar cabeça/torso e a pose IDLE do **próprio lutador**;
  aceitar braços/pernas estendidos sem redimensionar todo o personagem.
- **Ancoragem:** per-frame estável na arte para poses plantadas; e X na
  tela invariável se lutador + câmera estão congelados no KO.
- **Air states:** comparar JUMP ↔ FALL e aplicar escala corporal coerente
  durante a subida, ápice, descida e aterrissagem.
- **Ritmo:** confirmar as poses visíveis em 60 e 30 FPS nos movimentos
  em que todas as poses devem aparecer.
- **Colisões:** nenhuma mudança de escala só-visual muda hitboxes,
  dano, knockback, movimento físico ou janelas de combo.
- **Qualidade:** nunca reamostrar todos os quadros repetidamente para
  corrigir proporções; preferir transformações de renderização.
- **Gate:** casos ambíguos (personagens novos, rota faltante, ataque com
  deslocamento real) recebem **revisão excepcional**; nada deve ser marcado
  como aprovado automaticamente apenas por ter gerado PNG.

Não copiar fatores do P01 para outros lutadores: cada personagem tem
sua própria referência, escala física e animações distintas. A meta não
é zerar intervenção humana, e sim concentrar revisão apenas nas
anomalias onde a automação não tem evidência suficiente.
