package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.entity.ElementalProjectile;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Billboard uses the vanilla fireball sprite tinted to the species' first type. */
public final class ElementalProjectileRenderer extends EntityRenderer<ElementalProjectile> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("minecraft","textures/item/fire_charge.png");
    public ElementalProjectileRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(ElementalProjectile entity){return TEXTURE;}
    @Override public void render(ElementalProjectile entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.scale(.45F,.45F,.45F);pose.mulPose(entityRenderDispatcher.cameraOrientation());pose.mulPose(Vector3f.YP.rotationDegrees(180));
        var matrix=pose.last().pose();var normal=pose.last().normal();var out=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int color=entity.color(),r=color>>16&255,g=color>>8&255,b=color&255;
        out.vertex(matrix,-.5F,-.5F,0).color(r,g,b,255).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal,0,1,0).endVertex();
        out.vertex(matrix,.5F,-.5F,0).color(r,g,b,255).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal,0,1,0).endVertex();
        out.vertex(matrix,.5F,.5F,0).color(r,g,b,255).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal,0,1,0).endVertex();
        out.vertex(matrix,-.5F,.5F,0).color(r,g,b,255).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(normal,0,1,0).endVertex();
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
}
