package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public final class CombatGeometryTest {
    @Test public void rangeUsesTheNearestPointOnTheRealTargetHitbox() {
        Vec3 muzzle=new Vec3(0.0D,1.0D,0.5D);
        AABB target=new AABB(4.7D,0.0D,0.2D,5.3D,1.8D,0.8D);
        assertEquals(4.7D*4.7D,CombatGeometry.pointToBoxDistanceSquared(muzzle,target),1.0E-9D);
        assertTrue(CombatGeometry.rayDistanceToBox(muzzle,new Vec3(1.0D,0.0D,0.0D),target)<=5.0D);
        AABB beyond=new AABB(5.0001D,0.0D,0.2D,5.6001D,1.8D,0.8D);
        assertTrue(CombatGeometry.pointToBoxDistanceSquared(muzzle,beyond)>25.0D);
        assertTrue(CombatGeometry.rayDistanceToBox(muzzle,new Vec3(1.0D,0.0D,0.0D),beyond)>5.0D);
    }

    @Test public void closestPointClampsAllAxesAndPreservesPointsInside() {
        AABB box=new AABB(4.7D,0.0D,0.2D,5.3D,1.8D,0.8D);
        assertEquals(new Vec3(4.7D,1.0D,0.5D),CombatGeometry.closestPoint(new Vec3(0.0D,1.0D,0.5D),box));
        Vec3 inside=new Vec3(5.0D,0.9D,0.5D);
        assertEquals(inside,CombatGeometry.closestPoint(inside,box));
    }
}
