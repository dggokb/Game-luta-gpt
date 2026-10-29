# Conferência no aparelho: colisões, movimento e tempo de quadro

Este roteiro é para fazer com o celular na mão. Ele fecha o que o PC não consegue medir: T25 na tela, T03 com o dedo, T26 e T24.

Tudo é feito no **Modo Teste** com **IA OFF**, que deixa o boneco parado. O **DEBUG** mostra:

- caixa do corpo em verde;
- empurrão em amarelo;
- golpe ativo em vermelho;
- tabela de vantagem.

Grave a tela durante as rodadas. Anote só o que der diferente do esperado: número do item, personagem e o que se viu.

Tempo estimado: cerca de 60 minutos.

## 1. Colisões (T25), 20 min

Os casos foram escolhidos a partir das 5573 medições do PC, no anexo da frente E em [auditoria.md](auditoria.md). As imagens de referência estão em [t25/](t25/).

| # | Situação | Esperado hoje | O que olhar |
|---|---|---|---|
| 1.1 | p05 jM contra Ryūga em pé, a ~400 px, apertando no topo do pulo | acerta com o desenho a até 100 px de distância (fantasma) | se o acerto "no ar" incomoda o jogador |
| 1.2 | p10 2H e p15 H contra Ryūga agachado, a ~190–260 px | acertam pelo canto vazio da caixa do membro | se parece injusto ("passou por cima e acertou") |
| 1.3 | Kōga H contra agachado, a 120–160 px | **não acerta** (o chute passa por cima, coerente com o desenho) | se o jogador espera que acerte |
| 1.4 | p13 2M e p02 2M na ponta, contra **Ryūga** agachado | **não acerta**, mas o pé encosta | falha real: o agachado do Ryūga usa a caixa genérica |
| 1.5 | o mesmo 1.4 contra a **p02** agachada | acerta | confirma que o problema é só a caixa do Ryūga |
| 1.6 | p12 S (Investida) começando a ~320 px | atravessa o rival sem acertar | falha real: só 4 quadros ativos em 520 px |
| 1.7 | p09 L contra agachado, a ~90 px | o corpo encosta, mas o soco passa por cima | não é falha; confirmar a leitura |

Para cada item, faça 3 tentativas e marque: acertou / não acertou / o desenho encostou.

## 2. Perfis de movimento (T03), 15 min

No motor, 14 dos 16 têm a mesma mobilidade. Este item confere se o que se sente com o dedo é o que o motor mede, e se a diferença entre os tanques e os aéreos aparece.

| # | Roteiro | Valor do motor | Personagens |
|---|---|---|---|
| 2.1 | Andar da borda até o centro da arena | 300 px/s; p13 170, p14 200 | Ryūga, p09, p13, p14 |
| 2.2 | → → (dash) | 207 px em 20 q; p13 134 | Ryūga, p13 |
| 2.3 | Pulo neutro, pulo à frente, super pulo (↓ ↗) | 263 px de altura em 41 q; super pulo 627 px em 104 q, igual para todos | p05, p10, p09, p07 |
| 2.4 | Air dash no topo do pulo | +210 px; p10 tem 2 | p10 |
| 2.5 | Pulo na parede | só o p05 | p05 |
| 2.6 | Backdash saindo do canto com o rival colado | escapa em 84–100% (T06) | Ryūga contra p03 |

**Pergunta para quem testa:** dá para dizer, de olhos fechados, quem é o tanque e quem é o aéreo? Se não der, a decisão de mobilidade por arquétipo (T03, DECIDIR) ganha peso.

## 3. Tempo de quadro (T26), 15 min

No PC, sem GPU, os cenários ficam entre 29 e 51 ms por quadro. No celular, que desenha com a GPU, o número muda.

1. Ligue o contador de FPS no menu de pausa.
2. Rode cada cenário por 30 s.
3. Anote a média, o pior quadro e se houve engasgo visível.

| # | Cenário | Como provocar |
|---|---|---|
| 3.1 | Luta normal | 1x1, IA ON |
| 3.2 | Super | Super com barra cheia, perto do rival |
| 3.3 | Muitos projéteis | p02 contra p08, os dois soltando S1 |
| 3.4 | SHINKAI Clash | os dois ativam SHINKAI ao mesmo tempo |
| 3.5 | 2x2 com assist | 2x2, assist a cada recarga |
| 3.6 | Ultra | Ultra de qualquer personagem |

Depois de 10 minutos jogando, repita o 3.5 para ver se o aquecimento derruba o FPS. Se o jogo fechar sozinho numa luta 2x2, anote o modelo do celular. No PC, essa luta decodifica cerca de 1460 MB de sprites, e o fechamento pode ser falta de memória.

## 4. Comandos no toque (T24), 10 min

No toque simulado, 85–100% dos comandos saem certos, e os de carga perdem 20%. Faça cada comando 10 vezes e conte quantos saíram certos:

| # | Comando | Personagem |
|---|---|---|
| 4.1 | ↓ ↘ → + botão (S1, Onda) | Ryūga |
| 4.2 | → ↓ ↘ + botão (S2, Punho ascendente) | Ryūga |
| 4.3 | ↓ ↓ + botão (S3, Aparar) | Ryūga |
| 4.4 | ↓ ↙ ← + botão (S4, Chute giratório) | Ryūga |
| 4.5 | carga [←] → + botão (S4, Ombrada) | p09 |
| 4.6 | carga [↓] ↑ + botão (S2, Chute da garça) | p02 |
| 4.7 | Breaker (Super durante o combo do rival) | qualquer um |

Se a taxa no aparelho ficar abaixo de 80% num comando, ele vira item de ajuste de entrada (janela de carga ou buffer).

## Depois da conferência

- Os resultados entram no anexo T25/T26 da [auditoria.md](auditoria.md), com o modelo do celular.
- Itens que viram tarefa de código:
  - caixa medida no agachado do Ryūga (1.4), barata;
  - quadros ativos da Investida do p12 (1.6);
  - regra de "H pega agachado?" (1.2 e 1.3), que é decisão de design;
  - redução do custo do fundo do cenário e da memória de sprites (3.x).
