#!/usr/bin/env python3
"""Gera os sons de luta (golpe no ar, acerto, defesa, queda, KO) por síntese.

Só a biblioteca padrão: ruído filtrado, senos com o tom caindo e saturação. Nada é tirado
de outro jogo. Saída: WAV mono 16 bits em android/app/src/main/assets/sons/luta/, que o
GameView toca pelo SoundPool. Rodar de novo regera tudo igual (semente fixa):

    python3 tools/sons/gerar_sons.py
"""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parents[2] / 'android/app/src/main/assets/sons/luta'


def silence(seconds):
    return [0.0] * int(RATE * seconds)


def mix(*layers):
    n = max(len(layer) for layer in layers)
    out = [0.0] * n
    for layer in layers:
        for i, v in enumerate(layer):
            out[i] += v
    return out


def envelope(samples, attack, decay, curve=3.0):
    """Ataque linear curto e queda exponencial até o fim."""
    n = len(samples)
    a = max(1, int(RATE * attack))
    d = max(1, int(RATE * decay))
    out = []
    for i, v in enumerate(samples):
        if i < a:
            g = i / a
        else:
            g = math.exp(-curve * (i - a) / d)
        # Sem clique no fim.
        g *= min(1.0, (n - i) / (RATE * 0.004))
        out.append(v * g)
    return out


def noise(seconds, rng):
    return [rng.uniform(-1.0, 1.0) for _ in range(int(RATE * seconds))]


def lowpass(samples, cutoff):
    """Passa-baixa de um polo; cutoff pode ser um número ou uma função do tempo (s)."""
    out, y = [], 0.0
    for i, v in enumerate(samples):
        c = cutoff(i / RATE) if callable(cutoff) else cutoff
        k = 1.0 - math.exp(-2.0 * math.pi * c / RATE)
        y += k * (v - y)
        out.append(y)
    return out


def highpass(samples, cutoff):
    low = lowpass(samples, cutoff)
    return [v - l for v, l in zip(samples, low)]


def thump(seconds, start_hz, end_hz, drop=18.0):
    """Seno com o tom caindo rápido: o peso do golpe no corpo."""
    out, phase = [], 0.0
    for i in range(int(RATE * seconds)):
        t = i / RATE
        hz = end_hz + (start_hz - end_hz) * math.exp(-drop * t)
        phase += 2.0 * math.pi * hz / RATE
        out.append(math.sin(phase))
    return out


def tone(seconds, hz, vibrato=0.0):
    out, phase = [], 0.0
    for i in range(int(RATE * seconds)):
        t = i / RATE
        phase += 2.0 * math.pi * hz * (1.0 + vibrato * math.sin(2 * math.pi * 7 * t)) / RATE
        out.append(math.sin(phase))
    return out


def gain(samples, g):
    return [v * g for v in samples]


def saturate(samples, drive):
    norm = math.tanh(drive)
    return [math.tanh(v * drive) / norm for v in samples]


def normalize(samples, peak=0.89):
    top = max(abs(v) for v in samples) or 1.0
    return [v * peak / top for v in samples]


def whoosh(seconds, low, high, rng):
    """Golpe cortando o ar: ruído com o filtro abrindo e fechando."""
    def sweep(t):
        k = math.sin(math.pi * min(1.0, t / seconds))
        return low + (high - low) * k
    body = highpass(lowpass(noise(seconds, rng), sweep), low * 0.5)
    shaped = []
    for i, v in enumerate(body):
        t = i / (RATE * seconds)
        shaped.append(v * math.sin(math.pi * t) ** 1.6)
    return shaped


def hit(rng, weight):
    """Acerto: estalo (ruído agudo curto), soco (seno caindo) e cauda grave."""
    crack = envelope(highpass(noise(0.05, rng), 1800), 0.0008, 0.018 + 0.01 * weight)
    body = envelope(lowpass(noise(0.10 + 0.08 * weight, rng), 2600 - 500 * weight),
                    0.001, 0.04 + 0.05 * weight)
    punch = envelope(thump(0.10 + 0.12 * weight, 230 - 50 * weight, 85 - 25 * weight),
                     0.001, 0.05 + 0.09 * weight, 2.6)
    tail = envelope(lowpass(noise(0.15 + 0.25 * weight, rng), 700), 0.004, 0.06 + 0.16 * weight)
    sound = mix(gain(crack, 0.55), gain(body, 0.65), gain(punch, 0.9 + 0.2 * weight), gain(tail, 0.35 * weight))
    return normalize(saturate(sound, 1.6 + 0.9 * weight))


def build():
    rng = random.Random(20261016)
    sounds = {}
    sounds['ar_l'] = normalize(whoosh(0.11, 900, 5200, rng), 0.55)
    sounds['ar_m'] = normalize(whoosh(0.15, 700, 4200, rng), 0.62)
    sounds['ar_h'] = normalize(whoosh(0.22, 450, 3200, rng), 0.70)
    sounds['acerto_l'] = hit(rng, 0.0)
    sounds['acerto_m'] = hit(rng, 0.5)
    sounds['acerto_h'] = hit(rng, 1.0)
    # Especial/super: o pesado com um brilho de energia por cima.
    sparkle = envelope(highpass(noise(0.35, rng), 4200), 0.002, 0.16)
    sounds['acerto_s'] = normalize(mix(hit(rng, 1.15), gain(sparkle, 0.35)))
    # Defesa: um "toc" oco e seco, sem peso.
    knock = mix(gain(thump(0.12, 1250, 820, 30), 0.7), gain(thump(0.12, 1900, 1400, 40), 0.35))
    sounds['defesa'] = normalize(mix(envelope(knock, 0.0006, 0.035, 3.2),
                                     gain(envelope(highpass(noise(0.03, rng), 3000), 0.0005, 0.01), 0.5)), 0.7)
    # Queda no chão: baque grave e um pouco de poeira.
    sounds['queda'] = normalize(saturate(mix(
        envelope(thump(0.32, 120, 42, 10), 0.002, 0.16, 2.4),
        gain(envelope(lowpass(noise(0.30, rng), 900), 0.003, 0.12), 0.5)), 1.8), 0.85)
    # Punish counter: o golpe pesado com um "tim" agudo de contra-ataque.
    ding = mix(envelope(tone(0.45, 2350, 0.004), 0.001, 0.22, 2.0), gain(envelope(tone(0.45, 3520), 0.001, 0.12), 0.4))
    sounds['punish'] = normalize(mix(hit(rng, 1.1), gain(ding, 0.45)))
    # KO: explosão grave longa.
    boom = mix(
        envelope(thump(1.1, 95, 28, 4), 0.003, 0.55, 2.2),
        gain(envelope(lowpass(noise(1.3, rng), lambda t: 2400 * math.exp(-3 * t) + 120), 0.002, 0.6, 2.0), 0.8),
        gain(envelope(highpass(noise(0.06, rng), 1500), 0.0005, 0.02), 0.6))
    sounds['ko'] = normalize(saturate(boom, 2.4))
    # Ativação do Super: um "shing" metálico (parciais desafinados que tocam juntos), o ar
    # subindo atrás e um baque grave curto. Avisa antes de a tela parar.
    ring = mix(*(gain(envelope(tone(0.7, hz, 0.003), 0.001, decay, 2.2), g)
                 for hz, decay, g in ((1760, 0.42, 0.9), (2637, 0.34, 0.6), (3951, 0.24, 0.45), (5274, 0.16, 0.3))))
    rise = envelope(highpass(lowpass(noise(0.32, rng), lambda t: 900 + 7000 * t / 0.32), 500), 0.22, 0.06, 4.0)
    hum = envelope(thump(0.3, 160, 70, 9), 0.002, 0.14, 2.4)
    sounds['super'] = normalize(mix(gain(ring, 0.8), gain(rise, 0.45), gain(hum, 0.55)), 0.85)
    # Guard break: a defesa estilhaça. O golpe pesado, um estalo seco de rachadura e cacos de
    # vidro (tons agudos curtos espalhados) caindo depois. Sempre no fim: a mesma semente
    # continua dando os mesmos sons de antes.
    crack = envelope(highpass(noise(0.08, rng), 2500), 0.0004, 0.03)
    shards = silence(0.5)
    for _ in range(14):
        start = int(RATE * rng.uniform(0.02, 0.32))
        hz = rng.uniform(2800, 6800)
        piece = envelope(tone(0.09, hz), 0.0005, rng.uniform(0.02, 0.06), 2.5)
        for i, v in enumerate(piece):
            if start + i < len(shards):
                shards[start + i] += v * rng.uniform(0.25, 0.5)
    sounds['guarda_quebrada'] = normalize(saturate(mix(hit(rng, 1.2), gain(crack, 0.8), gain(shards, 0.6)), 1.4))
    return sounds


def write(path, samples):
    with wave.open(str(path), 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1.0, min(1.0, v)) * 32767)) for v in samples))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, samples in build().items():
        write(OUT / f'{name}.wav', samples)
        print(f'{name}.wav  {len(samples) / RATE:.2f}s')


if __name__ == '__main__':
    main()
