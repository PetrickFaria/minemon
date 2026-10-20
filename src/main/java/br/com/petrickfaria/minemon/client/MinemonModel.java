package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.SpeciesAssetIndex;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class MinemonModel extends AnimatedGeoModel<MinemonCreature> {
    @Override
    public ResourceLocation getModelResource(MinemonCreature entity) {
        return location(entity, "models", "geo/" + entity.getSpeciesSlug() + "/" + entity.getSpeciesSlug() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MinemonCreature entity) {
        return location(entity, "textures", "textures/pokemon/" + entity.getSpeciesSlug() + "/" + entity.getSpeciesSlug() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(MinemonCreature entity) {
        return location(entity, "animations", "animations/" + entity.getSpeciesSlug() + "/" + entity.getSpeciesSlug() + ".animation.json");
    }

    @Override
    public void setLivingAnimations(MinemonCreature entity, Integer uniqueID, software.bernie.geckolib3.core.event.predicate.AnimationEvent event) {
        super.setLivingAnimations(entity, uniqueID, event);
        String slug=entity.getSpeciesSlug();
        if(!(slug.equals("spearow")||slug.equals("fearow"))
           ||!getModelResource(entity).getPath().contains("_cobblemon_1_8_2/"))return;
        // The native source standing/walking poser combines a fixed Bedrock
        // stance with q.look('head'). Apply look only to these two audited rigs.
        var data=event.getExtraDataOfType(software.bernie.geckolib3.model.provider.data.EntityModelData.class);
        var head=getAnimationProcessor().getBone("head");
        if(data.isEmpty()||head==null)return;
        var input=(software.bernie.geckolib3.model.provider.data.EntityModelData)data.get(0);
        float basePitch=slug.equals("fearow")?-10.0F:0.0F;
        head.setRotationX((basePitch+net.minecraft.util.Mth.clamp(input.headPitch,-25.0F,25.0F))*(float)Math.PI/180.0F);
        head.setRotationY(net.minecraft.util.Mth.clamp(input.netHeadYaw,-60.0F,60.0F)*(float)Math.PI/180.0F);
    }

    private ResourceLocation location(MinemonCreature entity, String category, String fallback) {
        return new ResourceLocation(Minemon.MOD_ID, SpeciesAssetIndex.resource(entity.getSpeciesSlug(), category, fallback));
    }
}
