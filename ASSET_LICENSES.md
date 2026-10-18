# Third-party asset notices

The bundled Bulbasaur and other extracted Cobblemon artwork and data assets come from the user-provided Cobblemon 1.5.2 JAR. The upstream Cobblemon Assets project states that its assets are licensed under **Creative Commons Attribution-NonCommercial 3.0 Unported (CC BY-NC 3.0)**: https://gitlab.com/cable-mc/cobblemon-assets/-/blob/master/LICENSE

The original per-model `license` notices have been retained under `src/main/resources/assets/minemon/licenses/pokemon/` where they existed. Those notices do not name an individual creator. Credit: **Cobblemon Assets contributors**. These files are included for the user's private, non-commercial development build. Do not publish or sell a build containing them without checking the applicable permissions and attribution for every asset. The CC license does not grant Pokémon trademark rights.

The Cobblemon root software license (MPL 2.0) is not applied to the extracted art assets. No Cobblemon classes or source code were copied. The license for Minemon's new source code has not been set in this milestone.

## Runtime adaptations in this update

The selected per-model notices were compared and contain the same license text (SHA-256 `fb8e971d1895863ec9fc5f3cfc526c64af980bd6c93d0a1615c7969df46a6660`). Canonical license: https://creativecommons.org/licenses/by-nc/3.0/. Artwork title/source: Cobblemon Assets, from the user-provided Cobblemon Forge 1.5.2 JAR; attribution remains Cobblemon Assets contributors.

Minemon adapts copied animation JSON at build time: `q.` Molang aliases become `query.`, expression-only clips receive explicit durations, and Jolteon's optional cry clip with absent bone references is omitted from the runtime copy. Original animation files remain in source. Geometry and texture artwork are not replaced. These are compatibility adaptations by the Minemon project, without endorsement by the original contributors. All selected model notices and this attribution file accompany the distributable JAR.
