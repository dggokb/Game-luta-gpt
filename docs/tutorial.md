# Modo tutorial: o treino guiado dos golpes

O botão **TUTORIAL** (abaixo de HISTÓRIA) toca a cena `tutorial`: o cartão de título e as
páginas 1 a 4 do Prólogo, até o "Vamos começar o treinamento... Estão prontos?". Depois vem o
treino no **Dojo Shūshinkan**. No modo história é igual, porque o treino é um passo do Prólogo
(`"modo": "tutorial"`), e a história continua depois dele.

Takatoshi e Dafu assistem do fundo do dojo e reagem (aplaudem, riem, se espantam) e falam
em quadros de mangá nas laterais da tela (Takatoshi à esquerda, Dafu à direita), sem parar a
luta: a explicação de cada lição e comentários sobre o que o jogador faz. Os vídeos e as
imagens que faltam, com os prompts: [tutorial-plateia.md](tutorial-plateia.md).

## Como funciona

- Um painel embaixo das barras de vida mostra a lição (`TREINO 7/25`), o que fazer e uma
  bolinha para cada vez que precisa fazer. Quando completa, aparece **MUITO BEM!** e vem a
  próxima lição. No fim, **TREINO COMPLETO!**.
- **PULAR ▶▶** (no painel) pula a lição. Na tela de "treino completo", sai do treino.
- O jogador joga com o próprio controle. O boneco de treino (Kōga, `p03`) fica parado, sem a
  IA, e só ataca nas lições de defesa. Ninguém cai: a vida volta cheia o tempo todo.
- Antes de cada lição, os dois voltam para a posição da lição (perto para os golpes, longe
  para andar e para o especial de energia). Se o boneco for parar longe numa lição de golpe,
  ele volta para perto quando os dois param.

## As lições (com os golpes que já existem)

As lições saem da lista de golpes do personagem: uma lição de golpe que ele não tem fica de
fora, e os comandos dos especiais vêm do próprio personagem.

| Grupo | Lições |
|---|---|
| Movimento | andar, agachar, pular (2x), super pulo, dash, recuo |
| Golpes | L (2x), M (2x), H, ↓+L (2x), ↓+M, ↓+H, golpe no ar (↗ + H), cravada (↓+H no ar) |
| Combos | L > M > H, botão COMBO |
| Defesa | segurar ← contra o golpe alto (2x), segurar ↙ contra o golpe baixo (2x) |
| Especiais | energia (↓ → + botão), S2 (→ ↓ ↘ + botão), S3 (↓ ← + L ou M), S4 (↓ ← + H) |
| Sistemas | agarrão (L + M), SUPER (1 barra), ULTRA (↓ + SUPER com 3 barras) |

O texto de cada golpe diz o que ele faz, a partir dos dados do golpe ("derruba", "lança
para cima", "joga o rival na parede").

## Onde fica

- `TutorialDirector` (app, Java puro sobre o motor, como o `DemoDirector`): as lições, a
  contagem e o andamento.
- `HudRenderer.drawTutorial`: o painel. `ControlsLayout`: botão TUTORIAL e o PULAR.
- `GameView`: `onHistoriaLuta` liga o treino quando o passo é `"modo": "tutorial"`, e o fim
  do treino devolve o resultado para a cena ou a história.

## Testes

`TutorialDirectorTest` joga o treino inteiro no motor de verdade com o personagem base, o
p02 e o p03. Cada lição tem uma entrada de exemplo que a cumpre, e o teste confere que todas
passam. Também confere que nenhuma lição passa com o jogador parado, que o PULAR anda e
termina, e que ninguém cai.

## Para incrementar

Uma lição é um `new Lesson(título, como fazer, vezes, perto?, posição, o que o boneco faz,
o que conta como feito, exemplo)` em `TutorialDirector.lessons`. Dá para trocar o
personagem do treino no passo do Prólogo (`dupla`/`cpu`), mudar a ordem, pôr falas entre as
lições (uma cena com `playScene`) ou criar lições novas para os golpes do personagem final.
