package me.alek.honey.netty.packetwrappers.play.client;

import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packetwrappers.AutoReleasable;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WrappedPlayInBlockPlace extends ByteWrappedPacket<WrappedPlayInBlockPlace> {

    private static final List<BlockFace> CARTESIAN_FACES = Arrays.asList(BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

    public enum InteractionHand {
        MAIN_HAND, OFF_HAND
    }

    @Property
    private final InteractionHand interactionHand;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_13_2))
    private WrappedBlockPosition blockPosition;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_13_2))
    private int faceId = -1;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_13_2))
    private BlockFace face;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_13_2))
    private Vector cursorPosition;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_8_8))
    private WrappedImmutableItemStack itemStack;
    // @Property(from = @NetProtocol.From(from = NetProtocol.v1_14))
    private boolean insideBlock;
    // TODO
    // @Property(from = @NetProtocol.From(from = NetProtocol.v1_21_2))
    private boolean worldBorderHit;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_19))
    private int sequence;

    public WrappedPlayInBlockPlace(PacketDataSerializer serializer, PacketContainer<WrappedPlayInBlockPlace> packet) {
        super(serializer, packet);

        NetProtocol protocol = packet.getProtocolByBytePacketState();

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_14)) {
            this.interactionHand = InteractionHand.values()[serializer.readVarInt()];
            /*
            this.blockPosition = serializer.readBlockPosition();
            this.faceId = serializer.readVarInt();
            this.cursorPosition = new Vector(serializer.readFloat(), serializer.readFloat(), serializer.readFloat());
            this.insideBlock = serializer.readBoolean();
            */
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_19)) {
                // TODO
                /*
                if (protocol.isNewerThanOrEqual(NetProtocol.v1_21_2)) {
                    this.worldBorderHit = Optional.of(this.readBoolean());
                }
                */
                this.sequence = serializer.readVarInt();
            }
        } else {
            this.blockPosition = serializer.readBlockPosition();
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_9)) {
                this.faceId = serializer.readVarInt();
                this.interactionHand = InteractionHand.values()[serializer.readVarInt()];
            } else {
                this.faceId = serializer.readUnsignedByte();
                if (this.faceId == 255) {
                    this.face = BlockFace.SELF;
                }
                this.interactionHand = InteractionHand.MAIN_HAND;

                PacketDataSerializer copy = new PacketDataSerializer(Unpooled.copiedBuffer(serializer));
                this.itemStack = copy.readItemStack(protocol);
            }

            if (protocol.isNewerThanOrEqual(NetProtocol.v1_11)) {
                this.cursorPosition = new Vector(serializer.readFloat(), serializer.readFloat(), serializer.readFloat());
            } else {
                this.cursorPosition = new Vector(serializer.readUnsignedByte() / 16.0F, serializer.readUnsignedByte() / 16.0F, serializer.readUnsignedByte() / 16.0F);
            }
        }

        if (this.face != null && this.faceId != -1) {
            this.face = CARTESIAN_FACES.get(this.faceId % CARTESIAN_FACES.size());
        }
    }

    public InteractionHand getInteractionHand() {
        return this.interactionHand;
    }

    public WrappedBlockPosition getBlockPosition() {
        return this.blockPosition;
    }

    public BlockFace getFace() {
        return this.face;
    }

    public int getFaceId() {
        return this.faceId;
    }

    public Vector getCursorPosition() {
        return this.cursorPosition;
    }

    public WrappedImmutableItemStack getItemStack() {
        return this.itemStack;
    }

    public boolean isInsideBlock() {
        return this.insideBlock;
    }

    public boolean isWorldBorderHit() {
        return this.worldBorderHit;
    }

    public int getSequence() {
        return this.sequence;
    }
}
