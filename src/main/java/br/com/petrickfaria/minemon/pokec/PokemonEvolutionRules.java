package br.com.petrickfaria.minemon.pokec;

import java.util.List;

/** Evolution edges and level requirements derived from the shipped Gen I catalog. */
public final class PokemonEvolutionRules {
    private PokemonEvolutionRules() {}

    public static List<PokecSpecies.Entry> nextForms(String slug) {
        return PokecSpecies.ordered().stream().filter(entry -> slug.equals(entry.evolvesFrom())).toList();
    }

    public static int requiredLevel(String fromSlug) {
        if (nextForms(fromSlug).isEmpty()) return Integer.MAX_VALUE;
        boolean nextHasEvolution = nextForms(fromSlug).stream().anyMatch(next -> !nextForms(next.slug()).isEmpty());
        if (nextHasEvolution) return 5;
        boolean hasParent = PokecSpecies.get(fromSlug) != null && PokecSpecies.get(fromSlug).isEvolution();
        return hasParent ? 10 : 7;
    }

    public static boolean isValidEdge(String fromSlug, String toSlug) {
        return nextForms(fromSlug).stream().anyMatch(entry -> entry.slug().equals(toSlug));
    }
}
