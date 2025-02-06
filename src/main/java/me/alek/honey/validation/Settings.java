package me.alek.honey.validation;

import me.alek.honey.HoneyKey;
import me.alek.honey.HoneyPlayer;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.util.Array;

public class Settings {

    private static final Array<ConfigurationOptions.ViolationSetting> settings = new Array<>();

    public static ConfigurationOptions.ViolationSetting getSetting(ViolationValidation<?> validation, Class<? extends ConfigurationOptions.ViolationSetting> settingClass) {
        return settings.get(validation.getID(), () -> ConfigurationOptions.getVlSetting(settingClass));
    }

    public static ConfigurationOptions.ViolationSetting getSetting(HoneyValidation<?> validation) {
        if (!validation.isViolation()) {
            return null;
        }

        return ((ViolationValidation<?>) validation).getSetting();
    }

    public static <WP extends WrappedPacket<WP>> ConfigurationOptions.ViolationSetting getSetting(HoneyPlayer player, HoneyValidation<?> validation, HoneyKey key, WP packet) {
        if (!validation.isViolation()) {
            return null;
        }

        ViolationValidation<?> violationCheck = (ViolationValidation<?>) validation;
        if (packet != null && violationCheck.isKeyed()) {
            @SuppressWarnings("unchecked")
            KeyedVlValidation<WP> keyedViolationCheck = (KeyedVlValidation<WP>) violationCheck;
            ConfigurationOptions.ViolationSetting setting = keyedViolationCheck.getSetting(packet);
            HoneyKey distributionKey = keyedViolationCheck.getKey(packet);

            ViolationManagement.ViolationDistributor distributor = player.violationContext.getDistributor(key, setting);
            ViolationManagement.AbstractPacketLimitHandler handler = distributor.getPacketLimitHandler(distributionKey);

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
