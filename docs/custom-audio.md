# Minemon custom Pokémon audio

When Minemon starts, it creates one folder for each registered first-generation Pokémon under `.minecraft/config/minemon/sounds/`. The three-digit prefix and display name come from the same National Pokédex manifest that registers the entities.

Place one or more `.ogg` or `.mp3` files directly in that Pokémon's folder. The first `.ogg` file alphabetically has priority; if none exists, the first `.mp3` file alphabetically is used. Only one file plays per cue. Restarting Minecraft is sufficient after adding, replacing, or removing files.

Example: `.minecraft/config/minemon/sounds/025_Pikachu/som.ogg`

Each entity has its own five-minute server timer. A left-click attack plays a cue and resets that entity's timer while leaving the ordinary attack event uncancelled. Clients resolve and play their own local file, so audio is never transferred through multiplayer. Playback uses the neutral creature volume category and is positional.

Files are read only from that species folder. Symbolic links are ignored. Files above 8 MiB, decoded audio longer than 30 seconds, unsupported channel counts/rates, and invalid files are rejected with a log warning. OGG is preferred over MP3. No Pokémon audio recordings are copied into the distributable JAR. The MP3 decoder is JLayer 1.0.1, distributed under LGPL; its license text is included in `META-INF/LICENSE-JLAYER-LGPL-2.1.txt`.
