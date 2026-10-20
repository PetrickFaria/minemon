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
 private static final java.util.Set<String> walkedSpecies=new java.util.HashSet<>();
 private static final java.util.Map<String,net.minecraft.world.phys.Vec3> lastRenderedPositions=new java.util.HashMap<>();
 @SubscribeEvent public static void actualDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
  if(Boolean.getBoolean("minemon.clientSmokeTest") && event.getAmount()>0 && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer
     && event.getSource().getEntity() instanceof MinemonCreature b && b.getUUID().equals(testEntity))combatHits++;
 }
 private static int direction=1;
 private static final int SPECIES_TEST_WINDOW=80;
 private static final int BULBASAUR_VALIDATION_TICK=430;
 private static final int COMBAT_VALIDATION_TICK=BULBASAUR_VALIDATION_TICK+100;
 private static final int SPECIES_START_TICK=COMBAT_VALIDATION_TICK+10;
 private static final java.util.Set<String> renderedSpecies=java.util.concurrent.ConcurrentHashMap.newKeySet();
 private static final java.util.List<String> species=SpeciesDefinitions.ALL.values().stream()
   .sorted(java.util.Comparator.comparingInt(SpeciesDefinitions.Definition::nationalPokedexNumber))
   .map(SpeciesDefinitions.Definition::slug).toList();
  // Profiles use exact joints from each selected model skeleton, not one global bone-name list.
  private static final java.util.Map<String,java.util.List<String>> ARTICULATION_PROFILES=java.util.Map.ofEntries(
   java.util.Map.entry("bulbasaur",java.util.List.of("leg_front_right","leg_front_left","leg_back_right","leg_back_left")),
   java.util.Map.entry("pikachu",java.util.List.of("leg_right","leg_left")),
   java.util.Map.entry("caterpie",java.util.List.of("body_segment","body_segment2")),
   java.util.Map.entry("hitmonlee",java.util.List.of("leg_left","leg_right")),
   java.util.Map.entry("onix",java.util.List.of("segment1","segment7","segment14")),
   java.util.Map.entry("psyduck",java.util.List.of("foot_left","foot_right")),
   java.util.Map.entry("jolteon",java.util.List.of("leg_front_right","leg_front_left","leg_back_right","leg_back_left")),
   java.util.Map.entry("nidorina",java.util.List.of("left_leg","right_leg")),
   java.util.Map.entry("meowth",java.util.List.of("leg_left","leg_right"))
  );
  private static final java.util.Map<String,java.util.Map<String,float[]>> articulationRanges=new java.util.HashMap<>();
  private static final java.util.Map<String,java.util.Map<String,java.util.Set<String>>> articulationClips=new java.util.HashMap<>();
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
  if(!Boolean.getBoolean("minemon.clientSmokeTest")||event.phase!=TickEvent.Phase.END)return;
  Minecraft mc=Minecraft.getInstance();
  try {
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
    var dataFile=Path.of("saves/minemon-validation/level.dat").toFile();var tag=net.minecraft.nbt.NbtIo.readCompressed(dataFile);tag.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);net.minecraft.nbt.NbtIo.writeCompressed(tag,dataFile);
    Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);boolean invoked=false;
    for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")) {
     if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");invoked=true;break;}
     if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");invoked=true;break;}
    }
    if(!invoked)throw new IllegalStateException("Could not locate WorldOpenFlows.loadLevel");
    MinemonRenderer.validationObserver=(entity,model)->{
     if(mc.getResourceManager().getResource(model.getTextureResource(entity)).isEmpty())throw new IllegalStateException("Missing rendered texture "+entity.getSpeciesSlug());
     String slug=entity.getSpeciesSlug();renderedSpecies.add(slug);
     var position=entity.position();var previous=lastRenderedPositions.put(slug,position);
     if(previous!=null&&previous.distanceToSqr(position)>0.0001D) {
      walkedSpecies.add(slug);
     }
     // Sample every render frame, independently of position updates. Filtering
     // these samples by displacement can alias animation keyframes when the
     // client batches several server ticks into a single render frame.
     if(worldTicks>=SPECIES_START_TICK && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW) {
      if(slug.equals("charmander")||slug.equals("charmeleon")||slug.equals("charizard")) {
       var left=model.getAnimationProcessor().getBone("arm_left");var right=model.getAnimationProcessor().getBone("arm_right");
       int phase=Math.floorMod(worldTicks-SPECIES_START_TICK,SPECIES_TEST_WINDOW);
       int mode=phase>=38&&phase<=46?2:(phase>=16&&phase<=23?0:-1);
       if(mode>=0){
        var range=fireStarterArmRanges.computeIfAbsent(slug,k->new float[4]);
        range[mode]=Math.max(range[mode],Math.abs(left.getRotationZ()));
        range[mode+1]=Math.max(range[mode+1],Math.abs(right.getRotationZ()));
       }
      }
      for(String name:modelBones.get(slug)) {
       if(name.contains("eye")||name.contains("mouth")||name.contains("jaw"))continue;
       var bone=model.getAnimationProcessor().getBone(name);float[] values={bone.getRotationX(),bone.getRotationY(),bone.getRotationZ(),bone.getPositionX(),bone.getPositionY(),bone.getPositionZ()};
       var range=motionRanges.computeIfAbsent(slug+":"+name,k->{var r=new float[12];java.util.Arrays.fill(r,0,6,Float.MAX_VALUE);java.util.Arrays.fill(r,6,12,-Float.MAX_VALUE);return r;});
       for(int axis=0;axis<6;axis++){range[axis]=Math.min(range[axis],values[axis]);range[axis+6]=Math.max(range[axis+6],values[axis]);}
      }
     }
      if(worldTicks>=SPECIES_START_TICK && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW && ARTICULATION_PROFILES.containsKey(slug)) {
       int phase=Math.floorMod(worldTicks-SPECIES_START_TICK,SPECIES_TEST_WINDOW);
       String state=locomotionState(phase);
       // Clip selection is sampled only after a phase has settled. The first render
       // frame at a boundary can still expose the previous controller value.
       if(isSettledClipSample(phase)) {
        articulationClips.computeIfAbsent(slug,k->new java.util.HashMap<>())
         .computeIfAbsent(state,k->new java.util.HashSet<>()).add(entity.getSelectedAnimationForValidation());
       }
       var perSpecies=articulationRanges.computeIfAbsent(slug,k->new java.util.HashMap<>());
       for(String joint:ARTICULATION_PROFILES.get(slug)) {
        var bone=model.getAnimationProcessor().getBone(joint);
        if(bone==null)throw new IllegalStateException("Locomotion profile references missing bone "+slug+":"+joint);
        float[] values={bone.getRotationX(),bone.getRotationY(),bone.getRotationZ(),bone.getPositionX(),bone.getPositionY(),bone.getPositionZ()};
        var range=perSpecies.computeIfAbsent(state+":"+joint,k->{var v=new float[12];java.util.Arrays.fill(v,0,6,Float.MAX_VALUE);java.util.Arrays.fill(v,6,12,-Float.MAX_VALUE);return v;});
        for(int axis=0;axis<6;axis++){range[axis]=Math.min(range[axis],values[axis]);range[axis+6]=Math.max(range[axis+6],values[axis]);}
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
   worldTicks++;
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
    mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();var previous=level.getEntity(testEntity);if(previous!=null)previous.discard();
     var next=ModEntities.SPECIES.get(slug).get().create(level);next.moveTo(0,101,4,180,0);next.setNoAi(false);level.addFreshEntity(next);testEntity=next.getUUID();
    });
   }
   if(worldTicks>=SPECIES_START_TICK && worldTicks<SPECIES_START_TICK+species.size()*SPECIES_TEST_WINDOW)mc.getSingleplayerServer().execute(()->{
    var current=(MinemonCreature)mc.getSingleplayerServer().overworld().getEntity(testEntity);
     if(current!=null){
     // Drive a short, controlled displacement so larger collision boxes cannot make this
     // render-only animation check depend on pathfinding success. Navigation has its own tests.
     current.setNoAi(true);current.getNavigation().stop();
     if(current.getX()>2.5)direction=-1;if(current.getX()<-2.5)direction=1;
     int phase=Math.floorMod(worldTicks-SPECIES_START_TICK,SPECIES_TEST_WINDOW);
     double animationVelocity;
     if(phase<8 || phase>=54) animationVelocity=0.0D;                         // idle
     else if(phase<16) animationVelocity=(phase-7)*0.00625D;                   // walk start
     else if(phase<24) animationVelocity=0.05D;                               // steady walk
     else if(phase<27) animationVelocity=(27-phase)*0.0166667D;                // walk deceleration
     else if(phase<34) animationVelocity=0.0D;                                // idle return
     else if(phase<38) animationVelocity=(phase-33)*0.03D;                     // run transition
     else if(phase<47) animationVelocity=0.12D;                               // steady run
     else animationVelocity=Math.max(0.0D,(53-phase)*0.02D);                 // run deceleration
     if(animationVelocity>0.0D){
      double step=animationVelocity<0.08D?0.04D:0.08D;
      current.moveTo(current.getX()+direction*step,current.getY(),current.getZ(),current.getYRot(),current.getXRot());
      current.setDeltaMovement(direction*animationVelocity,0,0);
      current.hasImpulse=true;
     } else {
      current.setDeltaMovement(0,0,0);
      current.getNavigation().stop();
     }
    }
   });
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
     float[] arms=fireStarterArmRanges.get(slug);
     if(arms==null||arms[0]<0.35F||arms[1]<0.35F||arms[2]<0.35F||arms[3]<0.35F)
      throw new IllegalStateException("Fire-starter arms remained near T-pose during walk/run for "+slug+"; arm rotations="+java.util.Arrays.toString(arms));
    }
    validateArticulationProfiles();
    Files.writeString(Path.of("client-validation.txt"),"PASS LOCOMOTION: every registered species moved and had changing body bone transforms.\n",StandardOpenOption.APPEND);
    Files.writeString(Path.of("client-validation.txt"),"PASS FIRE-STARTER ARMS: Charmander, Charmeleon and Charizard kept both arm bones lowered during walk and run.\n",StandardOpenOption.APPEND);
    Files.writeString(Path.of("client-validation.txt"),"PASS ROSTER: every registered species rendered with its texture in a world: "+renderedSpecies+"\n",StandardOpenOption.APPEND);
    mc.stop();
   }
  }catch(Exception e){throw new IllegalStateException("Minemon client validation failed",e);}
 }
  private static boolean isSettledClipSample(int phase) {
   // Ignore samples at phase boundaries; the model renderer can run before
   // GeckoLib has evaluated this tick's controller predicate.
   return (phase>=3&&phase<8) || (phase>=12&&phase<16) || (phase>=18&&phase<24)
     || (phase>=25&&phase<27) || (phase>=29&&phase<34) || (phase>=36&&phase<38)
     || (phase>=41&&phase<47) || (phase>=50&&phase<54) || phase>=68;
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
  private static int movingJointCount(String slug,String state) {
   int moved=0;
   for(String joint:ARTICULATION_PROFILES.getOrDefault(slug,java.util.List.of())) {
    float[] range=articulationRanges.getOrDefault(slug,java.util.Map.of()).get(state+":"+joint);
    if(range==null)continue;
    for(int axis=0;axis<6;axis++)if(range[axis+6]-range[axis]>(axis<3?0.02F:0.05F)){moved++;break;}
   }
   return moved;
  }
  private static void validateArticulationProfiles() throws java.io.IOException {
   StringBuilder report=new StringBuilder();
   int passed=0,knownFallbacks=0;
   for(var entry:ARTICULATION_PROFILES.entrySet()) {
    String slug=entry.getKey();
    var definition=SpeciesDefinitions.get(slug);
    if(definition==null)throw new IllegalStateException("Missing test profile species "+slug);
    String fallback=definition.movementFallback();
    if("static".equals(fallback)||"idle".equals(fallback)) {
     String expected=definition.walk();
     if(!hasSelectedClip(slug,"WALK_STEADY",expected))throw new IllegalStateException("Fallback classification did not match selected walk clip for "+slug);
     knownFallbacks++;
     report.append("EXPECTED GAP ").append(slug).append(": fallback=").append(fallback)
       .append("; selected walk=").append(expected).append("; no gait pass claimed.\n");
     continue;
    }
    String walk=definition.walk();
    String run=SpeciesAssetIndex.clip(slug,"ground_run","run");
    String idle=definition.idle();
    if(!hasSelectedClip(slug,"IDLE_INITIAL",idle)||!hasSelectedClip(slug,"IDLE_FINAL",idle)||!hasSelectedClip(slug,"IDLE_RETURN",idle))
     throw new IllegalStateException("Idle state selection did not return to expected clip for "+slug+" expected="+idle+" observed="+articulationClips.get(slug));
    if(!hasSelectedClip(slug,"WALK_STEADY",walk)||!hasSelectedClip(slug,"WALK_START",walk)||!hasSelectedClip(slug,"WALK_DECEL",walk))
     throw new IllegalStateException("Walk state selection failed for "+slug+" expected="+walk+" states="+articulationClips.get(slug));
    if(run!=null&&!hasSelectedClip(slug,"RUN_STEADY",run))
     throw new IllegalStateException("Run state selection failed for "+slug+" expected="+run+" states="+articulationClips.get(slug));
    int walkJoints=movingJointCount(slug,"WALK_STEADY");
    int runJoints=movingJointCount(slug,"RUN_STEADY");
    int minimum=java.util.Set.of("bulbasaur","pikachu","hitmonlee","jolteon").contains(slug)?2:1;
    if(walkJoints<minimum||runJoints<minimum)
     throw new IllegalStateException("Relevant locomotion joints did not animate for "+slug+" walk="+walkJoints+" run="+runJoints+" required="+minimum);
    passed++;
    report.append("PASS ").append(slug).append(": idle/walk/run/idle clips selected; profile joints changed walk=")
      .append(walkJoints).append(" run=").append(runJoints).append("; transition samples=")
      .append(articulationClips.get(slug)).append(".\n");
   }
   Files.writeString(Path.of("client-validation.txt"),"ARTICULATION PROFILES: passed="+passed+" expected fallback gaps="+knownFallbacks+" profiles="+ARTICULATION_PROFILES.size()+"; other species remain unverified by limb-specific profiles.\n"+report,StandardOpenOption.APPEND);
  }
}
