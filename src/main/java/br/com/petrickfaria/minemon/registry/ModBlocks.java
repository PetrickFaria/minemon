package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.block.PokecBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Minemon.MOD_ID);
    public static final RegistryObject<Block> POKEC=BLOCKS.register("pokec",()->new PokecBlock(Block.Properties.of(Material.METAL).strength(4.0F,6.0F).sound(SoundType.METAL).noOcclusion()));
    private ModBlocks(){}
}
