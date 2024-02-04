package me.alek.honey.checks.item;

import me.alek.honey.netty.utility.reflect.MaterialMappings;
import org.bukkit.Material;

import java.util.Optional;
import java.util.function.Supplier;

public enum ItemCheckRegistry {
    MAX_CONTAINER_CHECK(ItemChecks.MaxContainerCheck::new,
            Material.CHEST, Material.TRAPPED_CHEST, Material.DROPPER, Material.HOPPER, Material.HOPPER_MINECART,
            MaterialMappings.getItemIfAny("storage-minecart"), Material.ENDER_CHEST, Material.DISPENSER),
    MAX_ENCHANTMENT_LEVEL_CHECK(ItemChecks.MaxEnchantmentLevelCheck::new),
    MAX_AMOUNT_CHECK(ItemChecks.MaxAmountCheck::new),
    MAX_DISPLAYNAME_LENGTH_CHECK(ItemChecks.MaxDisplayNameLengthCheck::new),
    MAX_TOTAL_LORE_LENGTH_CHECK(ItemChecks.MaxTotalLoreLengthCheck::new),
    ILLEGAL_CHARACTER_CHECK(ItemChecks.IllegalCharacterCheck::new),
    ILLEGAL_ENCHANTMENTS_CHECK(ItemChecks.IllegalEnchantmentsCheck::new),
    MAX_FIREWORK_EXPLOSIONS(ItemChecks.MaxFireworkExplosionsCheck::new, MaterialMappings.getItemIfAny("firework-rocket")),
    MAX_BANNER_PATTERNS(ItemChecks.MaxBannerPatternsCheck::new,
            MaterialMappings.getItem("standing-banner"), MaterialMappings.getItem("wall-banner"), MaterialMappings.getItem("banner"),
            MaterialMappings.getItemIfAny("red-banner"), MaterialMappings.getItemIfAny("green-banner"), MaterialMappings.getItemIfAny("brown-banner"),
            MaterialMappings.getItemIfAny("blue-banner"), MaterialMappings.getItemIfAny("purple-banner"), MaterialMappings.getItemIfAny("cyan-banner"),
            MaterialMappings.getItemIfAny("light-gray-banner"), MaterialMappings.getItemIfAny("gray-banner"), MaterialMappings.getItemIfAny("pink-banner"),
            MaterialMappings.getItemIfAny("lime-banner"), MaterialMappings.getItemIfAny("yellow-banner"), MaterialMappings.getItemIfAny("light-blue-banner"),
            MaterialMappings.getItemIfAny("magenta-banner"), MaterialMappings.getItemIfAny("orange-banner"), MaterialMappings.getItemIfAny("white-banner")),
    UNBREAKABLE_ITEM_CHECK(ItemChecks.UnbreakableItemCheck::new),
    CUSTOM_POTION_EFFECTS_CHECK(ItemChecks.CustomPotionEffectsCheck::new, Material.POTION),
    ATTRIBUTE_MODIFIERS_CHECK(ItemChecks.AttributeModifiersCheck::new),
    MAP_RANGE_CHECK(ItemChecks.MapRangeCheck::new, MaterialMappings.getItemIfAny("empty-map"), MaterialMappings.getItemIfAny("filled-map")),

    // nbt
    MAX_NBT_LENGTH(ItemChecks.MaxNBTLengthCheck::new),
    MAX_NBT_STRING_LENGTH(ItemChecks.MaxNBTStringLengthCheck::new),
    MAX_BOOK_PAGE_LENGTH(ItemChecks.MaxPageLengthCheck::new, Material.WRITTEN_BOOK, MaterialMappings.getItemIfAny("writable-book")),
    MAX_BOOK_NBT_LENGTH(ItemChecks.MaxBookNBTLengthCheck::new, Material.WRITTEN_BOOK, MaterialMappings.getItemIfAny("writable-book")),
    MAX_BOOK_PAGES(ItemChecks.MaxBookPagesCheck::new, Material.WRITTEN_BOOK, MaterialMappings.getItemIfAny("writable-book")),
    MAX_BOOK_TITLE_LENGTH(ItemChecks.MaxBookTitleLengthCheck::new, Material.WRITTEN_BOOK),
    MAX_BOOK_AUTHOR_LENGTH(ItemChecks.MaxBookAuthorLengthCheck::new, Material.WRITTEN_BOOK),
    MAX_NBT_BASES(ItemChecks.MaxNBTBasesCheck::new),
    MAX_NBT_NESTING(ItemChecks.MaxNBTNestingCheck::new),
    MAX_FIREWORK_NBT_LENGTH(ItemChecks.MaxFireworkLengthCheck::new, MaterialMappings.getItemIfAny("firework-rocket"), MaterialMappings.getItemIfAny("firework-star")),
    ILLEGAL_SKULL_TEXTURES(ItemChecks.IllegalSkullTexturesCheck::new,
            MaterialMappings.getItem("skull"), MaterialMappings.getItem("skull-item"),
            MaterialMappings.getItemIfAny("skeleton-skull"), MaterialMappings.getItemIfAny("wither-skeleton-skull"),
            MaterialMappings.getItemIfAny("player-head"), MaterialMappings.getItemIfAny("zombie-head"),
            MaterialMappings.getItemIfAny("creeper-head")),
    ILLEGAL_COMPONENT_STRINGS(ItemChecks.IllegalComponentStrings::new),
    ILLEGAL_NUMBERS(ItemChecks.IllegalNumbersCheck::new),

    UNKNOWN(null)
    ;

    private final ItemAnalysis.ItemChecker itemCheck;
    private final Material[] availableMaterials;
    private final String tag;

    ItemCheckRegistry(Supplier<ItemAnalysis.ItemChecker> check, Material... availableMaterials) {
        this(check, "", availableMaterials);
    }

    ItemCheckRegistry(Supplier<ItemAnalysis.ItemChecker> check, String tag) {
        this(check, tag, new Material[0]);
    }

    ItemCheckRegistry(Supplier<ItemAnalysis.ItemChecker> check, String tag, Material... availableMaterials) {
        this.itemCheck = Optional.ofNullable(check).map(Supplier::get).orElse(null);
        this.tag = tag;
        this.availableMaterials = availableMaterials;
    }

    public ItemAnalysis.ItemChecker getCheck() {
        return itemCheck;
    }

    public Material[] getAvailableMaterials() {
        return availableMaterials;
    }

    public String getTag() {
        return tag;
    }

}
