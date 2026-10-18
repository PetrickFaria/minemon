package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.entity.MinemonCreature;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class MinemonRenderer extends GeoEntityRenderer<MinemonCreature> {
    public MinemonRenderer(EntityRendererProvider.Context context) {
        super(context, new MinemonModel());
        this.shadowRadius = 0.45F;
    }
}
