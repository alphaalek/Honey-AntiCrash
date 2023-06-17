package me.alek.honey.checks.impl;

import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalConsumerCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FloodA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, IntervalConsumerCheck<WP> {

    private final Map<Class<? extends WrappedPacket<?>>, ConfigurationOptions.IntervalSetting> settingMap = new HashMap<>();
    private Function<Class<WP>, ConfigurationOptions.IntervalSetting> settingFunction;

    public FloodA() {
        for (ConfigurationOptions.IntervalSetting setting : ConfigurationOptions.getFloodSettings()) {

        }
    }

    @Override
    public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
        return null;
    }

    @Override
    public Function<Class<WP>, ConfigurationOptions.IntervalSetting> getSetting() {
        if (settingFunction == null) {
            settingFunction = settingMap::get;
        }
        return settingFunction;
    }
}
