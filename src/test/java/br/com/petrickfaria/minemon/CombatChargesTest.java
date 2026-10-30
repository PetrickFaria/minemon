package br.com.petrickfaria.minemon;
import br.com.petrickfaria.minemon.entity.CombatCharges;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;
public class CombatChargesTest {
 @Test public void threeSequentialChargesTakeExactly540ActiveTicks(){
  var c=new CombatCharges();assertTrue(c.consume());assertTrue(c.consume());assertTrue(c.consume());assertFalse(c.consume());
  for(int stage=0;stage<3;stage++){
   for(int t=0;t<179;t++)c.tick(true);assertEquals(stage,c.charges());c.tick(true);assertEquals(stage+1,c.charges());
  }
  for(int t=0;t<1000;t++)c.tick(true);assertEquals(3,c.charges());assertEquals(0,c.elapsed());
 }
 @Test public void defeatAndReloadPreservePartialRecharge(){
  var c=new CombatCharges();c.consume();for(int t=0;t<79;t++)c.tick(true);
  for(int t=0;t<1000;t++)c.tick(false);assertEquals(79,c.elapsed());assertEquals(2,c.charges());
  var n=new CompoundTag();c.save(n);var restored=new CombatCharges();restored.load(n);
  for(int t=0;t<100;t++)restored.tick(true);assertEquals(2,restored.charges());restored.tick(true);assertEquals(3,restored.charges());
 }
}
