package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Minemon.MOD_ID);
    public static final RegistryObject<Item> BULBASAUR_SPAWN_EGG = ITEMS.register("bulbasaur_spawn_egg",
            () -> new SpawnEggItem(ModEntities.BULBASAUR, 0x83B75B, 0xE4E6C4, new Item.Properties()));
    public static CreativeModeTab MINEMON_TAB;

    @Mod.EventBusSubscriber(modid = Minemon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Tabs {
        @SubscribeEvent
        public static void registerTabs(CreativeModeTabEvent.Register event) {
            MINEMON_TAB = event.registerCreativeModeTab(new net.minecraft.resources.ResourceLocation(Minemon.MOD_ID, "main"), builder ->
                    builder.title(net.minecraft.network.chat.Component.translatable("itemGroup.minemon"))
                            .icon(() -> BULBASAUR_SPAWN_EGG.get().getDefaultInstance())
                            .displayItems((parameters, output) -> output.accept(BULBASAUR_SPAWN_EGG.get())));
        }
    }

    private ModItems() {}
}
