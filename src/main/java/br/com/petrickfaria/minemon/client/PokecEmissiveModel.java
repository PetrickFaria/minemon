package br.com.petrickfaria.minemon.client;

import br.com.petrickfaria.minemon.Minemon;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Reuses the original geometry/UVs/textures, changing only selected face lightmaps. */
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PokecEmissiveModel extends BakedModelWrapper<BakedModel> {
    private final Map<BakedQuad,List<BakedQuad>> cache=new ConcurrentHashMap<>();
    public PokecEmissiveModel(BakedModel original){super(original);}

    @SubscribeEvent public static void bake(ModelEvent.BakingCompleted event){
        event.getModels().replaceAll((id,model)->{
            if(!id.getNamespace().equals(Minemon.MOD_ID))return model;
            if(id instanceof ModelResourceLocation location&&location.getVariant().equals("inventory"))return model;
            return Set.of("pokec","block/pokec_lower","block/pokec_upper").contains(id.getPath())
                    ?new PokecEmissiveModel(model):model;
        });
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){
        return selected(originalModel.getQuads(state,side,random));
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
        return selected(originalModel.getQuads(state,side,random,data,type));
    }
    private List<BakedQuad> selected(List<BakedQuad> source){
        var result=new ArrayList<BakedQuad>();
        for(var quad:source)result.addAll(cache.computeIfAbsent(quad,PokecEmissiveModel::split));
        return result;
    }
    private static BakedQuad light(BakedQuad source,int[] vertices,boolean glowing){
        var quad=new BakedQuad(vertices,source.getTintIndex(),source.getDirection(),source.getSprite(),glowing?false:source.isShade());
        if(glowing)QuadTransformers.settingMaxEmissivity().processInPlace(quad);
        return quad;
    }
    private static List<BakedQuad> split(BakedQuad quad){
        String sprite=quad.getSprite().getName().toString();
        if(sprite.equals("minemon_gen1:block/pokec_screen")||sprite.equals("minemon_gen1:block/pokec_glow"))
            return List.of(light(quad,quad.getVertices().clone(),true));
        if(!sprite.equals("minemon_gen1:block/pokec_side"))return List.of(quad);
        // The unchanged 16x16 side texture has exactly four cyan pixels:
        // x=10..11, y=6..7. Partition its face instead of illuminating its
        // red/white/grey pixels or painting a replacement texture.
        int[] source=quad.getVertices();int stride=source.length/4;
        float minU=Float.POSITIVE_INFINITY,maxU=Float.NEGATIVE_INFINITY,minV=minU,maxV=maxU;
        for(int i=0;i<4;i++){
            float u=Float.intBitsToFloat(source[i*stride+4]),v=Float.intBitsToFloat(source[i*stride+5]);
            minU=Math.min(minU,u);maxU=Math.max(maxU,u);minV=Math.min(minV,v);maxV=Math.max(maxV,v);
        }
        float[] us={minU,quad.getSprite().getU(10),quad.getSprite().getU(12),maxU};
        float[] vs={minV,quad.getSprite().getV(6),quad.getSprite().getV(8),maxV};
        float u0=Float.intBitsToFloat(source[4]),v0=Float.intBitsToFloat(source[5]);
        float du1=Float.intBitsToFloat(source[stride+4])-u0,dv1=Float.intBitsToFloat(source[stride+5])-v0;
        float du3=Float.intBitsToFloat(source[3*stride+4])-u0,dv3=Float.intBitsToFloat(source[3*stride+5])-v0;
        float determinant=du1*dv3-du3*dv1;
        if(Math.abs(determinant)<1e-12F)throw new IllegalStateException("Degenerate Pokec side UVs");
        var result=new ArrayList<BakedQuad>(9);
        for(int x=0;x<3;x++)for(int y=0;y<3;y++){
            int[] vertices=source.clone();
            for(int i=0;i<4;i++){
                float u=Float.intBitsToFloat(source[i*stride+4])<(minU+maxU)*.5F?us[x]:us[x+1];
                float v=Float.intBitsToFloat(source[i*stride+5])<(minV+maxV)*.5F?vs[y]:vs[y+1];
                float a=((u-u0)*dv3-(v-v0)*du3)/determinant;
                float b=(du1*(v-v0)-dv1*(u-u0))/determinant;
                for(int axis=0;axis<3;axis++){
                    float p0=Float.intBitsToFloat(source[axis]);
                    float p1=Float.intBitsToFloat(source[stride+axis]),p3=Float.intBitsToFloat(source[3*stride+axis]);
                    vertices[i*stride+axis]=Float.floatToRawIntBits(p0+a*(p1-p0)+b*(p3-p0));
                }
                vertices[i*stride+4]=Float.floatToRawIntBits(u);vertices[i*stride+5]=Float.floatToRawIntBits(v);
            }
            result.add(light(quad,vertices,x==1&&y==1));
        }
        return List.copyOf(result);
    }
}
