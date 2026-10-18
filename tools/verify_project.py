#!/usr/bin/env python3
"""Validate the generated catalog and every resource path it references."""
import json
import struct
from pathlib import Path

root = Path(__file__).resolve().parents[1]
resources = root / "src/main/resources"
catalog_path = resources / "data/minemon/species_catalog.json"
catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
missing = []
for species in catalog:
    for kind, paths in species["resources"].items():
        for relative in paths:
            if not (resources / relative).is_file():
                missing.append((species["slug"], kind, relative))
if missing:
    for row in missing[:50]:
        print("MISSING", *row)
    raise SystemExit(f"Failed: {len(missing)} catalog resource paths are missing")
print(f"OK: {len(catalog)} species records; {sum(len(v) for s in catalog for v in s['resources'].values())} referenced resources exist")
print(f"Source marked implemented: {sum(s['implementedInSource'] for s in catalog)}")
print(f"Model+texture+animation present: {sum(s['hasModel'] and s['hasTexture'] and s['hasAnimation'] for s in catalog)}")
# Validate syntax for every geometry and animation JSON and the PNG file signatures.
json_assets = list((resources / "assets/minemon/geo").rglob("*.geo.json")) + list((resources / "assets/minemon/animations").rglob("*.animation.json"))
for path in json_assets:
    json.loads(path.read_text(encoding="utf-8"))
pngs = list((resources / "assets/minemon/textures/pokemon").rglob("*.png"))
for path in pngs:
    if path.read_bytes()[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"Invalid PNG header: {path}")
print(f"Valid JSON models/animations: {len(json_assets)}; PNG files: {len(pngs)}")
shared = json.loads((resources / "data/minemon/shared_resources.json").read_text(encoding="utf-8"))
for entry in shared:
    if not (resources / entry["resource"]).is_file():
        raise SystemExit(f"Missing shared resource: {entry['resource']}")
oggs = list((resources / "assets/minemon/sounds/pokemon").rglob("*.ogg"))
for path in oggs:
    if path.read_bytes()[:4] != b"OggS":
        raise SystemExit(f"Invalid OGG header: {path}")
print(f"Shared resources: {len(shared)}; OGG files: {len(oggs)}")
