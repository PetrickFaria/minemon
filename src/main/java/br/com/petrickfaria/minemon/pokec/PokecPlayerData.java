package br.com.petrickfaria.minemon.pokec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
    public static int stagedCoins(ServerPlayer p){return Math.max(0,data(p).getInt("stagedCoins"));}
    public static int stagedSouls(ServerPlayer p){return Math.max(0,data(p).getInt("stagedSouls"));}
    public static String stagedSoulType(ServerPlayer p){return data(p).getString("stagedSoulType");}
    public static void setStagedCoins(ServerPlayer p,int value){data(p).putInt("stagedCoins",Math.max(0,value));}
    public static void setStagedSouls(ServerPlayer p,int value,String type){CompoundTag d=data(p);d.putInt("stagedSouls",Math.max(0,value));d.putString("stagedSoulType",value>0?type:"");}
    public static ItemStack conversionBall(ServerPlayer p){CompoundTag d=data(p);return d.contains("conversionBall",CompoundTag.TAG_COMPOUND)?ItemStack.of(d.getCompound("conversionBall")):ItemStack.EMPTY;}
    public static ItemStack conversionOutput(ServerPlayer p){CompoundTag d=data(p);return d.contains("conversionOutput",CompoundTag.TAG_COMPOUND)?ItemStack.of(d.getCompound("conversionOutput")):ItemStack.EMPTY;}
    public static int conversionResult(ServerPlayer p){return data(p).getInt("conversionResult");}
    public static void setConversion(ServerPlayer p,ItemStack ball,ItemStack output,int result){
        CompoundTag d=data(p);
        if(ball.isEmpty())d.remove("conversionBall");else d.put("conversionBall",ball.save(new CompoundTag()));
        if(output.isEmpty())d.remove("conversionOutput");else d.put("conversionOutput",output.save(new CompoundTag()));
        d.putInt("conversionResult",Math.max(-1,result));
    }
    public static void clearStaged(ServerPlayer p){CompoundTag d=data(p);d.putInt("stagedCoins",0);d.putInt("stagedSouls",0);d.putString("stagedSoulType","");d.remove("conversionBall");d.remove("conversionOutput");d.putInt("conversionResult",-1);}
    public static void copyOnRespawn(net.minecraft.world.entity.player.Player oldPlayer,net.minecraft.world.entity.player.Player newPlayer){
        if(oldPlayer.getPersistentData().contains(KEY))newPlayer.getPersistentData().put(KEY,oldPlayer.getPersistentData().getCompound(KEY).copy());
    }
    private PokecPlayerData(){}
}
