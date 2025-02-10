package me.alek.honey.netty.packet;

import me.alek.honey.HoneyPlayer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.packet.type.PacketType;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacket;
import me.alek.honey.netty.packetwrappers.ObjectWrappedPacket;
import me.alek.honey.netty.packetwrappers.SourcedWrappedPacket;
import me.alek.honey.netty.structure.ObjectPacketStructure;
import me.alek.honey.util.Callback;
import me.alek.honey.util.protocol.NetProtocol;

import java.util.Optional;

public class DummyPacketContainer<WP extends SourcedWrappedPacket<WP>> extends PacketContainerLifecycle<WP> implements PacketContainer<WP> {

    public static <WP extends SourcedWrappedPacket<WP>> DummyPacketContainer<WP> create(PacketType type, HoneyPlayer player) {
        return new DummyPacketContainer<>(type, player);
    }

    private final long created = System.currentTimeMillis();
    private final PacketType type;
    private final HoneyPlayer player;

    public DummyPacketContainer(PacketType type, HoneyPlayer player) {
        this.type = type;
        this.player = player;
    }

    @Override
    public NetProtocol getProtocol() {
        if (this.player.clientInfo != null) {
            return this.player.clientInfo.getProtocol();
        } else {
            return NetProtocol.getProtocol();
        }
    }

    @Override
    public long getTimeCreated() {
        return this.created;
    }

    @Override
    public PacketType getType() {
        return this.type;
    }

    @Override
    public HoneyPlayer getPlayer() {
        return this.player;
    }

    @Override
    public ObjectPacketStructure getStructure() {
        return null;
    }

    @Override
    public PacketLifecycleTimings getEventloopStateTimings() {
        return null;
    }

    @Override
    public void handleRewritten(PacketType type, PipelineState state) {

    }

    @Override
    public void callPostMinecraftDecode(Object packet, PacketType type) {

    }

    @Override
    public void callPreMinecraftDecode(PacketDataSerializer serializer) {

    }

    @Override
    public void callLateInitialByteListeners(PacketDataSerializer serializer) {

    }

    @Override
    public PipelineState getPipelineState() {
        return null;
    }

    @Override
    public PipelineState getCreatedPipelineState() {
        return null;
    }

    @Override
    public ObjectWrappedPacket<WP> getObjectWrapped() {
        return null;
    }

    @Override
    public ByteInWrappedPacket<WP> getInitialByteWrapped() {
        return null;
    }

    @Override
    public ByteInWrappedPacket<WP> getFinalByteWrapped() {
        return null;
    }

    @Override
    public ByteInWrappedPacket<WP> getAnyByteWrapped() {
        return null;
    }

    @Override
    public SourcedWrappedPacket<WP> getAnySourceWrapped() {
        return null;
    }

    @Override
    public WP getAnyWrapped() {
        return null;
    }

    @Override
    public WP getCurrentWrapped() {
        return null;
    }

    @Override
    public Callback getPostAction() {
        return null;
    }

    @Override
    public void releaseIfAutoReleasable() {

    }

    @Override
    public String toString() {
        return "(Dummy) Packet:{Type:" + Optional.ofNullable(this.type).orElse(PacketType.UNKNOWN) + ",Player:" + getPlayerName() + "}";
    }
}
