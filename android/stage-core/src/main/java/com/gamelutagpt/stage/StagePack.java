package com.gamelutagpt.stage;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Um cenário pronto para desenhar: a definição e as imagens já carregadas. */
public final class StagePack {
    public static final String DEFINITION_FILE = "stage.json";

    public final StageDefinition definition;
    private final Map<String, RenderImage> images;
    /** Problemas não fatais (imagem que não carregou: a camada some, o resto continua). */
    public final List<String> warnings;

    public StagePack(StageDefinition definition, Map<String, RenderImage> images, List<String> warnings) {
        this.definition = definition;
        this.images = Collections.unmodifiableMap(new HashMap<>(images));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /** Imagem pelo nome usado no stage.json, ou null se não carregou. */
    public RenderImage image(String name) {
        return name == null ? null : images.get(name);
    }

    public static StagePack load(RenderAssets assets, String folder) throws IOException {
        String base = folder.endsWith("/") ? folder : folder + "/";
        StageDefinition definition = StageDefinition.fromJson(assets.readText(base + DEFINITION_FILE));

        List<String> names = new ArrayList<>();
        for (StageDefinition.Layer layer : definition.layers) {
            if (layer.image != null && !names.contains(layer.image)) names.add(layer.image);
        }
        if (definition.floor != null && !names.contains(definition.floor.image)) names.add(definition.floor.image);

        Map<String, RenderImage> images = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        for (String name : names) {
            try {
                images.put(name, assets.loadImage(base + name));
            } catch (IOException | RuntimeException ex) {
                warnings.add("não carregou " + name + " (" + ex.getMessage() + ")");
            }
        }
        return new StagePack(definition, images, warnings);
    }
}
