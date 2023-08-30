package me.alek.honey.checks.impl;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.KeyedViolationCheck;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.api.packet.ListenerPriority;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.protocol.Protocol;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Supplier;

public class FloodListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public ListenerPriority getPriority() {
        return ListenerPriority.LOW;
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new FloodA<>());
        visitor.visit(new FloodB<>());
        visitor.visit(new FloodD<>());
        visitor.visit(new FloodC<>());
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

    public static abstract class AbstractCustomizableFloodCheck<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, KeyedViolationCheck<WP, PacketTypeEnum> {

        private final Map<PacketTypeEnum, ConfigurationOptions.VlSetting> settingMap = new HashMap<>();

        public AbstractCustomizableFloodCheck() {
            final ConfigurationOptions.VlSetting commonSetting = getSetting();
            for (Map.Entry<PacketTypeEnum, ConfigurationOptions.VlSetting> settingEntry : ConfigurationOptions.getFloodSettings().entrySet()) {

                final ConfigurationOptions.VlSetting setting = settingEntry.getValue().clone();
                setting.interval = commonSetting.getInterval();
                setting.maxPerInterval = commonSetting.getMaxPerInterval();
                setting.vlsAmount = commonSetting.getVlsAmount();

                settingMap.put(settingEntry.getKey(), setting);
            }
        }

        public abstract ConfigurationOptions.VlSetting getSetting();

        public abstract boolean useDefaultCheckKey();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {

                if (useDefaultCheckKey())
                    return ResponseBuilder.FLOOD_FLAG_DEFAULT_KEY.apply(packet.getContainer());

                return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting(PacketTypeEnum packetType) {
            return settingMap.get(packetType);
        }

        @Override
        public PacketTypeEnum getKey(WP packet) {
            return packet.getType();
        }
    }

    public static class FloodA<WP extends WrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.PacketFlood.FloodA.class);
        }

        @Override
        public boolean useDefaultCheckKey() {
            return false;
        }

        @Override
        public String getViolationName() {
            return "Flood.A";
        }

        @Override
        public String getDescription() {
            return "Packet flood for specific packet type";
        }
    }

    public static class FloodB<WP extends WrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.PacketFlood.FloodB.class);
        }

        @Override
        public boolean useDefaultCheckKey() {
            return false;
        }

        @Override
        public String getViolationName() {
            return "Flood.B";
        }

        @Override
        public String getDescription() {
            return "Packet flood for specific packet type";
        }
    }

    public static class FloodC<WP extends WrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.PacketFlood.FloodC.class);
        }

        @Override
        public boolean useDefaultCheckKey() {
            return true;
        }

        @Override
        public String getViolationName() {
            return "Flood.C";
        }

        @Override
        public String getDescription() {
            return "Packet flood for all packet types";
        }
    }

    public static class FloodD<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, KeyedViolationCheck<WP, PacketTypeEnum> {
        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {
            if (getSetting(packet.getType()).isEnabled()) {

                return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "Flood.D";
        }

        @Override
        public String getDescription() {
            return "Packet flood for specific packet type";
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting(PacketTypeEnum packetType) {
            return ConfigurationOptions.getFloodSettings().get(packetType);
        }

        @Override
        public PacketTypeEnum getKey(WP packet) {
            return packet.getType();
        }

    }

}
