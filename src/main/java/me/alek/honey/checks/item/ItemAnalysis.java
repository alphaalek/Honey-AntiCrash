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
    private static final List<ItemChecker> toggledChecks = new ArrayList<>();

    private static final Map<Material, List<ItemChecker>> directAvailableChecks = new EnumMap<>(Material.class);
    private static final List<ItemChecker> globalAvailableChecks = new ArrayList<>();

    static {
        for (CheckRegistry registry : CheckRegistry.values()) {
            checkRegistryLookup.put(registry, registry.getCheck());
        }
    }

    public static void reload() {
        toggledChecks.clear();
        directAvailableChecks.clear();
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
                        continue;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            toggledChecks.add(checkRegistryLookup.get(setting.value()));
        }

        for (CheckRegistry registry : CheckRegistry.values()) {

            if (!toggledChecks.contains(registry.getCheck())) {
                // den check er ikke slået til og skal derfor heller ikke tilføjes
                continue;
            }
            Material[] availableMaterials = registry.getAvailableMaterials();
            if (availableMaterials.length == 0) {
                globalAvailableChecks.add(registry.getCheck());
            }
            else {
                for (Material material : availableMaterials) {
                    if (!directAvailableChecks.containsKey(material)) {
                        directAvailableChecks.put(material, new ArrayList<>());
                    }
                    directAvailableChecks.get(material).add(registry.getCheck());
                }
            }
        }
    }

    public boolean checkItem(ItemStack item) {
        NBTCompound nbt = NBTFactory.fromItem(item);
        Material material = item.getType();

        if (directAvailableChecks.containsKey(material)) {
            for (ItemChecker checker : directAvailableChecks.get(material)) {

                if (checker.check(item, nbt)) {
                    return true;
                }
            }
        }
        for (ItemChecker checker : globalAvailableChecks) {

            if (checker.check(item, nbt)) {
                return true;
            }
        }
        return false;
    }

    public interface ItemChecker {

        boolean check(ItemStack item, NBTCompound nbt);

        void setValue(int value);

    }
}
