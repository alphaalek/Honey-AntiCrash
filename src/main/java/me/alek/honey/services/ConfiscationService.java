package me.alek.honey.services;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.validation.item.ItemCheckRegistry;
import me.alek.honey.validation.item.enums.ConfiscationStrategy;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.nbt.NBTCompound;
import org.bukkit.Material;
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

    public void confiscate(HoneyPlayer player, ItemStack item, NBTCompound nbt, ItemCheckRegistry check) {
        switch (confiscationStrategy) {

            case NOTHING:
                break;
            case LENIENT:
                confiscateLenient(item, nbt, check);
                break;
            case STRICT:
                player.bukkit.getInventory().setContents(new ItemStack[0]);
                player.bukkit.getInventory().setArmorContents(new ItemStack[0]);
                break;
            case AVERAGE:
                player.bukkit.getInventory().remove(item);
                item.setType(Material.AIR);
                break;
        }
    }

    private void confiscateLenient(ItemStack item, NBTCompound nbt, ItemCheckRegistry check) {
        switch (check) {
            case MAX_AMOUNT:
                item.setAmount(ConfigurationOptions.ItemAnalysis.MAX_AMOUNT);
                break;
            case MAX_DISPLAYNAME_LENGTH:
                item.getItemMeta().setDisplayName(item.getItemMeta().getDisplayName().substring(0, ConfigurationOptions.ItemAnalysis.MAX_DISPLAYNAME_LENGTH));
                break;
            case MAX_ENCHANTMENT_LEVEL:
                item.getItemMeta().getEnchants().entrySet()
                        .stream()
                        .filter((entry) -> entry.getValue() > ConfigurationOptions.ItemAnalysis.MAX_ENCHANTMENT_LEVEL)
                        .forEach((entry) -> entry.setValue(ConfigurationOptions.ItemAnalysis.MAX_ENCHANTMENT_LEVEL));
                break;
            case MAX_TOTAL_LORE_LENGTH:
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
            case UNBREAKABLE_ITEM:
                nbt.remove("Unbreakable");
                break;
            case ATTRIBUTE_MODIFIERS:
                nbt.remove("AttributeModifiers");
                break;
            case CUSTOM_POTION_EFFECTS:
                nbt.remove("CustomPotionEffects");
                break;
            default:
                nbt.clear();
        }
    }
}
