package br.com.petrickfaria.minemon.registry;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.block.PokecBlock;
import br.com.petrickfaria.minemon.block.RocketBlock;
import br.com.petrickfaria.minemon.block.BreedingEggBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,Minemon.MOD_ID);
    public static final RegistryObject<Block> POKEC=BLOCKS.register("pokec",()->new PokecBlock(Block.Properties.of(Material.METAL).strength(4.0F,6.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> BREEDING_EGG=BLOCKS.register("breeding_egg",BreedingEggBlock::new);
    public static final RegistryObject<Block> ROCKET_BLOCK=BLOCKS.register("rocket_block",()->new RocketBlock(Block.Properties.of(Material.METAL).strength(4.0F,6.0F).sound(SoundType.METAL)));
    private ModBlocks(){}
}
