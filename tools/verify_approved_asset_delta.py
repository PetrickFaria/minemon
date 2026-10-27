"""Exact, narrow exception for the requested Arbok UV texture adaptation."""
import hashlib

ARBOK_TEXTURE_PATH = "assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png"
ARBOK_TEXTURE_SHA256 = "ac0a5028891be22ffa2673ff5272f2a1a7bd0bee49e299c52fc9946c10a801ea"

# Exact static resources for the PokéC feature. Keep this explicit so the
# historical species asset audits continue to reject unrelated resource edits.
POKEC_RESOURCES = frozenset({
    "src/main/resources/assets/minemon_gen1/blockstates/pokec.json",
    "src/main/resources/assets/minemon_gen1/models/block/pokec_lower.json",
    "src/main/resources/assets/minemon_gen1/models/block/pokec_upper.json",
    "src/main/resources/assets/minemon_gen1/models/item/bug_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/dragon_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/electric_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/fighting_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/fire_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/flying_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/ghost_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/grass_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/ground_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/ice_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/normal_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/pokec.json",
    "src/main/resources/assets/minemon_gen1/models/item/poison_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/psychic_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/rock_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/water_soul.json",
    "src/main/resources/assets/minemon_gen1/textures/block/pokec_lower.png",
    "src/main/resources/assets/minemon_gen1/textures/block/pokec_upper.png",
    "src/main/resources/assets/minemon_gen1/textures/item/bug_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/dragon_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/electric_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/fighting_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/fire_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/flying_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/ghost_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/grass_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/ground_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/ice_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/normal_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/poison_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/psychic_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/rock_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/water_soul.png",
    "src/main/resources/data/minemon_gen1/loot_tables/blocks/pokec.json",
    "src/main/resources/data/minemon_gen1/pokec_species.json",
})


def is_approved_prior_asset_delta(path, content):
    """Accept only the reviewed Arbok atlas bytes; all other old assets stay exact."""
    return path == ARBOK_TEXTURE_PATH and hashlib.sha256(content).hexdigest() == ARBOK_TEXTURE_SHA256


def pokec_resource_paths():
    """Return only the checked-in PokéC block, soul, and catalog resources."""
    return POKEC_RESOURCES
