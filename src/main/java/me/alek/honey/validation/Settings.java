package me.alek.honey.validation;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.validation.types.HoneyValidation;
import me.alek.honey.validation.types.KeyedViolationValidation;
import me.alek.honey.validation.types.ViolationValidation;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.services.ViolationService;
import me.alek.honey.util.Array;

public class Settings {

    private static final Array<ConfigurationOptions.ViolationSetting> settingArray = new Array<>();

    public static ConfigurationOptions.ViolationSetting getSetting(HoneyValidation<?> check, Class<? extends ConfigurationOptions.ViolationSetting> settingClass) {
        return settingArray.get(check.getID(), () -> ConfigurationOptions.getVlSetting(settingClass));
    }

    public static ConfigurationOptions.ViolationSetting getSetting(HoneyValidation<?> check) {
        if (!check.isViolation()) {
            return null;
        }

        return ((ViolationValidation<?>) check).getSetting();
    }

    public static <WP extends WrappedPacket<WP>> ConfigurationOptions.ViolationSetting getSetting(HoneyPlayer player, HoneyValidation<?> check, ViolationService.HoneyKey key, WP packet) {
        if (!check.isViolation()) {
            return null;
        }

        ViolationValidation<?> violationCheck = (ViolationValidation<?>) check;
        if (packet != null && violationCheck.isKeyed()) {
            @SuppressWarnings("unchecked")
            KeyedViolationValidation<WP> keyedViolationCheck = (KeyedViolationValidation<WP>) violationCheck;
            ConfigurationOptions.ViolationSetting setting = keyedViolationCheck.getSetting(packet);
            ViolationService.HoneyKey distributionKey = keyedViolationCheck.getKey(packet);

            ViolationService.ViolationDistributor distributor = player.violationContext.getDistributor(key, setting);
            ViolationService.AbstractPacketLimitHandler handler = distributor.getPacketLimitHandler(distributionKey, false);

            if (handler != null && handler.getSetting() == setting) {
                return setting;
            }

            if (handler != null && handler.getSetting() != setting) {
                distributor.setSetting(distributionKey, setting);
                return setting;
            }

            distributor.createHandler(distributionKey, setting);
            return setting;
        }

        return violationCheck.getSetting();
    }
}
