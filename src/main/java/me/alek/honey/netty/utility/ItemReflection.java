package me.alek.honey.netty.utility;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructureCache;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ItemReflection {

    private static final Class<?> itemStackClass = Reflection.getFuzzyClass("{nms}.ItemStack", "{nms}.world.item.ItemStack");
    private static final Class<?> craftItemStackClass = Reflection.getClass("{obc}.inventory.CraftItemStack");

    private static final MethodInvoker<?> bukkitCopyInvoker = Reflection.getMethod(craftItemStackClass, "asBukkitCopy", itemStackClass);
    private static final MethodInvoker<?> nmsCopyInvoker = Reflection.getMethod(craftItemStackClass, "asNMSCopy", ItemStack.class);

    public static Class<?> getNMSItemStackClass() {
        return itemStackClass;
    }

    public static Class<?> getCraftItemStackClass() {
        return craftItemStackClass;
    }


    public static ItemStack asBukkitCopy(Object itemStack) {
        if (itemStack != null) {
            try {
                if (itemStackClass.isInstance(itemStack)) {
                    return (ItemStack) bukkitCopyInvoker.invokeWithException(null, itemStack);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new ItemStack(Material.AIR);
    }

    public static Object asNMSCopy(ItemStack item) {
        if (item != null && Arrays.equals(nmsCopyInvoker.getMethod().getParameterTypes(), new Class[]{ItemStack.class})) {

            try {
                Object nmsItem = nmsCopyInvoker.invokeWithException(null, item);
                if (isNMSItem(nmsItem)) {
                    return nmsItem;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static Object getHandleItem(ItemStack item) {
        if (craftItemStackClass.isInstance(item)) {
            IStructureModifier<?> structure = ReflectStructureCache.acquireStructure(craftItemStackClass)
                    .withType(itemStackClass)
                    .withTarget(item);

            Object handle = structure.readField(0);
            if (handle != null) {
                return handle;
            }
        }
        return ItemReflection.asNMSCopy(item);
    }

    public static boolean isNMSItem(Object item) {
        return itemStackClass.isInstance(item);
    }

}
