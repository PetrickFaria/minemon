package br.com.petrickfaria.minemon.pokec;

import java.util.*;

/** Modern typing used only by treats; the legacy PokéC soul rules remain unchanged. */
public final class PokemonTreatTypes {
    private static final Map<String, List<String>> MODERN_OVERRIDES = Map.of(
            "clefairy", List.of("fairy"),
            "clefable", List.of("fairy"),
            "jigglypuff", List.of("normal", "fairy"),
            "wigglytuff", List.of("normal", "fairy"),
            "mrmime", List.of("psychic", "fairy"),
            "magnemite", List.of("electric", "steel"),
            "magneton", List.of("electric", "steel")
    );

    private PokemonTreatTypes() {}

    public static List<String> forSpecies(String slug) {
        List<String> override = MODERN_OVERRIDES.get(slug);
        if (override != null) return override;
        PokecSpecies.Entry entry = PokecSpecies.get(slug);
        return entry == null ? List.of() : entry.types();
    }

    public static boolean accepts(String slug, String type) {
        return forSpecies(slug).contains(type);
    }
}
