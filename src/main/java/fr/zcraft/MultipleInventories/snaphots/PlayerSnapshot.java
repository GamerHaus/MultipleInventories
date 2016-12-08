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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;


/**
 * A snapshot of a player state (inventory, experience,
 * health, hunger, saturation).
 */
public class PlayerSnapshot
{
    private static final Gson GSON = new Gson();

    private final int level;
    private final float exp;
    private final float expTotal;

    private final int foodLevel;
    private final float exhaustion;
    private final float saturation;

    private final double health;
    private final double maxHealth;

    private final Map<Integer, ItemStackSnapshot> inventory;
    private final Map<Integer, ItemStackSnapshot> enderChest;

    private final ItemStackSnapshot[] armor;


    public PlayerSnapshot(int level, float exp, float expTotal, int foodLevel, float exhaustion, float saturation, double health, double maxHealth, Map<Integer, ItemStackSnapshot> inventory, Map<Integer, ItemStackSnapshot> enderChest, ItemStackSnapshot[] armor)
    {
        this.level = level;
        this.exp = exp;
        this.expTotal = expTotal;
        this.foodLevel = foodLevel;
        this.exhaustion = exhaustion;
        this.saturation = saturation;
        this.health = health;
        this.maxHealth = maxHealth;
        this.inventory = inventory;
        this.enderChest = enderChest;
        this.armor = armor;
    }

    public static PlayerSnapshot snap(Player player)
    {
        final ItemStack[] armor = player.getInventory().getArmorContents();
        final ItemStackSnapshot[] snapArmor = new ItemStackSnapshot[armor.length];

        for (int i = 0; i < armor.length; i++)
        {
            snapArmor[i] = ItemStackSnapshot.snap(armor[i]);
        }

        return new PlayerSnapshot(
                player.getLevel(),
                player.getExp(),
                player.getTotalExperience(),
                player.getFoodLevel(),
                player.getExhaustion(),
                player.getSaturation(),
                player.getHealth(),
                player.getMaxHealth(),
                snapInventory(player.getInventory()),
                snapInventory(player.getEnderChest()),
                snapArmor
        );
    }

    private static Map<Integer, ItemStackSnapshot> snapInventory(Inventory inventory)
    {
        final Map<Integer, ItemStackSnapshot> snap = new HashMap<>();
        final ListIterator<ItemStack> iterator = inventory.iterator();

        while (iterator.hasNext())
        {
            final int index = iterator.nextIndex();
            final ItemStack item = iterator.next();

            if (item != null && item.getType() != Material.AIR)
                snap.put(index, ItemStackSnapshot.snap(item));
        }

        return snap;
    }

    public void reconstruct(Player player)
    {

    }


    @Override
    public String toString()
    {
        return toJSONString();
    }

    @SuppressWarnings ("unchecked")
    public JsonElement toJSON()
    {
        final JsonObject dump = new JsonObject();

        dump.addProperty("level", level);
        dump.addProperty("exp", exp);
        dump.addProperty("expTotal", expTotal);
        dump.addProperty("foodLevel", foodLevel);
        dump.addProperty("exhaustion", exhaustion);
        dump.addProperty("saturation", saturation);
        dump.addProperty("health", health);
        dump.addProperty("maxHealth", maxHealth);

        final JsonArray armorDump = new JsonArray();

        for (ItemStackSnapshot item : armor)
        {
            armorDump.add(item != null ? item.toJSON() : JsonNull.INSTANCE);
        }

        dump.add("armor", armorDump);

        dump.add("inventory", toJSON(inventory));
        dump.add("enderChest", toJSON(enderChest));

        return dump;
    }

    private JsonElement toJSON(Map<Integer, ItemStackSnapshot> inventory)
    {
        final JsonObject dump = new JsonObject();

        for (Map.Entry<Integer, ItemStackSnapshot> entry : inventory.entrySet())
        {
            dump.add(entry.getKey().toString(), entry.getValue() != null ? entry.getValue().toJSON() : JsonNull.INSTANCE);
        }

        return dump;
    }

    public String toJSONString()
    {
        return GSON.toJson(toJSON());
    }

    public static PlayerSnapshot fromJSONString(String json)
    {
        return null;
    }
}
