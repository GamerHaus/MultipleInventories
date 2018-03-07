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
package fr.zcraft.MultipleInventories.snaphots;

import fr.zcraft.MultipleInventories.MultipleInventories;
import fr.zcraft.zlib.components.worker.Worker;
import fr.zcraft.zlib.components.worker.WorkerAttributes;
import fr.zcraft.zlib.components.worker.WorkerCallback;
import fr.zcraft.zlib.components.worker.WorkerRunnable;
import fr.zcraft.zlib.tools.FileUtils;
import fr.zcraft.zlib.tools.PluginLogger;
import org.bukkit.GameMode;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@WorkerAttributes (name = "multiple-inventories-snapshots-io")
public class SnapshotsIO extends Worker
{
    private static File getFileForSnapshot(final UUID playerID, final String group, final GameMode gamemode)
    {
        final String lowercaseUUID = playerID.toString().toLowerCase();

        return new File(
                MultipleInventories.get().getDataFolder(),
                "snapshots"
                        + File.separator + group
                        + File.separator + lowercaseUUID.substring(0, 2)
                        + File.separator + lowercaseUUID + "." + gamemode.name() + ".json");
    }

    public static void saveSnapshot(final UUID playerID, final String group, final GameMode gamemode, final PlayerSnapshot snapshot)
    {
        submitQuery(new WorkerRunnable<Void>()
        {
            @Override
            public Void run() throws Throwable
            {
                final String json = snapshot.toJSONString();
                final File storageFile = getFileForSnapshot(playerID, group, gamemode);

                final File directory = storageFile.getParentFile();
                if (!directory.mkdirs() && !(directory.exists() && directory.isDirectory()))
                    throw new IOException("Cannot write into the snapshots storage directory (cannot create folders)");

                FileUtils.writeFile(storageFile, json);

                return null;
            }
        }, new WorkerCallback<Void>()
        {
            @Override
            public void finished(final Void result) {}

            @Override
            public void errored(final Throwable exception)
            {
                PluginLogger.error("Unable to save player snapshot for group {0}, gamemode {1} and UUID {2}", exception, group, gamemode, playerID);
            }
        });
    }

    public static void loadSnapshots(final UUID playerID, final WorkerCallback<Map<String, Map<GameMode, PlayerSnapshot>>> callback)
    {
        final Set<String> groups = MultipleInventories.get().getPlayersManager().getWorldGroupsNames();

        submitQuery(new WorkerRunnable<Map<String, Map<GameMode, PlayerSnapshot>>>() {
            @Override
            public Map<String, Map<GameMode, PlayerSnapshot>> run()
            {
                final Map<String, Map<GameMode, PlayerSnapshot>> snapshots = new ConcurrentHashMap<>();

                for (final String group : groups)
                {
                    final Map<GameMode, PlayerSnapshot> groupSnapshots = new ConcurrentHashMap<>();

                    for (final GameMode mode : GameMode.values())
                    {
                        final String jsonSnapshot = FileUtils.readFile(getFileForSnapshot(playerID, group, mode));

                        if (!jsonSnapshot.isEmpty())
                        {
                            groupSnapshots.put(mode, PlayerSnapshot.fromJSONString(jsonSnapshot));
                        }
                    }

                    snapshots.put(group, groupSnapshots);
                }

                return snapshots;
            }
        }, callback);
    }
}
