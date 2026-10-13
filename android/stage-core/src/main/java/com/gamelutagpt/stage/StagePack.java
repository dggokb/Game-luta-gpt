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
    /** Atores pela pasta (a do stage.json); só os que abriram. */
    private final Map<String, StageActor> actors;
    /** Problemas não fatais (imagem que não carregou: a camada some, o resto continua). */
    public final List<String> warnings;

    public StagePack(StageDefinition definition, Map<String, RenderImage> images, List<String> warnings) {
        this(definition, images, new HashMap<String, StageActor>(), warnings);
    }

    public StagePack(StageDefinition definition, Map<String, RenderImage> images, Map<String, StageActor> actors,
                     List<String> warnings) {
        this.definition = definition;
        this.images = Collections.unmodifiableMap(new HashMap<>(images));
        this.actors = Collections.unmodifiableMap(new HashMap<>(actors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /** Imagem pelo nome usado no stage.json, ou null se não carregou. */
    public RenderImage image(String name) {
        return name == null ? null : images.get(name);
    }

    /** Ator da pasta {@code folder} (como no stage.json), ou null se não abriu. */
    public StageActor actor(String folder) {
        return folder == null ? null : actors.get(folder);
    }

    /** Caminho de uma folha do ator, como as imagens do pack são guardadas. */
    public static String actorImage(String folder, String sheet) {
        return folder + "/" + sheet;
    }

    public static StagePack load(RenderAssets assets, String folder) throws IOException {
        String base = folder.endsWith("/") ? folder : folder + "/";
        StageDefinition definition = StageDefinition.fromJson(assets.readText(base + DEFINITION_FILE));

        List<String> names = new ArrayList<>();
        for (StageDefinition.Layer layer : definition.layers) {
            if (layer.image != null && !names.contains(layer.image)) names.add(layer.image);
        }
        if (definition.floor != null) {
            if (!names.contains(definition.floor.image)) names.add(definition.floor.image);
            String plain = definition.floor.plainImage;
            if (plain != null && !names.contains(plain)) names.add(plain);
        }

        Map<String, RenderImage> images = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<String, StageActor> actors = new HashMap<>();
        for (StageDefinition.Layer layer : definition.layers) {
            if (layer.kind != StageDefinition.Kind.ACTOR || actors.containsKey(layer.actor)) continue;
            try {
                StageActor actor = StageActor.fromJson(assets.readText(base + layer.actor + "/" + StageActor.DEFINITION_FILE));
                actors.put(layer.actor, actor);
                for (StageActor.Animation a : actor.animations.values()) {
                    String name = a.sheet == null ? null : actorImage(layer.actor, a.sheet);
                    if (name != null && !names.contains(name)) names.add(name);
                }
            } catch (IOException | RuntimeException ex) {
                warnings.add("ator " + layer.actor + " não abriu (" + ex.getMessage() + ")");
            }
        }
        for (String name : names) {
            try {
                images.put(name, assets.loadImage(base + name));
            } catch (IOException | RuntimeException ex) {
                warnings.add("não carregou " + name + " (" + ex.getMessage() + ")");
            }
        }
        return new StagePack(definition, images, actors, warnings);
    }
}
