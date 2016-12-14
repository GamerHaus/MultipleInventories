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

import fr.zcraft.MultipleInventories.snaphots.PlayerSnapshot;
import fr.zcraft.MultipleInventories.snaphots.SnapshotsIO;
import fr.zcraft.zlib.components.worker.WorkerCallback;
import fr.zcraft.zlib.tools.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Stores the snapshots of a player.
 */
public class PlayerSnapshotsStore
{
    private UUID playerID;
    private Map<String, Map<GameMode, PlayerSnapshot>> snapshots = new ConcurrentHashMap<>();

    /**
     * Updated when the player data is being loaded, when he just logged in.
     * Avoids world or gamemode change if data is not loaded, as it would cause
     * losses.
     */
    private boolean dataLoaded = false;

    /**
     * Updated when the player state is being saved then changed, when he
     * updates its gamemode or teleport. Avoids exploits like rapid world or
     * gamemode change to duplicate stuff.
     */
    private boolean changesBeingApplied = false;


    public PlayerSnapshotsStore(OfflinePlayer player)
    {
        this(player.getUniqueId());
    }

    public PlayerSnapshotsStore(UUID playerID)
    {
        this.playerID = playerID;
    }

    /**
     * Loads the snapshots from the disk. Should be called only once when the
     * player login.
     */
    public void loadSnapshots()
    {
        SnapshotsIO.loadSnapshots(playerID, new WorkerCallback<Map<String, Map<GameMode, PlayerSnapshot>>>()
        {
            @Override
            public void finished(final Map<String, Map<GameMode, PlayerSnapshot>> result)
            {
                snapshots.clear();
                snapshots.putAll(result);
                dataLoaded = true;
            }

            @Override
            public void errored(final Throwable exception)
            {
                PluginLogger.error("Unable to load snapshots for player {0} ({1}), check the access permissions of the storage directory. This player will have its inventory lost in world-group or gamemode change!", exception, Bukkit.getOfflinePlayer(playerID).getName(), playerID);
                dataLoaded = true;
            }
        });
    }

    /**
     * Returns a snapshot for the given group and gamemode.
     *
     * @param group    The worlds group.
     * @param gamemode The gamemode.
     *
     * @return The snapshot, or {@code null} if none found.
     */
    public PlayerSnapshot getSnapshot(final String group, final GameMode gamemode)
    {
        final Map<GameMode, PlayerSnapshot> groupSnapshots = snapshots.get(group);
        if (groupSnapshots == null) return null;

        return groupSnapshots.containsKey(gamemode) ? groupSnapshots.get(gamemode) : null;
    }

    /**
     * Saves a snapshot in the given group and gamemode. A previous snapshot is
     * overwritten.
     *
     * @param group    The worlds group.
     * @param gamemode The gamemode.
     * @param snapshot The snapshot to save.
     */
    public void saveSnapshot(final String group, final GameMode gamemode, final PlayerSnapshot snapshot)
    {
        Map<GameMode, PlayerSnapshot> groupSnapshots = snapshots.get(group);

        if (groupSnapshots == null)
        {
            groupSnapshots = new ConcurrentHashMap<>();
            snapshots.put(group, groupSnapshots);
        }

        groupSnapshots.put(gamemode, snapshot);

        SnapshotsIO.saveSnapshot(playerID, group, gamemode, snapshot);
    }

    /**
     * Applies the snapshot in the given group & gamemode to the player. If
     * no-one is found, the player state is reinitialized, like on a first
     * connection.
     *
     * @param group    The worlds group.
     * @param gamemode The gamemode.
     */
    public void applySnapshot(final String group, final GameMode gamemode)
    {
        final Player player = Bukkit.getPlayer(playerID);
        if (player == null || !player.isOnline()) return;

        final PlayerSnapshot snapshot = getSnapshot(group, gamemode);

        if (snapshot != null)
        {
            snapshot.reconstruct(player);
        }
        else
        {
            player.setLevel(0);
            player.setExp(0);
            player.setTotalExperience(0);
            player.setFoodLevel(20);
            player.setExhaustion(0);
            player.setSaturation(5);
            player.setHealth(20);
            player.setMaxHealth(20);

            for (final PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());

            player.getInventory().clear();
            player.getEnderChest().clear();
            player.getInventory().setArmorContents(new ItemStack[] {null, null, null, null});

            player.getInventory().setHeldItemSlot(0);
        }
    }

    /**
     * @return {@code true} if this player's data has been fully loaded.
     */
    public boolean isDataLoaded()
    {
        return dataLoaded;
    }

    /**
     * @return {@code true} if state save/update is being processed for this
     * player.
     */
    public boolean areChangesBeingApplied()
    {
        return changesBeingApplied;
    }

    /**
     * @param changesBeingApplied {@code true} if state save/update is being
     *                            processed for this player.
     */
    public void setChangesBeingApplied(boolean changesBeingApplied)
    {
        this.changesBeingApplied = changesBeingApplied;
    }
}
