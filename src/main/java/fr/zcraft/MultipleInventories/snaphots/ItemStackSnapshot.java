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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import fr.zcraft.MultipleInventories.MultipleInventories;
import fr.zcraft.MultipleInventories.quartzlib.components.nbt.NBT;
import fr.zcraft.MultipleInventories.quartzlib.components.nbt.NBTCompound;
import fr.zcraft.MultipleInventories.quartzlib.tools.PluginLogger;
import fr.zcraft.MultipleInventories.quartzlib.tools.items.ItemStackBuilder;
import fr.zcraft.MultipleInventories.quartzlib.tools.reflection.NMSException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A snapshot of an ItemStack when it was taken. The snapshot is frozen in time,
 * and cannot be modified.
 */
public class ItemStackSnapshot
{
    private final Material id;
    private final short durability;
    private final int amount;
    private final Map<String, Object> nbt;


    /**
     * Creates a snapshot of an {@link ItemStack}.
     *
     * You should use {@link #snap(ItemStack)} instead.
     *
     * @param id         The item's material
     * @param durability The item's durability.
     * @param amount     The amount of items in the stack.
     * @param nbt        The NBT tags in the stack. May be {@code null}.
     *
     * @see #snap(ItemStack) Easier method to create a snapshot that you should
     * use.
     */
    public ItemStackSnapshot(final Material id, final short durability, final int amount, final NBTCompound nbt)
    {
        this(id, durability, amount, nbt != null ? nbt.toHashMap() : null);
    }

    /**
     * Creates a snapshot of an {@link ItemStack}.
     *
     * You should use {@link #snap(ItemStack)} instead.
     *
     * @param id         The item's material
     * @param durability The item's durability.
     * @param amount     The amount of items in the stack.
     * @param nbt        The NBT tags in the stack. May be {@code null}.
     *
     * @see #snap(ItemStack) Easier method to create a snapshot that you should
     * use.
     */
    public ItemStackSnapshot(final Material id, final short durability, final int amount, final Map<String, Object> nbt)
    {
        this.id = id;
        this.durability = durability;
        this.amount = amount;
        this.nbt = nbt;
    }

    /**
     * Creates a snapshot of the given item.
     *
     * @param stack The ItemStack to create a snapshot of.
     *
     * @return The snapshot, or {@code null} if the item was {@code null} or if
     * an error occurred.
     */
    public static ItemStackSnapshot snap(final ItemStack stack)
    {
        if (stack == null) return null;

        try
        {
            return new ItemStackSnapshot(
                    stack.getType(),
                    getDurabilityRemainingForItemStack(stack), stack.getAmount(),
                    NBT.fromItemStack(stack).toHashMap()
            );
        }
        catch (NMSException e)
        {
            PluginLogger.error("Unable to extract NBT data from item {0}", e, stack.getType());
            return null;
        }
    }

    public static void setDurabilityRemainingForItemStack(ItemStack item, short durabilityRemaining) {
        if (item.getItemMeta() instanceof Damageable) {
            Damageable dmg = (Damageable) item.getItemMeta();
            dmg.setDamage(item.getType().getMaxDurability() - durabilityRemaining);
        }
    }

    public static short getDurabilityRemainingForItemStack(ItemStack item) {
        short durabilityRemaining = item.getType().getMaxDurability();
        if (item.getItemMeta() instanceof Damageable) {
            Damageable dmg = (Damageable) item.getItemMeta();
            durabilityRemaining = (short) (item.getType().getMaxDurability() - dmg.getDamage());
        }
        return durabilityRemaining;
    }

    /**
     * Reconstructs an ItemStack from this snapshot.
     *
     * @return A new ItemStack reconstructed from this snapshot.
     */
    public ItemStack reconstruct()
    {
        return new ItemStackBuilder(id).withMeta((ItemStack item) -> setDurabilityRemainingForItemStack(item, durability)).amount(amount).nbt(nbt).replaceNBT().craftItem();
    }


    /**
     * @return A JSON representation of this snapshot, usable as export.
     */
    @Override
    public String toString()
    {
        return toJSONString();
    }

    /**
     * @return A JSON representation of this snapshot, usable as export.
     */
    public JsonElement toJSON()
    {
        final JsonObject dump = new JsonObject();

        dump.addProperty("id", id.toString());
        dump.addProperty("Damage", durability);
        dump.addProperty("Count", amount);

        try
        {
            dump.add("NBT", MultipleInventories.GSON.toJsonTree(nbt));
        }
        catch (final JsonSyntaxException e)
        {
            PluginLogger.error("Invalid NBT JSON string: {0}", e, nbt);
        }

        return dump;
    }

    /**
     * @return A JSON representation of this snapshot, usable as export.
     */
    public String toJSONString()
    {
        return MultipleInventories.GSON.toJson(toJSON());
    }

    /**
     * Loads a snapshot from a JSON export.
     *
     * @param json The JSON data.
     *
     * @return A snapshot with these data inside.
     */
    public static ItemStackSnapshot fromJSONString(final String json)
    {
        return fromJSON((new JsonParser().parse(json)).getAsJsonObject());
    }

    /**
     * Loads a snapshot from a JSON export.
     *
     * @param json The JSON data.
     *
     * @return A snapshot with these data inside.
     */
    @SuppressWarnings ("unchecked")
    public static ItemStackSnapshot fromJSON(final JsonObject json)
    {
        try
        {
            return new ItemStackSnapshot(
                    Material.getMaterial(json.getAsJsonPrimitive("id").getAsString()),
                    json.getAsJsonPrimitive("Damage").getAsShort(),
                    json.getAsJsonPrimitive("Count").getAsInt(),
                    jsonToNative(json.getAsJsonObject("NBT"))
            );
        }
        catch (IllegalStateException e)
        {
            PluginLogger.error("Unable to load malformed item stack snapshot: {0}", e, json.toString());
            return null;
        }
    }

    /**
     * From a JSON object, constructs a {@link Map Map&lt;String, Object&gt;} representing
     * the same structure (recursively) using native types.
     *
     * <p>GSON could have been used to achieve this, using something like</p>
     * <pre>
     *     (Map<String, Object>) GSON.fromJson(
     *         json,
     *         new TypeToken<HashMap<String, Object>>() {}.getType()
     *     )
     * </pre>
     * <p>…but if used this way, it loses precision on all big numbers
     * (e.g. {@code -4823875203713330821} become {@code -4823875203713331200}…).
     * The ultimate precision on such big integers/longs s <strong>critical</strong>
     * for us, as NBT data frequently holds UUID stored using two longs of a similar
     * size of the example below.</p>
     *
     * <p>We had to re-implement this to ensure the generated structure to have the
     * right data type (instead of all numbers being doubles) and precision.</p>
     *
     * @param json The json object to be decoded.
     * @return {@link Map Map&lt;String, Object&gt;} representing the same structure
     * (recursively) using native types.
     *
     * @see #jsonToNative(JsonElement) Converts any json element (including objects)
     * to a native data structure.
     */
    private static Map<String, Object> jsonToNative(final JsonObject json)
    {
        final Map<String, Object> nativeMap = new HashMap<>();

        json.entrySet().forEach(entry ->
        {
            final Object nativeValue = jsonToNative(entry.getValue());

            if (nativeValue != null)
            {
                nativeMap.put(entry.getKey(), nativeValue);
            }
        });

        return nativeMap;
    }

    /**
     * From a JSON element, constructs a data structure representing
     * the same structure (recursively) using native types.
     *
     * <p>We had to re-implement this to ensure the generated structure to have the
     * right data type (instead of all numbers being doubles) and precision.</p>
     *
     * @param element The json element to be decoded.
     * @return A native data structure (either a {@link Map Map&lt;String, Object&gt;},
     * a {@link List List&lt;Object&gt;}, or a native type) representing the same
     * structure (recursively).
     *
     * @see #jsonToNative(JsonObject) Converts a json object to an explicit {@link Map}.
     * The JavaDoc also contains explainations on why this is needed.
     */
    private static Object jsonToNative(final JsonElement element)
    {
        if (element.isJsonObject())
        {
            return jsonToNative(element.getAsJsonObject());
        }
        else if (element.isJsonArray())
        {
            final List<Object> list = new ArrayList<>();

            element.getAsJsonArray().forEach(listElement ->
            {
                final Object nativeValue = jsonToNative(listElement);

                if (nativeValue != null)
                {
                    list.add(nativeValue);
                }
            });

            return list;
        }
        else if (element.isJsonPrimitive())
        {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();

            if (primitive.isBoolean())
            {
                return primitive.getAsBoolean();
            }
            else if (primitive.isString())
            {
                return primitive.getAsString();
            }
            else /* it's a number… we yet have to find the type. */
            {
                final BigDecimal number = primitive.getAsBigDecimal();

                try
                {
                    return number.byteValueExact();
                }
                catch (final ArithmeticException e1)
                {
                    try
                    {
                        return number.shortValueExact();
                    }
                    catch (final ArithmeticException e2)
                    {
                        try
                        {
                            return number.intValueExact();
                        }
                        catch (final ArithmeticException e3)
                        {
                            try
                            {
                                return number.longValueExact();
                            }
                            catch (final ArithmeticException e4)
                            {
                                try
                                {
                                    return number.doubleValue();
                                }
                                catch (final ArithmeticException | NumberFormatException e5)
                                {
                                    return number;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Else the element is null.
        return null;
    }
}
