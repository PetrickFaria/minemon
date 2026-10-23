#!/usr/bin/env python3
"""Verify Mew's native Cobblemon 1.8.2 import and runtime selection."""
import hashlib
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
MANIFEST = RES / "data/minemon_gen1/runtime_species.json"
DEX = RES / "data/minemon_gen1/pokedex.json"
defs = json.loads(MANIFEST.read_text())
mew = next(d for d in defs if d["slug"] == "mew")
assert len(defs) == 151 and mew["nationalPokedexNumber"] == 151
assert mew["source_cobblemon_version"] == "1.8.2"
assert mew["mode"] == "flying"
assert mew["source_model_variant"] == "default"
assert all("mew_cobblemon_1_8_2/" in mew[k] for k in ("model", "texture", "animation"))
assert mew["clips"] == {
    "ground_idle": "animation.mew.ground_idle",
    "ground_walk": "animation.mew.ground_walk",
    "blink": "animation.mew.blink",
}
assert mew["idle"] == mew["clips"]["ground_idle"]
assert mew["walk"] == mew["clips"]["ground_walk"]
assert mew.get("source_static_idle") is True
assert "q.pitch_tilt(body)" in mew["native_poser"] and "q.look(head" in mew["native_poser"]

for key in ("model", "texture", "animation"):
    path = RES / mew[key]
    assert path.is_file(), (key, path)
    assert hashlib.sha256(path.read_bytes()).hexdigest() == mew["source_asset_sha256"][key], (key, "source asset hash changed")

geo = json.loads((RES / mew["model"]).read_text())["minecraft:geometry"][0]
bones = {b["name"]: b for b in geo["bones"]}
assert geo["description"]["identifier"] == "geometry.mew"
assert all(name in bones for name in ("mew", "body", "head", "arm_left", "arm_right", "leg_left", "leg_right", "tail", "tail11"))
assert bones["tail"]["parent"] == "torso" and bones["tail11"]["parent"] == "tail10"

animations = json.loads((RES / mew["animation"]).read_text())["animations"]
assert {"animation.mew.ground_idle", "animation.mew.ground_walk", "animation.mew.blink"} <= set(animations)
assert not any(name.endswith(".minemon_run") or name.endswith(".minemon_sit") or name.endswith(".minemon_sleep") for name in animations)
for clip_name in (mew["idle"], mew["walk"]):
    assert {"body", "head", "arm_left", "arm_right", "tail", "tail11"} <= set(animations[clip_name]["bones"])
assert animations[mew["idle"]]["bones"]["body"]["position"] != animations[mew["walk"]]["bones"]["body"]["position"]
assert animations[mew["idle"]]["bones"]["tail"]["rotation"] != animations[mew["walk"]]["bones"]["tail"]["rotation"]

dex = json.loads(DEX.read_text())
assert len(dex) == 151 and [d["nationalPokedexNumber"] for d in dex] == list(range(1, 152))
assert next(d for d in dex if d["nationalPokedexNumber"] == 151)["slug"] == "mew"
model_code = (ROOT / "src/main/java/br/com/petrickfaria/minemon/client/MinemonModel.java").read_text()
smoke_code = (ROOT / "src/main/java/br/com/petrickfaria/minemon/gametest/ClientSmokeTest.java").read_text()
assert 'slug.equals("mew")' in model_code
assert '"mew"' in smoke_code and "SOURCE PITCH" in smoke_code

if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as jar:
        names = set(jar.namelist())
        for key in ("model", "texture", "animation", "licenses"):
            paths = mew[key] if key != "licenses" else mew[key]
            for path in ([paths] if isinstance(paths, str) else paths):
                packed = path.replace("assets/minemon/", "assets/minemon_gen1/", 1)
                assert packed in names, ("missing Mew JAR asset", packed)
        packed_geo = json.loads(jar.read(mew["model"].replace("assets/minemon/", "assets/minemon_gen1/", 1)))
        packed_bones = {b["name"] for b in packed_geo["minecraft:geometry"][0]["bones"]}
        assert {"body", "head", "tail", "tail11"} <= packed_bones
        packed_anim = json.loads(jar.read(mew["animation"].replace("assets/minemon/", "assets/minemon_gen1/", 1)))["animations"]
        for clip in mew["clips"].values():
            assert clip in packed_anim, ("Mew native clip missing from JAR", clip)
        assert not any("mew_cobblemon_1_8_2" in name and name.endswith(".geo.json") and name != mew["model"].replace("assets/minemon/", "assets/minemon_gen1/", 1) for name in names)

print("PASS BATCH 16 #151 Mew: exact Cobblemon 1.8.2 model/texture/animation, native hover/fly poser mapping, 151 Pokédex records")
