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
import com.google.gson.JsonPrimitive;
import fr.zcraft.zlib.tools.PluginLogger;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;


/**
 * A snapshot of a player state (inventories, experience, health, hunger,
 * saturation, potion effects). The snapshot is frozen in time, and cannot be
 * modified.
 */
public class PlayerSnapshot
{
    private static final Gson GSON = new Gson();

    private final int level;
    private final float exp;
    private final int expTotal;

    private final int foodLevel;
    private final float exhaustion;
    private final float saturation;

    private final double health;
    private final double maxHealth;

    private final Map<Integer, ItemStackSnapshot> inventory;
    private final Map<Integer, ItemStackSnapshot> enderChest;

    private final ItemStackSnapshot[] armor;

    private final Collection<PotionEffect> effects;


    /**
     * Creates a snapshot of a player using the given data. You should use
     * {@link #snap(Player)} instead.
     *
     * @param level      The XP level.
     * @param exp        The XP.
     * @param expTotal   The total XP.
     * @param foodLevel  The food level.
     * @param exhaustion The player's exhaustion.
     * @param saturation The player's saturation.
     * @param health     The player's health.
     * @param maxHealth  The player's maximal health.
     * @param inventory  The player's inventory snapshot.
     * @param enderChest The player's ender chest snapshot.
     * @param armor      The player's armor snapshot.
     * @param effects    The player's potion effects.
     *
     * @see #snap(Player) Easier method to create a snapshot that you should
     * use.
     */
    public PlayerSnapshot(int level, float exp, int expTotal, int foodLevel, float exhaustion, float saturation, double health, double maxHealth, Map<Integer, ItemStackSnapshot> inventory, Map<Integer, ItemStackSnapshot> enderChest, ItemStackSnapshot[] armor, Collection<PotionEffect> effects)
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
        this.effects = effects;
    }

    /**
     * Creates a snapshot of a player.
     *
     * @param player The player to snap.
     *
     * @return The snapshot, or {@code null} if the player was {@code null}.
     * @see #snap(Player, boolean) Snapshot while respawning.
     */
    public static PlayerSnapshot snap(final Player player)
    {
        return snap(player, false);
    }

    /**
     * Creates a snapshot of a player.
     *
     * @param player      The player to snap.
     * @param fromRespawn {@code true} if the snapshot was taken after a
     *                    respawn. It prevents storing empty health & hunger.
     *
     * @return The snapshot, or {@code null} if the player was {@code null}.
     */
    public static PlayerSnapshot snap(final Player player, boolean fromRespawn)
    {
        if (player == null) return null;

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
                !fromRespawn ? player.getFoodLevel() : 20,
                !fromRespawn ? player.getExhaustion() : 0,
                !fromRespawn ? player.getSaturation() : 5,
                !fromRespawn ? player.getHealth() : player.getMaxHealth(),
                player.getMaxHealth(),
                snapInventory(player.getInventory()),
                snapInventory(player.getEnderChest()),
                snapArmor,
                player.getActivePotionEffects()
        );
    }

    /**
     * Creates a snapshot of an inventory.
     *
     * @param inventory The inventory.
     *
     * @return The snapshot.
     */
    private static Map<Integer, ItemStackSnapshot> snapInventory(final Inventory inventory)
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

    /**
     * Applies the snapshot on the given player.
     *
     * This will restore all the player properties, and clear then reload his
     * inventory as it was when the snapshot was taken.
     *
     * @param player The player.
     */
    public void reconstruct(final Player player)
    {
        player.setLevel(level);
        player.setExp(exp);
        player.setTotalExperience(expTotal);
        player.setFoodLevel(foodLevel);
        player.setExhaustion(exhaustion);
        player.setSaturation(saturation);
        player.setHealth(health);
        player.setMaxHealth(maxHealth);

        reconstructInventory(player.getInventory(), inventory);
        reconstructInventory(player.getEnderChest(), enderChest);

        final ItemStack[] newArmor = new ItemStack[player.getInventory().getArmorContents().length];
        for (int i = 0; i < armor.length; i++)
        {
            newArmor[i] = armor[i] != null ? armor[i].reconstruct() : null;
        }

        player.getInventory().setArmorContents(newArmor);

        for (PotionEffect effect : player.getActivePotionEffects())
            player.removePotionEffect(effect.getType());

        for (PotionEffect effect : effects)
            effect.apply(player);
    }

    /**
     * Clears then reconstructs from the snapshot the given inventory.
     *
     * @param inventory The inventory to reconstruct.
     * @param snapshot  The snapshot to apply.
     */
    private void reconstructInventory(final Inventory inventory, final Map<Integer, ItemStackSnapshot> snapshot)
    {
        inventory.clear();

        for (final Map.Entry<Integer, ItemStackSnapshot> entry : snapshot.entrySet())
        {
            inventory.setItem(entry.getKey(), entry.getValue().reconstruct());
        }
    }


    /**
     * @return A JSON export of this snapshot (including inventories and {@link
     * ItemStackSnapshot item snapshots}.
     */
    @Override
    public String toString()
    {
        return toJSONString();
    }

    /**
     * @return A JSON export of this snapshot (including inventories and {@link
     * ItemStackSnapshot item snapshots}.
     */
    public JsonElement toJSON()
    {
        final JsonObject dump = new JsonObject();
        final JsonArray armorDump = new JsonArray();
        final JsonArray effectsDump = new JsonArray();

        for (final ItemStackSnapshot item : armor)
        {
            armorDump.add(item != null ? item.toJSON() : JsonNull.INSTANCE);
        }

        for (final PotionEffect effect : effects)
        {
            effectsDump.add(toJSON(effect));
        }

        dump.addProperty("level", level);
        dump.addProperty("exp", exp);
        dump.addProperty("expTotal", expTotal);
        dump.addProperty("foodLevel", foodLevel);
        dump.addProperty("exhaustion", exhaustion);
        dump.addProperty("saturation", saturation);
        dump.addProperty("health", health);
        dump.addProperty("maxHealth", maxHealth);

        dump.add("armor", armorDump);
        dump.add("inventory", toJSON(inventory));
        dump.add("enderChest", toJSON(enderChest));
        dump.add("effects", effectsDump);

        return dump;
    }

    /**
     * @return A JSON export of this snapshot (including inventories and {@link
     * ItemStackSnapshot item snapshots}.
     */
    private JsonElement toJSON(final Map<Integer, ItemStackSnapshot> inventory)
    {
        final JsonObject dump = new JsonObject();

        for (Map.Entry<Integer, ItemStackSnapshot> entry : inventory.entrySet())
        {
            dump.add(entry.getKey().toString(), entry.getValue() != null ? entry.getValue().toJSON() : JsonNull.INSTANCE);
        }

        return dump;
    }

    private JsonElement toJSON(final PotionEffect effect)
    {
        final JsonObject dump = new JsonObject();

        dump.addProperty("type", effect.getType().getName());
        dump.addProperty("duration", effect.getDuration());
        dump.addProperty("amplifier", effect.getAmplifier());
        dump.addProperty("ambient", effect.isAmbient());
        dump.addProperty("has-particles", effect.hasParticles());
        dump.addProperty("color", effect.getColor() != null ? effect.getColor().asRGB() : null);

        return dump;
    }

    /**
     * @return A JSON export of this snapshot (including inventories and {@link
     * ItemStackSnapshot item snapshots}.
     */
    public String toJSONString()
    {
        return GSON.toJson(toJSON());
    }

    /**
     * Constructs a snapshot from a JSON export (including {@link
     * ItemStackSnapshot item snapshots} in the inventories).
     *
     * @param json The JSON export.
     *
     * @return The snapshot.
     */
    public static PlayerSnapshot fromJSONString(final String json)
    {
        return fromJSON(GSON.fromJson(json, JsonObject.class));
    }

    /**
     * Constructs a snapshot from a JSON export (including {@link
     * ItemStackSnapshot item snapshots} in the inventories).
     *
     * @param json The JSON export.
     *
     * @return The snapshot.
     */
    public static PlayerSnapshot fromJSON(final JsonObject json)
    {
        final JsonArray jsonArmor = json.getAsJsonArray("armor");
        final ItemStackSnapshot[] armor = new ItemStackSnapshot[jsonArmor.size()];

        for (int i = 0; i < jsonArmor.size(); i++)
        {
            final JsonElement armorItem = jsonArmor.get(i);
            armor[i] = !armorItem.isJsonNull() ? ItemStackSnapshot.fromJSON(armorItem.getAsJsonObject()) : null;
        }

        final JsonArray jsonEffects = json.getAsJsonArray("effects");
        final List<PotionEffect> effects = new ArrayList<>();

        for (int i = 0; i < jsonEffects.size(); i++)
        {
            effects.add(potionEffectFromJSON(jsonEffects.get(i).getAsJsonObject()));
        }

        return new PlayerSnapshot(
                json.getAsJsonPrimitive("level").getAsInt(),
                json.getAsJsonPrimitive("exp").getAsFloat(),
                json.getAsJsonPrimitive("expTotal").getAsInt(),
                json.getAsJsonPrimitive("foodLevel").getAsInt(),
                json.getAsJsonPrimitive("exhaustion").getAsFloat(),
                json.getAsJsonPrimitive("saturation").getAsFloat(),
                json.getAsJsonPrimitive("health").getAsDouble(),
                json.getAsJsonPrimitive("maxHealth").getAsDouble(),
                inventoryFromJSON(json.getAsJsonObject("inventory")),
                inventoryFromJSON(json.getAsJsonObject("enderChest")),
                armor,
                effects
        );
    }

    /**
     * Imports an inventory snapshot from a JSON export.
     *
     * @param json The JSON export.
     *
     * @return The inventory snapshot.
     */
    private static Map<Integer, ItemStackSnapshot> inventoryFromJSON(final JsonObject json)
    {
        final Map<Integer, ItemStackSnapshot> snapshot = new HashMap<>();

        for (Map.Entry<String, JsonElement> jsonItemEntry : json.entrySet())
        {
            try
            {
                snapshot.put(Integer.parseInt(jsonItemEntry.getKey()), ItemStackSnapshot.fromJSON(jsonItemEntry.getValue().getAsJsonObject()));
            }
            catch (NumberFormatException e)
            {
                PluginLogger.error("Skipping item with invalid index {0} from JSON snapshot", jsonItemEntry.getKey());
            }
        }

        return snapshot;
    }

    private static PotionEffect potionEffectFromJSON(final JsonObject json)
    {
        final JsonPrimitive color = json.getAsJsonPrimitive("color");

        return new PotionEffect(
                PotionEffectType.getByName(json.getAsJsonPrimitive("type").getAsString()),
                json.getAsJsonPrimitive("duration").getAsInt(),
                json.getAsJsonPrimitive("amplifier").getAsInt(),
                json.getAsJsonPrimitive("ambient").getAsBoolean(),
                json.getAsJsonPrimitive("has-particles").getAsBoolean(),
                color != null && !color.isJsonNull() ? Color.fromRGB(color.getAsInt()) : null
        );
    }
}
