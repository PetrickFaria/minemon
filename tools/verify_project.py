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
bulbasaur = next((s for s in catalog if s["slug"] == "bulbasaur"), None)
if bulbasaur is None or not bulbasaur["implementedInSource"]:
    raise SystemExit("Bulbasaur must remain the registered validation species")
geometry_path = resources / "assets/minemon/geo/bulbasaur/bulbasaur.geo.json"
geometry = json.loads(geometry_path.read_text(encoding="utf-8"))
if not geometry.get("minecraft:geometry") or not geometry["minecraft:geometry"][0].get("bones"):
    raise SystemExit("Bulbasaur geo JSON has no GeckoLib-compatible geometry/bones")
animation_path = resources / "assets/minemon/animations/bulbasaur/bulbasaur.animation.json"
animations = json.loads(animation_path.read_text(encoding="utf-8")).get("animations", {})
if not animations:
    raise SystemExit("Bulbasaur animation JSON contains no animation clips")
egg_model_path = resources / "assets/minemon/models/item/bulbasaur_spawn_egg.json"
egg_model = json.loads(egg_model_path.read_text(encoding="utf-8"))
if egg_model.get("parent") != "minecraft:item/template_spawn_egg":
    raise SystemExit("Bulbasaur spawn egg must use Minecraft's spawn egg item template")
pack_meta = json.loads((resources / "pack.mcmeta").read_text(encoding="utf-8"))
if pack_meta.get("pack", {}).get("pack_format") != 9:
    raise SystemExit("pack.mcmeta must use resource pack format 9 for Minecraft 1.19.2")
print(f"Bulbasaur: {len(geometry['minecraft:geometry'][0]['bones'])} bones; {len(animations)} animation clips; spawn egg and pack.mcmeta valid")

# The source tree intentionally keeps the full extracted asset library, while the
# runtime JAR includes only entities actually registered in this milestone.
properties = {}
for line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
runtime_species = {slug.strip().lower() for slug in properties.get("runtime_species", "").split(",") if slug.strip()}
catalog_species = {species["slug"]: species for species in catalog}
if not runtime_species or not runtime_species.issubset(catalog_species):
    raise SystemExit(f"Invalid runtime_species list: {sorted(runtime_species)}")
for slug in runtime_species:
    species = catalog_species[slug]
    animation_paths = species["resources"].get("animations", [])
    model_paths = species["resources"].get("models", [])
    if not animation_paths or not model_paths:
        raise SystemExit(f"Runtime species {slug} must declare model and animation resources")
    model_data = json.loads((resources / model_paths[0]).read_text(encoding="utf-8"))
    model_geometries = model_data.get("minecraft:geometry", [])
    geometry_bones = {bone["name"] for part in model_geometries for bone in part.get("bones", [])}
    for path in animation_paths:
        data = json.loads((resources / path).read_text(encoding="utf-8"))
        for animation_name, animation in data.get("animations", {}).items():
            for bone in animation.get("bones", {}):
                if bone not in geometry_bones:
                    raise SystemExit(f"{slug} animation {animation_name} references missing model bone {bone}")
            for field in ("particle_effects", "sound_effects"):
                events = animation.get(field)
                if events is None:
                    continue
                if not isinstance(events, dict) or any(not isinstance(event, dict) for event in events.values()):
                    raise SystemExit(f"{path}: {animation_name}.{field} is not compatible with GeckoLib 3.1.40")
    if slug == "bulbasaur":
        actual_clips = {name.rsplit(".", 1)[-1] for name in animations}
        missing_clips = set(species.get("animationClips", [])) - actual_clips
        if missing_clips:
            raise SystemExit(f"Bulbasaur catalog references missing animation clips: {sorted(missing_clips)}")
print(f"Runtime species: {', '.join(sorted(runtime_species))}; animation bones and GeckoLib event tracks validated")
shared = json.loads((resources / "data/minemon/shared_resources.json").read_text(encoding="utf-8"))
for entry in shared:
    if not (resources / entry["resource"]).is_file():
        raise SystemExit(f"Missing shared resource: {entry['resource']}")
oggs = list((resources / "assets/minemon/sounds/pokemon").rglob("*.ogg"))
for path in oggs:
    if path.read_bytes()[:4] != b"OggS":
        raise SystemExit(f"Invalid OGG header: {path}")
print(f"Shared resources: {len(shared)}; OGG files: {len(oggs)}")
