package me.alek.honey.netty.packetwrappers.play.client;

import io.netty.buffer.Unpooled;
import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
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

public class WrappedPlayInBlockPlace extends ByteWrappedPacket<WrappedPlayInBlockPlace> implements AutoReleasable {

    private static final List<BlockFace> CARTESIAN_FACES = Arrays.asList(BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

    public enum InteractionHand {
        MAIN_HAND, OFF_HAND
    }

    private final InteractionHand interactionHand;
    private final WrappedBlockPosition blockPosition;
    private final int faceId;
    private BlockFace face;
    private final Vector cursorPosition;
    private LazyInit<ItemStack> itemStack;
    private boolean insideBlock;
    private boolean worldBorderHit;
    private int sequence;

    private Runnable release;

    public WrappedPlayInBlockPlace(PacketDataSerializer serializer, PacketContainer<WrappedPlayInBlockPlace> packet) {
        super(serializer, packet);
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14)) {
            this.interactionHand = InteractionHand.values()[serializer.readVarInt()];
            this.blockPosition = serializer.readBlockPosition();
            this.faceId = serializer.readVarInt();
            this.cursorPosition = new Vector(serializer.readFloat(), serializer.readFloat(), serializer.readFloat());
            this.insideBlock = serializer.readBoolean();
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_19)) {
                /*
                if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_21_2)) {
                    this.worldBorderHit = Optional.of(this.readBoolean());
                }
                */
                this.sequence = serializer.readVarInt();
            }
        } else {
            this.blockPosition = serializer.readBlockPosition();
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
                this.faceId = serializer.readVarInt();
                this.interactionHand = InteractionHand.values()[serializer.readVarInt()];
            } else {
                this.faceId = serializer.readUnsignedByte();
                if (this.faceId == 255) {
                    this.face = BlockFace.SELF;
                }
                PacketDataSerializer copy = new PacketDataSerializer(Unpooled.copiedBuffer(serializer));
                this.itemStack = new LazyInit<>(copy::readBukkitItemStack);
                this.release = () -> AutoReleasable.doRelease(copy);
                this.interactionHand = InteractionHand.MAIN_HAND;
            }

            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_11)) {
                this.cursorPosition = new Vector(serializer.readFloat(), serializer.readFloat(), serializer.readFloat());
            } else {
                this.cursorPosition = new Vector(serializer.readUnsignedByte() / 16.0F, serializer.readUnsignedByte() / 16.0F, serializer.readUnsignedByte() / 16.0F);
            }
        }

        if (this.face != null) {
            this.face = CARTESIAN_FACES.get(faceId % CARTESIAN_FACES.size());
        }
    }

    @Override
    public void release() {
        if (this.release != null) {
            this.release.run();
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

    public ItemStack getItemStack() {
        return this.itemStack.get();
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
