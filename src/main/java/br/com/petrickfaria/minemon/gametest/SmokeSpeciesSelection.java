package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves an optional comma-separated smoke-test selection without changing any per-species gates. */
final class SmokeSpeciesSelection {
    private SmokeSpeciesSelection() {}

    static List<String> select(Collection<SpeciesDefinitions.Definition> definitions, String requested) {
        var ordered = definitions.stream()
                .sorted(Comparator.comparingInt(SpeciesDefinitions.Definition::nationalPokedexNumber))
                .toList();
        var all = ordered.stream().map(SpeciesDefinitions.Definition::slug).toList();
        if (requested == null || requested.isBlank()) return all;

        var bySlug = ordered.stream().collect(Collectors.toMap(
                SpeciesDefinitions.Definition::slug, Function.identity()));
        var byNumber = ordered.stream().collect(Collectors.toMap(
                SpeciesDefinitions.Definition::nationalPokedexNumber, Function.identity()));
        var tokens = requested.split(",");
        if (tokens.length == 1 && tokens[0].trim().equalsIgnoreCase("all")) return all;

        var selected = new LinkedHashSet<String>();
        for (String raw : tokens) {
            String token = raw.trim().toLowerCase(Locale.ROOT);
            if (token.startsWith("#")) token = token.substring(1);
            if (token.isBlank()) throw new IllegalArgumentException("Empty species in clientSmokeSpecies");
            SpeciesDefinitions.Definition definition;
            if (token.matches("\\d+")) {
                definition = byNumber.get(Integer.parseInt(token));
            } else {
                String slug = token.replace("'", "").replace("-", "").replace(" ", "");
                definition = bySlug.get(slug);
            }
            if (definition == null) {
                throw new IllegalArgumentException("Unknown client smoke species '" + raw.trim()
                        + "'; use a National Pokédex number or species slug");
            }
            selected.add(definition.slug());
        }
        if (selected.isEmpty()) throw new IllegalArgumentException("clientSmokeSpecies selected no species");
        return ordered.stream().map(SpeciesDefinitions.Definition::slug)
                .filter(selected::contains).toList();
    }
}
