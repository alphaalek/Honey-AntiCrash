package me.alek.honey.checks.item;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.item.enums.AcceptStrategy;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.response.Validation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import me.alek.honey.util.CallbackAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemAnalysis {

    private static final Map<ItemCheckRegistry, Check> checkRegistryLookup = new HashMap<>();
    private static final Map<Class<? extends Check>, ItemCheckRegistry> reversedCheckRegistryLookup = new HashMap<>();
    private static final List<Check> disabledChecks = new ArrayList<>();
    private static List<String> ignoredPlayers;

    private static final Map<Material, Set<Check>> directAvailableMaterialChecks = new EnumMap<>(Material.class);
    private static final Map<String, Set<Check>> directAvailableNBTChecks = new HashMap<>();
    private static final List<Check> globalAvailableChecks = new ArrayList<>();

    static {
        for (ItemCheckRegistry registry : ItemCheckRegistry.values()) {
            if (registry.getCheck() == null) {
                continue;
            }
            checkRegistryLookup.put(registry, registry.getCheck());
            reversedCheckRegistryLookup.put(registry.getCheck().getClass(), registry);
        }
    }

    public static void reload() {
        disabledChecks.clear();
        directAvailableMaterialChecks.clear();
        globalAvailableChecks.clear();

        ignoredPlayers = ConfigurationOptions.ItemAnalysis.General.IGNORED_PLAYERS;

        for (ItemCheckRegistry registry : ItemCheckRegistry.values()) {
            if (registry.getCheck() == null || disabledChecks.contains(registry.getCheck())) {
                continue;
            }

            Material[] availableMaterials = registry.getAvailableMaterials();
            String nbtTag = registry.getTag();
            if (availableMaterials.length == 0 && nbtTag.isEmpty()) {
                globalAvailableChecks.add(registry.getCheck());
            }
            if (availableMaterials.length > 0) {
                for (Material material : availableMaterials) {
                    if (material.equals(Material.AIR)) {
                        continue;
                    }
                    if (!directAvailableMaterialChecks.containsKey(material)) {
                        directAvailableMaterialChecks.put(material, new HashSet<>());
                    }
                    directAvailableMaterialChecks.get(material).add(registry.getCheck());
                }
            }
            else if (!nbtTag.isEmpty()) {
                if (!directAvailableNBTChecks.containsKey(nbtTag)) {
                    directAvailableNBTChecks.put(nbtTag, new HashSet<>());
                }
                directAvailableNBTChecks.get(nbtTag).add(registry.getCheck());
            }
        }
    }

    public static ResponseContainer flagIf(HoneyPlayer player, ItemStack item, CheckContextContainer.DevelopmentData developmentData) {
        CallbackAction<CheckContextContainer.DevelopmentData> callback = new CallbackAction<>();
        if (ItemAnalysis.checkItem(player, item, callback)) {
            return Validation.inst().cancel().flag().onViolation(onViolation -> {
                if (onViolation != null) onViolation.run();

                callback.call(developmentData);
            }).build();
        }

        return Validation.PASS;
    }

    public static boolean checkItem(HoneyPlayer player, ItemStack item, CallbackAction<CheckContextContainer.DevelopmentData> context) {
        if (!ConfigurationOptions.ItemAnalysis.General.ENABLED
                || item == null || item.getType() == Material.AIR) {
            return false;
        }

        NBTCompound nbt = NBTFactory.fromItem(item, false, false);
        return checkItem(player, item, nbt, context);
    }

    public static boolean checkItem(HoneyPlayer player, ItemStack item, NBTCompound nbt, CallbackAction<CheckContextContainer.DevelopmentData> callback) {
        for (final String ignoredPlayer : ignoredPlayers) {
            if (player.name.equalsIgnoreCase(ignoredPlayer)
                    || player.uuid.toString().equalsIgnoreCase(ignoredPlayer)) {
                return false;
            }
        }

        ItemCheckRegistry flaggedCheck = checkItem(player, item, nbt);
        if (flaggedCheck == null) {
            return false;
        }

        if (callback != null) {
            callback.addAction(context -> context.addPoint("Type", flaggedCheck.name()));
        }

        if (ConfigurationOptions.PacketLogger.DEBUG) {
            Bukkit.getLogger().info("DEBUG: item analysis flagged " + player.name + ", " + flaggedCheck.name());
        }

        HoneyPlugin.get().getConfiscationService().confiscate(player, item, nbt, flaggedCheck);
        return true;
    }

    private static ItemCheckRegistry checkItem(HoneyPlayer player, ItemStack item, NBTCompound nbt) {
        final Material material = item.getType();
        if (!material.equals(Material.AIR)
                && directAvailableMaterialChecks.containsKey(material)) {
            for (final Check checker : directAvailableMaterialChecks.get(material)) {
                ItemCheckRegistry registry = check(player, checker, item, nbt);
                if (registry != null) return registry;
            }
        }

        if (nbt != null) {
            for (final String key : nbt.getKeys()) {
                if (directAvailableNBTChecks.containsKey(key)) {
                    for (final Check checker : directAvailableNBTChecks.get(key)) {
                        ItemCheckRegistry registry = check(player, checker, item, nbt);
                        if (registry != null) return registry;
                    }
                }
            }
        }

        for (final Check checker : globalAvailableChecks) {
            ItemCheckRegistry registry = check(player, checker, item, nbt);
            if (registry != null) return registry;
        }

        return null;
    }

    private static ItemCheckRegistry check(HoneyPlayer player, Check checker, ItemStack item, NBTCompound nbt) {
        try {
            if (checkStrategyAcceptance(checker.getAcceptStrategy(), item, nbt)
                    && checker.check(player, item, nbt)) {
                return getRegistryFrom(checker);
            }
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred at item analysis check");
            ex.printStackTrace();
        }
        return null;
    }

    private static ItemCheckRegistry getRegistryFrom(Check checker) {
        Class<? extends Check> clazz = checker.getClass();
        ItemCheckRegistry registry = reversedCheckRegistryLookup.get(clazz);

        if (registry == null) {
            return ItemCheckRegistry.UNKNOWN;
        }
        return registry;
    }

    private static boolean checkStrategyAcceptance(AcceptStrategy strategy, ItemStack item, NBTCompound nbt) {
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

    public interface Check {

        boolean check(HoneyPlayer player, ItemStack item, NBTCompound nbt);

        AcceptStrategy getAcceptStrategy();
    }
}
