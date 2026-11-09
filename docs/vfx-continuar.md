# VFX: onde paramos e como continuar

Roteiro para continuar o trabalho de efeitos visuais em outra sessão (a que tem o Codex e a rede
liberada). Escrito em 09/11, na `game-luta-sprite-fable`.

## Em uma frase

Efeito desenhado por código foi tentado de novo e reprovado de novo. O caminho é **imagem gerada
(GPT/Codex) colocada no jogo e julgada com o jogo rodando**. As ferramentas para isso estão
prontas; falta gerar as imagens e escolher o estilo vendo.

## O que já estava decidido (e eu não segui)

- [efeitos-referencia-fighterz.md](efeitos-referencia-fighterz.md): **efeito é luz, sem contorno**.
  Miolo branco, cor saturada, halo macio; o contorno preto fica para os personagens.
- [efeitos-gpt.md](efeitos-gpt.md): a lista de imagens para gerar, em lotes, com a regra de cada
  tipo (LUZ em fundo preto, OBJETO e FUMAÇA em transparente, FOLHA de 4 quadros, TINTA) e o
  pedido de cada uma. O Lote 1 é o acerto, a defesa e o dano.
- [efeitos-de-video.md](efeitos-de-video.md): os efeitos que vêm de vídeo, e o registro de que os
  efeitos desenhados em código já tinham saído do jogo uma vez.
- [revisao-base/direcao.md](revisao-base/direcao.md), regras E1 a E7 e E13: um efeito-âncora e
  todos os outros derivados dele; uma paleta, uma taxa de quadros; tamanho preso ao alcance do
  golpe; duração presa à pausa do golpe; um efeito por atacante.

## O que foi feito nesta rodada

| O quê | Situação |
|---|---|
| Impacto de ar redesenhado em borda dura com miolo de nanquim (`tools/sprites/impacto_ar.py`) | Está no jogo. Contraria o "luz, sem contorno" acima e tem cara de recorte. Fica só até uma imagem substituir |
| Partículas dos poderes por código (fagulhas, estouro, fita) | **Reprovadas ("muito amador") e retiradas.** Confete geométrico, sem luz, brigando com a onda luminosa do Ryūga |
| Imagem parada → efeito do jogo (`tools/sprites/efeito_imagem.py`) | Pronto e testado no jogo com uma imagem de teste |
| Gravar o jogo sem tela por roteiro (`gradle :pc:gravarLuta`, ou `tools/sem-gradle/`) | Pronto e testado |

Para voltar o impacto de ar ao da review (macio, branco, sem contorno):

```
git checkout 10cef7e -- tools/sprites/impacto_ar.py android/app/src/main/assets/efeitos/impacto_ar android/app/src/main/assets/efeitos/impacto_ar_super
```

## O estilo ainda está em aberto

Três direções foram postas na mesa; nenhuma foi escolhida. A decisão é para ser tomada **vendo
imagens dentro do jogo**, não por descrição.

1. **Luz de anime** (a que os documentos acima já descrevem e a recomendada): a âncora é a onda e
   o Super do Ryūga, que já estão no jogo. Miolo branco, cor do personagem, brilho macio, poucos
   quadros segurados. Golpe físico no mesmo traço, branco e pequeno.
2. **Mangá**: borda dura, preto e branco, tinta. Foi o que saiu por código e ficou amador; só
   valeria como arte gerada, e ainda assim briga com a onda luminosa.
3. **Contido**: quase nada no golpe físico (como a review deixou) e efeito só nos poderes.

## Passo a passo na sessão com o Codex

O Codex está descrito em [cenarios-com-codex.md](cenarios-com-codex.md) (instalar, entrar com a
conta do ChatGPT, `codex exec`).

### 1. Gerar

Começar pelo **Lote 1** do [efeitos-gpt.md](efeitos-gpt.md): `acerto.png` (a referência do lote),
`acerto_forte.png` e `defesa.png`. Para testar estilo, vale pedir primeiro **uma imagem só**
(sem o trecho FOLHA) em duas ou três variações: é mais rápido e já mostra se a direção presta.

Regras que fazem a imagem servir:

- efeito luminoso em **fundo preto puro**; só o efeito, sem personagem, sem texto;
- **quadrada**, efeito centralizado, com folga nas bordas;
- **apontando para a direita** (o sentido do golpe; o jogo espelha).

Salvar em `art/vfx/` (por exemplo `art/vfx/acerto_01.png`), com o pedido usado ao lado
(`art/vfx/prompt_acerto_01.txt`), como nos cenários.

### 2. Colocar no jogo

```
# uma imagem parada: o estouro (nasce pequena, passa do tamanho, assenta, some) vai nos quadros
python3 tools/sprites/efeito_imagem.py art/vfx/acerto_01.png --nome impacto_ar

# uma FOLHA de 4 quadros lado a lado (o formato do efeitos-gpt.md)
python3 tools/sprites/efeito_imagem.py art/vfx/acerto.png --nome impacto_ar --quadros 4

# só olhar os quadros, sem gravar no jogo
python3 tools/sprites/efeito_imagem.py art/vfx/acerto_01.png --previa /tmp/previa.png
```

Os nomes que a luta toca hoje (`GameView.efeitoDeGolpe`): `impacto_ar` (acerto de golpe e de
projétil), `impacto_ar_super` (Super e Ultra), `defesa`, `defesa_2`, `defesa_3` (golpe
defendido, um de cada vez) e `defesa_poder`. Usar um desses nomes troca o efeito na hora; para
voltar, `git checkout -- android/app/src/main/assets/efeitos/<nome>`.

O que o motor já faz sozinho com a folha:

- **Tamanho**: `--tamanho` é o lado maior em px do mundo no tamanho 1 (128 no acerto forte); o
  jogo desenha o fraco a 0,625, o médio a 0,8125, e o Super a 1,25. Defesa: 0,6 / 0,7 / 0,8.
- **Tempo**: a folha é esticada para a pausa do golpe + 8 quadros (defesa + 4).
- **Lugar**: no ponto do contato, 12 px para dentro de quem apanha, espelhada pela direção.
- **Um por atacante**: o novo tira o anterior.

O que **não** existe e pode fazer falta para o estilo "luz":

- **Mistura aditiva**: a folha é desenhada com transparência comum. O brilho sobre fundo claro
  fica mais apagado do que no fundo preto da imagem. Se a luz parecer fraca no jogo, é aqui.
- **Pintar na cor do personagem** (a TINTA do efeitos-gpt.md): o desenho só muda a transparência.

### 3. Ver no jogo

Com o Gradle funcionando:

```
cd android
gradle :pc:gravarLuta -Proteiro="segura:D espera:1.3 solta:D grava:60:h tecla:L fim"
gradle :pc:gravarLuta -Proteiro="espera:1 grava:90:esp especial:J fim super:3000 grava:150:sup tecla:I fim"
gradle :pc:gravarLuta -Pdupla=p11,p13,player_base,p04 -Proteiro="espera:1 grava:80:b especial:J fim"
#   →  android/pc/build/gravar-luta/<nome>-000.png ...
```

Sem o Gradle (o Maven bloqueado): [tools/sem-gradle/](../tools/sem-gradle/README.md) faz o
mesmo só com `javac`.

O módulo `:pc` já tinha outras gravações sem tela que podem servir: `gradle :pc:testeMovimentos`
(os movimentos e golpes de um lutador), `:pc:revisarAnimacao`, `:pc:capturar` e `:pc:medirLuta`
(`gradle :pc:tasks --group verification` lista todas).

Os passos do roteiro estão no começo de `GravarLuta.java`. O jogador começa em x=1020 e o rival em
x=1580: para um golpe de perto acertar, andar 1,3 s antes. O especial de comando sai melhor com o
Ryūga (`especial:J`); em alguns personagens o mesmo comando solta outro golpe.

Para olhar e mandar:

```
python3 tools/sem-gradle/folha_quadros.py <pasta> h /tmp/h.png 8,12,16,20,24,28 - 3
ffmpeg -framerate 60 -i <pasta>/h-%03d.png -pix_fmt yuv420p /tmp/h.mp4
```

A gravação sai pelo desenho do PC em Java2D. No celular a luta é OpenGL: o que passar aqui ainda
precisa ser visto no aparelho (o APK sai sozinho na release `build-game-luta-sprite-fable` a
cada push).

### 4. Decidir e espalhar

Quando uma imagem de acerto agradar no jogo, ela vira a **âncora** (regra E1): as outras do lote
são geradas anexando ela ("mesmo estilo de traço e de brilho desta imagem") e só então vale gerar
os lotes seguintes do efeitos-gpt.md.

## Fora do VFX, o que ficou pendente

- **Jogar o APK e dizer se os golpes ficaram diferentes de um personagem para outro.** É a
  mudança principal da branch ([balanco/normais-fable.md](balanco/normais-fable.md)) e ainda não
  foi testada na mão.
- **Equilíbrio**: na simulação CPU contra CPU, Krüger e Ryūga ganham demais (79% e 76%) e
  Natisuko e Bonnie perdem demais (21% e 26%). Não foi mexido.
- **Capturas de outros jogos** em `docs/revisao-base/referencias/` (cerca de 90 imagens): decidir
  se ficam no repositório.

## Regras do repositório (valem em qualquer sessão)

- Antes do push: `git fetch` e `git merge` do origin, sem rebase e sem force.
- Não abrir PR sem pedir. Não mexer na `apk-export-sprite-7659823`.
- Nunca commitar `art/keys/<id>/tamanho.json` sem o pacote do personagem.
- APK e build de PC saem por release do GitHub, não por artefato do Actions.
