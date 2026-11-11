---
name: vfx-luta
description: |
  Regras de VFX deste jogo de luta 2D (rastro de golpe, efeito de dano, defesa, fumaça, poeira,
  poderes): o que o dono do jogo já aprovou e reprovou, como gerar, colocar no jogo, gravar e
  conferir. Use ANTES de criar, trocar ou ajustar qualquer efeito visual, textura de efeito,
  folha em assets/efeitos, RastroDoGolpe, EfeitosVideo ou as ferramentas de efeito em
  tools/sprites, e sempre que o pedido falar de VFX, efeito, rastro, corte de ar, faísca, dano,
  impacto, fumaça ou poeira.
---

# VFX do jogo de luta

O estado atual, com o histórico de cada decisão, está em `docs/vfx-continuar.md`: leia antes de
mexer. As referências analisadas estão em `docs/efeitos-referencia-tokon.md` (os tipos de VFX e a
proposta por personagem) e `docs/efeitos-referencia-fighterz.md`. A lista de imagens a gerar e os
pedidos por tipo estão em `docs/efeitos-gpt.md`. Um guia geral de VFX 2D (durações, jeitos de
fazer rastro) está na skill vizinha `vfx-2d`; onde ele discordar daqui, vale isto.

## O que o dono do jogo já decidiu

Cada linha custou pelo menos uma rodada errada. Não reabra sem ele pedir.

1. **Julga-se no jogo rodando, em vídeo.** Não por descrição nem pela imagem solta. Grave o
   golpe e mande o vídeo, normal e em câmera lenta. Quem aprova é ele.
2. **Desenho por código só em nível profissional** (regra revista pelo dono em 10/11: "só
   aceitar código de ficar nível profissional"). Formas simples desenhadas em código foram
   reprovadas três vezes ("muito amador") e continuam reprovadas. O caminho padrão segue sendo
   imagem ou vídeo gerado (Codex, ou os vídeos que ele mesmo gera), com o código posicionando,
   dobrando, animando e dando o tempo. Desenho feito no programa (EffectCraft: formas, ruído,
   partículas, raio) só vale se seguir técnica profissional e chegar ao nível da referência:
   - medido numa referência aprovada ou em técnica documentada (faixas chapadas, erosão,
     brilho em duas cores, tempo de dois em dois, faísca em camadas; ver
     `reports/VFX de luta estilo anime.md` e a skill `effectcraft`);
   - comparado lado a lado, no jogo, com o que está lá hoje ou com a referência dele;
   - se ficar com cara de forma geométrica simples, de confete ou de "programa", não serve:
     volta para imagem ou vídeo gerado.
   Quem diz se chegou ao nível é ele, no vídeo; até lá nada disso vai para a branch.
3. **Referência é inspiração, não cópia.** Os jogos de referência mostram o que ele quer dizer;
   a arte é nossa. Quadros de outros jogos não entram no repositório.
4. **Cada personagem tem o VFX dele.** A forma vem do que bate (garra, punho pesado, energia),
   não o mesmo desenho pintado de outra cor. Efeito igual para todos é "modesto". Ainda por
   fazer: ver a tabela em `docs/efeitos-referencia-tokon.md`.
5. **O rastro mostra de onde o golpe veio e para onde foi.** Segue o caminho real do membro,
   detectado na animação (`tools/sprites/linha_de_acao.py`). Nunca um arco inventado por regra.
6. **O rastro é procedural:** nasce no começo do caminho, cresce junto com o golpe e se apaga
   pelo fundo, de trás para a frente. Nunca aparece montado de uma vez.
7. **O rastro é exagerado:** o corte de ar é do tamanho do membro ou maior.
8. **O traço do rastro** é irregular e pontudo, nunca linhas retas e paralelas nem corte reto na
   ponta. A última referência dele é um véu branco meio transparente, largo, com muitos riscos
   finos dentro.
9. **Camadas na tela:** lutadores, depois o efeito de dano, e o rastro por cima de tudo, a 75%
   de opacidade. As fotos do Tōkon que ele mandou mostram o rastro na frente: a regra de
   "efeito grande atrás do personagem" que aparece na pesquisa é do Guilty Gear Strive e fala
   da faísca de acerto, não do rastro. Não levante isso como dúvida de novo.
10. **Efeito de dano por força:** fraco, médio, forte e baixo, cada um com o vídeo dele
    (`art/vfx/dano`).
11. **Arte que já traz efeito desenhado:** se o efeito está em cima do membro, a arte fica e o
    nosso VFX combina com ela. Não se regenera animação até sair limpa. Nunca dois efeitos um em
    cima do outro: golpe com efeito na arte não ganha rastro (`ARTE_COM_EFEITO`).
12. **Estilo:** efeito é luz, sem contorno preto; é a coisa mais clara da tela; tons chapados,
    do escuro para o branco.
13. **Técnica profissional, não palpite.** Ele parou as tentativas no olho e pediu a técnica dos
    estúdios: está em `reports/VFX de luta estilo anime.md`. O rastro segue a receita da Arc
    System Works (inclusive a **base escura sob o corte**, que ele mandou fazer como eles), com
    erosão e rampa de cor no lugar dos desenhos à mão. O desenho do PC precisa do equivalente,
    porque é no vídeo gravado lá que ele julga.

## Erros que já aconteceram (não repita)

| Erro | O certo |
|---|---|
| Onda de ar na ponta do golpe, quando o pedido era o arco do movimento | Perguntar qual dos dois antes de construir |
| Arco escolhido por regra (soco reto, chute sobe) | Caminho detectado, quadro a quadro, com 3 ou mais desenhos |
| Seguir a ponta só pela aparência da imagem | Pulava para o pé de apoio; usar o modelo de pose e conferir no olho |
| Riscos retos, paralelos, cortados retos | Irregulares, tamanhos diferentes, pontudos nas duas pontas |
| Fita em fatias soltas com textura transparente | As emendas aparecem: desenhar numa malha só |
| Faísca presa na borda do rival com os lutadores colados | Ir até a ponta desenhada do membro |
| Dizer que conferiu tudo tendo visto poucos golpes | Dizer quantos golpes e de quais personagens viu |

## Como fazer

**Gerar** (o Codex está descrito em `docs/cenarios-com-codex.md`):

- Luz em fundo preto puro; fumaça e poeira em fundo verde; só o efeito, sem personagem nem texto.
- Apontando para a direita (o jogo espelha). O pedido usado fica ao lado da imagem, em `art/vfx/`.
- Para manter a família, anexe a imagem que já foi aprovada.

**Colocar no jogo** (`android/app/src/main/assets/efeitos/<nome>/`):

| Entrada | Ferramenta |
|---|---|
| Imagem parada | `tools/sprites/efeito_imagem.py` (`--nuvem` para poeira, `--ancora chao`, `--fundo verde`) |
| Vídeo em fundo preto | `tools/sprites/efeito_video_preto.py` (`--quadros`, `--origem`, `--escala-mundo`) |
| Vídeo em fundo verde | `tools/sprites/efeito_video.py` |
| Textura da fita do rastro | `tools/sprites/rastro_fita.py` |
| Caminho de cada golpe | `tools/sprites/linha_de_acao.py --write --folha <prefixo>`, depois `build_characters.py --write` |
| Normal map de uma folha de lutador (motor 4 de luz; botão LUZ do Modo Teste). **Refez a folha, refaça o mapa** (`--refazer`) | `tools/sprites/normal_map.py <id ou folha>`; ver "Motor de luz 4" no `docs/vfx-continuar.md` |
| Smear (o membro esticado na arte; troca de desenho com efeito do vídeo) | `tools/sprites/smear.py <id> <GOLPE> [--trocar 4=8] [--comprimento 1.2,1.0]`, depois `build_characters.py --write` |

Os nomes que a luta toca estão em `GameView.efeitoDeGolpe` e nas tabelas do
`docs/vfx-continuar.md`. Folha de efeito com no máximo 4096 px de lado.

**Gravar e conferir:**

```
cd android
gradle :pc:gravarLuta -Proteiro="segura:D espera:1.8 solta:D grava:55:g tecla:L fim"
gradle :pc:gravarLuta -Pdupla=p04,p12,player_base,p02 -Proteiro="espera:0.6 grava:45:g tecla:L fim"
```

- `J K L` são fraco, médio e forte. `espera:1.8` encosta no rival; com menos o golpe erra.
- Olhe os quadros um a um antes de mandar (a tela pula desenhos; o defeito some no vídeo corrido).
- A gravação é o desenho do PC. O celular é OpenGL: diga que não foi visto no aparelho.
- `gradle :pc:testarMotor` tem 3 falhas antigas que não são do VFX (não acham `historia/` e a
  pasta de ultras). Qualquer outra é nova.

## Durações de partida

Do guia `vfx-2d`, para começar; o que vale é o olho dele no jogo.

| Efeito | Quadros desenhados | Tempo na tela |
|---|---|---|
| Rastro de golpe | 3 a 4 | 100 a 150 ms |
| Faísca | 3 a 5 | 50 a 150 ms |
| Fumaça | 6 a 8 | 600 a 700 ms |
| Explosão | 8 a 12 | 500 a 1000 ms |

A pausa no acerto do jogo (8, 11 e 16 quadros por força) veio de uma revisão própria e é de jogo
de luta: não troque pelos números do guia, que são de jogo de ação.

Três jeitos de fazer rastro: desenho pronto da curva (o melhor traço), cópias meio transparentes
dos últimos quadros do lutador, ou fita por código no caminho (o que está no jogo; é onde o traço
sai pior).

## Regras do repositório

- Push direto na `game-luta-sprite-fable`, depois de `git fetch` e `git merge` do origin. Sem
  rebase, sem force, sem PR. O APK sai sozinho na release `build-game-luta-sprite-fable`.
- A `game-luta-sprite-fable` é o que ele instala e testa. Trabalho pela metade ou que não foi
  visto funcionando: avise e pergunte antes de mandar para lá.
- Anote cada rodada no `docs/vfx-continuar.md`: o que ele pediu, o que foi feito e o que ficou.
