package br.com.petrickfaria.minemon.client;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
public class MinemonRenderer extends GeoEntityRenderer<MinemonCreature> {
 private final MinemonModel minemonModel;
 private static final java.util.Map<String,java.util.Set<Integer>> fireOverlayFrames=new java.util.concurrent.ConcurrentHashMap<>();
 private static final java.util.Map<String,java.util.Set<String>> fireOverlayBones=new java.util.concurrent.ConcurrentHashMap<>();
 public static java.util.function.Consumer<MinemonCreature> validationInput;
 public static java.util.function.BiConsumer<MinemonCreature,MinemonModel> validationObserver;
 public static java.util.Map<String,java.util.Set<Integer>> fireOverlayFrameSamples(){return java.util.Map.copyOf(fireOverlayFrames);}
 public static java.util.Map<String,java.util.Set<String>> fireOverlayBoneSamples(){return java.util.Map.copyOf(fireOverlayBones);}
 public MinemonRenderer(EntityRendererProvider.Context context){this(context,new MinemonModel());}
 private MinemonRenderer(EntityRendererProvider.Context context,MinemonModel model){super(context,model);this.minemonModel=model;this.shadowRadius=.45F;this.addLayer(new FireOverlayLayer(this));}
 @Override public void render(MinemonCreature entity,float yaw,float partialTick,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
  if(Boolean.getBoolean("minemon.clientSmokeTest") && validationInput!=null)validationInput.accept(entity);
  var d=SpeciesDefinitions.get(entity.getSpeciesSlug());pose.pushPose();pose.scale(d.scale(),d.scale(),d.scale());
  try { super.render(entity,yaw,partialTick,pose,buffers,light); }
  finally { pose.popPose(); }
  if(validationObserver!=null)validationObserver.accept(entity,this.minemonModel);
 }

 /** Cobblemon resolver layer: a transparent, looping, emissive atlas over the entire model. */
 private static final class FireOverlayLayer extends software.bernie.geckolib3.renderers.geo.GeoLayerRenderer<MinemonCreature> {
  FireOverlayLayer(software.bernie.geckolib3.renderers.geo.IGeoRenderer<MinemonCreature> renderer){super(renderer);}
  @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,
       int light,MinemonCreature entity,float limbSwing,float limbSwingAmount,float partialTick,float ageInTicks,float netHeadYaw,float headPitch){
   String slug=entity.getSpeciesSlug();
   if(!slug.equals("ponyta")&&!slug.equals("rapidash"))return;
   int frame=net.minecraft.util.Mth.floor((entity.tickCount+partialTick)*0.5F)&3; // Native resolver: 10 fps, loop=true
   var texture=new net.minecraft.resources.ResourceLocation(br.com.petrickfaria.minemon.Minemon.MOD_ID,
       "textures/pokemon/"+slug+"_cobblemon_1_8_2/fire/normal/"+(frame+1)+".png");
   var model=getEntityModel().getModel(getEntityModel().getModelResource(entity));
   var bones=java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
   for(var root:model.topLevelBones)collectFireBones(root,bones);
   fireOverlayFrames.computeIfAbsent(slug,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(frame);
   fireOverlayBones.put(slug,bones);
   // The transparent source atlas keeps the base texture visible; full-bright
   // matches the resolver's emissive=true layer without changing the rig.
   renderCopyModel(getEntityModel(),texture,pose,buffers,15728880,entity,partialTick,1,1,1);
  }
  private static void collectFireBones(software.bernie.geckolib3.geo.render.built.GeoBone bone,java.util.Set<String> found){
   if(bone.name.startsWith("fire_mane")||bone.name.endsWith("_fire"))found.add(bone.name);
   for(var child:bone.childBones)collectFireBones(child,found);
  }
  @Override public net.minecraft.client.renderer.RenderType getRenderType(net.minecraft.resources.ResourceLocation texture){
   return net.minecraft.client.renderer.RenderType.entityTranslucent(texture);
  }
 }
}
