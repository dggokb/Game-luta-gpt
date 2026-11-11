# Prompt para uma sessão Fable: motor 4 de luz (normal map nos lutadores)

Cole o texto abaixo numa sessão Fable no repositório `game-luta-gpt`.

---

Branch: `game-luta-sprite-fable` (faça `git fetch` e `git merge --ff-only` do origin antes; push
direto nela, sem rebase, sem force, sem PR; commit sem nome de modelo).

**Leia antes:** `CLAUDE.md`, a skill `vfx-luta` (`.claude/skills/vfx-luta`), `docs/vfx-continuar.md`
(a seção "Motores de luz 2 e 3") e `reports/Luz em jogos de luta profissionais.md`.

**Contexto:** o jogo é de luta 2D com lutadores em folhas de sprite que vêm de vídeo gerado
(Seedance → `tools/sprites/video_para_sprite.py`), desenhados no celular em OpenGL ES 3
(`android/app/src/main/java/com/gamelutagpt/GlRenderCanvas.java`, `CanvasGl`, `DesenhoGl`).
Já existem: a luz do cenário no lutador (tom, degradê dos pés e contraluz, no shader), a mistura
somada nos efeitos (`"soma"` no efeito.json) e a **luz local** (motor 3): o shader tira uma normal
da silhueta borrada do próprio quadro (16 vizinhos do alfa) e acende o lado do corpo virado para a
luz do impacto. Essa normal só sabe a borda; o miolo do corpo é "chapado".

**A tarefa (problema aberto):** dar aos lutadores uma normal de verdade por quadro, para a luz
local (e a do cenário) mostrar volume dentro do corpo — braço, rosto, dobras — como os jogos 2D
com normal map (Broken Age, The Siege and the Sandfox, Dead Cells; ver o report).

O que precisa ser resolvido, com medida:
1. **De onde vem a normal.** Não há normal map desenhado: os quadros vêm de vídeo. Avaliar gerar
   automaticamente a partir do desenho (luminância e silhueta, estilo SpriteIlluminator/Laigter,
   ou um modelo de estimar normais de imagem), e mostrar se fica bom em arte pintada. O artigo do
   Sandfox avisa: detalhe demais vira ruído e "cara de metal"; o normal map deve esculpir as
   formas grandes.
2. **Memória.** As folhas dos lutadores já ocupam ~1,4 GB carregadas (`atlas ... MB em ... folhas`
   no log). Um normal map do mesmo tamanho dobraria isso, o que não cabe no celular. Achar a
   resolução e o formato que cabem (normal em 1/4 ou 1/8, dois canais, compressão), medindo.
3. **No shader:** usar a normal da folha no lugar da da silhueta (motor 3) quando existir, sem
   perder a borda; manter o lote (não trocar de programa por lutador) e o custo baixo no celular.

**Como conferir:** grave em OpenGL no PC, que é o que o celular mostra:
`xvfb-run -a -s "-screen 0 1920x1080x24" gradle :pc:gravarLuta -Pgl=1 -Proteiro="segura:D espera:1.8 solta:D grava:40:g tecla:L fim"`
(`rm -rf android/pc/build/gravar-luta` antes; os quadros saem lá). Para ver a luz sem o efeito na
frente, ligue `LUZ_DO_IMPACTO` em `GameView` e aumente a duração da luz só no teste. Monte vídeo
antes/depois na velocidade do jogo (cada quadro gravado vale ~45 ms: repita cada um ~2,7 vezes num
vídeo a 60 por segundo) e apague a pasta de quadros depois (o disco já encheu uma vez).

**O que NÃO mandar para a `game-luta-sprite-fable`:** nada que mude a imagem do jogo antes de o
dono aprovar no vídeo (normal maps novos nos assets, shader usando a normal, `LUZ_DO_IMPACTO`
ligado). Pode subir ferramenta e código desligado. Experimentos de memória que pesem no celular,
nunca. Quem julga o resultado é o dono, em vídeo.
