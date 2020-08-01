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

- Install [LiteLoader][liteloader]
- Download [`Synapse-*.litemod`][latest-build] and put it into `.minecraft\mods\1.12.2` folder. Start Minecraft.
- Set up the mod's keyboard shortcuts in the Minecraft keyboard settings.
- Configuration: either press the "Open GUI" keybind if you configured it; or: press Escape, open the "LiteLoader chicken" tab on the top right, select `Synapse` from the list, select `Settings...` on the bottom.

[liteloader]: https://www.liteloader.com/download#snapshot_11220
[latest-build]: https://github.com/Gjum/Synapse/releases/latest

## Features Roadmap
- new, more flexible formatting syntax
- skynet and tablist show combat logging
- some other trusted player can update person standings live via group chat
- when pearled, start broadcasting automatically or on keybind
- directional radar sound

## Config files

All settings are available through the settings GUI.
If you edit the config files while Minecraft is running, reload them from the settings GUI by pressing `Reload config files` at the bottom.

- general settings: `.minecraft\Synapse\config.json`
- per server settings: `.minecraft\Synapse\servers\<server>\server.json`
- persons database (accounts, factions): `.minecraft\Synapse\servers\<server>\persons.json`
- all accounts ever seen across all servers: `.minecraft\Synapse\accounts.txt` - used for account completion suggestions

## Development

- `cd` into the `litemod` directory
- optional: run `./gradlew setupDecompWorkspace` and edit some code
- run `./gradlew reobfJar`
- use `build/libs/*.litemod`
