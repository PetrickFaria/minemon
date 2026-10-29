package br.com.petrickfaria.minemon.pokec;

import java.util.function.DoubleSupplier;

/** Shared PokéC soul formula and uniform yield rolls for deaths and nature returns. */
public final class PokemonSoulConversion {
    public enum Stage { INITIAL, INTERMEDIATE, FINAL }
    public record Preview(String species,int level,String rarity,Stage stage,int stageBase,int rarityBonus,
                          int levelBonus,SoulType soulType,int base,int minimum,int maximum) { }

    /** Last available form is Final; a species without an evolution line is Initial. */
    public static Stage stage(String slug) {
        PokecSpecies.Entry entry=PokecSpecies.get(slug); if(entry==null)return Stage.INITIAL;
        boolean hasEvolution=PokecSpecies.ALL.values().stream().anyMatch(candidate->slug.equals(candidate.evolvesFrom()));
        if(entry.evolvesFrom()==null||entry.evolvesFrom().isBlank())return Stage.INITIAL;
        return hasEvolution?Stage.INTERMEDIATE:Stage.FINAL;
    }
    public static int rarityTier(String rarity) {
        return switch(PokecEconomy.resolveRarity(rarity,"common")){
            case "common"->1;case "uncommon"->2;case "rare"->3;case "legendary"->4;case "mythic"->5;default->1;};
    }
    public static int levelBonus(int level){return PokecEconomy.get().levelSouls(level);}

    public static Preview preview(String slug,int level,String configuredRarity){return preview(slug,level,configuredRarity,75,100);}
    public static Preview preview(String slug,int level,String configuredRarity,int minimumPercent,int maximumPercent){
        PokecSpecies.Entry species=PokecSpecies.get(slug); if(species==null)return null;
        PokecEconomy economy=PokecEconomy.get(); Stage stage=stage(slug);String rarity=PokecEconomy.resolveRarity(configuredRarity,species.rarity());
        int stageBase=economy.stageSouls(stage),rarityBonus=economy.raritySouls(rarity),levelBonus=economy.levelSouls(level);
        int base=Math.max(0,stageBase+rarityBonus+levelBonus);
        int minPct=clampPercent(minimumPercent),maxPct=clampPercent(maximumPercent);if(minPct>maxPct){minPct=0;maxPct=100;}
        int minimum=ceilPercent(base,minPct),maximum=ceilPercent(base,maxPct);
        // Upper percentage is a ceiling too, but never let a value over 100% exceed the base.
        maximum=Math.min(base,maximum);minimum=Math.min(maximum,minimum);
        return new Preview(slug,Math.max(1,level),rarity,stage,stageBase,rarityBonus,levelBonus,
                SoulType.fromSpeciesType(species.primaryType()),base,minimum,maximum);
    }
    public static Preview preview(String slug,int level){
        PokecSpecies.Entry species=PokecSpecies.get(slug);if(species==null)return null;
        PokecEconomy e=PokecEconomy.get();return preview(slug,level,e.rarity(slug),e.releaseMinPercent(),e.releaseMaxPercent());
    }
    private static int clampPercent(int value){return Math.max(0,Math.min(100,value));}
    private static int ceilPercent(int base,int percent){return (int)(((long)Math.max(0,base)*clampPercent(percent)+99L)/100L);}

    /** Every integer in the inclusive valid range has equal probability. */
    public static int roll(Preview preview,DoubleSupplier serverRandom){return rollBetween(preview==null?0:preview.minimum(),preview==null?0:preview.maximum(),serverRandom);}
    public static int rollBetween(int minimum,int maximum,DoubleSupplier serverRandom){
        if(maximum<minimum||maximum<0)return 0;
        minimum=Math.max(0,minimum);int span=maximum-minimum+1;if(span<=1)return minimum;
        double draw=serverRandom.getAsDouble();if(!Double.isFinite(draw)||draw<0.0D||draw>=1.0D)return minimum;
        return minimum+(int)Math.floor(draw*span);
    }
    public static int deathDropMaximum(String slug,int level){
        PokecSpecies.Entry species=PokecSpecies.get(slug);if(species==null)return 0;PokecEconomy e=PokecEconomy.get();
        return preview(slug,level,e.rarity(slug),e.deathMaxPercent(),e.deathMaxPercent()).maximum();
    }
    public static int deathDropMinimum(String slug,int level){
        PokecSpecies.Entry species=PokecSpecies.get(slug);if(species==null)return 0;PokecEconomy e=PokecEconomy.get();
        return preview(slug,level,e.rarity(slug),e.deathMinPercent(),e.deathMinPercent()).minimum();
    }
    public static int rollDeathDrops(String slug,int level,DoubleSupplier serverRandom){
        PokecSpecies.Entry species=PokecSpecies.get(slug);if(species==null)return 0;PokecEconomy e=PokecEconomy.get();
        return roll(preview(slug,level,e.rarity(slug),e.deathMinPercent(),e.deathMaxPercent()),serverRandom);
    }
    private PokemonSoulConversion(){}
}
