package me.alek.honey.checks.impl;

import me.alek.honey.HoneyPlugin;
import me.alek.honey.checks.Check;
import me.alek.honey.netty.api.packet.container.PacketContainer;
import me.alek.honey.netty.listener.FuzzyPacketAdapter;
import me.alek.honey.netty.packet.structure.ReflectStructureCache;
import me.alek.honey.netty.packet.structure.ReflectStructure;
import me.alek.honey.netty.packet.type.PacketBound;
import me.alek.honey.netty.packet.type.PacketState;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packet.type.PacketTypeEnum;
import me.alek.honey.netty.utility.protocol.Protocol;
import me.alek.honey.services.ViolationService;
import me.alek.honey.utility.NumberConversions;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class IllegalAttribute extends FuzzyPacketAdapter implements Check {

    private final ViolationService VIOLATION_SERVICE = HoneyPlugin.get().getViolationService();

    @Override
    public String getName() {
        return "Illegal Attribute";
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
    public void onPacketReceive(Player player, PacketContainer packetContainer) {
        for (double value : packetContainer.getDoubles().readValues()) {

            if (NumberConversions.isInvalid(value)) {
                packetContainer.cancel();
                VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Double");
                return;
            }
        }
        for (float value : packetContainer.getFloats().readValues()) {

            if (NumberConversions.isInvalid(value)) {
                packetContainer.cancel();
                VIOLATION_SERVICE.increment(player, packetContainer.getPacket(), this, "Float");
                return;
            }
        }
    }
}
