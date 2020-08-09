# Synapse

Civ player utilities. Minecraft mod for LiteLoader

## Features

- Show waypoint at the last position each account was seen at (snitch hit, radar, pearl broadcast, engagement, seen by friends). Supports Journeymap and Voxelmap.
- Configure alt accounts for each person, as well as their factions.
- Show person's name next to the account name (name tag, tablist, chat messages), colored by standing (friendly/hostile/unset/neutral) according to the person's factions.
- Chat alerts show player distance and direction (NSEW/up/down), as well as movement direction (NSEW/up/down, approaching or moving away).
- Show a hoop or box on each player, colored by standing.
- Mark an account as "focused" (separate color) by pressing a keybind (or mouse), communicate to allies.
- Show chat message and play sound when player enters radar (same as CombatRadar's player notifications but with additional formatting/filtering options).
- Show number of nearby players, grouped by standing.
- Show number of health potions in inventory.
- Synchronize account standings with CombatRadar.

Each feature can be turned on/off and all message formatting can be customized.

## Usage

- Install [LiteLoader][liteloader] and optionally Forge (for JourneyMap). They can work together, for example versions `LiteLoader 1.12.2-SNAPSHOT` and `Forge 14.23.5.2854`
- Download [`Synapse-*.litemod`][latest-build] and put it into `.minecraft\mods\1.12.2` folder. Start Minecraft.
- Set up the mod's keyboard shortcuts in the Minecraft keyboard settings.
- Configuration: press the "Open settings GUI" keybind that you just assigned, then press "Settings" at the top left.

[liteloader]: https://www.liteloader.com/download#snapshot_11220
[latest-build]: https://github.com/Gjum/Synapse/releases/latest

## Features Roadmap
- new, more flexible formatting syntax
- skynet and tablist show combat logging
- some other trusted player can update person standings live via the team relay server
- when pearled, start periodically executing the pearl location command, either automatically or on keybind
- directional radar sound

## Config files

All settings are available through the settings GUI.
If you edit the config files while Minecraft is running, reload them from the settings GUI by pressing `Reload config files` at the bottom.

- general settings: `.minecraft\Synapse\config.json`
- per server settings: `.minecraft\Synapse\servers\<server>\server.json`
- persons database (accounts, factions): `.minecraft\Synapse\servers\<server>\persons.json`
- all accounts ever seen across all servers: `.minecraft\Synapse\accounts.txt` - used for account completion suggestions

## Development

The following assumes Linux/Mac. If you're on Windows, replace `./gradlew` with `gradlew.bat` and `/` with `\`

### Debugging

- Run `./gradlew litemod:genIntellijRuns` or `./gradlew litemod:eclipseProject` depending on your preference.
    In the generated run configurations it may be necessary to set the working directory to `litemod/run` and module classpath to `Synapse.litemod.main`
- Optional: Run `./gradlew litemod:setupDecompWorkspace` to generate deobfuscated Minecraft source code as a reference.
- If the library mods (CombatRadar/VoxelMap/JourneyMap) fail to load (big red error messages ingame), disable them.
    This is because the runtime uses MCP names, while the library mods expect notch names (obfuscated).
    They should be disabled by default via the provided `litemod/run/liteconfig/liteloader.profiles.json`,
    but this may be ignored by your setup if you are using a different `run` directory.
- Hot reloading should work by default, just press "Build" in your IDE.
    Remember that this only allows modifying method bodies, but not adding/removing classes/methods/fields or changing their order/signature.

### Releasing a new version

- Update `version` in `litemod/gradle.properties`
- Run `./gradlew litemod:reobfJar`
- The resulting mod is located in `litemod/build/libs/`
