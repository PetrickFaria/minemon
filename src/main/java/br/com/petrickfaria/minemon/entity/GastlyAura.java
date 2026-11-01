package br.com.petrickfaria.minemon.entity;
/** A 16-tick population window contains five large and seven small sprites. */
public final class GastlyAura {
 public static final int LIFETIME=16;
 public static final float LARGE_SIZE=.85F,SMALL_SIZE=.34F,OPACITY=.75F;
 private static final float[] CLOUD_X={-.48F,.42F,-.78F,.74F,-.18F,.85F,-.91F,.18F,.54F,-.46F,.93F,-.68F};
 private static final float[] CLOUD_Y={.42F,.78F,.08F,.30F,-.36F,.62F,.51F,-.08F,.92F,.18F,-.24F,.76F};
 private static final float[] CLOUD_Z={-.38F,-.62F,.03F,.02F,-.12F,-.06F,-.14F,-.80F,-.04F,-.26F,-.58F,.01F};
 public static int largeSlot(int tick){return switch(Math.floorMod(tick,LIFETIME)){case 0->0;case 3->1;case 6->2;case 9->3;case 12->4;default->-1;};}
 public static int smallSlot(int tick){int phase=Math.floorMod(tick,LIFETIME);return phase%2==1&&phase<14?phase/2+5:-1;}
 public static float cloudX(int slot){return CLOUD_X[Math.floorMod(slot,CLOUD_X.length)];}
 public static float cloudY(int slot){return CLOUD_Y[Math.floorMod(slot,CLOUD_Y.length)];}
 public static float cloudZ(int slot){return CLOUD_Z[Math.floorMod(slot,CLOUD_Z.length)];}
 public static float sizeVariation(int slot){return .82F+Math.floorMod(slot*37+11,19)*.02F;}
 public static float driftPhase(int slot){return Math.floorMod(slot*73+19,101)*.061F;}
 private GastlyAura(){}
}
