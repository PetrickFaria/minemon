# Individual behavior profiles — Generation 1

Environment: Minecraft 1.19.2, Forge 43.5.0, Java 17, GeckoLib 3.1.40.

The authoritative assignment is `SpeciesBehaviorProfiles`: 151 explicit species identifiers, one principal profile per species. Neither elemental types nor the old source asset `mode` field determines behavior.

| Profile | Species | Execution |
| --- | ---: | --- |
| A: ground | 91 | Ground navigation and original available ground locomotion; occasional runs when a run clip exists. |
| B: altitude cycle | 23 | 3–5 blocks for 6,000 ticks; 6–8 for 1,800; 9–10 for 1,200. |
| C: ground/flight | 2 | Charizard and Scyther: 6,000 active ground ticks, physical takeoff, 1,200 flight ticks, safe physical landing. |
| D: water visitor | 24 | Reachable water within 25 blocks; 600 submerged ticks, 600 surface ticks, real shore exit, 6,000 eligible dry-land ticks before returning. |
| E: submerged | 9 | Predominantly submerged swimming, varied reachable destinations, collision look-ahead and blocked-target cooldowns. |
| F: surface | 2 | Gyarados and Lapras use native surface idle/swim clips and dimension-aware buoyancy. |

## Clock semantics and persistence

`BehaviorCycle` has no Minecraft dependencies. Durations are measured in server ticks at 20 TPS. The altitude clock advances only inside the current altitude band; the land clock requires actual ground support, including normal idle between walking destinations. Priority interruptions are excluded; it does not require five cumulative minutes of individual footsteps. Water timers require the corresponding submerged/surface state. Safe takeoff, ascent, descent and shore transitions take the time required by real navigation in addition to the specified residence times. No transition teleports an entity.

Sleep, combat, sitting, passengers, death, disabled AI and active owner following pause natural execution. There is a per-entity initial delay of 0–29 ticks, and navigation destinations and idle intervals are individualized. Phase, elapsed ticks, altitude stage and remaining initial delay are saved to NBT. Unloaded/offline time is not added.

Only one navigation/controller is active at a time. Mixed fliers switch between their ground and flying controllers on actual state transitions. Sleep or an expired flight period requests a reachable dry landing; waking resumes the saved cycle.

## Water navigation

The same MOVE goal owns seeking, swimming and exiting. It scans the full three-dimensional 25-block radius in bounded batches (at most 2,048 block candidates and two path checks in a search batch), with persistent horizontal/vertical cursors and failed-destination expiry. It does not force-load chunks. Swimming checks the complete entity footprint, body clearance, depth and the next half-block toward the current path waypoint, rather than incorrectly vetoing an obstacle-avoiding path based on a ray to its final destination. Neutral buoyancy is active only inside water so the swimming controller can actually ascend; land gravity is preserved. A stalled destination is abandoned instead of pushed indefinitely.

Shallow water is not treated as a valid dive volume for a visiting species. If a visitor is externally placed in insufficient water, it seeks an exit. The five-minute return interval starts only after actual supported dry land is reached. Cooldown wandering rejects routes that cross water. Permanent swimmers and visitors retain the existing non-fish entity behavior: dry land does not cause fish-style suffocation damage.

## Assets and explicit runtime adaptations

Original model, texture and animation source files are retained. The historical asset manifest continues to record the approved source baseline; `SpeciesDefinitions` applies the following documented runtime adaptations:

* Ground profiles prefer existing ground clips. In particular Farfetch'd uses its original `ground_idle` and `ground_walk`, rather than the old manifest's aerial defaults.
* Seaking alone: source scale 1.8 × 0.75 = **1.35**; width 0.7 × 0.75 = **0.525**; height 0.6 × 0.75 = **0.45**. The operation is applied once while loading definitions, not repeatedly to saved entities. Goldeen is unchanged.
* Eevee, Jolteon and Flareon receive additional, skeleton-specific walk/run clips because the retained source supplies static poses, not these gaits.
* Vaporeon receives additional walk/run/swim clips for the same reason. Its swimming clip animates the existing tail chain and limb bones.
* Scyther retains its native airborne wing animation and receives a compatible ground gait based on its original ground stance.
* Spearow receives a compatible flight clip using its existing open-wing skeleton; an original compatible aerial clip is absent from the retained source.

These additions use the `.minemon_` namespace and are not described as native animations. Native clips win whenever available. Generated additions are merged only into processed runtime resources, without changing source animation files or model proportions.

Several visiting-water species have no native swim clip in the retained source (Poliwag, Seel, Krabby, Kingler, Kabuto and Kabutops). Their real water navigation is implemented; animation selection uses the existing source locomotion/pose fallback when a swim clip is absent. It does not claim an invented native swim animation.

Koffing and Weezing emit vanilla translucent smoke at approximately four particles per second per entity while alive, including during movement. Gastly's separate approved smoke remains unchanged.

## Validation coverage

`BehaviorCycleTest` checks exact profile membership, tick boundaries, inactive pauses, physical-transition gates, persistence, Seaking's runtime scale, and ground-clip selection. `IndividualBehaviorTests` uses real server movement for altitude stages, mixed takeoff/landing, water visits and cooldown, underwater/surface positioning, and NBT state.

The complete client smoke still covers all 151 species and preserved effects. Additional client scenarios render Charizard/Scyther in flight and Vaporeon swimming. Particle assertions count real client emissions, and gait assertions sample settled GeckoLib bone transforms rather than clip names alone.
