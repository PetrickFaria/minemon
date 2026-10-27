#!/usr/bin/env python3
"""Validate Rocket workstation registrations, villager resources, and the optional distributable JAR."""
import json
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
RESOURCE_PATHS = [
    "assets/minemon_gen1/blockstates/rocket_block.json",
    "assets/minemon_gen1/models/block/rocket_block.json",
    "assets/minemon_gen1/models/item/rocket_block.json",
    "assets/minemon_gen1/textures/block/rocket_front.png",
    "assets/minemon_gen1/textures/block/rocket_side.png",
    "assets/minemon_gen1/textures/block/rocket_top.png",
    "assets/minemon_gen1/textures/block/rocket_bottom.png",
    "assets/minemon_gen1/textures/entity/villager/profession/rocket.png",
    "assets/minemon_gen1/textures/entity/villager/profession/rocket.png.mcmeta",
    "assets/minemon_gen1/textures/entity/zombie_villager/profession/rocket.png",
    "assets/minemon_gen1/textures/entity/zombie_villager/profession/rocket.png.mcmeta",
    "data/minemon_gen1/loot_tables/blocks/rocket_block.json",
    "data/minecraft/tags/point_of_interest_type/acquirable_job_site.json",
]


def read(path, jar=None):
    return jar.read(path) if jar else (RES / path).read_bytes()


for path in RESOURCE_PATHS:
    read(path)

blockstate = json.loads(read("assets/minemon_gen1/blockstates/rocket_block.json"))
assert set(blockstate["variants"]) == {"facing=north", "facing=east", "facing=south", "facing=west"}
assert all(v["model"] == "minemon_gen1:block/rocket_block" for v in blockstate["variants"].values())
block_model = json.loads(read("assets/minemon_gen1/models/block/rocket_block.json"))
assert block_model["parent"] == "minecraft:block/cube"
assert block_model["textures"]["north"] == "minemon_gen1:block/rocket_front"
item_model = json.loads(read("assets/minemon_gen1/models/item/rocket_block.json"))
assert item_model["parent"] == "minemon_gen1:block/rocket_block"
loot = json.loads(read("data/minemon_gen1/loot_tables/blocks/rocket_block.json"))
assert loot["pools"][0]["entries"][0]["name"] == "minemon_gen1:rocket_block"
job_sites = json.loads(read("data/minecraft/tags/point_of_interest_type/acquirable_job_site.json"))
assert job_sites["replace"] is False and "minemon_gen1:rocket" in job_sites["values"]

for language, block_name in (("en_us", "Rocket Block"), ("pt_br", "Bloco Rocket")):
    source = json.loads((RES / f"assets/minemon/lang/{language}.json").read_text(encoding="utf-8"))
    assert source["block.minemon_gen1.rocket_block"] == block_name
    assert source["entity.minecraft.villager.rocket"] == "Rocket"
    assert source["entity.minecraft.villager.minemon_gen1.rocket"] == "Rocket"

for path, expected in ((p, (16, 16)) for p in RESOURCE_PATHS if "/textures/block/" in p):
    width, height = struct.unpack(">II", read(path)[16:24])
    assert (width, height) == expected, (path, width, height)
for path in ("assets/minemon_gen1/textures/entity/villager/profession/rocket.png",
             "assets/minemon_gen1/textures/entity/zombie_villager/profession/rocket.png"):
    width, height = struct.unpack(">II", read(path)[16:24])
    assert (width, height) == (64, 64), (path, width, height)
for path in ("assets/minemon_gen1/textures/entity/villager/profession/rocket.png.mcmeta",
             "assets/minemon_gen1/textures/entity/zombie_villager/profession/rocket.png.mcmeta"):
    assert json.loads(read(path))["villager"]["hat"] == "full"

if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as jar:
        names = set(jar.namelist())
        for path in RESOURCE_PATHS:
            assert path in names, ("missing Rocket JAR resource", path)
        for path in ("assets/minemon_gen1/lang/en_us.json", "assets/minemon_gen1/lang/pt_br.json"):
            packed = json.loads(jar.read(path))
            assert "block.minemon_gen1.rocket_block" in packed
            assert "entity.minecraft.villager.rocket" in packed
        assert "br/com/petrickfaria/minemon/block/RocketBlock.class" in names
        assert "br/com/petrickfaria/minemon/registry/ModVillagers.class" in names

print("PASS ROCKET: solid directional workstation assets, POI tag, bilingual names, villager overlays, and optional JAR contents")
