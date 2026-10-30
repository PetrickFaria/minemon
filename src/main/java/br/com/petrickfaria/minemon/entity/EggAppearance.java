package br.com.petrickfaria.minemon.entity;

import java.util.List;
import java.util.Map;

/** A persisted elemental accent; the shell itself is never tinted. */
public final class EggAppearance {
    private static final Map<String,Integer> COLORS=Map.ofEntries(
        Map.entry("normal",0xB8BFC7),Map.entry("fire",0xEE5925),Map.entry("water",0x2E6FDC),
        Map.entry("electric",0xF4C822),Map.entry("grass",0x43AB42),Map.entry("ice",0x72D7DE),
        Map.entry("fighting",0xA94030),Map.entry("poison",0xA840B9),Map.entry("ground",0xA76B2F),
        Map.entry("flying",0x8EB5F1),Map.entry("psychic",0xEB4089),Map.entry("bug",0x879C24),
        Map.entry("rock",0x8B7F5E),Map.entry("ghost",0x59408B),Map.entry("dragon",0x5E4DDB),
        Map.entry("dark",0x363A44),Map.entry("steel",0x899FA9),Map.entry("fairy",0xF2A2C7));
    public static int color(String type){return COLORS.getOrDefault(type,COLORS.get("normal"));}
    public static String choose(List<String> types,boolean second){return types.isEmpty()?"normal":types.get(second&&types.size()>1?1:0);}
    private EggAppearance(){}
}
