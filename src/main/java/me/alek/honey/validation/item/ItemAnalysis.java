package me.alek.honey.validation.item;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.util.Callback;
import me.alek.honey.validation.ValidationPipelineContext;
import me.alek.honey.validation.ValidationViolationContext;
import me.alek.honey.validation.ViolationKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.*;
import java.util.logging.Level;

public class ItemAnalysis {

    private static final Map<Class<? extends ItemValidation>, ItemValidationRegistry> validationRegistryLookup = new HashMap<>();
    private static final List<String> ignoredPlayers = new ArrayList<>();

    private static final Map<ItemType, Set<ItemValidation>> validationsForType = new HashMap<>();
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
        validationsForType.clear();
        validationsForAll.clear();

        ignoredPlayers.clear();
        ignoredPlayers.addAll(ConfigurationOptions.ItemAnalysis.General.IGNORED_PLAYERS);

        for (ItemValidationRegistry registry : ItemValidationRegistry.values()) {
            if (!registry.hasFilter()) {
                validationsForAll.add(registry.getValidation());
                continue;
            }

            ItemType[] types = registry.getTypes();
            String tag = registry.getTag();
            if (types.length > 0) {
                for (ItemType type : types) {
                    validationsForType.computeIfAbsent(type, __ -> new HashSet<>())
                            .add(registry.getValidation());
                }
            } else if (!tag.isEmpty()) {
                validationsForCompoundKey.computeIfAbsent(tag, __ -> new HashSet<>())
                        .add(registry.getValidation());
            }
        }
    }

    public static ValidationViolationContext flagIf(HoneyPlayer player, WrappedImmutableItemStack item, ViolationKey keyOuter, ValidationPipelineContext context) {
        return flagIf(player, item, keyOuter, new Callback(), context);
    }

    public static ValidationViolationContext flagIf(HoneyPlayer player, WrappedImmutableItemStack item, ViolationKey keyOuter, Callback callback, ValidationPipelineContext context) {
        if (ItemAnalysis.checkItem(player, item, callback)) {
            return ValidationViolationContext.Builder.validate()
                    .cancel().flag()
                    .onViolation(keyOuter, (keyInner, onViolation) -> {
                        if (onViolation != null) onViolation.run();

                        callback.call(context.getDevelopmentData(keyInner));
                    }).build();
        }

        return ValidationViolationContext.PASS;
    }

    public static boolean checkItem(HoneyPlayer player, WrappedImmutableItemStack item, Callback callback) {
        if (!ConfigurationOptions.ItemAnalysis.General.ENABLED
                || item == null
                || item.getType() == ItemTypes.AIR) {
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
            HoneyPlugin.get().getLogger().getLogger().info("DEBUG: item analysis flagged " + player.name + ", " + flagged);
        }

        return true;
    }

    private static ItemValidationRegistry checkItem(HoneyPlayer player, WrappedImmutableItemStack item) {
        ItemType type = item.getType();
        if (validationsForType.containsKey(type)) {
            for (ItemValidation validation : validationsForType.get(type)) {
                ItemValidationRegistry registry = checkValidation(player, validation, item);
                if (registry != null) {
                    return registry;
                }
            }
        }

        // TODO: fix for component
        if (item.getTag().asNBT().isPresent()) {
            for (String key : item.getTag().asNBT().get().getKeys()) {
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
            // TODO: fix for component
            if (!item.getTag().isEmpty()
                    && item.getTag().asNBT().isPresent()
                    && validation.validate(player, item, item.getTag().asNBT().get())) {
                return getRegistryFrom(validation);
            }
        } catch (Exception ex) {
            throw new DecoderException("Error occurred at item validation", ex);
        }

        return null;
    }

    private static ItemValidationRegistry getRegistryFrom(ItemValidation checker) {
        Class<? extends ItemValidation> clazz = checker.getClass();

        return validationRegistryLookup.get(clazz);
    }

    public interface ItemValidation {

        boolean validate(HoneyPlayer player, WrappedImmutableItemStack item, CompoundTag nbt);
    }
}
