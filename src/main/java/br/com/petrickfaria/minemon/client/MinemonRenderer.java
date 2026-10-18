package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class MinemonRenderer extends GeoEntityRenderer<MinemonCreature> {
    @Override
    public void render(MinemonCreature entity,float yaw,float partialTick,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light) {
        var definition=br.com.petrickfaria.minemon.entity.SpeciesDefinitions.get(entity.getSpeciesSlug());
        pose.pushPose();pose.scale(definition.scale(),definition.scale(),definition.scale());
        super.render(entity,yaw,partialTick,pose,buffers,light);pose.popPose();
    }
    public MinemonRenderer(EntityRendererProvider.Context context) {
        super(context, new MinemonModel());
        this.shadowRadius = 0.45F;
    }
}
