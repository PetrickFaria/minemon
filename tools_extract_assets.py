#!/usr/bin/env python3
"""Copy Cobblemon data assets into Minemon's namespace without copying Cobblemon code.

Usage: python tools_extract_assets.py /path/to/Cobblemon-forge-1.5.2+1.20.1.jar
The script is for local, non-commercial development. Keep asset notices with the copied files.
"""
from __future__ import annotations
import json
import re
import sys
import shutil
import zipfile
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit("usage: python tools_extract_assets.py <Cobblemon jar>")
source = Path(sys.argv[1]).resolve()
project = Path(__file__).resolve().parent
resources = project / "src/main/resources"
if not source.is_file():
    raise SystemExit(f"JAR not found: {source}")

# Clear only extractor-managed trees so reruns cannot leave stale species files.
for managed in (
    resources / "assets/minemon/geo", resources / "assets/minemon/animations",
    resources / "assets/minemon/textures/pokemon", resources / "assets/minemon/sounds/pokemon",
    resources / "assets/minemon/licenses/pokemon", resources / "assets/minemon/geo/shared",
    resources / "assets/minemon/animations/special",
):
    if managed.exists():
        shutil.rmtree(managed)

with zipfile.ZipFile(source) as jar:
    names = set(jar.namelist())
    catalog = []
    copied = 0
    species_files = sorted(n for n in names if n.startswith("data/cobblemon/species/") and n.endswith(".json"))
    for species_path in species_files:
        data = json.loads(jar.read(species_path))
        dex = int(data.get("nationalPokedexNumber", 0))
        slug = Path(species_path).stem
        prefix = f"{dex:04d}_"
        copied_paths = {"models": [], "animations": [], "textures": [], "sounds": [], "licenses": []}
        for src in sorted(n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/models/{prefix}") and n.endswith(".geo.json")):
            dest = resources / "assets/minemon/geo" / slug / Path(src).name
            dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(src)); copied += 1
            copied_paths["models"].append(str(dest.relative_to(resources)))
        animation_clips = []
        for src in sorted(n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/animations/{prefix}") and n.endswith(".animation.json")):
            dest = resources / "assets/minemon/animations" / slug / Path(src).name
            dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(src)); copied += 1
            copied_paths["animations"].append(str(dest.relative_to(resources)))
            animation_json = json.loads(jar.read(src))
            animation_clips.extend(key.split(".")[-1] for key in animation_json.get("animations", {}))
        for src in sorted(n for n in names if n.startswith(f"assets/cobblemon/textures/pokemon/{prefix}") and n.lower().endswith(".png")):
            relative_image = Path(*src.split("/")[5:])
            dest = resources / "assets/minemon/textures/pokemon" / slug / relative_image
            dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(src)); copied += 1
            copied_paths["textures"].append(str(dest.relative_to(resources)))
        # Sound files are organized by lowercase species slug, not Pokédex number.
        sound_prefix = f"assets/cobblemon/sounds/pokemon/{slug}/"
        for src in sorted(n for n in names if n.startswith(sound_prefix) and n.lower().endswith(".ogg")):
            dest = resources / "assets/minemon/sounds/pokemon" / slug / Path(src).name
            dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(src)); copied += 1
            copied_paths["sounds"].append(str(dest.relative_to(resources)))
        license_srcs = sorted(n for n in names if n.startswith(f"assets/cobblemon/bedrock/pokemon/models/{prefix}") and n.endswith("/license"))
        for src in license_srcs:
            dest = resources / "assets/minemon/licenses/pokemon" / slug / "model-license.txt"
            dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(src)); copied += 1
            copied_paths["licenses"].append(str(dest.relative_to(resources)))
        catalog.append({
            "name": data.get("name", slug), "slug": slug, "nationalPokedexNumber": dex,
            "implementedInSource": bool(data.get("implemented", False)),
            "baseScale": data.get("baseScale"), "height": data.get("height"), "weight": data.get("weight"),
            "hitbox": data.get("hitbox"), "baseStats": data.get("baseStats"),
            "forms": [{"name": f.get("name"), "aspects": f.get("aspects", [])} for f in data.get("forms", [])],
            "resources": copied_paths, "animationClips": sorted(set(animation_clips)),
            "hasModel": bool(copied_paths["models"]), "hasAnimation": bool(copied_paths["animations"]),
            "hasTexture": bool(copied_paths["textures"]), "hasSound": bool(copied_paths["sounds"])
        })
    shared_assets = []
    shared_map = {
        "assets/cobblemon/bedrock/pokemon/models/substitute.geo.json": "assets/minemon/geo/shared/substitute.geo.json",
        "assets/cobblemon/textures/pokemon/substitute.png": "assets/minemon/textures/pokemon/substitute.png",
        "assets/cobblemon/textures/pokemon/substitute_shiny.png": "assets/minemon/textures/pokemon/substitute_shiny.png",
        "assets/cobblemon/bedrock/pokemon/animations/special/particle_dummy.animation.json": "assets/minemon/animations/special/particle_dummy.animation.json",
        "assets/cobblemon/bedrock/pokemon/animations/special/particles.animation.json": "assets/minemon/animations/special/particles.animation.json",
        "assets/cobblemon/sounds/pokemon/evolution_notification.ogg": "assets/minemon/sounds/pokemon/evolution_notification.ogg",
        "assets/cobblemon/sounds/pokemon/evolving.ogg": "assets/minemon/sounds/pokemon/evolving.ogg",
    }
    for src, dest_rel in shared_map.items():
        if src in names:
            dest = resources / dest_rel
            dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(src)); copied += 1
            shared_assets.append({"source": src, "resource": dest_rel})
    # Eiscue's Ice form has a separate cry folder rather than a numbered base-species folder.
    for src in sorted(n for n in names if n.startswith("assets/cobblemon/sounds/pokemon/eiscueice/") and n.endswith(".ogg")):
        dest = resources / "assets/minemon/sounds/pokemon/eiscueice" / Path(src).name
        dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(src)); copied += 1
        shared_assets.append({"source": src, "resource": str(dest.relative_to(resources))})
    root_model_license = "assets/cobblemon/bedrock/pokemon/models/license"
    if root_model_license in names:
        dest = resources / "assets/minemon/licenses/shared/model-license.txt"
        dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(jar.read(root_model_license)); copied += 1
        shared_assets.append({"source": root_model_license, "resource": str(dest.relative_to(resources))})
    shared_manifest = resources / "data/minemon/shared_resources.json"
    shared_manifest.parent.mkdir(parents=True, exist_ok=True)
    shared_manifest.write_text(json.dumps(shared_assets, ensure_ascii=False, indent=2), encoding="utf-8")

    out = resources / "data/minemon/species_catalog.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(catalog, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Species records: {len(catalog)}; implemented in source data: {sum(x['implementedInSource'] for x in catalog)}")
    print(f"Copied resource files: {copied}; catalog: {out}")
