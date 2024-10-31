package me.alek.honey.validation.item;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.netty.modelwrappers.WrappedItemStack;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ViolationKey;
import me.alek.honey.validation.item.enums.ItemValidationFilterStrategy;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.nbt.NBTCompound;
import me.alek.honey.netty.nbt.NBTFactory;
import me.alek.honey.util.Callback;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Level;

public class ItemAnalysis {

    private static final Map<Class<? extends ItemValidation>, ItemValidationRegistry> validationRegistryLookup = new HashMap<>();
    private static final List<String> ignoredPlayers = new ArrayList<>();

    private static final Map<Material, Set<ItemValidation>> validationsForMaterial = new EnumMap<>(Material.class);
    private static final Map<String, Set<ItemValidation>> validationsForCompoundKey = new HashMap<>();
    private static final List<ItemValidation> validationsForAll = new ArrayList<>();

    static {
        for (ItemValidationRegistry registry : ItemValidationRegistry.values()) {
            if (registry.getValidation() == null) {
                continue;
            }
            validationRegistryLookup.put(registry.getValidation().getClass(), registry);
        }
    }

    public static void reload() {
        validationsForMaterial.clear();
        validationsForAll.clear();

        ignoredPlayers.clear();
        ignoredPlayers.addAll(ConfigurationOptions.ItemAnalysis.General.IGNORED_PLAYERS);

        for (ItemValidationRegistry registry : ItemValidationRegistry.values()) {
            if (!registry.hasFilter()) {
                validationsForAll.add(registry.getValidation());
                continue;
            }

            Material[] materials = registry.getMaterials();
            String tag = registry.getTag();
            if (materials.length > 0) {
                for (Material material : materials) {
                    validationsForMaterial.computeIfAbsent(material, __ -> new HashSet<>())
                            .add(registry.getValidation());
                }
            } else if (!tag.isEmpty()) {
                validationsForCompoundKey.computeIfAbsent(tag, __ -> new HashSet<>())
                        .add(registry.getValidation());
            }
        }
    }

    public static ValidationServiceContext flagIf(HoneyPlayer player, ItemStack item, ViolationKey keyOuter, ValidationPipelineContext context) {
        Callback callback = new Callback();
        if (ItemAnalysis.checkItem(player, item, callback)) {
            return ValidationServiceContext.Builder.validate()
                    .cancel()
                    .flag()
                    .onViolation(keyOuter, (keyInner, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        callback.call(context.getDevelopmentData(keyInner));
                    }).build();
        }

        return ValidationServiceContext.PASS;
    }

    public static boolean checkItem(HoneyPlayer player, ItemStack item, Callback context) {
        if (!ConfigurationOptions.ItemAnalysis.General.ENABLED
                || item == null
                || item.getType() == Material.AIR) {
            return false;
        }

        NBTCompound nbt = NBTFactory.fromItem(item, false, false);
        return checkItem(player, item, nbt, context);
    }

    public static boolean checkItem(HoneyPlayer player, WrappedItemStack item, NBTCompound nbt, Callback callback) {
        for (String ignoredPlayer : ignoredPlayers) {
            if (player.name.equalsIgnoreCase(ignoredPlayer)
                    || player.uuid.toString().equalsIgnoreCase(ignoredPlayer)) {
                return false;
            }
        }

        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemValidationRegistry flagged = checkItem(player, item, nbt);
        if (flagged == null) {
            return false;
        }

        if (callback != null) {
            callback.addAction(Callback.Action.<ValidationPipelineContext.DevelopmentData>from(context -> context.addPoint("Type", flagged.toString())));
        }

        if (ConfigurationOptions.PacketLogger.DEBUG) {
            Bukkit.getLogger().info("DEBUG: item analysis flagged " + player.name + ", " + flagged);
        }

        HoneyPlugin.get().getConfiscationService().confiscate(player, item, nbt, flagged);
        return true;
    }

    private static ItemValidationRegistry checkItem(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
        Material material = item.getType();
        if (validationsForMaterial.containsKey(material)) {
            for (ItemValidation validation : validationsForMaterial.get(material)) {
                ItemValidationRegistry registry = check(player, validation, item, nbt);
                if (registry != null) {
                    return registry;
                }
            }
        }

        if (nbt != null) {
            for (String key : nbt.getKeys()) {
                if (validationsForCompoundKey.containsKey(key)) {
                    for (ItemValidation validation : validationsForCompoundKey.get(key)) {
                        ItemValidationRegistry registry = check(player, validation, item, nbt);
                        if (registry != null) {
                            return registry;
                        }
                    }
                }
            }
        }

        for (ItemValidation validation : validationsForAll) {
            ItemValidationRegistry registry = check(player, validation, item, nbt);
            if (registry != null) {
                return registry;
            }
        }

        return null;
    }

    private static ItemValidationRegistry check(HoneyPlayer player, ItemValidation validation, ItemStack item, NBTCompound nbt) {
        try {
            if (checkFilter(validation.getFilter(), item, nbt)
                    && validation.validate(player, item, nbt)) {
                return getRegistryFrom(validation);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error occurred at item validation", ex);
        }

        return null;
    }

    private static ItemValidationRegistry getRegistryFrom(ItemValidation checker) {
        Class<? extends ItemValidation> clazz = checker.getClass();

        return validationRegistryLookup.get(clazz);
    }

    private static boolean checkFilter(ItemValidationFilterStrategy strategy, ItemStack item, NBTCompound nbt) {
        switch (strategy) {
            case ALL:
                return true;
            case ONLY_WITH_NBT:
                return nbt != null;
            case ONLY_WITH_METADATA:
                return item.hasItemMeta();
        }

        return true;
    }

    public interface ItemValidation {

        boolean validate(HoneyPlayer player, ItemStack item, NBTCompound nbt);

        ItemValidationFilterStrategy getFilter();
    }
}
