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
  var d=SpeciesDefinitions.get(entity.getSpeciesSlug());
  float shadowScale=switch(entity.getSpeciesSlug()){
   case "gastly" -> 0.5F;
   case "electabuzz" -> 1.5F;
   default -> 1.0F;
  };
  this.shadowRadius=0.45F*shadowScale;
  pose.pushPose();pose.scale(d.scale(),d.scale(),d.scale());
  try { super.render(entity,yaw,partialTick,pose,buffers,light); }
  finally { pose.popPose(); }
  if(validationObserver!=null)validationObserver.accept(entity,this.minemonModel);
 }

 /** Full-bright overlays use the source flame atlases and only the fire-bearing rig branches. */
 private static final class FireOverlayLayer extends software.bernie.geckolib3.renderers.geo.GeoLayerRenderer<MinemonCreature> {
  FireOverlayLayer(software.bernie.geckolib3.renderers.geo.IGeoRenderer<MinemonCreature> renderer){super(renderer);}
  @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,
       int light,MinemonCreature entity,float limbSwing,float limbSwingAmount,float partialTick,float ageInTicks,float netHeadYaw,float headPitch){
   String slug=entity.getSpeciesSlug();
   boolean animatedFlames=slug.equals("ponyta")||slug.equals("rapidash");
   boolean emissiveAtlas=slug.equals("charmander")||slug.equals("charmeleon")||slug.equals("charizard")||slug.equals("moltres");
   if(!animatedFlames&&!emissiveAtlas)return;
   int frame=animatedFlames?net.minecraft.util.Mth.floor((entity.tickCount+partialTick)*0.5F)&3:0;
   var model=getEntityModel().getModel(getEntityModel().getModelResource(entity));
   var bones=java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
   for(var root:model.topLevelBones)collectFireBones(root,bones,slug);
   fireOverlayFrames.computeIfAbsent(slug,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(frame);
   fireOverlayBones.put(slug,bones);
   if(emissiveAtlas){
    var selected=slug.equals("moltres")?java.util.Set.of("wing_open_left","wing_open_right"):
        java.util.Set.of("tail_flame");
    var texture=slug.equals("moltres")?getEntityModel().getTextureResource(entity):
        new net.minecraft.resources.ResourceLocation(br.com.petrickfaria.minemon.Minemon.MOD_ID,
            "textures/pokemon/"+slug+"_cobblemon_1_8_2/"+slug+"_flame1.png");
    renderSelectedBones(model,selected,texture,pose,buffers,entity,partialTick);
    return;
   }
   var texture=new net.minecraft.resources.ResourceLocation(br.com.petrickfaria.minemon.Minemon.MOD_ID,
       "textures/pokemon/"+slug+"_cobblemon_1_8_2/fire/normal/"+(frame+1)+".png");
   // The transparent source atlas keeps the base texture visible; full-bright
   // matches the resolver's emissive=true layer without changing the rig.
   renderCopyModel(getEntityModel(),texture,pose,buffers,15728880,entity,partialTick,1,1,1);
  }
  private void renderSelectedBones(software.bernie.geckolib3.geo.render.built.GeoModel model,java.util.Set<String> selected,
       net.minecraft.resources.ResourceLocation texture,com.mojang.blaze3d.vertex.PoseStack pose,
       net.minecraft.client.renderer.MultiBufferSource buffers,MinemonCreature entity,float partialTick){
   var original=new java.util.IdentityHashMap<software.bernie.geckolib3.geo.render.built.GeoBone,boolean[]>();
   for(var root:model.topLevelBones)saveVisibility(root,original);
   try{
    for(var root:model.topLevelBones)setSelectedVisibility(root,selected,false);
    renderCopyModel(getEntityModel(),texture,pose,buffers,15728880,entity,partialTick,1,1,1);
   }finally{original.forEach((bone,visibility)->bone.setHidden(visibility[0],visibility[1]));}
  }
  private static void saveVisibility(software.bernie.geckolib3.geo.render.built.GeoBone bone,
       java.util.IdentityHashMap<software.bernie.geckolib3.geo.render.built.GeoBone,boolean[]> original){
   original.put(bone,new boolean[]{bone.isHidden(),bone.childBonesAreHiddenToo()});for(var child:bone.childBones)saveVisibility(child,original);
  }
  private static boolean containsSelected(software.bernie.geckolib3.geo.render.built.GeoBone bone,java.util.Set<String> selected){
   if(selected.contains(bone.name))return true;
   for(var child:bone.childBones)if(containsSelected(child,selected))return true;
   return false;
  }
  private static void setSelectedVisibility(software.bernie.geckolib3.geo.render.built.GeoBone bone,
       java.util.Set<String> selected,boolean selectedParent){
   boolean keep=selectedParent||containsSelected(bone,selected);
   if(!keep)bone.setHidden(true,true);
   else if(selected.contains(bone.name))bone.setHidden(false,false);
   for(var child:bone.childBones)setSelectedVisibility(child,selected,selectedParent||selected.contains(bone.name));
  }
  private static void collectFireBones(software.bernie.geckolib3.geo.render.built.GeoBone bone,java.util.Set<String> found,String slug){
   boolean mane=bone.name.startsWith("fire_mane")||bone.name.endsWith("_fire");
   boolean tail=java.util.Set.of("charmander","charmeleon","charizard").contains(slug)
       &&(bone.name.equals("tail_flame")||bone.name.startsWith("fire"));
   boolean wings=slug.equals("moltres")&&(bone.name.startsWith("wing_open")||bone.name.startsWith("feathers")||bone.name.contains("fire"));
   if(mane||tail||wings)found.add(bone.name);
   for(var child:bone.childBones)collectFireBones(child,found,slug);
  }
  @Override public net.minecraft.client.renderer.RenderType getRenderType(net.minecraft.resources.ResourceLocation texture){
   return net.minecraft.client.renderer.RenderType.entityTranslucent(texture);
  }
 }
}
