package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
import br.com.petrickfaria.minemon.entity.SpeciesBehaviorProfiles;
import com.google.gson.*;
import org.junit.Test;
import software.bernie.geckolib3.core.molang.MolangParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ActiveAnimationMolangPreflightTest {
    private static final Path RESOURCES=Path.of(System.getProperty("minemon.testResources"));
    private static final Pattern COBBLEMON_QUERY=Pattern.compile("(?i)\\b(?:q|query)\\.r\\.[A-Za-z_][A-Za-z0-9_]*\\s*\\(");
    private static final Pattern BEDROCK_ALIAS=Pattern.compile("(?i)\\bq\\.[A-Za-z_][A-Za-z0-9_]*");

    @Test public void selectedLocomotionAndNativeAirWaterClipsParseInGeckoLib31() throws Exception {
        int parsedExpressions=0;
        // Match GeckoLibCache: initialize and reuse the same parser context for active clips.
        MolangParser parser=new MolangParser();
        parser.setValue("query.anim_time",0.0);
        int checkedSpecies=0;
        for(var definition:SpeciesDefinitions.ALL.values()) {
            if(definition.nationalPokedexNumber()<1||definition.nationalPokedexNumber()>151)continue;
            checkedSpecies++;
            String path=definition.animation().replace("assets/minemon/","assets/minemon_gen1/");
            Path animationPath=RESOURCES.resolve(path);
            if(!Files.isRegularFile(animationPath)) {
                assertEquals("Only static-fallback species may omit animation: "+definition.slug(),"static",definition.movementFallback());
                continue;
            }
            JsonObject clips=JsonParser.parseReader(Files.newBufferedReader(animationPath)).getAsJsonObject().getAsJsonObject("animations");
            Set<String> selected=new LinkedHashSet<>();
            if(definition.idle()!=null)selected.add(definition.idle());
            if(definition.walk()!=null)selected.add(definition.walk());
            String run=SpeciesBehaviorProfiles.isAirborne(definition.slug())?null:definition.clips().get("ground_run");
            if(run!=null)selected.add(run);
            if(SpeciesBehaviorProfiles.isAirborne(definition.slug())) {
                addSelected(selected,definition,"air_idle","air_fly","air_glide");
            }
            if(SpeciesBehaviorProfiles.isAquatic(definition.slug())) {
                addSelected(selected,definition,"water_idle","water_swim","water_surface_idle","water_surface_swim",
                        "surface_water_idle","surface_water_swim","surfacewater_idle","surfacewater_swim");
            }
            for(String clipName:selected) {
                assertTrue(definition.slug()+" missing selected clip "+clipName,clips.has(clipName));
                JsonObject clip=clips.getAsJsonObject(clipName);
                if(clip.has("bones"))parsedExpressions+=parse(clip.get("bones"),parser,definition.slug()+"/"+clipName);
            }
        }
        assertEquals("Every first-generation species must be covered",151,checkedSpecies);
        assertTrue("At least one active expression should be validated",parsedExpressions>0);
    }

    private static void addSelected(Set<String> selected,SpeciesDefinitions.Definition definition,String... keys) {
        for(String key:keys) {
            String clip=definition.clips().get(key);
            if(clip!=null)selected.add(clip);
        }
    }

    private static int parse(JsonElement value,MolangParser parser,String source) throws Exception {
        int count=0;
        if(value.isJsonArray()) {
            for(JsonElement child:value.getAsJsonArray())count+=parse(child,parser,source);
        } else if(value.isJsonObject()) {
            for(var entry:value.getAsJsonObject().entrySet())count+=parse(entry.getValue(),parser,source);
        } else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            String expression=value.getAsString();
            assertFalse(source+" has unsupported GeckoLib 3.1.40 query.r function: "+expression,COBBLEMON_QUERY.matcher(expression).find());
            assertFalse(source+" has unconverted Bedrock q. alias: "+expression,BEDROCK_ALIAS.matcher(expression).find());
            var parsed=parser.parseExpression(expression);
            for(int tick=0;tick<=20;tick++) {
                parser.setValue("query.anim_time",tick/20.0);
                assertTrue(source+" non-finite expression: "+expression,Double.isFinite(parsed.get()));
            }
            return 1;
        }
        return count;
    }
}
