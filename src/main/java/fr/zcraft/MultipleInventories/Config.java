package fr.zcraft.MultipleInventories;

import fr.zcraft.quartzlib.components.configuration.Configuration;
import fr.zcraft.quartzlib.components.configuration.ConfigurationItem;
import fr.zcraft.quartzlib.components.configuration.ConfigurationMap;

import java.util.List;
import java.util.Locale;

import static fr.zcraft.quartzlib.components.configuration.ConfigurationItem.item;
import static fr.zcraft.quartzlib.components.configuration.ConfigurationItem.map;


public class Config extends Configuration
{
    static public final ConfigurationItem<Locale> LOCALE = item("locale", null);
    static public final ConfigurationItem<Boolean> PER_GAMEMODE_INVENTORIES = item("per-gamemode-inventories", true);
    static public final ConfigurationMap<String, List> WORLD_GROUPS = map("world-groups", String.class, List.class);
}
