package NekoBridging.utils;

import org.bukkit.entity.Entity;
import java.lang.reflect.Method;

public class NoAIUtils {
    private static boolean works = false;
    private static Class<?> craftEntityClass = null;
    private static Method getHandleMethod = null;
    private static Class<?> nmsEntityClass = null;
    private static Class<?> nbtTagCompoundClass = null;
    private static Method setIntMethod = null;
    private static Method saveNBTTagMethod = null;
    private static Method loadNBTTagMethod = null;

    static {
        try {
            String nmsver = org.bukkit.Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            craftEntityClass = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftEntity");
            nmsEntityClass = Class.forName("net.minecraft.server." + nmsver + ".Entity");
            nbtTagCompoundClass = Class.forName("net.minecraft.server." + nmsver + ".NBTTagCompound");

            getHandleMethod = craftEntityClass.getDeclaredMethod("getHandle");

            // Try to get NBT-related methods for 1.12.2
            boolean methodsFound = false;
            try {
                saveNBTTagMethod = nmsEntityClass.getDeclaredMethod("save", nbtTagCompoundClass);
                loadNBTTagMethod = nmsEntityClass.getDeclaredMethod("load", nbtTagCompoundClass);
                setIntMethod = nbtTagCompoundClass.getDeclaredMethod("setInt", String.class, int.class);
                methodsFound = true;
            } catch (Exception e1) {
                // If above methods don't exist, try other possible names in 1.12.2
                try {
                    saveNBTTagMethod = nmsEntityClass.getDeclaredMethod("f", nbtTagCompoundClass);
                    loadNBTTagMethod = nmsEntityClass.getDeclaredMethod("a", nbtTagCompoundClass);
                    setIntMethod = nbtTagCompoundClass.getDeclaredMethod("a", String.class, int.class);
                    methodsFound = true;
                } catch (Exception e2) {
                    System.out.println("Warning: Could not find required NBT methods for NoAIUtils");
                }
            }
            
            if (methodsFound) {
                works = true;
            }
        } catch (Exception e) {
            System.out.println("Warning: Failed to initialize NoAIUtils: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void setAI(Entity bukkitEntity, boolean hasAI) {
        if (!works) {
            return;
        }

        try {
            // Get NMS entity
            Object nmsEntity = getHandleMethod.invoke(bukkitEntity);

            // Create NBT tag
            Object nbtTag = nbtTagCompoundClass.getConstructor().newInstance();

            // Read entity's NBT data
            if (saveNBTTagMethod != null) {
                saveNBTTagMethod.invoke(nmsEntity, nbtTag);
            }

            // Set NoAI tag
            if (setIntMethod != null) {
                setIntMethod.invoke(nbtTag, "NoAI", hasAI ? 0 : 1);
            }

            // Write modified NBT data back to entity
            if (loadNBTTagMethod != null) {
                loadNBTTagMethod.invoke(nmsEntity, nbtTag);
            }
        } catch (Exception e) {
            // Silently fail to avoid spamming server logs
        }
    }
}