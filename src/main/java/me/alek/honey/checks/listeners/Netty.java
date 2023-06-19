package me.alek.honey.checks.listeners;

import io.netty.channel.Channel;
import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.AbstractPacketListener;
import me.alek.honey.checks.CheckContextContainer;
import me.alek.honey.checks.CheckVisitor;
import me.alek.honey.checks.IPacketListener;
import me.alek.honey.checks.response.ResponseBuilder;
import me.alek.honey.checks.response.ResponseContainer;
import me.alek.honey.checks.types.PacketCheck;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.SyncPacketAdapter;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.packetwrappers.WrappedPacket;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.netty.utility.reflect.NMSUtils;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Netty<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements IPacketListener {


    @Override
    public Supplier<List<PacketTypeEnum>> getPacketTypes() {
        return () -> PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol());
    }

    @Override
    public CheckVisitor<WP> getVisitor() {
        CheckVisitor<WP> visitor = new CheckVisitor<>();
        visitor.visit(new NettyB<>());
        return visitor;
    }

    @Override
    public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
        return true;
    }

    @Override
    public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

    }

    @Override
    public void onPacketError(Player player, PacketTypeEnum packetType, PacketContainer<WP> packetContainer) {
        if (packetContainer != null) {
            packetContainer.cancel();
        }
        HoneyPlugin.get().getViolationService().punish(player, "NettyA");
    }

    public static class NettyB<WP extends WrappedPacket<WP>> implements PacketCheck<WP> {

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            if (player == null || !player.isOnline()) {
                return ResponseBuilder
                        .inst()
                        .cancel()
                        .notify(getViolationName())
                        .terminate()
                        .build();
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "NettyB";
        }
    }


    public static class Attributes<WP extends WrappedPacket<WP>> extends AbstractPacketListener<WP> implements PacketCheck<WP>, IPacketListener {

        @Override
        public CheckVisitor<WP> getVisitor() {
            CheckVisitor<WP> visitor = new CheckVisitor<>();
            visitor.visit(this);
            return visitor;
        }

        @Override
        public boolean onPrePacketReceive(Player player, PacketContainer<WP> packetContainer, CheckContextContainer container) {
            return true;
        }

        @Override
        public void onPostPacketReceive(Player player, PacketContainer<WP> packetContainer) {

        }

        @Override
        public Supplier<List<PacketTypeEnum>> getPacketTypes() {
            return () -> {
                final List<PacketTypeEnum> packetTypes = new ArrayList<>();

                for (PacketTypeEnum packetType : PacketType.getTypesAvailableFor(PacketState.PLAY, PacketBound.CLIENT, Protocol.getProtocol())) {
                    ReflectStructure<Object, Object> structure = ReflectStructureCache.acquireStructure(packetType);

                    if (structure.withType(double.class).getAccessors().size() != 0) {
                        packetTypes.add(packetType);
                        continue;
                    }
                    if (structure.withType(float.class).getAccessors().size() != 0) {
                        packetTypes.add(packetType);
                    }
                }
                return packetTypes;
            };
        }

        @Override
        public ResponseContainer accept(Player player, WP packet, CheckContextContainer container) {
            for (double value : packet.getContainer().getDoubles().readValues()) {

                if (NumberConversions.isInvalid(value)) {
                    return ResponseBuilder
                            .inst()
                            .cancel()
                            .punish()
                            .build();
                }
            }
            for (float value : packet.getContainer().getFloats().readValues()) {

                if (NumberConversions.isInvalid(value)) {
                    return ResponseBuilder
                            .inst()
                            .cancel()
                            .punish()
                            .build();
                }
            }
            return ResponseBuilder.SUCCESS;
        }

        @Override
        public String getViolationName() {
            return "NettyC";
        }
    }
}
