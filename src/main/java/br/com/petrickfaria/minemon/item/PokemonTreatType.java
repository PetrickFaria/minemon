package br.com.petrickfaria.minemon.item;

/** Element-specific Pokémon treats. */
public enum PokemonTreatType {
    AQUATIC("aquatic_treat", "water"), FROZEN("frozen_treat", "ice"),
    DRAGON("dragon_treat", "dragon"), ELECTRIC("electric_treat", "electric"),
    GHOST("ghost_treat", "ghost"), FAIRY("fairy_treat", "fairy"),
    FIRE("fire_treat", "fire"), BUG("bug_treat", "bug"),
    FIGHTING("fighting_treat", "fighting"), STEEL("steel_treat", "steel"),
    NORMAL("normal_treat", "normal"), ROCK("rock_treat", "rock"),
    PSYCHIC("psychic_treat", "psychic"), GROUND("ground_treat", "ground"),
    POISON("poison_treat", "poison"), FLYING("flying_treat", "flying"),
    GRASS("grass_treat", "grass");

    private final String itemId;
    private final String pokemonType;
    PokemonTreatType(String itemId, String pokemonType) { this.itemId = itemId; this.pokemonType = pokemonType; }
    public String itemId() { return itemId; }
    public String pokemonType() { return pokemonType; }
}
