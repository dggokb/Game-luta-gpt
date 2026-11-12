package com.gamelutagpt.stage;

import com.gamelutagpt.render.MiniJson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Descrição de um cenário, lida do stage.json. Cada camada tem uma profundidade
 * ({@code profundidade}, unidades do mundo atrás dos lutadores; negativa = na frente)
 * e diz onde aparece na tela na câmera de repouso (frações de 0 a 1 da altura e da
 * largura da tela). O motor converte isso para o mundo 3D uma vez e depois projeta a
 * cada quadro com a câmera da luta.
 *
 * <pre>
 * {
 *   "nome": "Templo da Lua",
 *   "camera": { "distancia": 1000, "horizonte": 0.40 },
 *   "corFundo": "#0B0D1E",
 *   "camadas": [
 *     { "imagem": "ceu.jpg", "profundidade": 100000, "topo": -0.2, "base": 0.62, "repetir": "espelhar" },
 *     { "imagem": "pilares.png", "profundidade": 300, "noChao": true, "altura": 0.9,
 *       "x": 0.06, "repetir": "espelhar", "periodo": 0.88,
 *       "luzes": [ { "x": 0.6, "y": 0.3, "raio": 0.06, "cor": "#FFB45A", "reflexo": true,
 *                    "lutadores": 0.8, "alcance": 0.9 } ],   // ponto de luz dos lutadores (veja Light)
 *       "bandeiras": [ { "imagem": "bandeira.png", "x": 0.3, "y": 0.29, "largura": 0.31, "altura": 0.53 } ] },
 *     { "tipo": "neblina", "profundidade": 2600, "topo": 0.45, "base": 0.62, "cor": "#3A2A66", "alfa": 0.5 }
 *   ],
 *   "chao": { "imagem": "chao.jpg", "imagemLajotas": "chao_lajotas.jpg", "perto": -420, "longe": 560,
 *             "centroX": 700, "reflexoLutadores": 0.25 },
 *   "petalas": { "quantidade": 60, "cor": "#FFB7D5" },
 *   "poeira": { "quantidade": 140, "cor": "#FFD8A0", "alfa": 0.5, "tamanho": 2.2, "vento": 25 },
 *   "chuva": { "quantidade": 260, "cor": "#B8CCFF", "velocidade": 1500, "vento": -140 },
 *   "relampagos": { "intervaloMinimo": 5, "intervaloMaximo": 12, "profundidade": 9000 },
 *   "luzLutadores": { "cor": "#A8B8F0", "forca": 0.35, "brilho": 0.9, "pes": "#C0C4DC",
 *                     "contraluz": { "cor": "#60A8FF", "forca": 0.9, "direcao": [1, -1] } },
 *   "sombraLutadores": { "alfa": 0.42, "x": 0.6, "y": 0.22 }
 *   // ou, com focos de luz (a sombra segue cada um):
 *   // "sombraLutadores": { "alfa": 0.6, "cor": "#1A0A18", "suave": 0.5,
 *   //                     "focos": [ { "x": -145, "altura": 1080, "profundidade": 2008 } ] }
 * }
 * </pre>
 */
public final class StageDefinition {
    public enum Kind { IMAGE, FOG, WATER, ACTOR }

    public enum Repeat { NONE, REPEAT, MIRROR }

    /** Luz presa num ponto da imagem (fração da largura/altura da imagem). */
    public static final class Light {
        public final float x, y;
        /** Raio em fração da altura da imagem. */
        public final float radius;
        public final int color;
        /** Intensidade da pulsação (0 = constante). */
        public final float pulse;
        /** Desenha o reflexo da luz no chão molhado. */
        public final boolean reflect;
        /**
         * Ponto de luz dos lutadores ({@code "lutadores"}): a força com que esta luz ilumina o
         * corpo de quem está perto, do lado dela (0 = só enfeita o cenário). {@code reach}
         * ({@code "alcance"}): até onde chega, em alturas de tela; 0 = não cai com a distância
         * (a lua, o sol).
         */
        public final float fighters, reach;

        Light(float x, float y, float radius, int color, float pulse, boolean reflect) {
            this(x, y, radius, color, pulse, reflect, 0f, 0f);
        }

        Light(float x, float y, float radius, int color, float pulse, boolean reflect, float fighters, float reach) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.pulse = pulse;
            this.reflect = reflect;
            this.fighters = fighters;
            this.reach = reach;
        }
    }

    /** Fogo animado preso num ponto da imagem; tamanho em fração da altura da imagem. */
    public static final class Fire {
        public final float x, y, size;
        /** Ponto de luz dos lutadores, como em {@link Light}: força, alcance e a cor da luz do fogo. */
        public final float fighters, reach;
        public final int color;

        Fire(float x, float y, float size) {
            this(x, y, size, 0f, 0f, 0xFFFF9A3C);
        }

        Fire(float x, float y, float size, float fighters, float reach, int color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.fighters = fighters;
            this.reach = reach;
            this.color = color;
        }
    }

    /**
     * Uma faixa de plateia viva numa imagem ({@code "plateia"}, um objeto ou uma lista): entre
     * {@code topo} e {@code base} (fração da altura da imagem), redesenhada em {@code colunas}
     * colunas que pulam, cada uma no seu tempo; {@code pulo} é a altura do pulo em fração da
     * altura da imagem. Pulam mais quando a luta esquenta ({@code StageScene.cheer}). Só o
     * miolo da faixa pula: uma margem do tamanho do maior pulo fica parada em cima e em baixo,
     * então a faixa precisa estar toda dentro da plateia (sem pegar grade ou parapeito).
     */
    public static final class Crowd {
        public final float top, bottom, jump;
        public final int columns;
        /**
         * Imagem só com a gente ({@code "mascara"}, do mesmo tamanho da camada; o resto
         * transparente): o pulo mexe só esses pixels, e os estandartes, luzes e grades que
         * passam pela faixa ficam parados. Null = a faixa inteira pula.
         */
        public final String mask;
        /**
         * Pontos de gente da faixa ({@code "flashes"}: [[x, y], ...] em fração da imagem), onde
         * piscam flashes de câmera e celular; muitos mais quando a luta esquenta. Saem da
         * máscara na montagem (tools/stage/pontos_plateia.py), então nunca caem num estandarte.
         */
        public final float[] flashX, flashY;
        /**
         * {@code "recortada"}: a gente foi tirada da imagem da camada (no lugar ficaram os
         * assentos escuros) e só existe na {@code mascara}. Ela é desenhada sempre, inteira,
         * e cada grupo pula livre: embaixo aparece assento vazio, não uma cópia da plateia.
         */
        public final boolean cutout;

        Crowd(float top, float bottom, float jump, int columns, String mask, float[] flashX, float[] flashY,
              boolean cutout) {
            this.cutout = cutout && mask != null;
            this.mask = mask;
            this.flashX = flashX;
            this.flashY = flashY;
            this.top = top;
            this.bottom = bottom;
            this.jump = jump;
            this.columns = columns;
        }
    }

    /**
     * Bandeira presa na imagem, balançando ao vento. Posição e tamanho em fração da imagem
     * da camada (canto de cima à esquerda); a imagem da bandeira é o pano recortado da
     * própria imagem (tools/stage/separar_bandeiras.py), que fica sem ele.
     */
    public static final class Banner {
        public final String image;
        public final float x, y, width, height;
        /** Quanto a ponta de baixo vai para o lado, em fração da largura da bandeira. */
        public final float sway;
        /** Ondas que descem pelo pano e a força do vento (1 = normal). */
        public final float waves, speed;

        Banner(String image, float x, float y, float width, float height, float sway, float waves, float speed) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.sway = sway;
            this.waves = waves;
            this.speed = speed;
        }
    }

    /**
     * Água pintada na imagem que se mexe ({@code "cachoeiras"}: um objeto ou uma lista, um por
     * folha): cachoeiras correndo ou mar agitado. A {@code imagem} é uma folha de quadros
     * feita na montagem (tools/stage/cachoeiras.py), em blocos de {@code colunas} quadros por
     * fileira (todos, se não disser). Cada bloco é um quadro: as áreas empilhadas, cada uma
     * com a altura proporcional à dela e todas na largura da área mais larga (a área fica
     * encostada à esquerda). Só os pixels de água mudam entre os quadros, então as pedras em
     * volta não se mexem.
     */
    public static final class Waterfalls {
        public final String image;
        public final int frames, columns;
        public final float fps;
        /** Áreas: x, y, largura e altura em fração da imagem da camada (4 por área). */
        public final float[] areas;
        /**
         * Pés das cachoeiras ({@code "espuma"}: [[x, y, largura], ...] em fração da imagem; y é
         * onde a água bate): ali sobem borrifos, nuvens claras que crescem e somem.
         */
        public final float[] foam;
        public final int foamColor;

        Waterfalls(String image, int frames, int columns, float fps, float[] areas, float[] foam, int foamColor) {
            this.image = image;
            this.frames = frames;
            this.columns = Math.max(1, Math.min(columns, frames));
            this.foam = foam;
            this.foamColor = foamColor;
            this.fps = fps;
            this.areas = areas;
        }

        public int count() {
            return areas.length / 4;
        }
    }

    /**
     * {@code "vento"} numa camada de imagem (as copas recortadas): uma grade de {@code colunas} x
     * {@code linhas} sobre a imagem; cada vértice tem o {@code alcance} (0 no tronco, que não mexe,
     * até 1 na ponta dos galhos). Os {@code grupos} (x, y em fração da imagem) são os galhos: cada um
     * uma mola com a sua frequência ({@code freq}, Hz) empurrada pelo vento, que vem em rajadas
     * lentas e atravessa a tela; cada vértice segue os galhos perto dele ({@code raio}, fração da
     * largura). {@code amplitude}: o deslocamento na ponta, em fração da altura da imagem.
     */
    public static final class Wind {
        public int cols, rows;
        public float[] reach, groupX, groupY, freq;
        public float amp, damping, radius;
        /** Com {@code "pendulos"} no lugar dos galhos: peças penduradas que giram (null = galhos). */
        public Pendulum[] pendulums;
    }

    /**
     * Uma peça pendurada ({@code "pendulos"} no {@code "vento"}): gira em volta do {@code pivo}
     * (fração da imagem) como um pêndulo de frequência {@code freq} (Hz), que o vento empurra até
     * {@code graus} para o lado e de volta. Cada vértice da grade gira com o {@code peso} dele (0 a
     * 1; 1 em tudo = a peça inteira gira). {@code atraso}: segundos até a rajada chegar aqui.
     */
    public static final class Pendulum {
        public float px, py, amp, freq, delay;
        public float[] weight;
    }

    public static final class Layer {
        public final String id;
        public final Kind kind;
        public final String image;
        public final float depth;
        /** Topo e base na tela de repouso (frações); NaN quando a camada usa noChao/altura. */
        public final float top, bottom;
        /** A base fica apoiada no chão da própria profundidade. */
        public final boolean onGround;
        /** Altura na tela de repouso (fração), para camadas noChao. */
        public final float height;
        /** Centro da primeira cópia na tela de repouso (fração da largura). */
        public final float x;
        public final Repeat repeat;
        /** Distância entre cópias na tela de repouso (fração da largura); NaN = encostadas. */
        public final float period;
        public final int color, colorBottom;
        public final float alpha;
        /** Cor que preenche a tela acima da camada (céu), ou 0. */
        public final int fillAbove;
        /** Opacidade da cópia espelhada embaixo da base (reflexo na água), 0 = sem. */
        public final float reflection;
        /**
         * Profundidade da camada em que esta se apoia ({@code "apoio"}), ou NaN. Uma ilha ou um
         * barco desenhados sobre o mar (uma imagem bem mais funda) deslizavam em cima da água
         * quando a câmera subia; com o apoio, a base acompanha na vertical o ponto do mar em que
         * ela está na câmera de repouso (o tamanho e o movimento para os lados continuam os da
         * própria profundidade).
         */
        public final float support;
        /**
         * Camada que anda sozinha ({@code "deriva"}: unidades do mundo por segundo, negativo =
         * para a esquerda), como barcos na água; com {@code repetir} os que saem de um lado
         * voltam pelo outro. {@code "balanco"}: quanto ela sobe e desce nas ondas, em fração
         * da altura dela.
         */
        public final float sail, bob;
        /** Faixas de plateia viva da imagem ({@code "plateia"}). */
        public final List<Crowd> crowds;
        /** Água animada da imagem ({@code "cachoeiras"}), uma por folha. */
        public final List<Waterfalls> waterfalls;
        /**
         * Neblina com {@code "fiapos"}: quantos bancos de névoa (manchas largas e baixas) cabem
         * na largura da tela de repouso, rente à base da faixa, andando com a {@code deriva} e
         * respirando devagar. Cobrem a linha em que ilhas e montanhas encontram o mar.
         */
        public final int wisps;
        /** Água: faixa de profundidades e quantidade de brilhos. */
        public final float waterNear, waterFar;
        public final int shimmer;
        /**
         * Água com {@code "imagem"}: textura do mar vista de cima, deitada em perspectiva entre
         * {@code perto} e {@code longe} como o piso. {@code tamanho} é a largura de uma cópia
         * no mundo e {@code corrente} quantas unidades por segundo ela anda para o lado.
         */
        public final float waterTile, drift;
        /** Reflexo do sol na água: x na tela (câmera de repouso, no céu), cor e opacidade; x NaN = sem. */
        public final float sunX, sunAlpha;
        public final int sunColor;
        public final List<Light> lights;
        public final List<Fire> fires;
        public final List<Banner> banners;
        /**
         * {@code "telao": {"x", "y", "largura", "altura"}}: a tela de um telão na imagem da
         * camada (frações dela, do canto de cima à esquerda), onde o jogo mostra a luta; null
         * sem telão.
         */
        public final float[] screen;
        /** Ator: pasta dele, relativa ao cenário (com o ator.json e as folhas). */
        public final String actor;
        /** Ator: espelhado (olhando para o outro lado). */
        public final boolean flip;

        Layer(Builder b) {
            id = b.id;
            kind = b.kind;
            image = b.image;
            depth = b.depth;
            top = b.top;
            bottom = b.bottom;
            onGround = b.onGround;
            height = b.height;
            x = b.x;
            repeat = b.repeat;
            period = b.period;
            color = b.color;
            colorBottom = b.colorBottom;
            alpha = b.alpha;
            fillAbove = b.fillAbove;
            reflection = b.reflection;
            support = b.support;
            sail = b.sail;
            bob = b.bob;
            crowds = Collections.unmodifiableList(b.crowds);
            waterfalls = Collections.unmodifiableList(new ArrayList<>(b.waterfalls));
            wisps = b.wisps;
            waterNear = b.waterNear;
            waterFar = b.waterFar;
            shimmer = b.shimmer;
            waterTile = b.waterTile;
            drift = b.drift;
            sunX = b.sunX;
            sunAlpha = b.sunAlpha;
            sunColor = b.sunColor;
            lights = Collections.unmodifiableList(new ArrayList<>(b.lights));
            fires = Collections.unmodifiableList(new ArrayList<>(b.fires));
            banners = Collections.unmodifiableList(new ArrayList<>(b.banners));
            screen = b.screen;
            actor = b.actor;
            flip = b.flip;
        }

        /**
         * {@code "curva": [[u, profundidade], ...]}: a camada não é um plano só; cada coluna (u =
         * fração da largura da imagem) fica na sua profundidade (interpolada entre os pontos). A
         * arquibancada que dá a volta na arena, chegando perto da câmera nas pontas. Null = plano.
         * A camada curva é desenhada em fatias (sem repetição, bandeiras, plateia ou telão).
         */
        public float[] curveU, curveZ;
        /**
         * {@code "onda": {"amplitude", "velocidade", "fase", "leve"}}: o alfa vai de 1 a 1 − amplitude
         * e volta. Duas camadas iguais em fase oposta (fachos de luz pintados em dois ângulos)
         * cruzam uma na outra, e a luz parece varrer. {@code leve}: o alfa fixo no modo leve.
         */
        public float waveAmp, waveSpeed = 1f, wavePhase, waveLight = 1f;
        /**
         * {@code "lateral": {"x", "longe", "perto"}}: a camada é uma parede do lado da arena, de
         * frente para o meio (paralela ao olhar da câmera), indo da profundidade {@code longe} (onde
         * encontra o fundo, em {@code x} = fração da tela de repouso) até {@code perto}. A imagem
         * é a fachada desenrolada, sem perspectiva pintada: a esquerda dela fica no fundo, e o
         * motor põe a perspectiva. {@code abre} (mundo, padrão 0): quanto a ponta de perto fica
         * mais para fora (a parede na diagonal, a arquibancada fazendo a volta). {@code topo} vale na profundidade {@code longe}; a base é o
         * chão. {@code "virar"}: a imagem vem espelhada e o motor lê as colunas de trás para frente (a
         * parede da esquerda, que na tela vai da direita para a esquerda; no Canvas cada fatia
         * sai do lado certo). NaN = não é parede lateral.
         */
        public float sideX = Float.NaN, sideFar, sideNear, sideOpen;
        /** {@code "vento"}: a camada balança com o vento (árvores); null = parada. */
        public Wind wind;

        /** Profundidade da coluna {@code u} da camada curva. */
        public float curveDepth(float u) {
            if (curveU == null) return depth;
            if (u <= curveU[0]) return curveZ[0];
            for (int i = 1; i < curveU.length; i++) {
                if (u <= curveU[i]) {
                    float t = (u - curveU[i - 1]) / Math.max(1e-6f, curveU[i] - curveU[i - 1]);
                    return curveZ[i - 1] + (curveZ[i] - curveZ[i - 1]) * t;
                }
            }
            return curveZ[curveZ.length - 1];
        }

        /** Profundidade usada para ordenar (a água usa a borda de trás; a curva, a mais perto). */
        public float sortDepth() {
            if (!Float.isNaN(sideX)) return sideNear;
            if (curveZ != null) {
                float min = Float.MAX_VALUE;
                for (float z : curveZ) min = Math.min(min, z);
                return min;
            }
            return kind == Kind.WATER ? waterFar : depth;
        }
    }

    private static final class Builder {
        String id = "";
        Kind kind = Kind.IMAGE;
        String image;
        float depth = Float.NaN;
        float top = Float.NaN, bottom = Float.NaN;
        boolean onGround;
        float height = Float.NaN;
        float x = 0.5f;
        Repeat repeat = Repeat.NONE;
        float period = Float.NaN;
        int color, colorBottom;
        float alpha = 1f;
        int fillAbove;
        float reflection;
        float support = Float.NaN;
        float sail, bob;
        final List<Crowd> crowds = new ArrayList<>();
        final List<Waterfalls> waterfalls = new ArrayList<>();
        int wisps;
        float waterNear = Float.NaN, waterFar = Float.NaN;
        int shimmer;
        float waterTile = 1600f, drift, sunX = Float.NaN, sunAlpha;
        int sunColor;
        final List<Light> lights = new ArrayList<>();
        final List<Fire> fires = new ArrayList<>();
        final List<Banner> banners = new ArrayList<>();
        float[] screen;
        String actor;
        boolean flip;
    }

    public static final class Floor {
        public final String image;
        /**
         * Imagem só de lajotas, sem o emblema, para as repetições fora do centro; null =
         * repete a imagem principal (o emblema aparece de novo quando a câmera anda).
         */
        public final String plainImage;
        /** Profundidades da borda de baixo e da borda de cima da imagem do piso. */
        public final float near, far;
        /** X do mundo onde fica o centro da imagem (o emblema, por exemplo). */
        public final float centerX;
        /** Opacidade do reflexo dos lutadores no piso (0 = sem reflexo). */
        public final float fighterReflection;
        /** Opacidade do reflexo das luzes no piso. */
        public final float lightReflection;
        /**
         * {@code "reflexoComprimento"}: o reflexo de cada luz vira um risco vertical descendo pelo
         * chão encerado, até esta fração do caminho para a borda de baixo da tela (0 = só o brilho
         * curto). Desenhado na tela, fica sempre na vertical embaixo da luz, com a câmera em
         * qualquer lugar (pintado no chão projetado, abria em leque nos cantos).
         */
        public float lightReflectionLength;
        /**
         * {@code "reflexoImagem"}: a imagem do risco (já na cor, alfa macio dos lados e sumindo para
         * baixo), esticada do pé de cada luz até o comprimento; null = o risco em degradês.
         */
        public String lightReflectionImage;
        /**
         * Até onde o chão continua atrás do piso ({@code "ate"}), repetindo a imagem lisa na
         * profundidade; igual a {@code far} = o piso acaba na borda de cima da imagem. Para
         * o chão de uma arena ir do ringue até a arquibancada, lá no fundo.
         */
        public final float extent;
        /**
         * Largura da imagem no mundo ({@code "largura"}); NaN = a da proporção da imagem
         * ({@code (far - near) * largura / altura}, lajota quadrada).
         */
        public final float width;
        /**
         * Linhas da imagem em perspectiva ({@code "linhasPerspectiva"}: a distância da câmera de
         * repouso): a linha da imagem anda junto com a linha da tela, em vez de igual na
         * profundidade. Assim o chão perto da câmera, que ocupa metade da tela, tem tantas linhas
         * quanto a tela (com linhas iguais na profundidade, ele ficava com poucas, esticadas).
         * NaN = linhas iguais na profundidade.
         */
        public final float rowDistance;
        /**
         * Chão projetado de uma pintura ({@code "projecao": {"topo", "base", "x"}}): a imagem é o
         * pedaço de baixo da pintura, em perspectiva, e fica na tela de repouso entre {@code topo}
         * e {@code base} (frações da altura) com o centro em {@code x} (fração da largura). Cada
         * ponto do chão pega o pixel em que a câmera de repouso o vê: parada, a tela é a pintura
         * sem deformar; andando, cada faixa do chão anda na velocidade da profundidade dela.
         * Null = o piso visto de cima (o modo normal).
         */
        public float[] projection;

        Floor(String image, String plainImage, float near, float far, float centerX,
              float fighterReflection, float lightReflection, float extent) {
            this(image, plainImage, near, far, centerX, fighterReflection, lightReflection, extent, Float.NaN, Float.NaN);
        }

        Floor(String image, String plainImage, float near, float far, float centerX,
              float fighterReflection, float lightReflection, float extent, float width, float rowDistance) {
            this.width = width;
            this.rowDistance = rowDistance;
            this.extent = Math.max(far, extent);
            this.image = image;
            this.plainImage = plainImage;
            this.near = near;
            this.far = far;
            this.centerX = centerX;
            this.fighterReflection = fighterReflection;
            this.lightReflection = lightReflection;
        }
    }

    public static final class Petals {
        public final int count;
        public final int color;
        public final float near, far;
        /** Velocidades em unidades do mundo por segundo. */
        public final float wind, fall;
        public final float size;

        Petals(int count, int color, float near, float far, float wind, float fall, float size) {
            this.count = count;
            this.color = color;
            this.near = near;
            this.far = far;
            this.wind = wind;
            this.fall = fall;
            this.size = size;
        }
    }

    /**
     * Poeira no ar: grãozinhos de luz boiando em 3D (sobem e descem devagar, vão com o vento e
     * piscam quando pegam a luz). Os da frente dos lutadores ficam maiores e desfocados.
     */
    public static final class Dust {
        public final int count;
        public final int color;
        public final float near, far;
        /** Vento em unidades do mundo por segundo; tamanho em px na profundidade 0. */
        public final float wind, size;
        /** Opacidade máxima (0 a 1). */
        public final float alpha;
        /** Faixa da tela de repouso onde a poeira boia (frações; topo &lt; base). */
        public final float top, bottom;

        Dust(int count, int color, float near, float far, float wind, float size, float alpha, float top, float bottom) {
            this.count = count;
            this.color = color;
            this.near = near;
            this.far = far;
            this.wind = wind;
            this.size = size;
            this.alpha = alpha;
            this.top = top;
            this.bottom = bottom;
        }
    }

    /** Chuva: riscos caindo em 3D, com respingos no piso. */
    public static final class Rain {
        public final int count;
        public final int color;
        public final float near, far;
        /** Queda e vento em unidades do mundo por segundo. */
        public final float speed, wind;
        /** Comprimento do risco em unidades do mundo e espessura em px na profundidade 0. */
        public final float length, width;
        public final float alpha;
        /** Quantos respingos podem estar no piso ao mesmo tempo (0 = sem respingos). */
        public final int splashes;

        Rain(int count, int color, float near, float far, float speed, float wind,
             float length, float width, float alpha, int splashes) {
            this.count = count;
            this.color = color;
            this.near = near;
            this.far = far;
            this.speed = speed;
            this.wind = wind;
            this.length = length;
            this.width = width;
            this.alpha = alpha;
            this.splashes = splashes;
        }
    }

    /**
     * Relâmpagos: de vez em quando um raio em zigue-zague cai no céu numa profundidade (atrás da
     * cidade, que fica recortada contra o clarão), o céu clareia piscando duas vezes e o
     * cenário todo clareia um pouco.
     */
    public static final class Lightning {
        /** Intervalo entre relâmpagos, em segundos (sorteado entre os dois). */
        public final float minInterval, maxInterval;
        /** Profundidade do raio: o clarão do céu cobre o que está atrás dela. */
        public final float depth;
        /** Topo e base do raio na tela de repouso (frações). */
        public final float top, bottom;
        public final int color;
        /** Opacidade do clarão no céu e no cenário todo (0 a 1). */
        public final float skyFlash, sceneFlash;

        Lightning(float minInterval, float maxInterval, float depth, float top, float bottom, int color,
                  float skyFlash, float sceneFlash) {
            this.minInterval = minInterval;
            this.maxInterval = maxInterval;
            this.depth = depth;
            this.top = top;
            this.bottom = bottom;
            this.color = color;
            this.skyFlash = skyFlash;
            this.sceneFlash = sceneFlash;
        }
    }

    public final String name;
    public final float cameraDistance;
    /** {@code camera.zoom}: multiplica o zoom da câmera da luta neste cenário (1 = o normal). */
    public float cameraZoom = 1f;
    /** {@code camera.zoomNoPulo} (padrão true): false = o pulo não abre o zoom da câmera da luta. */
    public boolean cameraZoomNoPulo = true;
    /**
     * {@code camera.zoomRepouso}: o zoom da câmera em que o stage.json descreve as camadas (NaN =
     * a câmera de repouso do jogo). Para um cenário pintado a partir de uma tela da luta: a pintura
     * bate com a luta nesse zoom, sem as proporções de chão e fundo mudarem.
     */
    public float cameraRestZoom = Float.NaN;
    /** Altura do horizonte na tela (fração). */
    public final float horizon;
    public final int backgroundColor;
    public final List<Layer> layers;
    public final Floor floor;
    /**
     * {@code "chaoNaPintura": true}: o chão já está pintado na camada (o cenário é uma pintura
     * só); sem {@code chao}, não desenha o chão liso por cima dela.
     */
    public final boolean floorInPainting;
    /** Null quando o cenário não tem pétalas. */
    public final Petals petals;
    /** Null quando o cenário não tem poeira no ar. */
    public Dust dust;
    /** Null quando o cenário não tem chuva. */
    public final Rain rain;
    /** Null quando o cenário não tem relâmpagos. */
    public final Lightning lightning;
    /** A luz do cenário nos lutadores (sol, noite, chuva, lanternas); null = sem tom. */
    public final FighterLight fighterLight;
    /** A sombra comprida dos lutadores no chão (sol baixo), ou null. */
    public final FighterShadow fighterShadow;

    /**
     * Sombra projetada ({@code "sombraLutadores"}): a silhueta do lutador deitada no chão, para
     * o lado contrário da luz. {@code alfa} é o escuro dela.
     *
     * <p>Sol (luz muito longe, a mesma sombra em todo lugar): um ponto a {@code h} acima do chão
     * cai a {@code x·h} para o lado (positivo = direita) e {@code y·h} para baixo na tela (para
     * perto da câmera).
     *
     * <p>Focos ({@code "focos"}: holofote, tocha, poste): cada um é um ponto no mundo
     * ({@code x}, {@code altura} acima do chão e {@code profundidade}, positiva atrás dos
     * lutadores) e a sombra de cada lutador sai para o lado contrário dele: comprida longe do
     * foco, curta embaixo dele, e vira de lado quando o lutador passa por baixo. Com
     * {@code alcance} ela clareia com a distância até sumir; {@code alfa} do foco troca o da
     * sombra. Até {@link #MAX_FOCOS} focos; cada lutador desenha a sombra dos
     * {@link #POR_LUTADOR} mais fortes sobre ele (cada uma é uma cópia do lutador a mais por
     * quadro). Uma tocha baixa (mais baixa que a cabeça) dá sombra comprida: ela fica na direção
     * certa e só é encurtada até {@link #MAX_COMPRIMENTO}.
     */
    public static final class FighterShadow {
        public static final int MAX_FOCOS = 8;
        /** Quantas sombras (as dos focos mais fortes) cada lutador desenha. */
        public static final int POR_LUTADOR = 3;
        /** Meia altura de um lutador (mundo): a conta do foco usa esse ponto do corpo. */
        static final float MEIO_CORPO = 150f;
        /** Até onde a sombra estica (para o lado e para a frente), por unidade de altura. */
        static final float MAX_COMPRIMENTO = 1.4f, MAX_TRAS = 0.5f;

        public final float alpha, skewX, squashY;
        /** Os focos de luz; vazio = sol (usa {@link #skewX} e {@link #squashY}). */
        public final List<Focus> focos;
        /** A cor da sombra ({@code "cor"}, RGB): a luz ambiente que sobra nela (preto = nenhuma). */
        public final int color;
        /**
         * O quanto ela abre e clareia longe dos pés ({@code "suave"}, 0 a 1): a sombra de verdade
         * é dura onde encosta no corpo e vai ficando borrada e fraca na ponta (penumbra).
         */
        public final float softness;

        FighterShadow(float alpha, float skewX, float squashY) {
            this(alpha, skewX, squashY, Collections.<Focus>emptyList(), 0, 0.5f);
        }

        FighterShadow(float alpha, float skewX, float squashY, List<Focus> focos, int color, float softness) {
            this.alpha = alpha;
            this.skewX = skewX;
            this.squashY = squashY;
            this.focos = Collections.unmodifiableList(new ArrayList<>(focos));
            this.color = color & 0xFFFFFF;
            this.softness = Math.max(0f, Math.min(1f, softness));
        }

        /**
         * Para onde fica o foco visto do peito do lutador, na tela (x para a direita, y para
         * baixo; {@code out[0..1]}, tamanho 1), e quanto a contraluz acende ({@code out[2]}: cheia
         * com o foco atrás dos lutadores, fraca com ele na frente, iluminando de frente). A conta
         * da câmera de repouso: um ponto na profundidade {@code z} aparece {@code D / (D + z)}
         * menor, puxado para o centro da câmera e para a linha do olho.
         */
        public static void rimFromFocus(Focus f, float fighterX, float cameraX, float eyeY, float groundY,
                                        float restDistance, float[] out) {
            float z = Math.max(f.depth, -0.8f * restDistance);
            float r = restDistance / (restDistance + z);
            float lx = cameraX + (f.x - cameraX) * r;
            float ly = eyeY + (groundY - eyeY) * r - f.height * r;
            float dx = lx - fighterX, dy = ly - (groundY - MEIO_CORPO);
            float n = (float)Math.sqrt(dx * dx + dy * dy);
            out[0] = n > 1e-3f ? dx / n : 0f;
            out[1] = n > 1e-3f ? dy / n : -1f;
            out[2] = f.depth >= 0f ? 1f : 0.35f;
        }

        /**
         * A sombra de um lutador em {@code fighterX} (no chão, profundidade 0) por um foco:
         * {@code out[0]} para o lado e {@code out[1]} para baixo na tela por unidade de altura
         * (como o {@code x} e o {@code y} do sol), {@code out[2]} o escuro (0 = some).
         * {@code depthToScreen} converte profundidade do chão em linhas da tela no plano dos
         * lutadores ({@link #depthToScreen}).
         */
        public void fromFocus(Focus f, float fighterX, float depthToScreen, float[] out) {
            float h = Math.max(f.height * 0.3f, f.height - MEIO_CORPO);
            float lado = (fighterX - f.x) / h;
            float frente = f.depth / h * depthToScreen;
            frente = Math.max(-MAX_TRAS, frente);
            // Comprida demais (foco baixo, quase na altura do corpo): mesma direção, mais curta.
            float comprimento = (float)Math.sqrt(lado * lado + frente * frente);
            float k = comprimento > MAX_COMPRIMENTO ? MAX_COMPRIMENTO / comprimento : 1f;
            out[0] = lado * k;
            out[1] = frente * k;
            float a = Float.isNaN(f.alpha) ? alpha : f.alpha;
            if (f.range > 0f) {
                float dx = fighterX - f.x;
                float d = (float)Math.sqrt(dx * dx + f.depth * f.depth);
                a *= Math.max(0f, 1f - d / f.range);
            }
            out[2] = a;
        }

        /**
         * Quantas linhas da tela (no plano dos lutadores, antes do zoom) o chão anda por unidade
         * de profundidade, na câmera de repouso: {@code (chão na tela − horizonte) / distância}.
         */
        public static float depthToScreen(float groundScreenY, float horizonScreenY, float cameraDistance) {
            return (groundScreenY - horizonScreenY) / cameraDistance;
        }
    }

    /** Um foco de luz da sombra ({@code "focos"} em {@code "sombraLutadores"}). */
    public static final class Focus {
        public final float x, height, depth, alpha, range;

        Focus(float x, float height, float depth, float alpha, float range) {
            this.x = x;
            this.height = height;
            this.depth = depth;
            this.alpha = alpha;
            this.range = range;
        }
    }

    /**
     * A luz do lugar sobre os lutadores: cada canal da cor multiplicado por
     * {@code brilho × ((1 − forca) + forca × cor)}. Uma matriz de cor só, somada ao filtro que o
     * lutador já usa (o desgaste): não custa um desenho a mais.
     */
    public static final class FighterLight {
        public final int color;
        public final float strength, brightness;
        /**
         * Degradê de cima para baixo: a cabeça fica como está e os pés multiplicados por esta cor
         * ({@code "pes"}; branco = sem degradê).
         */
        public final int feet;
        /**
         * Contraluz ({@code "contraluz"}): um fio de luz na borda do corpo do lado de onde a luz vem.
         * {@code rimDirX/Y}: para onde fica a luz na tela (x para a direita, y para baixo), já
         * normalizado; força 0 = sem contraluz.
         */
        public final int rimColor;
        public final float rimStrength, rimDirX, rimDirY;

        FighterLight(int color, float strength, float brightness) {
            this(color, strength, brightness, 0xFFFFFFFF, 0xFFFFFFFF, 0f, 0f, -1f);
        }

        FighterLight(int color, float strength, float brightness, int feet,
                     int rimColor, float rimStrength, float rimDirX, float rimDirY) {
            this.color = color;
            this.strength = strength;
            this.brightness = brightness;
            this.feet = feet;
            this.rimColor = rimColor;
            this.rimStrength = rimStrength;
            float n = (float)Math.sqrt(rimDirX * rimDirX + rimDirY * rimDirY);
            this.rimDirX = n > 0f ? rimDirX / n : 0f;
            this.rimDirY = n > 0f ? rimDirY / n : -1f;
        }

        /** O fator dos pés em cada canal (vermelho, verde, azul). */
        public float[] feetFactors() {
            return new float[] {((feet >> 16) & 0xFF) / 255f, ((feet >> 8) & 0xFF) / 255f, (feet & 0xFF) / 255f};
        }

        /** O fator de cada canal (vermelho, verde, azul). */
        public float[] factors() {
            float[] f = new float[3];
            for (int i = 0; i < 3; i++) {
                float c = ((color >> (16 - 8 * i)) & 0xFF) / 255f;
                f[i] = brightness * ((1f - strength) + strength * c);
            }
            return f;
        }
    }

    private StageDefinition(String name, float cameraDistance, float horizon, int backgroundColor,
                            List<Layer> layers, Floor floor, Petals petals, Rain rain, Lightning lightning,
                            FighterLight fighterLight, FighterShadow fighterShadow, boolean floorInPainting) {
        this.name = name;
        this.floorInPainting = floorInPainting;
        this.cameraDistance = cameraDistance;
        this.horizon = horizon;
        this.backgroundColor = backgroundColor;
        this.layers = Collections.unmodifiableList(layers);
        this.floor = floor;
        this.petals = petals;
        this.rain = rain;
        this.lightning = lightning;
        this.fighterLight = fighterLight;
        this.fighterShadow = fighterShadow;
    }

    // ------------------------------------------------------------- leitura

    @SuppressWarnings("unchecked")
    public static StageDefinition fromJson(String json) {
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map)) throw new IllegalArgumentException("stage.json deve ser um objeto");
        Map<String, Object> map = (Map<String, Object>)root;

        String name = string(map, "nome", null);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("stage.json precisa do campo \"nome\"");
        }
        Map<String, Object> camera = object(map, "camera");
        float distance = number(camera, "distancia", 1000f);
        float horizon = number(camera, "horizonte", 0.40f);
        if (distance <= 0f) throw new IllegalArgumentException("camera.distancia deve ser positiva");
        float zoomCenario = number(camera, "zoom", 1f);
        if (!(zoomCenario > 0.3f && zoomCenario <= 2f)) throw new IllegalArgumentException("camera.zoom fora de 0,3 a 2");

        List<Layer> layers = new ArrayList<>();
        Object layersField = map.get("camadas");
        if (layersField instanceof List) {
            int index = 0;
            for (Object item : (List<Object>)layersField) {
                if (!(item instanceof Map)) throw new IllegalArgumentException("camada " + index + " deve ser um objeto");
                layers.add(layer((Map<String, Object>)item, index++));
            }
        }

        Floor floor = null;
        Map<String, Object> floorMap = object(map, "chao");
        if (floorMap != null) {
            String image = string(floorMap, "imagem", null);
            if (image == null) throw new IllegalArgumentException("chao precisa de \"imagem\"");
            float near = number(floorMap, "perto", -400f);
            float far = number(floorMap, "longe", 550f);
            if (far <= near) throw new IllegalArgumentException("chao: \"longe\" deve ser maior que \"perto\"");
            floor = new Floor(image, string(floorMap, "imagemLajotas", null), near, far,
                number(floorMap, "centroX", Float.NaN),
                number(floorMap, "reflexoLutadores", 0f), number(floorMap, "reflexoLuzes", 0.35f),
                number(floorMap, "ate", far), number(floorMap, "largura", Float.NaN),
                number(floorMap, "linhasPerspectiva", Float.NaN));
            floor.lightReflectionLength = clamp01(number(floorMap, "reflexoComprimento", 0f));
            floor.lightReflectionImage = string(floorMap, "reflexoImagem", null);
            Map<String, Object> proj = object(floorMap, "projecao");
            if (proj != null) {
                float pt = number(proj, "topo", Float.NaN), pb = number(proj, "base", Float.NaN);
                if (!(pb > pt)) throw new IllegalArgumentException("chao.projecao precisa de \"topo\" e \"base\" (base > topo)");
                floor.projection = new float[] {pt, pb, number(proj, "x", 0.5f)};
            }
        }

        Petals petals = null;
        Map<String, Object> petalsMap = object(map, "petalas");
        if (petalsMap != null) {
            petals = new Petals(
                Math.max(0, Math.round(number(petalsMap, "quantidade", 50f))),
                color(petalsMap, "cor", 0xFFFFB7D5),
                number(petalsMap, "perto", -300f),
                number(petalsMap, "longe", 2500f),
                number(petalsMap, "vento", 40f),
                number(petalsMap, "queda", 70f),
                number(petalsMap, "tamanho", 7f));
        }

        Rain rain = null;
        Map<String, Object> rainMap = object(map, "chuva");
        if (rainMap != null) {
            float near = number(rainMap, "perto", -300f);
            float far = number(rainMap, "longe", 2500f);
            if (far <= near) throw new IllegalArgumentException("chuva: \"longe\" deve ser maior que \"perto\"");
            float speed = number(rainMap, "velocidade", 1500f);
            if (speed <= 0f) throw new IllegalArgumentException("chuva: \"velocidade\" deve ser positiva");
            rain = new Rain(
                Math.max(0, Math.round(number(rainMap, "quantidade", 240f))),
                color(rainMap, "cor", 0xFFB8CCFF),
                near, far, speed,
                number(rainMap, "vento", -120f),
                Math.max(1f, number(rainMap, "comprimento", 70f)),
                Math.max(0.3f, number(rainMap, "espessura", 1.6f)),
                clamp01(number(rainMap, "alfa", 0.5f)),
                Math.max(0, Math.round(number(rainMap, "respingos", 30f))));
        }

        Lightning lightning = null;
        Map<String, Object> lightningMap = object(map, "relampagos");
        if (lightningMap != null) {
            float min = number(lightningMap, "intervaloMinimo", 5f);
            float max = number(lightningMap, "intervaloMaximo", 12f);
            if (min <= 0f || max < min) {
                throw new IllegalArgumentException("relampagos: intervalos devem ser positivos e o máximo >= o mínimo");
            }
            float top = number(lightningMap, "topo", -0.02f), bottom = number(lightningMap, "base", 0.42f);
            if (bottom <= top) throw new IllegalArgumentException("relampagos: \"base\" deve ficar abaixo do \"topo\"");
            lightning = new Lightning(min, max, number(lightningMap, "profundidade", 9000f), top, bottom,
                color(lightningMap, "cor", 0xFFD8E4FF),
                clamp01(number(lightningMap, "claraoCeu", 0.45f)),
                clamp01(number(lightningMap, "claraoCena", 0.12f)));
        }

        FighterLight light = null;
        Map<String, Object> lightMap = object(map, "luzLutadores");
        if (lightMap != null) {
            float brilho = number(lightMap, "brilho", 1f);
            if (brilho <= 0f || brilho > 2f) throw new IllegalArgumentException("luzLutadores: \"brilho\" entre 0 e 2");
            int pes = color(lightMap, "pes", 0xFFFFFFFF);
            int rimCor = 0xFFFFFFFF;
            float rimForca = 0f, rimX = 0f, rimY = -1f;
            Map<String, Object> rim = object(lightMap, "contraluz");
            if (rim != null) {
                rimCor = color(rim, "cor", 0xFFFFFFFF);
                rimForca = number(rim, "forca", 0.8f);
                if (rimForca < 0f || rimForca > 2f) throw new IllegalArgumentException("contraluz: \"forca\" entre 0 e 2");
                Object dir = rim.get("direcao");
                if (dir != null) {
                    if (!(dir instanceof List) || ((List<?>)dir).size() != 2) {
                        throw new IllegalArgumentException("contraluz: \"direcao\" é [x, y]");
                    }
                    rimX = ((Number)((List<?>)dir).get(0)).floatValue();
                    rimY = ((Number)((List<?>)dir).get(1)).floatValue();
                    if (rimX == 0f && rimY == 0f) throw new IllegalArgumentException("contraluz: \"direcao\" não pode ser [0, 0]");
                }
            }
            light = new FighterLight(color(lightMap, "cor", 0xFFFFFFFF), clamp01(number(lightMap, "forca", 0.3f)), brilho,
                pes, rimCor, rimForca, rimX, rimY);
        }

        FighterShadow shadow = null;
        Map<String, Object> shadowMap = object(map, "sombraLutadores");
        if (shadowMap != null) {
            float alfa = number(shadowMap, "alfa", 0.4f);
            float y = number(shadowMap, "y", 0.22f);
            if (alfa <= 0f || alfa > 1f) throw new IllegalArgumentException("sombraLutadores: \"alfa\" entre 0 e 1");
            if (y < 0f || y > 1f) throw new IllegalArgumentException("sombraLutadores: \"y\" entre 0 e 1");
            List<Focus> focos = new ArrayList<>();
            Object focosField = shadowMap.get("focos");
            if (focosField instanceof List) {
                for (Object item : (List<Object>)focosField) {
                    if (!(item instanceof Map)) continue;
                    Map<String, Object> fm = (Map<String, Object>)item;
                    float altura = number(fm, "altura", 0f);
                    if (altura <= 0f) throw new IllegalArgumentException("sombraLutadores.focos: \"altura\" acima do chão (> 0)");
                    float fa = number(fm, "alfa", Float.NaN);
                    if (!Float.isNaN(fa) && (fa <= 0f || fa > 1f)) {
                        throw new IllegalArgumentException("sombraLutadores.focos: \"alfa\" entre 0 e 1");
                    }
                    focos.add(new Focus(number(fm, "x", 700f), altura, number(fm, "profundidade", 0f), fa,
                        Math.max(0f, number(fm, "alcance", 0f))));
                }
                if (focos.size() > FighterShadow.MAX_FOCOS) {
                    throw new IllegalArgumentException("sombraLutadores: no máximo " + FighterShadow.MAX_FOCOS + " focos");
                }
            }
            float suave = number(shadowMap, "suave", 0.5f);
            if (suave < 0f || suave > 1f) throw new IllegalArgumentException("sombraLutadores: \"suave\" entre 0 e 1");
            shadow = new FighterShadow(alfa, number(shadowMap, "x", 0.5f), y, focos,
                color(shadowMap, "cor", 0xFF000000), suave);
        }

        StageDefinition def = new StageDefinition(name.trim(), distance, horizon,
            color(map, "corFundo", 0xFF0B0D1E), layers, floor, petals, rain, lightning, light, shadow,
            bool(map, "chaoNaPintura", false));
        def.cameraZoom = zoomCenario;
        def.cameraZoomNoPulo = bool(camera, "zoomNoPulo", true);
        def.cameraRestZoom = number(camera, "zoomRepouso", Float.NaN);
        if (!Float.isNaN(def.cameraRestZoom) && def.cameraRestZoom <= 0f) {
            throw new IllegalArgumentException("camera: \"zoomRepouso\" deve ser positivo");
        }
        Map<String, Object> dustMap = object(map, "poeira");
        if (dustMap != null) {
            float near = number(dustMap, "perto", -250f), far = number(dustMap, "longe", 2500f);
            if (far <= near) throw new IllegalArgumentException("poeira: \"longe\" deve ser maior que \"perto\"");
            float alfa = number(dustMap, "alfa", 0.5f);
            if (alfa <= 0f || alfa > 1f) throw new IllegalArgumentException("poeira: \"alfa\" entre 0 e 1");
            float top = number(dustMap, "topo", 0.15f), bottom = number(dustMap, "base", 0.95f);
            if (top >= bottom) throw new IllegalArgumentException("poeira: \"topo\" acima da \"base\"");
            def.dust = new Dust(Math.max(0, Math.round(number(dustMap, "quantidade", 120f))),
                color(dustMap, "cor", 0xFFFFE0B0), near, far, number(dustMap, "vento", 20f),
                Math.max(0.5f, number(dustMap, "tamanho", 2f)), alfa, top, bottom);
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    private static Layer layer(Map<String, Object> map, int index) {
        Builder b = new Builder();
        String where = "camada " + index;
        b.id = string(map, "id", "camada" + index);
        where = "camada \"" + b.id + "\"";
        String kind = string(map, "tipo", "imagem");
        if ("imagem".equals(kind)) b.kind = Kind.IMAGE;
        else if ("neblina".equals(kind)) b.kind = Kind.FOG;
        else if ("agua".equals(kind)) b.kind = Kind.WATER;
        else if ("ator".equals(kind)) b.kind = Kind.ACTOR;
        else throw new IllegalArgumentException(where + ": tipo desconhecido \"" + kind + "\"");

        b.depth = number(map, "profundidade", Float.NaN);
        b.top = number(map, "topo", Float.NaN);
        b.bottom = number(map, "base", Float.NaN);
        b.onGround = bool(map, "noChao", false);
        b.height = number(map, "altura", Float.NaN);
        b.x = number(map, "x", 0.5f);
        b.period = number(map, "periodo", Float.NaN);
        b.alpha = clamp01(number(map, "alfa", 1f));
        b.color = color(map, "cor", 0xFF000000);
        b.colorBottom = color(map, "corBase", b.color);
        b.fillAbove = color(map, "corAcima", 0);
        b.reflection = clamp01(number(map, "reflexo", 0f));
        b.support = number(map, "apoio", Float.NaN);
        b.sail = number(map, "deriva", 0f);
        b.bob = number(map, "balanco", 0f);
        Object crowd = map.get("plateia");
        List<Object> crowdList = crowd instanceof List ? (List<Object>)crowd
            : crowd instanceof Map ? Collections.<Object>singletonList(crowd) : Collections.emptyList();
        for (Object item : crowdList) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> cm = (Map<String, Object>)item;
            Crowd cr = new Crowd(clamp01(number(cm, "topo", 0.3f)), clamp01(number(cm, "base", 0.9f)),
                number(cm, "pulo", 0f), Math.max(8, Math.round(number(cm, "colunas", 140f))),
                string(cm, "mascara", null), pontos(cm, "flashes", 0), pontos(cm, "flashes", 1),
                bool(cm, "recortada", false));
            if (cr.bottom <= cr.top) throw new IllegalArgumentException(where + ": plateia precisa de base > topo");
            b.crowds.add(cr);
        }

        Object falls = map.get("cachoeiras");
        List<Object> fallsList = falls instanceof List ? (List<Object>)falls
            : falls instanceof Map ? Collections.<Object>singletonList(falls) : Collections.emptyList();
        for (Object item : fallsList) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> fm = (Map<String, Object>)item;
            String fallsImage = string(fm, "imagem", null);
            Object areaList = fm.get("areas");
            if (fallsImage == null || !(areaList instanceof List)) {
                throw new IllegalArgumentException(where + ": cachoeiras precisa de \"imagem\" e \"areas\"");
            }
            List<Object> list = (List<Object>)areaList;
            float[] areas = new float[list.size() * 4];
            for (int i = 0; i < list.size(); i++) {
                Object area = list.get(i);
                if (!(area instanceof List) || ((List<Object>)area).size() != 4) {
                    throw new IllegalArgumentException(where + ": cada área das cachoeiras é [x, y, largura, altura]");
                }
                for (int k = 0; k < 4; k++) areas[i * 4 + k] = ((Number)((List<Object>)area).get(k)).floatValue();
                if (areas[i * 4 + 2] <= 0f || areas[i * 4 + 3] <= 0f) {
                    throw new IllegalArgumentException(where + ": área das cachoeiras sem tamanho");
                }
            }
            if (areas.length == 0) continue;
            Object foamList = fm.get("espuma");
            List<Object> feet = foamList instanceof List ? (List<Object>)foamList : Collections.emptyList();
            float[] foam = new float[feet.size() * 3];
            for (int i = 0; i < feet.size(); i++) {
                Object foot = feet.get(i);
                if (!(foot instanceof List) || ((List<Object>)foot).size() != 3) {
                    throw new IllegalArgumentException(where + ": cada espuma é [x, y, largura]");
                }
                for (int k = 0; k < 3; k++) foam[i * 3 + k] = ((Number)((List<Object>)foot).get(k)).floatValue();
            }
            int frames = Math.max(1, Math.round(number(fm, "quadros", 8f)));
            b.waterfalls.add(new Waterfalls(fallsImage, frames, Math.round(number(fm, "colunas", frames)),
                Math.max(0.1f, number(fm, "fps", 12f)), areas, foam, color(fm, "corEspuma", 0xFFEEF2FF)));
        }

        String repeat = string(map, "repetir", "nao");
        if ("nao".equals(repeat)) b.repeat = Repeat.NONE;
        else if ("repetir".equals(repeat)) b.repeat = Repeat.REPEAT;
        else if ("espelhar".equals(repeat)) b.repeat = Repeat.MIRROR;
        else throw new IllegalArgumentException(where + ": repetir deve ser nao, repetir ou espelhar");

        switch (b.kind) {
            case IMAGE:
                b.image = string(map, "imagem", null);
                // As arquibancadas das pontas podem compartilhar a mesma arte,
                // com uma das laterais invertida horizontalmente.
                b.flip = bool(map, "virar", false);
                if (b.image == null) throw new IllegalArgumentException(where + " precisa de \"imagem\"");
                requireDepth(b, where);
                if (b.onGround) {
                    if (!(b.height > 0f)) throw new IllegalArgumentException(where + ": noChao precisa de \"altura\"");
                } else if (Float.isNaN(b.top) || Float.isNaN(b.bottom) || b.bottom <= b.top) {
                    throw new IllegalArgumentException(where + " precisa de \"topo\" e \"base\" (base > topo) ou noChao + altura");
                }
                break;
            case ACTOR:
                b.actor = string(map, "pasta", null);
                if (b.actor == null) throw new IllegalArgumentException(where + ": ator precisa de \"pasta\"");
                b.flip = bool(map, "virar", false);
                requireDepth(b, where);
                if (!b.onGround || !(b.height > 0f)) {
                    throw new IllegalArgumentException(where + ": ator precisa de noChao + altura");
                }
                break;
            case FOG:
                requireDepth(b, where);
                if (Float.isNaN(b.top) || Float.isNaN(b.bottom) || b.bottom <= b.top) {
                    throw new IllegalArgumentException(where + ": neblina precisa de \"topo\" e \"base\"");
                }
                b.wisps = Math.max(0, Math.round(number(map, "fiapos", 0f)));
                break;
            case WATER:
                b.waterNear = number(map, "perto", Float.NaN);
                b.waterFar = number(map, "longe", Float.NaN);
                if (Float.isNaN(b.waterNear) || Float.isNaN(b.waterFar) || b.waterFar <= b.waterNear) {
                    throw new IllegalArgumentException(where + ": agua precisa de \"perto\" e \"longe\"");
                }
                b.depth = b.waterNear;
                b.color = color(map, "corLonge", 0xFF1A2550);
                b.colorBottom = color(map, "corPerto", 0xFF0A1028);
                b.shimmer = Math.max(0, Math.round(number(map, "brilhos", 40f)));
                b.image = string(map, "imagem", null);
                b.waterTile = Math.max(50f, number(map, "tamanho", 1600f));
                b.drift = number(map, "corrente", 25f);
                Object sun = map.get("sol");
                if (sun instanceof Map) {
                    Map<String, Object> sm = (Map<String, Object>)sun;
                    b.sunX = number(sm, "x", 0.5f);
                    b.sunColor = color(sm, "cor", 0xFFFFC060);
                    b.sunAlpha = clamp01(number(sm, "alfa", 0.5f));
                }
                break;
        }

        Object lights = map.get("luzes");
        if (lights instanceof List) {
            for (Object item : (List<Object>)lights) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> l = (Map<String, Object>)item;
                b.lights.add(new Light(number(l, "x", 0.5f), number(l, "y", 0.5f), number(l, "raio", 0.05f),
                    color(l, "cor", 0xFFFFB45A), number(l, "pulsar", 0.12f), bool(l, "reflexo", true),
                    pontoDeLuz(l, where), number(l, "alcance", ALCANCE_PADRAO)));
            }
        }
        Object fires = map.get("fogos");
        if (fires instanceof List) {
            for (Object item : (List<Object>)fires) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> f = (Map<String, Object>)item;
                b.fires.add(new Fire(number(f, "x", 0.5f), number(f, "y", 0.2f), number(f, "tamanho", 0.2f),
                    pontoDeLuz(f, where), number(f, "alcance", ALCANCE_PADRAO), color(f, "cor", 0xFFFF9A3C)));
            }
        }
        Object telao = map.get("telao");
        if (telao instanceof Map) {
            Map<String, Object> f = (Map<String, Object>)telao;
            float width = number(f, "largura", 0f), height = number(f, "altura", 0f);
            if (width <= 0f || height <= 0f) {
                throw new IllegalArgumentException(where + ": telao precisa de \"largura\" e \"altura\"");
            }
            b.screen = new float[] {number(f, "x", 0f), number(f, "y", 0f), width, height};
        }
        Object banners = map.get("bandeiras");
        if (banners instanceof List) {
            for (Object item : (List<Object>)banners) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> f = (Map<String, Object>)item;
                String image = string(f, "imagem", null);
                float width = number(f, "largura", 0f), height = number(f, "altura", 0f);
                if (image == null || width <= 0f || height <= 0f) {
                    throw new IllegalArgumentException(where + ": bandeira precisa de \"imagem\", \"largura\" e \"altura\"");
                }
                b.banners.add(new Banner(image, number(f, "x", 0f), number(f, "y", 0f), width, height,
                    number(f, "balanco", 0.12f), number(f, "ondas", 1.2f), number(f, "velocidade", 1f)));
            }
        }
        Layer layer = new Layer(b);
        Object curva = map.get("curva");
        if (curva instanceof List && !((List<Object>)curva).isEmpty()) {
            List<Object> pts = (List<Object>)curva;
            layer.curveU = new float[pts.size()];
            layer.curveZ = new float[pts.size()];
            for (int i = 0; i < pts.size(); i++) {
                Object pt = pts.get(i);
                if (!(pt instanceof List) || ((List<Object>)pt).size() != 2) {
                    throw new IllegalArgumentException(where + ": \"curva\" é uma lista de [u, profundidade]");
                }
                layer.curveU[i] = ((Number)((List<Object>)pt).get(0)).floatValue();
                layer.curveZ[i] = ((Number)((List<Object>)pt).get(1)).floatValue();
                if (i > 0 && layer.curveU[i] <= layer.curveU[i - 1]) {
                    throw new IllegalArgumentException(where + ": \"curva\" com u crescente");
                }
                if (layer.curveZ[i] <= 0f) throw new IllegalArgumentException(where + ": \"curva\" atrás dos lutadores (> 0)");
            }
        }
        Map<String, Object> vento = object(map, "vento");
        if (vento != null) {
            Wind w = new Wind();
            w.cols = Math.max(1, Math.round(number(vento, "colunas", 32f)));
            w.rows = Math.max(1, Math.round(number(vento, "linhas", 12f)));
            w.reach = lista(vento, "alcance");
            List<Object> pend = (List<Object>)vento.get("pendulos");
            List<Object> grupos = (List<Object>)vento.get("grupos");
            int ng = grupos == null ? 0 : grupos.size();
            w.groupX = new float[ng];
            w.groupY = new float[ng];
            for (int i = 0; i < ng; i++) {
                List<Object> pt = (List<Object>)grupos.get(i);
                w.groupX[i] = ((Number)pt.get(0)).floatValue();
                w.groupY[i] = ((Number)pt.get(1)).floatValue();
            }
            w.freq = lista(vento, "freq");
            w.amp = number(vento, "amplitude", 0.005f);
            w.damping = number(vento, "amortecimento", 0.35f);
            w.radius = number(vento, "raio", 0.03f);
            int nv = (w.cols + 1) * (w.rows + 1);
            if (pend != null) {
                w.pendulums = new Pendulum[pend.size()];
                for (int i = 0; i < w.pendulums.length; i++) {
                    Map<String, Object> m = (Map<String, Object>)pend.get(i);
                    Pendulum q = new Pendulum();
                    List<Object> pv = (List<Object>)m.get("pivo");
                    if (pv == null || pv.size() != 2) throw new IllegalArgumentException(where + ": pêndulo sem \"pivo\" [x, y]");
                    q.px = ((Number)pv.get(0)).floatValue();
                    q.py = ((Number)pv.get(1)).floatValue();
                    q.amp = (float)Math.toRadians(number(m, "graus", 3f));
                    q.freq = number(m, "freq", 0.7f);
                    q.delay = number(m, "atraso", 0f);
                    q.weight = lista(m, "peso");
                    if (q.weight == null || q.weight.length != nv) {
                        throw new IllegalArgumentException(where + ": pêndulo precisa de (colunas+1)*(linhas+1) pesos");
                    }
                    if (q.freq <= 0f) throw new IllegalArgumentException(where + ": pêndulo com freq > 0");
                    w.pendulums[i] = q;
                }
            } else {
                if (w.reach == null || w.reach.length != nv) {
                    throw new IllegalArgumentException(where + ": \"vento\" precisa de (colunas+1)*(linhas+1) valores de alcance");
                }
                if (ng == 0 || w.freq == null || w.freq.length != ng) {
                    throw new IllegalArgumentException(where + ": \"vento\" precisa de grupos e uma freq por grupo");
                }
            }
            layer.wind = w;
        }
        Map<String, Object> lateral = object(map, "lateral");
        if (lateral != null) {
            layer.sideX = number(lateral, "x", 0f);
            layer.sideFar = number(lateral, "longe", layer.depth);
            layer.sideNear = number(lateral, "perto", 60f);
            layer.sideOpen = number(lateral, "abre", 0f);
            if (layer.sideNear <= 0f || layer.sideFar <= layer.sideNear) {
                throw new IllegalArgumentException(where + ": \"lateral\" precisa de 0 < perto < longe");
            }
        }
        Map<String, Object> onda = object(map, "onda");
        if (onda != null) {
            layer.waveAmp = clamp01(number(onda, "amplitude", 1f));
            layer.waveSpeed = number(onda, "velocidade", 1f);
            layer.wavePhase = number(onda, "fase", 0f);
            layer.waveLight = clamp01(number(onda, "leve", 0.5f));
        }
        return layer;
    }

    /** Uma lista de números ({@code [1, 2.5, ...]}), ou null. */
    @SuppressWarnings("unchecked")
    private static float[] lista(Map<String, Object> map, String key) {
        Object o = map.get(key);
        if (!(o instanceof List)) return null;
        List<Object> l = (List<Object>)o;
        float[] out = new float[l.size()];
        for (int i = 0; i < out.length; i++) out[i] = ((Number)l.get(i)).floatValue();
        return out;
    }

    /** Coordenada {@code k} (0 = x, 1 = y) de uma lista de pontos [[x, y], ...]. */
    private static float[] pontos(Map<String, Object> map, String key, int k) {
        Object field = map.get(key);
        List<Object> list = field instanceof List ? (List<Object>)field : Collections.emptyList();
        float[] out = new float[list.size()];
        for (int i = 0; i < out.length; i++) {
            Object item = list.get(i);
            if (!(item instanceof List) || ((List<Object>)item).size() < 2) {
                throw new IllegalArgumentException(key + " precisa de pontos [x, y]");
            }
            out[i] = ((Number)((List<Object>)item).get(k)).floatValue();
        }
        return out;
    }

    private static void requireDepth(Builder b, String where) {
        if (Float.isNaN(b.depth)) throw new IllegalArgumentException(where + " precisa de \"profundidade\"");
    }

    // ------------------------------------------------------------ campos

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value instanceof Map ? (Map<String, Object>)value : null;
    }

    private static String string(Map<String, Object> map, String key, String fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof String ? (String)value : fallback;
    }

    /** Alcance de um ponto de luz dos lutadores sem {@code "alcance"}: 0,9 altura de tela. */
    static final float ALCANCE_PADRAO = 0.9f;

    /** {@code "lutadores"} de uma luz ou fogo: a força do ponto de luz no corpo dos lutadores (0 a 2). */
    private static float pontoDeLuz(Map<String, Object> map, String where) {
        float v = number(map, "lutadores", 0f);
        if (v < 0f || v > 2f) throw new IllegalArgumentException(where + ": \"lutadores\" entre 0 e 2");
        return v;
    }

    private static float number(Map<String, Object> map, String key, float fallback) {
        Object value = map == null ? null : map.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number)) throw new IllegalArgumentException("\"" + key + "\" deve ser um número");
        return ((Number)value).floatValue();
    }

    private static boolean bool(Map<String, Object> map, String key, boolean fallback) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Boolean ? (Boolean)value : fallback;
    }

    private static int color(Map<String, Object> map, String key, int fallback) {
        String text = string(map, key, null);
        return text == null ? fallback : parseColor(text);
    }

    /** Aceita #RRGGBB ou #AARRGGBB. */
    public static int parseColor(String text) {
        String hex = text.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6 && hex.length() != 8) throw new IllegalArgumentException("cor inválida: " + text);
        long value = Long.parseLong(hex, 16);
        if (hex.length() == 6) value |= 0xFF000000L;
        return (int)value;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
