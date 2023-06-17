package me.alek.honey.checks.types;

import me.alek.honey.checks.types.Check;
import me.alek.honey.configuration.ConfigurationOptions;

public interface IntervalCheck extends Check {

    ConfigurationOptions.IntervalSetting getSetting();
}
