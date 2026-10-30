package br.com.petrickfaria.minemon.client;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
public class MinemonRenderer extends GeoEntityRenderer<MinemonCreature> {
 private final MinemonModel minemonModel;
 private String activeFireSpecies;
 private boolean fireOverlayPass;
 private String activeBone;
 private static final java.util.Map<String,java.util.Set<String>> drawnFireBones=new java.util.concurrent.ConcurrentHashMap<>();
 public static java.util.Map<String,java.util.Set<String>> drawnFireBoneSamples(){return java.util.Map.copyOf(drawnFireBones);}
 private static final java.util.Map<String,java.util.Set<Integer>> fireOverlayFrames=new java.util.concurrent.ConcurrentHashMap<>();
 private static final java.util.Map<String,java.util.Set<String>> fireOverlayBones=new java.util.concurrent.ConcurrentHashMap<>();
 public static boolean previewRendering;
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
  float scale=d.scale()*(entity.isBaby()?0.5F:1.0F);
  pose.pushPose();
  if(!previewRendering)pose.translate(0,entity.isDefeated()?br.com.petrickfaria.minemon.entity.ModelMetrics.defeatedOffset(entity.getSpeciesSlug(),scale):br.com.petrickfaria.minemon.entity.ModelMetrics.floatOffset(entity.getSpeciesSlug(),scale),0);
  pose.scale(scale,scale,scale);
  String previousSpecies=activeFireSpecies;activeFireSpecies=entity.getSpeciesSlug();
  try { super.render(entity,yaw,partialTick,pose,buffers,light); }
  finally { activeFireSpecies=previousSpecies;pose.popPose(); }
  if(validationObserver!=null)validationObserver.accept(entity,this.minemonModel);
 }

 @Override public boolean shouldShowName(MinemonCreature entity){return entity.isTame();}
 @Override protected void renderNameTag(MinemonCreature entity,Component name,com.mojang.blaze3d.vertex.PoseStack pose,
      net.minecraft.client.renderer.MultiBufferSource buffers,int packedLight){
  if(!entity.isTame()){super.renderNameTag(entity,name,pose,buffers,packedLight);return;}
  Component shownName=entity.hasCustomName()?entity.getCustomName():entity.getName();
  float scale=SpeciesDefinitions.get(entity.getSpeciesSlug()).scale()*(entity.isBaby()?.5F:1);
  pose.pushPose();
  try{
   // Name tags are world-sized; cancel the species mesh scale before using
   // the measured visible top, rather than scaling the hitbox height twice.
   pose.scale(1/scale,1/scale,1/scale);
   double top=br.com.petrickfaria.minemon.entity.ModelMetrics.top(entity.getSpeciesSlug(),scale);
   pose.translate(0,Math.max(0,top-entity.getBbHeight()),0);
   super.renderNameTag(entity,shownName,pose,buffers,packedLight);
   pose.translate(0,-.22D,0);
   super.renderNameTag(entity,Component.literal("Nível: "+entity.getPokemonLevel()),pose,buffers,packedLight);
  }finally{pose.popPose();}
 }
 @Override public void renderRecursively(software.bernie.geckolib3.geo.render.built.GeoBone bone,
      com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer buffer,
      int light,int overlay,float red,float green,float blue,float alpha) {
  if(!fireOverlayPass&&java.util.Set.of("charmander","charmeleon","charizard").contains(activeFireSpecies)
      &&bone.name.equals("tail_flame"))return;
  String previousBone=activeBone;activeBone=bone.name;
  try{super.renderRecursively(bone,pose,buffer,light,overlay,red,green,blue,alpha);}
  finally{activeBone=previousBone;}
 }

 @Override public void renderCube(software.bernie.geckolib3.geo.render.built.GeoCube cube,
      com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer buffer,
      int light,int overlay,float red,float green,float blue,float alpha){
  if(fireOverlayPass&&Boolean.getBoolean("minemon.clientSmokeTest")){
   if(light!=15728880)throw new IllegalStateException("Fire cube lost full-bright light: "+activeFireSpecies);
   if(java.util.Arrays.stream(cube.quads).anyMatch(java.util.Objects::nonNull))
    drawnFireBones.computeIfAbsent(activeFireSpecies,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(activeBone);
  }
  super.renderCube(cube,pose,buffer,light,overlay,red,green,blue,alpha);
 }

 /** Full-bright overlays use transparent flame masks on fire-bearing bones only. */
 private static final class FireOverlayLayer extends software.bernie.geckolib3.renderers.geo.GeoLayerRenderer<MinemonCreature> {
  private final MinemonRenderer owner;
  FireOverlayLayer(software.bernie.geckolib3.renderers.geo.IGeoRenderer<MinemonCreature> renderer){super(renderer);this.owner=(MinemonRenderer)renderer;}
  @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,
       int light,MinemonCreature entity,float limbSwing,float limbSwingAmount,float partialTick,float ageInTicks,float netHeadYaw,float headPitch){
   String slug=entity.getSpeciesSlug();
   boolean animatedFlames=slug.equals("ponyta")||slug.equals("rapidash");
   boolean starterFlame=slug.equals("charmander")||slug.equals("charmeleon")||slug.equals("charizard");
   boolean moltresFlame=slug.equals("moltres");
   if(!animatedFlames&&!starterFlame&&!moltresFlame)return;
   int frame=animatedFlames?net.minecraft.util.Mth.floor((entity.tickCount+partialTick)*0.5F)&3:0;
   var model=getEntityModel().getModel(getEntityModel().getModelResource(entity));
   var bones=java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
   for(var root:model.topLevelBones)collectFireBones(root,bones,slug);
   fireOverlayFrames.computeIfAbsent(slug,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(frame);
   fireOverlayBones.put(slug,bones);
   if(moltresFlame){
    fireOverlayBones.put(slug,java.util.Set.of("FULL_MODEL"));
    boolean previousPass=owner.fireOverlayPass;owner.fireOverlayPass=true;
    try{renderCopyModel(getEntityModel(),getEntityModel().getTextureResource(entity),pose,buffers,15728880,entity,partialTick,1,1,1);}
    finally{owner.fireOverlayPass=previousPass;}
    return;
   }
   if(starterFlame){
    var selected=starterFlame?java.util.Set.of("tail_flame"):moltresFireBones(model);
    fireOverlayBones.put(slug,selected);
    var texture=new net.minecraft.resources.ResourceLocation(br.com.petrickfaria.minemon.Minemon.MOD_ID,
        "textures/emissive/"+(moltresFlame?"moltres_fire":slug+"_tail_flame")+".png");
    renderSelectedBones(model,selected,texture,pose,buffers,entity,partialTick);
    return;
   }
   var texture=new net.minecraft.resources.ResourceLocation(br.com.petrickfaria.minemon.Minemon.MOD_ID,
       "textures/pokemon/"+slug+"_cobblemon_1_8_2/fire/normal/"+(frame+1)+".png");
   var selected=java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
   for(var root:model.topLevelBones)collectFireBones(root,selected,slug);
   if(slug.equals("ponyta")||slug.equals("rapidash"))selected.add("tail");
   fireOverlayBones.put(slug,selected);
   renderSelectedBones(model,selected,texture,pose,buffers,entity,partialTick);
  }
  private void renderSelectedBones(software.bernie.geckolib3.geo.render.built.GeoModel model,java.util.Set<String> selected,
       net.minecraft.resources.ResourceLocation texture,com.mojang.blaze3d.vertex.PoseStack pose,
       net.minecraft.client.renderer.MultiBufferSource buffers,MinemonCreature entity,float partialTick){
   var original=new java.util.IdentityHashMap<software.bernie.geckolib3.geo.render.built.GeoBone,boolean[]>();
   for(var root:model.topLevelBones)saveVisibility(root,original);
   boolean previousPass=owner.fireOverlayPass;owner.fireOverlayPass=true;
   try{
    for(var root:model.topLevelBones)setSelectedVisibility(root,selected,false);
    renderCopyModel(getEntityModel(),texture,pose,buffers,15728880,entity,partialTick,1,1,1);
   }finally{owner.fireOverlayPass=previousPass;original.forEach((bone,visibility)->{bone.setHidden(visibility[0],visibility[1]);bone.setCubesHidden(visibility[2]);});}
  }
  private static void saveVisibility(software.bernie.geckolib3.geo.render.built.GeoBone bone,
       java.util.IdentityHashMap<software.bernie.geckolib3.geo.render.built.GeoBone,boolean[]> original){
   original.put(bone,new boolean[]{bone.isHidden(),bone.childBonesAreHiddenToo(),bone.cubesAreHidden()});for(var child:bone.childBones)saveVisibility(child,original);
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
   else if(!selectedParent){bone.setHidden(false,false);bone.setCubesHidden(true);} // Transform ancestors only; never draw their body cubes with a flame mask.
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
  private static java.util.Set<String> moltresFireBones(software.bernie.geckolib3.geo.render.built.GeoModel model){
   var selected=java.util.concurrent.ConcurrentHashMap.<String>newKeySet();
   selected.add("wing_open_left");selected.add("wing_open_right");
   for(var root:model.topLevelBones)collectMoltresFlames(root,selected);
   return selected;
  }
  private static void collectMoltresFlames(software.bernie.geckolib3.geo.render.built.GeoBone bone,java.util.Set<String> found){
   if(bone.name.startsWith("head_fire")||bone.name.startsWith("tail_fire_puff"))found.add(bone.name);
   for(var child:bone.childBones)collectMoltresFlames(child,found);
  }
  @Override public net.minecraft.client.renderer.RenderType getRenderType(net.minecraft.resources.ResourceLocation texture){
   if(texture.getPath().contains("moltres_cobblemon_1_8_2"))
    return net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texture);
   return net.minecraft.client.renderer.RenderType.entityTranslucent(texture);
  }
 }
}
