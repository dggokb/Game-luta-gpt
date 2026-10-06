package com.gamelutagpt.ultra;

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
 *   "paineis": {
 *     "olhos":    { "imagem": "olhos.png" },
 *     "carga":    { "imagem": "carga.png", "onomatopeia": "VRUUUM" },
 *     "golpe":    { "imagem": "golpe.png", "som": "soco", "posicaoOnomatopeia": [0.6, 0.9] },
 *     "atingido": { "imagem": "atingido.png" },
 *     "final":    { "imagem": "final.png", "onomatopeia": "KABUUUM!" }
 *   }
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

    public final String name;
    public final int color;
    public final int accentColor;
    private final Panel[] panels;

    public UltraDefinition(String name, int color, int accentColor, Panel[] panels) {
        if (panels.length != UltraSlot.values().length) {
            throw new IllegalArgumentException("um ultra tem exatamente 5 painéis");
        }
        this.name = name;
        this.color = color;
        this.accentColor = accentColor;
        this.panels = panels.clone();
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
        return new UltraDefinition(name, color, brighten(color), panels);
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
        return new UltraDefinition(name.trim().toUpperCase(Locale.ROOT), color, accentColor, panels);
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
