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
    /** Imagens do raio final (cada uma pode ser null: o motor desenha no lugar). */
    public final RenderImage beamBody;
    public final RenderImage beamTip;
    public final RenderImage beamStart;
    public final RenderImage beamImpact;
    /** Problemas não fatais encontrados ao carregar (imagem faltando etc.). */
    public final List<String> warnings;

    public UltraPack(UltraDefinition definition, RenderImage[] images, List<String> warnings) {
        this(definition, images, new RenderImage[4], warnings);
    }

    /** @param beamImages corpo, ponta, início e impacto do raio, nessa ordem */
    public UltraPack(UltraDefinition definition, RenderImage[] images, RenderImage[] beamImages,
                     List<String> warnings) {
        this.definition = definition;
        this.images = images.clone();
        beamBody = beamImages[0];
        beamTip = beamImages[1];
        beamStart = beamImages[2];
        beamImpact = beamImages[3];
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
        UltraDefinition.Beam beam = definition.beam;
        String[] beamPaths = {beam.bodyPath, beam.tipPath, beam.startPath, beam.impactPath};
        RenderImage[] beamImages = new RenderImage[beamPaths.length];
        for (int i = 0; i < beamPaths.length; i++) {
            if (beamPaths[i] == null) continue;
            try {
                beamImages[i] = assets.loadImage(base + beamPaths[i]);
            } catch (IOException | RuntimeException ex) {
                warnings.add("raio: não carregou " + beamPaths[i] + " (" + ex.getMessage() + ")");
            }
        }
        return new UltraPack(definition, images, beamImages, warnings);
    }
}
