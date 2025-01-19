package me.alek.honey.validation.item;

import me.alek.honey.util.MaterialMappings;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.function.Supplier;

public enum ItemValidationRegistry {
    MAX_ENCHANTMENT_LEVEL(ItemValidations.MaxEnchantmentLevelItemValidation::new),
    MAX_AMOUNT(ItemValidations.MaxAmountItemValidation::new),
    MAX_DISPLAYNAME_LENGTH(ItemValidations.MaxDisplayNameLengthItemValidation::new),
    MAX_TOTAL_LORE_LENGTH(ItemValidations.MaxTotalLoreLengthItemValidation::new),
    ILLEGAL_CHARACTER(ItemValidations.IllegalCharacterItemValidation::new),
    ILLEGAL_ENCHANTMENTS(ItemValidations.IllegalEnchantmentsItemValidation::new),
    MAX_FIREWORK_EXPLOSIONS(ItemValidations.MaxFireworkExplosionsItemValidation::new,
            MaterialMappings.getTypeIfAny("firework-rocket")),
    MAX_BANNER_PATTERNS(ItemValidations.MaxBannerPatternsItemValidation::new,
            MaterialMappings.getType("standing-banner"), MaterialMappings.getType("wall-banner"), MaterialMappings.getType("banner"),
            MaterialMappings.getTypeIfAny("red-banner"), MaterialMappings.getTypeIfAny("green-banner"), MaterialMappings.getTypeIfAny("brown-banner"),
            MaterialMappings.getTypeIfAny("blue-banner"), MaterialMappings.getTypeIfAny("purple-banner"), MaterialMappings.getTypeIfAny("cyan-banner"),
            MaterialMappings.getTypeIfAny("light-gray-banner"), MaterialMappings.getTypeIfAny("gray-banner"), MaterialMappings.getTypeIfAny("pink-banner"),
            MaterialMappings.getTypeIfAny("lime-banner"), MaterialMappings.getTypeIfAny("yellow-banner"), MaterialMappings.getTypeIfAny("light-blue-banner"),
            MaterialMappings.getTypeIfAny("magenta-banner"), MaterialMappings.getTypeIfAny("orange-banner"), MaterialMappings.getTypeIfAny("white-banner")),
    UNBREAKABLE_ITEM(ItemValidations.UnbreakableItemItemValidation::new),
    CUSTOM_POTION_EFFECTS(ItemValidations.CustomPotionEffectsItemValidation::new, Material.POTION),
    ATTRIBUTE_MODIFIERS(ItemValidations.AttributeModifiersItemValidation::new),
    BOOK_NO_TEXT_COMPONENT(ItemValidations.BookComponentsValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAP_RANGE(ItemValidations.MapRangeItemValidation::new, MaterialMappings.getTypeIfAny("empty-map"), MaterialMappings.getTypeIfAny("filled-map")),

    // nbt
    MAX_NBT_LENGTH(ItemValidations.MaxNBTLengthItemValidation::new),
    MAX_NBT_STRING_LENGTH(ItemValidations.MaxNBTStringLengthItemValidation::new),
    MAX_BOOK_PAGE_LENGTH(ItemValidations.MaxPageLengthItemValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_NBT_LENGTH(ItemValidations.MaxBookNBTLengthItemValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_PAGES(ItemValidations.MaxBookPagesItemValidation::new,
            Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_AUTHOR_LENGTH(ItemValidations.MaxBookAuthorLengthItemValidation::new, Material.WRITTEN_BOOK),
    MAX_BOOK_TITLE_LENGTH(ItemValidations.MaxBookTitleLengthItemValidation::new, Material.WRITTEN_BOOK),
    MAX_NBT_BASES(ItemValidations.MaxNBTBasesItemValidation::new),
    MAX_NBT_NESTING(ItemValidations.MaxNBTNestingItemValidation::new),
    MAX_FIREWORK_NBT_LENGTH(ItemValidations.MaxFireworkLengthItemValidation::new,
            MaterialMappings.getTypeIfAny("firework-rocket"), MaterialMappings.getTypeIfAny("firework-star")),
    ILLEGAL_SKULL_TEXTURES(ItemValidations.IllegalSkullTexturesItemValidation::new,
            MaterialMappings.getType("skull"), MaterialMappings.getType("skull-item"),
            MaterialMappings.getTypeIfAny("skeleton-skull"), MaterialMappings.getTypeIfAny("wither-skeleton-skull"),
            MaterialMappings.getTypeIfAny("player-head"), MaterialMappings.getTypeIfAny("zombie-head"),
            MaterialMappings.getTypeIfAny("creeper-head")),
    ILLEGAL_COMPONENT_STRINGS(ItemValidations.IllegalComponentStrings::new),
    ILLEGAL_NUMBERS(ItemValidations.IllegalNumbersItemValidation::new),
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
        return this.validation;
    }

    public Material[] getMaterials() {
        return this.forMaterials;
    }

    public String getTag() {
        return this.forTag;
    }

    public boolean hasFilter() {
        return !this.forTag.isEmpty() || this.forMaterials.length > 0;
    }

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "-");
    }
}
