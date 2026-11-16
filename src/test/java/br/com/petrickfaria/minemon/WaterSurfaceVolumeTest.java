package br.com.petrickfaria.minemon;

import br.com.petrickfaria.minemon.entity.WaterMovementGoal;
import net.minecraft.core.BlockPos;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaterSurfaceVolumeTest {
    @Test public void surfaceVisitorProbeExcludesTheAirLayer(){
        int y=WaterMovementGoal.navigableVolumeCenterY(4,1,5).orElseThrow();
        assertEquals(3,y);
        assertTrue(WaterMovementGoal.isNavigableWaterRegion(new BlockPos(5,y,5),p->p.getY()>=1&&p.getY()<=4));
    }
    @Test public void bottomProbeExcludesTheSolidFloor(){
        int y=WaterMovementGoal.navigableVolumeCenterY(1,1,5).orElseThrow();
        assertEquals(2,y);
        assertTrue(WaterMovementGoal.isNavigableWaterRegion(new BlockPos(5,y,5),p->p.getY()>=1&&p.getY()<=4));
    }
    @Test public void midwaterProbeKeepsItsRequestedDepth(){
        assertEquals(8,WaterMovementGoal.navigableVolumeCenterY(8,1,16).orElseThrow());
    }
    @Test public void shallowWaterDoesNotGainAnUnsupportedNavigationVolume(){
        assertTrue(WaterMovementGoal.navigableVolumeCenterY(2,1,3).isEmpty());
    }
}
