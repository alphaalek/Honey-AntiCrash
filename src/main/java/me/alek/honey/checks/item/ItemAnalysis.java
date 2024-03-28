package me.alek.honey.checks.item;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.item.enums.AcceptStrategy;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.*;

public class ItemAnalysis {

    private static final Map<ItemCheckRegistry, ItemChecker> checkRegistryLookup = new HashMap<>();
    private static final Map<Class<? extends ItemChecker>, ItemCheckRegistry> reversedCheckRegistryLookup = new HashMap<>();
    private static final List<ItemChecker> disabledChecks = new ArrayList<>();
    private static List<String> ignoredPlayers;

    private static final Map<Material, Set<ItemChecker>> directAvailableMaterialChecks = new EnumMap<>(Material.class);
    private static final Map<String, Set<ItemChecker>> directAvailableNBTChecks = new HashMap<>();
    private static final List<ItemChecker> globalAvailableChecks = new ArrayList<>();

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

        for (Field field : ConfigurationOptions.getItemAnalysisFields()) {

            ConfigurationOptions.ItemAnalysisSetting setting = field.getAnnotation(ConfigurationOptions.ItemAnalysisSetting.class);
            if (setting == null) {
                continue;
            }
            ItemCheckRegistry registry = setting.value();
            try {
                if (field.getType() == int.class) {
                    checkRegistryLookup.get(registry).setValue(field.getInt(null));
                }
                else if (field.getType() == boolean.class) {
                    if (!field.getBoolean(null)) {
                        disabledChecks.add(checkRegistryLookup.get(setting.value()));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
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
                    if (material.equals(Material.AIR)) continue;

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

    public static boolean checkItem(HoneyPlayer player, ItemStack item, CheckContextContainer.DevelopmentData context) {
        if (!ConfigurationOptions.ItemAnalysis.General.ENABLED
                || item == null || item.getType() == Material.AIR) {
            return false;
        }
        NBTCompound nbt = NBTFactory.fromItem(item);
        return checkItem(player, item, nbt, context);
    }

    public static boolean checkItem(HoneyPlayer player, ItemStack item, NBTCompound nbt, CheckContextContainer.DevelopmentData context) {
        for (final String ignoredPlayer : ignoredPlayers) {
            if (player.name.equalsIgnoreCase(ignoredPlayer) || player.uuid.toString().equalsIgnoreCase(ignoredPlayer)) {
                return false;
            }
        }
        ItemCheckRegistry flaggedCheck = checkItem(item, nbt);
        if (flaggedCheck == null) {
            return false;
        }
        context.addPoint("Type", flaggedCheck.name());

        HoneyPlugin.get().getConfiscationService().confiscate(player, item, nbt, flaggedCheck);
        return true;
    }

    private static ItemCheckRegistry checkItem(ItemStack item, NBTCompound nbt) {
        final Material material = item.getType();

        if (!material.equals(Material.AIR) && directAvailableMaterialChecks.containsKey(material)) {
            for (final ItemChecker checker : directAvailableMaterialChecks.get(material)) {
                ItemCheckRegistry registry = check(checker, item, nbt);
                if (registry != null) return registry;
            }
        }
        if (nbt != null) {
            for (final String key : nbt.getKeys()) {
                if (directAvailableNBTChecks.containsKey(key)) {
                    for (final ItemChecker checker : directAvailableNBTChecks.get(key)) {
                        ItemCheckRegistry registry = check(checker, item, nbt);
                        if (registry != null) return registry;
                    }
                }
            }
        }
        for (final ItemChecker checker : globalAvailableChecks) {
            ItemCheckRegistry registry = check(checker, item, nbt);
            if (registry != null) return registry;
        }
        return null;
    }

    private static ItemCheckRegistry check(ItemChecker checker, ItemStack item, NBTCompound nbt) {
        try {
            if (checkStrategyAcceptance(checker.getAcceptStrategy(), item, nbt) && checker.check(item, nbt)) {
                return getRegistryFrom(checker);
            }
        } catch (Exception ex) {
            HoneyPlugin.get().getLoggerService().getLogger().severe("Error occurred at item analysis check");
            ex.printStackTrace();
        }
        return null;
    }

    private static ItemCheckRegistry getRegistryFrom(ItemChecker checker) {
        Class<? extends ItemChecker> clazz = checker.getClass();
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

    public interface ItemChecker {

        boolean check(ItemStack item, NBTCompound nbt);

        void setValue(int value);

        AcceptStrategy getAcceptStrategy();
    }
}
