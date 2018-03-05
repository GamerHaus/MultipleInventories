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
package fr.zcraft.MultipleInventories.importers;

import fr.zcraft.MultipleInventories.snaphots.PlayerSnapshot;
import fr.zcraft.MultipleInventories.snaphots.SnapshotsIO;
import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.core.ZLib;
import fr.zcraft.zlib.tools.PluginLogger;
import fr.zcraft.zlib.tools.runners.RunTask;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


public class ImportProcess
{
    private final static int PLAYERS_PER_TICK = 16;
    private final static int RUN_EVERY_N_TICKS = 1;

    private final Importer importer;
    private final String importerName;
    private final CommandSender user;

    private final Queue<OfflinePlayer> importQueue = new ArrayDeque<>();
    private final Map<String, Set<String>> worldGroups = new HashMap<>();

    private int playersCountToProcess = 0;
    private int lastPercentage = -1;
    private double meanExecutionTimePerPlayer = 0.0;

    private boolean started = false;
    private boolean running = false;
    private ImportListener importListener;


    public ImportProcess(final Importer importer, final CommandSender user)
    {
        this.importer = importer;
        this.importerName = importer.getClass().getSimpleName().replace("Importer", "");
        this.user = user;
    }

    private void log(String message)
    {
        user.sendMessage(message);

        if (!(user instanceof ConsoleCommandSender))
            Bukkit.getConsoleSender().sendMessage(message);
    }

    /**
     * Starts the import process.
     */
    public void begin()
    {
        if (!importer.canImport())
        {
            log(I.t("{ce}The {0} importer cannot run, probably due to a dependency missing. Aborting.", importerName));
            return;
        }

        importListener = new ImportListener();
        ZLib.registerEvents(importListener);

        Bukkit.getOnlinePlayers()
              .forEach(player -> player.kickPlayer(I.t("{ce}Maintenance started, please come back later.") + "\n\n" + I.t("{gray}ETA: {0}", getHumanFriendlyETA())));

        started = true;
        running = true;
        importer.onBegin();

        Arrays.stream(Bukkit.getOfflinePlayers()).forEach(importQueue::offer);

        playersCountToProcess = importQueue.size();

        worldGroups.clear();
        worldGroups.putAll(importer.getWorldGroups());

        log(
                I.tn(
                        "{cs}Starting import, processing {0} player every {1} ticks, {2} players total.",
                        "{cs}Starting import, processing {0} players every {1} ticks, {2} players total.",
                        PLAYERS_PER_TICK,
                        PLAYERS_PER_TICK, RUN_EVERY_N_TICKS, playersCountToProcess
                )
        );

        PluginLogger.info(I.t("Groups found by the importer:"));
        worldGroups.forEach((group, worlds) -> PluginLogger.info(
                I.tn("- {0}, with world: {1}", "- {0}, with worlds: {1}", worlds.size(), group, StringUtils.join(worlds, ", "))
        ));

        RunTask.timer(new ImportRunnable(), 2L, RUN_EVERY_N_TICKS);
    }

    /**
     * Ends the import process.
     */
    private void end()
    {
        log(I.t("{cs}Import complete."));

        importer.onEnd();
        running = false;
    }

    /**
     * @return The total amount of players to process.
     */
    public int getPlayersCountToProcess()
    {
        return playersCountToProcess;
    }

    /**
     * @return The amount of players already imported.
     */
    public int getAmountProcessed()
    {
        return playersCountToProcess - importQueue.size();
    }

    /**
     * @return The estimated time remaining, in seconds.
     */
    public int getETA()
    {
        return (int) Math.ceil((meanExecutionTimePerPlayer / 1000.0) * importQueue.size());
    }

    /**
     * @return The estimated time remaining, formatted for human read.
     */
    public String getHumanFriendlyETA()
    {
        final int eta = getETA();
        if (eta > 0)
        {
            String friendlyETA = LocalTime.MIN.plusSeconds(eta).toString();

            // Adds seconds if needed, as LocalTime does not includes them if zero.
            if (friendlyETA.length() == 5) friendlyETA += ":00";

            return friendlyETA;
        }
        else return I.t("currently unknown");
    }

    /**
     * @return The current progress (percentage).
     */
    public int getProgressPercentage()
    {
        return lastPercentage;
    }

    private class ImportRunnable extends BukkitRunnable
    {
        @Override
        public void run()
        {
            for (int i = 0; i < PLAYERS_PER_TICK; i++)
            {
                final OfflinePlayer player = importQueue.poll();

                if (player == null)
                {
                    end();
                    cancel();
                    return;
                }

                final long t = System.currentTimeMillis();

                for (final String group : worldGroups.keySet())
                {
                    for (final GameMode mode : GameMode.values())
                    {
                        final PlayerSnapshot snapshot = importer.importSnapshot(player, group, mode);
                        if (snapshot != null)  SnapshotsIO.saveSnapshot(player.getUniqueId(), group, mode, snapshot);
                    }
                }

                // Calculates the cumulative moving average so we can estimate the time left
                final double executionTime = (double) (System.currentTimeMillis() - t);
                final double amountProcessed = (double) getAmountProcessed();

                meanExecutionTimePerPlayer = (executionTime + (amountProcessed - 1) * meanExecutionTimePerPlayer) / amountProcessed;

                // …and the percentage, to be displayed when it changes of a few cycles after the beginning,
                // when the ETA is more reliable.
                final int processed = playersCountToProcess - importQueue.size();
                final int percentage = (int) Math.floor((((double) (processed)) / ((double) playersCountToProcess)) * 100);

                if (percentage != lastPercentage || processed == 3 * PLAYERS_PER_TICK)
                {
                    PluginLogger.info(
                            I.t(
                                    "Importing snapshots from {0}: {1}%... ({2} / {3}) - ETA {4} ({5} ms per player)",
                                    importerName, percentage, processed,
                                    playersCountToProcess, getHumanFriendlyETA(), meanExecutionTimePerPlayer
                            )
                    );

                    lastPercentage = percentage;
                }
            }
        }
    }

    private class ImportListener implements Listener
    {
        @EventHandler (priority = EventPriority.HIGHEST)
        public void onPlayerPreLogin(AsyncPlayerPreLoginEvent ev)
        {
            if (running)
            {
                ev.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        "\n" + I.t("{ce}Maintenance in progress, please come back later.") + "\n\n" +
                                /// Completion displayed in the kick message when a player tries to login during a migration.
                                I.t("{gray}{0}% completed ({1} / {2})", getProgressPercentage(), getAmountProcessed(), getPlayersCountToProcess()) + "\n" +
                                /// ETA displayed in the kick message when a player tries to login during a migration.
                                I.t("{gray}ETA: {0}", getHumanFriendlyETA())
                );
            }
            else if (started)
            {
                ev.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        "\n" + I.t("{ce}Maintenance in progress, please come back later.") + "\n\n" +
                                /// Message displayed to the player when the migration is finished and the admin is needed to reboot
                                I.t("{gray}The migration completed; we still need an admin to finish the migration by hand and reboot the server.") + "\n" +
                                /// Message displayed to the player when the migration is finished and the admin is needed to reboot
                                I.t("{gray}If you are the admin, check out the console.")
                );
            }
        }
    }
}
