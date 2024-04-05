package me.alek.honey.checks;

import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.util.Array;

public class Settings {

    private static final Array<ConfigurationOptions.VlSetting> settingArray = new Array<>();

    public static ConfigurationOptions.VlSetting getSetting(HoneyCheck check, Class<? extends ConfigurationOptions.VlSetting> settingClass) {
        return settingArray.get(check.getID(), () -> ConfigurationOptions.getVlSetting(settingClass));
    }
}
