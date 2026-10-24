package br.com.petrickfaria.minemon.entity;

import java.util.*;

/** Additions only where the retained source has no locomotion clip. Never labeled native. */
public final class CompatibleLocomotion {
 public static boolean quadruped(String slug){return Set.of("eevee","jolteon","flareon","vaporeon").contains(slug);}
 public static boolean groundGait(String slug){return quadruped(slug)||slug.equals("scyther");}
 public static String clip(String slug,String key){return "animation."+slug+".minemon_"+key;}
 public static void apply(String slug,Map<String,String> clips){
  if(groundGait(slug))clips.putIfAbsent("ground_walk",clip(slug,"walk"));
  if(quadruped(slug))clips.putIfAbsent("ground_run",clip(slug,"run"));
  if(slug.equals("vaporeon")){
   clips.putIfAbsent("water_swim",clip(slug,"swim"));
   clips.putIfAbsent("water_idle",clips.get("ground_idle"));
  }
  if(slug.equals("spearow")){
   clips.putIfAbsent("air_fly",clip(slug,"fly"));clips.putIfAbsent("air_idle",clip(slug,"fly"));
  }
 }
 public static List<String> gaitJoints(String slug){
  if(slug.equals("scyther"))return List.of("left_upper_leg","right_upper_leg");
  String front=slug.equals("vaporeon")?"frontleg_":"front_leg_";
  String back=slug.equals("jolteon")?"backleg_":"back_leg_";
  return List.of(front+"left",front+"right",back+"left",back+"right");
 }
 private CompatibleLocomotion(){}
}
