package br.com.petrickfaria.minemon.entity;
import net.minecraft.nbt.CompoundTag;
/** Sequential, effective-tick recharge; unloading and defeat do not advance it. */
public final class CombatCharges {
 public static final int CAPACITY=3,RECHARGE_TICKS=180;
 private int charges=CAPACITY,elapsed;
 public int charges(){return charges;} public int elapsed(){return elapsed;}
 public boolean consume(){if(charges==0)return false;charges--;return true;}
 public void tick(boolean active){if(!active||charges==CAPACITY)return;if(++elapsed==RECHARGE_TICKS){charges++;elapsed=0;}}
 public void save(CompoundTag n){n.putInt("MinemonRangedCharges",charges);n.putInt("MinemonRangedRecharge",elapsed);}
 public void load(CompoundTag n){charges=n.contains("MinemonRangedCharges")?Math.max(0,Math.min(CAPACITY,n.getInt("MinemonRangedCharges"))):CAPACITY;elapsed=charges==CAPACITY?0:Math.max(0,Math.min(RECHARGE_TICKS-1,n.getInt("MinemonRangedRecharge")));}
}
