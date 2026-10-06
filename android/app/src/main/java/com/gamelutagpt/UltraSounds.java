package com.gamelutagpt;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sons dos ultras. Procura primeiro em {@code <pasta do ultra>/sons/} e
 * depois na pasta compartilhada {@code ultras/sons/}. O nome do arquivo
 * sem extensão é o id do som (ex.: impacto.ogg = "impacto"). Som que não
 * existe é simplesmente ignorado.
 */
final class UltraSounds {
    static final String SHARED_FOLDER = "ultras/sons";
    private static final String TAG = "UltraSounds";

    private final AssetManager assets;
    private final SoundPool pool;
    private final Map<String, Integer> loaded = new HashMap<>();

    UltraSounds(AssetManager assets) {
        this.assets = assets;
        pool = new SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .build();
        loadFolder(SHARED_FOLDER);
    }

    /** Carrega a pasta de sons de um ultra (ex.: "ultras/player1"). */
    void loadUltra(String ultraFolder) {
        loadFolder(ultraFolder + "/sons");
    }

    void play(String ultraFolder, String soundId) {
        if (soundId == null || soundId.isEmpty()) return;
        Integer sound = loaded.get(ultraFolder + "/sons/" + soundId);
        if (sound == null) sound = loaded.get(SHARED_FOLDER + "/" + soundId);
        if (sound != null) pool.play(sound, 1f, 1f, 1, 0, 1f);
    }

    void release() {
        pool.release();
        loaded.clear();
    }

    private void loadFolder(String folder) {
        String[] files;
        try {
            files = assets.list(folder);
        } catch (IOException ex) {
            return;
        }
        if (files == null) return;
        for (String file : files) {
            int dot = file.lastIndexOf('.');
            if (dot <= 0) continue;
            String extension = file.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
            if (!extension.equals("ogg") && !extension.equals("wav") && !extension.equals("mp3")) continue;
            try (AssetFileDescriptor descriptor = assets.openFd(folder + "/" + file)) {
                loaded.put(folder + "/" + file.substring(0, dot), pool.load(descriptor, 1));
            } catch (IOException ex) {
                Log.w(TAG, "não carregou " + folder + "/" + file, ex);
            }
        }
    }
}
