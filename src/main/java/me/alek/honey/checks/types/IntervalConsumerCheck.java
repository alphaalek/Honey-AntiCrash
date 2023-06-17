package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.function.Function;

public interface IntervalConsumerCheck<WP extends WrappedPacket<WP>> {

    Function<Class<WP>, ConfigurationOptions.IntervalSetting> getSetting();
}
