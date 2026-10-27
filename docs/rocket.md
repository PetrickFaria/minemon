# Rocket workstation and profession

`minemon_gen1:rocket_block` is a solid, directional 1×1 block. The front has a red pixel `R`; the other faces use the same dark metal, grille, and warning-light palette. It drops its own block item and appears in the Minemon creative tab.

The block's point of interest is registered as `minemon_gen1:rocket` with one ticket, and is included in Minecraft's `acquirable_job_site` point-of-interest tag. Forge maps every facing state of the block to that POI. An adult unemployed villager can therefore claim it through the normal village brain and schedule. Existing professions, nitwits, and babies remain subject to the vanilla job-acquisition rules. No radius conversion or profession-changing event is added.

The profession is a genuine `VillagerProfession`. The vanilla villager model and renderer remain in use, with one shared unisex profession overlay: dark uniform, red chest `R`, black cap, gray gloves and boots, and a dark belt with a metal buckle. The overlay works over all seven 1.19.2 villager biome types. Zombie villagers also have the matching profession texture.

There are no Rocket trade listings or custom interaction screens. The profession returns no offers. Vanilla releases the point-of-interest claim when the workstation is removed, and the villager's job-site memory is allowed to clear through its regular brain tasks. The profession itself follows vanilla rules about whether it can be lost; this mod does not force demotion.
