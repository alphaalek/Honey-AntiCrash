package me.alek.honey.checks;

import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.configuration.ConfigurationOptions;

public class Settings {

    private static ConfigurationOptions.VlSetting[] settings = new ConfigurationOptions.VlSetting[0];
    private static final Object lock = new Object();

    public static ConfigurationOptions.VlSetting getSetting(HoneyCheck check, Class<? extends ConfigurationOptions.VlSetting> settingClass) {
        if (check.getID() >= settings.length) {
            synchronized (lock) {
                if (check.getID() >= settings.length) {
                    ConfigurationOptions.VlSetting[] copy = settings;
                    settings = new ConfigurationOptions.VlSetting[check.getID() + 1];
                    System.arraycopy(copy, 0, settings, 0, copy.length);
                }
            }
        }
        if (settings[check.getID()] == null) {
            return settings[check.getID()] = ConfigurationOptions.getVlSetting(settingClass);
        }

        return settings[check.getID()];
    }
}
