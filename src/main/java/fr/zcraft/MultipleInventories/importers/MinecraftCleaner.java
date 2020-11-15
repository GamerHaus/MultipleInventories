package fr.zcraft.MultipleInventories.importers;

import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;


/**
 * This class provides utilities to "fix" Minecraft memory leaks that will arise during importation
 */
public class MinecraftCleaner
{
    static private Class criterionTrigger;
    static private Method criterionTriggerGetIterable;

    static private boolean hasFailed;

    static private Iterable getAllTriggers() throws ClassNotFoundException, InvocationTargetException, IllegalAccessException
    {
        if (criterionTrigger == null)
        {
            criterionTrigger = Reflection.getMinecraftClassByName("CriterionTriggers");
            criterionTriggerGetIterable = Reflection.findMethod(criterionTrigger, null, Iterable.class, Modifier.STATIC);
        }
        return (Iterable) criterionTriggerGetIterable.invoke(null);
    }

    static private Map findCriterionMap(final Object o) throws NoSuchFieldException, IllegalAccessException
    {
        final Field f = Reflection.getField(o.getClass(), Map.class);
        f.setAccessible(true);
        return (Map) f.get(o);
    }

    static public void cleanup()
    {
        if (hasFailed) return;

        try
        {
            for (Object o : getAllTriggers())
            {
                try
                {
                    findCriterionMap(o).clear();
                }
                catch (NoSuchFieldException ignored) {}
            }
        }
        catch (Exception e)
        {
            PluginLogger.error("Failed to cleanup Minecraft Leaked memory", e);
            hasFailed = true;
        }
    }
}
