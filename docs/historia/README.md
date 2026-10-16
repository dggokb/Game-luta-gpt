# Modo história: mangá animado entre as lutas

> Motor das páginas (padrão do jogo inteiro): [motor-paginas.md](motor-paginas.md).
> Motor das conversas durante as lutas (parceiro de tag, adversário esperando): [motor-conversas.md](motor-conversas.md).
> Motor das cinemáticas dentro da luta (faixas de cinema, o roteiro guia os lutadores, falas, onomatopeias, fim da luta): [motor-cinematicas.md](motor-cinematicas.md).
> Torneio (escolha do irmão, sorteio, chave de 8 duplas fixas, emboscada fora da arena, duelo 1x1): [torneio.md](torneio.md).

A história vem do documento "História Base — Torneio dos Irmãos" (Drive, pasta
game-luta-gpt). Este documento descreve **como ela é contada no jogo**. Os episódios
`ep00` e `ep01` são o MVP: textos de rascunho para revisar, com arte provisória até as
imagens chegarem (prompts em [prompts-ep00-ep01.md](prompts-ep00-ep01.md)).

## A proposta: anime episódico em páginas de mangá

O documento pede um modo história que funcione "como um anime episódico, e não como uma
sequência de telas de próxima luta". A forma mais barata de produzir isso e que combina com
o que o jogo já tem é o **mangá animado**, a mesma linguagem da Página Final do ultra:

- **Cada episódio** segue a estrutura do documento: cartão de título → páginas de mangá
  (preparação, convivência, conflito) → luta → páginas pós-luta → "Próximo episódio".
- **Cada página** se monta painel por painel: o painel "bate" na página com o mesmo
  tranco do ultra, os balões estalam e o texto é digitado. O toque avança, como virar a
  página. Lembranças (o Prólogo, os flashbacks do pai) têm a **página preta e as cores
  sépia**, a convenção de flashback dos mangás.
- **Arte**: imagens paradas geradas no GPT, uma por painel, com o mesmo estilo dos painéis
  do ultra. Movimento vem do motor (zoom lento de câmera, painel batendo, linhas de
  velocidade, tremor), não da arte, então não precisa de animação nem de vídeo.
- **Gancho de anime**: todo episódio fecha com o cartão "PRÓXIMO EPISÓDIO" + "CONTINUA...".
- **Referências visuais**: os concepts dos personagens da história (Takatoshi aos 45 e aos 55,
  Dafu, os irmãos aos 8 anos) e o logo estão em `art/concepts/`. O logo aparece no cartão de
  título de cada episódio (`historia/logo.jpg`).

O que fica para depois do MVP (quando o ritmo estiver aprovado): vozes e trilha, escolha de
lado no chaveamento (campanha do Ryūga e campanha do Kōga), lutas variáveis sorteadas,
salvar progresso e um menu de episódios.

### Como a temporada se divide (proposta)

| Episódio | Conteúdo | Luta |
|---|---|---|
| 0 — O Pai que Partiu | Infância no dojo Shūshinkan, o treino (tutorial), o convite, a divisão dos irmãos, o pai parte e não volta. **Pronto** (11 páginas) | Treino Ryūga × Kōga (tutorial) |
| 1 — Duas Cartas | A escolha do irmão (Ryūga ou Kōga), dez anos depois, treino, as duas cartas, motivos de cada um. **Pronto**: começo e fim separados por irmão (veja abaixo) | Treino jogador × rival, interrompido pela campainha numa cinemática |
| 2 — O Parceiro | Antes do torneio, cada irmão convence o parceiro a lutar com ele | 1x1: Ryūga × Yin-lu (p02) ou Kōga × Brucy (p04). Antes da luta, páginas prontas de cada irmão: `pag1_ryuga` a `pag4_ryuga` e `pag1_koga` a `pag4_koga` |
| 3 — O Sorteio | O templo, a regra dos capitães, a formação das duplas, o chaveamento em lados opostos | 1ª luta de cada lado |
| 4 a 7 | Episódios alternando o lado do Ryūga e o lado do Kōga (lutas fixas de história + lutas variáveis) | uma por episódio |
| meio da temporada | A revelação de que o desejo é real e pode ressuscitar a mãe | luta fixa |
| penúltimo | Final 2×2 entre as duas equipes e a regra escondida: a dupla vencedora luta entre si | final 2×2 e duelo |
| último | O Campeão é Takatoshi; o segredo do contrato e do Dafu | chefe final |

## Como funciona no jogo

- **MODO HISTÓRIA** no menu inicial ([../menu-inicial.md](../menu-inicial.md)) toca a
  temporada desde o Prólogo e emenda um episódio no outro (depois do "Próximo episódio" do
  ep00 vem o ep01); no fim da temporada volta ao menu. Sem os botões de teste nem o DEBUG.
  **◀ MENU** (canto de cima à esquerda nas páginas, botão do topo nas lutas) e a tecla
  voltar abrem a pausa (CONTINUAR / MENU INICIAL).
- No MODO TESTE, o botão **HISTÓRIA** do HUD (abaixo de DUPLA) toca a mesma temporada, com
  os botões de teste, e no fim volta para a seleção.
- **PULAR** (canto de cima à direita) vai direto para a próxima luta ou para o "próximo
  episódio". Com controle, qualquer botão vira a página e B pula (no modo história, voltar e
  START abrem a pausa).
- Numa luta da história, o jogo monta as duplas do episódio, liga a IA e mostra o rótulo
  da luta (ex.: TREINO). Depois do KO e da comemoração, a história continua. Uma derrota
  repete a luta, a não ser que o passo diga `"repetirSeDerrota": false`.
- No teste, voltar para a seleção (DUPLA ou SELEÇÃO) abandona o episódio; no modo história,
  MENU INICIAL na pausa abandona o episódio (ainda não há save).

O motor é o módulo `android/story-core` (Java puro, como o `ultra-core`): `PaginaHistoria`
toca o episódio e desenha; o `GameView` só repassa toques, desenha por cima e faz as lutas.

## Formato dos episódios

`android/app/src/main/assets/historia/temporada.json` define o elenco (nome e cor nos
balões; a cor também pinta a arte provisória) e a ordem dos episódios. Cada episódio fica em
`historia/<id>/episodio.json`, com as imagens dos painéis na mesma pasta. O campo `logo`
aponta a imagem do cartão de título (sem ele, o cartão escreve o nome do jogo).

```json
{
  "id": "ep01", "numero": 1, "titulo": "Duas Cartas",
  "passos": [
    { "tipo": "titulo" },
    { "tipo": "pagina", "layout": "faixa-e-dois", "tom": "papel", "paineis": [
      { "imagem": "pag1_q1.jpg", "cena": "dojo", "personagens": ["ryuga", "koga"], "foco": 0.4,
        "efeito": "velocidade", "sfx": "PÁ!", "posicaoSfx": [0.75, 0.3],
        "baloes": [
          { "tipo": "fala", "quem": "koga", "texto": "...", "pos": [0.5, 0.2], "rabo": [0.5, 0.5] }
        ] }
    ] },
    { "tipo": "luta", "rotulo": "TREINO", "dupla": ["player_base", "p02"], "cpu": ["p03", "p04"] },
    { "tipo": "proximo", "titulo": "O Sorteio", "linhas": ["...", "..."] }
  ]
}
```

| Campo | Valores |
|---|---|
| `tipo` do passo | `titulo`, `pagina`, `luta`, `proximo`, e os do torneio: `escolha`, `sorteio`, `chave` ([torneio.md](torneio.md)) |
| `torneio` (passo de luta) | `true`: luta contra o próximo adversário da chave |
| `dupla` / `cpu` | `[titular, parceiro]`, ou `"jogador"` / `"rival"` (a dupla fixa do irmão escolhido / do outro) |
| `irmao` (qualquer passo) | `ryuga` ou `koga`: o passo só toca na campanha desse irmão |
| `emboscada` (passo de luta) | `true`: a emboscada fora da arena, contra quem perdeu a outra quarta do lado do jogador |
| `duelo` (passo de luta) | `true`: o 1x1 da regra escondida, capitão × parceiro |
| `layout` | `splash` (1), `duas-colunas` (2), `tres-colunas`, `tres-faixas`, `faixa-e-dois`, `dois-e-faixa` (3), `quatro`, `grade` (4). Sem `layout`, vale o padrão pela quantidade de painéis |
| `tom` | `papel` (normal) ou `flashback` (página preta, cores sépia) |
| `cena` (arte provisória) | `dojo`, `dojo-noite`, `casa`, `estrada`, `templo`, `ceu`, `chuva`, `carta` |
| `enquadramento` | `plano` (corpo inteiro), `close` (rosto), `detalhe` (objeto) |
| `efeito` | `velocidade`, `impacto`, `chuva`, `brilho` (desenhados por cima da imagem) |
| `tipo` do balão | `fala`, `pensamento`, `grito`, `narracao` |
| `pos`, `rabo`, `posicaoSfx` | frações da caixa do painel: `[0, 0]` topo esquerdo, `[1, 1]` base direita. `rabo` é para onde aponta o bico do balão (a boca de quem fala) |
| `foco` | 0 a 1: que parte da imagem aparece quando o painel é mais largo que ela |
| `conversa` (passo de luta) | arquivo da conversa durante a luta, dentro de `historia/` (ex.: `ep00/conversa_treino.json`). Veja [motor-conversas.md](motor-conversas.md) |
| `cinematica` (passo de luta) | arquivo da cinemática dentro da luta, dentro de `historia/` (ex.: `ep01/cinematica_treino.json`): num gatilho (vida baixa, começo, tempo, KO) a luta vira cena. Veja [motor-cinematicas.md](motor-cinematicas.md) |

Sem a imagem, o painel mostra a arte provisória (cenário, silhuetas nas cores do elenco e
o nome do arquivo esperado). Assim dá para escrever e testar o ritmo antes da arte.

## Páginas prontas: o padrão do jogo

Toda a história é contada com **páginas de mangá prontas** (a página inteira numa imagem, com
balões e onomatopeias) e a **leitura guiada** do jogo: a página aparece inteira, a câmera vai
quadro a quadro com efeitos de ação, os balões são escritos no idioma do aparelho e, depois
do último quadro, segue direto para a próxima página.

Como funciona, o formato do `episodio.json`, os efeitos, os idiomas e o passo a passo de
uma página nova (gerar, recortar, preencher, traduzir, revisar):
**[motor-paginas.md](motor-paginas.md)**.

As páginas com painéis montados pelo jogo (`layout`, `paineis`, arte provisória) continuam
funcionando e servem de rascunho até a página pronta existir.

### Páginas de cada irmão

A história se ramifica pela campanha: uma página com `"irmao": "ryuga"` só aparece para quem
escolheu o Ryūga, e uma com `"irmao": "koga"`, só para quem escolheu o Kōga. Página sem
`irmao` é comum aos dois. A versão do Kōga leva o sufixo `_koga` no arquivo e fica logo
depois da versão do Ryūga no `episodio.json`.

Numeração da temporada: as páginas são contadas em sequência desde o Prólogo (ep00 = 1 a 11,
ep01 = 12 a 22).

| ep01 | Global | Ryūga | Kōga |
|---|---|---|---|
| começo | 12, 13, 14 | `pag1`, `pag2`, `pag3` (treino no dojo, Dafu) | `pag1_koga`, `pag2_koga`, `pag3_koga` (volta a Tóquio, briga no beco, chega ao dojo e ouve o Ryūga lá dentro) |
| meio | 15 a 21 | `pag4` a `pag10` (comuns) | iguais |
| fim | 22 | `pag11` (conversa com o Dafu) | `pag11_koga` (sai do dojo pensando na dupla) |

## Revisar no PC

```bash
cd android
gradle :pc-preview:runHistoria   # grava cada página montada em build/historia/
gradle :story-core:test          # valida os episódios (layouts, elenco, lutadores)
gradle :pc-preview:runCinematica # grava a cinemática do treino do ep01 em build/cinematica/
```
