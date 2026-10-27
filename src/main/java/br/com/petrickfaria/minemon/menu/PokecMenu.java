package br.com.petrickfaria.minemon.menu;

import br.com.petrickfaria.minemon.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

/** A menu anchor only; every economy action travels through the server-authoritative channel. */
public final class PokecMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    public PokecMenu(int id,Inventory inventory,FriendlyByteBuf buf){this(id,inventory,buf.readBlockPos());}
    public PokecMenu(int id,Inventory inventory,BlockPos pos){super(ModMenus.POKEC.get(),id);this.pos=pos;}
    public BlockPos pos(){return pos;}
    @Override public boolean stillValid(Player player){return stillValid(ContainerLevelAccess.create(player.level,pos),player,ModBlocks.POKEC.get());}
}
