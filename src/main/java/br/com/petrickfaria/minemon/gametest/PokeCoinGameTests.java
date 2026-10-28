package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Minemon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PokeCoinGameTests {
    @GameTest(template = "test_arena", timeoutTicks = 100)
    public static void pokeCoinIsRegisteredStackableAndCreative(GameTestHelper helper) {
        var coin = ModItems.POKE_COIN.get();
        var id = ForgeRegistries.ITEMS.getKey(coin);
        if (id == null || !id.toString().equals("minemon:poke_coin"))
            helper.fail("PokéCoin must be registered as minemon:poke_coin");
        if (coin.getMaxStackSize() != 64)
            helper.fail("PokéCoin must stack to 64");

        NonNullList<ItemStack> tab = NonNullList.create();
        ModItems.MINEMON_TAB.fillItemList(tab);
        long entries = tab.stream().filter(stack -> stack.getItem() == coin).count();
        if (entries != 1)
            helper.fail("PokéCoin must appear exactly once in the Minemon creative tab");

        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, new ItemStack(coin, 64));
        if (inventory.getItem(0).getCount() != 64)
            helper.fail("PokéCoin must be storable as a full stack in an inventory");
        helper.succeed();
    }

    private PokeCoinGameTests() {}
}
