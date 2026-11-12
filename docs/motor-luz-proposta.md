# Motor de luz e sombra: proposta (fase 2, 12/11)

Base: `reports/Motor de luz e sombra.md`. Branch `claude/motor-luz-definitivo`. Nada disto está
no jogo; os números daqui são ponto de partida e se acertam no protótipo (fase 3).

## A ideia em uma frase

**A arte é o teto.** O cenário nunca soma luz no lutador: ele diz de que cor o branco da roupa
fica no lado aceso e no lado de sombra, e o motor multiplica a arte por isso. O lado aceso, no
máximo, mostra a arte como foi pintada. Estourar deixa de ser possível pela conta, não por ajuste.

## O modelo

Por ponto do corpo, nesta ordem:

1. **O lugar** (o corpo inteiro): a arte é multiplicada pela cor de `sombra` do cenário, com um
   degradê para a cor do `chao` nos pés. É isto que faz o lutador pertencer à noite ou ao dia.
2. **A luz que manda** (uma só por lutador): o que está virado para ela sobe da cor de `sombra`
   até a cor de `luz`, em rampa macia (sem dois tons chapados, que viraram mancha).
3. **A borda**: o fio de contraluz do lado da luz, como hoje, mas misturado de um jeito que não
   passa do branco e bem mais fino na conta.

Uma luz por lutador, sempre. Os pontos de luz do cenário **não somam**: cada ponto perto puxa a
direção e a cor dessa única luz para si, com peso que cai suave até zero no alcance (a regra do
Guilty Gear Xrd: substituir pela distância). Vários pontos se misturam pelo peso, sem pulo.

## O que cada cenário declara

```
"luzLutadores": {
  "luz":     "#E4D8FF",    // a cor que o branco vira no lado aceso (no máximo #FFFFFF = a arte)
  "sombra":  "#8C84B4",    // a cor que o branco vira no lado de sombra (a cor do lugar)
  "chao":    "#7A7090",    // opcional: a cor que sobe do chão nos pés
  "direcao": [1, -1],      // de onde vem a luz que manda (lua, sol, teto)
  "borda":   0.5           // força do fio de contraluz (0 desliga)
}
```

Saem `cor`, `forca`, `brilho`, `pes` e `contraluz`. Os 12 cenários são convertidos por conta e
depois acertados um a um olhando o vídeo (trabalho de Opus, com receita, depois de aprovado).

## O que sai, o que fica

| Hoje | No motor novo |
|---|---|
| Tinta do cenário (`cor`, `forca`, `brilho`, `pes`) | Vira `luz`, `sombra` e `chao` |
| Contraluz somada | Fica como `borda`, sem somar |
| Volume do motor 4 (soma no lado aceso, exagero 2,3) | **Sai.** O normal map passa a só escolher entre `sombra` e `luz` |
| Luz do impacto no corpo (motor 3, 16 leituras por ponto) | **Sai.** Dura o tempo da faísca e ninguém vê |
| Pontos de luz (`lutadores`, `alcance` no `stage.json`) | **Ficam, no mesmo formato**, com o motor misturando todos (ver abaixo) |
| Sombra no chão (`sombraLutadores`, focos) | Fica. Sem `x`/`y` declarados, a direção passa a vir de `direcao` |
| Reflexo no chão | Fica como está |
| Botão LUZ do Modo Teste | Fica até a aprovação: NOVO / ANTIGO / OFF, para comparar no aparelho |

**Pontos de luz.** As marcações dos 7 cenários continuam valendo sem mexer. Muda o sentido de
`lutadores`: de "força somada" para "quanto este ponto toma o lugar da luz do cenário quando o
lutador está nele" (0 a 1; os 0,4 e 0,5 de hoje servem). Como o motor mistura, as fileiras de
lanternas podem ser marcadas inteiras depois (o lado aceso não pula mais). À noite um braseiro
aparece bastante (o lado virado para ele sobe da sombra até a cor do fogo); de dia quase nada,
que é o certo.

## Lutadores sem normal map (18 dos 20)

O motor não depende do mapa. Sem mapa, a rampa entre `sombra` e `luz` é um degradê liso que
atravessa o corpo na direção da luz (o que o anime faz na composição), mais a borda. Com mapa, a
mesma rampa segue o volume (braço, peito, rosto). O lugar, a noite, os pontos e a borda valem
para todos. Gerar mapa para o resto do elenco fica para decidir no vídeo da fase 3, que terá o
Ryūga e o Kōga com e sem mapa lado a lado: se a diferença for pouca, os mapas saem do jogo.

## Custo no celular (pela conta; nada foi medido no aparelho)

- Shader: sem mapa, nenhuma leitura de textura a mais que hoje; com mapa, uma (como hoje). Saem
  as 16 leituras da luz do impacto. Mesmo programa, mesmo lote, nenhuma passada nova.
- Memória: o motor não usa nenhuma. Os mapas continuam em 1/32 da folha (medido no PC: +32,5 MB
  para a dupla, sobre 1.359 MB). Para o elenco todo seriam ~+22 MB no APK e 2,7 h de geração.
- Processador: misturar até 24 pontos por lutador por quadro; desprezível.
- Com o OPENGL desligado no Modo Teste, o lutador recebe só a cor média entre `luz` e `sombra`.

## Escolhas (resolvidas pelos princípios em 12/11)

O dono: "o que conta é conceito de luz e sombra, que já existe, então temos que seguir isso".
Ficou: (1) o motor (b), uma luz que manda por lutador; (2) o nível da noite tirado de cada
cenário; (3) os pontos ficam e misturam; (4) a luz do impacto no corpo sai; (5) normal map para o
resto do elenco não se gera agora. O protótipo e as medidas estão no `docs/vfx-continuar.md`.
A lista abaixo é a que foi apresentada.


1. **O motor.** (a) Só o lugar e a borda, sem lado aceso (o mais simples; é o que KOF XIII e
   Hollow Knight fazem); (b) **o modelo acima, com uma luz por lutador (recomendo)**; (c) várias
   luzes por ponto com mapa em todo o elenco (o mais caro, e a pesquisa avisa que mapa gerado
   forte fica com cara de plástico). O protótipo mostra (a) e (b); (c) eu não faria.
2. **Quanto o lutador escurece à noite.** É a mudança que mais se vê: hoje ele fica quase na cor
   de dia em cima de um fundo escuro. Gravo dois níveis (leve e fundo) para você escolher.
3. **Pontos de luz:** ficam e misturam (recomendo), ficam como hoje, ou saem.
4. **Luz do impacto no corpo:** sai (recomendo) ou volta como mais um ponto na mistura.
5. **Normal map do resto do elenco:** decidir depois do vídeo.

## O que não sei ainda

Se o degradê liso dos lutadores sem mapa fica bom em pose aberta (chute, queda); quanto o mapa
acrescenta com a rampa macia e sem exagero; como fica na tela do celular. As três respostas
saem da fase 3, menos a última, que só você vê.
