package br.com.petrickfaria.minemon.entity;
/** A 16-tick population window contains five large and seven small sprites. */
public final class GastlyAura {
 public static final int LIFETIME=16;
 public static final float CORE_RADIUS_SCALE=.55F;
 public static final double PARTICLE_ORBIT_BODY_SCALE=.55D;
 public static final double RADIAL_DISTRIBUTION_SCALE=.85D;
 public static final double MAX_RENDER_DISTANCE=50.0D;
 public static final float LARGE_SIZE=.85F,SMALL_SIZE=.34F,OPACITY=.75F;
 public static int largeSlot(int tick){return switch(Math.floorMod(tick,LIFETIME)){case 0->0;case 3->1;case 6->2;case 9->3;case 12->4;default->-1;};}
 public static int smallSlot(int tick){int phase=Math.floorMod(tick,LIFETIME);return phase%2==1&&phase<14?phase/2+5:-1;}
 /** Historical quad-aware radius retained for compatibility tests and diagnostics. */
 public static double minimumVisibleParticleRadius(double bodyWidth,double quadSize){
  double width=Math.max(0.0D,bodyWidth),quad=Math.max(0.0D,quadSize);
  return Math.max(CORE_RADIUS_SCALE*width,width*.5D+quad*.72D);
 }
 /** Approved orbit; overlapping quads are masked by the normal core silhouette. */
 public static double particleOrbitRadius(double bodyWidth){
  return Math.max(0.0D,bodyWidth)*PARTICLE_ORBIT_BODY_SCALE;
 }
 private GastlyAura(){}
}
