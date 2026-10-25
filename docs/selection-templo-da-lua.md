# Seletor TAG — referência aprovada do Templo da Lua

## Objetivo

A tela reproduz a **distribuição espacial da imagem aprovada** (1648×928,
normalizada para o canvas Android 1280×720) e adapta retratos, equipe, cursor,
TAG e confirmação ao elenco real. Não recriar uma tela genérica.

## Arquitetura

- CharacterSelect: estado puro, seleção independente dos dois times,
  titular/segundo membro, cursor, hitboxes, seleção aleatória e ajuda
- CharacterSelectRenderer: pinta o ambiente do Templo da Lua, artes conceituais
  já presentes em art/concepts, grade 4×4, painéis TAG, preview e barra inferior
- GameView: repassa toque e teclas/gamepad para a seleção, mantém o combate
  congelado até confirmar
- Gradle: copia apenas p01…p08 de art/concepts para assets de seleção durante build
- Não alteramos dano, colisões, velocidade, hitboxes ou lógica de ataques

## Mapeamento dos elementos

- P1: topo esquerdo, iluminação azul, preview na lateral esquerda
- P2: topo direito, iluminação vermelha, preview na lateral direita
- Grade: quatro colunas por quatro linhas, reta e simétrica, x=387..893,
  y=137..537. Somente os oito personagens em GeneratedCharacters.SELECTABLE
  são selecionáveis; demais espaços mostram cadeados
- TEAM: boxes verticais x=22..223 e x=1058..1259, slots 1/2
- Cenário: preview trapezoidal x≈293..987, y=547..684. No modo teste as setas trocam o
  cenário da luta (toque nas setas/lados do painel, L2/R2 no controle, Z/C no teclado): todas
  as pastas de `assets/stages` com `stage.json`, pelo `nome`, com a miniatura de
  `assets/selection/stages/<id>.webp` (o fundo e a frente desenhados pelo jogo, sem lutadores
  nem HUD, recortados na faixa do meio). A luta de teste abre no escolhido e a seleção volta
  nele. No Arcade (dupla livre) o painel mostra "CENÁRIO SORTEADO": cada luta sorteia o seu.
- Comandos: faixa inferior y=685..720. Inclui confirmar, voltar,
  configurações (ajuda), trocar TAG e aleatório

## Escopo real x limitações da build

- P1 titular/P1 TAG e P2 titular são enviados ao CombatEngine
- A tela permite selecionar o parceiro P2, mas a CPU ainda não aciona
  a mecânica TAG de P2 nessa versão. Não apresentar como concluído.
- Os retratos vêm das artes de conceito existentes; a arte original inteira
  enviada na conversa não foi importada porque o arquivo não tinha bytes
  acessíveis neste runtime. Por isso, as linhas e símbolos espirituais foram
  reconstruídos com Canvas e ainda não são *pixel-perfect*.

## Aceitação / validação

1. A tela abre no início e preserva layout 16:9.
2. Grid é 4×4 reto, sem inclinação lateral; 8 retratos reais, 8 bloqueados.
3. As artes laterais refletem os titulares atuais.
4. TEAM mostra os dois retratos por time e slot em foco.
5. Toques e controle/teclado podem selecionar qualquer personagem desbloqueado.
6. A mesma dupla nunca tem duplicatas; escolher o parceiro como titular troca os slots.
7. Configurações abre ajuda legível, Aleatório altera personagem, LB/RB inverte TAG.
8. Confirmar transfere os titulares e o TAG P1 ao jogo e dispara INTRO.
9. A versão deve compilar e produzir captura do emulador, além de APK instalável.
