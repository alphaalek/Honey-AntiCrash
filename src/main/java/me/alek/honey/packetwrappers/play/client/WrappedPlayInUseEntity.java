package me.alek.honey.packetwrappers.play.client;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.packetwrappers.WrappedPacket;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.util.NMS;
import org.bukkit.util.Vector;

import java.util.Optional;

public class WrappedPlayInUseEntity extends WrappedPacket<WrappedPlayInUseEntity> {

    private static final Class<Object> enumEntityUseActionClass = NMS.getEntityUseActionClass();
    private static final Class<Object> vec3DClass = NMS.getVec3DClass();

    public WrappedPlayInUseEntity(Object rawPacket, PacketContainer<WrappedPlayInUseEntity> packetContainer) {
        super(rawPacket,packetContainer);
    }

    // TODO: fix til 1.17+

    public int getEntityID() {
        return getContainer().getInts().readField(0);
    }

    public Optional<Vector> getTarget() {
        Vector vector = getContainer().getModifier(vec3DClass, Converters.getVec3DConverter()).readField(0);
        return Optional.ofNullable(vector);
    }

    public void setTarget(Vector vector) {
        if (getAction() != EnumEntityUseAction.INTERACT_AT) return;

        IStructureModifier<Vector> modifier = getContainer().getModifier(vec3DClass, Converters.getVec3DConverter());
        modifier.writeField(0, vector);
    }

    public EnumEntityUseAction getAction() {
        return getContainer().getModifier(enumEntityUseActionClass, Converters.getEntityUseActionConverter()).readField(0);
    }

    public enum EnumEntityUseAction {
        INTERACT, ATTACK, INTERACT_AT
    }

}
