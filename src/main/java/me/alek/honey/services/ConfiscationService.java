package me.alek.honey.services;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.item.ItemCheckRegistry;
import me.alek.honey.checks.item.enums.ConfiscationStrategy;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.nbt.NBTCompound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

public class ConfiscationService {

    private ConfiscationStrategy confiscationStrategy;

    public ConfiscationService() {
        reload();
    }

    public void reload() {
        try {
            confiscationStrategy = ConfiscationStrategy.valueOf(ConfigurationOptions.ItemAnalysis.General.CONFISCATION_STRATEGY.toUpperCase());
        } catch (Exception ex) {
            HoneyPlugin.get().getPlugin().getLogger().log(Level.SEVERE, "You have wrote something badly in the config.yml! Delete the file and let it generate a new one.");
            confiscationStrategy = ConfiscationStrategy.LENIENT;
        }
    }

    public void confiscate(Player player, ItemStack item, NBTCompound nbt, ItemCheckRegistry check) {
        switch (confiscationStrategy) {

            case LENIENT:
                confiscateLenient(item, nbt, check);
                break;
            case STRICT:
                player.getInventory().setContents(new ItemStack[0]);
                player.getInventory().setArmorContents(new ItemStack[0]);
                break;
            case AVERAGE:
                player.getInventory().remove(item);
                item.setType(Material.AIR);
                break;
        }
    }

    private void confiscateLenient(ItemStack item, NBTCompound nbt, ItemCheckRegistry check) {
        switch (check) {
            case MAX_AMOUNT_CHECK:
                item.setAmount(ConfigurationOptions.ItemAnalysis.MAX_AMOUNT);
                break;
            case MAX_DISPLAYNAME_LENGTH_CHECK:
                item.getItemMeta().setDisplayName(item.getItemMeta().getDisplayName().substring(0, ConfigurationOptions.ItemAnalysis.MAX_DISPLAYNAME_LENGTH));
                break;
            case MAX_ENCHANTMENT_LEVEL_CHECK:
                item.getItemMeta().getEnchants().entrySet()
                        .stream()
                        .filter((entry) -> entry.getValue() > ConfigurationOptions.ItemAnalysis.MAX_ENCHANTMENT_LEVEL)
                        .forEach((entry) -> entry.setValue(ConfigurationOptions.ItemAnalysis.MAX_ENCHANTMENT_LEVEL));
                break;
            case MAX_TOTAL_LORE_LENGTH_CHECK:
                int counter = 0;
                int loreIndex = 0;
                for (String lore : item.getItemMeta().getLore()) {
                    counter += lore.length();

                    if (counter > ConfigurationOptions.ItemAnalysis.MAX_TOTAL_LORE_LENGTH) {
                        break;
                    }
                    loreIndex++;
                }
                item.getItemMeta().getLore().subList(0, loreIndex);
                break;
            case UNBREAKABLE_ITEM_CHECK:
                nbt.remove("Unbreakable");
                break;
            case ATTRIBUTE_MODIFIERS_CHECK:
                nbt.remove("AttributeModifiers");
                break;
            case CUSTOM_POTION_EFFECTS_CHECK:
                nbt.remove("CustomPotionEffects");
                break;
            default:
                nbt.clear();
        }
    }
}
