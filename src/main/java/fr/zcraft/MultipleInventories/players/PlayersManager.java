/*
 * Copyright or © or Copr. AmauryCarrade (2015)
 * 
 * http://amaury.carrade.eu
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.zcraft.MultipleInventories.players;

import fr.zcraft.MultipleInventories.Config;
import fr.zcraft.MultipleInventories.snaphots.PlayerSnapshot;
import fr.zcraft.MultipleInventories.quartzlib.core.QuartzComponent;
import fr.zcraft.MultipleInventories.quartzlib.tools.runners.RunTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.*;


public class PlayersManager extends QuartzComponent implements Listener
{
    private final static String DEFAULT_WORLD_GROUP = "default";

    private final Map<UUID, PlayerSnapshotsStore> players = new HashMap<>();

    private final Map<String, Set<String>> worldsGroups = new HashMap<>();
    private final Map<String, String> reversedWorldGroups = new HashMap<>();


    @Override
    protected void onEnable()
    {
        loadWorldsGroups();

        // The snapshot IO needs the players manager, so if this is executed directly in the enable method,
        // the import will crash on reload.
        RunTask.nextTick(() -> Bukkit.getOnlinePlayers().forEach(this::getStore));
    }

    /**
     * (Re)loads the worlds groups from the configuration.
     */
    public void loadWorldsGroups()
    {
        worldsGroups.clear();
        reversedWorldGroups.clear();

        Config.WORLD_GROUPS.forEach((group_name, worlds_names) ->
        {
            final Set<String> worlds = new HashSet<>();

            for (final Object world : worlds_names)
            {
                worlds.add(world.toString());
                reversedWorldGroups.put(world.toString(), group_name);
            }

            worldsGroups.put(group_name, worlds);
        });

        // We store non-listed worlds in the default group
        Bukkit.getWorlds().forEach(this::registerWorldInDefault);
    }

    /**
     * Registers a world in the default group, if not already in a group. Used
     * for unknown worlds at startup, and for worlds created/loaded during the
     * game.
     *
     * @param world The world.
     */
    private void registerWorldInDefault(World world)
    {
        if (!reversedWorldGroups.containsKey(world.getName()))
        {
            if (!worldsGroups.containsKey(DEFAULT_WORLD_GROUP))
            {
                worldsGroups.put(DEFAULT_WORLD_GROUP, new HashSet<>());
            }

            worldsGroups.get(DEFAULT_WORLD_GROUP).add(world.getName());
            reversedWorldGroups.put(world.getName(), DEFAULT_WORLD_GROUP);
        }
    }

    /**
     * Creates, queue load and return a store for the given player. If the store
     * was already loaded, it is returned directly (this will probably be the
     * most common case).
     *
     * @param playerID The player's UUID.
     *
     * @return The store, queued for load, or loaded if it was already.
     */
    public PlayerSnapshotsStore getStore(final UUID playerID)
    {
        if (players.containsKey(playerID))
            return players.get(playerID);

        final PlayerSnapshotsStore store = new PlayerSnapshotsStore(playerID);
        store.loadSnapshots();

        players.put(playerID, store);

        return store;
    }

    /**
     * Creates, queue load and return a store for the given player. If the store
     * is already loaded, it is returned directly (this will probably be the
     * most common case).
     *
     * @param player The player.
     *
     * @return The store, queued for load, or loaded if it was already.
     */
    public PlayerSnapshotsStore getStore(final Player player)
    {
        return getStore(player.getUniqueId());
    }

    /**
     * Unloads the store of the given player, e.g. when logging out.
     * The store will be re-loaded the next time {@link #getStore(UUID)}
     * is called.
     *
     * @param playerID The player's UUID.
     */
    public void unloadStore(final UUID playerID)
    {
        players.remove(playerID);
    }

    /**
     * Unloads the store of the given player, e.g. when logging out.
     * The store will be re-loaded the next time {@link #getStore(Player)}
     * is called.
     *
     * @param player The player.
     */
    public void unloadStore(final Player player)
    {
        unloadStore(player.getUniqueId());
    }

    /**
     * @param world A world.
     *
     * @return The group this world is into.
     */
    public String getGroupForWorld(final World world)
    {
        return reversedWorldGroups.get(world.getName());
    }

    /**
     * @param group A group name (case sensitive).
     *
     * @return The worlds names in this group (read-only).
     */
    public Set<String> getWorldsInGroup(final String group)
    {
        return Collections.unmodifiableSet(worldsGroups.get(group));
    }

    /**
     * @return A read-only set containing the groups names.
     */
    public Set<String> getWorldGroupsNames()
    {
        return Collections.unmodifiableSet(worldsGroups.keySet());
    }

    /**
     * @return A read-only map containing the groups.
     */
    public Map<String, Set<String>> getWorldsGroups()
    {
        final Map<String, Set<String>> groups = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : worldsGroups.entrySet())
        {
            groups.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }

        return Collections.unmodifiableMap(groups);
    }


    /**
     * Handles a world change by a player.
     *
     * @param player    The player.
     * @param oldWorld  The old player world.
     * @param newWorld  The new player world.
     * @param isRespawn {@code true} if the world change is due to a respawn.
     *
     * @return {@code false} if the underlying event have to be cancelled (e.g.
     * because snapshots were not fully loaded).
     */
    private boolean handleWorldChange(final Player player, final World oldWorld, final World newWorld, boolean isRespawn)
    {
        final String oldGroup = getGroupForWorld(oldWorld);
        final String newGroup = getGroupForWorld(newWorld);

        // If both groups are the same, we don't have to do anything.
        if (oldGroup.equals(newGroup)) return true;

        final PlayerSnapshotsStore store = getStore(player);
        if (!store.isDataLoaded() || store.areChangesBeingApplied())
            return false;

        // In very rare cases (especially with Essentials), the gamemode can be null.
        final GameMode gamemode = player.getGameMode() != null && Config.PER_GAMEMODE_INVENTORIES.get() ? player.getGameMode() : GameMode.SURVIVAL;

        store.saveSnapshot(oldGroup, gamemode, PlayerSnapshot.snap(player, isRespawn));
        store.applySnapshotFromStateNextTick();

        return true;
    }

    /**
     * Handles a gamemode change by a player.
     *
     * @param player      The player.
     * @param oldGameMode The old gamemode.
     * @param newGameMode The new gamemode.
     *
     * @return {@code false} if the underlying event have to be cancelled (e.g.
     * because snapshots were not fully loaded).
     */
    private boolean handleGameModeChange(final Player player, final GameMode oldGameMode, final GameMode newGameMode)
    {
        if (oldGameMode == newGameMode || !Config.PER_GAMEMODE_INVENTORIES.get())
            return true;

        final PlayerSnapshotsStore store = getStore(player);
        if (!store.isDataLoaded() || store.areChangesBeingApplied())
            return false;

        final String group = getGroupForWorld(player.getWorld());

        store.saveSnapshot(group, oldGameMode, PlayerSnapshot.snap(player));
        store.applySnapshotFromStateNextTick();

        return true;
    }



    /* **  EVENT HANDLERS  ** */

    private void unloadStoreLater(final Player player)
    {
        final UUID playerID = player.getUniqueId();

        RunTask.later(() ->
        {
            final Player player1 = Bukkit.getPlayer(playerID);
            if (player1 == null || !player1.isOnline())
            {
                unloadStore(playerID);
            }
        }, 60 * 20L);
    }


    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncPlayerLogin(final AsyncPlayerPreLoginEvent ev)
    {
        if (ev.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED)
        {
            getStore(ev.getUniqueId());
        }
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogout(final PlayerQuitEvent ev)
    {
        unloadStoreLater(ev.getPlayer());
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLogout(final PlayerKickEvent ev)
    {
        unloadStoreLater(ev.getPlayer());
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(final PlayerGameModeChangeEvent ev)
    {
        ev.setCancelled(!handleGameModeChange(ev.getPlayer(), ev.getPlayer().getGameMode(), ev.getNewGameMode()));
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(final PlayerTeleportEvent ev)
    {
        if (ev.getFrom().getWorld().equals(ev.getTo().getWorld())) return;
        ev.setCancelled(!handleWorldChange(ev.getPlayer(), ev.getFrom().getWorld(), ev.getTo().getWorld(), false));
    }

    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawnToADifferentWorld(final PlayerRespawnEvent ev)
    {
        if (ev.getPlayer().getWorld().equals(ev.getRespawnLocation().getWorld())) return;
        handleWorldChange(ev.getPlayer(), ev.getPlayer().getWorld(), ev.getRespawnLocation().getWorld(), true);
    }


    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoaded(WorldLoadEvent ev)
    {
        registerWorldInDefault(ev.getWorld());
    }
}
