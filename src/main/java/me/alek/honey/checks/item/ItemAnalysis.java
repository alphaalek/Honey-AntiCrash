package me.alek.honey.checks.item;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import me.alek.honey.netty.modelwrappers.nbt.NBTFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.*;

public class ItemAnalysis {

    private static final Map<CheckRegistry, ItemChecker> checkRegistryLookup = new HashMap<>();
    private static final List<ItemChecker> disabledChecks = new ArrayList<>();

    private static final Map<Material, List<ItemChecker>> directAvailableMaterialChecks = new EnumMap<>(Material.class);
    private static final Map<String, List<ItemChecker>> directAvailableNBTChecks = new HashMap<>();
    private static final List<ItemChecker> globalAvailableChecks = new ArrayList<>();

    static {
        for (CheckRegistry registry : CheckRegistry.values()) {
            checkRegistryLookup.put(registry, registry.getCheck());
        }
    }

    public static void reload() {
        disabledChecks.clear();
        directAvailableMaterialChecks.clear();
        globalAvailableChecks.clear();

        for (Field field : ConfigurationOptions.getItemAnalysisSettings()) {

            ConfigurationOptions.ItemAnalysisSetting setting = field.getAnnotation(ConfigurationOptions.ItemAnalysisSetting.class);
            if (setting == null) {
                continue;
            }
            CheckRegistry registry = setting.value();
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
        for (CheckRegistry registry : CheckRegistry.values()) {

            if (disabledChecks.contains(registry.getCheck())) {
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

    public static boolean checkItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        NBTCompound nbt = NBTFactory.fromItem(item);
        Material material = item.getType();

        if (directAvailableMaterialChecks.containsKey(material)) {
            for (ItemChecker checker : directAvailableMaterialChecks.get(material)) {

                if (checkAcceptance(checker.getAcceptStrategy(), item, nbt) && checker.check(item, nbt)) {
                    return true;
                }
            }
        }
        if (nbt != null) {
            for (String key : nbt.getKeys()) {
                if (directAvailableNBTChecks.containsKey(key)) {

                    for (ItemChecker checker : directAvailableNBTChecks.get(key)) {

                        if (checkAcceptance(checker.getAcceptStrategy(), item, nbt) && checker.check(item, nbt)) {
                            return true;
                        }
                    }
                }
            }
        }
        for (ItemChecker checker : globalAvailableChecks) {

            if (checkAcceptance(checker.getAcceptStrategy(), item, nbt) && checker.check(item, nbt)) {
                return true;
            }
        }

        return false;
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
