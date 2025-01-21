package me.alek.honey.validation.item;

import com.google.gson.JsonElement;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.modelwrappers.objects.WrappedRenderData;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.netty.nbt.NBTFactory;
import me.alek.honey.netty.nbt.PrimitiveTag;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.MapConverter;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.statics.NumberUtils;
import org.apache.commons.codec.binary.Base64;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapView;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemValidations {

    public static class IllegalNumbersItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            for (PrimitiveTag<Double> doubleValue : NBTFactory.<PrimitiveTag<Double>>getAllTagsAs(nbt, TagType.DOUBLE)) {
                if (NumberUtils.isInvalid(doubleValue.getValue())) {
                    return true;
                }
            }

            for (PrimitiveTag<Float> floatValue : NBTFactory.<PrimitiveTag<Float>>getAllTagsAs(nbt, TagType.FLOAT)) {
                if (NumberUtils.isInvalid(floatValue.getValue())) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxEnchantmentLevelItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            ItemMeta meta = item.getDefaultMeta();
            if (meta.hasEnchants()) {
                for (int level : meta.getEnchants().values()) {
                    if (level > ConfigurationOptions.ItemAnalysis.MAX_ENCHANTMENT_LEVEL) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_META;
        }
    }

    public static class MaxAmountItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return item.getAmount() > ConfigurationOptions.ItemAnalysis.MAX_AMOUNT;
        }
    }

    public static class MaxDisplayNameLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            ItemMeta meta = item.getDefaultMeta();
            return meta.hasDisplayName() && meta.getDisplayName().length() > ConfigurationOptions.ItemAnalysis.MAX_DISPLAYNAME_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_META;
        }
    }

    public static class MaxTotalLoreLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            ItemMeta meta = item.getDefaultMeta();
            if (meta.hasLore()) {
                List<String> loreArray = meta.getLore();
                if (loreArray != null) {
                    int counter = 0;

                    for (String lore : loreArray) {
                        counter += lore.length();
                        if (counter > ConfigurationOptions.ItemAnalysis.MAX_TOTAL_LORE_LENGTH) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_META;
        }
    }

    public static class UnbreakableItemItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_UNBREAKABLE_ITEMS
                    && nbt.hasKeyOfType("Unbreakable", TagType.BYTE)
                    && nbt.getByte("Unbreakable") == (byte) 1;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class AttributeModifiersItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_ATTRIBUTE_MODIFIERS
                    && nbt.hasKeyOfType("AttributeModifiers", TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND));
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class CustomPotionEffectsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return ConfigurationOptions.ItemAnalysis.CHECK_CUSTOM_POTION_EFFECTS
                    && nbt.hasKeyOfType("CustomPotionEffects", TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND));
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class BookComponentsValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            List<Object> pages = ItemReflection.getPages((BookMeta) item.getSpecificMeta());
            if (pages == null) {
                return false;
            }

            try {
                for (Object page : pages) {
                    if (NMS.getIChatBaseComponentClass().isAssignableFrom(page.getClass())) {
                        JsonElement json = ItemReflection.chatComponentToJson(page);
                        if (!json.getAsJsonObject().has("text")) {
                            return true;
                        }
                        JsonElement siblings = json.getAsJsonObject().get("extra");
                        for (JsonElement sibling : siblings.getAsJsonArray()) {
                            if (!sibling.getAsJsonObject().has("text") || sibling.getAsJsonObject().get("siblings").getAsJsonArray().size() > 0) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // malformed json structure ?
                return true;
            }

            return false;
        }


        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_SPECIFIC_META;
        }
    }

    public static class IllegalCharacterItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            ItemMeta meta = item.getDefaultMeta();
            if (meta.hasDisplayName() && checkBadCharacter(meta.getDisplayName())) {
                return true;
            }

            if (meta.hasLore()) {
                List<String> loreArray = meta.getLore();
                if (loreArray != null) {
                    for (String lore : loreArray) {
                        if (checkBadCharacter(lore)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_META;
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
    }

    public static class MaxNBTLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxNBTStringLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            for (PrimitiveTag<String> stringValue : NBTFactory.<PrimitiveTag<String>>getAllTagsAs(nbt, TagType.STRING)) {
                if (stringValue.getValue().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_STRING_LENGTH) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxPageLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            for (PrimitiveTag<String> page : nbt.<String>getArrayOf("pages", TagType.STRING)) {
                if (page != null
                        && page.getValue().length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGE_LENGTH) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxFireworkLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_FIREWORK_NBT_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxBookNBTLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return nbt.toString().getBytes().length > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_NBT_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxBookPagesItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return nbt.hasKeyOfType("pages", TagType.getFromCollection(TagType.Collection.ARRAY))
                    && nbt.getArrayOf("pages", String.class).size() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_PAGES;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxBookAuthorLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            String author;
            return nbt != null
                    && nbt.hasKeyOfType("author", TagType.STRING)
                    && (author = nbt.getString("author")) != null && author.length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_AUTHOR_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxBookTitleLengthItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return nbt.hasKeyOfType("title", TagType.STRING)
                    && nbt.getString("title").length() > ConfigurationOptions.ItemAnalysis.NBT.MAX_BOOK_TITLE_LENGTH;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxNBTBasesItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return NBTFactory.getAmountOfTags(nbt) > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxNBTNestingItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return NBTFactory.getComplexityOfTag(nbt) > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_NESTING;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class IllegalSkullTexturesItemValidation implements ItemAnalysis.ItemValidation {

        private static final Pattern URL_PATTERN = Pattern.compile("url");
        private static final String[] skullOwnerKeys = new String[]{"SkullOwner", "Owner"};

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            CompoundTag skullOwnerCompound = null;
            for (String key : skullOwnerKeys) {
                if (nbt.hasKeyOfType(key, TagType.getFromCollection(TagType.Collection.COMPOUND))) {
                    skullOwnerCompound = nbt.getCompound(key);
                    break;
                }
            }
            if (skullOwnerCompound == null || !skullOwnerCompound.hasKey("Properties")) {
                return false;
            }

            CompoundTag propertiesCompound = skullOwnerCompound.getCompound("Properties");
            if (!propertiesCompound.hasKeyOfType("textures", TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND))) {
                return false;
            }

            GeneticArrayTypes.ArrayOfCompoundTag textures = propertiesCompound.getArrayOfCompound("textures");
            for (CompoundTag texture : textures.getValues()) {
                if (!texture.hasKeyOfType("Value", TagType.STRING)) {
                    return false;
                }

                try {
                    byte[] value = texture.getString("Value").getBytes(StandardCharsets.UTF_8);
                    String base64 = new String(Base64.decodeBase64(value));
                    final String formatted = base64.trim()
                            .replace(" ", "")
                            .replace("\"", "")
                            .toLowerCase();

                    Matcher matcher = URL_PATTERN.matcher(formatted);
                    if (!matcher.find()) {
                        return true;
                    }
                    String url = formatted.substring(matcher.end() + 1).split("\n")[0];
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
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class IllegalEnchantmentsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return false;
        }
    }

    public static class MaxFireworkExplosionsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            if (!nbt.hasKeyOfType("Fireworks", TagType.getFromCollection(TagType.Collection.COMPOUND))) {
                return false;
            }

            CompoundTag fireworksCompound = nbt.getCompound("Fireworks");
            if (!fireworksCompound.hasKeyOfType("Explosions", TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND))) {
                return false;
            }

            return fireworksCompound.getArrayOfCompound("Explosions").size() > ConfigurationOptions.ItemAnalysis.MAX_FIREWORK_EXPLOSIONS;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MaxBannerPatternsItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            return nbt.hasKeyOfType("Patterns", TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND))
                    && nbt.getArrayOfCompound("Patterns").size() > ConfigurationOptions.ItemAnalysis.MAX_BANNER_PATTERNS;
        }

        @Override
        public ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }

    public static class MapRangeItemValidation implements ItemAnalysis.ItemValidation {

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13)) {
                return false;
            }

            MapView mapView = Bukkit.getMap(item.getDurability());
            if (mapView == null) return false;

            MapConverter<Object, Object, WrappedRenderData> renderCache = ReflectStructure.Provider.acquireStructure(mapView.getClass())
                    .withWeakType(Map.class, Converters.getMapConverterOf(WrappedRenderData.class, Converters.getRenderDataConverter()))
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
    }


    public static class IllegalComponentStrings implements ItemAnalysis.ItemValidation {

        private final String[] ILLEGAL_COMPONENT_STRINGS = new String[]{
                "{translate:\"translation.test.invalid\"}",
                "{translate:\"translation.test.invalid2\"}",
                "\"action:\":\"run_command\""
        };

        @Override
        public boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt) {
            for (PrimitiveTag<String> string : NBTFactory.<PrimitiveTag<String>>getAllTagsAs(nbt, TagType.STRING)) {
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
            return ItemValidationFilterStrategy.WITH_NBT;
        }
    }
}
