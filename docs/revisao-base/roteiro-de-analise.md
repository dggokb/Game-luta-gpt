# Roteiro de análise (igual para todos os jogos)

Objetivo: referência técnica e visual do BÁSICO de um jogo de luta "raiz" (nível Street Fighter 2) para um jogo 2D em sprites (lutadores desenhados, cenário pintado em camadas, celular e PC). Ignore mecânicas próprias (parry, drive, assists, tag, etc.), salvo quando o efeito visual delas ensina algo do básico.

Regras:
- Marque cada observação com [Vnn qNNNN] (vídeo e quadro onde viu). O que vier de conhecimento prévio, e não do vídeo, marque [CONHECIMENTO].
- Seja extremamente concreto: tamanho (em % da altura/largura da área de jogo), cor, forma, duração, posição. Nada de "bonito".
- Durações: as gravações são de tela do celular a ~24 quadros por segundo (o vídeo original do YouTube é 30 ou 60). Conte em quadros da gravação e converta: 1 quadro da gravação ≈ 41,7 ms ≈ 2,5 quadros do jogo a 60 Hz. Diga "≈" e a margem de erro.
- Medidas: a área de jogo dentro da gravação tem barras pretas nos lados e às vezes a interface do YouTube por cima; meça em % da ÁREA DE JOGO, não da gravação inteira.
- Use contact sheets (folhas de quadros seguidos) com Python/PIL para ver sequências; para detalhe, extraia o quadro em resolução cheia do vídeo original com ffmpeg (ex.: ffmpeg -ss <segundos> -i <arquivo> -frames:v 1 saida.png) e recorte.

Seções (para o jogo analisado):
A. TELA E CÂMERA — altura do personagem em pé (% da altura da área de jogo); distância inicial entre eles (% da largura); linha do chão (% de cima para baixo); zoom (muda com a distância? quanto?); a câmera sobe no pulo?; tremor de tela (em que golpes, força, direção, duração); câmera nos Supers (close, inclinação, escurecimento, duração); % da tela ocupada pelo HUD em cima e embaixo.
B. CENÁRIO — camadas percebidas e paralaxe; hierarquia visual lutador × fundo (o fundo é mais escuro/claro, menos saturado, desfocado? meça se der: brilho e saturação médios de uma área do fundo na altura dos lutadores × do lutador); detalhe na faixa da luta × resto; elementos animados (público, bandeiras, água, fogo) e intensidade; chão (textura, reflexo, sombra dos personagens: forma, opacidade); luz do cenário nos personagens; paleta.
C. PERSONAGENS (RENDER) — contorno (existe? espessura em px a 1080p, cor); sombreamento (2 tons, 3 tons, degradê; sombra dura/suave); contraluz/borda iluminada; leitura de silhueta (2 exemplos); proporções; taxa de animação (60 poses/s ou "animação limitada" com poses seguradas por 2–4 quadros? estime poses distintas por segundo contando poses diferentes em sequência).
D. ANIMAÇÃO POR ESTADO (o que aparecer) — idle/guarda (amplitude e ritmo da respiração), andar frente/trás (pés plantados?), dash/backdash, pulo (pré-pulo, subida, topo, descida, pouso, poeira), agachar, defesa, intro, vitória. Para cada: poses distintas, duração, o que segura, movimento secundário (cabelo, roupa, faixa), expressão do rosto.
E. GOLPES NORMAIS — preparação (antecipação), quadro de impacto (exagero além do natural? smear/rastro desenhado?), pose de impacto segurada (quantos quadros), recuperação, diferença visual entre fraco/médio/forte, corpo inteiro participa (quadril, ombro, pé de apoio gira)?
F. ESPECIAIS E PROJÉTEIS — pose de lançamento, forma/cor/tamanho do projétil, velocidade, animação, como some ao bater; investidas (rastro, poeira, linhas de velocidade); anti-aéreo.
G. SUPERS (apresentação) — sequência com tempos: congela? flash? escurece o cenário (quanto)? close/retrato/texto? quanto até o golpe sair? efeitos durante; final (pausa maior, tela branca, câmera lenta).
H. REAÇÃO A ACERTO — poses de dano (alto, baixo, no ar, forte), cabeça/corpo, o atingido TREME na pausa (amplitude, direção)?, recuo no acerto e na defesa (% da largura), queda (pose, quique, poeira, tempo no chão, levantar), cor no atingido (piscar branco/vermelho).
I. IMPACTO — pausa no acerto (hitstop: os dois congelam) em fraco/médio/forte/especial/Super (em quadros a 60 Hz, ≈); na defesa; tremor de tela; contra-ataque (o que muda: pausa, cor da faísca, texto, som); câmera lenta; ORDEM quadro a quadro de um golpe forte que acerta (contato → faísca → pausa com tremor → empurrão → faísca some).
J. EFEITOS VISUAIS — faísca de acerto (forma: estrela/explosão/linhas radiais/anel; cores por força; tamanho em % da altura do personagem; duração; 2D desenhada ou partícula; gira/expande); faísca de defesa; poeira; rastros/linhas de velocidade/smears; brilhos e auras; efeitos de chão; partículas do cenário reagindo.
K. HUD E TEXTOS — barras de vida (posição, % da largura, forma, cores, vida perdida em vermelho que desce depois?), timer, barra de Super (segmentos), retratos, contador de combo/dano e textos de evento (COUNTER, PUNISH etc.: posição, tamanho, animação, cor, duração), telas ROUND/FIGHT/KO, tipografia, quanto o HUD chama atenção.
L. SOM — as gravações têm áudio? Se tiverem, extraia com ffmpeg e descreva o que der (picos de volume no instante do acerto, se há voz, música). Não invente.
M. FLUXO DO ROUND — intro, anúncio do round, KO, vitória (tempos), se aparecer.

Fechamento:
- Para cada seção, 3 a 5 PRINCÍPIOS que este jogo segue e que um jogo 2D em sprites deveria copiar.
- Lista "O que este jogo faz que mais o faz parecer profissional" (10 itens em ordem de impacto, cada um com [Vnn qNNNN]).
- Folhas de imagens úteis que você montou: salve em referencias/img/<jogo>_*.jpg (≤ 1600 px de largura, JPEG q85) e cite no texto.
