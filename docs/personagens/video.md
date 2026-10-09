# Animação por vídeo (Seedance) — padrão

Decidido no p01: cada animação é um **vídeo curto** gerado no Seedance a partir de uma
imagem inicial do personagem. O vídeo vira os quadros do jogo com
`tools/sprites/video_para_sprite.py` (recorta o verde, tira sombra e poeira, corrige o
deslizamento, escolhe o loop e coloca tudo na mesma escala). Os prompts de cada
personagem estão na ficha dele (`p01.md`, `p03.md`).

## Antes do primeiro vídeo de um personagem

1. **Base** (`art/keys/<id>/idle_base.png`): a pose de guarda isolada do concept, em alta
   resolução e fundo transparente. Prompt para o ChatGPT, com o concept anexado:
   ```
   Recorte e isole a pose de luta 3/4 deste model sheet (a figura com os punhos em
   guarda). Não redesenhe e não mude nada: mesmo traço, mesma pintura, mesmas cores,
   mesmas proporções e mesmo nível de detalhe. Corpo inteiro, dos pés à ponta do cabelo.
   Imagem vertical 1024×1536, fundo transparente, personagem centralizado ocupando quase
   toda a altura. Sem chão, sem sombra, sem texto.
   ```
2. **Imagens iniciais** (`art/keys/<id>/inicio_*.png`, geradas a partir da Base): a Base
   sobre verde puro em 1280×720, sempre do mesmo tamanho, com espaço em cima para pulos e
   chutes altos.
   - `inicio_centro.png`: quase tudo (o personagem fica no lugar).
   - `inicio_esquerda.png`: golpes e movimentos que avançam para a direita.
   - `inicio_direita.png`: movimentos que recuam para a esquerda.

## Regras de todo vídeo

- **Formato 16:9**, imagem inicial indicada na tabela do golpe, sem cortar nem dar zoom.
  O vídeo tem de **começar (ou terminar) exatamente na imagem inicial**: é por ela que a
  ferramenta acha o tamanho (ver "Tamanho" abaixo).
- **Um vídeo por animação.** Começa e termina na guarda da imagem inicial.
- **O golpe acontece UMA vez e rápido** (velocidade de jogo de luta); depois ele fica em
  guarda respirando. A ferramenta corta o trecho certo.
- **Sem efeitos:** fogo, energia, poeira, rastro, faíscas e sombra são do jogo. Se o vídeo
  inventar efeito, o golpe fica sujo.
- **Sem adversário:** golpes acertam o ar; reações a golpes vêm de "alguém invisível".
- Gerou estranho (virou de frente, a câmera andou, o cabelo mudou, saiu do quadro)?
  **Gere de novo**, não dá para consertar depois.
- **Não precisa de vídeo** para: virar de lado (o jogo espelha o sprite) e pulo para
  frente/trás (o jogo usa o mesmo pulo e move o personagem).

## Tamanho (padrão de inserção)

As imagens iniciais de todos são geradas **na mesma escala do p01**, então a altura de cada
um no jogo sai delas: a guarda do p01 tem **224 px** na célula e os outros ficam
proporcionais (p02 203 px, p04 210 px). O p03 fica igual ao p01 (irmãos, decisão de
história). O jogo ainda aplica o `worldScale` do perfil. Ninguém escolhe escala à mão:

1. **Uma vez por personagem**, depois de criar as imagens iniciais:
   `python3 tools/sprites/tamanho.py calibrar <id> <vídeo_de_agachar.mp4> --escala-de p01`
   grava `art/keys/<id>/tamanho.json` com a escala de cada imagem inicial e a altura (o
   agachado é achado dentro do vídeo de onde foi tirado, mesmo reduzido). O perfil do
   personagem (`tools/sprites/profiles/<id>.json`) leva a mesma proporção.
2. **Personagem inteiro ou vídeo refeito** (o jeito normal):
   `python3 tools/sprites/personagem.py <id> <pasta com os vídeos> [ESTADO ...]`
   Sem estados, monta tudo; com estados (ex.: `CROUCH_LIGHT HIT_AIR`), refaz só esses. Ele
   acha em cada vídeo o trecho da ação e o quadro do impacto, escolhe os quadros na contagem
   do p01 (o tempo e o impacto do p01 servem direto), pula quadros com clarão, arco ou
   poeira, converte e atualiza `characters/<id>/character.json`. Personagem novo nasce com
   os dados do p01 na escala da altura dele; num personagem que já existe só muda a
   animação refeita, e dano, frame data, poderes e tempos ajustados à mão continuam (o tempo
   só volta ao do p01 se a contagem de quadros mudar). O nome do vídeo de cada estado fica
   em `tools/sprites/videos/<id>.json`. Depois: `build_characters.py --write`.
3. **Um clipe à mão** (casos especiais, como o S4 do p04):
   `python3 tools/sprites/video_para_sprite.py video.mp4 --personagem <id> --clipe tools/sprites/clips/<clipe>.json --inicio A --fim B`
   A ferramenta acha a imagem inicial no 1º ou no último quadro (mesmo se o Seedance deu
   zoom), calcula a escala e grava a receita no clipe (`"video"`: arquivo, escala, como foi
   medida, quadros e opções). Para refazer: `video_para_sprite.py video.mp4 --clipe <clipe>`.
4. Vídeo que não começa nem termina numa imagem inicial (intro, vitória...) é medido pela
   guarda do IDLE no meio do vídeo, e só vale se 3 quadros concordarem. Se nada bater, a
   ferramenta para com erro. A saída é gerar de novo a partir da imagem inicial; em último
   caso `--escala N --motivo "..."`, que fica gravado.
5. Se a câmera mudar o zoom no meio do vídeo, a ferramenta recusa: gere de novo.

Os testes conferem que toda receita diz como a escala foi achada.

## Tempo do golpe

Cada animação de golpe marca o quadro do impacto (`"impactFrame"` no `character.json`).
A animação estica sobre o frame data, e o build recusa o pacote se esse quadro não cair na
janela ativa do golpe; aí é ajustar os `durationsMs` (mais tempo antes ou depois do impacto).

## Auditoria (antes de revisar à mão)

```
python3 tools/sprites/auditoria.py p03 --folhas android/app/build/auditoria
```

Aponta, clipe por clipe, o que está fora do padrão, e grava uma folha de conferência (chão
em verde, raiz em amarelo, quadros marcados em vermelho) só dos clipes com aviso:

| Tipo | O que mede |
|---|---|
| tamanho | pontas do clipe em pose de guarda/agachado com altura diferente da referência; cabeça maior ou menor que a do idle (estimativa: confirmar na folha) |
| pulo | golpe que começa ou termina em guarda com o pé de trás fora do lugar do idle |
| deslize | pé de trás andando aos poucos durante o golpe (giro e rasteira não contam) |
| pose | golpe/reação agachado que fica em pé |
| efeito | cor fora da paleta do personagem num quadro (clarão, poeira, arco, rastro) |
| quebrado | quadro de outro personagem, vazio, cortado na borda ou flutuando |
| receita | clipe sem receita, com escala manual ou usando a arte de outro personagem |

É triagem: o "erro" quase sempre é real, o "aviso" precisa de olho na folha. Leva uns 30 s
por personagem. A lista de revisão de cada personagem sai dela (`docs/personagens/revisao.md`).

Correções sem vídeo novo (editam a folha normalizada e gravam na receita, para a
reconversão repetir; depois `build_characters.py --write`):

| Problema | Ferramenta |
|---|---|
| deslize, pulo ao trocar de animação | `ancorar.py p04 HEAVY_STRAIGHT` fixa o pé de apoio no lugar do idle |
| tamanho errado, zoom de câmera no meio | `reescalar.py p02 MEDIUM_KICK --ver` mede pela cabeça quadro a quadro; sem `--ver` aplica (`--fatores` à mão quando a medida erra) |
| poeira, risco, arco, clarão | `limpar.py p02 LIGHT_JAB:3,4,5 --previa pasta` apaga manchas de cor fora da paleta |

Não resolvem: efeito por cima do corpo (o arco cobre a perna), pose errada (agachado que
levanta), tamanho em pose ereta (intro/vitória: a cabeça engana). Aí é gerar o vídeo de novo.

## Um vídeo pode virar vários clipes

| Vídeo | Clipes do jogo |
|---|---|
| pulo | subida (`JUMP`), descida (`FALL`), aterrissagem (`LAND`) |
| agachar | abaixar (`CROUCH`), agachado parado, levantar (`RISE`) |
| derrubado | queda (`KNOCKDOWN`), no chão (`GROUNDED`), levantar (`GETUP`) |
| dash | arrancada, corrida em loop (`DASH`), freada |
