package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Redraws Gastly after translucent particles so smoke cannot cover its opaque core. */
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class GastlyCoreRenderHook {
    private static int redraws;
    public static int redrawsForValidation(){return redraws;}

    @SubscribeEvent
    public static void renderCoreAfterParticles(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        var dispatcher=mc.getEntityRenderDispatcher();
        var camera=event.getCamera().getPosition();
        var buffers=mc.renderBuffers().bufferSource();
        boolean rendered=false;
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof MinemonCreature gastly)||!gastly.getSpeciesSlug().equals("gastly")
                    ||!gastly.isAlive()||gastly.isRemoved())continue;
            dispatcher.render(gastly,gastly.getX()-camera.x,gastly.getY()-camera.y,gastly.getZ()-camera.z,
                    gastly.getYRot(),event.getPartialTick(),event.getPoseStack(),buffers,
                    dispatcher.getPackedLightCoords(gastly,event.getPartialTick()));
            rendered=true;
        }
        if(rendered){
            buffers.endBatch();
            if(Boolean.getBoolean("minemon.clientSmokeTest"))redraws++;
        }
    }
    private GastlyCoreRenderHook(){}
}
