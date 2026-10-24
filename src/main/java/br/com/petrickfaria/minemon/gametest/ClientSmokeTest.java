package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import br.com.petrickfaria.minemon.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib3.resource.GeckoLibCache;
import java.nio.file.*;
import java.util.UUID;
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,value=Dist.CLIENT)
public final class ClientSmokeTest {
 private static boolean fixtureAir(String slug){return SpeciesBehaviorProfiles.isAirborne(slug)
     ||Boolean.getBoolean("minemon.clientSmokeAir")&&SpeciesBehaviorProfiles.canFly(slug);}
 private static boolean fixtureWater(String slug){return SpeciesBehaviorProfiles.isAquatic(slug)
     ||Boolean.getBoolean("minemon.clientSmokeWater")&&SpeciesBehaviorProfiles.waterCapable(slug);}
 private static boolean fixtureSurface(String slug){return SpeciesBehaviorProfiles.profile(slug)==SpeciesBehaviorProfiles.Profile.SURFACE;}
 private static final java.util.Map<String,Integer> smokeParticleSamples=new java.util.HashMap<>();
 private static boolean opened,setup;
 private static int worldTicks,titleTicks,frames;
 private static volatile UUID testEntity;
 private static volatile int initialEntityId;
 private static float minLeg=Float.MAX_VALUE,maxLeg=-Float.MAX_VALUE,minBody=Float.MAX_VALUE,maxBody=-Float.MAX_VALUE;
 private static volatile boolean combatPassed;
 private static volatile int combatHits;
 private static final java.util.Map<String,java.util.List<String>> modelBones=new java.util.HashMap<>();
 private static final java.util.Map<String,float[]> motionRanges=new java.util.HashMap<>();
 private static final java.util.Map<String,float[]> fireStarterArmRanges=new java.util.HashMap<>();
 private static boolean fearowNativeAirPoseObserved;
 private static final java.util.Map<String,float[]> sourceLookRanges=new java.util.HashMap<>();
 private static final java.util.Map<String,float[]> sourcePitchRanges=new java.util.HashMap<>();
 private static final java.util.Set<String> walkedSpecies=new java.util.HashSet<>();
 private static final java.util.Set<String> wingVisibilitySpecies=new java.util.HashSet<>();
 private static final java.util.Map<String,net.minecraft.world.phys.Vec3> firstRenderedPositions=new java.util.HashMap<>();
 @SubscribeEvent public static void actualDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
  if(Boolean.getBoolean("minemon.clientSmokeTest") && event.getAmount()>0 && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer
     && event.getSource().getEntity() instanceof MinemonCreature b && b.getUUID().equals(testEntity))combatHits++;
 }
 private static int direction=1;
 private static String renderedPhaseSpecies;
 private static int renderedPhase=-1,renderedPhaseFrames;
 private static final java.util.Map<String,String> lastRenderedClip=new java.util.HashMap<>();
 private static final java.util.Map<String,Integer> clipChangedTick=new java.util.HashMap<>();
 private static final java.util.Map<String,Integer> lastRenderedEntityTick=new java.util.HashMap<>();
 private static final java.util.Map<String,Integer> finalIdleFirstTick=new java.util.HashMap<>();
 private static final java.util.Map<String,Integer> steadyWalkFirstTick=new java.util.HashMap<>();
 private static final java.util.Map<String,Integer> steadyRunFirstTick=new java.util.HashMap<>();
 private static boolean steadyMotionObserved(String slug,boolean running) {
  if(running&&(fixtureAir(slug)||fixtureWater(slug)
      ||SpeciesAssetIndex.clip(slug,"ground_run","run")==null))return true;
  var definition=SpeciesDefinitions.get(slug);
  if(!running&&(sourcePoseTransition(slug)||slug.equals("fearow")
      ||definition!=null&&("static".equals(definition.movementFallback())||"idle".equals(definition.movementFallback()))))return true;
  Integer first=(running?steadyRunFirstTick:steadyWalkFirstTick).get(slug),last=lastRenderedEntityTick.get(slug);
  return first!=null&&last!=null&&last-first>=40;
 }
 private static boolean finalIdleObserved(String slug) {
  if(sourceStaticIdle(slug)||sourcePoseTransition(slug)||slug.equals("fearow"))return true;
  var definition=SpeciesDefinitions.get(slug);
  if(definition!=null&&"static".equals(definition.movementFallback()))return true;
  Integer first=finalIdleFirstTick.get(slug),last=lastRenderedEntityTick.get(slug);
  // Keep every threshold; collect enough real idle time for slow native
  // breathing. Rhyhorn's original idle is a four-second cycle.
  int required=slug.equals("rhyhorn")?80:40;
  return first!=null&&last!=null&&last-first>=required;
 }
 private static boolean clipHasSettled(String slug) {
  Integer changed=clipChangedTick.get(slug);
  Integer observed=lastRenderedEntityTick.get(slug);
  // The production controller blends for four ticks (200 ms). Keep the
  // original visibility/articulation assertions, but sample after that blend.
  return changed!=null&&observed!=null&&observed-changed>=5;
 }
 private static final long smokeStartedNanos=System.nanoTime();
 private static final java.util.Set<String> loggedProgress=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private static volatile int completedSpeciesWindows;
 private static volatile String lastApprovedCheck="Minecraft client initialized";
 private static void reportProgress(String slug,String phase) {
  if(!loggedProgress.add(slug+":"+phase))return;
  var definition=SpeciesDefinitions.get(slug);
  int dex=definition==null?0:definition.nationalPokedexNumber();
  int index=species.indexOf(slug)+1;
  double elapsed=(System.nanoTime()-smokeStartedNanos)/60_000_000_000.0D;
  System.out.printf(java.util.Locale.ROOT,"[CLIENT SMOKE] %d/%d — #%03d %s — %s — concluídas=%d/%d — duração=%.1f min — última=%s%n",
    index,species.size(),dex,slug,phase,completedSpeciesWindows,species.size(),elapsed,lastApprovedCheck);
  System.out.flush();
 }
 private static final int SPECIES_TEST_WINDOW=80;
 private static final int BULBASAUR_VALIDATION_TICK=430;
 private static final int COMBAT_VALIDATION_TICK=BULBASAUR_VALIDATION_TICK+100;
 private static final int SPECIES_START_TICK=COMBAT_VALIDATION_TICK+10;
 private static final java.util.Set<String> renderedSpecies=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private static final java.util.List<String> species=SmokeSpeciesSelection.select(
   SpeciesDefinitions.ALL.values(),System.getProperty("minemon.clientSmokeSpecies"));
  // Profiles use exact joints from each selected model skeleton, not one global bone-name list.
  // The active ten-species development batch receives detailed gait validation;
  // the general render, texture, movement and roster smoke continues to cover all 151.
  private static final java.util.Map<String,java.util.Map<String,java.util.List<String>>> ADDITIONAL_PROFILES=loadAdditionalProfiles();
  private static java.util.Map<String,java.util.Map<String,java.util.List<String>>> loadAdditionalProfiles() {
   try(var stream=ClientSmokeTest.class.getResourceAsStream("/data/minemon_gen1/animation_validation_profiles.json")) {
    if(stream==null)throw new IllegalStateException("Missing batch articulation profiles");
    var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject();
    var result=new java.util.HashMap<String,java.util.Map<String,java.util.List<String>>>();
    for(var species:json.entrySet()) {
     var states=new java.util.HashMap<String,java.util.List<String>>();
     for(var state:species.getValue().getAsJsonObject().entrySet()) {
      var joints=new java.util.ArrayList<String>();
      for(var joint:state.getValue().getAsJsonArray())joints.add(joint.getAsString());
      if(joints.isEmpty())throw new IllegalStateException("Empty articulation profile "+species.getKey()+"/"+state.getKey());
      states.put(state.getKey(),java.util.List.copyOf(joints));
     }
     String slug=species.getKey();
     if(CompatibleLocomotion.groundGait(slug)&&!fixtureAir(slug)&&!fixtureWater(slug)){
      states.put("walk",CompatibleLocomotion.gaitJoints(slug));
      states.put("run",CompatibleLocomotion.gaitJoints(slug));
     }
     result.put(slug,java.util.Map.copyOf(states));
    }
    return java.util.Map.copyOf(result);
   }catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}
  }
  private static java.util.Set<String> validationSpecies() {
   var result=new java.util.HashSet<>(java.util.Set.of("bulbasaur","ivysaur","venusaur","charmander","charmeleon","charizard","squirtle","wartortle","blastoise","caterpie"));
   result.addAll(ADDITIONAL_PROFILES.keySet());return java.util.Set.copyOf(result);
  }
  private static java.util.Map<String,java.util.List<String>> addProfiles(java.util.Map<String,java.util.List<String>> original) {
   var result=new java.util.HashMap<>(original);
   ADDITIONAL_PROFILES.forEach((slug,states)->result.put(slug,states.get("walk")));
   return java.util.Map.copyOf(result);
  }
  private static final java.util.Set<String> ARTICULATION_VALIDATION_SPECIES=validationSpecies();
  private static final java.util.Map<String,java.util.List<String>> ARTICULATION_PROFILES=addProfiles(java.util.Map.ofEntries(
   java.util.Map.entry("bulbasaur",java.util.List.of("leg_front_right","leg_front_left","leg_back_right","leg_back_left")),
   java.util.Map.entry("ivysaur",java.util.List.of("leg_front_right","leg_front_left","leg_back_right","leg_back_left")),
   java.util.Map.entry("venusaur",java.util.List.of("leg_front_left","leg_front_right","leg_left","leg_right")),
   java.util.Map.entry("charmander",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("charmeleon",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("charizard",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("squirtle",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("wartortle",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("blastoise",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("pikachu",java.util.List.of("leg_right","leg_left")),
   java.util.Map.entry("caterpie",java.util.List.of("body_segment","body_segment2")),
   java.util.Map.entry("hitmonlee",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("onix",java.util.List.of("segment1","segment7","segment14")),
   java.util.Map.entry("psyduck",java.util.List.of("foot_left","foot_right")),
   java.util.Map.entry("jolteon",java.util.List.of("leg_front_right","leg_front_left","leg_back_right","leg_back_left")),
   java.util.Map.entry("nidorina",java.util.List.of("left_leg","right_leg")),
   java.util.Map.entry("meowth",java.util.List.of("leg_left","leg_right"))
  ));
  // These source idle clips intentionally keep their gait legs static. Inspect
  // their animated idle joints without weakening the leg checks for walk/run.
  private static final java.util.Map<String,java.util.List<String>> IDLE_PROFILES=java.util.Map.of(
   "bulbasaur",java.util.List.of("torso","head"),
   "squirtle",java.util.List.of("arm_left","arm_right","torso"),
   "wartortle",java.util.List.of("arm_left","arm_right","torso")
  );
  private static java.util.List<String> profileJoints(String slug,String state) {
   if(slug.equals("fearow")&&fixtureAir(slug))
    return java.util.List.of("body","wing_open_left","wing_open_right");
   if(ADDITIONAL_PROFILES.containsKey(slug)) {
    String key=state.startsWith("IDLE_")?"idle":state.startsWith("RUN_")
      &&!fixtureAir(slug)&&!fixtureWater(slug)?"run":"walk";
    var joints=ADDITIONAL_PROFILES.get(slug).get(key);
    if(joints!=null)return joints;
    // Some native posers map the smoke's faster-motion phase to their fly clip
    // and have no ground_run articulation profile. Do not invent one; the
    // final profile validator separately requires movement only when a native
    // run clip is declared.
    if(key.equals("run")&&SpeciesAssetIndex.clip(slug,"ground_run","run")==null)return java.util.List.of();
    throw new IllegalStateException("Missing articulation profile "+slug+"/"+key);
   }
   // Native turtle runs retract their limbs and spin the shell/body.
   if(java.util.Set.of("squirtle","wartortle","blastoise").contains(slug) && state.startsWith("RUN_"))return java.util.List.of("body","body2");
   return state.startsWith("IDLE_") && IDLE_PROFILES.containsKey(slug)
    ? IDLE_PROFILES.get(slug) : ARTICULATION_PROFILES.getOrDefault(slug,java.util.List.of());
  }
  private static final java.util.Map<String,java.util.Map<String,float[]>> articulationRanges=new java.util.HashMap<>();
  private static final java.util.Map<String,java.util.Map<String,java.util.Set<String>>> articulationClips=new java.util.HashMap<>();
  private static final java.util.Map<String,java.util.Map<String,Integer>> articulationSampleCounts=new java.util.HashMap<>();
  private static final java.util.Map<String,java.util.Map<String,java.util.Set<String>>> articulationInputs=new java.util.HashMap<>();
  private static final java.util.Map<String,java.util.Map<String,float[]>> tailFlameRanges=new java.util.HashMap<>();
  private static final java.util.Map<String,java.util.Set<String>> tailFlameClips=new java.util.HashMap<>();
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
  if(!Boolean.getBoolean("minemon.clientSmokeTest")||event.phase!=TickEvent.Phase.END)return;
  Minecraft mc=Minecraft.getInstance();
  try {
   if(!opened && mc.screen instanceof TitleScreen && titleTicks==0)
   System.out.println("[CLIENT SMOKE] inicialização — TitleScreen alcançada; verificando cache GeckoLib e recursos registrados.");
  if(!opened && mc.screen instanceof TitleScreen && ++titleTicks==20) {
    var tabItems=net.minecraft.core.NonNullList.<net.minecraft.world.item.ItemStack>create();
    ModItems.MINEMON_TAB.fillItemList(tabItems);
    var eggOrder=new java.util.ArrayList<String>();
    for(var stack:tabItems){var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());if(key!=null&&key.getPath().endsWith("_spawn_egg"))eggOrder.add(key.getPath().substring(0,key.getPath().length()-"_spawn_egg".length()));}
    var expectedEggOrder=SpeciesDefinitions.ALL.values().stream()
      .sorted(java.util.Comparator.comparingInt(SpeciesDefinitions.Definition::nationalPokedexNumber))
      .map(SpeciesDefinitions.Definition::slug).toList();
    if(!eggOrder.equals(expectedEggOrder))throw new IllegalStateException("Creative spawn eggs are not unique and in National Pokédex order: "+eggOrder);
    for(var d:SpeciesDefinitions.ALL.values()) {
     var geo=new ResourceLocation(Minemon.MOD_ID,d.model().substring("assets/minemon/".length()));var anim=new ResourceLocation(Minemon.MOD_ID,d.animation().substring("assets/minemon/".length()));
     try(var stream=ClientSmokeTest.class.getResourceAsStream("/"+d.model().replace("assets/minemon/","assets/minemon_gen1/"))) {
      var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream)).getAsJsonObject();var names=new java.util.ArrayList<String>();
      for(var geometry:json.getAsJsonArray("minecraft:geometry"))for(var bone:geometry.getAsJsonObject().getAsJsonArray("bones"))names.add(bone.getAsJsonObject().get("name").getAsString());modelBones.put(d.slug(),names);
     }
     if(!GeckoLibCache.getInstance().getGeoModels().containsKey(geo)||!GeckoLibCache.getInstance().getAnimations().containsKey(anim))throw new IllegalStateException("GeckoLib did not parse "+d.slug());
    }
    Files.writeString(Path.of("client-validation.txt"),"PASS MENU: Minecraft reached TitleScreen; every registered model/animation parsed by GeckoLib.\nPASS CREATIVE ORDER: all "+SpeciesDefinitions.ALL.size()+" spawn eggs are unique and appear in National Pokédex order.\n");
    opened=true;
    System.out.println("[CLIENT SMOKE] PASS carregamento — cache GeckoLib e "+SpeciesDefinitions.ALL.size()+" espécies verificadas.");
    System.out.flush();
    var dataFile=Path.of("saves/minemon-validation/level.dat").toFile();var tag=net.minecraft.nbt.NbtIo.readCompressed(dataFile);tag.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);net.minecraft.nbt.NbtIo.writeCompressed(tag,dataFile);
    Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);boolean invoked=false;
    for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")) {
     if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");invoked=true;break;}
     if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");invoked=true;break;}
    }
    if(!invoked)throw new IllegalStateException("Could not locate WorldOpenFlows.loadLevel");
    // Animation selection receives a controlled input, independent of the timing
    // of velocity packets and client-side friction. World movement is still
    // driven by real server travel and checked separately for every species.
    MinemonRenderer.validationInput=entity->{
     Integer phase=validationPhase(entity);
     if(phase!=null && ARTICULATION_VALIDATION_SPECIES.contains(entity.getSpeciesSlug())) {
      entity.setDeltaMovement(animationVelocity(phase),0,0);
      if(entity.getSpeciesSlug().equals("spearow")||entity.getSpeciesSlug().equals("fearow")) {
       // Probe the native procedural look input while preserving a static idle
       // stance; no invented autonomous idle oscillation is claimed.
       entity.setXRot((float)Math.sin(entity.tickCount*0.17F)*12.0F);
       entity.yHeadRot=entity.getYRot()+(float)Math.sin(entity.tickCount*0.13F)*25.0F;
      }
      if(java.util.Set.of("abra","kadabra","alakazam","bellsprout","geodude","golem","slowpoke","slowbro","mewtwo","mew").contains(entity.getSpeciesSlug())) {
       // Exercise source q.look('head') / q.look('head_ai') adaptations.
       entity.setXRot((float)Math.sin(entity.tickCount*0.17F)*12.0F);
       entity.yHeadRot=entity.getYRot()+(float)Math.sin(entity.tickCount*0.13F)*25.0F;
      }
     }
    };
    MinemonRenderer.validationObserver=(entity,model)->{
     if(mc.getResourceManager().getResource(model.getTextureResource(entity)).isEmpty())throw new IllegalStateException("Missing rendered texture "+entity.getSpeciesSlug());
     String slug=entity.getSpeciesSlug();renderedSpecies.add(slug);
     String currentClip=entity.getSelectedAnimationForValidation();
     lastRenderedEntityTick.put(slug,entity.tickCount);
     if(currentClip!=null&&!currentClip.equals(lastRenderedClip.put(slug,currentClip)))
      clipChangedTick.put(slug,entity.tickCount);
     if(validationPhase(entity)!=null)reportProgress(slug,"renderização");
     var position=entity.position();var origin=firstRenderedPositions.putIfAbsent(slug,position);
     // Rendering can interpolate one server step over several frames. Measure
     // net movement from the first rendered position instead of requiring a
     // single frame to cross the threshold.
     if(origin!=null&&origin.distanceToSqr(position)>0.0025D) {
      walkedSpecies.add(slug);
     }
     // Sample every render frame, independently of position updates. Filtering
     // these samples by displacement can alias animation keyframes when the
     // client batches several server ticks into a single render frame.
     Integer validationPhase=validationPhase(entity);
     if(validationPhase!=null && ARTICULATION_VALIDATION_SPECIES.contains(slug)) {
      if(!slug.equals(renderedPhaseSpecies)||renderedPhase!=validationPhase) {
       renderedPhaseSpecies=slug;renderedPhase=validationPhase;renderedPhaseFrames=0;
      }
      renderedPhaseFrames++;
      if(clipHasSettled(slug)) {
       if(validationPhase>=68)finalIdleFirstTick.putIfAbsent(slug,entity.tickCount);
       if(validationPhase>=18&&validationPhase<24)steadyWalkFirstTick.putIfAbsent(slug,entity.tickCount);
       if(validationPhase>=41&&validationPhase<47)steadyRunFirstTick.putIfAbsent(slug,entity.tickCount);
      }
     }
     if(validationPhase!=null && java.util.Set.of("abra","kadabra","alakazam","bellsprout","geodude","golem","slowpoke","slowbro","mewtwo","mew").contains(slug)) {
      var lookHead=model.getAnimationProcessor().getBone("head");
      if(lookHead==null)throw new IllegalStateException("Source q.look head adapter has no GeckoLib head bone for "+slug);
      float[] range=sourceLookRanges.computeIfAbsent(slug,k->{var v=new float[4];java.util.Arrays.fill(v,0,2,Float.MAX_VALUE);java.util.Arrays.fill(v,2,4,-Float.MAX_VALUE);return v;});
      float[] values={lookHead.getRotationX(),lookHead.getRotationY()};
      for(int axis=0;axis<2;axis++){range[axis]=Math.min(range[axis],values[axis]);range[axis+2]=Math.max(range[axis+2],values[axis]);}
     }
     if(validationPhase!=null&&(slug.equals("mewtwo")||slug.equals("mew"))) {
      var body=model.getAnimationProcessor().getBone("body");
      if(body==null)throw new IllegalStateException("Source q.pitch_tilt adapter has no GeckoLib body bone for "+slug);
      float[] range=sourcePitchRanges.computeIfAbsent(slug,k->{var v=new float[2];v[0]=Float.MAX_VALUE;v[1]=-Float.MAX_VALUE;return v;});
      range[0]=Math.min(range[0],body.getRotationX());range[1]=Math.max(range[1],body.getRotationX());
     }
     if(validationPhase!=null) {
      if(slug.equals("koffing")||slug.equals("weezing"))smokeParticleSamples.merge(slug,entity.getSmokeParticleCountForValidation(),Math::max);
      if(slug.equals("charmander")||slug.equals("charmeleon")||slug.equals("charizard")) {
       var left=model.getAnimationProcessor().getBone("arm_left");var right=model.getAnimationProcessor().getBone("arm_right");
       int phase=validationPhase;
       int mode=phase>=38&&phase<=46?2:(phase>=16&&phase<=23?0:-1);
       if(mode>=0){
        var range=fireStarterArmRanges.computeIfAbsent(slug,k->new float[4]);
        range[mode]=Math.max(range[mode],Math.abs(left.getRotationZ()));
        range[mode+1]=Math.max(range[mode+1],Math.abs(right.getRotationZ()));
       }
      }
      for(String name:modelBones.get(slug)) {
       if(name.contains("eye")||name.contains("mouth")||name.contains("jaw"))continue;
       var bone=model.getAnimationProcessor().getBone(name);if(bone==null)continue;
       float[] values={bone.getRotationX(),bone.getRotationY(),bone.getRotationZ(),bone.getPositionX(),bone.getPositionY(),bone.getPositionZ()};
       var range=motionRanges.computeIfAbsent(slug+":"+name,k->{var r=new float[12];java.util.Arrays.fill(r,0,6,Float.MAX_VALUE);java.util.Arrays.fill(r,6,12,-Float.MAX_VALUE);return r;});
       for(int axis=0;axis<6;axis++){range[axis]=Math.min(range[axis],values[axis]);range[axis+6]=Math.max(range[axis+6],values[axis]);}
      }
     }
      if(validationPhase!=null && ARTICULATION_VALIDATION_SPECIES.contains(slug) && ARTICULATION_PROFILES.containsKey(slug)) {
       int phase=validationPhase;
       String state=locomotionState(phase);
       // Clip selection is sampled only after a phase has settled. The first render
       // frame at a boundary can still expose the previous controller value.
       if(isSettledClipSample(phase)) {
        if(fixtureWater(slug)&&!entity.isInWater())
         throw new IllegalStateException("Aquatic smoke fixture lost immersion for "+slug+" phase="+phase
             +" position="+entity.position()+" bounds="+entity.getBoundingBox()
             +" clip="+entity.getSelectedAnimationForValidation());
        articulationClips.computeIfAbsent(slug,k->new java.util.HashMap<>())
         .computeIfAbsent(state,k->new java.util.HashSet<>()).add(entity.getSelectedAnimationForValidation());
        articulationSampleCounts.computeIfAbsent(slug,k->new java.util.HashMap<>()).merge(state,1,Integer::sum);
        double velocitySqr=entity.getDeltaMovement().horizontalDistanceSqr();
        double displacementSqr=(entity.getX()-entity.xo)*(entity.getX()-entity.xo)+(entity.getZ()-entity.zo)*(entity.getZ()-entity.zo);
        articulationInputs.computeIfAbsent(slug,k->new java.util.HashMap<>())
         .computeIfAbsent(state,k->new java.util.HashSet<>()).add("phase="+phase+",clip="+entity.getSelectedAnimationForValidation()+",velocitySqr="+velocitySqr+",positionDeltaSqr="+displacementSqr);
       }
       if(slug.equals("zubat")&&isSettledClipSample(phase)) {
        var folded=model.getAnimationProcessor().getBone("wings_folded");
        var open=model.getAnimationProcessor().getBone("wings_open");
        if(folded==null||open==null||Math.abs(folded.getScaleX())>0.001F||Math.abs(open.getScaleX()-1.0F)>0.001F)
         throw new IllegalStateException("Zubat poser visibility failed: folded wings must be hidden and open wings visible; phase="+phase+" clip="+entity.getSelectedAnimationForValidation()+" scales="+(folded==null?"missing":folded.getScaleX())+"/"+(open==null?"missing":open.getScaleX()));
       }
       if((slug.equals("pidgeotto")||slug.equals("pidgeot"))&&isSettledClipSample(phase)) {
        var closed=model.getAnimationProcessor().getBone("wings_closed");
        var open=model.getAnimationProcessor().getBone("wings_open");
        if(closed==null||open==null||!closed.isHidden()||!closed.childBonesAreHiddenToo()
           ||open.isHidden()||open.childBonesAreHiddenToo()||Math.abs(open.getScaleX()-1.0F)>0.001F)
         throw new IllegalStateException("Pidge wing poser visibility failed for "+slug+"/"+entity.getSelectedAnimationForValidation()+": closed hierarchy must be hidden and open hierarchy visible; scales="+(closed==null?"missing":closed.getScaleX())+"/"+(open==null?"missing":open.getScaleX()));
        wingVisibilitySpecies.add(slug);
       }
       // Measure articulation only after the current phase has settled. The
       // selected-clip/input records already use this gate; applying it to pose
       // ranges prevents transition blend frames from contaminating static-idle
       // checks (notably Nidoqueen's native fixed ground_idle stance).
       if(isSettledClipSample(phase)) {
        var perSpecies=articulationRanges.computeIfAbsent(slug,k->new java.util.HashMap<>());
        for(String joint:profileJoints(slug,state)) {
         var bone=model.getAnimationProcessor().getBone(joint);
         if(bone==null)throw new IllegalStateException("Locomotion profile references missing bone "+slug+":"+joint);
         float[] values={bone.getRotationX(),bone.getRotationY(),bone.getRotationZ(),bone.getPositionX(),bone.getPositionY(),bone.getPositionZ()};
         var range=perSpecies.computeIfAbsent(state+":"+joint,k->{var v=new float[12];java.util.Arrays.fill(v,0,6,Float.MAX_VALUE);java.util.Arrays.fill(v,6,12,-Float.MAX_VALUE);return v;});
         for(int axis=0;axis<6;axis++){range[axis]=Math.min(range[axis],values[axis]);range[axis+6]=Math.max(range[axis+6],values[axis]);}
        }
       }
       if(slug.equals("fearow")&&isSettledClipSample(phase)) {
        var body=model.getAnimationProcessor().getBone("body");
        var left=model.getAnimationProcessor().getBone("wing_open_left");
        var right=model.getAnimationProcessor().getBone("wing_open_right");
        // Keep the source's static open-wing air pose; no native flap cycle exists.
        float pitch=(float)Math.toRadians(52.5D),wing=(float)Math.toRadians(52.07555D);
        if(body==null||left==null||right==null
           ||Math.abs(Math.abs(body.getRotationX())-pitch)>0.02F
           ||Math.abs(Math.abs(left.getRotationZ())-wing)>0.02F
           ||Math.abs(Math.abs(right.getRotationZ())-wing)>0.02F
           ||left.getRotationZ()*right.getRotationZ()>=0.0F)
         throw new IllegalStateException("Fearow did not render its native static open-wing air pose");
        fearowNativeAirPoseObserved=true;
       }
      if(java.util.Set.of("charmander","charmeleon","charizard").contains(slug)) {
        var flame=model.getAnimationProcessor().getBone("tail_flame");
        if(flame==null)throw new IllegalStateException(slug+" runtime model is missing tail_flame bone");
        String mode=(phase<8||phase>=54)?"idle":(phase>=8&&phase<27)?"walk":null;
        if(mode!=null) {
         float[] range=tailFlameRanges.computeIfAbsent(slug,k->new java.util.HashMap<>()).computeIfAbsent(mode,k->{var v=new float[6];java.util.Arrays.fill(v,0,3,Float.MAX_VALUE);java.util.Arrays.fill(v,3,6,-Float.MAX_VALUE);return v;});
         float[] rotations={flame.getRotationX(),flame.getRotationY(),flame.getRotationZ()};
         for(int axis=0;axis<3;axis++){range[axis]=Math.min(range[axis],rotations[axis]);range[axis+3]=Math.max(range[axis+3],rotations[axis]);}
        }
        String flameClip=entity.getSelectedTailFlameAnimationForValidation();if(flameClip!=null)tailFlameClips.computeIfAbsent(slug,k->java.util.concurrent.ConcurrentHashMap.newKeySet()).add(flameClip);
       }
      }
     if(testEntity==null||!entity.getUUID().equals(testEntity)||worldTicks<110||worldTicks>=BULBASAUR_VALIDATION_TICK)return;
     var leg=model.getAnimationProcessor().getBone("leg_front_right");var body=model.getAnimationProcessor().getBone("body");
     minLeg=Math.min(minLeg,leg.getRotationX());maxLeg=Math.max(maxLeg,leg.getRotationX());minBody=Math.min(minBody,body.getPositionY());maxBody=Math.max(maxBody,body.getPositionY());frames++;
    };
   }
   if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
   if(!setup) {
    setup=true;mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();level.setDayTime(1000);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,mc.getSingleplayerServer());
     for(int x=-8;x<=8;x++)for(int z=-5;z<=12;z++)level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
     // The generated validation world can contain terrain or structures above y=100.
     // Clear a roomy air volume so the render fixture cannot suffocate before sampling.
     for(int x=-8;x<=8;x++)for(int z=-5;z<=12;z++)for(int y=101;y<=110;y++)
      level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
     var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);player.setGameMode(GameType.CREATIVE);player.teleportTo(level,0,101,0,0,12);
     var b=ModEntities.BULBASAUR.get().create(level);b.moveTo(0,101,4,180,0);b.tame(player);b.setOrderedToSit(false);b.setNoAi(true);level.addFreshEntity(b);testEntity=b.getUUID();initialEntityId=b.getId();
    });
   }
   if(mc.screen!=null || (worldTicks==0 && (initialEntityId==0 || mc.level.getEntity(initialEntityId)==null || mc.player.getY()<100)))return;
   if(worldTicks>=SPECIES_START_TICK&&worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW&&testEntity!=null) {
    // Keep real airborne/swimming specimens in view during the longer idle
    // observation; camera movement does not alter their pose or physics.
    for(var tracked:mc.level.entitiesForRendering())if(tracked.getUUID().equals(testEntity)) {
     double dx=tracked.getX()-mc.player.getX(),dz=tracked.getZ()-mc.player.getZ();
     double dy=tracked.getY()+tracked.getBbHeight()*0.5D-mc.player.getEyeY();
     mc.player.setYRot((float)(Math.toDegrees(Math.atan2(dz,dx))-90.0D));
     mc.player.setXRot((float)-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
     break;
    }
   }
   // Do not skip a short input phase when the client batches ticks between
   // render frames. Keep every original phase and assertion, and require two
   // rendered observations before advancing the ten-species fixture.
   if(worldTicks>=SPECIES_START_TICK && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW) {
    String active=species.get((worldTicks-SPECIES_START_TICK)/SPECIES_TEST_WINDOW);
    int appliedPhase=(worldTicks-SPECIES_START_TICK)%SPECIES_TEST_WINDOW;
    if(ARTICULATION_VALIDATION_SPECIES.contains(active)
       && (!active.equals(renderedPhaseSpecies)||renderedPhase!=appliedPhase||renderedPhaseFrames<2
           ||!clipHasSettled(active)||appliedPhase==79&&!finalIdleObserved(active)
           ||appliedPhase==23&&!steadyMotionObserved(active,false)
           ||appliedPhase==46&&!steadyMotionObserved(active,true))) {
     applyValidationInput(mc,appliedPhase);
     return;
    }
   }
   worldTicks++;
   if(worldTicks>=SPECIES_START_TICK&&worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW) {
    int index=(worldTicks-SPECIES_START_TICK)/SPECIES_TEST_WINDOW;
    int phase=(worldTicks-SPECIES_START_TICK)%SPECIES_TEST_WINDOW;
    if(phase==0&&index>completedSpeciesWindows) {
     completedSpeciesWindows=index;
     String previous=species.get(index-1);
     lastApprovedCheck=previous+" teve a janela de renderização e movimento coletada";
     System.out.printf("[CLIENT SMOKE] %d/%d — %s — janela concluída; verificações de renderização e movimento coletadas.%n",
       index,species.size(),previous);
     System.out.flush();
    }
    String slug=species.get(index);
    String stage=phase<8?"repouso inicial":phase<27?"caminhada":phase<34?"repouso após caminhada":phase<54?"corrida":"repouso final";
    reportProgress(slug,"Validando "+stage);
   }
   if(worldTicks==70||worldTicks==170)Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
   if(worldTicks>=100&&worldTicks<BULBASAUR_VALIDATION_TICK)mc.getSingleplayerServer().execute(()->{
    var b=(MinemonCreature)mc.getSingleplayerServer().overworld().getEntity(testEntity);if(b!=null){b.setNoAi(true);b.getNavigation().stop();if(b.getX()>2.5)direction=-1;if(b.getX()< -2.5)direction=1;b.moveTo(b.getX()+direction*0.08,b.getY(),b.getZ(),b.getYRot(),b.getXRot());}
   });
   if(worldTicks==BULBASAUR_VALIDATION_TICK){
    if(frames<10||maxLeg-minLeg<0.1F||maxBody-minBody<0.01F)throw new IllegalStateException("Rendered body animation did not advance: frames="+frames+" leg="+(maxLeg-minLeg)+" body="+(maxBody-minBody));
    Files.writeString(Path.of("client-validation.txt"),"PASS WORLD: Bulbasaur rendered in a Minecraft world; frames="+frames+" leg rotation range="+(maxLeg-minLeg)+" body position range="+(maxBody-minBody)+"\n",StandardOpenOption.APPEND);
    mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();var old=level.getEntity(testEntity);if(old!=null)old.discard();
     var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);player.teleportTo(level,0,101,0,0,12);player.setGameMode(GameType.SURVIVAL);player.setHealth(20);player.getFoodData().setFoodLevel(0);
     var wild=ModEntities.BULBASAUR.get().create(level);wild.moveTo(0,101,1.5,180,0);level.addFreshEntity(wild);testEntity=wild.getUUID();wild.hurt(net.minecraft.world.damagesource.DamageSource.playerAttack(player),1);
    });
   }
   if(worldTicks==COMBAT_VALIDATION_TICK)mc.getSingleplayerServer().execute(()->{
    var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var wild=(MinemonCreature)mc.getSingleplayerServer().overworld().getEntity(testEntity);
    combatPassed=combatHits>0&&player.getHealth()<20&&player.isAlive();
    if(!combatPassed)throw new IllegalStateException("Connected Survival player did not receive retaliation damage; health="+player.getHealth()+" target="+wild.getTarget());
    try{Files.writeString(Path.of("client-validation.txt"),"PASS COMBAT: connected Survival player received melee retaliation; damage events="+combatHits+" health="+player.getHealth()+"\n",StandardOpenOption.APPEND);}catch(java.io.IOException e){throw new RuntimeException(e);}
    player.setGameMode(GameType.CREATIVE);player.setHealth(20);player.teleportTo(mc.getSingleplayerServer().overworld(),0,101,0,0,12);player.fallDistance=0;
   });
   if(worldTicks>=SPECIES_START_TICK && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW && (worldTicks-SPECIES_START_TICK)%SPECIES_TEST_WINDOW==0) {
    String slug=species.get((worldTicks-SPECIES_START_TICK)/SPECIES_TEST_WINDOW);
    reportProgress(slug,"carregamento");
    mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();var previous=level.getEntity(testEntity);if(previous!=null)previous.discard();
    boolean aquatic=fixtureWater(slug);
    // Still, contained water is required for an actual zero-input idle:
    // exposed sources above the platform push large entities like Gyarados.
    // Centered at z=4, Lapras extends to z=2.8 (width 2.4). The old
    // z=3..5 pool intersected its hitbox with the solid rim at z=2,
    // allowing collision step-up to select ground_idle during final rest.
    // Leave clearance for the complete roster, not just the entity center.
    for(int x=-9;x<=9;x++)for(int z=1;z<=7;z++){
     boolean pool=aquatic&&x>=-8&&x<=8&&z>=2&&z<=6;
     level.setBlockAndUpdate(new BlockPos(x,96,z),Blocks.STONE.defaultBlockState());
     for(int y=97;y<=100;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),(pool?Blocks.WATER:Blocks.STONE).defaultBlockState());
     level.setBlockAndUpdate(new BlockPos(x,101,z),Blocks.AIR.defaultBlockState());
    }
    var next=ModEntities.SPECIES.get(slug).get().create(level);
    if(fixtureAir(slug)&&SpeciesBehaviorProfiles.profile(slug)==SpeciesBehaviorProfiles.Profile.GROUND_FLIGHT){
     var saved=new net.minecraft.nbt.CompoundTag();next.addAdditionalSaveData(saved);saved.putString("NaturalBehaviorPhase","AIR");next.readAdditionalSaveData(saved);
    }
    double waterY=fixtureSurface(slug)?101.0D-WaterMovementGoal.preferredSurfaceDepth(next):101.0D-next.getBbHeight()-0.35D;
    next.moveTo(0,aquatic?waterY:101.0D,4,180,0);next.setNoAi(false);next.goalSelector.removeAllGoals();next.targetSelector.removeAllGoals();setValidationPhase(next,0);level.addFreshEntity(next);testEntity=next.getUUID();
    });
   }
   if(worldTicks>=SPECIES_START_TICK && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW)
    applyValidationInput(mc,Math.floorMod(worldTicks-SPECIES_START_TICK,SPECIES_TEST_WINDOW));
   if(worldTicks>=SPECIES_START_TICK+20 && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW && (worldTicks-(SPECIES_START_TICK+20))%SPECIES_TEST_WINDOW==0)Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
   if(worldTicks>=SPECIES_START_TICK && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW && ((worldTicks-SPECIES_START_TICK)%SPECIES_TEST_WINDOW)==40
       && java.util.Set.of("charmander","charmeleon","charizard").contains(species.get((worldTicks-SPECIES_START_TICK)/SPECIES_TEST_WINDOW)))Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
   if(worldTicks==SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW) {
    if(!combatPassed)throw new IllegalStateException("Survival combat check did not pass");
    if(!renderedSpecies.containsAll(species))throw new IllegalStateException("Some species were not rendered: expected="+species+" actual="+renderedSpecies);
    for(String slug:species) {
     boolean bodyMoved=motionRanges.entrySet().stream().filter(e->e.getKey().startsWith(slug+":")).anyMatch(e->{for(int axis=0;axis<6;axis++)if(e.getValue()[axis+6]-e.getValue()[axis]>0.0001)return true;return false;});
     var definition=SpeciesDefinitions.get(slug);
     boolean staticFallback=definition!=null&&"static".equals(definition.movementFallback());
     if(!walkedSpecies.contains(slug)||(!staticFallback&&!bodyMoved))throw new IllegalStateException("No rendered movement for "+slug+"; displacement="+walkedSpecies.contains(slug)+" bodyChanged="+bodyMoved+" staticFallback="+staticFallback);
    }
    for(String slug:java.util.List.of("charmander","charmeleon","charizard")) {
     if(!species.contains(slug))continue;
     float[] arms=fireStarterArmRanges.get(slug);
     // Native Charizard air_fly arms are 17.5 +/- 1 degrees;
     // the adapted ground gait uses 55 degrees. Validate the active source
     // flight pose without weakening the other starters' ground regression.
     float minimumArm=slug.equals("charizard")&&fixtureAir(slug)?(float)Math.toRadians(16.0D):0.35F;
     float maximumArm=slug.equals("charizard")&&fixtureAir(slug)?(float)Math.toRadians(19.0D):Float.MAX_VALUE;
     boolean walkArms=arms!=null&&arms[0]>=minimumArm&&arms[1]>=minimumArm
       &&arms[0]<=maximumArm&&arms[1]<=maximumArm;
     boolean fasterArms=slug.equals("charmander")||(arms!=null&&arms[2]>=minimumArm&&arms[3]>=minimumArm&&arms[2]<=maximumArm&&arms[3]<=maximumArm);
     if(!walkArms||!fasterArms)
      throw new IllegalStateException("Fire-starter arms remained near T-pose during validated movement for "+slug+"; arm rotations="+java.util.Arrays.toString(arms));
    }
    validateArticulationProfiles();
    if(species.contains("gastly")) {
     var particleJson=new ResourceLocation(Minemon.MOD_ID,"particles/gastly_ghostly_smoke.json");
     if(mc.getResourceManager().getResource(particleJson).isEmpty())
      throw new IllegalStateException("Missing Gastly Cobblemon smoke particle definition");
     int smokeParticles=br.com.petrickfaria.minemon.client.GastlySmokeParticle.spawnCountForValidation();
     if(smokeParticles<10)throw new IllegalStateException("Gastly's native idle/walk smoke emitter did not create client particles: "+smokeParticles);
     Files.writeString(Path.of("client-validation.txt"),"PASS GASTLY SMOKE: Cobblemon ghostly_smoke definition loaded and "+smokeParticles+" client particle instances were created during the full render smoke. Manual appearance review remains separate.\\n",StandardOpenOption.APPEND);
    }
    for(String slug:java.util.List.of("abra","kadabra","alakazam","bellsprout","geodude","golem","slowpoke","slowbro","mewtwo","mew")) {
     if(!species.contains(slug))continue;
     float[] look=sourceLookRanges.get(slug);
     if(look==null||look[2]-look[0]<0.05F||look[3]-look[1]<0.05F)
      throw new IllegalStateException("Source q.look adaptation did not move the head for "+slug+": "+java.util.Arrays.toString(look));
    }
    var selectedPidge=java.util.Set.of("pidgeotto","pidgeot").stream().filter(species::contains).collect(java.util.stream.Collectors.toSet());
    if(!wingVisibilitySpecies.containsAll(selectedPidge))
     throw new IllegalStateException("Pidge wing visibility was not sampled for selected models "+selectedPidge+": "+wingVisibilitySpecies);
    Files.writeString(Path.of("client-validation.txt"),"PASS LOCOMOTION: every selected species moved and had changing body bone transforms.\n",StandardOpenOption.APPEND);
    if(!selectedPidge.isEmpty())Files.writeString(Path.of("client-validation.txt"),"PASS PIDGE WINGS: selected Pidgeotto/Pidgeot rendered the open pair only in settled idle/walk/run poses; closed alternatives stayed hidden.\n",StandardOpenOption.APPEND);
    for(String slug:java.util.List.of("mewtwo","mew"))if(species.contains(slug)) {
     float[] pitch=sourcePitchRanges.get(slug);
     if(pitch==null||pitch[1]-pitch[0]<0.05F)throw new IllegalStateException("Source q.pitch_tilt adaptation did not move "+slug+"'s body: "+java.util.Arrays.toString(pitch));
    }
    var selectedLook=java.util.List.of("abra","kadabra","alakazam","bellsprout","geodude","golem","slowpoke","slowbro","mewtwo","mew").stream().filter(species::contains).toList();
    if(!selectedLook.isEmpty())Files.writeString(Path.of("client-validation.txt"),"PASS SOURCE LOOK: selected head bones respond to source poser look input: "+selectedLook+"\n",StandardOpenOption.APPEND);
    for(String slug:java.util.List.of("mewtwo","mew"))if(species.contains(slug))Files.writeString(Path.of("client-validation.txt"),"PASS SOURCE PITCH: "+slug+" source q.pitch_tilt input moves the body bone: "+java.util.Arrays.toString(sourcePitchRanges.get(slug))+"\n",StandardOpenOption.APPEND);
    var selectedStarters=java.util.List.of("charmander","charmeleon","charizard").stream().filter(species::contains).toList();
    if(!selectedStarters.isEmpty())Files.writeString(Path.of("client-validation.txt"),"PASS FIRE-STARTER ARMS: validated walk and faster-movement poses remain lowered: "+selectedStarters+"; Charmander uses no source run clip.\n",StandardOpenOption.APPEND);
    var selectedFireHorses=java.util.List.of("ponyta","rapidash").stream().filter(species::contains).toList();
    for(String slug:selectedFireHorses) {
     var samples=MinemonRenderer.fireOverlayFrameSamples().getOrDefault(slug,java.util.Set.of());
     var bones=MinemonRenderer.fireOverlayBoneSamples().getOrDefault(slug,java.util.Set.of());
     if(!samples.containsAll(java.util.Set.of(0,1,2,3))||bones.isEmpty())
      throw new IllegalStateException("Animated original fire overlay did not render all frames/bones for "+slug+": frames="+samples+" bones="+bones);
    }
    if(!selectedFireHorses.isEmpty())Files.writeString(Path.of("client-validation.txt"),"PASS PONYTA/RAPIDASH FIRE OVERLAYS: source 10 fps atlas frames rendered on source fire bones: "+selectedFireHorses+"; manual visual approval remains separate.\n",StandardOpenOption.APPEND);
    var selectedEmissive=java.util.List.of("charmander","charmeleon","charizard","moltres").stream().filter(species::contains).toList();
    for(String slug:selectedEmissive) {
     var samples=MinemonRenderer.fireOverlayFrameSamples().getOrDefault(slug,java.util.Set.of());
     var bones=MinemonRenderer.fireOverlayBoneSamples().getOrDefault(slug,java.util.Set.of());
     if(!samples.contains(0)||bones.isEmpty())throw new IllegalStateException("Full-bright flame atlas did not render on fire rig for "+slug+": frames="+samples+" bones="+bones);
    }
    if(!selectedEmissive.isEmpty())Files.writeString(Path.of("client-validation.txt"),"PASS EMISSIVE FIRE: full-bright native flame atlas rendered on starter tails and Moltres wings: "+selectedEmissive+".\n",StandardOpenOption.APPEND);
    Files.writeString(Path.of("client-validation.txt"),"PASS ROSTER: every selected species rendered with its texture in a world: "+renderedSpecies+"\n",StandardOpenOption.APPEND);
    completedSpeciesWindows=species.size();
    lastApprovedCheck="all selected species completed render/movement sampling";
    for(String gas:java.util.List.of("koffing","weezing"))if(species.contains(gas)){
     int particles=smokeParticleSamples.getOrDefault(gas,0);
     if(particles<2)throw new IllegalStateException("Real smoke particles were not emitted for "+gas+": "+particles);
     Files.writeString(Path.of("client-validation.txt"),"PASS GAS SMOKE: "+gas+" real Minecraft smoke particles="+particles+"\n",StandardOpenOption.APPEND);
    }
    mc.stop();
   }
  }catch(Exception e){throw new IllegalStateException("Minemon client validation failed",e);}
 }
  private static void applyValidationInput(Minecraft mc,int phase) {
    mc.getSingleplayerServer().execute(()->{
    var current=(MinemonCreature)mc.getSingleplayerServer().overworld().getEntity(testEntity);
     if(current!=null){
     setValidationPhase(current,phase);
     // Keep the fixture's movement deterministic while still exercising its real travel
     // and render path. The goals are removed at spawn, so no pathfinding competes with
     // the controlled velocity used to select the locomotion clips.
     current.setNoAi(false);current.getNavigation().stop();
     if(current.getX()>2.5)direction=-1;if(current.getX()<-2.5)direction=1;
     double animationVelocity=animationVelocity(phase);
     // Preserve ordinary land gravity: resetting Y to zero on every server
     // update prevents the negative collision step that maintains onGround,
     // which correctly disqualifies a production creature from ground runs.
     double vertical=current.isFlyingSpecies()||current.isAquaticSpecies()&&current.isInWater()?0.0D:current.getDeltaMovement().y;
     if(animationVelocity>0.0D){
      current.setDeltaMovement(direction*animationVelocity,vertical,0);
      current.hasImpulse=true;
     } else {
      current.setDeltaMovement(0,vertical,0);
     }
    }
    });
  }
  private static double animationVelocity(int phase) {
   if(phase<8 || phase>=54)return 0.0D;
   if(phase<16)return (phase-7)*0.00625D;
   if(phase<24)return 0.05D;
   if(phase<27)return (27-phase)*0.0166667D;
   if(phase<34)return 0.0D;
   if(phase<38)return (phase-33)*0.03D;
   if(phase<47)return 0.12D;
   if(phase<49)return 0.16D;
   if(phase<52)return 0.05D;
   return 0.0D;
  }
  private static boolean isSettledClipSample(int phase) {
   if(!clipHasSettled(renderedPhaseSpecies))return false;
   // Ignore samples at phase boundaries; the model renderer can run before
   // GeckoLib has evaluated this tick's controller predicate.
   return (phase>=3&&phase<8) || (phase>=12&&phase<16) || (phase>=18&&phase<24)
     || (phase>=25&&phase<27) || (phase>=29&&phase<34) || (phase>=36&&phase<38)
     || (phase>=41&&phase<47) || (phase>=47&&phase<49) || (phase>=50&&phase<54) || phase>=68;
  }
  private static final String VALIDATION_PHASE_PREFIX="minemon-validation-phase:";
  private static void setValidationPhase(MinemonCreature entity,int phase) {
   if(!Boolean.getBoolean("minemon.clientSmokeTest"))return;
   String marker=VALIDATION_PHASE_PREFIX+phase;
   boolean phaseChanged=entity.getCustomName()==null||!marker.equals(entity.getCustomName().getString());
   entity.setCustomName(net.minecraft.network.chat.Component.literal(marker));
   entity.setCustomNameVisible(false);
   entity.setSpontaneousRunForValidation(phase>=34&&phase<50);
   if(phaseChanged&&entity.level instanceof net.minecraft.server.level.ServerLevel level) {
    // Each of the 80 fixture phases still waits for two real rendered samples.
    // Send the changed phase through the normal network packet immediately,
    // rather than adding the entity tracker's multi-tick delay to every phase.
    level.getChunkSource().broadcastAndSend(entity,new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
        entity.getId(),entity.getEntityData(),true));
   }
  }
  private static Integer validationPhase(MinemonCreature entity) {
   if(!Boolean.getBoolean("minemon.clientSmokeTest")||entity.getCustomName()==null)return null;
   String marker=entity.getCustomName().getString();
   if(!marker.startsWith(VALIDATION_PHASE_PREFIX))return null;
   try {int phase=Integer.parseInt(marker.substring(VALIDATION_PHASE_PREFIX.length()));return phase>=0&&phase<SPECIES_TEST_WINDOW?phase:null;}
   catch(NumberFormatException ignored) {return null;}
  }
  private static String locomotionState(int phase) {
   if(phase<8)return "IDLE_INITIAL";
   if(phase<16)return "WALK_START";
   if(phase<24)return "WALK_STEADY";
   if(phase<27)return "WALK_DECEL";
   if(phase<34)return "IDLE_RETURN";
   if(phase<38)return "RUN_START";
   if(phase<47)return "RUN_STEADY";
   if(phase<54)return "RUN_DECEL";
   return "IDLE_FINAL";
  }
  private static boolean hasSelectedClip(String slug,String state,String clip) {
   return clip!=null && articulationClips.getOrDefault(slug,java.util.Map.of()).getOrDefault(state,java.util.Set.of()).contains(clip);
  }
  private static boolean hasVelocitySample(String slug,String state,String clip,boolean aboveRunThreshold) {
   for(String sample:articulationInputs.getOrDefault(slug,java.util.Map.of()).getOrDefault(state,java.util.Set.of())) {
    if(!sample.contains("clip="+clip+",velocitySqr="))continue;
    int start=sample.indexOf("velocitySqr=")+12;int end=sample.indexOf(',',start);
    if(end<0)continue;
    double speedSqr=Double.parseDouble(sample.substring(start,end));
    if((speedSqr>0.01D)==aboveRunThreshold)return true;
   }
   return false;
  }
  private static int movingJointCount(String slug,String state) {
   int moved=0;
   for(String joint:profileJoints(slug,state)) {
    float[] range=articulationRanges.getOrDefault(slug,java.util.Map.of()).get(state+":"+joint);
    if(range==null)continue;
    for(int axis=0;axis<6;axis++)if(range[axis+6]-range[axis]>(axis<3?0.02F:0.05F)){moved++;break;}
   }
   return moved;
  }
  private static boolean sourceStaticIdle(String slug) {
   return ADDITIONAL_PROFILES.getOrDefault(slug,java.util.Map.of()).containsKey("idle_static");
  }
  private static boolean sourcePoseTransition(String slug) {
   return ADDITIONAL_PROFILES.getOrDefault(slug,java.util.Map.of()).containsKey("pose_transition");
  }
  private static int validateSourcePoseTransition(String slug,String idleState,String movementState) {
   var profile=ADDITIONAL_PROFILES.getOrDefault(slug,java.util.Map.of()).get("pose_transition");
   if(profile==null||profile.isEmpty())throw new IllegalStateException("Missing source pose transition profile for "+slug);
   if(articulationSampleCounts.getOrDefault(slug,java.util.Map.of()).getOrDefault(idleState,0)<2
      ||articulationSampleCounts.getOrDefault(slug,java.util.Map.of()).getOrDefault(movementState,0)<2)
    throw new IllegalStateException("Insufficient settled pose samples for "+slug+" states="+idleState+"/"+movementState);
   int transitioned=0;
   for(String joint:profile) {
    float[] idle=articulationRanges.getOrDefault(slug,java.util.Map.of()).get(idleState+":"+joint);
    float[] movement=articulationRanges.getOrDefault(slug,java.util.Map.of()).get(movementState+":"+joint);
    if(idle==null||movement==null)throw new IllegalStateException("Missing settled source pose "+slug+":"+joint);
    for(int axis=0;axis<6;axis++) {
     float tolerance=axis<3?0.02F:0.05F;
     if(!Float.isFinite(idle[axis])||!Float.isFinite(idle[axis+6])||idle[axis+6]<idle[axis]
        ||idle[axis+6]-idle[axis]>tolerance||!Float.isFinite(movement[axis])
        ||!Float.isFinite(movement[axis+6])||movement[axis+6]<movement[axis]
        ||movement[axis+6]-movement[axis]>tolerance)
      throw new IllegalStateException("Native source pose should settle without invented cyclic movement for "+slug+":"+joint);
     float idlePose=(idle[axis]+idle[axis+6])*0.5F;
     float movementPose=(movement[axis]+movement[axis+6])*0.5F;
     if(Math.abs(movementPose-idlePose)>tolerance){transitioned++;break;}
    }
   }
   if(transitioned<1)throw new IllegalStateException("Native movement pose did not articulate any profiled joint for "+slug);
   return transitioned;
  }
  private static void validateSourceStaticIdle(String slug) {
   if(articulationSampleCounts.getOrDefault(slug,java.util.Map.of()).getOrDefault("IDLE_FINAL",0)<2)
    throw new IllegalStateException("Insufficient static idle samples for "+slug);
   for(String joint:profileJoints(slug,"IDLE_FINAL")) {
    float[] range=articulationRanges.getOrDefault(slug,java.util.Map.of()).get("IDLE_FINAL:"+joint);
    if(range==null)throw new IllegalStateException("Missing static idle pose "+slug+":"+joint);
    for(int axis=0;axis<6;axis++)
     if(!Float.isFinite(range[axis])||!Float.isFinite(range[axis+6])||range[axis+6]<range[axis]
        ||range[axis+6]-range[axis]>(axis<3?0.02F:0.05F))
      throw new IllegalStateException("Native static idle pose was not stable for "+slug+":"+joint);
   }
  }
  private static void validateArticulationProfiles() throws java.io.IOException {
   StringBuilder report=new StringBuilder();
   int passed=0,knownFallbacks=0;
   for(String slug:java.util.List.of("charmander","charmeleon","charizard")) {
    if(!species.contains(slug))continue;
    var ranges=tailFlameRanges.getOrDefault(slug,java.util.Map.of());var flameIdle=ranges.get("idle");var flameWalk=ranges.get("walk");
    String expected="animation."+slug+".minemon_tail_flame";
    boolean animatedFlame=flameIdle!=null&&flameWalk!=null&&tailFlameClips.getOrDefault(slug,java.util.Set.of()).contains(expected);
    for(int axis=0;axis<3&&animatedFlame;axis++)animatedFlame=flameIdle[axis+3]-flameIdle[axis]>0.01F&&flameWalk[axis+3]-flameWalk[axis]>0.01F;
    if(!animatedFlame)throw new IllegalStateException(slug+" tail flame did not animate independently during idle and walk; idle="+java.util.Arrays.toString(flameIdle)+" walk="+java.util.Arrays.toString(flameWalk)+" selected="+tailFlameClips.get(slug));
    Files.writeString(Path.of("client-validation.txt"),"PASS "+slug.toUpperCase(java.util.Locale.ROOT)+" TAIL FLAME: independent clip="+tailFlameClips.get(slug)+" changed in idle range="+java.util.Arrays.toString(flameIdle)+" and walk range="+java.util.Arrays.toString(flameWalk)+".\n",StandardOpenOption.APPEND);
   }
   for(var entry:ARTICULATION_PROFILES.entrySet()) {
    String slug=entry.getKey();
    if(!ARTICULATION_VALIDATION_SPECIES.contains(slug)||!species.contains(slug))continue;
    var definition=SpeciesDefinitions.get(slug);
    if(definition==null)throw new IllegalStateException("Missing test profile species "+slug);
    String fallback=definition.movementFallback();
    if(slug.equals("fearow")&&fixtureAir(slug)) {
     String air="animation.fearow.air_idle";
     for(String state:java.util.List.of("IDLE_INITIAL","WALK_START","WALK_STEADY","WALK_DECEL",
         "IDLE_RETURN","RUN_START","RUN_STEADY","RUN_DECEL","IDLE_FINAL"))
      if(!hasSelectedClip(slug,state,air))throw new IllegalStateException("Fearow native air pose missing in "+state);
     if(!fearowNativeAirPoseObserved)throw new IllegalStateException("Fearow native air pose was not rendered");
     validateSourceStaticIdle(slug);
     knownFallbacks++;
     report.append("SOURCE STATIC AIR POSE fearow: native air_idle selected in all phases; actual body pitch and opposite open-wing angles verified; no native fly cycle exists and no animated aerial gait pass is claimed.\n");
     continue;
    }
    if("static".equals(fallback)||"idle".equals(fallback)) {
    String expected=expectedWalk(slug,definition);
     if(!hasSelectedClip(slug,"WALK_STEADY",expected))throw new IllegalStateException("Fallback classification did not match selected walk clip for "+slug);
     if(ADDITIONAL_PROFILES.containsKey(slug)) {
      if(!hasSelectedClip(slug,"IDLE_INITIAL",expectedIdle(slug,definition))||!hasSelectedClip(slug,"IDLE_FINAL",expectedIdle(slug,definition)))
       throw new IllegalStateException("Source idle fallback did not select the expected clip for "+slug);
      if(sourceStaticIdle(slug))validateSourceStaticIdle(slug);
      else if(movingJointCount(slug,"IDLE_FINAL")<1)
       throw new IllegalStateException("Animated source idle fallback did not animate for "+slug);
     }
     knownFallbacks++;
     report.append("EXPECTED GAP ").append(slug).append(": fallback=").append(fallback)
       .append("; selected walk=").append(expected).append("; no gait pass claimed.\n");
     continue;
    }
    String walk=expectedWalk(slug,definition);
    String run=fixtureAir(slug)||fixtureWater(slug)
      ?null:SpeciesAssetIndex.clip(slug,"ground_run","run");
    String idle=expectedIdle(slug,definition);
    if(!hasSelectedClip(slug,"IDLE_INITIAL",idle)||!hasSelectedClip(slug,"IDLE_FINAL",idle)||!hasSelectedClip(slug,"IDLE_RETURN",idle))
     throw new IllegalStateException("Idle state selection did not return to expected clip for "+slug+" expected="+idle+" observed="+articulationClips.get(slug));
    if(!hasSelectedClip(slug,"WALK_STEADY",walk)||!hasSelectedClip(slug,"WALK_START",walk)||!hasSelectedClip(slug,"WALK_DECEL",walk))
     throw new IllegalStateException("Walk state selection failed for "+slug+" expected="+walk+" states="+articulationClips.get(slug)+" samples="+articulationSampleCounts.get(slug)+" measured="+articulationInputs.get(slug));
    if(run!=null&&!hasSelectedClip(slug,"RUN_STEADY",run))
     throw new IllegalStateException("Run state selection failed for "+slug+" expected="+run+" states="+articulationClips.get(slug));
    if(run!=null) {
     var deceleration=articulationClips.getOrDefault(slug,java.util.Map.of()).getOrDefault("RUN_DECEL",java.util.Set.of());
     boolean observedLowerSpeed=hasVelocitySample(slug,"RUN_DECEL",walk,false)||hasVelocitySample(slug,"RUN_DECEL",idle,false);
     if(!deceleration.contains(run)||java.util.Collections.disjoint(deceleration,java.util.Set.of(walk,idle))
        ||!hasVelocitySample(slug,"RUN_DECEL",run,true)||!observedLowerSpeed)
      throw new IllegalStateException("Run deceleration did not transition from run to a lower-speed state for "+slug+" run="+run+" walk="+walk+" idle="+idle+" observed="+deceleration+" measured="+articulationInputs.getOrDefault(slug,java.util.Map.of()).get("RUN_DECEL"));
    }
    int walkJoints=movingJointCount(slug,"WALK_STEADY");
    int runJoints=run==null?0:movingJointCount(slug,"RUN_STEADY");
    int minimum=slug.equals("charmander")?2:1;
    int idleJoints=movingJointCount(slug,"IDLE_FINAL");
    boolean staticIdle=sourceStaticIdle(slug)||(slug.equals("fearow")&&fixtureAir(slug));
    if(staticIdle)validateSourceStaticIdle(slug);
    boolean poseTransition=sourcePoseTransition(slug);
    int transitionedJoints=poseTransition?validateSourcePoseTransition(slug,"IDLE_FINAL","WALK_STEADY"):0;
    if((!staticIdle&&!poseTransition&&idleJoints<minimum)||(!poseTransition&&walkJoints<minimum)
       ||(poseTransition&&(idleJoints!=0||walkJoints!=0))||(run!=null&&runJoints<minimum))
     throw new IllegalStateException("Relevant locomotion joints did not animate for "+slug+" idle="+idleJoints+" walk="+walkJoints+" run="+runJoints+" required="+minimum);
    if(slug.equals("fearow")&&fixtureAir(slug))report.append("FEAROW FLIGHT: source air_idle remains static; compatible air_fly clip passed wing articulation sampling.\n");
    else if(staticIdle)report.append("SOURCE STATIC IDLE ").append(slug).append(": native stance stable; no idle oscillation claimed.\n");
    if(poseTransition)report.append("SOURCE STATIC POSE TRANSITION ").append(slug).append(": native idle and movement clips are stable poses; profiled joints changed between settled poses=").append(transitionedJoints).append(".\n");
    passed++;
    report.append("PASS ").append(slug).append(": idle/walk").append(run==null?" (no source run clip)":"/run").append(" clips selected; profile joints changed idle=")
      .append(idleJoints).append(" walk=").append(walkJoints).append(" run=").append(runJoints).append("; transition samples=")
      .append(articulationClips.get(slug)).append("; synchronized inputs=").append(articulationInputs.get(slug)).append(".\n");
   }
   var selectedProfiles=ARTICULATION_VALIDATION_SPECIES.stream().filter(species::contains).collect(java.util.stream.Collectors.toSet());
   Files.writeString(Path.of("client-validation.txt"),"ARTICULATION PROFILES: passed="+passed+" expected fallback gaps="+knownFallbacks+" selected profiles="+selectedProfiles.size()+" detailed scope="+selectedProfiles+"; unselected species were not tested in this targeted run.\n"+report,StandardOpenOption.APPEND);
  }
  private static String expectedIdle(String slug,SpeciesDefinitions.Definition definition) {
   if(fixtureWater(slug)) {
    String water=fixtureSurface(slug)?firstMappedClip(definition,"surface_water_idle","surfacewater_idle","water_surface_idle","water_idle"):firstMappedClip(definition,"water_idle","water_surface_idle","surface_water_idle","surfacewater_idle");
    if(water!=null)return water;
   }
   String air=fixtureAir(slug)?SpeciesAssetIndex.clip(slug,"air_idle","air_idle"):null;
   return air==null?definition.idle():air;
  }
  private static String expectedWalk(String slug,SpeciesDefinitions.Definition definition) {
   if(fixtureWater(slug)) {
    String water=fixtureSurface(slug)?firstMappedClip(definition,"surface_water_swim","surfacewater_swim","water_surface_swim","water_swim"):firstMappedClip(definition,"water_swim","water_surface_swim","surface_water_swim","surfacewater_swim");
    if(water!=null)return water;
   }
   if(!fixtureAir(slug))return definition.walk();
   String air=SpeciesAssetIndex.clip(slug,"air_fly","air_fly");
   if(air==null)air=SpeciesAssetIndex.clip(slug,"air_glide","air_glide");
   if(air==null)air=SpeciesAssetIndex.clip(slug,"air_idle","air_idle");
   return air==null?definition.walk():air;
  }
  private static String firstMappedClip(SpeciesDefinitions.Definition definition,String... keys) {
   for(String key:keys) {String clip=definition.clips().get(key);if(clip!=null)return clip;}
   return null;
  }
}
