# MultipleInventories [![Build Status](https://jenkins.carrade.eu/job/MultipleInventories/job/master/badge/icon)](https://jenkins.carrade.eu/job/MultipleInventories/job/master/)

_MultipleInventories_ is a Minecraft Bukkit plugin to separate inventories on a server per gamemode or world.

It was conceived to store data in a compatible way, converting players inventories to a JSON-based format close to the data structure Minecraft uses internally—as a consequence, this storage is very resilient to future updates, except if Mojang changes completely how the data is stored onto the server, dropping the NBT format, but this is _very_ unlikely.

To use it, add it to your server, start it, and then go to the configuration file in `plugins/MultipleInventories/config.yml` to update some options and the worlds groups. A world group is a group of worlds (yes) where players share the same inventory. If when a player change world, the worlds are not in the same world group, its inventory will change, reloading its previous inventory from when he/she were on the target world (or a new empty one if he was never there before).

The same applies when a player changes its gamemode, if the `per-gamemode-inventories` option is enabled (it is by default).

Players data are stored in the folder `plugins/MultipleInventories/snapshots`. Files are named after the players' UUID, on subfolders to avoid too many files on a single folder. The precise location is:

```
snapshots/<world-group>/<first two characters of the lowercased UUID>/<lowercased UUID>.<GAMEMODE>.json
```

If you were using another plugin to manage inventories and you want to switch to this one, an importer is available to migrate data to our own data structure. Currently, only MultiInv is supported for import, but feel free to ask if you want another plugin to be supported, by opening an issue.
