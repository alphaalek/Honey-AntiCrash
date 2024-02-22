package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlayer;
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
import me.alek.honey.netty.PacketLibrary;
import me.alek.honey.netty.api.ListenerPriority;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.injector.handler.InboundPacketDecoder;
import me.alek.honey.netty.packet.bundle.PacketBundleFilter;
import me.alek.honey.netty.packet.bundle.PacketBundleFilters;
import me.alek.honey.netty.packet.cancellables.CancellablePacketDelegateImpl;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.packetwrappers.play.client.WrappedPlayInSetCreativeSlot;
import me.alek.honey.netty.utility.NetProtocol;
import me.alek.honey.services.ViolationService;

import java.util.*;
import java.util.function.Supplier;

public class FloodListener<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {

    private static final Map<UUID, Integer> lastSetCreativeSlot = new HashMap<>();

    private static final ViolationService.CheckKey sharedIgnoringKey = new ViolationService.CheckKey();
    private static final ConfigurationOptions.VlSetting sharedIgnoringSetting = new ConfigurationOptions.VlSetting();

    public static ViolationService.CheckKey getSharedIgnoringKey() {
        return sharedIgnoringKey;
    }

    public static ConfigurationOptions.VlSetting getSharedIgnoringSetting() {
        return sharedIgnoringSetting;
    }

    @Override
    public boolean onPrePacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer, CheckContextContainer container) {

        // return if the player is removing content from his inventory by using the clear all slots button in creative menu
        if (packetContainer.getType() == PacketType.Play.Client.SET_CREATIVE_SLOT) {
            int slot = ((WrappedPlayInSetCreativeSlot)packetContainer.getPacket()).getSlot();
            int prevSlot = lastSetCreativeSlot.computeIfAbsent(player.uuid, (d) -> slot);

            lastSetCreativeSlot.put(player.uuid, slot);
            return prevSlot + 1 != slot;
        }
        return true;
    }

    @Override
    public void onPostPacketReceive(HoneyPlayer player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public void cleanup(HoneyPlayer player) {
        lastSetCreativeSlot.remove(player.uuid);
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

            for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(NetProtocol.getProtocol(), PacketState.PLAY, PacketBound.CLIENT)) {
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
                setting.interval = commonSetting.interval;
                setting.maxPerInterval = commonSetting.maxPerInterval;
                setting.vlsAmount = commonSetting.vlsAmount;

                settingMap.put(settingEntry.getKey(), setting);
            }
        }

        public abstract ConfigurationOptions.VlSetting getSetting();

        public abstract ResponseContainer getResponse(HoneyPlayer player);

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) throws Exception {
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
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.FLOOD_FILTER;
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
        public ResponseContainer getResponse(HoneyPlayer player) {
            ResponseContainer response = ResponseBuilder.inst().flag().withLimitsKey(ViolationService.CheckKey.DEFAULT).build();

            if (HoneyPlugin.get().shouldExempt(player)) {
                return response.builder().terminate().build();
            }
            return response;
        }

        @Override
        public String getName() {
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
        public ResponseContainer getResponse(HoneyPlayer player) {
            return ResponseBuilder.inst().flag().build();
        }

        @Override
        public String getName() {
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
        public ResponseContainer getResponse(HoneyPlayer player) {
            return ResponseBuilder.inst().flag().build();
        }

        @Override
        public String getName() {
            return "Flood.C";
        }

        @Override
        public String getDescription() {
            return "Packet flood for sudden spikes";
        }

    }

    public static class FloodD<WP extends WrappedPacket<WP>> implements PacketCheck<WP>, KeyedViolationCheck<WP, PacketTypeEnum> {

        @Override
        public ResponseContainer check(HoneyPlayer player, WP packet, CheckContextContainer container) {

            // set the interval of this ignoring setting to be the spike interval specified in the configuration
            // it gets updated when the configuration is being reloaded, but not here
            if (sharedIgnoringSetting.interval == 0) sharedIgnoringSetting.interval = ConfigurationOptions.Exempt.SPIKE_INTERVAL;

            // force the violation service to make a packet limit handler with the shared key and shared setting
            HoneyPlugin.get().getViolationService().getDistributor(player, sharedIgnoringKey, sharedIgnoringSetting);

            InboundPacketDecoder decoder = (InboundPacketDecoder) HoneyPlugin.get().getPacketLibrary().getPacketManager().getDecoder(packet.getContainer().getChannel());

            if (getSetting(packet).isEnabled() && decoder != null) {
                // check if the delta time is high enough to exempt the next packet spike for this player
                if (decoder.getRecentDeltaReceived() > (ConfigurationOptions.Exempt.MAX_TIME_BETWEEN * 1_000)) {

                    return ResponseBuilder.inst().ignoreSpike().flag(0).withLimitsKey(sharedIgnoringKey).build();
                }
                // flag the player for the shared ignoring key to check if it's currently ignoring a packet spike of the player,
                // if it isn't, and it gets flagged, then the real flood setting and values will be flagged afterwards

                // NOTE: it's not a shared packet limit handler, just a shared key and setting
                return ResponseBuilder.inst().flag(0).withLimitsKey(sharedIgnoringKey).onFlag(() -> {
                    HoneyPlugin.get().getViolationService().flag(player, packet.getHandle(), new CancellablePacketDelegateImpl(packet.getContainer()), new ViolationService.CheckKey(getName(), getKey(packet)), null, 1, FloodD.this, getSetting(packet), container.getDevelopmentData(FloodD.this), false);
                }).build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getName() {
            return "Flood.D";
        }

        @Override
        public String getDescription() {
            return "Packet flood for specific packet type";
        }

        @Override
        public PacketBundleFilter getDumpFilter() {
            return PacketBundleFilters.FLOOD_FILTER;
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
