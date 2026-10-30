package br.com.petrickfaria.minemon.entity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Explicit, mutually exclusive routine assignments for the National Pokédex 001–151. */
public final class SpeciesBehaviorProfiles {
    public enum Profile {
        GROUND,
        WATER_VISITOR,
        GROUND_FLIGHT,
        FLYER,
        FLOAT_HALF,
        FLOAT_LOW,
        FLOAT_CYCLE,
        GROUND_FLOAT_CYCLE,
        AQUATIC_CYCLE,
        AQUATIC_BOTTOM,
        AQUATIC_NEAR_SURFACE,
        AQUATIC_SURFACE
    }

    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();

    private static final Set<String> BOTTOM_PREFERENCE = Set.of(
            "shellder", "cloyster", "krabby", "kingler", "staryu", "starmie",
            "omanyte", "omastar", "kabuto", "kabutops");
    private static final Set<String> BOTTOM_NIGHT_REST = Set.of(
            "squirtle", "wartortle", "blastoise", "psyduck", "golduck", "poliwag", "poliwhirl", "poliwrath",
            "slowpoke", "slowbro", "seel", "dewgong", "krabby", "kingler", "staryu", "starmie", "vaporeon",
            "omanyte", "omastar", "kabuto", "kabutops", "dratini", "dragonair", "shellder", "cloyster", "gyarados");
    private static final Set<String> LAND_FOR_NIGHT_REST = Set.of(
            "charizard", "scyther", "dragonite", "butterfree", "beedrill", "pidgey", "pidgeotto", "pidgeot",
            "spearow", "fearow", "zubat", "golbat", "venomoth", "aerodactyl", "articuno", "zapdos", "moltres",
            "metapod", "kakuna", "geodude", "kadabra", "alakazam", "gengar");
    private static final Set<String> NOCTURNAL = Set.of("gastly", "haunter", "gengar", "zubat", "golbat");
    private static final Set<String> TWO_VISIT_SURFACE = Set.of("staryu", "starmie");

    static {
        assign(Profile.GROUND, "bulbasaur,ivysaur,venusaur,charmander,charmeleon,caterpie,weedle,rattata,raticate,ekans,arbok,pikachu,raichu,sandshrew,sandslash,nidoranf,nidorina,nidoqueen,nidoranm,nidorino,nidoking,clefairy,clefable,vulpix,ninetales,jigglypuff,wigglytuff,oddish,gloom,vileplume,paras,parasect,venonat,diglett,dugtrio,meowth,persian,mankey,primeape,growlithe,arcanine,machop,machoke,machamp,bellsprout,weepinbell,victreebel,graveler,golem,ponyta,rapidash,farfetchd,doduo,dodrio,grimer,muk,onix,drowzee,hypno,voltorb,electrode,exeggcute,exeggutor,cubone,marowak,hitmonlee,hitmonchan,lickitung,rhyhorn,rhydon,chansey,tangela,kangaskhan,mrmime,jynx,electabuzz,magmar,pinsir,tauros,ditto,eevee,jolteon,flareon,snorlax");
        assign(Profile.WATER_VISITOR, "squirtle,wartortle,blastoise,psyduck,golduck,poliwag,poliwhirl,poliwrath,slowpoke,slowbro,seel,dewgong,krabby,kingler,staryu,starmie,vaporeon,omanyte,omastar,kabuto,kabutops,dratini,dragonair");
        assign(Profile.GROUND_FLIGHT, "charizard,scyther,dragonite");
        assign(Profile.FLYER, "butterfree,beedrill,pidgey,pidgeotto,pidgeot,spearow,fearow,zubat,golbat,venomoth,aerodactyl,articuno,zapdos,moltres");
        assign(Profile.FLOAT_HALF, "metapod,kakuna,abra");
        assign(Profile.FLOAT_LOW, "geodude,porygon");
        assign(Profile.FLOAT_CYCLE, "magnemite,magneton,gastly,haunter,koffing,weezing,mewtwo,mew");
        assign(Profile.GROUND_FLOAT_CYCLE, "kadabra,alakazam,gengar");
        assign(Profile.AQUATIC_CYCLE, "tentacool,tentacruel,horsea,seadra,goldeen,seaking,magikarp");
        assign(Profile.AQUATIC_BOTTOM, "shellder,cloyster");
        assign(Profile.AQUATIC_NEAR_SURFACE, "gyarados");
        assign(Profile.AQUATIC_SURFACE, "lapras");

        if (PROFILES.size() != 151) throw new ExceptionInInitializerError(
                "Behavior profiles must assign each of the 151 species exactly once; assigned=" + PROFILES.size());
    }

    private static void assign(Profile profile, String names) {
        for (String slug : names.split(",")) {
            if (PROFILES.put(slug, profile) != null) {
                throw new ExceptionInInitializerError("Conflicting behavior profile for " + slug);
            }
        }
    }

    public static Profile profile(String slug) {
        Profile profile = PROFILES.get(slug);
        if (profile == null) throw new IllegalArgumentException("Unassigned species " + slug);
        return profile;
    }

    public static Map<String, Profile> all() { return Collections.unmodifiableMap(PROFILES); }
    public static boolean isNocturnal(String slug) { return NOCTURNAL.contains(slug); }
    public static boolean getsTwoSurfaceVisits(String slug) { return TWO_VISIT_SURFACE.contains(slug); }
    public static boolean isAirborne(String slug) {
        return switch (profile(slug)) {
            case FLYER, FLOAT_HALF, FLOAT_LOW, FLOAT_CYCLE -> true;
            default -> false;
        };
    }
    public static boolean canFly(String slug) {
        return isAirborne(slug) || profile(slug) == Profile.GROUND_FLIGHT || profile(slug) == Profile.GROUND_FLOAT_CYCLE;
    }
    public static boolean isFloating(String slug) {
        return switch (profile(slug)) {
            case FLOAT_HALF, FLOAT_LOW, FLOAT_CYCLE, GROUND_FLOAT_CYCLE -> true;
            default -> false;
        };
    }
    public static boolean isVisitor(String slug) { return profile(slug) == Profile.WATER_VISITOR; }
    public static boolean isAquatic(String slug) {
        return switch (profile(slug)) {
            case AQUATIC_CYCLE, AQUATIC_BOTTOM, AQUATIC_NEAR_SURFACE, AQUATIC_SURFACE -> true;
            default -> false;
        };
    }
    public static boolean waterCapable(String slug) { return isAquatic(slug) || isVisitor(slug); }
    public static boolean prefersBottom(String slug) { return BOTTOM_PREFERENCE.contains(slug); }
    public static boolean restsAtWaterBottom(String slug) { return BOTTOM_NIGHT_REST.contains(slug); }
    public static boolean landsForNightRest(String slug) { return LAND_FOR_NIGHT_REST.contains(slug); }
    public static boolean hoversDuringNightRest(String slug) {
        if (landsForNightRest(slug)) return false;
        return profile(slug) == Profile.FLOAT_HALF || profile(slug) == Profile.FLOAT_LOW || profile(slug) == Profile.FLOAT_CYCLE;
    }
    public static double nightRestHoverHeight(String slug) {
        if (slug.equals("kakuna") || slug.equals("metapod")) return 1.0D / 3.0D;
        return profile(slug) == Profile.FLOAT_HALF || profile(slug) == Profile.FLOAT_LOW ? 0.5D : 1.0D;
    }

    /** The Minecraft world day is 24,000 ticks; no host clock or daylight-cycle setting is consulted. */
    public static boolean shouldSleep(String slug, long worldDayTime) {
        int time = (int) Math.floorMod(worldDayTime, 24_000L);
        return isNocturnal(slug) ? time >= 6_000 && time < 13_000 : time >= 13_000 && time < 23_000;
    }

    public static Set<String> airborneSpecies() {
        return PROFILES.keySet().stream().filter(SpeciesBehaviorProfiles::isAirborne).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public static Set<String> aquaticSpecies() {
        return PROFILES.keySet().stream().filter(SpeciesBehaviorProfiles::isAquatic).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private SpeciesBehaviorProfiles() {}
}
