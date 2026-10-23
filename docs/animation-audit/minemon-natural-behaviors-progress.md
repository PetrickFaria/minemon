# Minemon natural behavior changes — progress

## Base and scope

- Base branch: `fix/minemon-tailflames-gastly-smoke`
- Base commit: `8597b071eb730bfcfbb72d4f8be47c662f8510ff`
- Working branch: `feature/minemon-natural-behaviors`
- Scope: Electabuzz scale, explicit airborne behavior, occasional native terrestrial runs, and water seeking/survival for explicitly aquatic species.
- No Pokémon assets, models, textures, or prior visual fixes were changed.

## Current implementation checkpoint

- Electabuzz render scale changed from 1.8 to 1.35; collision/selection dimensions changed from 1.4 × 3.0 to 1.05 × 2.25; shadow scale changed from 2.0 to 1.5.
- Explicit airborne profiles cover the existing flying registrations plus Charizard, Fearow, Gastly, Aerodactyl, Dragonite, and Mew. The profile keeps Gyarados aquatic despite its source air clips.
- Airborne entities use flying navigation and native air clips when present, receive a soft altitude correction toward 7 blocks below the 3–15 block surface clearance band, and enable gravity while sleeping so they land. Existing combat and owner-follow goal priorities remain ahead of patrol.
- Ground running is considered only for species with a selected `ground_run` clip, while grounded and navigating without sleep, sit, combat, or water activity. A synced state selects the native run clip and applies a temporary 55% movement-speed multiplier for 40–90 ticks.
- The explicitly aquatic profile currently contains Tentacool, Tentacruel, Dewgong, Shellder, Cloyster, Horsea, Seadra, Goldeen, Seaking, Staryu, Starmie, Magikarp, Gyarados, Lapras, Omanyte, and Omastar. It seeks reachable water, wanders between nearby water nodes, selects available native water clips, and breathes underwater. No dry-land damage was added.
- Client smoke phases now render aquatic specimens in water and expect the actual native air/water locomotion clips. GeckoLib Molang preflight now includes selected air and water clips.
- GameTests added for air altitude/sleep landing, water seeking/dry survival, and a native Geodude run during navigation. JUnit coverage checks the scale and explicit species profiles.

## Validation state

- Passed: `tools/verify_project.py`.
- Passed: structural validators for Batches 8–16 and Pidge wing visibility after updating the narrow Electabuzz scale expectation and Charizard native air clip selection.
- Passed: current Molang static preflight; selected native air/water clips are also added to the Java GeckoLib 3.1.40 parser test.
- Local Gradle tests could not start because this workspace does not contain the Gradle 7.5.1 distribution and the wrapper download is unavailable. Java 17 is installed.
- Not yet run: Java compilation/tests, GameTests, client smoke, JAR inspection, GitHub Actions, and artifact publication.

## Next actions

1. Publish this checkpoint and code to GitHub.
2. Inspect the single resulting Actions run; resolve any compile or focused test errors before interpreting server/client results.
3. Confirm GameTests and the full client smoke, inspect the JAR contents, and verify that its artifact belongs to the approved commit.
4. Update this file with exact commit, run, artifact, and expiry details.

## Preserved visual fixes

Charizard arms, Pidgeotto/Pidgeot wings, Graveler/Golem movement, Moltres aerial animation, Mewtwo, Gastly smoke, tail flames, and Mew resources remain unchanged.
