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
import fr.zcraft.zlib.components.nbt.NBT;
import fr.zcraft.zlib.components.nbt.NBTCompound;
import fr.zcraft.zlib.tools.PluginLogger;
import fr.zcraft.zlib.tools.reflection.NMSException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class ItemStackSnapshot
{
    private static final Gson GSON = new Gson();

    private final Material id;
    private final float durability;
    private final int amount;
    private final NBTCompound nbt;


    public ItemStackSnapshot(Material data, int amount, float durability, NBTCompound nbt)
    {
        this.id = data;
        this.durability = durability;
        this.amount = amount;
        this.nbt = nbt;
    }

    public static ItemStackSnapshot snap(ItemStack stack)
    {
        if (stack == null) return null;

        try
        {
            return new ItemStackSnapshot(
                    stack.getType(),
                    stack.getAmount(),
                    stack.getDurability(),
                    NBT.fromItemStack(stack)
            );
        }
        catch (NMSException e)
        {
            PluginLogger.error("Unable to extract NBT data from item {0}", e, stack.getType());
            return null;
        }
    }

    public ItemStack reconstruct()
    {
        return new ItemStack(Material.AIR); // TODO
    }


    @Override
    public String toString()
    {
        return toJSONString();
    }

    public JsonElement toJSON()
    {
        final JsonObject dump = new JsonObject();

        dump.addProperty("id", id.toString());
        dump.addProperty("Damage", durability);
        dump.addProperty("Count", amount);
        try
        {
            dump.add("NBT", GSON.fromJson(nbt.toString(), JsonObject.class));
        }
        catch (final JsonSyntaxException e)
        {
            PluginLogger.error("Invalid NBT JSON string: {0}", e, nbt);
        }

        return dump;
    }

    public String toJSONString()
    {
        return GSON.toJson(toJSON());
    }
}
