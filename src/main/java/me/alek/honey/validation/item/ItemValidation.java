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
import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.NBTArray;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.validation.item.enums.ItemValidationFilterStrategy;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ItemValidation {

    public static class IllegalNumbersItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxEnchantmentLevelItemValidation implements ItemAnalysis.ItemValidation {

        private final String[] enchantmentKeys = new String[]{"ench", "StoredEnchantments", "Enchantments"};

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            GeneticArrayTypes.NBTArrayOfCompound enchantments = null;

            for (String key : enchantmentKeys) {
                if (nbt.hasKeyOfType(key, NBTType.ARRAY_OF_COMPOUND)) {
                    enchantments = nbt.getArrayOfCompound(key);
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
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxAmountItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return item.getAmount() > ConfigurationOptions.ItemAnalysis.MAX_AMOUNT;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ALL;
        }
    }

    public static class MaxDisplayNameLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().length() > ConfigurationOptions.ItemAnalysis.MAX_DISPLAYNAME_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class MaxTotalLoreLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class UnbreakableItemItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_UNBREAKABLE_ITEMS
                    && nbt.hasKeyOfType("Unbreakable", NBTType.BYTE)
                    && nbt.getByte("Unbreakable") == (byte) 1;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class AttributeModifiersItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_ATTRIBUTE_MODIFIERS
                    && nbt.hasKeyOfType("AttributeModifiers", NBTType.ARRAY_OF_COMPOUND);
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class CustomPotionEffectsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_CUSTOM_POTION_EFFECTS
                    && nbt.hasKeyOfType("CustomPotionEffects", NBTType.ARRAY_OF_COMPOUND);
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class BookTranslationComponents implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class IllegalCharacterItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_METADATA;
        }
    }

    public static class MaxNBTLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxNBTStringLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        @SuppressWarnings("unchecked")
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            for (NBTElement<String> stringValue : nbt.getRoot().<String>getAllPrimitiveElementsAs(NBTType.STRING)) {
                if (stringValue.getValue().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_STRING_LENGTH) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxPageLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        @SuppressWarnings("unchecked")
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            for (NBTElement<String> page : nbt.<String>getListOf("pages", NBTType.STRING)) {
                if (page != null
                        && page.getValue().length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxFireworkLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_FIREWORK_NBT_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class InvalidBookValidation implements ItemAnalysis.ItemValidation {

        private static final int BOOK_PIXEL_WIDTH = 57;
        private static final int BOOK_LINES = 14;

        private static final int CHARS_PER_PAGE_LEGACY = 255;
        private static final int CHARS_PER_PAGE_MODERN = 798;

        private static final int PAGES_PER_BOOK_LEGACY = 50;
        private static final int PAGES_PER_BOOK_MODERN = 100;

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            if (nbt.hasKeyOfType("pages", NBTType.ARRAY)) {
                NBTArray<String> pages = nbt.getArray("pages");

                boolean v1_14 = player.clientInfo.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14);

                // up until v1.14 the page limit for a book was set to 50 pages
                // then, the limit was changed to 100
                if (!v1_14 && pages.size() > PAGES_PER_BOOK_LEGACY) {
                    return true;
                } else if (v1_14 && pages.size() > PAGES_PER_BOOK_MODERN) {
                    return true;
                }

                for (String page : pages.<String>streamOfValues().collect(Collectors.toList())) {

                    // up until v1.14 the chars per book page limit was set to 255 chars
                    // then, the limit was changed to 798 chars
                    if (!v1_14 && page.length() > CHARS_PER_PAGE_LEGACY) {
                        return true;
                    } else if (v1_14 && page.length() > CHARS_PER_PAGE_MODERN) {
                        return true;
                    }

                    int forLine = 0, lines = 1, sectionChars = 0;
                    for (int i = 0; i < page.length(); i++) {
                        char c = page.charAt(i);
                        // check if the default Minecraft font allows this character
                        if (!MinecraftFont.Font.isValid(String.valueOf(c))) {
                            return true;
                        }

                        // skip the colour code chars
                        if (c == '§') {
                            sectionChars++;
                            int j = page.indexOf(c, i);
                            if (j == -1) {
                                return true;
                            }
                            i = j;
                            continue;
                        }

                        // get the width of the default Minecraft font which is displayed in books
                        // check if the display used for this book page is actually possible to do with a client
                        int width = MinecraftFont.Font.getWidth(String.valueOf(c));
                        forLine += width;
                        if (forLine > BOOK_PIXEL_WIDTH) { // assume line break
                            forLine = width;
                            lines++;
                        } else if (c == '\n') {
                            lines++;
                            forLine = 0;
                        }

                        // does the book has more lines than allowed?
                        if (lines > BOOK_LINES) {
                            return true;
                        }
                    }

                    // allow up to max 2 color codes averagely per char for a page
                    if (sectionChars > page.length() * 2) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookNBTLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_NBT_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookPagesItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("pages", NBTType.ARRAY)
                    && nbt.getListOf("pages", String.class).size() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGES;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookAuthorLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", NBTType.STRING)
                    && (author = nbt.getString("author")) != null && author.length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBookTitleLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("title", NBTType.STRING)
                    && nbt.getString("title").length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxNBTBasesItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.getRoot().getTotalElements() > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxNBTNestingItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.getRoot().getMaxDistanceBetweenTail() > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_NESTING;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class IllegalSkullTexturesItemValidation implements ItemAnalysis.ItemValidation {

        private static final Pattern URL_PATTERN = Pattern.compile("url");
        private static final String[] skullOwnerKeys = new String[]{"SkullOwner", "Owner"};

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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
            if (!propertiesCompound.hasKeyOfType("textures", NBTType.ARRAY_OF_COMPOUND)) {
                return false;
            }

            GeneticArrayTypes.NBTArrayOfCompound textures = propertiesCompound.getArrayOfCompound("textures");
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
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class IllegalEnchantmentsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ALL;
        }
    }

    public static class MaxFireworkExplosionsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            if (!nbt.hasKeyOfType("Fireworks", NBTType.COMPOUND)) {
                return false;
            }

            NBTCompound fireworksCompound = nbt.getCompound("Fireworks");
            if (!fireworksCompound.hasKeyOfType("Explosions", NBTType.ARRAY_OF_COMPOUND)) {
                return false;
            }

            return fireworksCompound.getArrayOfCompound("Explosions").size() > ConfigurationOptions.ItemAnalysis.MAX_FIREWORK_EXPLOSIONS;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MaxBannerPatternsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
            return nbt.hasKeyOfType("Patterns", NBTType.ARRAY_OF_COMPOUND)
                    && nbt.getArrayOfCompound("Patterns").size() > ConfigurationOptions.ItemAnalysis.MAX_BANNER_PATTERNS;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }

    public static class MapRangeItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        @SuppressWarnings("unchecked")
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ALL;
        }
    }


    public static class IllegalComponentStrings implements ItemAnalysis.ItemValidation {

        private final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\"translation.test.invalid\"}",
                "{translate:\"translation.test.invalid2\"}",
                "\"action:\":\"run_command\""
        };

        @Override
        @SuppressWarnings("unchecked")
        public boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
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
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ONLY_WITH_NBT;
        }
    }
}
