package me.alek.honey.netty.utility.reflect;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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


    public static ItemStack asBukkitItem(Object itemStack) {
        if (itemStack != null) {
            try {
                if (itemStackClass.isInstance(itemStack)) {
                    return (ItemStack) bukkitCopyInvoker.invoke(null, itemStack);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new ItemStack(Material.AIR);
    }

    public static Object asNMSItem(ItemStack item) {
        if (item != null) {

            try {
                Object nmsItem = nmsCopyInvoker.invoke(null, item);
                if (isNMSItem(nmsItem)) {
                    return nmsItem;
                }
            } catch (Exception ex) {
            }
        }
        return null;
    }

    public static boolean isNMSItem(Object item) {
        return itemStackClass.isInstance(item);
    }

}
