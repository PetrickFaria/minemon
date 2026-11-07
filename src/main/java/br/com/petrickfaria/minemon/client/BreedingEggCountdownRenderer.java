package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.BreedingEggBlockEntity;
import br.com.petrickfaria.minemon.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Locale;

/** Renders an incubation timer only for the egg currently under the crosshair. */
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public final class BreedingEggCountdownRenderer {
    @SubscribeEvent
    public static void renderLookedAtEgg(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!(mc.hitResult instanceof BlockHitResult hit)
                ||hit.getType()!=HitResult.Type.BLOCK) return;
        BlockPos pos=hit.getBlockPos();
        if(!mc.level.getBlockState(pos).is(ModBlocks.BREEDING_EGG.get())
                ||!(mc.level.getBlockEntity(pos) instanceof BreedingEggBlockEntity egg))return;
        double reach=mc.gameMode==null?4.5D:mc.gameMode.getPickRange();
        if(mc.player.distanceToSqr(hit.getLocation().x,hit.getLocation().y,hit.getLocation().z)>reach*reach)return;

        int seconds=Math.max(0,(egg.remainingTicks()+19)/20);
        String text=String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);
        PoseStack pose=event.getPoseStack();
        var camera=event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(pos.getX()+0.5D-camera.x,pos.getY()+0.82D-camera.y,pos.getZ()+0.5D-camera.z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F,-0.025F,0.025F);
        var buffers=mc.renderBuffers().bufferSource();
        float x=-mc.font.width(text)/2.0F;
        mc.font.drawInBatch(Component.literal(text),x,0.0F,0xFFFFFFFF,false,pose.last().pose(),buffers,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL,0x70000000,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
        buffers.endBatch();
        pose.popPose();
    }
    private BreedingEggCountdownRenderer() {}
}
