package me.alek.honey.checks.item;

import me.alek.honey.netty.util.MaterialMappings;
import org.bukkit.Material;

import java.util.Optional;
import java.util.function.Supplier;

public enum ItemCheckRegistry {
    MAX_ENCHANTMENT_LEVEL(ItemChecks.MaxEnchantmentLevelCheck::new),
    MAX_AMOUNT(ItemChecks.MaxAmountCheck::new),
    MAX_DISPLAYNAME_LENGTH(ItemChecks.MaxDisplayNameLengthCheck::new),
    MAX_TOTAL_LORE_LENGTH(ItemChecks.MaxTotalLoreLengthCheck::new),
    ILLEGAL_CHARACTER(ItemChecks.IllegalCharacterCheck::new),
    ILLEGAL_ENCHANTMENTS(ItemChecks.IllegalEnchantmentsCheck::new),
    MAX_FIREWORK_EXPLOSIONS(ItemChecks.MaxFireworkExplosionsCheck::new, MaterialMappings.getTypeIfAny("firework-rocket")),
    MAX_BANNER_PATTERNS(ItemChecks.MaxBannerPatternsCheck::new,
            MaterialMappings.getType("standing-banner"), MaterialMappings.getType("wall-banner"), MaterialMappings.getType("banner"),
            MaterialMappings.getTypeIfAny("red-banner"), MaterialMappings.getTypeIfAny("green-banner"), MaterialMappings.getTypeIfAny("brown-banner"),
            MaterialMappings.getTypeIfAny("blue-banner"), MaterialMappings.getTypeIfAny("purple-banner"), MaterialMappings.getTypeIfAny("cyan-banner"),
            MaterialMappings.getTypeIfAny("light-gray-banner"), MaterialMappings.getTypeIfAny("gray-banner"), MaterialMappings.getTypeIfAny("pink-banner"),
            MaterialMappings.getTypeIfAny("lime-banner"), MaterialMappings.getTypeIfAny("yellow-banner"), MaterialMappings.getTypeIfAny("light-blue-banner"),
            MaterialMappings.getTypeIfAny("magenta-banner"), MaterialMappings.getTypeIfAny("orange-banner"), MaterialMappings.getTypeIfAny("white-banner")),
    UNBREAKABLE_ITEM(ItemChecks.UnbreakableItemCheck::new),
    CUSTOM_POTION_EFFECTS(ItemChecks.CustomPotionEffectsCheck::new, Material.POTION),
    ATTRIBUTE_MODIFIERS(ItemChecks.AttributeModifiersCheck::new),
    BOOK_NO_TEXT_COMPONENT(ItemChecks.BookTranslationComponents::new, Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAP_RANGE(ItemChecks.MapRangeCheck::new, MaterialMappings.getTypeIfAny("empty-map"), MaterialMappings.getTypeIfAny("filled-map")),

    // nbt
    MAX_NBT_LENGTH(ItemChecks.MaxNBTLengthCheck::new),
    MAX_NBT_STRING_LENGTH(ItemChecks.MaxNBTStringLengthCheck::new),
    MAX_BOOK_PAGE_LENGTH(ItemChecks.MaxPageLengthCheck::new, Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_NBT_LENGTH(ItemChecks.MaxBookNBTLengthCheck::new, Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_PAGES(ItemChecks.MaxBookPagesCheck::new, Material.WRITTEN_BOOK, MaterialMappings.getTypeIfAny("writable-book")),
    MAX_BOOK_AUTHOR_LENGTH(ItemChecks.MaxBookAuthorLengthCheck::new, Material.WRITTEN_BOOK),
    MAX_BOOK_TITLE_LENGTH(ItemChecks.MaxBookTitleLengthCheck::new, Material.WRITTEN_BOOK),
    MAX_NBT_BASES(ItemChecks.MaxNBTBasesCheck::new),
    MAX_NBT_NESTING(ItemChecks.MaxNBTNestingCheck::new),
    MAX_FIREWORK_NBT_LENGTH(ItemChecks.MaxFireworkLengthCheck::new, MaterialMappings.getTypeIfAny("firework-rocket"), MaterialMappings.getTypeIfAny("firework-star")),
    ILLEGAL_SKULL_TEXTURES(ItemChecks.IllegalSkullTexturesCheck::new,
            MaterialMappings.getType("skull"), MaterialMappings.getType("skull-item"),
            MaterialMappings.getTypeIfAny("skeleton-skull"), MaterialMappings.getTypeIfAny("wither-skeleton-skull"),
            MaterialMappings.getTypeIfAny("player-head"), MaterialMappings.getTypeIfAny("zombie-head"),
            MaterialMappings.getTypeIfAny("creeper-head")),
    ILLEGAL_COMPONENT_STRINGS(ItemChecks.IllegalComponentStrings::new),
    ILLEGAL_NUMBERS(ItemChecks.IllegalNumbersCheck::new),

    UNKNOWN(null)
    ;

    private final ItemAnalysis.Check itemCheck;
    private final Material[] availableMaterials;
    private final String tag;

    ItemCheckRegistry(Supplier<ItemAnalysis.Check> check, Material... availableMaterials) {
        this(check, "", availableMaterials);
    }

    ItemCheckRegistry(Supplier<ItemAnalysis.Check> check, String tag) {
        this(check, tag, new Material[0]);
    }

    ItemCheckRegistry(Supplier<ItemAnalysis.Check> check, String tag, Material... availableMaterials) {
        this.itemCheck = Optional.ofNullable(check).map(Supplier::get).orElse(null);
        this.tag = tag;
        this.availableMaterials = availableMaterials;
    }

    public ItemAnalysis.Check getCheck() {
        return itemCheck;
    }

    public Material[] getAvailableMaterials() {
        return availableMaterials;
    }

    public String getTag() {
        return tag;
    }

}
