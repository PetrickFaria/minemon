package br.com.petrickfaria.minemon.menu;
import br.com.petrickfaria.minemon.Minemon;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,Minemon.MOD_ID);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<PokecMenu>> POKEC=MENUS.register("pokec",()->IForgeMenuType.create(PokecMenu::new));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<RocketTradeMenu>> ROCKET_TRADE=MENUS.register("rocket_trade",()->IForgeMenuType.create(RocketTradeMenu::new));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<PokemonInfoMenu>> POKEMON_INFO=MENUS.register("pokemon_info",()->IForgeMenuType.create(PokemonInfoMenu::new));
    private ModMenus(){}
}
