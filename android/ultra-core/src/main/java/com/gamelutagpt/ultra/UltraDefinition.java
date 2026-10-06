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
 *     "golpe":    { "imagem": "golpe.png", "som": "soco" },
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

        Panel(String imagePath, String onomatopoeia, String soundId) {
            this.imagePath = imagePath;
            this.onomatopoeia = onomatopoeia;
            this.soundId = soundId;
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
            panels[slot.ordinal()] = new Panel(null, slot.defaultOnomatopoeia, slot.defaultSound);
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
            panels[slot.ordinal()] = new Panel(image, sfx, sound);
        }
        return new UltraDefinition(name.trim().toUpperCase(Locale.ROOT), color, accentColor, panels);
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
