package me.alek.honey.checks.item;

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

    private static final Map<Material, List<ItemChecker>> directAvailableMaterialChecks = new EnumMap<>(Material.class);
    private static final Map<String, List<ItemChecker>> directAvailableNBTChecks = new HashMap<>();
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
                        // lad være med at enable dem
                        disabledChecks.add(checkRegistryLookup.get(setting.value()));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        for (ItemCheckRegistry registry : ItemCheckRegistry.values()) {

            if (registry.getCheck() == null || disabledChecks.contains(registry.getCheck())) {
                // den check er ikke slået til og skal derfor heller ikke tilføjes
                continue;
            }
            Material[] availableMaterials = registry.getAvailableMaterials();
            String nbtTag = registry.getTag();
            if (availableMaterials.length == 0 && nbtTag.equals("")) {
                globalAvailableChecks.add(registry.getCheck());
            }
            if (availableMaterials.length > 0) {
                for (Material material : availableMaterials) {
                    if (!directAvailableMaterialChecks.containsKey(material)) {
                        directAvailableMaterialChecks.put(material, new ArrayList<>());
                    }
                    directAvailableMaterialChecks.get(material).add(registry.getCheck());
                }
            }
            else if (!nbtTag.equals("")) {
                if (!directAvailableNBTChecks.containsKey(nbtTag)) {
                    directAvailableNBTChecks.put(nbtTag, new ArrayList<>());
                }
                directAvailableNBTChecks.get(nbtTag).add(registry.getCheck());
            }
        }
    }

    public static boolean checkItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        for (final String ignoredPlayer : ignoredPlayers) {
            if (player.getName().equalsIgnoreCase(ignoredPlayer) || player.getUniqueId().toString().equalsIgnoreCase(ignoredPlayer)) {
                return false;
            }
        }
        final NBTCompound nbt = NBTFactory.fromItem(item);
        final ItemCheckRegistry flaggedCheck = checkItem(item, nbt);
        if (flaggedCheck == null) {
            return false;
        }
        Bukkit.getLogger().info("DEBUG: " + flaggedCheck.name());

        // HoneyPlugin.get().getConfiscationService().confiscate(player, item, nbt, flaggedCheck);
        return true;
    }

    private static ItemCheckRegistry checkItem(ItemStack item, NBTCompound nbt) {
        final Material material = item.getType();

        if (directAvailableMaterialChecks.containsKey(material)) {
            for (final ItemChecker checker : directAvailableMaterialChecks.get(material)) {

                if (checkAcceptance(checker.getAcceptStrategy(), item, nbt) && checker.check(item, nbt)) {
                    return getRegistryFrom(checker);
                }
            }
        }
        if (nbt != null) {
            for (final String key : nbt.getKeys()) {

                if (directAvailableNBTChecks.containsKey(key)) {
                    for (final ItemChecker checker : directAvailableNBTChecks.get(key)) {

                        if (checkAcceptance(checker.getAcceptStrategy(), item, nbt) && checker.check(item, nbt)) {
                            return getRegistryFrom(checker);
                        }
                    }
                }
            }
        }
        for (final ItemChecker checker : globalAvailableChecks) {

            if (checkAcceptance(checker.getAcceptStrategy(), item, nbt) && checker.check(item, nbt)) {
                return getRegistryFrom(checker);
            }
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

    private static boolean checkAcceptance(AcceptStrategy strategy, ItemStack item, NBTCompound nbt) {
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
