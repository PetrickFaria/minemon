#!/usr/bin/env python3
"""Validate PokéC block model texture aliases and intentional screen/control tilt."""
import json
from pathlib import Path

ASSETS = Path("src/main/resources/assets/minemon_gen1")
MODELS = ("pokec_lower", "pokec_upper")
errors = []
for name in MODELS:
    path = ASSETS / "models" / "block" / f"{name}.json"
    model = json.loads(path.read_text(encoding="utf-8"))
    aliases = model.get("textures", {})
    for element in model.get("elements", []):
        for face in element.get("faces", {}).values():
            ref = face.get("texture", "")
            while ref.startswith("#"):
                key = ref[1:]
                if key not in aliases:
                    errors.append(f"{path}: undefined texture alias #{key}")
                    break
                ref = aliases[key]
            if not ref:
                continue
            namespace, texture = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
            texture_file = Path("src/main/resources/assets") / namespace / "textures" / f"{texture}.png"
            if not texture_file.is_file():
                errors.append(f"{path}: missing texture {ref} ({texture_file})")

upper = json.loads((ASSETS / "models/block/pokec_upper.json").read_text(encoding="utf-8"))
screen = [e for e in upper["elements"] if any(f.get("texture") == "#screen" for f in e.get("faces", {}).values())]
if not screen or any(e.get("rotation", {}).get("axis") != "x" for e in screen):
    errors.append("PokéC monitor frame/screen must use an X-axis rotation to tilt back")
for index in (11, 12, 13, 14, 15):
    if upper["elements"][index].get("rotation", {}).get("axis") != "x":
        errors.append(f"PokéC control component {index} must be angled")

if errors:
    raise SystemExit("\n".join(errors))
print("PokéC models: all texture aliases resolve, textures exist, and screen/control geometry is angled.")
