package br.com.petrickfaria.minemon.gametest;

import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
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

    @Test public void selectedIdleWalkAndRunClipsParseInGeckoLib31() throws Exception {
        int parsedExpressions=0;
        for(var definition:SpeciesDefinitions.ALL.values()) {
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
            String run=definition.clips().get("ground_run");
            if(run!=null)selected.add(run);
            for(String clipName:selected) {
                assertTrue(definition.slug()+" missing selected clip "+clipName,clips.has(clipName));
                JsonObject clip=clips.getAsJsonObject(clipName);
                if(clip.has("bones"))parsedExpressions+=parse(clip.get("bones"),new MolangParser(),definition.slug()+"/"+clipName);
            }
        }
        assertTrue("At least one active expression should be validated",parsedExpressions>0);
    }

    private static int parse(JsonElement value,MolangParser parser,String source) throws Exception {
        int count=0;
        if(value.isJsonArray()) {
            for(JsonElement child:value.getAsJsonArray())count+=parse(child,parser,source);
        } else if(value.isJsonObject()) {
            for(var entry:value.getAsJsonObject().entrySet())count+=parse(entry.getValue(),parser,source);
        } else if(value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
            String expression=value.getAsString();
            assertFalse(source+" has unsupported GeckoLib 3.1.40 query.r function: "+expression,COBALT_QUERY.matcher(expression).find());
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
