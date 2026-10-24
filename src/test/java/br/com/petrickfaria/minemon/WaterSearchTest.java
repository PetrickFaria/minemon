package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.WaterMovementGoal;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.Test;
import static org.junit.Assert.*;

public final class WaterSearchTest {
 @Test public void searchRadiusIsExactlyOneHundredBlocks(){
  var origin=BlockPos.ZERO;
  assertTrue(WaterMovementGoal.withinSearchRadius(origin,new BlockPos(100,0,0)));
  assertFalse(WaterMovementGoal.withinSearchRadius(origin,new BlockPos(101,0,0)));
 }

 @Test public void threeByThreeByThreeVolumeIsRequiredForVisitorsOnly(){
  assertTrue(WaterMovementGoal.requiresNavigableVolume("squirtle"));
  assertFalse(WaterMovementGoal.requiresNavigableVolume("goldeen"));
  assertFalse(WaterMovementGoal.requiresNavigableVolume("gyarados"));
 }

 @Test public void targetRequiresACompleteContiguousThreeByThreeByThreeWaterVolume(){
  var center=BlockPos.ZERO;Set<BlockPos> water=new HashSet<>();
  for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)water.add(center.offset(x,y,z));
  assertEquals(27,water.size());
  assertTrue(WaterMovementGoal.isNavigableWaterRegion(center,water::contains));
  water.remove(center.offset(1,1,1));
  assertFalse(WaterMovementGoal.isNavigableWaterRegion(center,water::contains));
 }
}
