# Luz em jogos de luta profissionais — pesquisa (11/11)

Pedido do dono: "como os jogos profissionais fazem essa questão de luz?" → "quero que pesquise".
Fontes lidas na íntegra estão marcadas (lido); as outras vieram de resumo de busca (busca). Os
slides da GDC 2015 da Arc System Works não abriram nesta rede (ggxrd.com bloqueado).

## 1. Arc System Works (Guilty Gear Xrd/Strive, Dragon Ball FighterZ, e o Marvel Tōkon, da mesma casa)

- **Cada personagem tem a sua própria luz, que ilumina só ele.** "Every character has their own
  individual light sources which affect their body and NOTHING else in the scene" (transcrição de
  vídeo sobre a palestra da GDC 2015, busca). O cenário não manda na luz do personagem.
- **Sombra em dois ou três tons, controlada à mão.** O cel shading é "lit, or not, and no value in
  between"; o artista acerta onde a sombra cai editando o modelo (normais e cor de vértice), porque
  qualquer ruído vira mancha (slides de Motomura, GDC 2015, busca).
- **A luz é animada à mão, quadro a quadro, não calculada.** "Arc System Games do not actually light
  in 'real time' per se, but every single option of the shader is MANUALLY edited frame by frame by
  the art team as they make animations" — e por isso uma luz pontual "na mão para fazer um
  kamehameha" não fica certa (README do shader que imita o estilo ASW, lido).
- **Strive:** "a system that allows us to forcibly control the shading and light of individual
  parts, such as hands, legs, and weapons", para ter o visual pedido mesmo fugindo da luz realista
  (entrevista da Epic com a ASW, busca).
- **Fundo com luz limitada** para os personagens destacarem (resumo da palestra, busca).

Consequência: no estilo que o dono quer (Tōkon), o impacto não "acende" o personagem por física;
quando acende, é porque o artista animou aquilo. Efeito e personagem são controlados à parte.

## 2. Capcom, Street Fighter 6 (RE Engine) — palestra de efeitos, 2023 (lido)

- Os efeitos usam **mistura somada** ("an additive blend using AddContrast") e um modo que pinta o
  contorno de uma cor ("EdgeBlend ... blends contours with a specific color").
- Cada golpe é **vários efeitos separados**: o Hadoken tem "buildup, launch, flight, and impact",
  cada um um asset.
- Ajustes por efeito: "Soft Particle and Detonemap" (o efeito escapa do mapeamento de tom e estoura
  mais), e "**Particle Lighting**" (partícula recebendo luz) liga e desliga por efeito.
- **StretchBlur**: borrão esticado só numa área (o rastro dos chutes da Chun-Li), no lugar do
  motion blur.
- Custo: "lighting translucent effects, where shading costs are very high" — no SF6 a maior parte
  dos efeitos não é iluminada; é luz própria (somada), não recebe luz.

## 3. Jogos 2D de sprite

- **Skullgirls:** sprites desenhados à mão num motor 3D com luz em tempo real — num cenário de
  noite, passar pelo poste ilumina o lutador; sprites desenhados no dobro da resolução do jogo para
  ficarem nítidos (prévias de 2011, busca).
- **Broken Age e Monkey Island SE (Double Fine)** — artigo do programador (lido):
  - *Luz ambiente em degradê* no personagem conforme onde ele está: "the backbone of Broken Age's
    lighting system ... used in every scene for every character". É o nosso `luzLutadores`.
  - *Luz local sem normal map* (MISE): desenha a silhueta (alfa) dos personagens numa textura
    reduzida, borra, tira as normais do "relevo" borrado e soma a luz das fontes próximas
    (`n·l / d²`); depois soma por cima da tela ("additive blending"). A fogueira acende só o lado
    do corpo virado para ela, sem nenhum arte a mais.
  - Fontes de luz como manchas radiais que podem ser animadas (piscar com ruído) e presas a
    alguém ("attaching this light source to the hand ... makes it a cool looking torch").
- **Normal map desenhado** (The Siege and the Sandfox, lido): o normal map serve para "esculpir as
  formas grandes", não detalhe — detalhe demais vira "noisy mess" e cara de metal.
- **Dead Cells:** personagens são modelos 3D renderizados como sprite; cenário com normal maps e
  luz 3D dinâmica (busca).

## 4. Golpe: o que se faz no acerto (busca, guias de game feel)

- Piscar de 1 a 2 quadros, em alto contraste, e voltar ("long flashes read as stun UI").
- Pausa do golpe + tremida para o peso; efeitos de tela cheia só nos finais.
- Luz pontual curta presa ao golpe é comum em jogos 3D de ação (guias de VFX), não achei fonte de
  jogo de luta grande dizendo que usa.

## O que serve para o nosso jogo (sprites 2D vindos de vídeo, Canvas/OpenGL no celular)

| Técnica | Quem usa | Custo para nós |
|---|---|---|
| Tom do cenário no lutador | Broken Age, todos | já temos (`luzLutadores`) |
| Contraluz | SF6 (retratos), ASW | já temos |
| Mistura somada nos efeitos de luz | SF6 (AddContrast) | baixo: modo de mistura no OpenGL |
| Efeito escapando do tom (estoura) | SF6 (Detonemap) | já é assim (efeito não leva filtro) |
| Piscar o atingido 1–2 quadros | guias de game feel, 2D clássicos | baixo: o filtro de cor que já existe |
| Luz local sem normal map (silhueta borrada) | Monkey Island SE | médio: uma passada a mais no OpenGL |
| Normal map por quadro | Broken Age, Sandfox, Dead Cells | alto: um mapa por quadro de cada animação |
| Luz animada à mão por golpe | Arc System Works | alto: trabalho de arte por golpe |
