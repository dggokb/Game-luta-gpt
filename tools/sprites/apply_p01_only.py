#!/usr/bin/env python3
"""Restore the P01-only Android test variant after build_characters.py --write."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
GENERATED = ROOT / "android/app/src/main/java/com/gamelutagpt/GeneratedCharacters.java"
DRAWABLE = ROOT / "android/app/src/main/res/drawable-nodpi"

HEADER = "package com.gamelutagpt;\nimport java.util.*;\n/** P01-only APK. Reapply with tools/sprites/apply_p01_only.py after sprite generation. */\nfinal class GeneratedCharacters {\n static final String[] TEAM = new String[]{\"player_base\",\"p01_training\"};\n static final String[][] TEAMS = new String[][]{{\"player_base\",\"p01_training\"}};\n static final String[] SELECTABLE = new String[]{\"player_base\",\"p01_training\"};\n static final String OPPONENT = \"player_base\";\n private static final Map<String,CharacterDefinition> ALL = build();\n static CharacterDefinition get(String id) { CharacterDefinition c=ALL.get(id);if(c==null)throw new IllegalArgumentException(id);return c; }\n static CharacterDefinition defaultCharacter() { return get(\"player_base\"); }\n static CharacterDefinition opponentCharacter() { return get(OPPONENT); }\n private static Map<String,CharacterDefinition> build() {\n Map<String,CharacterDefinition> all=new LinkedHashMap<>();\n add8(all);\n CharacterDefinition p01=all.get(\"player_base\");\n all.put(\"p01_training\",new CharacterDefinition(\"p01_training\",\"P01 (Treino)\",p01.profile,p01.artFacing,\n p01.visualStandHeight,p01.visualCrouchHeight,p01.fighter,p01.animations,p01.moves,p01.specialAnimations,p01.specials));\n return Collections.unmodifiableMap(all);\n }\n"

def main():
    original = GENERATED.read_text(encoding="utf-8")
    methods = re.findall(
        r"(?ms)^ private static void add\d+\(Map<String,CharacterDefinition> all\) \{\n.*?^ \}\n",
        original,
    )
    method = next((part for part in methods if 'all.put("player_base"' in part), None)
    if method is None:
        raise RuntimeError("Player P01 pack not found in GeneratedCharacters.java")
    atlases = set(re.findall(r'new CharacterDefinition\.Atlas\("([^"]+)"', method))
    if not atlases or any(not name.startswith("player_base_") for name in atlases):
        raise RuntimeError("P01 atlas references invalid; refusing to prune")
    missing = [name for name in sorted(atlases) if not (DRAWABLE / (name + ".png")).exists()]
    if missing:
        raise RuntimeError("Missing required P01 atlas: " + ", ".join(missing))
    GENERATED.write_text(HEADER + method.rstrip() + "\n}\n", encoding="utf-8")
    removed = 0
    for file in DRAWABLE.glob("*.png"):
        if file.stem not in atlases:
            file.unlink()
            removed += 1
    print(f"P01-only: {len(atlases)} atlases preserved, {removed} removed")

if __name__ == "__main__":
    main()
