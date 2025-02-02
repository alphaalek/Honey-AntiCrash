package me.alek.honey.validation.item;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.mapper.MappedEntity;

import java.util.Arrays;
import java.util.function.Supplier;

public enum ItemValidationRegistry {
    MAX_ENCHANTMENT_LEVEL(ItemValidations.MaxEnchantmentLevelItemValidation::new),
    MAX_AMOUNT(ItemValidations.MaxAmountItemValidation::new),
    MAX_DISPLAYNAME_LENGTH(ItemValidations.MaxDisplayNameLengthItemValidation::new),
    MAX_TOTAL_LORE_LENGTH(ItemValidations.MaxTotalLoreLengthItemValidation::new),
    ILLEGAL_CHARACTER(ItemValidations.IllegalCharacterItemValidation::new),
    ILLEGAL_ENCHANTMENTS(ItemValidations.IllegalEnchantmentsItemValidation::new),
    MAX_FIREWORK_EXPLOSIONS(ItemValidations.MaxFireworkExplosionsItemValidation::new, ItemTypes.FIREWORK_ROCKET),
    MAX_BANNER_PATTERNS(ItemValidations.MaxBannerPatternsItemValidation::new,
            ItemTypes.BLACK_BANNER, ItemTypes.BLUE_BANNER, ItemTypes.BROWN_BANNER, ItemTypes.LIGHT_BLUE_BANNER, ItemTypes.GRAY_BANNER,
            ItemTypes.CYAN_BANNER, ItemTypes.GREEN_BANNER, ItemTypes.LIGHT_BLUE_BANNER, ItemTypes.LIGHT_GRAY_BANNER, ItemTypes.LIME_BANNER,
            ItemTypes.MAGENTA_BANNER, ItemTypes.ORANGE_BANNER, ItemTypes.PINK_BANNER, ItemTypes.PURPLE_BANNER, ItemTypes.GRAY_BANNER,
            ItemTypes.RED_BANNER, ItemTypes.WHITE_BANNER, ItemTypes.YELLOW_BANNER),
    UNBREAKABLE_ITEM(ItemValidations.UnbreakableItemItemValidation::new),
    CUSTOM_POTION_EFFECTS(ItemValidations.CustomPotionEffectsItemValidation::new,
            ItemTypes.POTION, ItemTypes.LINGERING_POTION, ItemTypes.SPLASH_POTION),
    ATTRIBUTE_MODIFIERS(ItemValidations.AttributeModifiersItemValidation::new),
    BOOK_NO_TEXT_COMPONENT(ItemValidations.BookComponentsValidation::new, ItemTypes.WRITABLE_BOOK, ItemTypes.WRITTEN_BOOK),
    MAP_RANGE(ItemValidations.MapRangeItemValidation::new, ItemTypes.MAP, ItemTypes.FILLED_MAP),

    // nbt
    MAX_NBT_LENGTH(ItemValidations.MaxNBTLengthItemValidation::new),
    MAX_NBT_STRING_LENGTH(ItemValidations.MaxNBTStringLengthItemValidation::new),
    MAX_BOOK_PAGE_LENGTH(ItemValidations.MaxPageLengthItemValidation::new, ItemTypes.WRITABLE_BOOK, ItemTypes.WRITTEN_BOOK),
    MAX_BOOK_NBT_LENGTH(ItemValidations.MaxBookNBTLengthItemValidation::new, ItemTypes.WRITABLE_BOOK, ItemTypes.WRITTEN_BOOK),
    MAX_BOOK_PAGES(ItemValidations.MaxBookPagesItemValidation::new, ItemTypes.WRITABLE_BOOK, ItemTypes.WRITTEN_BOOK),
    MAX_BOOK_AUTHOR_LENGTH(ItemValidations.MaxBookAuthorLengthItemValidation::new, ItemTypes.WRITTEN_BOOK),
    MAX_BOOK_TITLE_LENGTH(ItemValidations.MaxBookTitleLengthItemValidation::new, ItemTypes.WRITTEN_BOOK),
    MAX_NBT_BASES(ItemValidations.MaxNBTBasesItemValidation::new),
    MAX_NBT_NESTING(ItemValidations.MaxNBTNestingItemValidation::new),
    MAX_FIREWORK_NBT_LENGTH(ItemValidations.MaxFireworkLengthItemValidation::new, ItemTypes.FIREWORK_ROCKET, ItemTypes.FIREWORK_STAR),
    ILLEGAL_SKULL_TEXTURES(ItemValidations.IllegalSkullTexturesItemValidation::new, ItemTypes.PLAYER_HEAD),
    ILLEGAL_COMPONENT_STRINGS(ItemValidations.IllegalComponentStrings::new),
    ILLEGAL_NUMBERS(ItemValidations.IllegalNumbersItemValidation::new),
    ;

    private final ItemAnalysis.ItemValidation validation;
    private final ItemType[] forTypes;
    private final String forTag;

    ItemValidationRegistry(Supplier<ItemAnalysis.ItemValidation> check, ItemType... availableTypes) {
        this(check, "", availableTypes);
    }

    ItemValidationRegistry(Supplier<ItemAnalysis.ItemValidation> validation, String tag, ItemType... availableTypes) {
        this.validation = validation.get();
        this.forTag = tag;
        this.forTypes = Arrays.stream(availableTypes).filter(MappedEntity::isRegistered).toArray(ItemType[]::new);
    }

    public ItemAnalysis.ItemValidation getValidation() {
        return this.validation;
    }

    public ItemType[] getTypes() {
        return this.forTypes;
    }

    public String getTag() {
        return this.forTag;
    }

    public boolean hasFilter() {
        return !this.forTag.isEmpty() || this.forTypes.length > 0;
    }

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "-");
    }
}
