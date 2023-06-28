package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

public interface ViolationConsumerCheck<WP extends WrappedPacket<WP>, T> {

    ConfigurationOptions.VlSetting getSetting(T key);

    T getKey(WP packet);
}
