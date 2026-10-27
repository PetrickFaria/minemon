package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Per-player durable PokéC state lives in Forge's player persistent data compound. */
public final class PokecPlayerData {
    private static final String KEY="minemon_gen1_pokec";
    private static CompoundTag data(ServerPlayer p){
        CompoundTag root=p.getPersistentData();
        CompoundTag d=root.getCompound(KEY);
        CompoundTag unlocked=d.getCompound("unlocked");
        for(String starter:PokecEconomy.get().starters()) {
            if(PokecSpecies.get(starter)!=null)unlocked.putBoolean(starter,true);
        }
        CompoundTag deposits=d.getCompound("deposits");
        for(var species:PokecSpecies.ALL.values()){
            if(!species.isEvolution()&&deposits.getInt(species.slug())>=PokecEconomy.get().souls(species.slug()))unlocked.putBoolean(species.slug(),true);
        }
        d.put("unlocked",unlocked);root.put(KEY,d);return d;
    }
    public static int coins(ServerPlayer p){return Math.max(0,data(p).getInt("coins"));}
    public static void setCoins(ServerPlayer p,int amount){data(p).putInt("coins",Math.max(0,amount));}
    public static boolean isUnlocked(ServerPlayer p,String slug){return data(p).getCompound("unlocked").getBoolean(slug);}
    public static void unlock(ServerPlayer p,String slug){if(PokecSpecies.get(slug)!=null)data(p).getCompound("unlocked").putBoolean(slug,true);}
    public static Set<String> unlocked(ServerPlayer p){CompoundTag tag=data(p).getCompound("unlocked");Set<String> out=new HashSet<>();for(String s:PokecSpecies.ALL.keySet())if(tag.getBoolean(s))out.add(s);return out;}
    public static int deposited(ServerPlayer p,String slug){return data(p).getCompound("deposits").getInt(slug);}
    public static int addDeposit(ServerPlayer p,String slug){CompoundTag d=data(p);CompoundTag deposits=d.getCompound("deposits");int n=deposits.getInt(slug)+1;deposits.putInt(slug,n);d.put("deposits",deposits);return n;}
    public static boolean usedFreePurchase(ServerPlayer p){return data(p).getBoolean("freePurchaseUsed");}
    public static void setFreePurchaseUsed(ServerPlayer p){data(p).putBoolean("freePurchaseUsed",true);}
    public static void copyOnRespawn(net.minecraft.world.entity.player.Player oldPlayer,net.minecraft.world.entity.player.Player newPlayer){
        if(oldPlayer.getPersistentData().contains(KEY))newPlayer.getPersistentData().put(KEY,oldPlayer.getPersistentData().getCompound(KEY).copy());
    }
    private PokecPlayerData(){}
}
