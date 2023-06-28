package me.alek.honey.checks.item;

import org.bukkit.Material;

import java.util.function.Supplier;

public enum CheckRegistry {
    MAX_CONTAINER_CHECK(ItemChecks.MaxContainerCheck::new,
            Material.CHEST, Material.TRAPPED_CHEST, Material.DROPPER, Material.HOPPER, Material.HOPPER_MINECART,
            Material.STORAGE_MINECART, Material.ENDER_CHEST, Material.DISPENSER),
    MAX_ENCHANTMENT_LEVEL_CHECK(ItemChecks.MaxEnchantmentLevelCheck::new),
    MAX_AMOUNT_CHECK(ItemChecks.MaxAmountCheck::new),
    MAX_DISPLAYNAME_LENGTH_CHECK(ItemChecks.MaxDisplayNameLengthCheck::new),
    MAX_TOTAL_LORE_LENGTH_CHECK(ItemChecks.MaxTotalLoreLengthCheck::new),
    ILLEGAL_CHARACTER_CHECK(ItemChecks.IllegalCharacterCheck::new),
    ILLEGAL_ENCHANTMENTS_CHECK(ItemChecks.IllegalEnchantmentsCheck::new),
    MAX_FIREWORK_EXPLOSIONS(ItemChecks.MaxFireworkExplosionsCheck::new, Material.FIREWORK),
    MAX_BANNER_PATTERNS(ItemChecks.MaxBannerPatternsCheck::new, Material.BANNER, Material.STANDING_BANNER, Material.WALL_BANNER),
    UNBREAKABLE_ITEM_CHECK(ItemChecks.UnbreakableItemCheck::new),
    CUSTOM_POTION_EFFECTS_CHECK(ItemChecks.CustomPotionEffectsCheck::new, Material.POTION),
    ATTRIBUTE_MODIFIERS_CHECK(ItemChecks.AttributeModifiersCheck::new),
    MAP_RANGE_CHECK(ItemChecks.MapRangeCheck::new, Material.MAP, Material.EMPTY_MAP),

    // nbt
    MAX_NBT_LENGTH(ItemChecks.MaxNBTLengthCheck::new),
    MAX_BOOK_PAGE_LENGTH(ItemChecks.MaxNBTStringLengthCheck::new),
    MAX_FIREWORK_NBT_LENGTH(ItemChecks.MaxFireworkLengthCheck::new, Material.FIREWORK, Material.FIREWORK_CHARGE),
    MAX_BOOK_NBT_LENGTH(ItemChecks.MaxBookNBTLengthCheck::new, Material.WRITTEN_BOOK, Material.BOOK_AND_QUILL),
    MAX_BOOK_PAGES(ItemChecks.MaxBookPagesCheck::new, Material.WRITTEN_BOOK, Material.BOOK_AND_QUILL),
    MAX_BOOK_TITLE_LENGTH(ItemChecks.MaxBookTitleLengthCheck::new, Material.WRITTEN_BOOK),
    MAX_BOOK_AUHTOR_LENGTH(ItemChecks.MaxBookAuthorLengthCheck::new, Material.WRITTEN_BOOK),
    MAX_NBT_BASES(ItemChecks.MaxNBTBasesCheck::new),
    MAX_NBT_NESTING(ItemChecks.MaxNBTNestingCheck::new),
    ILLEGAL_SKULL_TEXTURES(ItemChecks.IllegalSkullTexturesCheck::new, Material.SKULL, Material.SKULL_ITEM),
    ILLEGAL_COMPONENT_STRINGS(ItemChecks.IllegalComponentStrings::new),
    ILLEGAL_NUMBERS(ItemChecks.IllegalNumbersCheck::new)

    ;

    private final ItemAnalysis.ItemChecker itemCheck;
    private final Material[] availableMaterials;
    private final String tag;

    CheckRegistry(Supplier<ItemAnalysis.ItemChecker> check, Material... availableMaterials) {
        this(check, "", availableMaterials);
    }

    CheckRegistry(Supplier<ItemAnalysis.ItemChecker> check, String tag) {
        this(check, tag, new Material[0]);
    }

    CheckRegistry(Supplier<ItemAnalysis.ItemChecker> check, String tag, Material... availableMaterials) {
        this.itemCheck = check.get();
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
