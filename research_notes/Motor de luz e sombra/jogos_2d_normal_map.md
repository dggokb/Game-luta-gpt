# Como jogos 2D profissionais iluminam sprites dinamicamente (normal map e equivalentes)

Legenda de leitura das fontes: **[lido]** = página/arquivo lido por inteiro (ou o código-fonte baixado e conferido); **[trecho]** = só snippet de busca ou resumo parcial; **[bloqueado]** = site recusou acesso.

Sites bloqueados nesta pesquisa: unrealengine.com (403 nas entrevistas de Octopath II); docs.unity3d.com versões novas redirecionam para docs.unity.com (contornado); o espelho do Reddit sobre Eastward não carregou; o blog slembcke.net só devolveu o título.

---

## 1. Fórmula de combinação dos motores (Unity URP 2D, Godot 2D) — o que vem pronto e testado

### Takeaway
Os dois motores fazem **luz multiplicada pela cor do sprite** (albedo × soma das luzes), com a "luz ambiente" sendo um termo dentro dessa soma (Global Light 2D na Unity) ou uma multiplicação de base (CanvasModulate no Godot). O termo de normal é só `saturate(dot(N, L))` multiplicando a cor da luz, sem degraus, sem "lado escuro" negativo, e a Unity só deixa passar de 1.0 se você pedir (intensidade > 1 ou HDR Emulation Scale).

### Cited Findings

**Unity URP 2D — shader de composição (código-fonte, [lido])**
- Arquivo `Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/CombinedShapeLightShared.hlsl` (branch master, baixado em 12/11/2026). Cada "Blend Style" vira uma textura de luz em espaço de tela (`_ShapeLightTexture0..3`) e é dividida em parte multiplicativa e aditiva:
  ```hlsl
  half4 shapeLight0 = SAMPLE_TEXTURE2D(_ShapeLightTexture0, sampler_ShapeLightTexture0, lightingUV);
  if (any(_ShapeLightMaskFilter0)) {
      half4 processedMask = (1 - _ShapeLightInvertedFilter0) * mask + _ShapeLightInvertedFilter0 * (1 - mask);
      shapeLight0 *= dot(processedMask, _ShapeLightMaskFilter0);
  }
  half4 shapeLight0Modulate = shapeLight0 * _ShapeLightBlendFactors0.x;
  half4 shapeLight0Additive = shapeLight0 * _ShapeLightBlendFactors0.y;
  ...
  half4 finalModulate = shapeLight0Modulate + shapeLight1Modulate + shapeLight2Modulate + shapeLight3Modulate;
  half4 finalAdditve = shapeLight0Additive + shapeLight1Additive + shapeLight2Additive + shapeLight3Additive;
  finalOutput = _HDREmulationScale * (color * finalModulate + finalAdditve);
  finalOutput.a = alpha;
  return max(0, finalOutput);
  ```
  Ou seja: **final = HDRScale × (albedo × ΣluzesMultiply + ΣluzesAdditive)**; não há clamp superior no shader (só `max(0, …)`), o clamp vem do formato do alvo de render. — [CombinedShapeLightShared.hlsl](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/CombinedShapeLightShared.hlsl) [lido]
- Fatores de cada modo (código C#): `Additive: x=0, y=1`; `Multiply: x=1, y=0`; `Subtractive: x=0, y=-1`. — [Light2DBlendStyle.cs](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Runtime/2D/Lights/Light2DBlendStyle.cs) [lido, trecho do switch]
- Doc antiga confirma: "All of the Blend Modes available can be expressed with Custom Blend Factors": Multiplicative "Modulate = 1, Additive = 0"; Additive "Modulate = 0, Additive = 1"; Subtractive "Modulate = 0, Additive = -1". Máscara: "In a mask max value means full light, min value means no light." "Lowering the Render Texture Scale can increase performance and decrease memory usage for Scenes that contain large Lights." "Lowering the Texture Scale to too low a value may cause visual artefacts or a shimmering effect when there is motion in the Scene." — [URP 7.1 Light Blend Styles](http://docs.unity3d.com/Packages/com.unity.render-pipelines.universal@7.1/manual/LightBlendStyles.html) [lido]
- Doc atual (Unity 6.6) das propriedades da Light 2D:
  - Blend Style: "Multiply: Multiplies the light with the background, which results in a darker color." / "Additive: Adds the light to the background, which results in a brighter color." / "Multiply with Mask (R): Multiplies the light with the background, but only where the red value of the mask map of the sprite is greater than 0." / "Additive with Mask (R): Adds the light to the background, but only in areas where the red value of the mask map of the sprite is greater than 0."
  - Intensity: "The default value is 1. If you set the value above 1, lights with a Blend Style of Multiply or Multiply with Mask (R) will brighten sprites beyond their original color."
  - Overlap Operation: "Additive: Adds the colors together so the lights blend. This is the default." / "Alpha Blend: Uses the alpha value of the Color property to control the blending amount. If you set the alpha value to 0, Unity doesn't blend the lights, so one completely overlaps the other."
  - Normal Map Quality: "Accurate: … with high accuracy. This option can reduce performance compared to Fast." / "Fast: … lower accuracy than Accurate, which results in faster performance but can produce artefacts." "Smaller lights and larger distance values will reduce the difference between Fast and Accurate."
  - Normal Map Distance: "Sets the distance between the light and the lit sprite, without moving the light or changing its intensity."
  - Volumetric Intensity: "Sets the strength of the light in empty space. The value ranges from 0 to 1…"
  - Shadows Strength: "0 means no light is blocked and 1 means all light is blocked."
  — [Unity 6.6 2D Light properties](https://docs.unity.com/en-us/engine/6000.6/manual/unity2d/2d-urp/2d-index/2dlight-properties) [lido]
- Overlap no código: Additive → `Blend One One`; Alpha Blend → `Blend SrcAlpha OneMinusSrcAlpha` na textura de luz; volumétrico Point → `One One`, outros → `SrcAlpha One`. — [RendererLighting.cs](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Runtime/2D/Passes/Utility/RendererLighting.cs) [lido, linhas 370–390]
- Termo de normal map (por luz, ao desenhar a luz na textura de luz) — `LightingUtility.hlsl`, modo Accurate:
  ```hlsl
  half4 normal = SAMPLE_TEXTURE2D(_NormalMap, sampler_NormalMap, input.screenUV);
  half3 normalUnpacked = UnpackNormalRGBNoScale(normal);
  half3 planeNormal = -GetViewForwardDir();
  half3 projLightPos = lightPosition.xyz - (dot(lightPosition.xyz - input.positionWS.xyz, planeNormal) - lightZDistance) * planeNormal;
  half3 dirToLight = normalize(projLightPos - input.positionWS.xyz);
  lightColor = lightColor * saturate(dot(dirToLight, normalUnpacked));
  ```
  (Fast faz o mesmo mas com a direção interpolada por vértice.) A "Normal Map Distance" é o `lightZDistance` = altura virtual da luz sobre o plano do sprite. Os normais dos sprites são desenhados num **buffer de normais em espaço de tela** cujo clear é `k_NormalClearColor = new Color(0.5f, 0.5f, 0.5f, 1.0f)`. — [LightingUtility.hlsl](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Shaders/2D/Include/LightingUtility.hlsl) [lido]; [RendererLighting.cs](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Runtime/2D/Passes/Utility/RendererLighting.cs) [lido]
- Atenuação da Point Light 2D (`Light2D.shader`):
  ```hlsl
  half attenuation = saturate(_L2D_INNER_RADIUS_MULT * lookupValue.r);   // inner radius
  half spotAttenuation = saturate((_L2D_OUTER_ANGLE - lookupValue.g + isFullSpotlight) * (1.0f / (_L2D_OUTER_ANGLE - _L2D_INNER_ANGLE)));
  attenuation = attenuation * spotAttenuation;
  mappedUV.x = attenuation; mappedUV.y = _L2D_FALLOFF_INTENSITY;
  attenuation = SAMPLE_TEXTURE2D(_FalloffLookup, sampler_FalloffLookup, mappedUV).r;
  ...
  lightColor *= attenuation;
  APPLY_NORMALS_LIGHTING(i, lightColor, _L2D_POSITION.xyz, _L2D_POSITION.w);
  APPLY_SHADOWS(i, lightColor, _L2D_SHADOW_INTENSITY);
  return ToFragmentOutput(lightColor * _InverseHDREmulationScale);
  ```
  A curva da tabela `_FalloffLookup` (2048×128): `baseValue = (y+32)/(HEIGHT+64); exponent = log(1-baseValue)/log(baseValue); red = pow(t, exponent)` — ou seja a "Falloff Strength" escolhe um expoente de uma curva potência sobre a distância normalizada (1 no centro/inner radius → 0 no raio externo). Não é 1/d²: é **raio finito, chega a zero exatamente no raio**. — [Light2D.shader](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Shaders/2D/Light2D.shader) [lido]; [Light2DLookupTexture.cs](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Runtime/2D/Passes/Utility/Light2DLookupTexture.cs) [lido]
- HDR Emulation: a luz é escrita na textura dividida por `_InverseHDREmulationScale` e o composite multiplica por `_HDREmulationScale` — truque para guardar valores > 1 numa textura de 8 bits. Default `m_HDREmulationScale = 1`; `m_LightRenderTextureScale = 0.5f` (texturas de luz a **meia resolução** por padrão); `m_MaxLightRenderTextureCount = 16`. — [Renderer2DData.cs](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Runtime/2D/Renderer2DData.cs) [lido, grep]

**Godot 4 — shader de canvas (código-fonte, [lido])**
- Ordem no fragment (`servers/rendering/renderer_rd/shaders/canvas.glsl`): `vec4 base_color = color;` → `color *= canvas_data.canvas_modulation;` (CanvasModulate = ambiente multiplicado) → para cada DirectionalLight2D e cada luz posicional: calcula `light_color` e chama `light_blend_compute`. — [canvas.glsl](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/shaders/canvas.glsl) [lido]
- Difuso com normal:
  ```glsl
  vec3 light_normal_compute(vec3 light_vec, vec3 normal, vec3 base_color, vec3 light_color, vec4 specular_shininess, bool specular_shininess_used) {
      float cNdotL = max(0.0, dot(normal, light_vec));
      ... // Blinn opcional, view = vec3(0,0,1) "not great but good enough"
      return light_color * base_color * cNdotL;
  }
  ```
  Sem normal map: `light_color.rgb *= base_color.rgb;` (luz plana, só distância/textura da luz). Direcional: `vec3 light_vec = normalize(mix(vec3(direction, 0.0), vec3(0, 0, 1), height));` — height 0 = luz rasante, 1 = de frente. Posicional: `light_vec = normalize(vec3(position, height) - light_vertex)`. — [canvas.glsl](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/shaders/canvas.glsl) [lido]
- Blend:
  ```glsl
  case LIGHT_FLAGS_BLEND_MODE_ADD: color.rgb += light_color.rgb * light_color.a;
  case LIGHT_FLAGS_BLEND_MODE_SUB: color.rgb -= light_color.rgb * light_color.a;
  case LIGHT_FLAGS_BLEND_MODE_MIX: color.rgb = mix(color.rgb, light_color.rgb, light_color.a);
  ```
  Logo, em Add: **final = albedo×CanvasModulate + Σ (corLuz × energia × albedo × max(0,N·L))**. Como cada luz já vem multiplicada pelo albedo, uma luz branca energia 1 sobre ambiente 1 dobra a cor (estoura). — [canvas.glsl](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/shaders/canvas.glsl) [lido]
- Normal map decodificado com profundidade ajustável: `normal = mix(vec3(0,0,1), normal_map * vec3(2,-2,1) - vec3(1,-1,0), normal_map_depth);` — `normal_map_depth` é o "achatar/exagerar" oficial. — [canvas.glsl](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/shaders/canvas.glsl) [lido]
- Limite: `#define MAX_LIGHTS_PER_ITEM 16`; `MAX_LIGHTS_PER_RENDER = 256`. — [canvas_uniforms_inc.glsl](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/shaders/canvas_uniforms_inc.glsl), [renderer_canvas_render_rd.h](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/renderer_canvas_render_rd.h) [lido, grep]
- Doc: "CanvasModulate is used to darken the scene by specifying a color that will act as the base 'ambient' color." "The default Add is suited for most use cases." "Subtract can be used for negative lights, which are not physically accurate but can be used for special effects." PointLight2D height: "The light's virtual height with regards to normal mapping. By default, the light is very close to surfaces receiving lights." Desempenho: "Larger lights have a higher performance cost as they affect more pixels on screen"; "Additive sprites are much faster to render, since they don't need to go through a separate rendering pipeline." — [Godot docs: 2D lights and shadows](https://docs.godotengine.org/en/stable/tutorials/2d/2d_lights_and_shadows.html) [lido]

**Sprite sem normal map numa cena iluminada**
- Godot: sem normal, `light_color.rgb *= base_color.rgb` — recebe a luz inteira, só modulada pela atenuação da textura da luz (sem N·L). — [canvas.glsl](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/shaders/canvas.glsl) [lido]
- Unity: o buffer de normais é limpo com (0.5, 0.5, 0.5) — onde nada foi escrito o normal decodificado é ~0 e o shader com normal map ativo dá N·L ≈ 0; por isso a Unity exige normal map na luz ou "Normal Map Quality: Disabled" para luz plana. — [RendererLighting.cs](https://github.com/Unity-Technologies/Graphics/blob/master/Packages/com.unity.render-pipelines.universal/Runtime/2D/Passes/Utility/RendererLighting.cs) [lido]

### Inferences
- O padrão consolidado é **multiplicar**: `albedo × (ambiente + Σ luz_i × atenuação_i × max(0, N·L_i))`. Com ambiente + luzes ≤ 1, o sprite nunca passa da cor pintada; o brilho acima da arte pintada só aparece se alguém pedir intensidade > 1 (a doc da Unity avisa exatamente isso).
- Nenhum dos dois motores tem um termo "volume" que **escurece** o lado de trás abaixo do ambiente: o lado oposto à luz recebe `max(0, N·L) = 0` daquela luz, mas continua com o ambiente. O nosso termo atual que soma no lado aceso e subtrai no lado oposto é mais forte que o padrão de mercado (equivale a uma luz Add + uma luz Sub).
- A Unity limita naturalmente porque a textura de luz é 8 bits (0..1) por padrão e `HDREmulationScale = 1`.

### Gaps
- Não confirmei no código atual onde a Global Light 2D é escrita (antes era o clear color da textura de luz); a doc moderna só diz que ela ilumina tudo no sorting layer.
- Tonemapping no 2D Renderer: não achei doc específica nesta rodada; o URP aplica tonemapping no pós (Volume), não no composite 2D.

---

## 2. Octopath Traveler e HD-2D (Acquire / Square Enix, Unreal Engine)

### Takeaway
No HD-2D o sprite de pixel art é um material que **recebe luz do mundo 3D**, mas com uma parcela emissiva fixa misturada ("emissive : lit" com valor comum, ajustado por mapa) justamente para não ficar escuro demais nem perder a leitura; as sombras são a própria ilustração deitada (ou blob decal em interiores); o pós-processamento (DoF forte, vinheta) é o que dá o "clima", e o Live A Live reduziu DoF/vinheta por legibilidade.

### Cited Findings
- Live A Live (UNREAL FEST WEST '22, matéria da GameMakers):
  - 「ドット絵がライティングを受けるのはHD-2Dの特徴の一つではありますが、洞窟など一部の暗がりのマップでは、キャラクターの色味も暗くなりすぎてしまうことがありました。」 (receber luz é marca do HD-2D, mas em mapas escuros como cavernas a cor dos personagens ficava escura demais).
  - 「ドット絵のエミッシブ要素と、ライティングを受ける要素の割合には共通の値が設定されている。」 (há um valor comum para a proporção entre a parte emissiva da pixel art e a parte que recebe iluminação).
  - 「共通設定の値が暗い場合は、明るさを調整する」 (quando o valor comum fica escuro, ajusta-se o brilho por mapa).
  - 「原作で攻撃前に画面全体に色を乗せる演出を、PointLightを使用して再現したもの」 (o tingimento de tela antes de ataques do original foi refeito com PointLight).
  - Sombras: 「屋外では、DirectionalLightを用いてドット絵の影を表現しています。」 (fora: DirectionalLight projeta a sombra da pixel art); 「屋内では丸影をデカールで表示する」 (dentro: sombra redonda via decal); 「マップごとに影の濃さや薄さを調整できるように実装された」 (força da sombra ajustável por mapa); batalha (sobre Octopath): 「影の実装は、アクター内部に寝かせた状態の同一のドットを貼り付けるシンプルなもの」 (a sombra é o mesmo sprite deitado dentro do ator).
  - Pós: Octopath 「『情緒』を強く打ち出す表現」; Live A Live 「DoFやビネットは弱めています」, 「ビネットをほとんど入れず、色収差といった『今っぽい表現』は取り入れた」, 「ヒット時に色収差を強くかける」; DoF não servia porque 「奥がぼやけてしまいがち」.
  - Desempenho: switched to 「モバイルフォワードレンダリング」; 「スクリーンパーセンテージを7％落とすと、5msほど負荷軽減」 mas a qualidade caiu tanto que mantiveram 100%; desligar FXAA degradou o fundo.
  — [GameMakers: Live A Live HD-2D (UNREAL FEST WEST '22)](https://gamemakers.jp/article/2023_02_21_30973/) [lido via resumo da ferramenta; citações em japonês verbatim]
- Octopath Traveler, slides "Nintendo Switch『OCTOPATH TRAVELER』はこうして作られた" (UNREAL FEST EAST 2018): 「ドットが見えるような調整」「暗いところもテクスチャが見えるライティング」「被写界深度深め（近くも遠くもくっきりしたドット）」; batalha: 「ブーストすると、キャラクター足元のライトが発光」「ライト演出が入る際は、マップに配置されている太陽光源が オフになる→負荷対策でもある」「動的な影の変化を表現するため、バトル専用ポイントライト をレベルに配置」. — [Docswell slides](https://www.docswell.com/s/EpicGamesJapan/KXPWY5-UE4_UFE2018_ACQUIRE_OCTOPATHTRAVELER) [trecho]; vídeo: [YouTube](https://www.youtube.com/watch?v=UPzeHX3VaHY) [não assistido]
- Interior/exterior: 「屋外のライトがオフになり，室内用のライトがオンになる。」 e a escala interior é metade da exterior, com câmera e tamanho do personagem trocados juntos. — [i-mezzo, relato do talk](https://www.i-mezzo.net/log/2018/10/24085339.html/49961/) [lido]; [AUTOMATON relato](https://automaton-media.com/devlog/report/20181023-78326/) [não lido]
- Entrevistas Unreal sobre Octopath/Octopath II: [bloqueado, 403] — [unrealengine.com](https://www.unrealengine.com/en-US/developer-interviews/octopath-traveler-ii-builds-a-bigger-bolder-world-in-its-stunning-hd-2d-style)

### Inferences
- A receita HD-2D para não estourar nem apagar é **mistura com piso emissivo**: `cor = albedo × (k_emissivo + (1−k_emissivo) × luzDoMundo)` (a forma exata não foi publicada; o que foi dito é "proporção comum entre emissivo e lit, ajustada por mapa"). Isso é diretamente aplicável ao nosso caso: a arte pintada nunca cai abaixo de k e nunca sobe muito acima de 1.
- Na batalha eles **desligam o sol** e usam luzes pontuais dedicadas — análogo ao nosso "flash de impacto" substituindo a luz do cenário.

### Gaps
- Não achei o valor numérico do "共通の値" nem se o sprite usa normal map (os relatos não falam de normal map em personagem; o CGWORLD gg2019 não foi lido). Triangle Strategy e Dragon Quest III HD-2D: sem fonte técnica encontrada nesta rodada.

---

## 3. Dead Cells, The Siege and the Sandfox, Graveyard Keeper, Broken Age — o que os devs disseram sobre mapas, detalhe do normal e falhas

### Takeaway
Quem usa normal map em sprite pintado converge em: **normal só com as formas grandes** (detalhe vira "noisy mess" e "shiny metallic look"), luz Lambert simples (`saturate(N·L)`), sem sombra dinâmica, e a pintura difusa feita primeiro com o normal derivado dela; Dead Cells sai de 3D com toon shader; Broken Age usa ambiente em gradiente como base e luz local separada.

### Cited Findings
- **Dead Cells** (Thomas Vasseur): "We export each frame of the animation we made with the 3D skeleton to a .png, along with its normal map"; o volume é renderizado "using a basic toon shader"; "We solved that using cell shading on our models rendered in low resolution"; o programa caseiro "renders the mesh in a very small size and without antialiasing"; "the ingame height of the character will only be 50 pixels"; "30Fps for the animations"; em ScarKrow "I had to draw all the shadows by hand on all the frames", evitado em Dead Cells "designing a lightning system with that in mind." O artigo **não** detalha a fórmula da luz no motor. — [Game Developer: Art Design Deep Dive, Dead Cells](https://www.gamedeveloper.com/production/art-design-deep-dive-using-a-3d-pipeline-for-2d-animation-in-i-dead-cells-i-) [lido]
- **The Siege and the Sandfox** (Keith Duke-Cox, art director, Cardboard Sword, 2018): normais usados "to 'sculpt' the large obvious shapes of the pixel art instead"; primeira tentativa "led to a very detailed normal" e "the end result was just a noisy mess"; implementações anteriores davam "too much of a shiny metallic look"; fluxo "do the diffuse art first and then do the normals afterwards using the diffuse as a reference"; pintam a partir de uma paleta "normal map dome" e depois "We use Nvidia's TextureTools for Photoshop to correct/normalize vectors"; pintado à mão venceu conversão de tons de cinza/reconhecimento de forma; "Lights are dynamically lit only, they don't dynamically shadow at all in our game"; sem normal a arte tinha um look de "sheet of paper". — [Game Developer: hand-drawn normal maps in Sandfox](https://gamedeveloper.com/art/adding-depth-to-2d-with-hand-drawn-normal-maps-in-i-the-siege-and-the-sandfox-i-) [lido]
- Contradição: em posts no fórum da Unreal, um dev da Cardboard Sword escreveu que testaram e **não iam seguir** com normal map porque "made the pixel art noisy and less pixelart-esque", "the stronger the effect of the normal mapped lighting on the pixel art was, the more the 'purity' of the pixel art was lost", e que normal funciona melhor em "clean bold shapes with obvious angle changes"; outra resposta diz que os sprites são "standard, non emissive materials that accept the standard lighting of the engine". — [Unreal forums: Sandfox thread](https://forums.unrealengine.com/t/the-siege-and-the-sandfox-a-stealthvania-set-in-an-ancient-desert-kingdom/59803) [trecho]; não está claro qual estado é o do jogo lançado.
- **Graveyard Keeper** (Lazy Bear): "every sprite has its own normal map. This helps to feel the volume."; "These normals are pretty easy to draw. An artist roughly paints light on 4 sides with a brush" (script junta os 4 lados no normal); "We use regular light sources, the default Unity ones."; "There is a shader that calculates the distance between a light source and a sprite on the vertical axis"; luz ambiente "Darker at night, lighter in the daytime. The light's color set with a gradient." "By nightfall a light source not only becomes darker but gets blue tint."; 10 LUTs de correção de cor por hora do dia; sombras "made with sprites rotating around a point", rotação "inside a vertex shader", máximo 4 sombras por objeto (1 do sol + até 3 de luzes dinâmicas), ~10 sprites de sombra reaproveitáveis. — [Game Developer: Graveyard Keeper graphics effects](https://www.gamedeveloper.com/programming/graveyard-keeper-how-the-graphics-effects-are-made) [lido]
- **Broken Age / MISE / Lucidity** (Oliver Franzke, Double Fine, 2014): ambiente é "the backbone" e vale para todo personagem em toda cena; tinge o sprite com **gradiente** (cima/baixo) para "emulate ambient occlusion between the floor and the character"; em Broken Age os "normals as the vectors from the translated center of the bounding box to each vertex" amostram o gradiente pelo y; ambientes são funções de base radial com média ponderada por distância e uma luz global quando não há fonte por perto. Luz local: Broken Age usa "hand authored normal maps that are then used to compute the reflected light"; MISE gera normais **sem asset extra**: máscara alfa renderizada fora da tela, reduzida e com blur gaussiano → "a soft height field containing a blobby approximation of the characters", normal = gradiente do campo de altura; pseudocódigo:
  ```
  vec l = light_pos - fragment_world_pos
  scalar nDotL = saturate(dot(normalize(l), n))
  vec result = light_color * nDotL / (dist * dist)
  ```
  MISE somava o mapa de luz no framebuffer (aditivo), e o problema citado é que "reflected light is barely visible on bright pixels"; alternativa é amostrar o mapa de luz no shader do personagem (light pre-pass). "it is generally not possible to paint the influence of light sources directly into the sprites". — [Game Developer: Dynamic 2D Character Lighting (Franzke)](https://www.gamedeveloper.com/programming/dynamic-2d-character-lighting) [lido]
- **Unforeseen Incidents** (inspirado em Broken Age): luzes 3D reais spot/point no personagem ("using real lights make the best impression of lighting on the characters", mais draw calls); alternativa barata "completely tints the character" interpolando entre fontes de cor; luz de fundo volumétrica refratada na silhueta mantida "very subtle" porque é "perspectively wrong"; normal map só para a luz do farol do carro bater de lado. — [Game Developer: Dynamic character lighting in Unforeseen Incidents](https://gamedeveloper.com/programming/dynamic-character-lighting-in-unforeseen-incidents) [lido]
- **Sprite Lamp** (Snake Hill Games): gera "normal maps, depth maps, anisotropy maps, and ambient occlusion maps" a partir de 2–5 "lighting profiles" "painted in greyscale (as though the subject was made of white plaster)" com "a single light source coming from a single direction"; perfis "require shading but not shadowing"; o difuso ideal é "a picture of the character with theoretical 'perfect' lighting from every angle"; "standard normal mapped shader" dá "reasonably good results"; o autossombreamento por depth map pode pesar demais em mobile. — [Sprite Lamp](http://www.snakehillgames.com/spritelamp/) [lido]
- Fórum Defold (Pawel): normal em resolução cheia "looks smooth, but it does not ressemble any hand-drawn pixel art"; "To reduce the smoothness you can use a reduced color pallete for normal map"; fórmula usada: `gl_FragColor = (AMBIENT + color_diffuse) * color_albedo;` com `float diff = max(dot(color_normal.xyz, light_dir.xyz), 0);`; falha: "When sprite is flipped to go left, the shading is still static" (precisa inverter o X do normal ao espelhar). — [Defold forum: normal map lighting for pixel art](https://forum.defold.com/t/normal-map-lighting-for-2d-pixel-art-sprites/70967) [lido]

### Inferences
- O "manchou/virou mancha" do dono é o mesmo sintoma descrito como "noisy mess"/"shiny metallic": normal exagerado (nosso 2.3×) + luz forte. A correção que os devs aplicaram foi **menos detalhe/menos inclinação**, não mais. O equivalente oficial no Godot é `normal_map_depth` < 1 (mistura com (0,0,1)).
- O modelo Franzke encaixa no nosso caso: gradiente ambiente vertical (piso/teto do cenário) como base multiplicativa + luz local Lambert com raio, tudo multiplicando o albedo pintado.
- Espelhar sprite exige inverter normal.x — conferir no nosso shader.

### Gaps
- Dead Cells: não achei a fórmula/número de tons do toon shader no motor Heaps nem quantas luzes por personagem. Children of Morta: só há fala (via TechRaptor, [trecho]) de que estenderam a pixel art para HD para usar luzes dinâmicas em alta resolução — [TechRaptor](https://techraptor.net/content/how-dead-mage-created-the-art-for-children-of-morta) [trecho]; nada confirma normal map. 80.lv sobre Children of Morta não fala de luz — [80.lv](https://80.lv/articles/children-of-morta-beautiful-pixelart-rpg) [lido].

---

## 4. Sea of Stars, Eastward, Songs of Conquest e demais títulos

### Takeaway
Pouca documentação técnica pública: Sea of Stars confirma que a luz dinâmica ditou todo o pipeline de assets mas não publica o "como"; Songs of Conquest ilumina o cenário com um **lightmap em tempo real projetado de cima** e usa shaders "de forma sutil"; Eastward não tem fonte primária encontrada.

### Cited Findings
- Sea of Stars (Thierry Boulanger): "one person, Jeakim Hamel, worked on the dynamic lighting since the very beginning of Sea of Stars' development in 2018"; "the pipeline, all of the production, all of the assets that we make have to answer to the technical needs of the dynamic lighting working out"; "a full-time job for him for five years straight". Nada sobre normais/altura. — [MobileSyrup interview](https://mobilesyrup.com/2023/09/11/sea-of-stars-level-design-sabotage-studio-thierry-boulanger-interview/) [lido]
- Songs of Conquest (Lavapotion; Patrik, technical artist; Anders, art director): "we opted for a real-time lightmap applied by our shaders using a top-down projection", lightmap feito de sprites de luz renderizados por uma câmera separada, suporta luz e escuridão com cor; dia/noite principalmente por color grading + luz e ambiente; pós: bloom, vinheta, color grading, DoF nos zooms; shaders modernos usados "in a fairly subtle way along with pixel art". O artigo não diz como as unidades (sprites) são iluminadas. — [80.lv: mixing 2D billboards and 3D environments](https://80.lv/articles/mixing-2d-billboards-and-3d-environments-in-a-game/) [lido]
- Eastward: só um post de Reddit (via espelho, não carregou) diz que a técnica de deferred lighting em pixel art veio de um post do Tumblr da Pixpil; comentaristas no itch.io duvidam que haja normal map. — busca em [itch.io post](https://itch.io/post/9039152) [trecho]; espelho Reddit [bloqueado]
- Brigador, Legend of Dungeon, CrossCode, Blasphemous 2, Darkest Dungeon II, Triangle Strategy, DQ3 HD-2D: nenhuma fonte técnica encontrada nas buscas desta rodada.

### Inferences
- Lightmap em textura de tela (Songs of Conquest, Unity URP 2D a 0.5× de resolução) é o padrão quando há muitas luzes; para poucas luzes num sprite (nosso caso: 1 direcional + 1 pontual), uniforms num único passe forward bastam.

### Gaps
- Sea of Stars: sem dados de mapa por sprite (normal/altura). Eastward: sem fonte primária da Pixpil. Não verificado se há talk da GDC sobre Sea of Stars.

---

## 5. Falhas conhecidas e correções (resumo cruzado) + sombras + custo mobile

### Takeaway
Os relatos e os motores apontam as mesmas regras: luz **multiplica** a arte pintada; ambiente + luz difusa ≤ 1 salvo em flashes; normal só de forma grande e com profundidade reduzida; sem degraus duros sobre normal estimado; sombra de sprite é blob/decal ou o próprio sprite achatado; luzes com raio finito.

### Cited Findings
- Detalhe demais → "noisy mess", "shiny metallic look" — [Sandfox, Game Developer](https://gamedeveloper.com/art/adding-depth-to-2d-with-hand-drawn-normal-maps-in-i-the-siege-and-the-sandfox-i-) [lido]; efeito forte → perda de "purity" — [Unreal forums](https://forums.unrealengine.com/t/the-siege-and-the-sandfox-a-stealthvania-set-in-an-ancient-desert-kingdom/59803) [trecho].
- Luz > 1 em Multiply clareia acima da cor original — [Unity 6.6 Light properties](https://docs.unity.com/en-us/engine/6000.6/manual/unity2d/2d-urp/2d-index/2dlight-properties) [lido].
- Aditivo puro sobre pixels claros quase não aparece ("barely visible on bright pixels") — [Franzke](https://www.gamedeveloper.com/programming/dynamic-2d-character-lighting) [lido]; ou, inversamente, estoura pixels médios (Godot Add soma `luz × albedo` sobre `albedo × ambiente`) — [canvas.glsl](https://github.com/godotengine/godot/blob/master/servers/rendering/renderer_rd/shaders/canvas.glsl) [lido].
- Escuro demais em mapas escuros → piso emissivo comum ajustado por mapa — [GameMakers Live A Live](https://gamemakers.jp/article/2023_02_21_30973/) [lido].
- Normal suave demais → reduzir paleta do normal; espelhar → shading fica errado se não inverter — [Defold forum](https://forum.defold.com/t/normal-map-lighting-for-2d-pixel-art-sprites/70967) [lido].
- Textura de luz em resolução baixa demais → "shimmering effect when there is motion" — [URP 7.1 docs](http://docs.unity3d.com/Packages/com.unity.render-pipelines.universal@7.1/manual/LightBlendStyles.html) [lido].
- Sombras: HD-2D = sprite deitado (batalha), DirectionalLight real (exterior), blob decal (interior), força por mapa — [GameMakers](https://gamemakers.jp/article/2023_02_21_30973/) [lido]; Graveyard Keeper = sprites de sombra girados no vertex shader, até 4 por objeto — [Game Developer](https://www.gamedeveloper.com/programming/graveyard-keeper-how-the-graphics-effects-are-made) [lido]; Sandfox = sem sombra dinâmica — [Game Developer](https://gamedeveloper.com/art/adding-depth-to-2d-with-hand-drawn-normal-maps-in-i-the-siege-and-the-sandfox-i-) [lido]; Unity Shadow Caster 2D com "Strength" 0..1 — [Unity docs](https://docs.unity.com/en-us/engine/6000.6/manual/unity2d/2d-urp/2d-index/2dlight-properties) [lido].
- Custo: Godot até 16 luzes por item num passe; Unity acumula luzes em até 4 texturas de luz (uma por blend style) a 0.5× de resolução e amostra no shader do sprite (deferred-ish); Live A Live trocou para mobile forward rendering por desempenho — fontes acima.

### Inferences
- Para o nosso shader (GLES3, 2 lutadores, 1 direcional + 1 pontual), uma fórmula alinhada ao mercado seria: `luz = ambienteCenário(gradiente vertical opcional) + kDir × max(0, N·Ldir) + kPt × atenuação(raio finito) × max(0, N·Lpt)`; `cor = albedo × min(luz, ~1.0–1.1)`; normal com profundidade 0.3–0.6 (em vez de 2.3×) via `mix((0,0,1), N, depth)`; flash de impacto como única exceção acima de 1; sem termo negativo que escureça abaixo do ambiente. Os números (0.3–0.6, 1.1) são **sugestão minha**, não vieram de fonte.
- Banding/mancha de toon: nenhum dos jogos documentados aplica degraus duros sobre normal estimado; Dead Cells aplica toon sobre normal de modelo 3D limpo (não estimado). Se quiser degrau, suavizar o normal (blur) antes e usar `smoothstep` largo — inferência.

### Gaps
- Nenhuma fonte encontrada com números de "intensidade ≤ 1 / ambiente + luz ≤ 1" escritos por um dev de jogo lançado; a regra decorre da fórmula multiplicativa e do aviso da Unity.
- Cintilação de normal estimado quadro a quadro: nenhum relato de jogo lançado encontrado (todos usam normal pintado à mão ou vindo de 3D).
- Orçamento oficial de luzes por pixel em GLES3 para sprite: não encontrado.
