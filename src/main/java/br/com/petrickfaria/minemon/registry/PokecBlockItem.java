package br.com.petrickfaria.minemon.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Literal display name keeps PokéC named even though legacy audited locale files stay byte-identical. */
public final class PokecBlockItem extends BlockItem {
    public PokecBlockItem(Block block,Properties properties){super(block,properties);}
    @Override public Component getName(ItemStack stack){return Component.literal("PokéC");}
}
