package com.gamelutagpt.ultra;

import com.gamelutagpt.render.MiniJson;
import java.util.Locale;
import java.util.Map;

/**
 * Descrição de um ultra, lida do ultra.json. Exemplo:
 *
 * <pre>
 * {
 *   "nome": "EXPLOSÃO SOLAR",
 *   "cor": "#F4B73B",
 *   "corSecundaria": "#FF5A1F",
 *   "dano": 4200,
 *   "paineis": {
 *     "olhos":    { "imagem": "olhos.png" },
 *     "carga":    { "imagem": "carga.png", "onomatopeia": "VRUUUM" },
 *     "golpe":    { "imagem": "golpe.png", "som": "soco", "posicaoOnomatopeia": [0.6, 0.9] },
 *     "atingido": { "imagem": "atingido.png" },
 *     "final":    { "imagem": "final.png", "onomatopeia": "KABUUUM!" }
 *   },
 *   "raio": { "corpo": "raio_corpo.png", "ponta": "raio_ponta.png", "inicio": "raio_inicio.png",
 *             "centroInicio": [0.44, 0.48], "impacto": "raio_impacto.png", "hits": 20 }
 * }
 * </pre>
 *
 * Tudo é opcional menos o nome: painel sem imagem usa a arte provisória
 * desenhada pelo próprio motor.
 */
public final class UltraDefinition {
    public static final class Panel {
        /** Caminho relativo à pasta do ultra, ou null para usar a arte provisória. */
        public final String imagePath;
        public final String onomatopoeia;
        public final String soundId;
        /**
         * Onde a onomatopeia aparece, como fração {x, y} da caixa do painel
         * (0,0 = canto de cima à esquerda; y é a linha de base do texto), ou
         * null para a posição padrão. Serve para não cobrir o rosto na arte.
         */
        public final float[] onomatopoeiaPosition;

        Panel(String imagePath, String onomatopoeia, String soundId, float[] onomatopoeiaPosition) {
            this.imagePath = imagePath;
            this.onomatopoeia = onomatopoeia;
            this.soundId = soundId;
            this.onomatopoeiaPosition = onomatopoeiaPosition;
        }
    }

    /**
     * O raio final, disparado quando a luta volta. Imagens com transparência (veja
     * tools/ultra/preparar_raio.py); sem imagem, o motor desenha um raio com as cores do ultra.
     */
    public static final class Beam {
        /** Feixe horizontal que se repete; centrado no núcleo. */
        public final String bodyPath;
        /** Frente do feixe (o feixe vai para a direita). */
        public final String tipPath;
        /** Esfera nas mãos; {@link #startCenter} é o centro dela na imagem (frações). */
        public final String startPath;
        public final float[] startCenter;
        /** Explosão no alvo. */
        public final String impactPath;
        /** Acertos pequenos antes da explosão final. */
        public final int hits;
        /** Altura do feixe no mundo do jogo, em pixels (a imagem do corpo inteira). */
        public final float thickness;
        /** Folhas animadas (quadros lado a lado): aura atrás do corpo, rajadas de vento, poeira. */
        public final Sheet aura, wind, dust;
        /**
         * Onde ficam as mãos na pose de carga e na de disparo: {à frente, acima} da raiz do
         * lutador, em pixels do mundo. Null quando o sprite não tem pose própria.
         */
        public final float[] chargeHands, fireHands;

        public static final int DEFAULT_HITS = 20;
        public static final float DEFAULT_THICKNESS = 150f;
        static final Beam DEFAULT = new Beam(null, null, null, null, null, DEFAULT_HITS, DEFAULT_THICKNESS,
            null, null, null, null, null);

        Beam(String bodyPath, String tipPath, String startPath, float[] startCenter, String impactPath,
             int hits, float thickness, Sheet aura, Sheet wind, Sheet dust, float[] chargeHands, float[] fireHands) {
            if (hits < 1 || hits > 60) throw new IllegalArgumentException("\"hits\" do raio deve ser de 1 a 60");
            if (thickness <= 0f) throw new IllegalArgumentException("\"espessura\" do raio deve ser positiva");
            this.bodyPath = bodyPath;
            this.tipPath = tipPath;
            this.startPath = startPath;
            this.startCenter = startCenter != null ? startCenter : new float[]{0.5f, 0.5f};
            this.impactPath = impactPath;
            this.hits = hits;
            this.thickness = thickness;
            this.aura = aura;
            this.wind = wind;
            this.dust = dust;
            this.chargeHands = chargeHands;
            this.fireHands = fireHands;
        }
    }

    /** Uma folha de animação: {@code frames} quadros do mesmo tamanho, lado a lado. */
    public static final class Sheet {
        public final String path;
        public final int frames;

        Sheet(String path, int frames) {
            if (frames < 1) throw new IllegalArgumentException("\"quadros\" deve ser pelo menos 1");
            this.path = path;
            this.frames = frames;
        }
    }

    public final String name;
    public final int color;
    public final int accentColor;
    /** Dano total do ultra antes da escala de combo, dividido entre os acertos da cinemática. */
    public final int damage;
    private final Panel[] panels;
    public final Beam beam;

    public static final int DEFAULT_DAMAGE = 4000;

    public UltraDefinition(String name, int color, int accentColor, int damage, Panel[] panels) {
        this(name, color, accentColor, damage, panels, Beam.DEFAULT);
    }

    public UltraDefinition(String name, int color, int accentColor, int damage, Panel[] panels, Beam beam) {
        if (panels.length != UltraSlot.values().length) {
            throw new IllegalArgumentException("um ultra tem exatamente 5 painéis");
        }
        this.name = name;
        this.color = color;
        this.accentColor = accentColor;
        if (damage < 0) throw new IllegalArgumentException("o dano do ultra não pode ser negativo");
        this.damage = damage;
        this.panels = panels.clone();
        this.beam = beam != null ? beam : Beam.DEFAULT;
    }

    public Panel panel(UltraSlot slot) {
        return panels[slot.ordinal()];
    }

    /** Ultra só com arte provisória, útil antes de existir qualquer imagem. */
    public static UltraDefinition placeholder(String name, int color) {
        Panel[] panels = new Panel[UltraSlot.values().length];
        for (UltraSlot slot : UltraSlot.values()) {
            panels[slot.ordinal()] = new Panel(null, slot.defaultOnomatopoeia, slot.defaultSound, null);
        }
        return new UltraDefinition(name, color, brighten(color), DEFAULT_DAMAGE, panels);
    }

    @SuppressWarnings("unchecked")
    public static UltraDefinition fromJson(String json) {
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map)) throw new IllegalArgumentException("ultra.json deve ser um objeto");
        Map<String, Object> map = (Map<String, Object>)root;

        String name = stringField(map, "nome", null);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("ultra.json precisa do campo \"nome\"");
        }
        int color = parseColor(stringField(map, "cor", "#F4B73B"));
        String accent = stringField(map, "corSecundaria", null);
        int accentColor = accent != null ? parseColor(accent) : brighten(color);
        Object damageField = map.get("dano");
        if (damageField != null && !(damageField instanceof Number)) {
            throw new IllegalArgumentException("\"dano\" deve ser um número");
        }
        int damage = damageField != null ? ((Number)damageField).intValue() : DEFAULT_DAMAGE;

        Object panelsField = map.get("paineis");
        Map<String, Object> panelsMap = panelsField instanceof Map
            ? (Map<String, Object>)panelsField
            : null;

        Panel[] panels = new Panel[UltraSlot.values().length];
        for (UltraSlot slot : UltraSlot.values()) {
            Map<String, Object> entry = null;
            if (panelsMap != null && panelsMap.get(slot.key) instanceof Map) {
                entry = (Map<String, Object>)panelsMap.get(slot.key);
            }
            String image = entry != null ? stringField(entry, "imagem", null) : null;
            String sfx = entry != null
                ? stringField(entry, "onomatopeia", slot.defaultOnomatopoeia)
                : slot.defaultOnomatopoeia;
            String sound = entry != null
                ? stringField(entry, "som", slot.defaultSound)
                : slot.defaultSound;
            float[] position = entry != null ? positionField(entry, "posicaoOnomatopeia") : null;
            panels[slot.ordinal()] = new Panel(image, sfx, sound, position);
        }
        Beam beam = Beam.DEFAULT;
        if (map.get("raio") instanceof Map) {
            Map<String, Object> raio = (Map<String, Object>)map.get("raio");
            beam = new Beam(
                stringField(raio, "corpo", null),
                stringField(raio, "ponta", null),
                stringField(raio, "inicio", null),
                positionField(raio, "centroInicio"),
                stringField(raio, "impacto", null),
                numberField(raio, "hits", Beam.DEFAULT_HITS).intValue(),
                numberField(raio, "espessura", Beam.DEFAULT_THICKNESS).floatValue(),
                sheetField(raio, "aura"),
                sheetField(raio, "vento"),
                sheetField(raio, "poeira"),
                handsField(raio, "carga"),
                handsField(raio, "disparo")
            );
        }
        return new UltraDefinition(name.trim().toUpperCase(Locale.ROOT), color, accentColor, damage, panels, beam);
    }

    @SuppressWarnings("unchecked")
    private static Sheet sheetField(Map<String, Object> raio, String key) {
        Object value = raio.get(key);
        if (value == null) return null;
        if (!(value instanceof Map)) throw new IllegalArgumentException("\"" + key + "\" deve ser {\"imagem\", \"quadros\"}");
        Map<String, Object> map = (Map<String, Object>)value;
        String path = stringField(map, "imagem", null);
        if (path == null) throw new IllegalArgumentException("\"" + key + "\" precisa de \"imagem\"");
        return new Sheet(path, numberField(map, "quadros", 1).intValue());
    }

    @SuppressWarnings("unchecked")
    private static float[] handsField(Map<String, Object> raio, String key) {
        if (!(raio.get("maos") instanceof Map)) return null;
        Object value = ((Map<String, Object>)raio.get("maos")).get(key);
        if (value == null) return null;
        if (!(value instanceof java.util.List) || ((java.util.List<?>)value).size() != 2
            || !(((java.util.List<?>)value).get(0) instanceof Number) || !(((java.util.List<?>)value).get(1) instanceof Number)) {
            throw new IllegalArgumentException("\"maos." + key + "\" deve ser [à frente, acima] em pixels");
        }
        java.util.List<?> list = (java.util.List<?>)value;
        return new float[]{((Number)list.get(0)).floatValue(), ((Number)list.get(1)).floatValue()};
    }

    private static Number numberField(Map<String, Object> map, String key, Number fallback) {
        Object value = map.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number)) throw new IllegalArgumentException("\"" + key + "\" deve ser um número");
        return (Number)value;
    }

    private static float[] positionField(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (!(value instanceof java.util.List) || ((java.util.List<?>)value).size() != 2) {
            throw new IllegalArgumentException("\"" + key + "\" deve ser [x, y] com frações de 0 a 1");
        }
        java.util.List<?> list = (java.util.List<?>)value;
        float[] position = new float[2];
        for (int i = 0; i < 2; i++) {
            if (!(list.get(i) instanceof Number)) {
                throw new IllegalArgumentException("\"" + key + "\" deve ter dois números");
            }
            position[i] = Math.max(0f, Math.min(1f, ((Number)list.get(i)).floatValue()));
        }
        return position;
    }

    private static String stringField(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value instanceof String ? (String)value : fallback;
    }

    /** Aceita #RRGGBB ou #AARRGGBB. */
    public static int parseColor(String text) {
        String hex = text.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6 && hex.length() != 8) {
            throw new IllegalArgumentException("cor inválida: " + text);
        }
        long value = Long.parseLong(hex, 16);
        if (hex.length() == 6) value |= 0xFF000000L;
        return (int)value;
    }

    static int brighten(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r += (255 - r) / 2;
        g += (255 - g) / 2;
        b += (255 - b) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
