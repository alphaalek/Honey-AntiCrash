package me.alek.honey.helper;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.MethodInvoker;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBTHelper {

    private static final Pattern URL_PATTERN = Pattern.compile("url");

    private static final Class<?> nbtTagCompoundClass = Reflection.getFuzzyClass("{nms}.NBTTagCompound", "{nms}.nbt.NBTTagCompound");
    private static final Class<?> nbtTagListClass = Reflection.getFuzzyClass("{nms}.NBTTagList", "{nms}.nbt.NBTTagList");
    private static final Class<?> nbtBaseClass = Reflection.getFuzzyClass("{nms}.NBTTagList", "{nms}.nbt.NBTTagList");
    private static final Class<?> nbtStringClass = Reflection.getFuzzyClass("{nms}.NBTTagString", "{nms}.nbt.NBTTagString");

    private static final MethodInvoker getCompoundInvoker = Reflection.getMethod(nbtTagCompoundClass, "getCompound", String.class);
    private static final MethodInvoker hasKeyInvoker = Reflection.getMethod(nbtTagCompoundClass, "hasKey", String.class);
    private static final MethodInvoker hasKeyOfTypeInvoker = Reflection.getMethod(nbtTagCompoundClass, "hasKeyOfType", String.class, Integer.class);
    private static final MethodInvoker getListInvoker = Reflection.getMethod(nbtTagCompoundClass, "getList", String.class, Integer.class);
    private static final MethodInvoker removeTagInvoker = Reflection.getMethod(nbtTagCompoundClass, "remove", String.class);
    private static final MethodInvoker getKeysInvoker = Reflection.getMethodFromReturnType(nbtTagCompoundClass, 0, Set.class);
    private static final FieldAccessor<Object> mapAccessor = Reflection.getField(nbtTagCompoundClass, 0, Map.class);

    private static final MethodInvoker listGetSizeInvoker = Reflection.getMethod(nbtTagListClass, "size");
    private static final MethodInvoker listGetIndexInvoker = Reflection.getMethod(nbtTagListClass, "get", Integer.class);
    private static final FieldAccessor<ArrayList<Object>> listAccessor = Reflection.getField(nbtTagListClass, 0, List.class);

    private static final FieldAccessor<String> dataAccessor = Reflection.getField(nbtStringClass, 0, String.class);
    private static final MethodInvoker getTypeIdInvoker = Reflection.getMethod(nbtBaseClass, "getTypeId");

    private static final MethodInvoker getTagCompoundInvoker = Reflection.getMethod(ItemHelper.getNMSItemStackClass(), "getTag");

    // nbt fra item
    public static Object getTag(ItemStack item) {
        return getTag(ItemHelper.asNMSItem(item));
    }

    // nbt fra item
    public static Object getTag(Object item) {
        if (item == null) {
            return null;
        }
        return getTagCompoundInvoker.invoke(item);
    }

    // tjekker om object er nbt compound
    public static boolean isNBTCompound(Object tag) {
        if (tag == null) {
            return false;
        }
        return nbtTagCompoundClass.isInstance(tag);
    }

    // får en nbt compound for den key i en nbt compound
    public static Object compoundGetCompound(Object tag, String key) {
        if (!isNBTCompound(tag)) {
            return null;
        }
        return getCompoundInvoker.invoke(tag, key);
    }

    // får en nbt list for den key og type id i en nbt compound
    public static Object compoundGetList(Object tag, String key, int type) {
        if (!isNBTCompound(tag)) {
            return null;
        }
        return getListInvoker.invoke(tag, key, type);
    }

    // får hvilken som helst nbt base for den key i en nbt compound
    public static Object compoundGetFuzzyBase(Object tag, String key) {
        if (!isNBTCompound(tag)) {
            return null;
        }
        Map<String, Object> map = compoundGetMap(tag);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return map.get(key);
    }

    // får en nbt list for den key i en nbt compound
    public static Object compoundGetFuzzyList(Object tag, String key) {
        Object list = compoundGetFuzzyBase(tag, key);
        Bukkit.broadcastMessage(list + "");
        if (list == null) {
            return null;
        }
        if (!isNBTList(list)) {
            return null;
        }
        return list;
    }

    // får map af key-value pair i en nbt compound
    public static Map<String, Object> compoundGetMap(Object tag) {
        if (!isNBTCompound(tag)) {
            return null;
        }
        Object map = mapAccessor.get(tag);
        if (map == null) {
            return null;
        }
        try {
            return (Map<String, Object>) map;
        } catch (ClassCastException ex) {
            return null;
        }
    }

    // tjekker om nbt compound har key
    public static boolean compoundHasKey(Object tag, String key) {
        if (!isNBTCompound(tag)) {
            return false;
        }
        try {
            return (boolean) hasKeyInvoker.invoke(tag, key);
        } catch (ClassCastException ex) {
            return false;
        }
    }

    // tjekker om nbt compound har key og type id
    public static boolean compoundHasKeyOfType(Object tag, String key, int type) {
        if (!isNBTCompound(tag)) {
            return false;
        }
        try {
            return (boolean) hasKeyOfTypeInvoker.invoke(tag, key, type);
        } catch (ClassCastException ex) {
            return false;
        }
    }

    // får keys tilsvarende alle nbt bases i en nbt compound
    public static Set<String> compoundGetKeys(Object tag) {
        if (!isNBTCompound(tag)) {
            return null;
        }
        try {
            return (Set<String>) getKeysInvoker.invoke(tag);
        } catch (ClassCastException ex) {
            return null;
        }
    }

    // fjerner nbt base tilsvarende key fra nbt compound
    public static void removeCompoundTag(Object compound, String tag) {
        if (!isNBTCompound(tag)) {
            return;
        }
        removeTagInvoker.invoke(compound, tag);
    }

    // tjekker om object er nbt list
    public static boolean isNBTList(Object list) {
        if (list == null) {
            return false;
        }
        return nbtTagListClass.isInstance(list);
    }

    // får længde af nbt list
    public static int getListSize(Object list) {
        if(!isNBTList(list)) {
            return 0;
        }
        try {
            return (int) listGetSizeInvoker.invoke(list);
        } catch (ClassCastException ex) {
            return 0;
        }
    }

    // får nbt compound for index i nbt list
    public static Object listGetIndexCompound(Object list, int index) {
        if (!isNBTList(list)) {
            return null;
        }
        return listGetIndexInvoker.invoke(list, index);
    }

    // får hvilken som helst nbt base for index i nbt list
    public static Object listGetIndexFuzzy(Object list, int index) {
        if (!isNBTList(list)) {
            return null;
        }
        Object internalList = listAccessor.get(list);
        if (internalList == null) {
            return null;
        }
        try {
            ArrayList<Object> array = (ArrayList<Object>) internalList;
            return array.get(index);
        } catch (ClassCastException ex) {
            return null;
        }
    }

    // tjekker om object er nbt base
    public static boolean isNBTBase(Object base) {
        return nbtBaseClass.isInstance(base) || nbtBaseClass.isAssignableFrom(base.getClass());
    }

    // får type id af nbt base
    public static int baseGetTypeId(Object base) {
        if (!isNBTBase(base)) {
            return -1;
        }
        try {
            return (int) getTypeIdInvoker.invoke(base);
        } catch (ClassCastException ex) {
            return -1;
        }
    }


    public static boolean checkNMSNBT(Object item) {
        if (item == null) {
            return false;
        }
        final ItemStack bukkitItem = ItemHelper.asBukkitItem(item);
        if (bukkitItem == null) {
            return false;
        }
        return checkBukkitNBT(bukkitItem);
    }

    public static int getNbtLength(Object tag) {
        return tag.toString().getBytes().length;
    }

    public static boolean checkBukkitNBT(ItemStack item) {
        if (item == null) {
            return false;
        }
        Object tag = getTag(item);
        if (tag == null) {
            return false;
        }
        int nbtLength = getNbtLength(tag);
        switch (item.getType()) {
            case FIREWORK: {
                if (nbtLength > ConfigurationOptions.NBT.MAX_FIREWORK_NBT_LENGTH) {
                    return true;
                }
                break;
            }
            case FIREWORK_CHARGE: {
                if (nbtLength > ConfigurationOptions.NBT.MAX_FIREWORK_CHARGE_NBT_LENGTH) {
                    return true;
                }
                break;
            }
            case WRITTEN_BOOK:
            case BOOK_AND_QUILL: {
                if (checkBook(tag, nbtLength)) {
                    clearBookTags(tag);
                    return true;
                }
                break;
            }
            default: {
                if (nbtLength > ConfigurationOptions.NBT.MAX_NBT_LENGTH) {
                    return true;
                }
            }
        }
        if (ItemHelper.isContainer(item)) {
            if (checkContainer(item)) {
                return true;
            }
        }
        if (ConfigurationOptions.NBT.CHECK_ILLEGAL_SKULL_TEXTURES) {
            if (ItemHelper.isSkull(item)) {
                if (checkSkull(tag)) {
                    return true;
                }
            }
        }
        if (checkBigNBT(tag)) {
            if (ItemHelper.isBook(item)) {
                clearBookTags(tag);
            }
            return true;
        }
        if (checkBadCharacter(item)) {
            return true;
        }
        return false;
    }

    public static boolean checkBadCharacter(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        final ItemMeta meta = item.getItemMeta();
        if (checkBadCharacter(item.getItemMeta().getDisplayName())) {
            return true;
        }
        if (meta.getLore() == null) {
            return false;
        }
        for (String line : item.getItemMeta().getLore()) {
            if (checkBadCharacter(line)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkBadCharacter(String str) {
        if (str == null) {
            return false;
        }
        for (char character : str.toCharArray()) {
            if (character == Character.MIN_VALUE) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkBigNBT(Object tag) {
        return checkCompoundNBT(tag, new AtomicInteger(), 0);
    }

    private static boolean checkCompoundNBT(Object tag, AtomicInteger totalBases, int nestingContext) {
        nestingContext++;
        if (checkValues(totalBases.get(), nestingContext)) {
            return true;
        }
        Set<String> keys = compoundGetKeys(tag);
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        for (final String key : keys) {
            if (checkValues(totalBases.incrementAndGet(), nestingContext)) {
                return true;
            }
            if (compoundHasKeyOfType(tag, key, 9)) {
                Object list = compoundGetFuzzyList(tag, key);
                if (list == null) {
                    continue;
                }
                if (checkListNBT(list, totalBases, nestingContext)) {
                    return true;
                }
                continue;
            }
            if (compoundHasKeyOfType(tag, key, 10)) {
                Object compound = compoundGetCompound(tag, key);
                if (compound == null) {
                    continue;
                }
                if (checkCompoundNBT(compound, totalBases, nestingContext)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkListNBT(Object list, AtomicInteger totalBases, int nestingContext) {
        nestingContext++;
        if (checkValues(totalBases.get(), nestingContext)) {
            return true;
        }
        int size = getListSize(list);
        for (int i = 0; i < size; i++) {
            if (checkValues(totalBases.incrementAndGet(), nestingContext)) {
                return true;
            }
            Object base = listGetIndexFuzzy(list, i);
            if (base == null) {
                continue;
            }
            if (baseGetTypeId(base) == 10) {
                if (checkCompoundNBT(base, totalBases, nestingContext)) {
                    return true;
                }
                continue;
            }
            if (baseGetTypeId(base) == 9) {
                if (checkListNBT(base, totalBases, nestingContext)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkValues(int totalBases, int nestingContext) {
        return totalBases > ConfigurationOptions.NBT.MAX_NBT_BASES || nestingContext > ConfigurationOptions.NBT.MAX_NBT_NESTING;
    }

    public static boolean checkSkull(Object nbt) {
        if (!compoundHasKey(nbt, "SkullOwner")) {
            return false;
        }
        Object skullOwnerCompound = compoundGetCompound(nbt, "SkullOwner");
        if (!compoundHasKey(skullOwnerCompound, "Properties")) {
            return false;
        }
        Object propertiesCompound = compoundGetCompound(skullOwnerCompound, "Properties");
        if (!compoundHasKey(propertiesCompound, "textures")) {
            return false;
        }
        Object textures = compoundGetList(propertiesCompound, "textures", 10);
        for (int i = 0; i < getListSize(textures); i++) {
            final Object compound = listGetIndexCompound(textures, i);
            if (!compoundHasKey(compound, "Value")) {
                continue;
            }
            Object value = compoundGetFuzzyBase(compound, "Value");
            String base64;
            try {
                base64 = new String(Base64.getDecoder().decode(dataAccessor.get(value)));
            } catch (IllegalArgumentException ex) {
                return true;
            }
            final String formatted = base64
                    .trim()
                    .replace(" ", "")
                    .replace("\"", "")
                    .toLowerCase();
            final Matcher matcher = URL_PATTERN.matcher(formatted);
            while (matcher.find()) {
                final String url = formatted.substring(matcher.end() + 1).split("\n")[0];
                if (!url.startsWith("http://textures.minecraft.net") && !url.startsWith("https://textures.minecraft.net")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkBook(Object tag) {
        return checkBook(tag, getNbtLength(tag));
    }

    public static boolean checkBook(Object tag, int nbtLength) {
        if (nbtLength > ConfigurationOptions.NBT.MAX_BOOK_NBT_LENGTH || tag == null) {
            return true;
        }
        if (!compoundHasKeyOfType(tag, "pages", 9)) {
            return true;
        }
        Object nbtList = compoundGetFuzzyList(tag, "pages");
        int size = getListSize(nbtList);
        if (size > ConfigurationOptions.NBT.MAX_BOOK_PAGES) {
            return true;
        }
        for (int i = 0; i < size; i++) {
            final Object base = listGetIndexFuzzy(nbtList, i);
            if (base == null) {
                return true;
            }
            String page = dataAccessor.get(base);
            if (page == null || page.length() > ConfigurationOptions.NBT.MAX_BOOK_PAGE_LENGTH) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkContainer(ItemStack item) {
        return item.hasItemMeta() && item.toString().getBytes().length > ConfigurationOptions.Items.MAX_CHEST_DATA;
    }

    public static void clearBookTags(ItemStack item) {
        clearBookTags(ItemHelper.asNMSItem(item));
    }

    public static void clearBookTagsFromItem(Object nmsItem) {
        if (!ItemHelper.isNMSItem(nmsItem)) {
            return;
        }
        Object tag = getTag(nmsItem);
        if (tag == null) {
            return;
        }
        clearBookTags(tag);
    }

    public static void clearBookTags(Object tag) {
        if (!isNBTCompound(tag)) {
            return;
        }
        removeCompoundTag(tag, "pages");
        removeCompoundTag(tag, "author");
        removeCompoundTag(tag, "title");
    }

}
