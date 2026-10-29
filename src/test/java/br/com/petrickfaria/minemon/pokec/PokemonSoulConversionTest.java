package br.com.petrickfaria.minemon.pokec;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class PokemonSoulConversionTest {
    @Test public void stagesUseTheFullEvolutionLineAndUnrelatedSpeciesStartAtInitial() {
        assertEquals(PokemonSoulConversion.Stage.INITIAL,PokemonSoulConversion.stage("charmander"));
        assertEquals(PokemonSoulConversion.Stage.INTERMEDIATE,PokemonSoulConversion.stage("charmeleon"));
        assertEquals(PokemonSoulConversion.Stage.FINAL,PokemonSoulConversion.stage("charizard"));
        assertEquals(PokemonSoulConversion.Stage.INITIAL,PokemonSoulConversion.stage("rattata"));
        assertEquals(PokemonSoulConversion.Stage.FINAL,PokemonSoulConversion.stage("raticate"));
        assertEquals(PokemonSoulConversion.Stage.INITIAL,PokemonSoulConversion.stage("snorlax"));
    }

    @Test public void formulaHasRequiredMinimumAndMaximumAndReadsActualLevel() {
        var minimum=PokemonSoulConversion.preview("charmander",1,"common",50,100);
        assertEquals(3,minimum.base()); assertEquals(2,minimum.minimum()); assertEquals(3,minimum.maximum());
        assertEquals(1,minimum.stageBase());assertEquals(1,minimum.rarityBonus());assertEquals(1,minimum.levelBonus());
        var maximum=PokemonSoulConversion.preview("charizard",10,"mythic",50,100);
        assertEquals(13,maximum.base()); assertEquals(7,maximum.minimum());assertEquals(13,maximum.maximum());
        assertEquals(5,maximum.levelBonus());assertEquals(SoulType.FIRE,maximum.soulType());
        assertEquals(2,PokemonSoulConversion.preview("charmander",3,"common",50,100).levelBonus());
    }

    @Test public void rarityTiersAreExplicitAndVeryRareRemainsRare() {
        assertEquals(1,PokemonSoulConversion.rarityTier("common"));
        assertEquals(2,PokemonSoulConversion.rarityTier("uncommon"));
        assertEquals(3,PokemonSoulConversion.rarityTier("rare"));
        assertEquals(3,PokemonSoulConversion.rarityTier("very_rare"));
        assertEquals(4,PokemonSoulConversion.rarityTier("legendary"));
        assertEquals(5,PokemonSoulConversion.rarityTier("mythic"));
    }

    @Test public void defeatAndNatureReturnIntervalsUseCeilingAndUniformIntegerRolls() {
        var defeated=PokemonSoulConversion.preview("charmander",1,"common",50,100);
        assertEquals(2,defeated.minimum());assertEquals(3,defeated.maximum());
        var returned=PokemonSoulConversion.preview("charmander",1,"common",75,100);
        assertEquals(3,returned.minimum());assertEquals(3,returned.maximum());
        assertEquals(3,PokemonSoulConversion.roll(returned,()->0.999));
        var interval=PokemonSoulConversion.preview("charizard",10,"mythic",75,100);
        assertEquals(10,interval.minimum());assertEquals(13,interval.maximum());
        assertEquals(10,PokemonSoulConversion.roll(interval,()->0.0));
        assertEquals(13,PokemonSoulConversion.roll(interval,()->0.999999));
        AtomicInteger calls=new AtomicInteger();
        assertEquals(11,PokemonSoulConversion.roll(interval,()->{calls.incrementAndGet();return .25;}));
        assertEquals(1,calls.get());
    }

    @Test public void dropsAndConversionsDoNotUseFractionsOrEscapeTheirRanges() {
        assertEquals(2,PokemonSoulConversion.rollBetween(2,4,()->0.0));
        assertEquals(4,PokemonSoulConversion.rollBetween(2,4,()->0.999999));
        assertEquals(2,PokemonSoulConversion.rollBetween(2,4,()->Double.NaN));
        assertEquals(0,PokemonSoulConversion.rollBetween(4,2,()->0.0));
        assertEquals(0,PokemonSoulConversion.rollDeathDrops("not-a-pokemon",1,()->0.5));
    }
}
