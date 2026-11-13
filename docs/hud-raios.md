# Barra da luta com raios

A barra da luta (vida, SHINKAI, Super, TAG, relógio e MENU) usa a arte que o dono mandou em
13/11: `art/hud/barra-raios-original.png` (1672×941, fundo transparente, com rostos, barras
cheias e textos já pintados).

## Como funciona

- `tools/hud/montar_barra_raios.py` lê a arte do dono e grava `assets/ui/hud/hud-raios.png`.
  - Abre buracos (transparente) nos rostos, nas vidas, no TAG, no SHINKAI e nos 5 quadradinhos
    do Super. Os rostos são medidos à mão, por dentro da linha da moldura. As barras e os
    quadradinhos são achados pela cor.
  - Apaga os textos que mudam: o número do Super, ∞/TREINO, ◀ MENU e TAG.
  - Recorta só a faixa de cima (linhas 76 a 324).
- `HudRenderer.drawRaios` desenha nas coordenadas da imagem do dono: escala 1280/1672, e a
  linha 76 fica em y = -6 da tela virtual. A barra ocupa o alto da tela até y ≈ 178.
  1. Antes da arte, nos buracos: os rostos, as vidas, o SHINKAI e os quadradinhos do Super.
     A vida, o TAG e o SHINKAI enchem a partir da borda da tela.
  2. A arte por cima.
  3. Por cima da arte: o número do Super, TAG (apagado sem parceiro), o anel do assist, o nome
     do SHINKAI ativo, o relógio, BATALHA/TREINO e o rótulo do MENU.
- Mudanças fora da barra:
  - O toque do MENU agora é a placa da arte (`ControlsLayout.SELECT_BUTTON_*`, 557–723 × 132–170).
  - Os avisos do `drawBanner` descem para logo abaixo da barra.
  - O Super saiu de baixo da tela: a moldura de baixo não é mais desenhada.
- As medidas da arte ficam em dois lugares: em `RAIOS_P1`/`RAIOS_P2` no `HudRenderer` e no
  script. Se a arte mudar de lugar, mude os dois.

## Voltar à barra anterior

Mude `HudRenderer.BARRA_RAIOS` para `false`. A moldura de energia (`energy-frame.png`), o
Super embaixo e o toque antigo do MENU voltam como estavam.

## Arte nova do dono

1. Salve a imagem em `art/hud/barra-raios-original.png`. Ela tem que manter 1672×941 e as
   mesmas posições.
2. Rode o script:

   ```bash
   python3 tools/hud/montar_barra_raios.py art/hud/barra-raios-original.png --previa /tmp/previa.png
   ```

3. Confira a prévia: os buracos aparecem em magenta e não pode sobrar pedaço do desenho antigo
   na borda.
4. Grave um quadro da luta:

   ```bash
   cd android && gradle :pc:gravarLuta -Proteiro="espera:2 foto:luta" -Presolucao=1280x720
   ```

## O que falta

- Os testes `HudArtTest` agora medem os pixels nas posições novas. Eles usam Robolectric, que
  precisa do SDK Android, e não rodaram na sessão que fez a barra. Rodam na CI do PR ou pelo
  botão "Run workflow".
- Os retratos são as artes de conceito (`ui/hud/concepts`). Alguns têm fundo branco, que
  aparece nos cantos da moldura hexagonal.
