package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.CaptureBallProjectile;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemTransforms;
import com.mojang.math.Vector3f;

public final class CaptureBallRenderer extends EntityRenderer<CaptureBallProjectile> {
    private final ItemRenderer itemRenderer;
    public CaptureBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        shadowRadius = .12F;
    }

    @Override public void render(CaptureBallProjectile ball, float yaw, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        pose.pushPose();
        try {
            pose.translate(0.0D, .12D, 0.0D);
            pose.mulPose(Vector3f.YP.rotationDegrees(ball.tickCount * 24.0F + partialTick * 24.0F));
            pose.mulPose(Vector3f.ZP.rotationDegrees(ball.getXRot()));
            float scale = ball.tickCount >= 12 && ball.tickCount <= 15 ? 1.12F : .78F;
            pose.scale(scale, scale, scale);
            itemRenderer.renderStatic(ball.renderStack(), ItemTransforms.TransformType.GROUND,
                    light, OverlayTexture.NO_OVERLAY, pose, buffers, ball.getId());
        } finally { pose.popPose(); }
        super.render(ball, yaw, partialTick, pose, buffers, light);
    }

    @Override public ResourceLocation getTextureLocation(CaptureBallProjectile entity) {
        return new ResourceLocation(Minemon.MOD_ID, "textures/item/pokeball.png");
    }
}
