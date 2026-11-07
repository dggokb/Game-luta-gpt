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
- Golpes: L, M, H, agachados (2L, 2M, 2H), no ar (jL, jM, jH, j2H), danos (em pé, agachado, no
  ar), defesas, pulo (o vídeo novo), dash, backdash (o pulinho para trás), provocação, S1 e S2.
  Tempo dos golpes igual ao do Kōga 8 anos (mesma contagem de quadros).
- **S1 "Fumacinha"** (↓↘→): o vídeo inteiro no tempo dele (5,2 s): carrega, empurra a palma com o
  clarão (encosta no quadro 40 do vídeo, 100 quadros do jogo), sai só uma fumacinha e ele volta
  para a guarda. Sem projétil: o dano é só o da palma, de perto (`s1.corpoACorpo`).
- **S2 "Chute desastrado"** (→↓↘): o vídeo inteiro no tempo dele: um passo para a frente (~80 px,
  quem anda é o motor), o chute alto (um acerto, no quadro 22 do vídeo, 55 do jogo), ele perde o
  equilíbrio e **cai no chão**, fica deitado e se levanta. A recuperação é longa (252 quadros):
  errou, apanha.
- Montagens (`tools/sprites/montagens/ryuga_8.json`): backdash, S1 e S2, todos com os efeitos.
- **Emprestado do Kōga 8 anos até o vídeo chegar:** a queda e o levantar (`derruba_levanta`).
- Caixas (corpo, golpes, posturas) medidas no desenho dele (`caixas_sprites.py`).
- No treino (tutorial) não há SUPER, ULTRA nem SHINKAI: os botões somem, o botão apertado não
  chega ao motor e as barras ficam vazias (`TutorialDirector.semPoderes`).
