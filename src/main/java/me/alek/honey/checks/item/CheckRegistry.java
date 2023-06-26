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
    ILLEGAL_FIREWORK_CHECK(ItemChecks.IllegalFireworkCheck::new, Material.FIREWORK),
    ILLEGAL_POTION_CHECK(ItemChecks.IllegalPotionsCheck::new, Material.POTION),

    // nbt
    MAX_NBT_LENGTH(ItemChecks.MaxNBTLengthCheck::new),
    MAX_FIREWORK_NBT_LENGTH(ItemChecks.MaxFireworkLengthCheck::new, Material.FIREWORK, Material.FIREWORK_CHARGE),
    MAX_BOOK_NBT_LENGTH(ItemChecks.MaxBookNBTLengthCheck::new, Material.WRITTEN_BOOK, Material.BOOK_AND_QUILL),
    MAX_BOOK_PAGES(ItemChecks.MaxBookPagesCheck::new, Material.WRITTEN_BOOK, Material.BOOK_AND_QUILL),
    MAX_BOOK_PAGE_LENGTH(ItemChecks.MaxBookPageLengthCheck::new, Material.WRITTEN_BOOK, Material.BOOK_AND_QUILL),
    MAX_NBT_BASES(ItemChecks.MaxNBTBasesCheck::new),
    MAX_NBT_NESTING(ItemChecks.MaxNBTNestingCheck::new),
    ILLEGAL_SKULL_TEXTURES(ItemChecks.IllegalSkullTexturesCheck::new, Material.SKULL, Material.SKULL_ITEM)

    ;

    private final ItemAnalysis.ItemChecker itemCheck;
    private final Material[] availableMaterials;

    CheckRegistry(Supplier<ItemAnalysis.ItemChecker> check, Material... availableMaterials) {
        this.itemCheck = check.get();
        this.availableMaterials = availableMaterials;
    }

    public ItemAnalysis.ItemChecker getCheck() {
        return itemCheck;
    }

    public Material[] getAvailableMaterials() {
        return availableMaterials;
    }

}
