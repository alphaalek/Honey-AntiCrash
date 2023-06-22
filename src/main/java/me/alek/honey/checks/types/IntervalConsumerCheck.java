package me.alek.honey.checks.types;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.packet.type.PacketTypeEnum;

public interface IntervalConsumerCheck {

    ConfigurationOptions.VlSetting getSetting(PacketTypeEnum packetType);
}
