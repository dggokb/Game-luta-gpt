# Ryūga com 8 anos (`ryuga_8`)

Lutador só da história (`characters/roster.json` > `historia`): o treino (tutorial) e a briga dos
irmãos no Prólogo. Quem escolhe o Ryūga joga com ele contra o Kōga 8 anos (`koga_8`); quem escolhe
o Kōga joga com o Kōga 8 anos contra ele (`historia/ep00/episodio.json`, passos com `"irmao"`).

- Vídeos no Drive em `ryuga_8` (10 vídeos e as imagens iniciais), convertidos em HD pelo
  `personagem.py` (`tools/sprites/videos/ryuga_8.json`): idle, frente, trás, dash, agacha, pulo,
  defesa, defesaAgachado, defesaPulo e provoca.
- Tamanho: 164 px de guarda, na escala das imagens iniciais do Kōga 8 anos (geradas juntas;
  `art/keys/ryuga_8/tamanho.json`). Defesa agachada e no ar: escala manual 0,30, a do idle.
- Backdash sem vídeo: o recuo do andar para trás (`tools/sprites/montagens/ryuga_8.json`).
- **Emprestado do Kōga 8 anos até os vídeos chegarem:**
  - golpes (L, M, H, agachados, no ar);
  - S1 e S2;
  - dano em pé, agachado e no ar;
  - queda e levantar;
  - frame data, vida e dano (`mecanicas.json`, `pancadas.json`).

  O build aceita a folha de outro personagem quando o perfil dela desenha na mesma escala
  (`worldScale`).
- Caixas do corpo medidas no desenho dele (`caixas_sprites.py`).
- **Vídeos que faltam para ficar todo dele:** L, M, H, 2L, 2M, 2H, jL, jM, jH, j2H, dano (em pé,
  agachado, no ar), queda e levantar, S1, S2 e backdash.
