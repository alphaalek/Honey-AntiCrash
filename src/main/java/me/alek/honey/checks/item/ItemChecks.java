package me.alek.honey.checks.item;

import me.alek.honey.checks.item.enums.AcceptStrategy;
import me.alek.honey.netty.modelwrappers.WrappedRenderData;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTElement;
import me.alek.honey.netty.modelwrappers.nbt.NBTRoot;
import me.alek.honey.netty.modelwrappers.nbt.NBTType;
import me.alek.honey.netty.modelwrappers.nbt.lists.NBTListOfCompound;
import me.alek.honey.netty.structure.ReflectStructureCache;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemChecks {

    public static class IllegalNumbersCheck implements ItemAnalysis.ItemChecker {

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(ItemStack item, NBTCompound nbt) {
            NBTRoot root = nbt.getRoot();

            for (NBTElement<Double> doubleValue : root.getAllElementsAs(NBTType.DOUBLE, NBTElement.class)) {
                if (NumberConversions.isInvalid(doubleValue.getValue())) {
                    return true;
                }
            }
            for (NBTElement<Float> floatValue : root.getAllElementsAs(NBTType.FLOAT, NBTElement.class)) {
                if (NumberConversions.isInvalid(floatValue.getValue())) {
                    return true;
                }
            }
            for (NBTElement<Integer> intValue : root.getAllElementsAs(NBTType.INT, NBTElement.class)) {
                if (intValue.getValue() == Integer.MAX_VALUE || intValue.getValue() == Integer.MIN_VALUE) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxContainerCheck implements ItemAnalysis.ItemChecker {

        private int MAX_CONTAINER_DATA;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return item.getItemMeta().toString().getBytes().length > MAX_CONTAINER_DATA;
        }

        @Override
        public void setValue(int value) {
            MAX_CONTAINER_DATA = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class MaxEnchantmentLevelCheck implements ItemAnalysis.ItemChecker {

        private int MAX_ENCHANTMENT_LEVEL;
        private String[] enchantmentKeys = new String[]{"ench", "StoredEnchantments", "Enchantments"};

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            NBTListOfCompound enchantments = null;

            for (String key : enchantmentKeys) {
                if (nbt.hasKeyOfType(key, NBTType.LISTOFCOMOUND)) {
                    enchantments = nbt.getListOfCompound(key);
                    break;
                }
            }
            if (enchantments != null) {
                for (NBTCompound enchantment : enchantments) {
                    NBTElement<Short> level = enchantment.get("lvl").asElement(short.class);
                    if (level.getValue() > MAX_ENCHANTMENT_LEVEL) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
            MAX_ENCHANTMENT_LEVEL = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ALL;
        }
    }

    public static class MaxDisplayNameLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_DISPLAYNAME_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (item.getItemMeta().getDisplayName() == null) {
                return false;
            }
            return item.getItemMeta().getDisplayName().length() > MAX_DISPLAYNAME_LENGTH;
        }

        @Override
        public void setValue(int value) {
            MAX_DISPLAYNAME_LENGTH = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class MaxTotalLoreLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_TOTAL_LORE_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (item.getItemMeta().getLore() == null) {
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class UnbreakableItemCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!nbt.hasKeyOfType("Unbreakable", NBTType.BYTE)) {
                return false;
            }
            NBTElement<Byte> unbreakable = nbt.get("Unbreakable").asElement(Byte.class);
            return unbreakable.getValue() == (byte) 1;
        }

        @Override
        public void setValue(int value) {
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class AttributeModifiersCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("AttributeModifiers", NBTType.LISTOFCOMOUND);
        }

        @Override
        public void setValue(int value) {
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class CustomPotionEffectsCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("CustomPotionEffects", NBTType.LISTOFCOMOUND);
        }

        @Override
        public void setValue(int value) {
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class IllegalCharacterCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxNBTStringLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_NBT_STRING_LENGTH;

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(ItemStack item, NBTCompound nbt) {
            for (NBTElement<String> stringValue : nbt.getRoot().getAllElementsAs(NBTType.STRING, NBTElement.class)) {
                if (stringValue.getValue().getBytes().length > MAX_NBT_STRING_LENGTH) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
            MAX_NBT_STRING_LENGTH = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxPageLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_PAGE_LENGTH;

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(ItemStack item, NBTCompound nbt) {
            for (NBTElement<String> page : nbt.getRoot().getAllElementsAs(NBTType.STRING, NBTElement.class)) {

                if (page.getValue().length() > MAX_PAGE_LENGTH) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
            MAX_PAGE_LENGTH = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookPagesCheck implements ItemAnalysis.ItemChecker {

        private int MAX_BOOK_PAGES;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (nbt.hasKeyOfType("pages", NBTType.LIST)) {
                return nbt.getListOf("pages", String.class).asList().size() > MAX_BOOK_PAGES;
            }
            return true;
        }

        @Override
        public void setValue(int value) {
            MAX_BOOK_PAGES = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookTitleLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_BOOK_TITLE_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.getString("title").length() > MAX_BOOK_TITLE_LENGTH;
        }

        @Override
        public void setValue(int value) {
            MAX_BOOK_TITLE_LENGTH = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookAuthorLengthCheck implements ItemAnalysis.ItemChecker {

        private int MAX_BOOK_AUHTOR_LENGTH;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return nbt.getString("author").length() > MAX_BOOK_AUHTOR_LENGTH;
        }

        @Override
        public void setValue(int value) {
            MAX_BOOK_AUHTOR_LENGTH = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class IllegalSkullTexturesCheck implements ItemAnalysis.ItemChecker {

        private static final Pattern URL_PATTERN = Pattern.compile("url");
        private static final String[] skullOwnerKeys = new String[]{"SkullOwner", "Owner"};

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            NBTCompound skullOwnerCompound = null;
            for (String key : skullOwnerKeys) {
                if (nbt.hasKeyOfType(key, NBTType.COMPOUND)) {
                    skullOwnerCompound = nbt.getCompound(key);
                    break;
                }
            }
            if (skullOwnerCompound == null || !skullOwnerCompound.hasKey("Properties")) {
                return false;
            }
            NBTCompound propertiesCompound = skullOwnerCompound.getCompound("Properties");
            if (!propertiesCompound.hasKeyOfType("textures", NBTType.LISTOFCOMOUND)) {
                return true;
            }
            NBTListOfCompound textures = propertiesCompound.getListOfCompound("textures");
            for (NBTCompound texture : textures.getValues()) {

                if (!texture.hasKeyOfType("Value", NBTType.STRING)) {
                    return true;
                }
                NBTElement<String> value = texture.get("Value").asElement(String.class);
                String base64;
                try {
                    base64 = new String(Base64.getDecoder().decode(value.getValue()));
                } catch (IllegalArgumentException ex) {
                    return true;
                }
                final String formatted = base64.trim().replace(" ", "").replace("\"", "").toLowerCase();
                final Matcher matcher = URL_PATTERN.matcher(formatted);
                if (!matcher.find()) {
                    return true;
                }
                final String url = formatted.substring(matcher.end() + 1).split("\n")[0];
                if (!url.startsWith("http://textures.minecraft.net") && !url.startsWith("https://textures.minecraft.net")) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {

        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ALL;
        }
    }

    public static class MaxFireworkExplosionsCheck implements ItemAnalysis.ItemChecker {

        private int MAX_FIREWORK_EXPLOSIONS;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!nbt.hasKeyOfType("Fireworks", NBTType.COMPOUND)) {
                return false;
            }
            NBTCompound fireworksCompound = nbt.getCompound("Fireworks");
            if (!fireworksCompound.hasKeyOfType("Explosions", NBTType.LISTOFCOMOUND)) {
                return false;
            }
            return fireworksCompound.getListOfCompound("Explosions").size() > MAX_FIREWORK_EXPLOSIONS;
        }

        @Override
        public void setValue(int value) {
            MAX_FIREWORK_EXPLOSIONS = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBannerPatternsCheck implements ItemAnalysis.ItemChecker {

        private int MAX_BANNER_PATTENRS;

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!nbt.hasKeyOfType("Patterns", NBTType.LISTOFCOMOUND)) {
                return false;
            }
            return nbt.getListOfCompound("Patterns").size() > MAX_BANNER_PATTENRS;
        }

        @Override
        public void setValue(int value) {
            MAX_BANNER_PATTENRS = value;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
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

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ALL;
        }
    }


    public static class IllegalComponentStrings implements ItemAnalysis.ItemChecker {

        private final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\"translation.test.invalid\"}",
                "{translate:\"translation.test.invalid2\"}",
                "\"action:\":\"run_command\""
        };

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(ItemStack item, NBTCompound nbt) {
            NBTRoot root = nbt.getRoot();
            for (NBTElement<String> string : root.getAllElementsAs(NBTType.STRING, NBTElement.class)) {
                for (String component : ILLEGAL_COMPONENT_STRINGS) {
                    if (string.getValue().toLowerCase().replace("\\", "").trim().contains(component)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }
}
