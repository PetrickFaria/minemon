package br.com.petrickfaria.minemon.entity;

import java.util.*;

/** Explicit assignments from the approved 151-species behavior specification. */
public final class SpeciesBehaviorProfiles {
 public enum Profile { GROUND, ALTITUDE_CYCLE, GROUND_FLIGHT, WATER_VISITOR, SUBMERGED, SURFACE }
 private static final Map<String,Profile> PROFILES=new LinkedHashMap<>();
 private static final Set<String> BOTTOM_PREFERENCE=Set.of("shellder","staryu","starmie","krabby","kingler","omanyte","omastar","kabuto");
 private static final Set<String> LAND_FOR_NIGHT_REST=Set.of("butterfree","beedrill","gengar");
 static {
  assign(Profile.GROUND, "bulbasaur,ivysaur,venusaur,charmander,charmeleon,caterpie,metapod,weedle,kakuna,rattata,raticate,ekans,arbok,pikachu,raichu,sandshrew,sandslash,nidoranf,nidorina,nidoqueen,nidoranm,nidorino,nidoking,clefairy,clefable,vulpix,ninetales,jigglypuff,wigglytuff,oddish,gloom,vileplume,paras,parasect,venonat,diglett,dugtrio,meowth,persian,mankey,primeape,growlithe,arcanine,abra,kadabra,alakazam,machop,machoke,machamp,bellsprout,weepinbell,victreebel,geodude,graveler,golem,ponyta,rapidash,farfetchd,doduo,dodrio,grimer,muk,onix,drowzee,hypno,voltorb,electrode,exeggcute,exeggutor,cubone,marowak,hitmonlee,hitmonchan,lickitung,rhyhorn,rhydon,chansey,tangela,kangaskhan,mrmime,jynx,electabuzz,magmar,pinsir,tauros,ditto,eevee,jolteon,flareon,porygon,snorlax");
  assign(Profile.ALTITUDE_CYCLE, "butterfree,beedrill,spearow,fearow,magnemite,magneton,haunter,gengar,koffing,weezing,articuno,zapdos,moltres");
  assign(Profile.GROUND_FLIGHT, "charizard,scyther,dragonite,pidgey,pidgeotto,pidgeot,zubat,golbat,venomoth,gastly,aerodactyl,mewtwo,mew");
  assign(Profile.WATER_VISITOR, "squirtle,wartortle,blastoise,psyduck,golduck,poliwag,poliwhirl,poliwrath,slowpoke,slowbro,seel,dewgong,krabby,kingler,staryu,starmie,vaporeon,omanyte,omastar,kabuto,kabutops,dratini,dragonair");
  assign(Profile.SUBMERGED, "tentacool,tentacruel,shellder,cloyster,horsea,seadra,goldeen,seaking,magikarp");
  assign(Profile.SURFACE, "gyarados,lapras");
  if(PROFILES.size()!=151)throw new ExceptionInInitializerError("Expected 151 explicit behavior profiles");
 }
 private static void assign(Profile profile,String names) {
  for(String slug:names.split(","))if(PROFILES.put(slug,profile)!=null)throw new ExceptionInInitializerError("Conflicting profile "+slug);
 }
 public static Profile profile(String slug) {
  Profile p=PROFILES.get(slug);if(p==null)throw new IllegalArgumentException("Unassigned species "+slug);return p;
 }
 public static Map<String,Profile> all(){return Collections.unmodifiableMap(PROFILES);}
 public static boolean isAirborne(String slug){return profile(slug)==Profile.ALTITUDE_CYCLE;}
 public static boolean canFly(String slug){return isAirborne(slug)||profile(slug)==Profile.GROUND_FLIGHT;}
 public static boolean isVisitor(String slug){return profile(slug)==Profile.WATER_VISITOR;}
 public static boolean isAquatic(String slug){return profile(slug)==Profile.SUBMERGED||profile(slug)==Profile.SURFACE;}
 public static boolean waterCapable(String slug){return isAquatic(slug)||isVisitor(slug);}
 public static boolean prefersBottom(String slug){return BOTTOM_PREFERENCE.contains(slug);}
 public static boolean landsForNightRest(String slug){return LAND_FOR_NIGHT_REST.contains(slug);}
 public static boolean hoversDuringNightRest(String slug){return isAirborne(slug)&&!landsForNightRest(slug);}
 public static Set<String> airborneSpecies(){return select(true);}
 public static Set<String> aquaticSpecies(){return select(false);}
 private static Set<String> select(boolean air){
  Set<String> result=new HashSet<>();PROFILES.keySet().forEach(s->{if(air?isAirborne(s):isAquatic(s))result.add(s);});return Set.copyOf(result);
 }
 private SpeciesBehaviorProfiles(){}
}
