package me.alek.honey.helper;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.nbt.*;
import me.alek.honey.netty.structure.converters.MapConverter;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NBTHelper {

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
            case FIREWORK_CHARGE:
            case FIREWORK: {
                if (nbtLength > ConfigurationOptions.ItemAnalysis.NBT.MAX_FIREWORK_NBT_LENGTH) {
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
                    return true;
                }
            }
        }
        if (ItemHelper.isContainer(item)) {
            if (checkContainer(item)) {
                return true;
            }
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


    public static boolean checkBook(NBTCompound tag, int nbtLength) {
        try {
            if (nbtLength > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_NBT_LENGTH || tag == null) {
                return true;
            }
            if (!tag.hasKey("pages")) {
                return true;
            }
            NBTList<String> stringList = tag.getListOf("pages", String.class);
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

    public static void clearBookTags(NBTCompound tag) {
        tag.remove("pages");
        tag.remove("author");
        tag.remove("title");
    }

}
