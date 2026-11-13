# Como fazer uma animação (vídeo → jogo)

Cada animação de lutador é um vídeo curto gerado a partir da imagem inicial do personagem e
convertido em sprites. Este é o caminho de hoje, do começo ao fim. Os prompts de cada personagem
estão na ficha dele (`pNN.md`, seção "Vídeos (H3)").

## O caminho

1. **Imagens iniciais `_h3`** do personagem (uma vez).
2. **Vídeo no H3**, com o prompt da ficha.
3. **Converter** (`personagem.py`, `video_para_sprite.py` ou `montagem.py`).
4. **Depois de converter**: nitidez HD, smear (se for o caso), emenda no idle, caixas, normal map,
   build.
5. **Conferir**: testes, régua, e gravar o vídeo para o dono julgar.

## 1. Imagens iniciais (`art/keys/<id>/`)

O H3 desenha o personagem do tamanho que ele está na imagem. As imagens saem da **Base** do concept
(`art/keys/<id>/idle_base.png`: a pose de guarda isolada, fundo transparente), nunca de um quadro de
vídeo ampliado:

```
python3 tools/sprites/inicio_da_base.py pNN art/keys/pNN/idle_base.png --sufixo _h3 --previa pasta
python3 tools/sprites/inicio_da_base.py pNN art/keys/pNN/idle_base.png --sufixo _h3
```

- Tela **1344×768** (o tamanho que o H3 devolve), verde puro, corpo com **77%** da altura, pés em
  y 733; `inicio_esquerda_h3` e `inicio_direita_h3` a ±273 px do centro.
- `--sufixo _h3` quando o personagem já tem vídeos antigos (`inicio_*.png`); personagem novo vai sem.
- `--altura`: personagem mais baixo ou mais alto, medido pelo rosto (orelha, orelha–olho), não pela
  silhueta. Yin-lu: 0,6146 (86,5% do Ryūga, a proporção do jogo).
- **`tamanho.json`**: cada chave nova recebe a escala que dá a altura do jogo (a escala antiga ×
  altura da guarda antiga / altura nova). A conversão mede cada vídeo contra as chaves.
- Cabelo de mechas: `--bolso` (retângulos onde o fundo fica preso), `--desfranja 2`, `--halo 4` e
  `--sem-halo` para o que é claro de verdade. Conferir a prévia ampliada na cabeça.
- Sombra do chão rabiscada: `--sombra-sat 26 --sombra-y 0.85`.
- **`inicio_agachado_h3`** sai do quadro mais baixo do vídeo de agachar (precisa dela antes dos golpes
  agachados).
- **`inicio_corrida_h3`** (golpe de corrida): um quadro da corrida do jogo, na escala das `_h3`.

## 2. O vídeo no H3

- **MiniMax H3 Fast** (no Flick); **H3 Max** só se o Fast errar o golpe.
- **5 s, 768p.** Sai 1344×768, 24 fps, ~124 quadros.
- **Imagem de início e de fim**: a mesma guarda nas duas pontas em tudo que é no lugar (sem a
  final, a guarda volta diferente). Andar: as duas da tabela da ficha. Golpe de corrida: corrida no
  começo, guarda no fim.
- O Flick reescreve o prompt (sem opção de desligar); as faixas de tempo são respeitadas mesmo assim.
- Gerou estranho (virou de frente, a câmera andou, o cabelo mudou, saiu do quadro)? **Gere de novo**:
  não tem conserto depois.

## 3. O prompt

```
Static locked-off side view, one continuous shot with no scene cuts. 2D anime fighting-game
animation of the <personagem> from the start frame, on a flat solid green background. <Ele/Ela>
faces RIGHT the whole time and stays in place; the camera stays fixed and never zooms.
[0-1s] <He/She> holds <his/her> fighting guard exactly as in the start frame.
[1-...s] <a ação, descrita pela mecânica do corpo>
[...-5s] <He/She> holds the same guard as the end frame, breathing calmly, feet planted.
<regras do golpe>. <His/Her> whole body stays inside the frame. <He/She> never holds any object.
Keep <rosto, cabelo, roupa, luvas, calça, tênis>, colors, line art and size exactly as in the
frames; <his/her> size never changes. Clean animation only: no shadow, floor, dust, speed lines,
motion trails, impact flashes, sparks or effects. No opponent. No music, no dialogue.
```

- **Faixas de tempo** `[0-1s]`: guarda no começo e no fim; o golpe vai cedo e o resto é guarda
  parada (a ferramenta corta).
- **Uma ação, contada**: "ONE ...", "One kick only", "Exactly two punches". Sem contar, sai rajada.
- **O membro pelo lado do quadro e pela câmera**: "his rear leg (the leg farther back, on the LEFT side
  of the frame)", "her FRONT arm (the arm closer to the camera)". Foi o que fez o H3 acertar.
- **Sem nome de golpe de arte marcial** ("mawashi-geri", "gyaku-zuki", "Shotokan" puxam combinação e
  postura errada). Descreva o movimento.
- **Pose concreta, não número**: "hips down to the level of the knees, thighs horizontal".
- **Andar: só a frase curta, em português**: "Andando para frente como personagem de jogo de luta
  mantendo sua guarda" (para trás: "... sempre virado(a) para a direita"). O andar detalhado sai
  pulando.
- **Prompt curto**: a aparência vem da imagem; o texto só lista o que não pode mudar.
- Golpe agachado: começa e termina na `inicio_agachado_h3`, "stays low and never stands up".

## 4. Regras do dono (valem para todo vídeo)

- **Sem efeito no vídeo**: fogo, energia, poeira, rastro, faísca e sombra são do jogo.
- **Sem adversário**: os golpes acertam o ar; as reações vêm de "an invisible attack".
- **Sempre dois idles** (12/11): o idle (a guarda respirando, em loop) e o segundo idle (um gesto que
  toca uma vez a cada 3 voltas e volta à guarda). Peça os dois junto com os outros vídeos.
- **Todo golpe emenda no idle** (13/11): o último quadro do golpe é **a guarda do idle**, no mesmo
  lugar, com os pés na mesma altura e na mesma pose. Para isso:
  - o vídeo começa e termina na guarda da imagem inicial;
  - **o idle começa nessa guarda**: o loop do idle sai de um trecho do vídeo que começa e termina
    nela (o da Yin-lu saía do meio da respiração e nenhum golpe batia com ele);
  - **na montagem, o último quadro escolhido é o do vídeo que mais parece o idle** (os vídeos voltam
    à guarda no fim; a montagem não pode parar antes);
  - depois de converter, `emendar_idle.py` acerta o lugar e a altura dos pés (seção 5).
- **Golpe com efeito desenhado na arte**: a arte fica e o VFX do jogo combina com ela (skill
  `vfx-luta`).

## 5. Converter

**Personagem inteiro ou vídeo refeito** (o jeito normal):

```
python3 tools/sprites/personagem.py <id> <pasta com os vídeos> [ESTADO ...]
```

Acha em cada vídeo o trecho da ação e o quadro do impacto, escolhe os quadros, converte e atualiza
`characters/<id>/character.json` (num personagem que já existe, só a animação refeita muda). O nome
do vídeo de cada estado fica em `tools/sprites/videos/<id>.json`.

**Um clipe à mão:**

```
python3 tools/sprites/video_para_sprite.py video.mp4 --personagem <id> --clipe tools/sprites/clips/<clipe>.json --inicio A --fim B
```

Acha a imagem inicial no 1º ou no último quadro, calcula a escala e grava a receita no clipe
(`"video"`); para refazer, `video_para_sprite.py video.mp4 --clipe <clipe>`. Câmera que muda o zoom
no meio é recusada: gere de novo. `--escala N --motivo "..."` só em último caso (fica gravado).

**Vários vídeos ou quadros escolhidos num clipe** (o jeito dos vídeos H3):

```
python3 tools/sprites/montagem.py <id> <pasta com os vídeos> [clipes]
```

A receita fica em `tools/sprites/montagens/<id>.json`: trechos dos vídeos colados (sem perda, na
mesma câmera), os quadros escolhidos e as animações do pacote que usam a folha (`"0-5,4-0"` volta
pelo inverso). Trecho `"fixo"` tira o deslocamento lateral (quem anda é o motor, com o avanço do
golpe); `"alinha"` leva o fim de volta à raiz. Opções: `escala` (com `motivo`), `fixar: video`,
`sem_poeira`, `recorte_suave` (cabelo de fios finos). **Cuidado:** a montagem refaz o clipe e tira
do `character.json` o tempo feito à mão (`durationsMs`) e as caixas (`silhueta`) da animação:
guarde antes e devolva depois.

Andar: `"distancia": "video"` mede o passo no vídeo (pé plantado); `"velocidade": "video"` faz o
lutador andar na velocidade do vídeo; `"tempo": "video"` deixa o andar do motor e acerta o ciclo
(`"acelerar"` para tocar mais rápido). O loop tem de ser um ciclo do vídeo, sem largada nem freada.

| Um vídeo | vira os clipes |
|---|---|
| pulo | subida (`JUMP`), descida (`FALL`), aterrissagem (`LAND`) |
| agachar | abaixar (`CROUCH`), agachado parado (`CROUCH_IDLE`), levantar (`RISE`) |
| derrubado | queda (`KNOCKDOWN`), no chão (`GROUNDED`), levantar (`GETUP`) |
| dash | arrancada, corrida em loop (`DASH`), freada |

## 6. Depois de converter (nesta ordem)

1. **Nitidez HD**: `python3 tools/sprites/sr_hd.py tools/sprites/clips/<clipe>.json --pesos ~/.cache/sokon/realesr-animevideov3.pth`
   (o clipe precisa de `frameWidth`, `frameHeight` e `rootY`; a fonte é a `*_video_normalized.png`).
2. **Smear**, se o golpe tiver: `smear.py` com os parâmetros guardados no clipe (antes, a skill
   `vfx-luta`).
3. **Emenda no idle**: `python3 tools/sprites/emendar_idle.py <id> --write`; posturas que nunca
   saem do chão (agachar, agachado parado, levantar): `--assentar CROUCH CROUCH_IDLE RISE --write`.
4. **Build**: `python3 tools/sprites/build_characters.py --write`. Ele às vezes apaga `_n.webp` de
   outra sessão: depois, `git status --short | grep "^ D" | awk '{print $2}' | xargs -r git checkout --`.
5. **Caixas** (corpo e golpe medidos no desenho): `caixas_sprites.py <id>` mostra; `--write` grava
   todos, então para um golpe só refaça a `silhueta` da animação (e o `angulo`, `angulo_golpe.py`, e
   o `caminho`, `linha_de_acao.py`). Nos especiais a altura do acerto é o `hitHeight`/`hitFloor` do
   golpe, não o desenho.
6. **Normal map** (quem tem): `python3 tools/sprites/normal_map.py <folha> --refazer`, e o build de
   novo.
7. **Tabela da CPU**, se o alcance mudou: `GeraAlcanceCpu` (o `CpuReachTest` avisa).
8. **Ficha**: `python3 tools/sprites/doc_personagem.py <id> --write`.

`mecanicas.py --write` refaz os pacotes inteiros a partir do `mecanicas.json` e desfaz ajustes feitos
à mão: para mudar um golpe, edite o `character.json` e registre a mesma coisa no `mecanicas.json`.

## 7. Conferir

- **Testes**: `tools/sem-gradle/montar.sh` e `tools/sem-gradle/testes.sh`. Os de Python das
  ferramentas rodam no build do APK (inclusive `test_emenda_idle.py`).
- **Gravar** no PC: `DUPLA=<p1>,<parceiro>,<p2>,<parceiro> tools/sem-gradle/gravar.sh <pasta> 960x540 espera:1 ... grava:N:nome rota:L,M,M,S2 fim`
  (`rota:` aperta o combo quadro a quadro, como o teste do motor; `canto:100` põe o rival no canto).
  A gravação trava a qualidade em ALTO (`GRAFICOS=` troca): no AUTO o PC pesado trocava a resolução
  no meio e a imagem pulava. Mande o vídeo ao dono em velocidade normal e em câmera lenta; quem julga
  é ele, e ninguém testa no celular daqui.

## Ferramentas de correção (sem vídeo novo)

**Auditoria**: `python3 tools/sprites/auditoria.py <id> --folhas android/app/build/auditoria` aponta,
clipe por clipe, tamanho, pulo, deslize, pose, efeito, quadro quebrado, receita, ritmo e ciclo, e grava
uma folha de conferência dos clipes com aviso. É triagem: o "erro" quase sempre é real, o "aviso"
precisa de olho na folha.

| Problema | Ferramenta |
|---|---|
| fim do golpe fora do lugar do idle | `emendar_idle.py <id> --write` |
| pé de apoio deslizando, pulo ao trocar de animação | `ancorar.py <id> <ESTADO>` |
| tronco fora do eixo da pose de referência | `centralizar.py <id> <ESTADOS> --ref CROUCH_IDLE` |
| tamanho errado, zoom de câmera no meio | `reescalar.py <id> <ESTADO> --ver` (sem `--ver` aplica) |
| poeira, risco, arco, clarão do vídeo | `limpar.py <id> <ESTADO>:3,4,5 --previa pasta` (cuidado: come cabelo prateado) |

Não resolvem: efeito por cima do corpo, pose errada (agachado que levanta), tamanho em pose ereta.
Aí é gerar o vídeo de novo.

**Régua de tamanho**: `python3 tools/sprites/regua.py <id> --relatorio` grava
`docs/personagens/regua/<id>.md` com o fator de cada clipe contra a imagem inicial (100% = certo;
aviso fora de ±3%) e as trocas de animação sobre o idle. `--corrigir ESTADOS` leva a 100%; só
corrija o que duas medidas confirmarem. No celular: DEBUG > AJUSTAR TAMANHOS, e
`regua.py --aplicar ajustes-tamanho-<data>.json`.

**Ritmo**: `ritmo.py <id>` acerta o tempo da arte contra o p01 (andar, dash, pulo, agachar pelo
movimento; idle, intro e vitória pelo vídeo; golpes pelo frame data do personagem, com o impacto na
janela ativa).

**Tempo do golpe**: cada animação marca o quadro do impacto (`impactFrame`); o build recusa o pacote se
ele não cair na janela ativa. Ajuste os `durationsMs`.
