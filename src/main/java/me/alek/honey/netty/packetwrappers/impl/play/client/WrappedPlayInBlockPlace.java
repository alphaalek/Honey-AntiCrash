package me.alek.honey.netty.packetwrappers.impl.play.client;

import me.alek.honey.configuration.ConfigurationOptions;
import me.alek.honey.netty.codec.Rewritable;
import me.alek.honey.netty.packet.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedImmutableItemStack;
import me.alek.honey.netty.packetwrappers.ByteInWrappedPacketImpl;
import me.alek.honey.netty.packetwrappers.protocol.InteractionHand;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

public class WrappedPlayInBlockPlace extends ByteInWrappedPacketImpl.CacheRewriteCaller<WrappedPlayInBlockPlace> {

    private static final List<BlockFace> CARTESIAN_FACES = Arrays.asList(BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

    @Property(from = @NetProtocol.From(from = NetProtocol.v1_9))
    private final InteractionHand interactionHand;
    @Property
    private final WrappedBlockPosition blockPosition;
    private final int faceId;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_13_2))
    private BlockFace face;
    @Property
    private final Vector cursorPosition;
    @Property(to = @NetProtocol.To(to = NetProtocol.v1_8_8))
    private WrappedImmutableItemStack itemStack;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_14))
    private boolean insideBlock;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_21_2))
    private boolean worldBorderHit;
    @Property(from = @NetProtocol.From(from = NetProtocol.v1_19))
    private int sequence;

    public WrappedPlayInBlockPlace(PacketDataSerializer serializer, PacketContainer<WrappedPlayInBlockPlace> packet) {
        super(serializer, packet);
        NetProtocol protocol = getProtocol();

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_14)) {
            this.interactionHand = InteractionHand.values()[serializer.readVarInt()];
            this.blockPosition = serializer.readBlockPosition();
            this.faceId = serializer.readVarInt();
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
                Rewritable<WrappedImmutableItemStack> rewritable = serializer.readItemStackSafely(packet, 3); // 3 missing bytes for x, y, z of cursor position
                handleRewritten(rewritable);
                this.itemStack = rewritable.get();
                this.interactionHand = InteractionHand.MAIN_HAND;
            }
        }
        if (this.face == null && this.faceId != -1) {
            this.face = CARTESIAN_FACES.get(this.faceId);
        }

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_11)) {
            this.cursorPosition = new Vector(serializer.readFloat(), serializer.readFloat(), serializer.readFloat());
        } else {
            this.cursorPosition = new Vector(serializer.readUnsignedByte() / 16.0F, serializer.readUnsignedByte() / 16.0F, serializer.readUnsignedByte() / 16.0F);
        }

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_14)) {
            this.insideBlock = serializer.readBoolean();
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_19)) {
                if (protocol.isNewerThanOrEqual(NetProtocol.v1_21_2)) {
                    this.worldBorderHit = serializer.readBoolean();
                }
                this.sequence = serializer.readVarInt();
            }
        }
    }

    @Override
    protected boolean acceptsRewriting(PacketContainer<?> packet) {
        return !ConfigurationOptions.Decoder.DISABLE_ITEM_TAG_BLOCK_PLACE && super.acceptsRewriting(packet);
    }

    @Override
    public boolean shouldRewrite(PacketContainer<?> packet) {
        return ConfigurationOptions.Decoder.DISABLE_ITEM_TAG_BLOCK_PLACE && packet.getLastRewrittenPipelineState() == null && getProtocol().isOlderThanOrEqual(NetProtocol.v1_8_8)
                || super.shouldRewrite(packet);
    }

    @Override
    public void rewrite(PacketDataSerializer serializer) {
        if (ConfigurationOptions.Decoder.DISABLE_ITEM_TAG_BLOCK_PLACE) {
            rewriteSanitized(serializer);
        } else {
            super.rewrite(serializer);
        }
    }

    private void rewriteSanitized(PacketDataSerializer serializer) {
        // only for pre 1.9
        serializer.writeBlockPosition(this.blockPosition);
        serializer.writeByte(this.faceId);
        serializer.writeItemStack(getContainer(), new WrappedImmutableItemStack(getProtocol(), this.itemStack.getType()));
        serializer.writeByte((byte) (this.cursorPosition.getX() * 16.0F));
        serializer.writeByte((byte) (this.cursorPosition.getY() * 16.0F));
        serializer.writeByte((byte) (this.cursorPosition.getZ() * 16.0F));
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
