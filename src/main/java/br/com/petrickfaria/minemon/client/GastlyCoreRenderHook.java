package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.Comparator;

/** Redraws Gastly after translucent particles so smoke cannot cover its opaque core. */
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class GastlyCoreRenderHook {
    private static int redraws;
    private static double maxCameraDistanceForValidation;
    public static int redrawsForValidation(){return redraws;}
    public static double maxCameraDistanceForValidation(){return maxCameraDistanceForValidation;}

    @SubscribeEvent
    public static void renderCoreAfterParticles(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null)return;
        var dispatcher=mc.getEntityRenderDispatcher();
        var camera=event.getCamera().getPosition();
        var buffers=mc.renderBuffers().bufferSource();
        var gastlys=new ArrayList<MinemonCreature>();
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof MinemonCreature gastly)||!gastly.getSpeciesSlug().equals("gastly")
                    ||!gastly.isAlive()||gastly.isRemoved())continue;
            // Do not draw detached-looking aura billboards from a Gastly whose
            // body is outside the current camera frustum.
            if(!event.getFrustum().isVisible(gastly.getBoundingBox()))continue;
            gastlys.add(gastly);
            maxCameraDistanceForValidation=Math.max(maxCameraDistanceForValidation,
                    Math.sqrt(gastly.distanceToSqr(camera.x,camera.y,camera.z)));
        }
        if(gastlys.isEmpty())return;
        if(br.com.petrickfaria.minemon.client.GastlySmokeParticle.auraSpritesReady()){
            var draws=new ArrayList<AuraDraw>();
            for(var gastly:gastlys){
                double distanceSqr=gastly.distanceToSqr(camera.x,camera.y,camera.z);
                var bounds=gastly.getBoundingBox();
                double dx=Math.max(0.0D,Math.max(bounds.minX-camera.x,camera.x-bounds.maxX));
                double dy=Math.max(0.0D,Math.max(bounds.minY-camera.y,camera.y-bounds.maxY));
                double dz=Math.max(0.0D,Math.max(bounds.minZ-camera.z,camera.z-bounds.maxZ));
                double distanceToBodySqr=dx*dx+dy*dy+dz*dz;
                if(distanceToBodySqr>br.com.petrickfaria.minemon.entity.GastlyAura.MAX_RENDER_DISTANCE
                        *br.com.petrickfaria.minemon.entity.GastlyAura.MAX_RENDER_DISTANCE)continue;
                int tick=gastly.tickCount;
                for(int slot=0;slot<5;slot++)addDraw(draws,gastly,slot,false,slot*3,tick,distanceSqr);
                for(int slot=5;slot<12;slot++)addDraw(draws,gastly,slot,true,1+(slot-5)*2,tick,distanceSqr);
            }
            if(!draws.isEmpty()){
                var tessellator=com.mojang.blaze3d.vertex.Tesselator.getInstance();
                var builder=tessellator.getBuilder();
                GastlySmokeParticle.beginAuraFrame(builder,mc.getTextureManager());
                try{
                    draws.sort(Comparator.comparingDouble(AuraDraw::distanceSqr).reversed());
                    for(var draw:draws)GastlySmokeParticle.renderAuraParticle(draw.owner(),draw.slot(),draw.small(),
                            draw.age(),event.getPartialTick(),event.getCamera(),builder,draw.distanceSqr());
                }finally{GastlySmokeParticle.endAuraFrame(tessellator);}
            }
        }
        for(var gastly:gastlys){
            dispatcher.render(gastly,gastly.getX()-camera.x,gastly.getY()-camera.y,gastly.getZ()-camera.z,
                    gastly.getYRot(),event.getPartialTick(),event.getPoseStack(),buffers,
                    dispatcher.getPackedLightCoords(gastly,event.getPartialTick()));
        }
        if(!gastlys.isEmpty()){
            buffers.endBatch();
            if(Boolean.getBoolean("minemon.clientSmokeTest"))redraws++;
        }
    }
    private static void addDraw(ArrayList<AuraDraw> draws,MinemonCreature owner,int slot,boolean small,
                                int phase,int tick,double distanceSqr){
        int age;
        if(tick<16){age=tick-phase;if(age<0||age>=br.com.petrickfaria.minemon.entity.GastlyAura.LIFETIME)return;}
        else age=Math.floorMod(tick-phase,br.com.petrickfaria.minemon.entity.GastlyAura.LIFETIME);
        draws.add(new AuraDraw(owner,slot,small,age,distanceSqr));
    }
    private record AuraDraw(MinemonCreature owner,int slot,boolean small,int age,double distanceSqr){}
    private GastlyCoreRenderHook(){}
}
