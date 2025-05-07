![Title](https://i.imgur.com/naWBH5p.png)

[![Support me on Patreon](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Fshieldsio-patreon.vercel.app%2Fapi%3Fusername%3Dnonamecrackers2%26type%3Dpatrons&style=flat-square)](https://patreon.com/nonamecrackers2)
[![Discord](https://img.shields.io/discord/987817685293355028?style=flat-square&logo=discord&label=Discord&color=%235865F2)](https://discord.gg/cracker-s-modded-community-987817685293355028)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/simple-clouds?style=flat-square&logo=modrinth&label=Modrinth&color=dark-green)](https://modrinth.com/project/simple-clouds)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1121215?style=flat-square&logo=curseforge&label=CurseForge&color=orange)](https://www.curseforge.com/minecraft/mc-mods/simple-clouds)
![Modrinth Version](https://img.shields.io/modrinth/v/simple-clouds?style=flat-square&label=Version&color=blue)

# About

Simple Clouds is a cloud rendering overhaul mod for Minecraft: Java Edition, adding new cloud types, breathtaking visuals, and localized weather. It attempts to mimic real-life weather and cloud formations in a stylized, ambient, and aesthetic way that is meant to build on to the vanilla Minecraft experience.

**Simple Clouds is currently in open BETA, and you may experience bugs, crashes, and instability.** There are still lots of features I still want to implement.

# Info

Simple Clouds can work on the client-side only (connected to vanilla server or server without Simple Clouds installed), or with server-side support (singleplayer or on a server with Simple Clouds installed). There are limitiations/advantages in either case:

## Client-Side Only Limitations
- Localized weather is disabled and the vanilla, global weather system will be used.
- Cloud positions are not saved and the seed is randomized each time you login to a world. A custom cloud seed can be used that will keep the clouds the same each time you login if wanted.

## Server-Side Capabilities
- Localized weather + effects when under stormy clouds
- Cloud saving/synchronization with a unique seed per world

Simple Clouds also has a built-in cloud editor, which can be accessed in the main config menu. You can customize existing or create your own cloud types, and export them for use in datapacks/resourcepacks. Documentation will be coming soon.

# How Does It Work?
Simple Clouds works by using compute shaders to generate the clouds in semi-realtime. It does this by iterating over a grid of voxels, testing each cube against layers of 3D noise, and adding vertices to create the clouds. This work is done on the GPU which handles parallel tasks such as these more efficiently than the CPU.

See the ``cube_mesh.comp``, ``SimpleCloudsRenderer``, and ``CloudMeshGenerator`` and its subclasses to see how it works.

Despite being super fast, Simple Clouds can still have a noticeable effect on your frames, especially if you have an older GPU. The client config has options for fine-tweaking the mod to see if you can get something that works well for your system. **In general, systems with more modern GPUs should handle Simple Clouds with ease.** I’m constantly looking for new ways to make this mod more performant, so it may get better over time.

# Contributions

If you have something that could help improve Simple Clouds (performance, features, etc.) feel free to make a pull request, or an issue in the issues tab. Please follow the [contributing guidelines](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/docs/CONTRIBUTING.md) when contributing.

# License

Simple Clouds is licensed under [PolyForm Perimeter License 1.0.1](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/LICENSE.md) by nonamecrackers2 unless otherwise stated. The following files contain code that are subject to different licenses:
- [/src/main/resources/assets/simpleclouds/shaders/program/storm_fog.fsh](https://github.com/nonamecrackers2/simple-clouds/blob/658c05e5e97eb21b3106ee9940f19028e98722fa/src/main/resources/assets/simpleclouds/shaders/program/storm_fog.fsh#L64C1-L89C3)
- [/src/main/resources/assets/simpleclouds/shaders/include/random.glsl](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/src/main/resources/assets/simpleclouds/shaders/include/random.glsl)
- [/src/main/resources/assets/simpleclouds/shaders/include/random_hash.glsl](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/src/main/resources/assets/simpleclouds/shaders/include/random_hash.glsl)
- [/src/main/resources/assets/simpleclouds/shaders/include/simplex_noise.glsl](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/src/main/resources/assets/simpleclouds/shaders/include/simplex_noise.glsl)
- [/src/main/resources/assets/simpleclouds/shaders/compute/cloud_regions.comp](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/src/main/resources/assets/simpleclouds/shaders/compute/cloud_regions.comp)
- [/src/main/resources/assets/simpleclouds/shaders/core/cloud_region_tex.fsh](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/src/main/resources/assets/simpleclouds/shaders/core/cloud_region_tex.fsh)
- [/src/main/java/dev/nonamecrackers2/simpleclouds/client/event/SimpleCloudsClientEvents.java](https://github.com/nonamecrackers2/simple-clouds/blob/1.20.1/src/main/java/dev/nonamecrackers2/simpleclouds/client/event/SimpleCloudsClientEvents.java)
