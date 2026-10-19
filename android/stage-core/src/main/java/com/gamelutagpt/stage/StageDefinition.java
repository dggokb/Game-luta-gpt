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
 *       "luzes": [ { "x": 0.6, "y": 0.3, "raio": 0.06, "cor": "#FFB45A", "reflexo": true } ],
 *       "bandeiras": [ { "imagem": "bandeira.png", "x": 0.3, "y": 0.29, "largura": 0.31, "altura": 0.53 } ] },
 *     { "tipo": "neblina", "profundidade": 2600, "topo": 0.45, "base": 0.62, "cor": "#3A2A66", "alfa": 0.5 }
 *   ],
 *   "chao": { "imagem": "chao.jpg", "imagemLajotas": "chao_lajotas.jpg", "perto": -420, "longe": 560,
 *             "centroX": 700, "reflexoLutadores": 0.25 },
 *   "petalas": { "quantidade": 60, "cor": "#FFB7D5" },
 *   "chuva": { "quantidade": 260, "cor": "#B8CCFF", "velocidade": 1500, "vento": -140 },
 *   "relampagos": { "intervaloMinimo": 5, "intervaloMaximo": 12, "profundidade": 9000 }
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

        Light(float x, float y, float radius, int color, float pulse, boolean reflect) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.pulse = pulse;
            this.reflect = reflect;
        }
    }

    /** Fogo animado preso num ponto da imagem; tamanho em fração da altura da imagem. */
    public static final class Fire {
        public final float x, y, size;

        Fire(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
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
        /** Água: faixa de profundidades e quantidade de brilhos. */
        public final float waterNear, waterFar;
        public final int shimmer;
        public final List<Light> lights;
        public final List<Fire> fires;
        public final List<Banner> banners;
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
            waterNear = b.waterNear;
            waterFar = b.waterFar;
            shimmer = b.shimmer;
            lights = Collections.unmodifiableList(new ArrayList<>(b.lights));
            fires = Collections.unmodifiableList(new ArrayList<>(b.fires));
            banners = Collections.unmodifiableList(new ArrayList<>(b.banners));
            actor = b.actor;
            flip = b.flip;
        }

        /** Profundidade usada para ordenar (a água usa a borda de trás). */
        public float sortDepth() {
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
        float waterNear = Float.NaN, waterFar = Float.NaN;
        int shimmer;
        final List<Light> lights = new ArrayList<>();
        final List<Fire> fires = new ArrayList<>();
        final List<Banner> banners = new ArrayList<>();
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

        Floor(String image, String plainImage, float near, float far, float centerX,
              float fighterReflection, float lightReflection) {
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
    /** Altura do horizonte na tela (fração). */
    public final float horizon;
    public final int backgroundColor;
    public final List<Layer> layers;
    public final Floor floor;
    /** Null quando o cenário não tem pétalas. */
    public final Petals petals;
    /** Null quando o cenário não tem chuva. */
    public final Rain rain;
    /** Null quando o cenário não tem relâmpagos. */
    public final Lightning lightning;

    private StageDefinition(String name, float cameraDistance, float horizon, int backgroundColor,
                            List<Layer> layers, Floor floor, Petals petals, Rain rain, Lightning lightning) {
        this.name = name;
        this.cameraDistance = cameraDistance;
        this.horizon = horizon;
        this.backgroundColor = backgroundColor;
        this.layers = Collections.unmodifiableList(layers);
        this.floor = floor;
        this.petals = petals;
        this.rain = rain;
        this.lightning = lightning;
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
                number(floorMap, "reflexoLutadores", 0f), number(floorMap, "reflexoLuzes", 0.35f));
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

        return new StageDefinition(name.trim(), distance, horizon,
            color(map, "corFundo", 0xFF0B0D1E), layers, floor, petals, rain, lightning);
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

        String repeat = string(map, "repetir", "nao");
        if ("nao".equals(repeat)) b.repeat = Repeat.NONE;
        else if ("repetir".equals(repeat)) b.repeat = Repeat.REPEAT;
        else if ("espelhar".equals(repeat)) b.repeat = Repeat.MIRROR;
        else throw new IllegalArgumentException(where + ": repetir deve ser nao, repetir ou espelhar");

        switch (b.kind) {
            case IMAGE:
                b.image = string(map, "imagem", null);
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
                break;
        }

        Object lights = map.get("luzes");
        if (lights instanceof List) {
            for (Object item : (List<Object>)lights) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> l = (Map<String, Object>)item;
                b.lights.add(new Light(number(l, "x", 0.5f), number(l, "y", 0.5f), number(l, "raio", 0.05f),
                    color(l, "cor", 0xFFFFB45A), number(l, "pulsar", 0.12f), bool(l, "reflexo", true)));
            }
        }
        Object fires = map.get("fogos");
        if (fires instanceof List) {
            for (Object item : (List<Object>)fires) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> f = (Map<String, Object>)item;
                b.fires.add(new Fire(number(f, "x", 0.5f), number(f, "y", 0.2f), number(f, "tamanho", 0.2f)));
            }
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
        return new Layer(b);
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
