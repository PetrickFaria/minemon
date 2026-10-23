package br.com.petrickfaria.minemon.client;
import br.com.petrickfaria.minemon.entity.MinemonCreature;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
public class MinemonRenderer extends GeoEntityRenderer<MinemonCreature> {
 private final MinemonModel minemonModel;
 private static final java.util.Map<String,java.util.Set<Integer>> fireOverlayFrames=new java.util.concurrent.ConcurrentHashMap<>();
 private static final java.util.Map<String,java.util.Set<String>> fireOverlayBones=new java.util.concurrent.ConcurrentHashMap<>();
 private String activeFireSpecies;
 private int activeFireFrame;
 private com.mojang.blaze3d.vertex.VertexConsumer baseVertexBuffer;
 public static java.util.function.Consumer<MinemonCreature> validationInput;
 public static java.util.function.BiConsumer<MinemonCreature,MinemonModel> validationObserver;
 public static java.util.Map<String,java.util.Set<Integer>> fireOverlayFrameSamples(){return java.util.Map.copyOf(fireOverlayFrames);}
 public static java.util.Map<String,java.util.Set<String>> fireOverlayBoneSamples(){return java.util.Map.copyOf(fireOverlayBones);}
 public MinemonRenderer(EntityRendererProvider.Context context){this(context,new MinemonModel());}
 private MinemonRenderer(EntityRendererProvider.Context context,MinemonModel model){super(context,model);this.minemonModel=model;this.shadowRadius=.45F;}
 @Override public void render(MinemonCreature entity,float yaw,float partialTick,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
  if(Boolean.getBoolean("minemon.clientSmokeTest") && validationInput!=null)validationInput.accept(entity);
  var d=SpeciesDefinitions.get(entity.getSpeciesSlug());
  String slug=entity.getSpeciesSlug();
  activeFireSpecies=(slug.equals("ponyta")||slug.equals("rapidash"))?slug:null;
  activeFireFrame=net.minecraft.util.Mth.floor((entity.tickCount+partialTick)*0.5F)&3; // Native resolver: loop=true, 10 fps
  baseVertexBuffer=null;
  pose.pushPose();pose.scale(d.scale(),d.scale(),d.scale());
  try { super.render(entity,yaw,partialTick,pose,buffers,light); }
  finally { pose.popPose();activeFireSpecies=null;baseVertexBuffer=null; }
  if(validationObserver!=null)validationObserver.accept(entity,this.minemonModel);
 }

 @Override public void renderRecursively(software.bernie.geckolib3.geo.render.built.GeoBone bone,
      com.mojang.blaze3d.vertex.PoseStack pose,
      com.mojang.blaze3d.vertex.VertexConsumer buffer,int light,int overlay,
      float red,float green,float blue,float alpha) {
  if(baseVertexBuffer==null)baseVertexBuffer=buffer;
  if(activeFireSpecies!=null&&isFireLayerBone(bone.name)) {
   var texture=new net.minecraft.resources.ResourceLocation(br.com.petrickfaria.minemon.Minemon.MOD_ID,
       "textures/pokemon/"+activeFireSpecies+"_cobblemon_1_8_2/fire/normal/"+(activeFireFrame+1)+".png");
   var fireBuffer=getCurrentRTB().getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texture));
   fireOverlayFrames.computeIfAbsent(activeFireSpecies,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(activeFireFrame);
   fireOverlayBones.computeIfAbsent(activeFireSpecies,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(bone.name);
   // Cobblemon marks these resolver overlays emissive. Keep them fullbright
   // while retaining the source image alpha and every original bone transform.
   super.renderRecursively(bone,pose,fireBuffer,15728880,overlay,red,green,blue,alpha);
  } else {
   super.renderRecursively(bone,pose,baseVertexBuffer,light,overlay,red,green,blue,alpha);
  }
 }

 private static boolean isFireLayerBone(String name) {
  return name.startsWith("fire_mane")||name.endsWith("_fire");
 }
}
