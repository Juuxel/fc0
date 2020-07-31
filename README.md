# fc0

**fc0** is a modding toolkit for the game [2fc0f18](https://github.com/valoeghese/2fc-early-releases).

## Design

fc0 is designed around modding with source patches. It also comes with
intermediary mappings, match files and proper deobfuscation mappings,
a bit like [Yarn](https://github.com/FabricMC/yarn) for Minecraft.

### Structure
- `buildSrc`: The actual toolchain code, mostly Gradle tasks.
- `bytecode_tweaks`: Bytecode tweak configuration files for fc0 Tools,
  currently used to mark bridge methods as such in bytecode.
- `intermediary`: Deobfuscation mappings between obfuscated names and intermediary names,
  which mostly stay constant between versions.
- `mappings`: Deobfuscation mappings between intermediary names and readable names.
- `matches`: Match files for migrating mappings between versions.
- `patches`: Source patches for making the game recompile.
- `src`: Modifiable game sources that will be generated with `gradle setup`.

## Setup

### For modding
If you want to get a modding environment running,
clone this repo, import it to IDEA and run
`gradle setup applyPatches`.

When you want to build your mod, run `gradle build`.
Your mod will be saved in `build/libs` as `2fc0f18-[version]-mod.jar`

### For mapping
Run `gradle enigma`.

## Related projects

- [fc0 Launcher](https://github.com/Juuxel/fc0-launcher) is a simple launcher
  for 2fc0f18. I made it because it was easier to run the game in a dev environment
  than outside of it.
- [fc0 Tools](https://github.com/Juuxel/fc0-tools) contains various bytecode and source code
  tools used by the fc0 environment. In the future, more fc0 toolchain code will be moved there
  so this repo becomes smaller.
