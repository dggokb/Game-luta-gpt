# Ultra cinematográfico

Todo Ultra do jogo é uma cena de cinema, feita da animação que o personagem já tem mais as
imagens da pasta `android/app/src/main/assets/ultras/<id>/`. Imagem que falta cai num
visual provisório desenhado pelo jogo: dá para jogar antes de ter qualquer arte.

## A cena

1. **Abertura** (`AberturaUltra`, 1 s, luta parada): clarão, faixas pretas de cinema, o
   palco do Ultra (`palco`) toma a tela, o personagem recortado (`corte`) entra com o **nome**
   e o **grito** (do jeito do estilo da abertura, veja abaixo), e um clarão corta de volta para
   a luta.
   Sem `corte`, entra a arte de seleção do personagem (quando existe).
2. **Investida**: a ativação e a investida de sempre, já com as faixas pretas e sem o HUD.
3. **Sequência com câmera** (os Ultras em sequência, feitos dos vídeos): a cada golpe que
   acerta a câmera corta seco para outro plano (`UltraDirector`): plano médio, depois os planos
   do roteiro de câmera do Ultra (close de lado, câmera baixa, plano aberto...), e o close de
   preparação antes do golpe final.
   Cada golpe tem os raios, a tremida e o som de sempre.
4. **Página de mangá com o toque no tempo** (`PaginaFinal`): logo antes do golpe final a luta
   para e a página se monta no modelo do Ultra (`olhos`, `carga`, `golpe`, que bate duas vezes, e `final`); tocar quando o
   anel fecha dá PERFEITO (+25%) ou BOM (+10%) no golpe final. A página estilhaça de volta.
5. **Golpe final**: nos Ultras em sequência, o último golpe da animação sai com o bônus, com o
   impact frame e a câmera lenta do estilo; no P01 e no p06, o raio final (como antes).
6. **Saída**: as faixas saem e o HUD volta.

**Ultra que tira mais que a vida que sobra**: o rival zera no meio, mas continua preso no Ultra
levando os golpes que faltam (com faísca e som, sem dano) e só voa no golpe final, onde entram o
K.O., a câmera lenta e a finalização de mangá. O "K.O." da luta e o parceiro entrando esperam
(`CombatEngine.ultraHoldsKo`). Antes ele saía voando 6 quadros depois de zerar, e o golpe final
acontecia no vento.

O tempo e o dano dos golpes não mudam; a abertura e a página param a luta enquanto rodam.
No motor puro (testes, IA) a sequência corre direto: a tela liga a pausa da página com
`CombatConfig.ultraSequencePage`.

## Estilo de cada Ultra

A cena tem a mesma ordem para todos, mas a direção muda por personagem: o modelo da página,
o estilo da abertura, o roteiro de câmera e o impacto final. Fica no bloco `"estilo"` do
`ultra.json`; trocar é só mudar a palavra (palavra desconhecida volta para o padrão).

```json
"estilo": {
  "pagina": "diagonal",
  "abertura": "racha",
  "camera": "pesada",
  "impacto": "vermelho",
  "camaraLenta": 0.3
}
```

| Campo | Opções |
|---|---|
| `pagina` | `classica` (tiras quase retas), `diagonal` (tiras paralelas inclinadas que entram deslizando, rasgo na cor do Ultra atrás), `leque` (painéis em leque com borda branca, como fotos jogadas, entram girando), `quebrada` (página preta, painéis tortos como vidro quebrado, rachaduras) |
| `abertura` | `faixa` (personagem numa faixa diagonal com o nome), `vertical` (coluna com o nome de cima para baixo), `dividida` (tela partida: o personagem e o rival se encarando), `racha` (a tela preta racha, abre e o personagem sobe pela luz) |
| `camera` | `padrao` (lado, baixo, aberto), `agil` (planos abertos, a câmera troca de lado a cada golpe), `pesada` (câmera baixa e closes, bem perto), `sombria` (planos abertos tortos, fundo bem escuro), ou a lista de planos: `["lado", "baixo", "aberto", "medio", "close"]` |
| `impacto` | `vazio` (impact frame preto com a estrela vazada) ou `vermelho` |
| `camaraLenta` | velocidade da câmera lenta do golpe final (0.3 = bem lenta, 0.5 = leve) |

As imagens servem em qualquer modelo de página: os painéis mudam de lugar e inclinação, mas
mantêm o formato.

| Personagem | Página | Abertura | Câmera | Impacto |
|---|---|---|---|---|
| P01 | clássica | faixa | padrão | vazio |
| p02 | leque | vertical | ágil | vazio |
| p03 | diagonal | racha | pesada | vermelho |
| p04 | diagonal | faixa | pesada | vermelho |
| p05 | quebrada | vertical | ágil | vazio |
| p06 | quebrada | racha | sombria | vazio |
| p07 | clássica | dividida | pesada | vazio |
| p08 | leque | vertical | ágil | vermelho |
| p09 | diagonal | dividida | pesada | vermelho |
| p10 | leque | faixa | ágil | vazio |
| p11 | quebrada | dividida | sombria | vazio |
| p12 | quebrada | racha | sombria | vermelho |

## Trocar nome, grito, cores e onomatopeias

Tudo no `ultra.json` de cada personagem:

```json
{
  "nome": "Dança da Garça",
  "cor": "#3FA7D6",
  "corSecundaria": "#BFF3FF",
  "abertura": { "corte": "corte.png", "palco": "palco.jpg", "grito": "Você não vai me alcançar!" },
  "paineis": {
    "olhos":    { "imagem": "olhos.jpg" },
    "carga":    { "imagem": "carga.jpg", "onomatopeia": "FUUUSH" },
    "golpe":    { "imagem": "golpe.jpg", "onomatopeia": "PAH!" },
    "final":    { "imagem": "final.jpg", "onomatopeia": "HAAAA!" }
  }
}
```

| Personagem | Ultra | Cor |
|---|---|---|
| P01 | Explosão Solar | dourada |
| p02 | Dança da Garça | azul-celeste |
| p03 | Fúria da Casa | dourada (a energia da família é a mesma do P01) |
| p04 | Soco de Rua | laranja |
| p05 | Eclipse | roxa |
| p06 | Abismo | roxa-escura |
| p07 | Herança | ouro |
| p08 | Sinfonia Escarlate | escarlate |
| p09 | Resgate Final | fogo |
| p10 | Resgate Aéreo | azul-água |
| p11 | Autópsia | verde-veneno |
| p12 | Último Suspiro | vermelho-sangue |

Os nomes vêm das fichas dos personagens (`docs/personagens/`), menos o do P01, que já estava
no jogo como "Explosão Solar" (a ficha diz "Raio da Casa").

## Imagens

Prompts prontos para o ChatGPT, por personagem: [prompts.md](prompts.md). Com as imagens
numa pasta, `tools/ultra/preparar_ultra.py --id <id> --entrada <pasta>` recorta cada uma no
formato certo, tira o fundo do corte se precisar e atualiza o `ultra.json`.

O ChatGPT escolhe o tamanho da imagem por conta própria, então o recorte se ajusta a cada uma:
`--foco golpe=0.2` sobe o recorte do painel (0 = topo, 1 = base). O foco escolhido fica gravado
no `ultra.json` (`"foco"` no painel, `"focoPalco"` na abertura) e vale nas próximas vezes.
