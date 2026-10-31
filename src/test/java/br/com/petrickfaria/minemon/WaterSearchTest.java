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

 @Test public void threeByThreeByThreeVolumeIsRequiredForEveryAquaticProfile(){
  assertTrue(WaterMovementGoal.requiresNavigableVolume("squirtle"));
  assertTrue(WaterMovementGoal.requiresNavigableVolume("goldeen"));
  assertTrue(WaterMovementGoal.requiresNavigableVolume("gyarados"));
 }

 @Test public void targetRequiresACompleteContiguousThreeByThreeByThreeWaterVolume(){
  var center=BlockPos.ZERO;Set<BlockPos> water=new HashSet<>();
  for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)water.add(center.offset(x,y,z));
  assertEquals(27,water.size());
  assertTrue(WaterMovementGoal.isNavigableWaterRegion(center,water::contains));
  water.remove(center.offset(1,1,1));
  assertFalse(WaterMovementGoal.isNavigableWaterRegion(center,water::contains));
 }
 @Test public void wallClearanceUsesTheEntityBoundsAndIgnoresBlocksBelowTheBody(){
  var body=new net.minecraft.world.phys.AABB(0,2,0,2,4,2);
  var wall=new net.minecraft.world.phys.AABB(7,2,0,8,4,2);
  var floor=new net.minecraft.world.phys.AABB(0,0,0,2,1,2);
  assertEquals(5.0D,WaterMovementGoal.horizontalWallClearance(body,java.util.List.of(wall,floor)),0.0001D);
 }

 @Test public void closeVerticalObstaclesDoNotCountAsLateralWalls(){
  var body=new net.minecraft.world.phys.AABB(0,2,0,1,3,1);
  var ceiling=new net.minecraft.world.phys.AABB(0,4,0,1,5,1);
  assertTrue(Double.isInfinite(WaterMovementGoal.horizontalWallClearance(body,java.util.List.of(ceiling))));
  assertEquals(5.0D,WaterMovementGoal.WALL_PREFERENCE_DISTANCE,0.0D);
 }

}
