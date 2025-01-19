package me.alek.honey.util.reflection;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.alek.honey.netty.structure.IStructureModifier;
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


    private static final ConstructorInvoker<?> itemStackConstructor;

    private static final MethodInvoker<?> nmsCopyInvoker = Reflection.getMethod(NMS.getCraftItemStackClass(), "asNMSCopy", ItemStack.class);
    private static final MethodInvoker<ItemStack> craftMirrorInvoker = Reflection.getMethod(NMS.getCraftItemStackClass(), "asCraftMirror", NMS.getItemStackClass());
    private static final MethodInvoker<ItemStack> bukkitCopyInvoker = Reflection.getMethod(NMS.getCraftItemStackClass(), "asBukkitCopy", NMS.getItemStackClass());
    private static final MethodInvoker<ItemMeta> createItemMetaInvoker = Reflection.getMethod(NMS.getCraftItemStackClass(), "getItemMeta", NMS.getItemStackClass());
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

    private static final AtomicReference<FieldAccessor<List<Object>>> bookMetaPageCache = new AtomicReference<>();

    private static final Gson CHAT_SERIALIZER_GSON;

    static {
        ConstructorInvoker<?> invoker;
        try {
            invoker = Reflection.getConstructor(NMS.getItemStackClass(), NMS.getItemClass(), int.class, int.class);
        } catch (Exception ex) {
            invoker = Reflection.getConstructor(NMS.getItemStackClass(), NMS.getIMaterialClass(), int.class);
        }
        itemStackConstructor = invoker;

        CHAT_SERIALIZER_GSON = ReflectStructure.Provider.acquireStructure(NMS.getChatSerializerClass())
                .withType(Gson.class)
                .withTarget(null)
                .readField(0);
    }

    public static JsonElement chatComponentToJson(Object component) {
        return CHAT_SERIALIZER_GSON.toJsonTree(component);
    }

    public static List<Object> getPages(BookMeta meta) {
        IStructureModifier<List<Object>> bookMetaPageModifier = ReflectStructure.Provider.acquireStructure(meta.getClass())
                .<List<Object>>withWeakTypeWithoutCache(List.class)
                .withSuperClass(false)
                .withTarget(meta);

        return bookMetaPageModifier.readField(bookMetaPageCache, "pages", 0);
    }

    public static void setPages(BookMeta meta, List<Object> pages) {
        IStructureModifier<List<Object>> bookMetaPageModifier = ReflectStructure.Provider.acquireStructure(meta.getClass())
                .<List<Object>>withWeakTypeWithoutCache(List.class)
                .withSuperClass(false)
                .withTarget(meta);

        bookMetaPageModifier.writeField(bookMetaPageCache, "pages", 0, pages);
    }

    public static ItemStack asBukkitCopy(Object itemStack) {
        if (itemStack != null) {
            try {
                if (NMS.getItemStackClass().isInstance(itemStack)) {
                    return bukkitCopyInvoker.invokeWithException(null, itemStack);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return new ItemStack(Material.AIR);
    }

    public static ItemStack asCraftMirror(Object itemStack) {
        if (itemStack != null) {
            try {
                if (NMS.getItemStackClass().isInstance(itemStack)) {
                    return craftMirrorInvoker.invokeWithException(null, itemStack);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return new ItemStack(Material.AIR);
    }

    public static Object asNMSCopy(ItemStack item) {
        if (item != null) {
            try {
                return nmsCopyInvoker.invokeWithException(null, item);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }

    public static Object getHandleItem(ItemStack item) {
        if (NMS.getCraftItemStackClass().isInstance(item)) {
            IStructureModifier<?> structure = ReflectStructure.Provider.acquireStructure(NMS.getCraftItemStackClass())
                    .withType(NMS.getItemStackClass())
                    .withTarget(item);

            Object handle = structure.readField(0);
            if (handle != null) {
                return handle;
            }
        }

        return ItemReflection.asNMSCopy(item);
    }

    public static Material getMaterialFromId(int id) {
        return getMaterialFromItem(getItemFromId(id));
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
        return NMS.getItemStackClass().isInstance(item);
    }

    public static boolean isCraftItem(Object item) {
        return NMS.getCraftItemStackClass().isInstance(item);
    }

    public static void setItemTag(Object item, Object tag) {
        ReflectStructure.Provider.acquireStructure(NMS.getItemStackClass())
                .withType(NMS.getNBTTagCompoundClass()).withTarget(item)
                .writeField(0, tag);
    }

    public static Object getItemTag(Object item) {
        return ReflectStructure.Provider.acquireStructure(NMS.getItemStackClass())
                .withType(NMS.getNBTTagCompoundClass()).withTarget(item)
                .readField(0);
    }

    public static void setItemMeta(Object item) {
        craftItemSetMeta.invoke(null, item, createItemMeta(item));
    }

    public static ItemMeta createItemMeta(Object item) {
        return createItemMetaInvoker.invoke(null, item);
    }
}
