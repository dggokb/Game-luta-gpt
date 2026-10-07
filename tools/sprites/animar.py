#!/usr/bin/env python3
"""Anima um desenho mestre a partir de poses-chave do ChatGPT, sem redesenhar nada.

Todo quadro é o próprio desenho mestre deformado. Das poses-chave só se usa o
movimento (fluxo óptico), nunca os pixels: por isso não há fantasma nem tremido de
traço. Cabeça, punhos e antebraços andam como peças rígidas; as pernas dobram junto
com o quadril; do joelho para cima o corpo sobe e desce como um bloco; os pés ficam
presos no chão.

    pip install -r tools/sprites/requirements-animar.txt
    python3 tools/sprites/animar.py tools/sprites/anim/p01_idle.json

Gera os quadros PNG em `saida` e uma prévia GIF em `previa`. Coordenadas da
configuração em pixels da imagem mestre; elipses são [cx, cy, rx, ry].
"""
import argparse
import json
import os

import cv2
import numpy as np
from PIL import Image


def load(path):
    return np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)


def gray(img):
    alpha = img[..., 3:] / 255
    rgb = img[..., :3] * alpha + 128 * (1 - alpha)
    return cv2.cvtColor(rgb.astype(np.uint8), cv2.COLOR_RGB2GRAY)


def smooth(a):
    a = np.clip(a, 0, 1)
    return a * a * (3 - 2 * a)


def mean(flow, box):
    x0, y0, x1, y1 = box
    return flow[y0:y1, x0:x1].reshape(-1, 2).mean(0)


class Rig:
    def __init__(self, cfg, master):
        self.cfg = cfg
        self.h, self.w = master.shape[:2]
        self.yy, self.xx = np.mgrid[0:self.h, 0:self.w].astype(np.float32)
        head = self.blob(cfg["cabeca"]["formas"], 6)
        self.parts = [("cabeca", head, cfg["cabeca"]["amostra"])]
        for part in cfg.get("rigidas", []):
            mask = self.blob(part["formas"], 8) * (1 - head)
            self.parts.append((part["nome"], mask, part["amostra"]))
        legs = cfg["pernas"]
        self.legs = smooth((self.yy - legs["cintura_y"]) / 140)
        self.leg_ramp = smooth((legs["tornozelo_y"] - self.yy) / (legs["tornozelo_y"] - 700))
        self.above_knee = smooth((legs["joelho_y"] - self.yy) / 170)
        self.torso = np.clip(1 - np.maximum.reduce([m for _, m, _ in self.parts] + [self.legs]), 0, 1)

    def ellipse(self, cx, cy, rx, ry, p):
        d = ((self.xx - cx) / rx) ** 2 + ((self.yy - cy) / ry) ** 2
        return np.exp(-(d ** (p / 2)))

    def blob(self, shapes, blur):
        mask = np.maximum.reduce([self.ellipse(*s, 8) for s in shapes])
        return cv2.GaussianBlur(mask, (0, 0), blur)

    def flow(self, master, key):
        """Campo que leva cada pixel do quadro-chave à posição no mestre."""
        gm = gray(master)
        floor = np.zeros((self.h, self.w), np.float32)
        floor[int(self.cfg["pernas"]["tornozelo_y"]) - 190:] = 1
        (dx, dy), _ = cv2.phaseCorrelate(gm.astype(np.float32) * floor, gray(key).astype(np.float32) * floor)
        key = cv2.warpAffine(key, np.float32([[1, 0, -dx], [0, 1, -dy]]), (self.w, self.h), borderValue=(0, 0, 0, 0))
        s = 0.5
        a = cv2.resize(gray(key), None, fx=s, fy=s)
        b = cv2.resize(gm, None, fx=s, fy=s)
        f = cv2.calcOpticalFlowFarneback(a, b, None, 0.5, 6, 41, 5, 7, 1.5, 0)
        f = cv2.GaussianBlur(cv2.resize(f, (self.w, self.h)) / s, (0, 0), 10)
        legs = self.cfg["pernas"]
        neck = mean(f, self.cfg["cabeca"]["amostra"])
        out = f * self.torso[..., None]
        for _, mask, box in self.parts:
            out += mean(f, box) * mask[..., None]
        waist = np.array([mean(f, legs["amostra_cintura"])[0], legs["dobra"] * neck[1]], np.float32)
        bob = np.array([0, legs["balanco"] * neck[1]], np.float32)
        out += waist * (self.legs * self.leg_ramp)[..., None] + bob * self.above_knee[..., None]
        return out.astype(np.float32), (dx, dy)

    def warp(self, master, f, t):
        return cv2.remap(master, self.xx + f[..., 0] * t, self.yy + f[..., 1] * t, cv2.INTER_CUBIC,
                         borderValue=(0, 0, 0, 0))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("config")
    cfg_path = parser.parse_args().config
    with open(cfg_path, encoding="utf-8") as fh:
        cfg = json.load(fh)
    master = load(cfg["mestre"])
    rig = Rig(cfg, master)
    steps = cfg.get("passos", [0, 0.25, 0.5, 0.75, 1])
    ease = lambda t: t * t * (3 - 2 * t)
    seq = []
    for key_path in cfg["chaves"]:
        f, shift = rig.flow(master, load(key_path))
        print(f"{key_path}: alinhado {shift[0]:.1f},{shift[1]:.1f}")
        go = [ease(t) for t in steps]
        seq += [(f, t) for t in go] + [(f, t) for t in go[-2:0:-1]]
    os.makedirs(cfg["saida"], exist_ok=True)
    frames = []
    for i, (f, t) in enumerate(seq, 1):
        img = Image.fromarray(np.clip(rig.warp(master, f, t), 0, 255).astype(np.uint8), "RGBA")
        img.save(os.path.join(cfg["saida"], f"{cfg['nome']}_{i:02d}.png"))
        frames.append(img)
    if cfg.get("previa"):
        hold, mid = cfg.get("tempo_extremo_ms", 200), cfg.get("tempo_ms", 90)
        durations = [hold if t in (0, 1) else mid for _, t in seq]
        gif = []
        for img in frames:
            bg = Image.new("RGBA", img.size, (232, 232, 238, 255))
            bg.alpha_composite(img)
            gif.append(bg.convert("RGB").resize((img.width // 2, img.height // 2), Image.LANCZOS))
        gif[0].save(cfg["previa"], save_all=True, append_images=gif[1:], duration=durations, loop=0)
    print(f"{len(frames)} quadros em {cfg['saida']}")


if __name__ == "__main__":
    main()
