package me.alek.honey.checks;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.checks.types.HoneyValidation;
import me.alek.honey.checks.types.KeyedViolationValidation;
import me.alek.honey.checks.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.Array;

public class Settings {

    private static final Array<ConfigurationOptions.VlSetting> settingArray = new Array<>();

    public static ConfigurationOptions.VlSetting getSetting(HoneyValidation<?> check, Class<? extends ConfigurationOptions.VlSetting> settingClass) {
        return settingArray.get(check.getID(), () -> ConfigurationOptions.getVlSetting(settingClass));
    }

    public static ConfigurationOptions.VlSetting getSetting(HoneyValidation<?> check) {
        if (!check.isViolation()) {
            return null;
        }

        return ((ViolationValidation<?>) check).getSetting();
    }

    public static <WP extends WrappedPacket<WP>> ConfigurationOptions.VlSetting getSetting(HoneyPlayer player, HoneyValidation<?> check, ViolationService.HoneyKey key, WP packet) {
        if (!check.isViolation()) {
            return null;
        }

        ViolationValidation<?> violationCheck = (ViolationValidation<?>) check;
        if (packet != null && violationCheck.isKeyed()) {
            @SuppressWarnings("unchecked")
            KeyedViolationValidation<WP> keyedViolationCheck = (KeyedViolationValidation<WP>) violationCheck;
            ConfigurationOptions.VlSetting setting = keyedViolationCheck.getSetting(packet);
            ViolationService.HoneyKey violationKey = keyedViolationCheck.getKey(packet);

            ViolationService.ViolationDistributor distributor = player.violationContext.getDistributor(key, setting);
            ViolationService.AbstractPacketLimitHandler handler = distributor.getPacketLimitHandler(violationKey, false);

            if (handler != null && handler.getSetting() == setting) {
                return setting;
            }

            if (handler != null && handler.getSetting() != setting) {
                distributor.setSetting(violationKey, setting);
                return setting;
            }

            distributor.createHandler(violationKey, setting);
            return setting;
        }

        return violationCheck.getSetting();
    }
}
