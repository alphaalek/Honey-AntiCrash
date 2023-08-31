package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
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
import me.alek.honey.netty.injector.AbstractPipelineProxy;
import me.alek.honey.netty.injector.InboundPacketDecoder;
import me.alek.honey.netty.packet.cancellables.CancellablePacketDelegateImpl;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.services.ViolationService;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Supplier;

public class FloodListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final ViolationService.CheckKey sharedIgnoringKey = new ViolationService.CheckKey();
    private static final ConfigurationOptions.VlSetting sharedIgnoringSetting = new ConfigurationOptions.VlSetting();

    public static ViolationService.CheckKey getSharedIgnoringKey() {
        return sharedIgnoringKey;
    }

    public static ConfigurationOptions.VlSetting getSharedIgnoringSetting() {
        return sharedIgnoringSetting;
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public void cleanup(Player player) {
        FloodD.packetDecoderMap.remove(player.getUniqueId());
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

        public abstract ResponseContainer getResponse(Player player);

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) throws Exception {
            if (getSetting().isEnabled()) {
                return getResponse(player);
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public ConfigurationOptions.VlSetting getSetting(WP packet) {
            return settingMap.get(packet.getType());
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
        public ResponseContainer getResponse(Player player) {
            ResponseContainer response = ResponseBuilder.inst().flag().withKey(ViolationService.CheckKey.DEFAULT).build();

            if (HoneyPlugin.get().getRecentTPS() < ConfigurationOptions.General.Exempt.MIN_TPS || HoneyPlugin.get().getRecentPing(player) > ConfigurationOptions.General.Exempt.MAX_PING) {
                return response.builder().terminate().build();
            }
            return response;
        }

        @Override
        public String getViolationName() {
            return "Flood.A";
        }

        @Override
        public String getDescription() {
            return "General packet flood limiter";
        }
    }

    public static class FloodB<WP extends WrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.PacketFlood.FloodB.class);
        }

        @Override
        public ResponseContainer getResponse(Player player) {
            return ResponseBuilder.inst().flag().build();
        }

        @Override
        public String getViolationName() {
            return "Flood.B";
        }

        @Override
        public String getDescription() {
            return "Packet flood for sudden spikes";
        }
    }

    public static class FloodC<WP extends WrappedPacket<WP>> extends AbstractCustomizableFloodCheck<WP> {

        @Override
        public ConfigurationOptions.VlSetting getSetting() {
            return ConfigurationOptions.getVlSetting(ConfigurationOptions.PacketFlood.FloodC.class);
        }

        @Override
        public ResponseContainer getResponse(Player player) {
            return ResponseBuilder.inst().flag().build();
        }

        @Override
        public String getViolationName() {
            return "Flood.C";
        }

        @Override
        public String getDescription() {
            return "Packet flood for sudden spikes";
        }

    }

    public static class FloodD<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, KeyedViolationCheck<WP, PacketTypeEnum> {

        private static final Map<UUID, InboundPacketDecoder> packetDecoderMap = new HashMap<>();

        @Override
        public ResponseContainer check(Player player, WP packet, CheckContextContainer container) {

            // set the interval of this ignoring setting to be the spike interval specified in the configuration
            synchronized (sharedIgnoringSetting) {
                if (sharedIgnoringSetting.getInterval() == 0) sharedIgnoringSetting.interval = ConfigurationOptions.General.Exempt.SPIKE_INTERVAL;
            }

            // force the violation service to make a packet limit handler with the shared key and shared setting
            HoneyPlugin.get().getViolationService().getLimits(player, sharedIgnoringKey, sharedIgnoringSetting);

            if (getSetting(packet).isEnabled()) {

                final UUID uuid = player.getUniqueId();

                // add the inbound packet decoder of this player to the map, so we can get the delta time between the
                // last packet received and this one, because the decoder tracks this
                if (!packetDecoderMap.containsKey(uuid)) {

                    AbstractPipelineProxy proxy = HoneyPlugin.get().getPacketLibrary().getInjector().getDecoder(NMSUtils.getChannel(player));
                    if (proxy instanceof InboundPacketDecoder) {
                        packetDecoderMap.put(uuid, (InboundPacketDecoder) proxy);
                    }
                }
                if (packetDecoderMap.containsKey(uuid)) {
                    // check if the delta time is high enough to exempt the next packet spike for this player
                    if (packetDecoderMap.get(uuid).getRecentDeltaReceived() > (ConfigurationOptions.General.Exempt.MAX_TIME_BETWEEN * 1_000)) {

                        return ResponseBuilder.inst().ignoreSpike().flag(0).withKey(sharedIgnoringKey).build();
                    }
                }
                // flag the player for the shared ignoring key to check if it's currently ignoring a packet spike of the player,
                // if it isn't, and it gets flagged, then the real flood setting and values will be flagged afterwards
                return ResponseBuilder.inst().flag(0).withKey(sharedIgnoringKey).onFlag(() -> {
                    HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new CancellablePacketDelegateImpl(packet.getContainer()), new ViolationService.CheckKey(this, getKey(packet)), 1, null, null, getViolationName(), getSetting(packet), container.getDevelopmentData(FloodD.this), false);
                }).build();
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
        public ConfigurationOptions.VlSetting getSetting(WP packet) {
            return ConfigurationOptions.getFloodSettings().get(packet.getType());
        }

        @Override
        public PacketTypeEnum getKey(WP packet) {
            return packet.getType();
        }
    }

}
