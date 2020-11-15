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

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import fr.zcraft.MultipleInventories.MultipleInventories;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A snapshot of a player state (inventories, experience, health, hunger,
 * saturation, potion effects). The snapshot is frozen in time, and cannot be
 * modified.
 */
public class PlayerSnapshot
{
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
     * @param expTotal   The total XP. -1 if unknown (e.g. from import).
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

        final ItemStackSnapshot[] snapArmor = Arrays.stream(player.getInventory().getArmorContents())
                                                    .map(ItemStackSnapshot::snap)
                                                    .toArray(ItemStackSnapshot[]::new);

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
        if (expTotal >= 0) player.setTotalExperience(expTotal);
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

        player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);

        effects.forEach(effect -> effect.apply(player));
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
        snapshot.forEach((index, itemSnapshot) -> inventory.setItem(index, itemSnapshot.reconstruct()));
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

        Arrays.stream(armor).map(item -> item != null ? item.toJSON() : JsonNull.INSTANCE).forEachOrdered(armorDump::add);

        effects.stream().map(this::toJSON).forEach(effectsDump::add);

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
     * @return A JSON export of an inventory.
     */
    private JsonElement toJSON(final Map<Integer, ItemStackSnapshot> inventory)
    {
        final JsonObject dump = new JsonObject();

        inventory.forEach((index, itemSnapshot) -> dump.add(index.toString(), itemSnapshot != null ? itemSnapshot.toJSON() : JsonNull.INSTANCE));

        return dump;
    }

    /**
     * @return A JSON export of the given potion effect.
     */
    private JsonElement toJSON(final PotionEffect effect)
    {
        final JsonObject dump = new JsonObject();

        dump.addProperty("type", effect.getType().getName());
        dump.addProperty("duration", effect.getDuration());
        dump.addProperty("amplifier", effect.getAmplifier());
        dump.addProperty("ambient", effect.isAmbient());
        dump.addProperty("has-particles", effect.hasParticles());
        dump.addProperty("has-icon", effect.hasIcon());

        return dump;
    }

    /**
     * @return A JSON export of this snapshot (including inventories and {@link
     * ItemStackSnapshot item snapshots}.
     */
    public String toJSONString()
    {
        return MultipleInventories.GSON.toJson(toJSON());
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
        return fromJSON((new JsonParser().parse(json)).getAsJsonObject());
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
            armor[i] = armorItem.isJsonObject() ? ItemStackSnapshot.fromJSON(armorItem.getAsJsonObject()) : null;
        }

        final JsonArray jsonEffects = json.get("effects").isJsonArray() ? json.getAsJsonArray("effects") : new JsonArray();
        final List<PotionEffect> effects = Streams.stream(jsonEffects)
                                                  .filter(jsonEffect -> jsonEffect != null && jsonEffect.isJsonObject())
                                                  .map(jsonEffect -> potionEffectFromJSON(jsonEffect.getAsJsonObject()))
                                                  .collect(Collectors.toList());

        return new PlayerSnapshot(
                isNull(json, "level")      ? 0    : json.getAsJsonPrimitive("level").getAsInt(),
                isNull(json, "exp")        ? 0.0f : json.getAsJsonPrimitive("exp").getAsFloat(),
                isNull(json, "expTotal")   ? 0    : json.getAsJsonPrimitive("expTotal").getAsInt(),
                isNull(json, "foodLevel")  ? 20   : json.getAsJsonPrimitive("foodLevel").getAsInt(),
                isNull(json, "exhaustion") ? 0f   : json.getAsJsonPrimitive("exhaustion").getAsFloat(),
                isNull(json, "saturation") ? 5f   : json.getAsJsonPrimitive("saturation").getAsFloat(),
                isNull(json, "health")     ? 20.0 : json.getAsJsonPrimitive("health").getAsDouble(),
                isNull(json, "maxHealth")  ? 20.0 : json.getAsJsonPrimitive("maxHealth").getAsDouble(),
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

        json.entrySet().forEach(jsonItemEntry ->
        {
            if (!jsonItemEntry.getValue().isJsonObject()) return;

            try
            {
                snapshot.put(Integer.parseInt(jsonItemEntry.getKey()), ItemStackSnapshot.fromJSON(jsonItemEntry.getValue().getAsJsonObject()));
            }
            catch (NumberFormatException e)
            {
                PluginLogger.error("Skipping item with invalid index {0} from JSON snapshot", jsonItemEntry.getKey());
            }
        });

        return snapshot;
    }

    private static PotionEffect potionEffectFromJSON(final JsonObject json)
    {
        // 1.13+: there is no longer a color, but there is a `has-icon` flag.
        try
        {
            return Reflection.instantiate(PotionEffect.class,
                PotionEffectType.getByName(json.getAsJsonPrimitive("type").getAsString()),
                isNull(json, "duration") ? 1 : json.getAsJsonPrimitive("duration").getAsInt(),
                isNull(json, "amplifier") ? 1 : json.getAsJsonPrimitive("amplifier").getAsInt(),
                !isNull(json, "ambient") && json.getAsJsonPrimitive("ambient").getAsBoolean(),
                isNull(json, "has-particles") || json.getAsJsonPrimitive("has-particles").getAsBoolean(),
                isNull(json, "has-icon") || json.getAsJsonPrimitive("has-icon").getAsBoolean()
            );
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex)
        {
            // This one should always work
            return new PotionEffect(
                PotionEffectType.getByName(json.getAsJsonPrimitive("type").getAsString()),
                isNull(json, "duration") ? 1 : json.getAsJsonPrimitive("duration").getAsInt(),
                isNull(json, "amplifier") ? 1 : json.getAsJsonPrimitive("amplifier").getAsInt(),
                !isNull(json, "ambient") && json.getAsJsonPrimitive("ambient").getAsBoolean(),
                isNull(json, "has-particles") || json.getAsJsonPrimitive("has-particles").getAsBoolean()
            );
        }
    }

    private static boolean isNull(final JsonObject element, final String child)
    {
        final JsonElement childElement = element.get(child);
        return childElement == null || childElement.isJsonNull();
    }
}
