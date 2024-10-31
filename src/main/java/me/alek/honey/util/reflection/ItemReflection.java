package me.alek.honey.util.reflection;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ItemReflection {

    private static final Class<?> itemStackClass = Reflection.getUnsureClass("{nms}.ItemStack", "{nms}.world.item.ItemStack");
    private static final Class<?> craftItemStackClass = Reflection.getClass("{obc}.inventory.CraftItemStack");
    private static final ConstructorInvoker<?> itemStackConstructor;

    private static final MethodInvoker<?> nmsCopyInvoker = Reflection.getMethod(craftItemStackClass, "asNMSCopy", ItemStack.class);
    private static final MethodInvoker<?> craftMirrorInvoker = Reflection.getMethod(craftItemStackClass, "asCraftMirror", itemStackClass);
    private static final MethodInvoker<ItemStack> bukkitCopyInvoker = Reflection.getMethod(craftItemStackClass, "asBukkitCopy", itemStackClass);
    private static final MethodInvoker<?> itemGetByIdInvoker = Reflection.getMethod(NMS.getItemClass(), method -> {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_17_1)) {
            return method.getName().equals("getById");
        } else {
            return method.getReturnType().equals(NMS.getItemClass()) && Arrays.equals(method.getParameterTypes(), new Class[]{int.class});
        }
    });
    private static final MethodInvoker<Material> materialGetByItemInvoker = Reflection.getMethod(NMS.getMagicNumbersUtilClass(), method -> {
        return method.getReturnType().equals(Material.class) && Arrays.equals(method.getParameterTypes(), new Class[]{NMS.getItemClass()});
    });
    private static final MethodInvoker<Void> craftItemSetMeta = Reflection.getMethod(NMS.getCraftItemStackClass(), "setItemMeta", NMS.getItemStackClass(), ItemMeta.class);
    private static final MethodInvoker<ItemMeta> craftItemGetMeta = Reflection.getMethod(NMS.getCraftItemStackClass(), "getItemMeta", NMS.getItemStackClass());

    private static final AtomicReference<FieldAccessor<List<Object>>> bookMetaPageCache = new AtomicReference<>();

    private static final Gson CHAT_SERIALIZER_GSON;

    static {
        ConstructorInvoker<?> invoker;
        try {
            invoker = Reflection.getConstructor(itemStackClass, NMS.getItemClass(), int.class, int.class);
        } catch (Exception ex) {
            invoker = Reflection.getConstructor(itemStackClass, NMS.getIMaterialClass(), int.class);
        }
        itemStackConstructor = invoker;

        CHAT_SERIALIZER_GSON = ReflectStructure.Provider.acquireStructure(NMS.getChatSerializerClass())
                .withType(Gson.class)
                .withTarget(null)
                .readField(0);
    }

    public static Class<?> getNMSItemStackClass() {
        return itemStackClass;
    }

    public static Class<?> getCraftItemStackClass() {
        return craftItemStackClass;
    }

    public static JsonElement chatComponentToJson(Object component) {
        return CHAT_SERIALIZER_GSON.toJsonTree(component);
    }

    public static List<Object> getPages(AtomicReference<IStructureModifier<List<Object>>> reference, BookMeta meta) {
        IStructureModifier<List<Object>> bookMetaPageModifier = ReflectStructure.Provider.acquireStructure(meta.getClass())
                .<List<Object>>withWeakType(List.class)
                .withTarget(meta);
        reference.set(bookMetaPageModifier);

        return bookMetaPageModifier.readField(bookMetaPageCache, "pages", 0);
    }

    public static ItemStack asBukkitCopy(Object itemStack) {
        if (itemStack != null) {
            try {
                if (itemStackClass.isInstance(itemStack)) {
                    return bukkitCopyInvoker.invokeWithException(null, itemStack);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return new ItemStack(Material.AIR);
    }

    public static Object asCraftMirror(Object itemStack) {
        if (itemStack != null) {
            try {
                if (itemStackClass.isInstance(itemStack)) {
                    return craftMirrorInvoker.invokeWithException(null, itemStack);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    public static Object asNMSCopy(ItemStack item) {
        if (item != null && Arrays.equals(nmsCopyInvoker.getHandle().getParameterTypes(), new Class[]{ItemStack.class})) {
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
            IStructureModifier<?> structure = ReflectStructure.Provider.acquireStructure(craftItemStackClass)
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

    public static Material getMaterialFromItem(Object item) {
        return materialGetByItemInvoker.invoke(null, item);
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
        ReflectStructure.Provider.acquireStructure(NMS.getItemClass())
                .withType(NMS.getNBTTagCompoundClass()).withTarget(item)
                .writeField(0, tag);
    }

    public static void setItemMeta(Object item) {
        ItemMeta meta = craftItemGetMeta.invoke(null, item);
        craftItemSetMeta.invoke(null, item, meta);
    }
}
