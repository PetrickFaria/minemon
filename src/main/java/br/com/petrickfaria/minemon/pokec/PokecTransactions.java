package br.com.petrickfaria.minemon.pokec;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.menu.PokecMenu;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** All economy mutations are performed after validating the live menu on the server. */
public final class PokecTransactions {
    public enum Action { DEPOSIT, PURCHASE }
    private static final Map<UUID,Set<Long>> RECENT_IDS=new ConcurrentHashMap<>();
    public static boolean perform(ServerPlayer player,BlockPos pos,String slug,Action action,long id){
        if(!(player.containerMenu instanceof PokecMenu menu)||!menu.pos().equals(pos)||!menu.stillValid(player))return false;
        if(player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!PokecSpecies.ALL.containsKey(slug))return false;
        if(!remember(player.getUUID(),id))return false;
        synchronized(player.getPersistentData()){
            PokecSpecies.Entry species=PokecSpecies.get(slug);
            if(action==Action.DEPOSIT)return deposit(player,species);
            return purchase(player,species);
        }
    }
    private static boolean deposit(ServerPlayer player,PokecSpecies.Entry species){
        PokecEconomy economy=PokecEconomy.get();
        if(PokecPlayerData.isUnlocked(player,species.slug())||species.isEvolution())return false;
        SoulType required=SoulType.fromSpeciesType(species.primaryType());if(required==null)return false;
        ItemStack hand=matchingSoul(player.getMainHandItem(),required)?player.getMainHandItem():
                matchingSoul(player.getOffhandItem(),required)?player.getOffhandItem():ItemStack.EMPTY;
        if(hand.isEmpty())return false;
        int count=PokecPlayerData.deposited(player,species.slug());if(count>=economy.souls(species.slug()))return false;
        if(!player.getAbilities().instabuild)hand.shrink(1);
        if(PokecPlayerData.addDeposit(player,species.slug())>=economy.souls(species.slug()))PokecPlayerData.unlock(player,species.slug());
        return true;
    }
    private static boolean matchingSoul(ItemStack stack,SoulType type){return !stack.isEmpty()&&stack.getItem() instanceof SoulItem soul&&soul.soulType()==type;}
    private static boolean purchase(ServerPlayer player,PokecSpecies.Entry species){
        if(!PokecPlayerData.isUnlocked(player,species.slug()))return false;
        if(!(player.level instanceof ServerLevel level))return false;
        var type=ModEntities.SPECIES.get(species.slug());if(type==null)return false;
        MinemonCreature creature=type.get().create(level);if(creature==null)return false;
        BlockPos safe=findSafePosition(level,player,creature);if(safe==null){creature.discard();return false;}
        creature.moveTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5,player.getYRot(),0);
        creature.tame(player);creature.setOrderedToSit(true);
        boolean free=PokecEconomy.get().firstPurchaseFree()&&!PokecPlayerData.usedFreePurchase(player);
        int price=PokecEconomy.get().coins(species.slug());
        if(!free&&PokecPlayerData.coins(player)<price){creature.discard();return false;}
        if(!level.addFreshEntity(creature)){creature.discard();return false;}
        // Charge only once the correctly-owned entity has been accepted by the world.
        if(free)PokecPlayerData.setFreePurchaseUsed(player);
        else PokecPlayerData.setCoins(player,PokecPlayerData.coins(player)-price);
        return true;
    }
    private static BlockPos findSafePosition(ServerLevel level,ServerPlayer player,Entity entity){
        BlockPos origin=player.blockPosition();
        for(int radius=1;radius<=3;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int dy=0;dy<=2;dy++){
                BlockPos p=origin.offset(dx,dy,dz);
                if(!level.getWorldBorder().isWithinBounds(p)||p.distToCenterSqr(player.position())<1.5||!level.getBlockState(p).getCollisionShape(level,p).isEmpty())continue;
                AABB box=entity.getBoundingBox().move(p.getX()+.5-entity.getX(),p.getY()-entity.getY(),p.getZ()+.5-entity.getZ());
                if(level.noCollision(entity,box))return p;
            }
        }
        return null;
    }
    /** Called only by server evolution code after confirming the owner's UUID. */
    public static void onOwnedEvolution(MinemonCreature source,String evolvedSlug){
        if(source==null||!source.isTame()||source.getOwnerUUID()==null||!(source.level instanceof ServerLevel level))return;
        PokecSpecies.Entry target=PokecSpecies.get(evolvedSlug);if(target==null||!source.getSpeciesSlug().equals(target.evolvesFrom()))return;
        ServerPlayer player=source.getOwner() instanceof ServerPlayer loadedOwner?loadedOwner:
                level.getServer().getPlayerList().getPlayer(source.getOwnerUUID());
        if(player!=null){PokecPlayerData.unlock(player,evolvedSlug);br.com.petrickfaria.minemon.network.MinemonNetwork.refreshPokec(player);}
    }
    public static void clearTransactions(UUID player){RECENT_IDS.remove(player);}
    private static boolean remember(UUID player,long id){
        return RECENT_IDS.computeIfAbsent(player,k->ConcurrentHashMap.newKeySet()).add(id);
    }
    private PokecTransactions(){}
}
