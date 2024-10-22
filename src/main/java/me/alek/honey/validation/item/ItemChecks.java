package me.alek.honey.validation.item;

import com.google.gson.JsonElement;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.modelwrappers.WrappedRenderData;
import me.alek.honey.netty.nbt.NBTCompound;
import me.alek.honey.netty.nbt.NBTElement;
import me.alek.honey.netty.nbt.NBTRoot;
import me.alek.honey.netty.nbt.NBTType;
import me.alek.honey.netty.nbt.lists.NBTListOfCompound;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.validation.item.enums.AcceptStrategy;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemChecks {

    public static class IllegalNumbersCheck implements ItemAnalysis.Check {

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            NBTRoot root = nbt.getRoot();

            for (NBTElement<Double> doubleValue : root.<Double>getAllPrimitiveElementsAs(NBTType.DOUBLE)) {
                if (NumberUtils.isInvalid(doubleValue.getValue())) {
                    return true;
                }
            }

            for (NBTElement<Float> floatValue : root.<Float>getAllPrimitiveElementsAs(NBTType.FLOAT)) {
                if (NumberUtils.isInvalid(floatValue.getValue())) {
                    return true;
                }
            }

            for (NBTElement<Integer> intValue : root.<Integer>getAllPrimitiveElementsAs(NBTType.INT)) {
                if (intValue.getValue() == Integer.MAX_VALUE
                        || intValue.getValue() == Integer.MIN_VALUE) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxEnchantmentLevelCheck implements ItemAnalysis.Check {

        private final String[] enchantmentKeys = new String[]{"ench", "StoredEnchantments", "Enchantments"};

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            NBTListOfCompound enchantments = null;

            for (String key : enchantmentKeys) {
                if (nbt.hasKeyOfType(key, NBTType.LISTOFCOMOUND)) {
                    enchantments = nbt.getListOfCompound(key);
                    break;
                }
            }

            if (enchantments != null) {
                for (NBTCompound enchantment : enchantments) {
                    short level = enchantment.getShort("lvl");
                    if (level > ConfigurationOptions.ItemAnalysis.MAX_ENCHANTMENT_LEVEL) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxAmountCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return item.getAmount() > ConfigurationOptions.ItemAnalysis.MAX_AMOUNT;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ALL;
        }
    }

    public static class MaxDisplayNameLengthCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().length() > ConfigurationOptions.ItemAnalysis.MAX_DISPLAYNAME_LENGTH;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class MaxTotalLoreLengthCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            if (!item.getItemMeta().hasLore()) {
                return false;
            }

            int counter = 0;
            for (String lore : item.getItemMeta().getLore()) {
                counter += lore.length();
                if (counter > ConfigurationOptions.ItemAnalysis.MAX_TOTAL_LORE_LENGTH) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class UnbreakableItemCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_UNBREAKABLE_ITEMS
                    && nbt.hasKeyOfType("Unbreakable", NBTType.BYTE)
                    && nbt.getByte("Unbreakable") == (byte) 1;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class AttributeModifiersCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_ATTRIBUTE_MODIFIERS
                    && nbt.hasKeyOfType("AttributeModifiers", NBTType.LISTOFCOMOUND);
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class CustomPotionEffectsCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_CUSTOM_POTION_EFFECTS
                    && nbt.hasKeyOfType("CustomPotionEffects", NBTType.LISTOFCOMOUND);
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class BookTranslationComponents implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            AtomicReference<IStructureModifier<List<Object>>> bookMetaPageModifier = new AtomicReference<>();
            List<Object> pages = ItemReflection.getPages(bookMetaPageModifier, (BookMeta) item.getItemMeta());

            for (Object page : pages) {
                if (NMS.getIChatBaseComponentClass().isAssignableFrom(page.getClass())) {
                    JsonElement json = ItemReflection.chatComponentToJson(page);
                    if (json.isJsonObject() && !json.getAsJsonObject().has("text")) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class IllegalCharacterCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            if (item.getItemMeta().hasDisplayName() && checkBadCharacter(item.getItemMeta().getDisplayName())) {
                return true;
            }

            if (item.getItemMeta().hasLore()) {
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
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class MaxNBTLengthCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_LENGTH;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxNBTStringLengthCheck implements ItemAnalysis.Check {

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            for (NBTElement<String> stringValue : nbt.getRoot().<String>getAllPrimitiveElementsAs(NBTType.STRING)) {
                if (stringValue.getValue().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_STRING_LENGTH) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxPageLengthCheck implements ItemAnalysis.Check {

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            for (NBTElement<String> page : nbt.<String>getListOf("pages", NBTType.STRING)) {
                if (page != null
                        && page.getValue().length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxFireworkLengthCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_FIREWORK_NBT_LENGTH;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookNBTLengthCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_NBT_LENGTH;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookPagesCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("pages", NBTType.LIST)
                    && nbt.getListOf("pages", String.class).size() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGES;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookAuthorLengthCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", NBTType.STRING)
                    && (author = nbt.getString("author")) != null && author.length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookTitleLengthCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("title", NBTType.STRING)
                    && nbt.getString("title").length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxNBTBasesCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.getRoot().getTotalElements() > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxNBTNestingCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.getRoot().getMaxDistanceBetweenTail() > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_NESTING;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class IllegalSkullTexturesCheck implements ItemAnalysis.Check {

        private static final Pattern URL_PATTERN = Pattern.compile("url");
        private static final String[] skullOwnerKeys = new String[]{"SkullOwner", "Owner"};

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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
                return false;
            }

            NBTListOfCompound textures = propertiesCompound.getListOfCompound("textures");
            for (NBTCompound texture : textures.getValues()) {
                if (!texture.hasKeyOfType("Value", NBTType.STRING)) {
                    return false;
                }

                try {
                    byte[] value = texture.getString("Value").getBytes(StandardCharsets.UTF_8);
                    String base64 = new String(Base64.decodeBase64(value));
                    final String formatted = base64.trim()
                            .replace(" ", "")
                            .replace("\"", "")
                            .toLowerCase();

                    final Matcher matcher = URL_PATTERN.matcher(formatted);
                    if (!matcher.find()) {
                        return true;
                    }
                    final String url = formatted.substring(matcher.end() + 1).split("\n")[0];
                    if (!url.startsWith("http://textures.minecraft.net") && !url.startsWith("https://textures.minecraft.net")) {
                        return true;
                    }

                } catch (Exception ex) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class IllegalEnchantmentsCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ALL;
        }
    }

    public static class MaxFireworkExplosionsCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            if (!nbt.hasKeyOfType("Fireworks", NBTType.COMPOUND)) {
                return false;
            }

            NBTCompound fireworksCompound = nbt.getCompound("Fireworks");
            if (!fireworksCompound.hasKeyOfType("Explosions", NBTType.LISTOFCOMOUND)) {
                return false;
            }

            return fireworksCompound.getListOfCompound("Explosions").size() > ConfigurationOptions.ItemAnalysis.MAX_FIREWORK_EXPLOSIONS;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBannerPatternsCheck implements ItemAnalysis.Check {

        @Override
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("Patterns", NBTType.LISTOFCOMOUND)
                    && nbt.getListOfCompound("Patterns").size() > ConfigurationOptions.ItemAnalysis.MAX_BANNER_PATTERNS;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MapRangeCheck implements ItemAnalysis.Check {

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            MapView mapView = Bukkit.getMap(item.getDurability());
            if (mapView == null) return false;

            MapConverter<Object, Object, WrappedRenderData> renderCache = ReflectStructure.Provider.acquireStructure(mapView.getClass())
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
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ALL;
        }
    }


    public static class IllegalComponentStrings implements ItemAnalysis.Check {

        private final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\"translation.test.invalid\"}",
                "{translate:\"translation.test.invalid2\"}",
                "\"action:\":\"run_command\""
        };

        @Override
        @SuppressWarnings("unchecked")
        public boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            for (NBTElement<String> string : nbt.getRoot().<String>getAllPrimitiveElementsAs(NBTType.STRING)) {
                String trimmed = string.getValue().toLowerCase().replace("\\", "").trim();
                for (String component : ILLEGAL_COMPONENT_STRINGS) {
                    if (trimmed.contains(component)) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public AcceptStrategy getAcceptStrategy() {
            return AcceptStrategy.ONLY_WITH_NBT;
        }
    }
}
