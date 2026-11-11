package br.com.petrickfaria.minemon.entity;

import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import static org.junit.Assert.*;

public class MobTargetCategoriesTest {
    @Test public void registryNamespaceMapsToTheRequestedSourceGroups() {
        assertEquals(MobTargetCategories.Origin.VANILLA, MobTargetCategories.origin(new ResourceLocation("minecraft", "cow")));
        assertEquals(MobTargetCategories.Origin.MINEMON, MobTargetCategories.origin(new ResourceLocation("minemon", "pikachu")));
        // This repository's Forge mod id is minemon_gen1; its registered species
        // must still appear in the user-facing Minemon source filter.
        assertEquals(MobTargetCategories.Origin.MINEMON, MobTargetCategories.origin(new ResourceLocation("minemon_gen1", "pikachu")));
        assertEquals(MobTargetCategories.Origin.MODS, MobTargetCategories.origin(new ResourceLocation("examplemod", "mob")));
    }

    @Test public void knownSpeciesUseTheirNationalDexNumberAndUnknownIdsRemainStable() {
        ResourceLocation bulbasaur = new ResourceLocation("minemon_gen1", "bulbasaur");
        ResourceLocation pikachu = new ResourceLocation("minemon_gen1", "pikachu");
        ResourceLocation mew = new ResourceLocation("minemon_gen1", "mew");
        assertEquals(1, MobTargetCategories.nationalDexNumber(bulbasaur));
        assertEquals(25, MobTargetCategories.nationalDexNumber(pikachu));
        assertEquals(151, MobTargetCategories.nationalDexNumber(mew));
        assertEquals(-1, MobTargetCategories.nationalDexNumber(new ResourceLocation("minecraft", "pikachu")));
        assertTrue(MobTargetCategories.numericIdOrder().compare(bulbasaur, pikachu) < 0);
        assertTrue(MobTargetCategories.numericIdOrder().compare(pikachu, mew) < 0);
        assertTrue(MobTargetCategories.numericIdOrder().compare(
                new ResourceLocation("alpha", "mob"), new ResourceLocation("zeta", "mob")) < 0);
    }
}
