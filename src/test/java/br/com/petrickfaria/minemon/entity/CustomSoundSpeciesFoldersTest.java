package br.com.petrickfaria.minemon.entity;

import br.com.petrickfaria.minemon.sound.CustomSoundFolders;
import org.junit.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CustomSoundSpeciesFoldersTest {
    @Test public void layoutUsesAll151CanonicalPokedexDefinitions() {
        assertEquals(151, SpeciesDefinitions.ALL.size());
        Set<String> folders = new HashSet<>();
        var definitions = SpeciesDefinitions.ALL.values().stream()
                .sorted(java.util.Comparator.comparingInt(SpeciesDefinitions.Definition::nationalPokedexNumber)).toList();
        for (int i = 0; i < definitions.size(); i++) {
            var definition = definitions.get(i);
            assertEquals(i + 1, definition.nationalPokedexNumber());
            assertTrue(folders.add(CustomSoundFolders.folderName(definition)));
        }
        assertEquals("001_Bulbasaur", CustomSoundFolders.folderName(definitions.get(0)));
        assertEquals("025_Pikachu", CustomSoundFolders.folderName(definitions.get(24)));
        assertEquals("092_Gastly", CustomSoundFolders.folderName(definitions.get(91)));
        assertEquals("151_Mew", CustomSoundFolders.folderName(definitions.get(150)));
        assertEquals(151, folders.size());
    }
}
