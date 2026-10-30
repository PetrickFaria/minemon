"""Exact, narrow exception for the requested Arbok UV texture adaptation."""
import hashlib

ARBOK_TEXTURE_PATH = "assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png"
ARBOK_TEXTURE_SHA256 = "ac0a5028891be22ffa2673ff5272f2a1a7bd0bee49e299c52fc9946c10a801ea"

# Exact static resources for the PokéC, Rocket, Pokémon treats, PokéCoin, Pokébolas, and breeding egg. Keep this explicit
# so the historical species asset audits continue to reject unrelated edits.
POKEC_RESOURCES = frozenset({
    "src/main/resources/data/minemon_gen1/model_metrics.json",
    "src/main/resources/assets/minemon_gen1/models/item/breeding_egg.json",
    "src/main/resources/assets/minemon_gen1/textures/block/breeding_egg_shell.png",
    "src/main/resources/assets/minemon_gen1/textures/block/breeding_egg_marks.png",
    "src/main/resources/assets/minemon_gen1/models/item/fairy_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/steel_soul.json",
    "src/main/resources/assets/minemon_gen1/models/item/dark_soul.json",
    "src/main/resources/assets/minemon_gen1/textures/item/fairy_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/steel_soul.png",
    "src/main/resources/assets/minemon_gen1/textures/item/dark_soul.png",
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
    "src/main/resources/assets/minemon_gen1/blockstates/breeding_egg.json",
    "src/main/resources/assets/minemon_gen1/models/block/breeding_egg.json",
    "src/main/resources/assets/minemon_gen1/lang/pt_br.json",
    "src/main/resources/assets/minemon/models/item/master_ball.json",
    "src/main/resources/assets/minemon/textures/item/master_ball.png",
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

ROCKET_RESOURCES = frozenset({
    "src/main/resources/assets/minemon/lang/en_us.json",
    "src/main/resources/assets/minemon/lang/pt_br.json",
    "src/main/resources/assets/minemon_gen1/blockstates/rocket_block.json",
    "src/main/resources/assets/minemon_gen1/models/block/rocket_block.json",
    "src/main/resources/assets/minemon_gen1/models/item/rocket_block.json",
    "src/main/resources/assets/minemon_gen1/textures/block/rocket_bottom.png",
    "src/main/resources/assets/minemon_gen1/textures/block/rocket_front.png",
    "src/main/resources/assets/minemon_gen1/textures/block/rocket_side.png",
    "src/main/resources/assets/minemon_gen1/textures/block/rocket_top.png",
    "src/main/resources/assets/minemon_gen1/textures/entity/villager/profession/rocket.png",
    "src/main/resources/assets/minemon_gen1/textures/entity/villager/profession/rocket.png.mcmeta",
    "src/main/resources/assets/minemon_gen1/textures/entity/zombie_villager/profession/rocket.png",
    "src/main/resources/assets/minemon_gen1/textures/entity/zombie_villager/profession/rocket.png.mcmeta",
    "src/main/resources/assets/minemon/textures/pokemon/rattata_rocket.png",
    "src/main/resources/data/minecraft/tags/point_of_interest_type/acquirable_job_site.json",
    "src/main/resources/data/minemon_gen1/loot_tables/blocks/rocket_block.json",
})


TREAT_RESOURCES = frozenset(
    f"src/main/resources/assets/minemon/{kind}/{item}{suffix}"
    for kind, suffix in (("models/item", ".json"), ("textures/item", ".png"))
    for item in (
        "aquatic_treat", "frozen_treat", "dragon_treat", "electric_treat",
        "ghost_treat", "fairy_treat", "fire_treat", "bug_treat",
        "fighting_treat", "steel_treat", "normal_treat", "rock_treat",
        "psychic_treat", "ground_treat", "poison_treat", "flying_treat", "grass_treat", "dark_treat",
        "poke_coin", "pokeball",
    )
)


def is_approved_prior_asset_delta(path, content):
    """Accept only the reviewed Arbok atlas bytes; all other old assets stay exact."""
    return path == ARBOK_TEXTURE_PATH and hashlib.sha256(content).hexdigest() == ARBOK_TEXTURE_SHA256


def pokec_resource_paths():
    """Return exact reviewed resources for PokéC, Rocket, treats, Pokébolas, and the breeding egg."""
    recipes = frozenset(f"src/main/resources/data/minemon/recipes/{item}_treat.json" for item in (
        "aquatic", "frozen", "dragon", "electric", "ghost", "fairy", "fire", "bug", "fighting",
        "steel", "normal", "rock", "psychic", "ground", "poison", "flying", "grass", "dark"))
    return POKEC_RESOURCES | ROCKET_RESOURCES | TREAT_RESOURCES | recipes | {"src/main/resources/data/minemon_gen1/preview_body_pivots.json", "src/main/resources/assets/minemon_gen1/particles/gastly_small_smoke.json", "src/main/resources/data/minemon/recipes/pokeball.json", "src/main/resources/data/minemon/recipes/master_ball.json"}

