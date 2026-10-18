package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final CreativeModeTab MINEMON_TAB = new CreativeModeTab("minemon") {
        @Override
        public ItemStack makeIcon() {
            return BULBASAUR_SPAWN_EGG.get().getDefaultInstance();
        }
    };

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Minemon.MOD_ID);
    public static final RegistryObject<Item> BULBASAUR_SPAWN_EGG = ITEMS.register("bulbasaur_spawn_egg",
            () -> new SpawnEggItem(ModEntities.BULBASAUR.get(), 0x83B75B, 0xE4E6C4,
                    new Item.Properties().tab(MINEMON_TAB)));

    private ModItems() {}
}
