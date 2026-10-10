#!/usr/bin/env python3
"""Vocabulário único dos vídeos de personagens (não renomeia arquivos no Drive).

O nome do arquivo não prova que o movimento foi executado corretamente:
este módulo resolve SOMENTE a correspondência semântica do nome.
"""
from __future__ import annotations

import re
import unicodedata
from pathlib import Path

# Os 35 movimentos-vídeo. O renderer deriva COMBAT, RISE, FALL, LAND, GROUNDED,
# GETUP e THROW_TOSS de trechos de outros clipes, sem exigir vídeos separados.
CANONICAL = {
    "IDLE": "idle", "WALK_FORWARD": "andar_frente",
    "WALK_BACK": "andar_tras", "DASH": "dash", "BACKDASH": "backdash",
    "CROUCH": "agachar", "JUMP": "pulo",
    "LIGHT_JAB": "L", "MEDIUM_KICK": "M", "HEAVY_STRAIGHT": "H",
    "CROUCH_LIGHT": "2L", "CROUCH_MEDIUM": "2M", "CROUCH_HEAVY": "2H",
    "JUMP_LIGHT": "jL", "JUMP_MEDIUM": "jM", "JUMP_HEAVY": "jH",
    "JUMP_HEAVY_DOWN": "jH_baixo",
    "SPECIAL_ENERGY": "s1", "SPECIAL_S2": "s2", "SPECIAL_S3": "s3",
    "SPECIAL_S4": "s4", "SUPER_WAVE": "super", "ULTRA_BEAM": "ultra",
    "DEFENSE_STAND": "defesa_cima", "DEFENSE_CROUCH": "defesa_baixo",
    "DEFENSE_AIR": "defesa_pulo", "HIT_STAND": "dano_cima",
    "HIT_CROUCH": "dano_baixo", "HIT_AIR": "dano_pulo",
    "KNOCKDOWN": "derruba_levanta",
    "INTRO": "intro", "VICTORY": "vitoria", "DEFEAT": "derrota",
    "TAUNT": "provocacao", "THROW": "agarrao",
}

ALIASES = {
    "IDLE": ("idle",),
    "WALK_FORWARD": ("frente", "andarfrente", "andarparafrente", "walk_fward", "andar_frente"),
    "WALK_BACK": ("tras", "back", "andartras", "andarparatras", "walk_back", "andar_tras"),
    "DASH": ("dash",), "BACKDASH": ("backdash", "back_dash"),
    "CROUCH": ("agachar", "abaixar", "agachado", "agacha", "crounch"),
    "JUMP": ("pulo", "jump"),
    "LIGHT_JAB": ("l", "socofraco", "ll", "L(jab)"),
    "MEDIUM_KICK": ("m", "socomedio", "M (soco médio)"),
    "HEAVY_STRAIGHT": ("h", "socoforte", "H(chute alto)"),
    "CROUCH_LIGHT": ("2l", "socofracobaixo", "chutebaixofraco", "l2(jab)"),
    "CROUCH_MEDIUM": ("2m", "rasteira", "rasteirabaixo", "m2(rasteira)"),
    "CROUCH_HEAVY": ("2h", "upper"),
    "JUMP_LIGHT": ("jl", "pulochute", "pulochutefraco"),
    "JUMP_MEDIUM": ("jm", "socopulo"),
    "JUMP_HEAVY": ("jh", "pulosocoforte", "pulochuteforteparafrente"),
    "JUMP_HEAVY_DOWN": ("j2h", "jhbaixo", "jH_baixo", "pulosocoparabaixo", "pulosocoparabaixoforte"),
    "SPECIAL_ENERGY": ("s1",), "SPECIAL_S2": ("s2",),
    "SPECIAL_S3": ("s3",), "SPECIAL_S4": ("s4",),
    "SUPER_WAVE": ("super",), "ULTRA_BEAM": ("ultra",),
    "DEFENSE_STAND": ("defesaempe", "defesacima", "defendecima"),
    "DEFENSE_CROUCH": ("defesaagachado", "defesaagaxado", "defesabaixo", "defesanochao", "defesaembaixo", "defesachao", "defendebaixo"),
    "DEFENSE_AIR": ("defesanoar", "defesapulo", "defesapulando"),
    "HIT_STAND": ("levargolpeempe", "danoempe", "damoempe", "danocima", "danoalto"),
    "HIT_CROUCH": ("levargolpeagachada", "levargolpeagachado", "danochao", "danoagachado", "danobaixo"),
    "HIT_AIR": ("levargolpenoar", "danopulando", "danoar", "golpenoar", "danopulo", "dano pulo"),
    "KNOCKDOWN": ("derrubado", "cair_levantar", "caindo", "queda_levanta", "derrubar_levantar", "derruma_levanta", "derrumba_levanta", "derruba_levanta", "deruba_levanta"),
    "INTRO": ("intro",), "VICTORY": ("vitoria",), "DEFEAT": ("derrota",),
    "TAUNT": ("provocacao", "provocar", "provoca"),
    "THROW": ("agarrao", "agarrar"),
}

# 'ente.mp4' é um typo real do P09 (frente) — não generalizar para outros.
SPECIFIC = {"p09": {"WALK_FORWARD": ("ente",)}}


def normalize(name: str) -> str:
    stem = Path(name).stem if str(name).lower().endswith(".mp4") else str(name)
    ascii_text = "".join(c for c in unicodedata.normalize("NFKD", stem)
                         if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]", "", ascii_text.lower())


def resolve(names, character_id=""):
    """Retorna (state -> nome exato sem extensão, ambiguidades, não classificados).

    NUNCA adivinha entre duas variantes do mesmo estado. Mantém a grafia do Drive.
    """
    normalized = {}
    for filename in names:
        if str(filename).lower().endswith(".mp4"):
            normalized.setdefault(normalize(filename), []).append(Path(filename).stem)
    mapping, ambiguous, matched = {}, {}, set()
    for state, standard in CANONICAL.items():
        opts = (standard,) + ALIASES.get(state, ()) + SPECIFIC.get(character_id, {}).get(state, ())
        candidate = sorted({v for o in opts for v in normalized.get(normalize(o), [])})
        if len(candidate) == 1:
            mapping[state] = candidate[0]
            matched.add(candidate[0])
        elif len(candidate) > 1:
            ambiguous[state] = candidate
            matched.update(candidate)
    unmatched = sorted(v for entries in normalized.values() for v in entries if v not in matched)
    return mapping, ambiguous, unmatched
