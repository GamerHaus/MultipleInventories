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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import fr.zcraft.zlib.components.nbt.NBT;
import fr.zcraft.zlib.components.nbt.NBTCompound;
import fr.zcraft.zlib.tools.PluginLogger;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import fr.zcraft.zlib.tools.reflection.NMSException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;


/**
 * A snapshot of an ItemStack when it was taken. The snapshot is frozen in time,
 * and cannot be modified.
 */
public class ItemStackSnapshot
{
    private static final Gson GSON = new Gson();

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
                    stack.getDurability(), stack.getAmount(),
                    NBT.fromItemStack(stack).toHashMap()
            );
        }
        catch (NMSException e)
        {
            PluginLogger.error("Unable to extract NBT data from item {0}", e, stack.getType());
            return null;
        }
    }

    /**
     * Reconstructs an ItemStack from this snapshot.
     *
     * @return A new ItemStack reconstructed from this snapshot.
     */
    public ItemStack reconstruct()
    {
        return new ItemStackBuilder(id).data(durability).amount(amount).nbt(nbt)
                .item();
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
            dump.add("NBT", GSON.toJsonTree(nbt));
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
        return GSON.toJson(toJSON());
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
        return fromJSON(GSON.fromJson(json, JsonObject.class));
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
        return new ItemStackSnapshot(
                Material.getMaterial(json.getAsJsonPrimitive("id").getAsString()),
                json.getAsJsonPrimitive("Damage").getAsShort(),
                json.getAsJsonPrimitive("Count").getAsInt(),
                (Map<String, Object>) GSON.fromJson(json.getAsJsonObject("NBT"), new TypeToken<HashMap<String, Object>>() {}.getType())
        );
    }
}
