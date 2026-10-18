# Minemon 1.0.0 — project structure milestone

Independent Forge mod project for Minecraft Java 1.19.2, Forge 43.5.0, Java 17, mod ID `minemon`.

## Current milestone

This project now has the Forge/Gradle structure, config, creative tab, a shared wolf-like tamable entity, spawn egg, GeckoLib renderer/model, and a Bulbasaur reference registration. It also contains a generated species catalog for 1,025 species records and extracted resource assets under the `minemon` namespace. The asset extraction process does not copy Cobblemon classes or make Cobblemon a runtime dependency.

The registry currently contains **one entity and one spawn egg: Bulbasaur**. The other asset files and catalog records are staged for later registration and validation; they are not yet playable Minemon. The catalog has 633 source records marked implemented, while 627 have model, texture, and animation files associated. Presence in the catalog is not proof of working rendering.

## Behavior implemented in shared entity

- Wild creatures do not acquire targets unless hurt.
- Bone interaction attempts taming using configurable `tameChancePercent` (default 33%) and consumes one bone outside Creative mode.
- Successful taming stores the vanilla owner UUID, orders the creature to sit, and displays hearts; failure displays smoke.
- Owner interaction toggles sitting. Owner protection and owner attack assistance use vanilla tame goals.
- Tamed creatures follow their owner; vanilla `FollowOwnerGoal` provides its standard safe-position teleport behavior when far away.
- Same-owner Minemon creatures, and untamed Minemon creatures, will not attack one another through the retaliation/assist logic.
- Owner, sitting state, and health persistence use `TamableAnimal`'s built-in entity save data.

## Current animation limitations

Bulbasaur has the source clips `ground_idle`, `ground_walk`, `sleep`, `faint`, `blink`, `cry`, and `render`. The current renderer selects idle/walk/faint. Its source animation file has no dedicated attack or sit clip, so attack movement currently reuses the walk animation and sitting uses idle. No in-game visual validation has been performed yet.

## Build

Requirements: Java 17, Gradle 7.5.1, network access to the Forge and GeckoLib Maven repositories. ForgeGradle is configured in the 5.1 line (`[5.1,6.0)`). GeckoLib 3.1.40 for Forge 1.19.2 is the only additional runtime mod dependency. Cobblemon is not required at runtime.

From this directory, run:

```bash
./gradlew build
```

Expected output name: `build/libs/Minemon-1.19.2-1.0.0.jar`. The current source checkout does not include the Gradle Wrapper files and this environment has no Gradle installation, so a local compilation cannot currently run. The GitHub Actions workflow bootstraps the standard Gradle 7.5.1 Wrapper on its runner, runs the resource verifier and `clean build`, then uploads the JAR as an artifact. A run requires this project to be pushed to a repository with Actions enabled.

To regenerate the local resource bundle from the original JAR:

```bash
python tools_extract_assets.py /path/to/Cobblemon-forge-1.5.2+1.20.1.jar
python tools/verify_project.py
```

The extraction command is only a development tool. It copies data assets and their available model notices, not Cobblemon bytecode. Read [ASSET_LICENSES.md](ASSET_LICENSES.md) before sharing any build containing those assets.

## Next implementation work

1. Compile and launch the Bulbasaur fixture in a Forge 1.19.2 client.
2. Verify geometry, texture, animation clip names and scale in-game; adjust renderer and clip selection.
3. Validate taming, sitting, following, owner defense, persistence, and teleport positions in a test world.
4. Expand entity and spawn egg registration to the remaining supported species, then add species-specific attributes and natural spawn data.
5. Add species-aware animation mapping and sound integration. Keep species without valid model/animation data out of the completed list.
