# Ryūga com 8 anos (`ryuga_8`)

Lutador só da história (`characters/roster.json` > `historia`): o treino (tutorial) e a briga dos
irmãos no Prólogo. Quem escolhe o Ryūga joga com ele contra o Kōga 8 anos (`koga_8`); quem escolhe
o Kōga joga com o Kōga 8 anos contra ele (`historia/ep00/episodio.json`, passos com `"irmao"`).

- Vídeos no Drive em `ryuga_8` (27 vídeos e as imagens iniciais), convertidos em HD pelo
  `personagem.py` (`tools/sprites/videos/ryuga_8.json`) com `--manter-efeitos`: os quadros com
  efeito do vídeo (clarão, rastro do chute, poeira) entram como estão, com o brilho meio
  transparente.
- Tamanho: 164 px de guarda, na escala das imagens iniciais do Kōga 8 anos (geradas juntas;
  `art/keys/ryuga_8/tamanho.json`). Todos os vídeos na escala do idle, 0,30 (mesma câmera).
- Golpes: L, M, H, agachados (2L, 2M, 2H), →+M (6M, o golpe alto, do `M_BAIXO`), no ar (jL, jM,
  jH, j2H), danos (em pé, agachado, no ar), defesas, pulo (o vídeo novo), dash, backdash (o
  pulinho para trás), provocação, S1 e S2. Tempo dos golpes igual ao do Kōga 8 anos.
- **Montados à mão no padrão do p14** (`tools/sprites/montagens/ryuga_8.json`), todos com a
  câmera do vídeo (`fixar: video`: os pés ficam onde estão no vídeo, todos os clipes no mesmo
  registro):
  - idle; agachar (descida 6-18), **agachado parado** (CROUCH_IDLE, 20-70 em loop) e levantar;
  - andar no tempo do vídeo: para frente a 150 px/s (criança; o motor anda a 300, o que deixava
    o ciclo duas vezes mais rápido); para trás o corpo anda o que o vídeo anda (~40 px/s), sem o
    pé deslizar;
  - dash: um ciclo da passada em loop pela distância (como o do Kōga 8 anos); backdash no tempo
    do motor (30 quadros), não no do vídeo;
  - L: no vídeo o braço fica esticado até o fim, então a volta é a ida de trás para a frente;
  - H: no vídeo ele dá um passinho com o pé de apoio; `fixar: pe` deixa o pé de trás no lugar
    do idle (o chute no eixo);
  - S2: com a câmera do vídeo (os pés não deslizam na queda); o passo para a frente quem anda é o
    motor, e a `rampa` desconta o mesmo do desenho;
  - 2L, 2M, 2H: do agachado ao agachado, com `deslocar` 9 (o agachado do vídeo fica 30 px à
    frente da guarda em pé, a mesma posição do agachar).
- **S1 "Fumacinha"** (↓↘→): o vídeo inteiro, 1,3x mais rápido que ele (4 s): carrega, empurra a palma com o
  clarão (encosta no quadro 40 do vídeo, 77 quadros do jogo), sai só uma fumacinha e ele volta
  para a guarda. Sem projétil: o dano é só o da palma, de perto (`s1.corpoACorpo`).
- **S2 "Chute desastrado"** (↓↙←, meia-lua para trás): o vídeo inteiro no tempo dele: um passo para a frente (~80 px,
  quem anda é o motor), o chute alto (um acerto, no quadro 22 do vídeo, 55 do jogo), ele perde o
  equilíbrio e **cai no chão**, fica deitado e se levanta. A recuperação é longa (252 quadros):
  errou, apanha.
- Montagens (`tools/sprites/montagens/ryuga_8.json`): backdash, S1 e S2, todos com os efeitos.
- Queda e levantar do vídeo dele (`derruba_levanta`, 07/11), no tempo da do Kōga 8 anos: nada mais emprestado.
- Caixas (corpo, golpes, posturas) medidas no desenho dele (`caixas_sprites.py`).
- No treino (tutorial) não há SUPER, ULTRA nem SHINKAI: os botões somem, o botão apertado não
  chega ao motor e as barras ficam vazias (`TutorialDirector.semPoderes`).
- **Poses de cena** (`CENA_BRIGA`, 14/11): os golpes do vídeo da briga dos irmãos (o `qt1`, os dois
  juntos no verde), separados por `tools/sprites/dupla_video.py`. Só a animação feita à mão da
  briga (`historia/ep00/cinematica_briga.json`) toca; nenhum golpe da luta usa.
