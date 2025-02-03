package me.alek.honey.util.reflection;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class ItemFactory {

    private static final ConstructorInvoker<?> itemStackConstructor;

    private static final MethodInvoker<?> nmsCopyInvoker = Reflection.getMethod(MinecraftReflection.getCraftItemStackClass(), "asNMSCopy", ItemStack.class);
    private static final MethodInvoker<ItemStack> craftMirrorInvoker = Reflection.getMethod(MinecraftReflection.getCraftItemStackClass(), "asCraftMirror", MinecraftReflection.getItemStackClass());
    private static final MethodInvoker<ItemStack> bukkitCopyInvoker = Reflection.getMethod(MinecraftReflection.getCraftItemStackClass(), "asBukkitCopy", MinecraftReflection.getItemStackClass());
    private static final MethodInvoker<ItemMeta> createItemMetaInvoker = Reflection.getMethod(MinecraftReflection.getCraftItemStackClass(), "getItemMeta", MinecraftReflection.getItemStackClass());
    private static final MethodInvoker<?> itemGetByIdInvoker = Reflection.getMethod(MinecraftReflection.getItemClass(), method -> {
        if (NetProtocol.getProtocol().isOlderThanOrEqual(NetProtocol.v1_17_1)) {
            return method.getName().equals("getById");
        } else {
            return method.getReturnType().equals(MinecraftReflection.getItemClass()) && Arrays.equals(method.getParameterTypes(), new Class[]{int.class});
        }
    });
    private static final MethodInvoker<Material> materialGetByItemInvoker = Reflection.getMethod(MinecraftReflection.getMagicNumbersUtilClass(), method -> {
        return method.getReturnType().equals(Material.class) && Arrays.equals(method.getParameterTypes(), new Class[]{MinecraftReflection.getItemClass()});
    });
    private static final MethodInvoker<Void> craftItemSetMeta = Reflection.getMethod(MinecraftReflection.getCraftItemStackClass(), "setItemMeta", MinecraftReflection.getItemStackClass(), ItemMeta.class);

    private static final Gson CHAT_SERIALIZER_GSON;

    static {
        ConstructorInvoker<?> invoker;
        try {
            invoker = Reflection.getConstructor(MinecraftReflection.getItemStackClass(), MinecraftReflection.getItemClass(), int.class, int.class);
        } catch (Exception ex) {
            invoker = Reflection.getConstructor(MinecraftReflection.getItemStackClass(), MinecraftReflection.getIMaterialClass(), int.class);
        }
        itemStackConstructor = invoker;

        CHAT_SERIALIZER_GSON = ReflectStructure.Provider.acquireStructure(MinecraftReflection.getChatSerializerClass())
                .withType(Gson.class)
                .withTarget(null)
                .readField(0);
    }

    public static JsonElement chatComponentToJson(Object component) {
        return CHAT_SERIALIZER_GSON.toJsonTree(component);
    }

    public static ItemStack asBukkitCopy(Object itemStack) throws Exception {
        return bukkitCopyInvoker.invokeWithException(null, itemStack);
    }

    public static ItemStack asCraftMirror(Object itemStack) throws Exception {
        return craftMirrorInvoker.invokeWithException(null, itemStack);
    }

    public static Object asNMSCopy(ItemStack item) throws Exception {
        return nmsCopyInvoker.invokeWithException(null, item);
    }

    public static ItemStack createLiteralItem(ItemStack item) throws Exception {
        return asCraftMirror(asNMSCopy(item));
    }

    public static Object getHandleItem(ItemStack item) {
        if (MinecraftReflection.getCraftItemStackClass().isInstance(item)) {
            IStructureModifier<?> structure = ReflectStructure.Provider.acquireStructure(MinecraftReflection.getCraftItemStackClass())
                    .withType(MinecraftReflection.getItemStackClass())
                    .withTarget(item);

            Object handle = structure.readField(0);
            if (handle != null) {
                return handle;
            }
        }

        try {
            return ItemFactory.asNMSCopy(item);
        } catch (Exception ex) {
            return null;
        }
    }

    public static Material getMaterial(ItemType type) {
        return getMaterialFromId(type.getId(NetProtocol.getProtocol().toPECVersion()));
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
        return MinecraftReflection.getItemStackClass().isInstance(item);
    }

    public static boolean isCraftItem(Object item) {
        return MinecraftReflection.getCraftItemStackClass().isInstance(item);
    }

    public static void setItemTag(ItemStack item, ItemTag tag) {
        setItemTag(ItemFactory.getHandleItem(item), tag);
    }

    public static void setItemTag(Object item, ItemTag tag) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            // TODO: fix
        } else {
            ReflectStructure.Provider.acquireStructure(MinecraftReflection.getItemStackClass())
                    .withType(MinecraftReflection.getNBTTagCompoundClass()).withTarget(item)
                    .writeField(0, tag.asNBT().get().getHandle());
        }
    }

    public static void setItemMeta(Object item) {
        craftItemSetMeta.invoke(null, item, createItemMeta(item));
    }

    public static ItemMeta createItemMeta(Object item) {
        return createItemMetaInvoker.invoke(null, item);
    }
}
