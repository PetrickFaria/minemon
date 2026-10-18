# Minemon

Minecraft Java 1.19.2 · Forge 43.5.0 · Java 17 · GeckoLib 3.1.40 · mod ID `minemon`.

The source preserves the existing 1,025-entry catalog and extracted artwork. Only species explicitly listed in `data/minemon/runtime_species.json` and `gradle.properties` are registered or packaged. Catalog entries are not playable entity registrations. No Cobblemon classes or runtime dependency are included.

## Build and validation

Use the checked-in Gradle 7.5.1 Wrapper and ForgeGradle 5.1.77:

```bash
python3 tools/verify_project.py
./gradlew clean test build
./gradlew runGameTestServer
python3 tools/verify_runtime.py build/libs/Minemon-1.19.2-1.3.0.jar
```

The workflow runs JUnit against GeckoLib's actual Molang parser, Minecraft server GameTests, and an Xvfb Minecraft client. The client test checks the title screen, GeckoLib's loaded models/animations, and actual rendered locomotion for each registered species. Diagnostic artifacts contain test results, client/server logs and screenshots. Validation code and test structures are excluded from the distributable JAR.

## Shared companion behavior

- Bone interaction uses the existing configurable taming probability, consumes one bone outside Creative, assigns the vanilla owner UUID and orders sitting.
- Owner interaction toggles sitting. Vanilla owner defense, owner attack assistance and follow/teleport goals remain in place.
- Wild creatures have no spontaneous target-selection goal. Retaliation expires after 20 seconds, excessive chase distance or prolonged unreachable navigation.
- Owner UUID checks prevent friendly retaliation. Sitting blocks following and distant attack movement. Vanilla NBT stores taming, owner and sitting state; the optional sleep pose state is synchronized and persisted as well.
- Survival or Adventure mode is required to validate player retaliation. Creative and Spectator players remain protected by Minecraft's normal targeting rules.

## Animation compatibility

Original animation files remain untouched in source. During resource processing, runtime copies normalize the Bedrock `q.anim_time` alias to GeckoLib 3's `query.anim_time`. GeckoLib's native continuous timing is retained for expression-only loops. Model, texture and animation paths are chosen explicitly per species. A secondary blink controller is kept separate from body locomotion and stops during movement/attacks. Physical attack clips are triggered after a successful melee hit. When an approved source clip is absent, runtime processing adds a conservative species-specific run variation from that model's walk clip, plus sit, sleep, or short physical pose clips using only bones present in that species' geometry.

The current runtime roster is listed in `data/minemon/runtime_species.json`. Its six common state mappings are idle, walk, run, sit, sleep and attack. Original clips are preferred; generated runtime copies are reported as compatibility adaptations and leave the source artwork unchanged.

`tools/audit_candidates.py` records model/animation bone matching, event structure, UV dimensions and actual animated locomotion. Optional Jolteon cry has invalid bone references and is omitted only from the prepared runtime copy; the original source is preserved. Species with unsupported events, missing bones, only static poses or unvalidated locomotion remain unregistered.

## Assets and future species

Read [ASSET_LICENSES.md](ASSET_LICENSES.md). Artwork credit: Cobblemon Assets contributors, CC BY-NC 3.0; noncommercial use only, with retained notices and attribution. Pokémon trademarks are separate.

To enable an audited batch, run `python3 tools/enable_batch.py` with the complete intended species list, then validate, compile, run the server/client tests and inspect the JAR before enabling another batch. This generates explicit runtime definitions, egg models and translations without extracting or rewriting the original asset library. Original extraction tools remain available for provenance; this correction does not rerun extraction.
