package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;

import java.util.function.Function;

public interface IntervalConsumerCheck {

    Function<PacketTypeEnum, ConfigurationOptions.IntervalSetting> getSetting();
}
