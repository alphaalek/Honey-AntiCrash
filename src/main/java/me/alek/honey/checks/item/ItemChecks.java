package me.alek.honey.checks.item;

import me.alek.honey.netty.api.packet.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedRenderData;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTElement;
import me.alek.honey.netty.modelwrappers.nbt.NBTList;
import me.alek.honey.netty.modelwrappers.nbt.NBTListOfCompound;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.netty.utility.reflect.FieldAccessor;
import me.alek.honey.netty.utility.reflect.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.map.RenderData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemChecks {

    public static class MaxContainerCheck implements ItemAnalysis.ItemChecker {

        private int MAX_CONTAINER_DATA;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!item.hasItemMeta()) {
                return false;
            }
            return item.getItemMeta().toString().getBytes().length > MAX_CONTAINER_DATA;
        }

        @Override
        public void setValue(int value) {
            MAX_CONTAINER_DATA = value;
        }
    }

    public static class MaxEnchantmentLevelCheck implements ItemAnalysis.ItemChecker {

        private int MAX_ENCHANTMENT_LEVEL;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            NBTListOfCompound enchantments;
            Material material = item.getType();
            if (material == Material.ENCHANTED_BOOK) {
                enchantments = nbt.getListOfCompound("StoredEnchantments");
            }
            else {
                enchantments = nbt.getListOfCompound("ench");
            }
            for (NBTCompound enchantment : enchantments) {
                NBTElement<Short> level = enchantment.get("lvl").asElement(short.class);
                if (level.getValue() > MAX_ENCHANTMENT_LEVEL) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
            MAX_ENCHANTMENT_LEVEL = value;
        }
    }

    public static class MaxAmountCheck implements ItemAnalysis.ItemChecker {

        private int MAX_AMOUNT;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return item.getAmount() > MAX_AMOUNT;
        }

        @Override
        public void setValue(int value) {
            MAX_AMOUNT = value;
        }
    }

    public static class MaxDisplayNameLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_DISPLAYNAME_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!item.hasItemMeta() || item.getItemMeta().getDisplayName() == null) {
                return false;
            }
            return item.getItemMeta().getDisplayName().length() > MAX_DISPLAYNAME_LENGTH;
        }

        @Override
        public void setValue(int value) {
            MAX_DISPLAYNAME_LENGTH = value;
        }
    }

    public static class MaxTotalLoreLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_TOTAL_LORE_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!item.hasItemMeta() || item.getItemMeta().getLore() == null) {
                return false;
            }
            int counter = 0;
            for (String lore : item.getItemMeta().getLore()) {
                counter += lore.length();
                if (counter > MAX_TOTAL_LORE_LENGTH) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
            MAX_TOTAL_LORE_LENGTH = value;
        }
    }

    public static class IllegalCharacterCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!item.hasItemMeta()) {
                return false;
            }
            if (item.getItemMeta().getDisplayName() != null && checkBadCharacter(item.getItemMeta().getDisplayName())) {
                return true;
            }
            if (item.getItemMeta().getLore() != null) {
                for (String lore : item.getItemMeta().getLore()) {
                    if (checkBadCharacter(lore)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean checkBadCharacter(String str) {
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

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxNBTLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_NBT_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > MAX_NBT_LENGTH;
        }

        @Override
        public void setValue(int value) {
            MAX_NBT_LENGTH = value;
        }
    }

    public static class MaxFireworkLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_FIREWORK_NBT_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > MAX_FIREWORK_NBT_LENGTH;
        }

        @Override
        public void setValue(int value) {
            MAX_FIREWORK_NBT_LENGTH = value;
        }
    }

    public static class MaxBookNBTLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_BOOK_NBT_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > MAX_BOOK_NBT_LENGTH;
        }

        @Override
        public void setValue(int value) {
            MAX_BOOK_NBT_LENGTH = value;
        }
    }

    public static class MaxBookPagesCheck implements ItemAnalysis.ItemChecker {

        private int MAX_BOOK_PAGES;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            Bukkit.broadcastMessage(nbt.getListOf("pages", String.class).size() + "");
            return nbt.getListOf("pages", String.class).asList().size() > MAX_BOOK_PAGES;
        }

        @Override
        public void setValue(int value) {
            MAX_BOOK_PAGES = value;
        }
    }

    public static class MaxBookPageLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_BOOK_PAGE_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            NBTList<String> pages = nbt.getListOf("pages", String.class);
            for (NBTElement<String> page : pages) {
                if (page.getValue().length() > MAX_BOOK_PAGE_LENGTH) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
            MAX_BOOK_PAGE_LENGTH = value;
        }
    }

    public static class MaxNBTBasesCheck implements ItemAnalysis.ItemChecker {

        private int MAX_NBT_BASES;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.getRoot().getTotalElements() > MAX_NBT_BASES;
        }

        @Override
        public void setValue(int value) {
            MAX_NBT_BASES = value;
        }
    }

    public static class MaxNBTNestingCheck implements ItemAnalysis.ItemChecker {

        private int MAX_NBT_NESTING;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.getRoot().getMaxDistanceBetweenTail() > MAX_NBT_NESTING;
        }

        @Override
        public void setValue(int value) {
            MAX_NBT_NESTING = value;
        }
    }

    public static class IllegalSkullTexturesCheck implements ItemAnalysis.ItemChecker {

        private static final Pattern URL_PATTERN = Pattern.compile("url");

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
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
            NBTListOfCompound textures = propertiesCompound.getListOfCompound("textures");
            for (NBTCompound texture : textures.getValues()) {

               if (!texture.containsKey("Value")) {
                    return true;
                }
                String value = texture.getString("Value");
                String base64;
                try {
                    base64 = new String(Base64.getDecoder().decode(value));
                } catch (IllegalArgumentException ex) {
                    return true;
                }
                final String formatted = base64.trim().replace(" ", "").replace("\"", "").toLowerCase();
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

        @Override
        public void setValue(int value) {

        }
    }

    public static class IllegalEnchantmentsCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class IllegalFireworkCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class IllegalPotionsCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MapRangeCheck implements ItemAnalysis.ItemChecker {

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(ItemStack item, NBTCompound nbt) {
            MapView mapView = Bukkit.getMap(item.getDurability());

            MapConverter<Object, Object, WrappedRenderData> renderCache = ReflectStructureCache.acquireStructure(mapView.getClass())
                    .withWeakType(Map.class, Converters.getMapConverterOf(WrappedRenderData.class, Converters.getRenderDataConverter(), false))
                    .withTarget(mapView)
                    .readFieldAndApplyMapping(0);

            for (WrappedRenderData cache : renderCache.convertedValues()) {
                for (MapCursor cursor : cache.getMapCursors()) {

                    byte rawType = cursor.getRawType();
                    if (rawType < 0 || rawType > 15) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }
}
