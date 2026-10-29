# PokéC

PokéC is a two-block terminal that browses all 151 Gen I species in Pokédex order. The client creates previews only for the 18 cards on the current page and uses the existing GeckoLib model, texture, renderer, and idle controller. Preview entities are never inserted into the world and have AI disabled.

## Economy configuration

The server creates `config/minemon/pokec-economy.toml` on first start. Edit it and run `/pokec reload` as an operator to reload the values. Defaults use five explicitly named rarity classes:

| Rarity | Souls to unlock | PokéCoins | Soul points |
|---|---:|---:|---:|
| Common | 25 | 10 | 1 |
| Uncommon | 50 | 20 | 2 |
| Rare | 75 | 30 | 3 |
| Legendary | 100 | 40 | 4 |
| Mythic | 150 | 50 | 5 |

The yield base is `stage points + rarity points + level points`. Evolution stages come from the catalog's evolution-parent data: unevolved roots are Initial, forms with both a parent and a further evolution are Intermediate, and the last form is Final. A species with no evolution is treated as Initial. Levels are read from the individual Pokémon and map to 1–2, 3–4, 5–6, 7–8, and 9–10 soul points. The defaults therefore produce a base of 3–13 souls.

A wild Pokémon's eligible death drop is a uniformly selected whole number from 50%–100% of its base. Returning a Pokémon to nature through the PokéC is uniformly selected from 75%–100%. Lower limits round up to whole souls. Both actions use the soul matching the Pokémon's primary type; they reuse the registered soul items.

The config contains settings for `yield.stage.*`, `yield.rarity.*.souls`, `yield.level.*`, `yield.death.*Percent`, and `yield.release.*Percent`. It contains a `species.<slug>.rarity` entry for each species, plus rarity-level costs under `rarity.<rarity>.souls` and `rarity.<rarity>.coins`. Optional `species.<slug>.coins` and `species.<slug>.souls` entries override the corresponding species' defaults. Invalid numbers use the safe defaults, and percentage limits outside 0–100 or with minimum above maximum are rejected. Legacy rarity labels are normalized to the five supported categories; Very Rare remains Rare, and Mew is Mythic.

Configuration migrations update legacy default costs while retaining individual configured overrides. Player unlocks, acquired Pokémon, balances, partial soul deposits, first-purchase state, levels, XP, owner data, and occupied Pokébolas remain in their existing persistent data.

Use `/pokec coins <player> <amount>` as an operator to set a test balance. PokéCoins continue to use the existing balance and registered coin item.

## Player actions

Click a locked base species card while holding one matching primary-type soul to stage a deposit. The two existing soul slots and partial deposits remain supported. Evolution cards remain locked until the immediately preceding owned species evolves into them. Once a species is unlocked, clicking its card purchases a tame Minemon owned by the player. The first completed purchase is free; later purchases use the configured price. The server validates the open terminal, range, species, soul item, price, balance, owner, and transaction ID before mutating player data.

The **Devolver à Natureza** panel accepts an occupied Pokébola containing a Pokémon owned by the player. It previews the species, Pokédex number, level, rarity, base yield, and possible return range. The player must confirm before the server consumes the Pokébola token, delivers the matching souls, and returns the empty ball. Empty balls and balls belonging to other players are rejected. A consumed token cannot release or convert the Pokémon a second time. The operation does not clear the species unlock or reset the free-purchase flag.

The existing Minemon code has no evolution trigger or evolution operation. PokéC provides `MinemonCreature.notifyPokecEvolution(String)` as the server-side integration point: an evolution implementation must call it on the owned source entity after a successful transition. The method validates the owner and the declared one-step evolution edge; it does not create an evolution mechanic or unlock unrelated branches by itself.

## Data source

`data/minemon_gen1/pokec_species.json` lists the National Pokédex number, display name, Gen I type(s), explicit starting rarity, and in-generation evolution parent for every card. The first listed type is the soul type. PokéC reuses the registered species definitions and existing GeckoLib resources.
