package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.config.MinemonConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

/** A scoped config override that cannot be replaced by the file watcher's reload. */
final class GameTestCommonConfig implements AutoCloseable {
    private final CommentedConfig original;

    private GameTestCommonConfig() {
        original=ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.COMMON).stream()
                .filter(config->config.getSpec()==MinemonConfig.SPEC).findFirst().orElseThrow().getConfigData();
        MinemonConfig.SPEC.setConfig(copy(original));
    }

    static GameTestCommonConfig isolate() { return new GameTestCommonConfig(); }

    private static CommentedConfig copy(UnmodifiableConfig source) {
        var result=CommentedConfig.inMemory();
        source.valueMap().forEach((key,value)->result.set(java.util.List.of(key),
                value instanceof UnmodifiableConfig nested ? copy(nested) : value));
        return result;
    }

    @Override public void close() { MinemonConfig.SPEC.setConfig(original); }
}
