package me.alek.honey.checks.item;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.modelwrappers.nbt.*;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemChecks {


    public static class MaxContainerCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxEnchantmentLevelCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxAmountCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxDisplayNameLengthCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxTotalLoreLengthCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class IllegalEnchantmentsCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class IllegalFireworkCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class IllegalPotionsCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxNBTLengthCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxFireworkLengthCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxBookNBTLengthCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxBookPagesCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxBookPageLengthCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxNBTBasesCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }

    public static class MaxNBTNestingCheck implements ItemAnalysis.ItemChecker {

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            return checkCompoundNBT(nbt, new AtomicInteger(), 0);
        }

        @Override
        public void setValue(int value) {
        }

        private static boolean checkCompoundNBT(NBTCompound tag, AtomicInteger totalBases, int nestingContext) {
            nestingContext++;
            if (checkValues(totalBases.get(), nestingContext)) {
                return true;
            }
            Set<String> keys = tag.getKeys();
            if (keys == null || keys.isEmpty()) {
                return false;
            }
            for (final String key : keys) {
                if (checkValues(totalBases.incrementAndGet(), nestingContext)) {
                    return true;
                }
                if (tag.hasKeyOfType(key, NBTType.LIST) || tag.hasKeyOfType(key, NBTType.LISTOFCOMOUND)) {
                    NBTAbstractList<?> list = tag.getAbstractList(key);
                    if (list == null) {
                        continue;
                    }
                    if (checkListNBT(list, totalBases, nestingContext)) {
                        return true;
                    }
                    continue;
                }
                if (tag.hasKeyOfType(key, NBTType.COMPOUND)) {
                    NBTCompound compound = tag.getCompound(key);
                    if (compound == null) {
                        continue;
                    }
                    if (checkCompoundNBT(compound, totalBases, nestingContext)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean checkListNBT(NBTAbstractList<?> list, AtomicInteger totalBases, int nestingContext) {
            nestingContext++;
            if (checkValues(totalBases.get(), nestingContext)) {
                return true;
            }
            for (int i = 0; i < list.size(); i++) {
                if (checkValues(totalBases.incrementAndGet(), nestingContext)) {
                    return true;
                }
                NBTBase<?, ?> base = list.getValue().getConverted(i);
                if (base == null) {
                    continue;
                }
                if (base.getType() == NBTType.COMPOUND) {
                    if (checkCompoundNBT((NBTCompound) base, totalBases, nestingContext)) {
                        return true;
                    }
                    continue;
                }
                if (base.getType() == NBTType.LIST || base.getType() == NBTType.LISTOFCOMOUND) {
                    if (checkListNBT((NBTAbstractList<?>) base, totalBases, nestingContext)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean checkValues(int totalBases, int nestingContext) {
            return totalBases > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_BASES || nestingContext > ConfigurationOptions.ItemAnalysis.NBT.MAX_NBT_NESTING;
        }
    }

    public static class IllegalSkullTexturesCheck implements ItemAnalysis.ItemChecker {

        private static final Pattern URL_PATTERN = Pattern.compile("url");

        @Override
        public boolean check(ItemStack item, NBTCompound nbt) {
            if (!nbt.hasKey("SkullOwner")) {
                return false;
            }
            NBTCompound skullOwnerCompound = nbt.getCompound("SkullOwner");
            if (!skullOwnerCompound.hasKey("Properties")) {
                return false;
            }
            NBTCompound propertiesCompound = skullOwnerCompound.getCompound("Properties");
            if (!propertiesCompound.hasKey("textures")) {
                return false;
            }
            NBTListOfCompound textures = propertiesCompound.getListOfCompound("textures");
            for (NBTCompound texture : textures.getValues()) {

               if (!texture.containsKey("Value")) {
                    return true;
                }
                String value = texture.getString("Value");
                String base64;
                try {
                    base64 = new String(Base64.getDecoder().decode(value));
                } catch (IllegalArgumentException ex) {
                    return true;
                }
                final String formatted = base64.trim().replace(" ", "").replace("\"", "").toLowerCase();
                final Matcher matcher = URL_PATTERN.matcher(formatted);

                while (matcher.find()) {
                    final String url = formatted.substring(matcher.end() + 1).split("\n")[0];
                    if (!url.startsWith("http://textures.minecraft.net") && !url.startsWith("https://textures.minecraft.net")) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void setValue(int value) {

        }
    }
}
