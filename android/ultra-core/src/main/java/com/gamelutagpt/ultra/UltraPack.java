package com.gamelutagpt.ultra;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Um ultra pronto para tocar: a definição e as imagens já carregadas. */
public final class UltraPack {
    public static final String DEFINITION_FILE = "ultra.json";

    public final UltraDefinition definition;
    private final RenderImage[] images;
    /** Problemas não fatais encontrados ao carregar (imagem faltando etc.). */
    public final List<String> warnings;

    public UltraPack(UltraDefinition definition, RenderImage[] images, List<String> warnings) {
        this.definition = definition;
        this.images = images.clone();
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /** Imagem do painel, ou null quando ele usa a arte provisória. */
    public RenderImage image(UltraSlot slot) {
        return images[slot.ordinal()];
    }

    public static UltraPack placeholder(String name, int color) {
        return new UltraPack(
            UltraDefinition.placeholder(name, color),
            new RenderImage[UltraSlot.values().length],
            Collections.<String>emptyList()
        );
    }

    /**
     * Lê {@code pasta/ultra.json} e as imagens dos painéis. Imagem que não
     * carrega vira aviso e o painel cai na arte provisória.
     */
    public static UltraPack load(RenderAssets assets, String folder) throws IOException {
        String base = folder.endsWith("/") ? folder : folder + "/";
        UltraDefinition definition = UltraDefinition.fromJson(assets.readText(base + DEFINITION_FILE));

        List<String> warnings = new ArrayList<>();
        RenderImage[] images = new RenderImage[UltraSlot.values().length];
        for (UltraSlot slot : UltraSlot.values()) {
            String path = definition.panel(slot).imagePath;
            if (path == null) continue;
            try {
                images[slot.ordinal()] = assets.loadImage(base + path);
            } catch (IOException | RuntimeException ex) {
                warnings.add("painel " + slot.key + ": não carregou " + path + " (" + ex.getMessage() + ")");
            }
        }
        return new UltraPack(definition, images, warnings);
    }
}
