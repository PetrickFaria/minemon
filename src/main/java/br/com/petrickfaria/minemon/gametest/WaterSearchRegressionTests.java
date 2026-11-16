package br.com.petrickfaria.minemon.gametest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
@GameTestHolder("minemon_navigation") @PrefixGameTestTemplate(false)
public final class WaterSearchRegressionTests {
 @GameTest(template="test_arena",templateNamespace="minemon_navigation",batch="followup_inaccessible_neighbor_water",timeoutTicks=2600)
 public static void inaccessibleNeighborWaterMustNotDisplaceReachableDistantPond(GameTestHelper h){
  // The nearby deep reservoir has no walkable connection to the elevated
  // corridor. Its many wet cells must not starve the unchanged real-AI proof
  // that the seeker physically reaches water more than ninety blocks away.
  for(int x=-40;x<=-20;x++)for(int z=0;z<=15;z++)for(int y=0;y<=18;y++)
   h.setBlock(new BlockPos(x,y,z),(y==0||x==-40||x==-20||z==0||z==15?Blocks.STONE:Blocks.WATER));
  IndividualBehaviorTests.aquaticSeekerPhysicallyReachesWaterBeyondNinetyBlocks(h);
 }
}
