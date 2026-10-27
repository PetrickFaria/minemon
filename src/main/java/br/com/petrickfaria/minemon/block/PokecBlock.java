package br.com.petrickfaria.minemon.block;

import br.com.petrickfaria.minemon.menu.ModMenus;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import java.util.List;

/** Two-block machine. Either half opens the same server-validated terminal. */
public final class PokecBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public PokecBlock(BlockBehaviour.Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER).setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HALF,FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        BlockPos p=context.getClickedPos();Level level=context.getLevel();
        if(p.getY()>=level.getMaxBuildHeight()-1||!level.getBlockState(p.above()).canBeReplaced(context))return null;
        return defaultBlockState().setValue(HALF,DoubleBlockHalf.LOWER).setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,net.minecraft.world.item.ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(!level.isClientSide){BlockPos upper=pos.above();if(level.getBlockState(upper).canBeReplaced())level.setBlock(upper,state.setValue(HALF,DoubleBlockHalf.UPPER),Block.UPDATE_ALL);}
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide&&player instanceof ServerPlayer serverPlayer){
            BlockPos base=state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos;
            if(level.getBlockState(base).getBlock()==this)NetworkHooks.openScreen(serverPlayer,new net.minecraft.world.MenuProvider(){
                @Override public Component getDisplayName(){return Component.translatable("container.minemon_gen1.pokec");}
                @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inventory,Player p){return new PokecMenu(id,inventory,base);}
            },buf->buf.writeBlockPos(base));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&state.getValue(HALF)==DoubleBlockHalf.UPPER){
            if(!player.isCreative())
            popResource(level,pos,new net.minecraft.world.item.ItemStack(br.com.petrickfaria.minemon.registry.ModItems.POKEC.get()));
            BlockPos lower=pos.below();if(level.getBlockState(lower).getBlock()==this)level.setBlock(lower,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        }
        super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(state.getBlock()!=next.getBlock()&&!level.isClientSide){BlockPos other=state.getValue(HALF)==DoubleBlockHalf.LOWER?pos.above():pos.below();if(level.getBlockState(other).getBlock()==this)level.setBlock(other,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);}
        super.onRemove(state,level,pos,next,moving);
    }
}
