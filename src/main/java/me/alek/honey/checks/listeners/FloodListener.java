package me.alek.honey.checks.listeners;

import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.ViolationConsumerCheck;
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

    public static abstract class AbstractCustomizedFlood<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationConsumerCheck<WP, PacketTypeEnum> {

        private final Map<PacketTypeEnum, ConfigurationOptions.VlSetting> settingMap = new HashMap<>();

        public AbstractCustomizedFlood() {
            final ConfigurationOptions.VlSetting commonSetting = getSetting();
            for (Map.Entry<PacketTypeEnum, ConfigurationOptions.VlSetting> settingEntry : ConfigurationOptions.getFloodSettings().entrySet()) {

                final ConfigurationOptions.VlSetting setting = settingEntry.getValue().clone();
                setting.INTERVAL = commonSetting.INTERVAL;
                setting.MAX_PER_INTERVAL = commonSetting.MAX_PER_INTERVAL;
                setting.VLS_AMOUNT = commonSetting.VLS_AMOUNT;

                settingMap.put(settingEntry.getKey(), setting);
            }
        }

        public abstract ConfigurationOptions.VlSetting getSetting();

        public abstract boolean useDefaultCheckKey();

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (useDefaultCheckKey()) {
                return ResponseBuilder.FLOOD_FLAG_DEFAULT_KEY.apply(packet.getContainer());
            }
            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
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

    public static class FloodA<WP extends WrappedPacket<WP>> extends AbstractCustomizedFlood<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.PacketFlood.FloodA.get();
        }

        @Override
        public boolean useDefaultCheckKey() {
            return false;
        }

        @Override
        public String getViolationName() {
            return "FloodA";
        }
    }

    public static class FloodB<WP extends WrappedPacket<WP>> extends AbstractCustomizedFlood<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.PacketFlood.FloodB.get();
        }

        @Override
        public boolean useDefaultCheckKey() {
            return false;
        }

        @Override
        public String getViolationName() {
            return "FloodB";
        }
    }

    public static class FloodC<WP extends WrappedPacket<WP>> extends AbstractCustomizedFlood<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.PacketFlood.FloodC.get();
        }

        @Override
        public boolean useDefaultCheckKey() {
            return true;
        }

        @Override
        public String getViolationName() {
            return "FloodC";
        }
    }

    public static class FloodD<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, ViolationConsumerCheck<WP, PacketTypeEnum> {
        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            return ResponseBuilder.FLOOD_FLAG.apply(packet.getContainer());
        }

        @Override
        public String getViolationName() {
            return "FloodD";
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
