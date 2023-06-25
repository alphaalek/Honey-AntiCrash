package me.alek.honey.helper;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.nbt.*;
import me.alek.honey.netty.structure.converters.MapConverter;
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
        NBTCompound compound = NBTFactory.fromItem(item);
        if (compound == null) {
            return false;
        }
        int nbtLength = compound.toString().getBytes().length;
        switch (item.getType()) {
            case FIREWORK: {
                if (nbtLength > ConfigurationOptions.ItemAnalysis.NBT.MAX_FIREWORK_NBT_LENGTH) {
                    return true;
                }
                break;
            }
            case FIREWORK_CHARGE: {
                if (nbtLength > ConfigurationOptions.ItemAnalysis.NBT.MAX_FIREWORK_CHARGE_NBT_LENGTH) {
                    return true;
                }
                break;
            }
            case WRITTEN_BOOK:
            case BOOK_AND_QUILL: {
                if (checkBook(compound, nbtLength)) {
                    clearBookTags(compound);
                    return true;
                }
                break;
            }
            default: {
                if (nbtLength > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_LENGTH) {
                    Bukkit.broadcastMessage("ok");
                    return true;
                }
            }
        }
        if (ItemHelper.isContainer(item)) {
            if (checkContainer(item)) {
                return true;
            }
        }
        if (ConfigurationOptions.ItemAnalysis.NBT.CHECK_ILLEGAL_SKULL_TEXTURES) {
            if (ItemHelper.isSkull(item)) {
                if (checkSkull(compound)) {
                    return true;
                }
            }
        }
        if (checkBigNBT(compound)) {
            if (ItemHelper.isBook(item)) {
                clearBookTags(compound);
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

    public static boolean checkBigNBT(NBTCompound tag) {
        return checkCompoundNBT(tag, new AtomicInteger(), 0);
    }

    private static boolean checkCompoundNBT(NBTCompound tag, AtomicInteger totalBases, int nestingContext) {
        nestingContext++;
        if (checkValues(totalBases.get(), nestingContext)) {
            return true;
        }
        Set<String> keys = tag.getKeys();
        if (keys == null || keys.isEmpty()) {
            return false;
        }
        for (final String key : keys) {
            if (checkValues(totalBases.incrementAndGet(), nestingContext)) {
                return true;
            }
            if (tag.hasKeyOfType(key, NBTType.LIST) || tag.hasKeyOfType(key, NBTType.COMPOUNDLIST)) {
                NBTAbstractList<?> list = tag.getList(key);
                if (list == null) {
                    continue;
                }
                if (checkListNBT(list, totalBases, nestingContext)) {
                    return true;
                }
                continue;
            }
            if (tag.hasKeyOfType(key, NBTType.COMPOUND)) {
                NBTCompound compound = tag.getCompound(key);
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

    private static boolean checkListNBT(NBTAbstractList<?> list, AtomicInteger totalBases, int nestingContext) {
        nestingContext++;
        if (checkValues(totalBases.get(), nestingContext)) {
            return true;
        }
        for (int i = 0; i < list.size(); i++) {
            if (checkValues(totalBases.incrementAndGet(), nestingContext)) {
                return true;
            }
            NBTBase<?, ?> base = list.getUnknown(i);
            if (base == null) {
                continue;
            }
            if (base.getType() == NBTType.COMPOUND) {
                if (checkCompoundNBT((NBTCompound) base, totalBases, nestingContext)) {
                    return true;
                }
                continue;
            }
            if (base.getType() == NBTType.LIST || base.getType() == NBTType.COMPOUNDLIST) {
                if (checkListNBT((NBTAbstractList<?>) base, totalBases, nestingContext)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkValues(int totalBases, int nestingContext) {
        return totalBases > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES || nestingContext > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_NESTING;
    }

    public static boolean checkSkull(NBTCompound nbt) {
        if (!nbt.hasKey("SkullOwner")) {
            return false;
        }
        NBTCompound skullOwnerCompound = nbt.getCompound("SkullOwner");
        if (!skullOwnerCompound.hasKey("Properties")) {
            return false;
        }
        NBTCompound propertiesCompound = skullOwnerCompound.getCompound("Properties");
        if (!propertiesCompound.hasKey("textures")) {
            return false;
        }
        NBTCompoundList textures = propertiesCompound.getCompoundList("textures");
        Bukkit.broadcastMessage(textures + "");
        for (NBTBase<?, MapConverter<String, Object, NBTBase<?, ?>>> compoundBase : textures.getValues()) {
            final MapConverter<String, Object, NBTBase<?, ?>> texture = compoundBase.getValue();
            if (!texture.containsKey("Value")) {
                return true;
            }
            NBTBase<?, String> value = (NBTBase<?, String>) texture.getConverted("Value");

            String base64;
            try {
                base64 = new String(Base64.getDecoder().decode(value.getValue()));
            } catch (IllegalArgumentException ex) {
                return true;
            }
            final String formatted = base64
                    .trim()
                    .replace(" ", "")
                    .replace("\"", "")
                    .toLowerCase();
            Bukkit.broadcastMessage(formatted);

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

    public static boolean checkBook(NBTCompound tag, int nbtLength) {
        try {
            if (nbtLength > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_NBT_LENGTH || tag == null) {
                return true;
            }
            if (!tag.hasKey("pages")) {
                return true;
            }
            NBTList<String> stringList = tag.getListOf("pages", NBTType.STRING);
            if (stringList.size() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGES) {
                return true;
            }
            for (final NBTBase<?, String> page : stringList.getValues()) {
                if (page == null) {
                    return true;
                }
                String value = page.getValue();
                if (value == null || value.length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH) {
                    return true;
                }
            }
        } catch (Exception ex) {
            return true;
        }
        return false;
    }

    public static boolean checkContainer(ItemStack item) {
        return item.hasItemMeta() && item.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.MAX_CONTAINER_DATA;
    }

    public static void clearBookTagsFromItem(ItemStack item) {
        NBTCompound tag = NBTFactory.fromItem(item);
        if (tag == null) {
            return;
        }
        clearBookTags(tag);
    }

    public static void clearBookTags(NBTCompound tag) {
        tag.remove("pages");
        tag.remove("author");
        tag.remove("title");
    }

}
