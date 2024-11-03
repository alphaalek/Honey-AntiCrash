package me.alek.honey.validation.item;

import jdk.vm.ci.code.site.Call;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.util.Callback;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ValidationServiceContext;
import me.alek.honey.validation.ViolationKey;
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
        return flagIf(player, WrappedImmutableItemStack.fromBukkit(item), keyOuter, context);
    }

    public static ValidationServiceContext flagIf(HoneyPlayer player, WrappedImmutableItemStack item, ViolationKey keyOuter, ValidationPipelineContext context) {
        return flagIf(player, item, keyOuter, new Callback(), context);
    }

    public static ValidationServiceContext flagIf(HoneyPlayer player, WrappedImmutableItemStack item, ViolationKey keyOuter, Callback callback, ValidationPipelineContext context) {
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
        return checkItem(player, WrappedImmutableItemStack.fromBukkit(item), context);
    }

    public static boolean checkItem(HoneyPlayer player, WrappedImmutableItemStack item, Callback callback) {
        if (!ConfigurationOptions.ItemAnalysis.General.ENABLED
                || item == null
                || item.getType() == Material.AIR) {
            return false;
        }

        for (String ignoredPlayer : ignoredPlayers) {
            if (player.name.equalsIgnoreCase(ignoredPlayer)
                    || player.uuid.toString().equalsIgnoreCase(ignoredPlayer)) {
                return false;
            }
        }

        ItemValidationRegistry flagged = checkItem(player, item);
        if (flagged == null) {
            return false;
        }

        if (callback != null) {
            callback.addAction(Callback.Action.<ValidationPipelineContext.DevelopmentData>from(context -> context.addPoint("Type", flagged.toString())));
        }

        if (ConfigurationOptions.PacketLogger.DEBUG) {
            Bukkit.getLogger().info("DEBUG: item analysis flagged " + player.name + ", " + flagged);
        }

        return true;
    }

    private static ItemValidationRegistry checkItem(HoneyPlayer player, WrappedImmutableItemStack item) {
        Material material = item.getType();
        if (validationsForMaterial.containsKey(material)) {
            for (ItemValidation validation : validationsForMaterial.get(material)) {
                ItemValidationRegistry registry = checkValidation(player, validation, item);
                if (registry != null) {
                    return registry;
                }
            }
        }

        if (item.getNBT() != null) {
            for (String key : item.getNBT().getKeys()) {
                if (validationsForCompoundKey.containsKey(key)) {
                    for (ItemValidation validation : validationsForCompoundKey.get(key)) {
                        ItemValidationRegistry registry = checkValidation(player, validation, item);
                        if (registry != null) {
                            return registry;
                        }
                    }
                }
            }
        }

        for (ItemValidation validation : validationsForAll) {
            ItemValidationRegistry registry = checkValidation(player, validation, item);
            if (registry != null) {
                return registry;
            }
        }

        return null;
    }

    private static ItemValidationRegistry checkValidation(HoneyPlayer player, ItemValidation validation, WrappedImmutableItemStack item) {
        try {
            if (checkFilter(validation, item) && validation.validate(player, item, item.getNBT())) {
                return getRegistryFrom(validation);
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Error occurred at item validation", ex);
        }

        return null;
    }

    private static boolean checkFilter(ItemValidation validation, WrappedImmutableItemStack item) {
        switch (validation.getFilter()) {
            case ALL: return true;
            case WITH_NBT: return item.hasNBT();
            case WITH_META: return item.hasDefaultItemMeta();
            case WITH_SPECIFIC_META: return item.hasSpecificItemMeta();
            default: throw new IllegalArgumentException();
        }
    }

    private static ItemValidationRegistry getRegistryFrom(ItemValidation checker) {
        Class<? extends ItemValidation> clazz = checker.getClass();

        return validationRegistryLookup.get(clazz);
    }

    public interface ItemValidation {

        boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt);

        default ItemValidationFilterStrategy getFilter() {
            return ItemValidationFilterStrategy.ALL;
        }
    }
}
