package br.com.petrickfaria.minemon.entity;

import java.util.*;

/** Additions only where the retained source has no locomotion clip. Never labeled native. */
public final class CompatibleLocomotion {
 public static boolean quadruped(String slug){return Set.of("eevee","jolteon","flareon","vaporeon").contains(slug);}
 public static boolean groundGait(String slug){return quadruped(slug)||Set.of("scyther","meowth","persian").contains(slug);}
 public static String clip(String slug,String key){return "animation."+slug+".minemon_"+key;}
 public static void apply(String slug,Map<String,String> clips){
  if(groundGait(slug))clips.putIfAbsent("ground_walk",clip(slug,"walk"));
  if(quadruped(slug)||slug.equals("meowth")||slug.equals("persian"))clips.putIfAbsent("ground_run",clip(slug,"run"));
  if(slug.equals("vaporeon")){
   clips.putIfAbsent("water_swim",clip(slug,"swim"));
   clips.putIfAbsent("water_idle",clips.get("ground_idle"));
  }
 }
 public static List<String> gaitJoints(String slug){
  if(slug.equals("scyther"))return List.of("left_upper_leg","right_upper_leg");
  if(slug.equals("meowth"))return List.of("leg_left","leg_right");
  if(slug.equals("persian"))return List.of("leg_front_left1","leg_front_right1","leg_back_left1","leg_back_right1");
  String front=slug.equals("vaporeon")?"frontleg_":"front_leg_";
  String back=slug.equals("jolteon")?"backleg_":"back_leg_";
  return List.of(front+"left",front+"right",back+"left",back+"right");
 }
 private CompatibleLocomotion(){}
}
