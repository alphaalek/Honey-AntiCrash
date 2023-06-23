package me.alek.honey.helper;

import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.Reflection;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemHelper {

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

    public static ItemStack asBukkitItem(Object item) {
        if (item != null) {

            try {
                if (itemStackClass.isInstance(item)) {
                    Object bukkitItem = bukkitCopyInvoker.invoke(null, item);
                    if (bukkitItem instanceof ItemStack) {
                        return (ItemStack) bukkitItem;
                    }
                }
            } catch (Exception ex) {
            }
        }
        return null;
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

    public static boolean isBukkitItem(Object item) {
        return item instanceof ItemStack;
    }

    public static boolean isContainer(ItemStack item) {
        if (item == null) {
            return false;
        }
        String name = item.getType().name();
        return name.equals("chest") || name.equals("hopper") || name.equals("shulker");
    }

    public static boolean isBook(ItemStack item) {
        if (item == null) {
            return false;
        }
        return item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.BOOK_AND_QUILL;
    }

    public static boolean isSkull(ItemStack item) {
        if (item == null) {
            return false;
        }
        return item.getType() == Material.SKULL || item.getType() == Material.SKULL_ITEM;
    }


}
