package br.com.petrickfaria.minemon.entity;

import java.util.Set;

/** Explicit natural-movement groups. Type labels alone are not sufficient to choose AI. */
public final class SpeciesBehaviorProfiles {
    private static final Set<String> AIRBORNE = Set.of(
            "butterfree", "beedrill", "pidgey", "pidgeotto", "pidgeot", "charizard", "fearow",
            "zubat", "golbat", "venomoth", "magnemite", "magneton", "farfetchd", "gastly",
            "haunter", "gengar", "koffing", "weezing", "scyther", "articuno", "zapdos",
            "moltres", "aerodactyl", "dragonite", "mewtwo", "mew");

    // Species that live predominantly in water. Amphibious walkers and land-capable
    // Water types (for example Squirtle, Psyduck, Slowpoke, and Poliwrath) are omitted.
    private static final Set<String> AQUATIC = Set.of(
            "tentacool", "tentacruel", "dewgong", "shellder", "cloyster", "horsea", "seadra",
            "goldeen", "seaking", "staryu", "starmie", "magikarp", "gyarados", "lapras",
            "omanyte", "omastar");

    private SpeciesBehaviorProfiles() {}

    public static boolean isAirborne(String slug) { return AIRBORNE.contains(slug); }
    public static boolean isAquatic(String slug) { return AQUATIC.contains(slug); }
    public static Set<String> airborneSpecies() { return AIRBORNE; }
    public static Set<String> aquaticSpecies() { return AQUATIC; }
}
