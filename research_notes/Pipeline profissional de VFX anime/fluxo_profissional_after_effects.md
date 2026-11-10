# Fluxo profissional de VFX 2D/anime no After Effects (técnicas, plugins, pacotes, licenças)

Pesquisa feita em 10/11/2026. **Aviso de método:** quase tudo aqui foi visto **só pelos resumos da
busca (WebSearch)**. A leitura direta falhou com ENOTFOUND em `www.actionvfx.com` e
`realtimevfx.com`. Pelo aviso da tarefa, também estão bloqueados schoolofmotion.com,
lesterbanks.com, pixflow.net, premiumbeat.com, videocopilot.net e medium.com, então não tentei
abri-los. Os valores e as cláusulas citados abaixo saíram de trechos de busca e de revendedores, e
precisam ser conferidos na página oficial antes de comprar ou de usar no jogo.

Legenda de portabilidade para o EffectCraft (o clone de AE que o projeto usa):
**[nativo]** = efeito embutido no After Effects, que dá para reproduzir no EffectCraft (pela
`.claude/skills/effectcraft/SKILL.md` o EffectCraft já tem Fractal Noise, Turbulent Noise, Polar
Coordinates, Glow, Colorama, Stencil Luma e a expressão `posterizeTime(n)`).
**[plugin pago]** = depende de plugin de terceiros que não roda no EffectCraft.

---

## 1. Fluxo profissional típico de FX anime/2D no AE e técnicas recorrentes

### Takeaway
Para FX 2D no AE, os profissionais montam formas base (sólidos, máscaras, shape layers) e as
quebram com **ruído (Fractal/Turbulent Noise) mais corte duro** (Threshold, Levels, Stencil Luma,
Simple Choker). Dobram a imagem com **Polar Coordinates** para tirar raios e anéis, colorem com
**Colorama/Tint/Tritone em faixas chapadas**, travam o tempo em **2s/3s (Posterize Time, 8 a 12
quadros por segundo)** e por cima põem **Glow**. Quem quer o visual "desenhado de verdade" desenha os
quadros (Animate, Rough Animator, Photoshop) e usa o AE só para guia e composição. Quase todos os
passos centrais usam só efeitos nativos.

### Cited Findings
**Tutoriais de referência e o que cada um ensina**
- O School of Motion tem um vídeo no YouTube, "How to Create an Anime Explosion in After Effects"
  (cerca de 44 min), com Joey Korenman. Ele monta uma explosão de forte inspiração anime aplicando
  princípios de animação e um visual cel-shaded no AE. Vi só a ficha do Class Central, não o vídeo —
  [Class Central](https://classcentral.com/course/youtube-how-to-create-an-anime-explosion-in-after-effects-134171)
- O artigo do School of Motion "Create a Cartoon Explosion in After Effects" usa Polar Coordinates:
  "desenrola" a imagem, mexe nela e enrola de volta para o visual polar. O instrutor diz que foi
  "exatamente assim" que fez a explosão. Visto só pelo trecho da busca —
  [SoM Polar Coordinates](https://www.schoolofmotion.com/polar-coordinates-is-your-friend);
  [SoM Cartoon Explosion](https://schoolofmotion.com/blog/cartoon-explosion-after-effects)
- O School of Motion publica também "Hand Animated Effects in Adobe Animate", com FX desenhados
  quadro a quadro no Animate e não no AE —
  [SoM](https://www.schoolofmotion.com/blog/hand-animated-effects-in-adobe-animate)
- O artigo "Tricks for Creating a Hand-Drawn Look in After Effects" (School of Motion) usa só
  ferramentas nativas, sem plugins: Roughen Edges, Gaussian Blur e Posterize Time. Visto pelo trecho
  da busca — [SoM](https://www.schoolofmotion.com/blog/tricks-for-creating-a-hand-drawn-look-in-after-effects)
- Ben Marriott distribui de graça no Gumroad o projeto do tutorial "2D Cartoon Smoke + Explosions"
  (vídeo em https://youtu.be/w8mHmW6GgHM) — [Gumroad Ben Marriott](https://benmarriott.gumroad.com/l/aesmoke)
- Ben Marriott também mostra o visual "desenhado à mão" em que **cada quadro é desenhado**: ele monta
  a animação base no AE, usa como guia e redesenha por cima, quadro a quadro. Num tutorial de líquido
  usa Rough Animator (tablet), Photoshop e AE —
  [Lester Banks, só trecho da busca](https://lesterbanks.com/2019/09/working-with-ae-but-still-drawing-every-frame-by-hand/);
  [Gumroad "Hand Drawn Look"](https://benmarriott.gumroad.com/l/uPdeI)
- Jake In Motion tem o vídeo "Fractal Noise + Turbulent Noise | Effects of After Effects" (2022) e um
  script grátis, o "Fractal Noise Generator", que sorteia texturas de ruído. Não achei tutorial dele
  específico de FX anime —
  [ProVideo Coalition](https://www.provideocoalition.com/fractal-noise-in-after-effects/amp/);
  [jakeinmotion.com](https://jakeinmotion.com/fractal-noise-generator)
- Não encontrei página do **ECAbrams** nem do **Sergei Prokhnevskiy** nos resultados. A busca por
  "ECAbrams anime" só trouxe agregadores de títulos de vídeo de baixa qualidade, sem conteúdo
  verificável.

**Técnicas recorrentes (todas [nativo], salvo indicação)**
- **Tempo em 2s/Posterize Time:** posto numa camada de ajuste, faz tudo abaixo andar "em dois",
  inclusive o tremor de borda animado. Outro guia usa cerca de 8 quadros por segundo para o
  movimento picado —
  [Skillshare, Russ Etheridge](https://www.skillshare.com/en/classes/beginners-guide-to-creating-a-hand-drawn-animation-style-in-adobe-after-effects/278126370);
  [Creation Effects](https://www.creationeffects.com/post/how-to-make-a-stop-motion-paper-texture-animation-in-after-effects)
- **Tremor/borda de desenho:** Turbulent Displace é "o efeito de estilização cartoon mais popular" e
  vem com Posterize Time para o movimento andar em degraus. Matt Vojacek combina Turbulent Displace,
  Stroke e Posterize Time para fazer linha de cartoon animada —
  [Motion Design School](https://motiondesign.school/blog/cartoon-effect-stylization-with-turbulent-displace/);
  [Lester Banks, só trecho da busca](https://lesterbanks.com/?p=29198)
- **Eletricidade anime com Turbulent Displace:** existe tutorial próprio, "Easily create anime-style
  electricity using Turbulent Displace". Vi só o título e o trecho —
  [Lester Banks](https://lesterbanks.com/2014/08/easily-create-anime-style-electricity-using-turbulent-displace-effects/)
- **Borda dura e quebrada:** um tutorial do Skillshare diz que, com o efeito no máximo, a borda fica
  "dura e quebrada", e junta isso a um efeito de borda áspera que muda a cada quadro por expressão de
  tempo — [Skillshare](https://www.skillshare.com/en/classes/beginners-guide-to-creating-a-hand-drawn-animation-style-in-adobe-after-effects/278126370)
- **Linha cel:** para linha serrilhada num fórum da Adobe, recomendam o plugin japonês grátis
  **OLM Smoother** e um **Simple Choker negativo** para afinar a borda —
  [Adobe Community](https://community.adobe.com/t5/after-effects-discussions/smoothing-of-hard-lines/m-p/10455447)
- **Fumaça cartoon sem plugin:** o AE já tem Fractal Noise, CC Particle World e Turbulent Displace,
  sem precisar de terceiros. Visto pelo trecho da busca —
  [Pixflow](https://pixflow.net/blog/how-to-create-a-stunning-cartoon-smoke-effect-in-after-effects/)
- **Fogo por rampa de cor:** a base é Fractal Noise (tipo Dynamic, Evolution animada para cintilar) e
  a cor vem do Colorama, com o ciclo de saída em "fire". Um mapa de Fractal Noise mais Displacement Map
  deforma a camada de fogo. Para colorir ruído em laranja/vermelho o fórum da Adobe sugere Tint,
  Tritone ou Colorama —
  [freevisuals](https://www.freevisuals.net/post/adobe-after-effects-how-to-make-a-fireball);
  [AEJuice](https://aejuice.com/en-in/blog/how-to-make-fire-in-after-effects/);
  [Adobe Community](https://community.adobe.com/t5/after-effects/best-effect-to-color-fractal-noise/td-p/11073705)
- Um tutorial de explosão anime sem plugin monta camadas separadas de flare, ruído e círculo e depois
  compõe tudo. Visto só em agregador de baixa confiabilidade —
  [agregador](https://irc.pcper.com/energy-rays-tutorial-anime-explosion-after-effects.html)
- **Impact frame:** quadro único de distorção/inversão no instante do contato; o quadro seguinte
  volta ao normal, e é isso que dá o "estalo". Fonte de baixa confiabilidade (agregador) —
  [agregador](https://baks.verde.ag/impact-frame-after-effects-tutorial.html)
- O animador Minh Nguyen fez no Maya um combo inspirado em Guilty Gear e criou **no After Effects**
  as linhas de velocidade, o rim light e os impact frames. É trabalho de fã, não da Arc System Works —
  [80.lv](https://80.lv/articles/animating-guilty-gear-inspired-fighting-game-combo-in-maya/)
- Um comentário no Reddit chama essas técnicas de noise/displace de "fundamentais" para auras de ki,
  ondas de choque e linhas de ação em anime. É opinião, não tutorial —
  [Reddit, espelho](https://nyc1.lr.ggtyler.dev/r/AfterEffects/comments/1gsjvg2/can_someone_help_with_how_he_did_this_video_i)
- **Find Edges** aparece como jeito de criar contorno preto em volta dos objetos —
  [VFX Apprentice](https://www.vfxapprentice.com/blog/cel-shading-video-games-zelda)

**Brilho na anime japonesa (撮影, satsuei)**
- Na anime japonesa a composição 2D ("撮影") é feita normalmente no After Effects —
  [GIGAZINE, 11/11/2025](https://gigazine.net/news/20251111-troyca-anime-shot-machiasobi29/)
- A OLM distribui de graça plugins de AE feitos para o 撮影. O **OLM KiraKira** gera glow e estrelas a
  partir dos realces, com comprimento e rotação do glow ajustáveis —
  [OLM](https://www.olm.co.jp/post/olm-kirakira);
  [slides OLM](https://speakerdeck.com/olmdrd/matsuri2022-aeplugin)
- Numa reportagem da CGWORLD, efeitos NPR (estilo cel) são gerados no Houdini e compostos no AE. O AE
  foi escolhido por ser a "língua comum" da indústria de anime 2D japonesa —
  [CGWORLD](https://cgworld.jp/feature/202102-houdini-nrp.html)

### Inferences
- A receita que se repete em todas as fontes é esta: **forma base → ruído → corte duro (Threshold,
  Levels ou Stencil Luma) → Polar Coordinates quando é radial → Colorama em faixas → Posterize Time 2s/3s
  → Glow (às vezes empilhado) → composição Add/Screen**. Nada disso exige plugin, e o EffectCraft já
  tem as peças (Fractal Noise, Polar, Colorama, Glow, Stencil Luma, `posterizeTime`). É portável.
- O que separa o profissional do "desenhado por código" nas fontes é: **degrau de tempo** (2s/3s),
  **borda dura com forma que se quebra** (erosão, e não fade de opacidade), **paleta chapada** (sem
  degradê suave) e, no topo da qualidade, **quadros desenhados por cima da guia do AE** (o método de
  Ben Marriott). É o que bate com o critério do dono.
- O tutorial de anime explosion do School of Motion e o de cartoon explosion são os melhores candidatos
  a receita completa, mas não consegui ler os passos exatos (site bloqueado).

### Gaps
- Não consegui os passos e parâmetros exatos do vídeo "Anime Explosion" do School of Motion, nem os
  vídeos do ECAbrams, do Sergei Prokhnevskiy e da Video Copilot sobre anime.
- Não encontrei tutorial em note.com sobre ヒットエフェクト (efeito de golpe) feito no AE. A busca em
  japonês não trouxe nada útil do note.com.
- Não encontrei fonte confiável que descreva **Threshold/Levels** depois do Colorama para cel-shading
  de fogo. É prática comum, mas aqui fica sem fonte.

---

## 2. Plugins de terceiros usados para esse visual: o que fazem, preço, licença e alternativa grátis

### Takeaway
Os pagos mais citados são Deep Glow (brilho), Saber (energia/raio, **grátis**), Optical Flares,
Trapcode (partículas e Shine, hoje **só por assinatura** Maxon), Sapphire (caro, por assinatura) e as
ferramentas Plexus, Newton, Flow e AutoFill. **Nenhum é essencial ao visual anime 2D.** O núcleo do
estilo é feito com efeitos nativos, e para quase todo plugin existe equivalente nativo (Glow
empilhado, Fractal Noise, CC Particle World, Turbulent Displace). O que sai renderizado de um plugin
é imagem sua, e nenhuma fonte encontrada restringe o uso desse render num jogo. Não achei, porém, uma
cláusula explícita confirmando isso em cada EULA.

### Cited Findings
| Plugin | O que faz | Preço/modelo | Fonte |
|---|---|---|---|
| **Video Copilot Saber** | Raios de energia, sabres de luz, lasers, portais, neon, eletricidade, névoa | **Grátis** | [toolfarm](https://www.toolfarm.com/?p=16183); [CaptionX, out/2026](https://caption-x.com/blog/after-effects-plugins); [ProVideo Coalition](https://www.provideocoalition.com/saber-new-free-effects-plug-video-copilot/) |
| **Video Copilot Optical Flares** | Reflexos de lente | US$ 124,95 (perpétua) | [CaptionX, out/2026](https://caption-x.com/blog/after-effects-plugins) |
| **Video Copilot Element 3D** | Modelos 3D dentro do AE | "Pago". O número antigo, US$ 199, pode estar desatualizado | [CaptionX](https://caption-x.com/blog/after-effects-plugins); [PremiumBeat (antigo)](https://www.premiumbeat.com/blog/5-alternatives-to-popular-video-editing-motion-design-plugins) |
| **Deep Glow (Plugin Everything)** | Glow fisicamente mais correto que o nativo | V1 US$ 49,95; o V2 subiu para US$ 99,95 (preços conflitantes nos trechos, com valores de US$ 25/35 que parecem antigos). Há licenças só de render e flutuantes | [aescripts](https://aescripts.com/deep-glow/?aff=50); [fórum aescripts](https://aescripts.com/forums/discussion/1849/deep-glow/p3) |
| **Maxon Red Giant** (Trapcode Particular/Form/Shine/Starglow, Universe) | Partículas 3D, raios de luz, brilho em estrela, pacote de efeitos estilizados | **Só assinatura** (a perpétua do Trapcode Suite acabou). Red Giant Complete ~US$ 1.299/ano (Capterra); Maxon One ~US$ 2.405/ano em revendedor australiano. Trial de 14 dias | [Digital Production, set/2025](https://digitalproduction.com/2025/09/17/maxon-drops-red-giant-2026-0/); [Capterra](https://www.capterra.com/p/210623/Trapcode-Suite/); [Videocraft](https://videocraft.com.au/products/maxon-one-annual-subscription); [Maxon](https://maxon.net/en/red-giant) |
| **Boris FX Sapphire 2026** | Grande biblioteca de efeitos (glows, raios, distorções, transições) | US$ 545/ano ou US$ 80/mês; Boris FX Suite US$ 1.295/ano (revendedor); trial gratuito | [Motion Media](https://www.motionmedia.com/sapphire-avid-annual-subscription/); [Boris FX](https://borisfx.com/news/sapphire-2026-an-ode-to-physical-media/) |
| **Plexus 3** (Rowbyte) | Linhas e pontos 3D generativos | US$ 249,99 | [aescripts](https://aescripts.com/plexus/) |
| **Newton 4** (Motion Boutique) | Física 2D em camadas | US$ 249,99 perpétua; trial com exportação limitada a 25 quadros | [aescripts](https://aescripts.com/newton/) |
| **Flow** | Curvas de easing | US$ 35 | [aescripts](https://aescripts.com/flow/) |
| **AutoFill v2** | Preenchimento que "cresce" pela forma (líquido, contorno) | US$ 99,95 | [aescripts](https://aescripts.com/autofill/?aff=60) |
| **Motion Bro** | Gerenciador de presets/pacotes | Freemium (~1.400 presets grátis e pacotes pagos) | resumo da busca nos resultados de [aescripts](https://aescripts.com/) (não confirmado na página do produto) |
| **Mister Horse Animation Composer** | Gerenciador de presets e pacotes de animação | **Plano grátis** (plugin e mais de 1.000 itens); pago US$ 16,50/mês anual (US$ 198/ano) ou US$ 19,90/mês | [misterhorse.com/pricing](https://misterhorse.com/pricing) |
| **OLM Smoother / OLM KiraKira** | Suavizar linha cel / glow e estrelas a partir de realces | **Grátis** (OLM, estúdio de anime) | [Adobe Community](https://community.adobe.com/t5/after-effects-discussions/smoothing-of-hard-lines/m-p/10455447); [OLM](https://www.olm.co.jp/post/olm-kirakira) |

- O Kamehameha do tutorial do WonderHowTo exige **Trapcode Shine** —
  [WonderHowTo](https://after-effects.wonderhowto.com/how-to/create-kamehameha-energy-blast-attack-from-dragon-ball-after-effects-170428)
- Num fórum do realtimevfx, para levar FX ao jogo citam Particular com um gerador de flipbook e o
  plugin **Sheetah** para montar a folha de sprites. Visto só pelo trecho, o site está bloqueado —
  [realtimevfx](https://realtimevfx.com/t/game-fx-with-after-effect/2327)
- A Video Copilot fez uma promoção de 40% que acabou em 27/04/2026 —
  [Motion Media](https://www.motionmedia.com/mm-blog/video-copilot-sale-40-off-ends-april-27/)
- O Mister Horse diz que o plano grátis pode ser usado em "vídeos sob demanda ilimitados em formato
  fechado, inclusive comercial". **Fala de vídeo**, não de jogo —
  [misterhorse.com/pricing](https://misterhorse.com/pricing)

### Inferences
- Para o projeto, **nenhum plugin pago é necessário** para hit spark, guard spark, rastro, anel de ar,
  fumaça, poeira, fogo, eletricidade ou energia. O Saber (grátis) e o Deep Glow são confortos e têm
  substituto nativo: o Glow nativo empilhado em 2 ou 3 raios diferentes (já anotado no EffectCraft),
  e o raio/eletricidade feito com Fractal Noise mais Turbulent Displace.
- Os plugins de "física" e "workflow" (Newton, Flow, AutoFill, Motion Bro, Animation Composer) só dão
  velocidade de produção em motion design e não mudam o visual anime. Têm pouca relevância aqui.

### Gaps
- Não achei o EULA do Saber, do Deep Glow nem do Trapcode com cláusula explícita sobre usar o
  **render** num jogo comercial (a videocopilot.net está bloqueada). Por prática de mercado o render é
  do usuário, mas sem fonte isso fica como lacuna.
- O preço oficial do Element 3D em 2026 e o preço atual do Deep Glow 2 na página oficial ficaram sem
  confirmação.

---

## 3. Como os profissionais chegam ao visual "desenhado à mão" sem desenhar todo quadro

### Takeaway
Há três caminhos. (a) **Pacotes de elementos desenhados à mão quadro a quadro** (com alfa) usados como
sprites e compostos no AE. (b) **Estilização procedural nativa** (ruído com corte duro, Turbulent
Displace, Roughen Edges, Posterize Time). (c) **Híbrido**: o AE gera a guia de movimento e o artista
redesenha por cima (Ben Marriott), ou o efeito é desenhado no Animate, Harmony, Krita ou Photoshop e
o AE só compõe. Para jogo, a licença dos pacotes é o gargalo.

### Cited Findings
**Pacotes desenhados à mão (2D FX)**
- **Ute FX Pack** (VideoHive): 25 efeitos 2D desenhados à mão, ProRes 4444 com alfa, sem plugins —
  [VideoHive](https://videohive.net/item/ute-fx-pack/31109985)
- **2D FX Elements** (VideoHive 21865706 e 22359605): 59 vídeos com alfa (poeira, fumaça, explosão,
  flash, soco, slash), "animação cel tradicional quadro a quadro" —
  [VideoHive](https://videohive.net/item/2d-fx-elements/21865706);
  [VideoHive](https://videohive.net/item/2d-fx-elements/22359605)
- **Elemental 2D FX Pack** e **Flash FX Elements Pack 01** (VideoHive), desenhados à mão —
  [VideoHive](https://videohive.net/item/elemental-2d-fx-pack/9673890);
  [VideoHive](https://videohive.net/item/flash-fx-elements-pack-01/23211881)
- **Flash FX 3** (AEJuice): mais de 600 elementos desenhados quadro a quadro (fogo, fumaça, swoosh,
  eletricidade), US$ 59 — [AEJuice](https://aejuice.com/product/flash-fx-3/)
- Motion Array: **Powerful Flash FX Pack** (40 flashes cartoon), **Hand Drawn Smoke FX**, **Energy
  Impact Pack** (10 impactos de energia em versões limpa, glow, pixel e rabisco), **Hand Drawn Fire
  Elements**, mais um pacote de "Anime VFX Speed Lines" e "24 Cartoon FX" —
  [Motion Array](https://motionarray.com/stock-motion-graphics/powerful-flash-fx-pack-1373970/);
  [Energy Impact](https://motionarray.com/stock-motion-graphics/energy-impact-pack-839960/);
  [VFX](https://motionarray.com/stock-motion-graphics/vfx/)
- Envato Elements: "Hand Drawn Effects", "2D Cartoon FX Explosion Set" e outros —
  [Envato](https://elements.envato.com/hand-drawn-effects-W7YJEXV);
  [Envato](https://elements.envato.com/2d-cartoon-fx-explosion-set-17-UQ8SK8G)
- Mister Horse "2D Special Effects" e "Hand Drawn Stuff" para o Animation Composer. O "2D Special
  Effects" está **descontinuado como avulso** e agora entra só na assinatura —
  [aescripts](https://aescripts.com/2d-special-effects-for-animation-composer/);
  [aescripts](https://aescripts.com/hand-drawn-stuff/)

**Licença para usar dentro de jogo comercial**
- **Motion Array:** a licença padrão **exclui** "apps, software, jogos e seus usuários finais". Para
  jogos, ou para empresa com mais de 50 funcionários, é preciso uma licença empresarial dedicada
  (vendas) — [Motion Array Help](https://help.motionarray.com/hc/en-us/articles/8995487565981);
  [Motion Array License](https://motionarray.com/license/)
- **Envato (VideoHive / licença Video Media):** proíbe redistribuir o item "como clipe, como stock,
  numa ferramenta ou template, ou com arquivos-fonte", e cada produto final pede uma licença. O FAQ
  da Envato só fala de jogo para **modelos 3D com Extended License**. Não achei regra explícita para
  vídeo/FX dentro de jogo —
  [ThemeForest Video Media](https://themeforest.net/licenses/terms/video_media_single);
  [FAQ](https://themeforest.net/licenses/faq)
- **Envato Elements:** o item incorporado a um produto final durante a assinatura fica licenciado
  para sempre naquele produto —
  [Envato Elements Help](https://help.elements.envato.com/hc/en-us/articles/360000629206-Full-License-Wording)
- **ActionVFX:** num espelho não oficial (domínio Vercel) da licença, o uso em **jogos é permitido**
  desde que os arquivos fiquem embutidos de forma que terceiros não os extraiam e reutilizem com
  facilidade. Há faixas Individual, Startup (receita ou investimento abaixo de US$ 3 mi) e Enterprise
  (US$ 3 mi ou mais), e a ActionVFX já ofereceu elementos prontos para Unreal/Unity. **Não foi
  possível abrir actionvfx.com** (ENOTFOUND). O acervo é sobretudo fogo e fumaça **filmados**, e não
  desenhados — [espelho da licença](https://actionvfx-frontend-eta.vercel.app/license);
  [befores & afters](https://beforesandafters.com/2021/12/10/soon-youll-be-able-to-get-stock-effects-elements-for-game-engines/)
- **ProductionCrate:** não achei o texto oficial sobre jogos. Um agregador diz que a licença proíbe uso
  em produtos concorrentes de stock e exige atribuição, e que o plano grátis tem 5 downloads por mês —
  [licenseorg](https://licenseorg.com/guide/video/productioncrate)

**Híbrido profissional e jogo**
- A VFX Apprentice diz que muitas de suas texturas e flipbooks são **desenhados à mão** em Animate,
  Toon Boom Harmony, Krita ou Photoshop —
  [VFX Apprentice](https://www.vfxapprentice.com/blog/what-are-flipbooks-in-games)
- O blog da Unity descreve o fluxo comum de folhas de textura com ferramentas em série (After Effects
  e Photoshop), que gera muitos dados intermediários —
  [Unity](https://blog.unity.com/technology/experimental-feature-vfx-toolbox-image-sequencer)
- Um tópico do realtimevfx, "Stylized Flipbooks VFX Using After Effects + Unity", descreve FX com
  visual "desenhado à mão" feitos no AE **sem desenhar quadro a quadro**. Só vi o trecho da busca e o
  método ficou cortado. A leitura direta falhou (ENOTFOUND) —
  [realtimevfx](https://realtimevfx.com/t/stylized-flipbooks-vfx-using-after-effects-unity/27915)
- O artigo da VFX Voice "The Return of Hand-Drawn and Stylized Effects Animation" (Ian Failes,
  27/09/2022, **antigo**) registra a volta do FX desenhado e estilizado (The Mitchells vs. The
  Machines, Turning Red, The Bad Guys) e o aumento desse visual em game engines —
  [VFX Voice](https://www.vfxvoice.com/the-return-of-hand-drawn-and-stylized-effects-animation/)
- Existe plugin comercial (PixFlipBook) para ler folhas de sprite direto no AE —
  [itch.io](https://wunkolo.itch.io/pixflipbook)

### Inferences
- Os pacotes "2D FX desenhados à mão" de VideoHive e Motion Array são o caminho mais curto para um
  visual profissional, mas **colocá-los dentro de um jogo é justamente o caso que a Motion Array
  exclui**, e a Envato não deixa claro. Comprar e usar exige licença empresarial ou consulta por
  escrito ao vendedor. Pela regra do projeto (referência é inspiração, não cópia), o uso seguro é
  **estudar o timing e as formas** desses pacotes e refazer.
- O caminho portável e sem risco de licença é o (b) mais o (c): procedural nativo no EffectCraft para
  a base, e os quadros-chave desenhados vindos de imagem ou vídeo gerado como "guia desenhada"
  composta no EffectCraft. É o mesmo princípio do Ben Marriott, com o desenho substituído pela geração.

### Gaps
- O texto integral do tópico do realtimevfx (o método exato) e a licença oficial da ActionVFX, da
  ProductionCrate e de cada item da VideoHive para jogo não puderam ser lidos.
- Faltou o preço individual de cada pacote da VideoHive (os trechos não traziam preço).

---

## 4. Relatos profissionais de VFX de jogo de luta ou gacha/anime feitos no After Effects

### Takeaway
Não achei relato público de VFX artist da Arc System Works, Cygames ou miHoYo descrevendo FX de golpe
feitos no AE. Os sinais concretos são as **vagas da Cygames**, que pedem After Effects para 2D演出 e
2Dエフェクト junto com ParticleSystem (Unity). A Arc System Works trabalha com equipe de efeitos
dentro de um pipeline 3D (Unreal), e o AE não aparece nas fontes.

### Cited Findings
- **Cygames/CySphere:** as vagas de "2Dキャラクター・エフェクト担当" (anunciada em 25/06/2026) cobrem
  2Dエフェクト, cut-ins e direção de skill. As de UIアニメーション・画面演出 citam After Effects,
  Animate, ParticleSystem, uGUI e DOTween. Uma vaga de 正社員 exige experiência em After Effects e em
  ferramentas de efeito tipo ParticleSystem, com salário anual de 4,5 a 12 milhões de ienes. Em Saga
  há vaga de "ゲームエフェクトデザイナー" —
  [gamebiz CySphere](https://gamebiz.jp/enter-enter/job/383);
  [gamebiz](https://gamebiz.jp/enter-enter/job/372);
  [Recruit Direct Scout](https://directscout.recruit.co.jp/job_descriptions/3018313)
- **Arc System Works (Guilty Gear Strive):** a entrevista da Unreal só diz que o visual vem de 2D,
  modelos, animação, **efeitos**, cenários e programação trabalhando juntos, sem citar ferramenta de
  efeito —
  [Unreal Engine](https://www.unrealengine.com/en-US/developer-interviews/how-guilty-gear--strive--hits-an-ultra-combo-with-groundbreaking-visuals-and-gameplay)
- Na colaboração com Cyberpunk: Edgerunners (Lucy), a Arc System Works refez os VFX padrão (de
  estética "mágica") em versões glitch/cyberpunk — [ANN](https://www.animenewsnetwork.com/cms/.226928)
- A palestra GDC 2015 de Junya Motomura, "GuiltyGearXrd's Art Style: The X Factor Between 2D and 3D",
  é sobre o pipeline 2D/3D. O resumo não fala de After Effects (**antiga**) —
  [GDC Vault](https://gdcvault.com/play/1022031/contactUs)
- Um artigo do 4Gamer sobre efeitos de jogo destaca a atenção ao tempo de exibição e ao equilíbrio
  quando vários efeitos aparecem juntos, e o uso do fundo real do jogo durante a criação —
  [4Gamer](https://www.4gamer.net/games/999/G999905/20231201050/)

### Inferences
- No mercado japonês de gacha (Cygames), o AE aparece como ferramenta de **cut-in, 演出 e 2Dエフェクト
  pré-renderizada**, ao lado de partículas em tempo real. Isso confirma que "AE para sequência de
  quadros com alfa e motor para o resto" é prática da indústria, mas não diz quais efeitos ou receitas
  são usados.
- Jogos de luta grandes (Arc System Works) fazem FX dentro do motor 3D, então o AE não é a referência
  certa para eles. Para um jogo 2D com flipbooks, o paralelo mais próximo é o fluxo da anime/gacha.

### Gaps
- Não achei palestra da CEDEC nem artigo da CGWORLD que descreva **receita** de efeito de golpe 2D feito
  no AE por estúdio de jogo (Cygames, miHoYo, Arc System Works). A busca em japonês trouxe só vagas e
  conteúdo genérico. Talvez exista em CEDEC Digital Library ou em blogs técnicos (Cygames Engineers'
  Blog), mas não foi verificado.
- Não achei nada da miHoYo/HoYoverse que cite After Effects para FX.
