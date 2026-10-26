package br.com.petrickfaria.minemon.sound;

import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

/** Creates the per-species user audio folders without touching their contents. */
public final class CustomSoundFolders {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void createAll() {
        try {
            Path root = root();
            Path modRoot = root.getParent();
            if (Files.isSymbolicLink(modRoot)) throw new IOException("Minemon config directory must not be a symbolic link");
            Files.createDirectories(modRoot);
            if (Files.isSymbolicLink(root)) throw new IOException("Sounds root must not be a symbolic link");
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root) || !Files.isDirectory(root, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Sounds root must be a real directory, not a symbolic link");
            var species = SpeciesDefinitions.ALL.values().stream()
                    .sorted(Comparator.comparingInt(SpeciesDefinitions.Definition::nationalPokedexNumber)).toList();
            if (species.size() != 151) throw new IllegalStateException("Expected 151 registered Kanto species, got " + species.size());
            for (var definition : species) {
                Path folder = folder(definition);
                if (!folder.startsWith(root)) throw new IllegalStateException("Unsafe species folder " + folder);
                if (Files.isSymbolicLink(folder)) {
                    LOGGER.warn("Ignoring symbolic link in Minemon sound folders: {}", folder);
                    continue;
                }
                Files.createDirectories(folder);
            }
            LOGGER.info("Minemon custom audio folders ready: {} species at {}", species.size(), root);
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Could not create Minemon custom audio folders", exception);
        }
    }

    public static Path root() { return FMLPaths.CONFIGDIR.get().resolve("minemon").resolve("sounds").normalize(); }

    public static Path folder(SpeciesDefinitions.Definition definition) {
        return root().resolve(folderName(definition)).normalize();
    }

    public static String folderName(SpeciesDefinitions.Definition definition) {
        String safeName = definition.name().replaceAll("[<>:\"/\\\\|?*\\p{Cntrl}]", "_").replaceAll("[ .]+$", "");
        return String.format(Locale.ROOT, "%03d_%s", definition.nationalPokedexNumber(), safeName);
    }

    /** OGG has fixed priority. Within a format, use the first filename in lexical order. */
    public static Path findAudio(String slug) throws IOException {
        var definition = SpeciesDefinitions.get(slug);
        if (definition == null) return null;
        return selectAudio(root(), folder(definition));
    }

    public static Path selectAudio(Path root, Path directory) throws IOException {
        Path normalizedRoot = root.normalize();
        Path normalizedDirectory = directory.normalize();
        if (Files.isSymbolicLink(normalizedRoot) || Files.isSymbolicLink(normalizedDirectory) || !normalizedDirectory.startsWith(normalizedRoot)
                || !Files.isDirectory(normalizedRoot, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(normalizedDirectory, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return null;
        for (String extension : new String[]{".ogg", ".mp3"}) {
            try (Stream<Path> paths = Files.list(normalizedDirectory)) {
                Path selected = paths.filter(path -> path.getParent().equals(normalizedDirectory))
                        .filter(path -> Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .findFirst().orElse(null);
                if (selected != null && selected.normalize().startsWith(normalizedDirectory)) return selected;
            }
        }
        return null;
    }

    private CustomSoundFolders() {}
}
