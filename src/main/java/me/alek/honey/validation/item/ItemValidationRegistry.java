package me.alek.honey.validation.item;

import me.alek.honey.util.MaterialMappings;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.function.Supplier;

public enum ItemValidationRegistry {
    MAX_ENCHANTMENT_LEVEL(ItemValidation.MaxEnchantmentLevelItemValidation::new),
    MAX_AMOUNT(ItemValidation.MaxAmountItemValidation::new),
    MAX_DISPLAYNAME_LENGTH(ItemValidation.MaxDisplayNameLengthItemValidation::new),
    MAX_TOTAL_LORE_LENGTH(ItemValidation.MaxTotalLoreLengthItemValidation::new),
    ILLEGAL_CHARACTER(ItemValidation.IllegalCharacterItemValidation::new),
    ILLEGAL_ENCHANTMENTS(ItemValidation.IllegalEnchantmentsItemValidation::new),
    MAX_FIREWORK_EXPLOSIONS(ItemValidation.MaxFireworkExplosionsItemValidation::new,
            MaterialMappings.getTypeIfAny("firework-rocket")),
    MAX_BANNER_PATTERNS(ItemValidation.MaxBannerPatternsItemValidation::new,
            MaterialMappings.getType("standing-banner"), MaterialMappings.getType("wall-banner"), MaterialMappings.getType("banner"),
            MaterialMappings.getTypeIfAny("red-banner"), MaterialMappings.getTypeIfAny("green-banner"), MaterialMappings.getTypeIfAny("brown-banner"),
            MaterialMappings.getTypeIfAny("blue-banner"), MaterialMappings.getTypeIfAny("purple-banner"), MaterialMappings.getTypeIfAny("cyan-banner"),
            MaterialMappings.getTypeIfAny("light-gray-banner"), MaterialMappings.getTypeIfAny("gray-banner"), MaterialMappings.getTypeIfAny("pink-banner"),
            MaterialMappings.getTypeIfAny("lime-banner"), MaterialMappings.getTypeIfAny("yellow-banner"), MaterialMappings.getTypeIfAny("light-blue-banner"),
            MaterialMappings.getTypeIfAny("magenta-banner"), MaterialMappings.getTypeIfAny("orange-banner"), MaterialMappings.getTypeIfAny("white-banner")),
    UNBREAKABLE_ITEM(ItemValidation.UnbreakableItemItemValidation::new),
    CUSTOM_POTION_EFFECTS(ItemValidation.CustomPotionEffectsItemValidation::new, Material.POTION),
    ATTRIBUTE_MODIFIERS(ItemValidation.AttributeModifiersItemValidation::new),
    BOOK_NO_TEXT_COMPONENT(ItemValidation.BookTranslationComponents::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    INVALID_BOOK_CONTENT(ItemValidation.InvalidBookContentValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAP_RANGE(ItemValidation.MapRangeItemValidation::new, MaterialMappings.getTypeIfAny("empty-map"), MaterialMappings.getTypeIfAny("filled-map")),

    // nbt
    MAX_NBT_LENGTH(ItemValidation.MaxNBTLengthItemValidation::new),
    MAX_NBT_STRING_LENGTH(ItemValidation.MaxNBTStringLengthItemValidation::new),
    MAX_BOOK_PAGE_LENGTH(ItemValidation.MaxPageLengthItemValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_NBT_LENGTH(ItemValidation.MaxBookNBTLengthItemValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_PAGES(ItemValidation.MaxBookPagesItemValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_AUTHOR_LENGTH(ItemValidation.MaxBookAuthorLengthItemValidation::new, Material.WRITTEN_BOOK),
    MAX_BOOK_TITLE_LENGTH(ItemValidation.MaxBookTitleLengthItemValidation::new, Material.WRITTEN_BOOK),
    MAX_NBT_BASES(ItemValidation.MaxNBTBasesItemValidation::new),
    MAX_NBT_NESTING(ItemValidation.MaxNBTNestingItemValidation::new),
    MAX_FIREWORK_NBT_LENGTH(ItemValidation.MaxFireworkLengthItemValidation::new,
            MaterialMappings.getTypeIfAny("firework-rocket"), MaterialMappings.getTypeIfAny("firework-star")),
    ILLEGAL_SKULL_TEXTURES(ItemValidation.IllegalSkullTexturesItemValidation::new,
            MaterialMappings.getType("skull"), MaterialMappings.getType("skull-item"),
            MaterialMappings.getTypeIfAny("skeleton-skull"), MaterialMappings.getTypeIfAny("wither-skeleton-skull"),
            MaterialMappings.getTypeIfAny("player-head"), MaterialMappings.getTypeIfAny("zombie-head"),
            MaterialMappings.getTypeIfAny("creeper-head")),
    ILLEGAL_COMPONENT_STRINGS(ItemValidation.IllegalComponentStrings::new),
    ILLEGAL_NUMBERS(ItemValidation.IllegalNumbersItemValidation::new),

    ;

    private final ItemAnalysis.ItemValidation validation;
    private final Material[] forMaterials;
    private final String forTag;

    ItemValidationRegistry(Supplier<ItemAnalysis.ItemValidation> check, Material... availableMaterials) {
        this(check, "", availableMaterials);
    }

    ItemValidationRegistry(Supplier<ItemAnalysis.ItemValidation> validation, String tag, Material... availableMaterials) {
        this.validation = validation.get();
        this.forTag = tag;
        this.forMaterials = Arrays.stream(availableMaterials).filter(m -> m != Material.AIR).toArray(Material[]::new);
    }

    public ItemAnalysis.ItemValidation getValidation() {
        return validation;
    }

    public Material[] getMaterials() {
        return forMaterials;
    }

    public String getTag() {
        return forTag;
    }

    public boolean hasFilter() {
        return !forTag.isEmpty() || forMaterials.length > 0;
    }

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "-");
    }

}
