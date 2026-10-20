#!/usr/bin/env python3
"""Import the audited #001-#010 Cobblemon 1.8.2 runtime source assets.

Run once with the user-provided Cobblemon Fabric 1.8.2 JAR. The original
Minemon source assets are retained; this only adds namespaced copies and
switches the first ten explicit runtime manifest entries.
"""
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
JAR = Path(sys.argv[1]) if len(sys.argv) > 1 else None
if JAR is None or not JAR.is_file():
    raise SystemExit("Usage: tools/import_cobblemon_182_batch1.py /path/to/Cobblemon-fabric-1.8.2+1.21.1.jar")

SPECIES = [
    (1, "bulbasaur", "bulbasaur_female.geo.json"),
    (2, "ivysaur", "ivysaur_female.geo.json"),
    (3, "venusaur", "venusaur_female.geo.json"),
    (4, "charmander", "charmander.geo.json"),
    (5, "charmeleon", "charmeleon.geo.json"),
    (6, "charizard", "charizard.geo.json"),
    (7, "squirtle", "squirtle.geo.json"),
    (8, "wartortle", "wartortle.geo.json"),
    (9, "blastoise", "blastoise.geo.json"),
    (10, "caterpie", "caterpie.geo.json"),
]

def source_paths(number, slug, model_file):
    prefix = f"assets/cobblemon/bedrock/pokemon"
    model = f"{prefix}/models/{number:04d}_{slug}/{model_file}"
    animation = f"{prefix}/animations/{number:04d}_{slug}/{slug}.animation.json"
    texture = f"assets/cobblemon/textures/pokemon/{number:04d}_{slug}/{slug}.png"
    flame_texture = f"assets/cobblemon/textures/pokemon/{number:04d}_{slug}/{slug}_flame1.png" if slug in ("charmander", "charmeleon", "charizard") else None
    license_path = f"{prefix}/models/{number:04d}_{slug}/license"
    flame_texture = f"assets/cobblemon/textures/pokemon/{number:04d}_{slug}/{slug}_flame1.png" if slug in ("charmander", "charmeleon", "charizard") else None
    return model, animation, texture, license_path, flame_texture

manifest_path = RES / "data/minemon_gen1/runtime_species.json"
manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
by_slug = {entry["slug"]: entry for entry in manifest}
with zipfile.ZipFile(JAR) as source:
    for number, slug, model_file in SPECIES:
        model_src, anim_src, texture_src, license_src, flame_texture = source_paths(number, slug, model_file)
        suffix = "charmander_cobblemon_1_8_2" if slug == "charmander" else f"{slug}_cobblemon_1_8_2"
        model_rel = f"assets/minemon/geo/{suffix}/{slug}.geo.json"
        anim_rel = f"assets/minemon/animations/{suffix}/{slug}.animation.json"
        texture_rel = f"assets/minemon/textures/pokemon/{suffix}/{slug}.png"
        license_rel = f"assets/minemon/licenses/pokemon/{slug}/cobblemon-1.8.2-model-license.txt"
        for archive_path, target in (
            (model_src, model_rel),
            (anim_src, anim_rel),
            (texture_src, texture_rel),
        ):
            dest = RES / target
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(source.read(archive_path))
        license_file = RES / license_rel
        license_file.parent.mkdir(parents=True, exist_ok=True)
        license_file.write_bytes(source.read(license_src))
        d = by_slug[slug]
        d["licenses"] = list(dict.fromkeys(d.get("licenses", []) + [license_rel]))
        d["model"], d["animation"], d["texture"] = model_rel, anim_rel, texture_rel
        d["clips"] = {"ground_idle": f"animation.{slug}.ground_idle", "ground_walk": f"animation.{slug}.ground_walk"}
        with source.open(anim_src) as stream:
            animations = json.load(stream)["animations"]
        source_geo = json.loads(source.read(model_src))["minecraft:geometry"][0]
        model_bones = {bone["name"] for bone in source_geo["bones"]}
        d["stripped_bones"] = {
            clip: sorted(set(animation.get("bones", {})) - model_bones)
            for clip, animation in animations.items()
            if set(animation.get("bones", {})) - model_bones
        }
        names = {name.rsplit(".", 1)[-1]: name for name in animations}
        for semantic, candidates in {
            "ground_run": ("ground_run",),
            "sleep": ("sleep",),
            "blink": ("blink",),
            "faint": ("faint",),
            "cry": ("cry",),
            "recoil": ("recoil",),
            "physical_attack": ("physical", "attack"),
            "special": ("special",),
            "water_idle": ("water_idle",),
            "water_swim": ("water_swim", "surfacewater_swim"),
            "surfacewater_idle": ("surfacewater_idle",),
        }.items():
            clip = next((names[name] for name in candidates if name in names), None)
            if clip:
                d["clips"][semantic] = clip
        # The source poser files for #007 and #008 are absent from this Cobblemon
        # package. Their clips remain available for inspection, but a terrestrial
        # run selector is not inferred without the original species poser reference.
        if slug in ("squirtle", "wartortle"):
            d["clips"].pop("ground_run", None)
        d["idle"] = f"animation.{slug}.ground_idle"
        d["walk"] = f"animation.{slug}.ground_walk"
        d.pop("generated_static_animation", None)
        d.pop("movement_fallback", None)
        d["geckolib3_bedrock_keyframes"] = True
        d["strip_bedrock_locators"] = True
        if flame_texture:
            flame_host = next(b for b in source_geo["bones"] if b["name"] in ("fire2", "fire") and b.get("cubes"))
            source_plane = flame_host["cubes"][0]
            d["flame_anchor"] = {"host": flame_host["name"], "parent": flame_host["parent"], "pivot": flame_host["pivot"], "origin": source_plane["origin"], "size": source_plane["size"], "uv": source_plane["uv"]}
            flame_rel = f"assets/minemon/textures/pokemon/{suffix}/{slug}_flame1.png"
            flame_dest = RES / flame_rel
            flame_dest.parent.mkdir(parents=True, exist_ok=True)
            flame_dest.write_bytes(source.read(flame_texture))
            d["flame_texture"] = flame_rel
        else:
            d.pop("flame_texture", None)
        d["tail_flame_animation"] = slug == "charmander"
        if slug == "charmander":
            d["clips"]["tail_flame"] = "animation.charmander.minemon_tail_flame"
        d["source_model_variant"] = "female" if number <= 3 else "default"
        # Preserve original dimensions/statistics/behavior values; only switch
        # the three explicit rendering paths and source-backed clip references.
        for key in ("sit", "physical_attack"):
            if key not in d["clips"]:
                d["clips"][key] = f"animation.{slug}.minemon_{key}"
        if "sleep" not in d["clips"]:
            d["clips"]["sleep"] = f"animation.{slug}.minemon_sleep"
        d["attackTicks"] = 12
        if "physical_attack" in d["clips"] and d["clips"]["physical_attack"] in animations:
            length = animations[d["clips"]["physical_attack"]].get("animation_length", 4.0)
            d["attackTicks"] = max(1, int(length * 20 + 0.999999))

manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("Imported Cobblemon 1.8.2 source assets and updated runtime paths for #001-#010.")
