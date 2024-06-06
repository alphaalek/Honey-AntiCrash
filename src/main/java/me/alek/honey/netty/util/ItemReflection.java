package me.alek.honey.netty.util;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructureCache;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class ItemReflection {

    private static final Class<?> itemStackClass = Reflection.getFuzzyClass("{nms}.ItemStack", "{nms}.world.item.ItemStack");
    private static final Class<?> craftItemStackClass = Reflection.getClass("{obc}.inventory.CraftItemStack");
    private static ConstructorInvoker<?> itemStackConstructor;

    private static final MethodInvoker<?> bukkitCopyInvoker = Reflection.getMethod(craftItemStackClass, "asBukkitCopy", itemStackClass);
    private static final MethodInvoker<?> nmsCopyInvoker = Reflection.getMethod(craftItemStackClass, "asNMSCopy", ItemStack.class);
    private static final MethodInvoker<?> itemGetByIdInvoker = Reflection.getMethod(NMS.getItemClass(), (method) -> {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_17_1)) {
            return method.getName().equals("getById");
        }
        return method.getReturnType().equals(NMS.getItemClass()) && Arrays.equals(method.getParameterTypes(), new Class[]{int.class});
    });

    static {
        try {
            itemStackConstructor = Reflection.getConstructor(itemStackClass, NMS.getItemClass(), int.class, int.class);
        } catch (Exception ex) {
            itemStackConstructor = Reflection.getConstructor(itemStackClass, NMS.getIMaterialClass(), int.class);
        }
    }

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

    public static Object getItemFromId(int id) {
        return itemGetByIdInvoker.invoke(null, id);
    }

    public static Object createItemStack(Object item, byte amount, short damage) {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_12_2)) {
            return createItemStackLegacy(item, amount, damage);
        } else {
            return createItemStackModern(item, amount);
        }
    }

    public static Object createItemStackLegacy(Object item, byte amount, short damage) {
        return itemStackConstructor.invoke(item, amount, damage);
    }

    public static Object createItemStackModern(Object item, byte amount) {
        return itemStackConstructor.invoke(item, amount);
    }

    public static boolean isNMSItem(Object item) {
        return itemStackClass.isInstance(item);
    }

    public static void setItemTag(Object item, Object tag) {
        ReflectStructureCache.acquireStructure(NMS.getItemClass())
                .withType(NMS.getNBTTagCompoundClass()).withTarget(item)
                .writeField(0, tag);
    }
}
