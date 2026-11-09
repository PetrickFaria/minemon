package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.StencilManager;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

/** Records the normal, single core draw so its own foreground smoke cannot cover it. */
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID, value=Dist.CLIENT)
public final class GastlyCoreMask {
    private static final int BIT=StencilManager.reserveBit();
    private static final int MASK=BIT>=0 && BIT<8 ? 1<<BIT : 0;
    private static final java.util.Map<ResourceLocation,RenderType> TYPES=new java.util.HashMap<>();
    private static long verifiedPixels;
    public static long verifiedPixelsForValidation(){return verifiedPixels;}

    static Snapshot captureForValidation(int frame) {
        if(!Boolean.getBoolean("minemon.clientSmokeGhostLine") || frame%60!=0)return null;
        var image=net.minecraft.client.Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
        var stencil=org.lwjgl.BufferUtils.createByteBuffer(image.getWidth()*image.getHeight());
        int alignment=GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);
        try { GL11.glReadPixels(0,0,image.getWidth(),image.getHeight(),GL11.GL_STENCIL_INDEX,GL11.GL_UNSIGNED_BYTE,stencil); }
        finally { GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,alignment); }
        return new Snapshot(image,stencil);
    }
    static final class Snapshot implements AutoCloseable {
        private final com.mojang.blaze3d.platform.NativeImage before;
        private final java.nio.ByteBuffer stencil;
        Snapshot(com.mojang.blaze3d.platform.NativeImage before,java.nio.ByteBuffer stencil){this.before=before;this.stencil=stencil;}
        void verify() {
            try(var after=net.minecraft.client.Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                for(int y=0;y<before.getHeight();y++)for(int x=0;x<before.getWidth();x++) {
                    if((stencil.get((before.getHeight()-1-y)*before.getWidth()+x)&MASK)==0)continue;
                    if(before.getPixelRGBA(x,y)!=after.getPixelRGBA(x,y))
                        throw new IllegalStateException("Gastly aura changed an opaque core pixel at "+x+","+y);
                    verifiedPixels++;
                }
            }
        }
        @Override public void close(){before.close();}
    }

    @SubscribeEvent public static void prepare(TickEvent.RenderTickEvent event) {
        // Enabling stencil reallocates the framebuffer: do it before drawing the frame.
        if(event.phase==TickEvent.Phase.START && MASK!=0)
            Minecraft.getInstance().getMainRenderTarget().enableStencil();
    }
    @SubscribeEvent public static void clear(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY || MASK==0)return;
        RenderSystem.stencilMask(MASK);
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.stencilMask(0xFF);
    }
    static RenderType core(ResourceLocation texture) {
        if(MASK==0)return RenderType.entityCutoutNoCull(texture);
        return TYPES.computeIfAbsent(texture,t -> new MaskedCore(RenderType.entityCutoutNoCull(t)));
    }
    static void beginAura() {
        if(MASK==0)return;
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0);
        RenderSystem.stencilFunc(GL11.GL_NOTEQUAL,MASK,MASK);
        RenderSystem.stencilOp(GL11.GL_KEEP,GL11.GL_KEEP,GL11.GL_KEEP);
    }
    static void endAura() {
        if(MASK==0)return;
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.stencilMask(0xFF);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS,0,0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP,GL11.GL_KEEP,GL11.GL_KEEP);
    }
    private static final class MaskedCore extends RenderType {
        MaskedCore(RenderType base) {
            super("minemon_gastly_core",base.format(),base.mode(),base.bufferSize(),true,false,()->{
                base.setupRenderState();
                GL11.glEnable(GL11.GL_STENCIL_TEST);
                RenderSystem.stencilMask(MASK);
                RenderSystem.stencilFunc(GL11.GL_ALWAYS,MASK,MASK);
                // Only opaque texels that pass world depth mark the core silhouette.
                RenderSystem.stencilOp(GL11.GL_KEEP,GL11.GL_KEEP,GL11.GL_REPLACE);
            },()->{
                endAura();
                base.clearRenderState();
            });
        }
    }
    private GastlyCoreMask() {}
}
