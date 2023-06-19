package me.alek.honey.checks.listeners;

import io.netty.channel.Channel;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.IntervalConsumerCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class Flood<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new FloodA<>());
        return visitor;
    }

    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> {
            Set<PacketTypeEnum> floodSettings = ConfigurationOptions.getFloodSettings().keySet();
            List<PacketTypeEnum> packetTypes = new ArrayList<>();

            for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol())) {
                if (floodSettings.contains(packetType)) {
                    packetTypes.add(packetType);
                }
            }
            return packetTypes;
        };
    }

    public static class FloodA<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, IntervalConsumerCheck {

        private final Map<PacketTypeEnum, ConfigurationOptions.IntervalSetting> settingMap = new HashMap<>();

        public FloodA() {
            settingMap.putAll(ConfigurationOptions.getFloodSettings());
        }

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
        }

        @Override
        public String getViolationName() {
            return "FloodA";
        }

        @Override
        public ConfigurationOptions.IntervalSetting getSetting(PacketTypeEnum packetType) {
            return settingMap.get(packetType);
        }
    }

}
