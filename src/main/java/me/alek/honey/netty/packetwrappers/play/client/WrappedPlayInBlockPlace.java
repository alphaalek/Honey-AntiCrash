package me.alek.honey.netty.packetwrappers.play.client;

import me.alek.honey.netty.api.PacketContainer;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.packetwrappers.ByteWrappedPacket;
import me.alek.honey.util.protocol.NetProtocol;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

public class WrappedPlayInBlockPlace extends ByteWrappedPacket<WrappedPlayInBlockPlace> {

    private static final List<BlockFace> CARTESIAN_FACES = Arrays.asList(BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

    public enum InteractionHand {
        MAIN_HAND, OFF_HAND
    }

    private final InteractionHand interactionHand;
    private final WrappedBlockPosition blockPosition;
    private final int faceId;
    private BlockFace face;
    private final Vector cursorPosition;
    private ItemStack itemStack;
    private boolean insideBlock;
    private boolean worldBorderHit;
    private int sequence;

    public WrappedPlayInBlockPlace(PacketDataSerializer buf, PacketContainer<WrappedPlayInBlockPlace> packet) {
        super(buf, packet);
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_14)) {
            interactionHand = InteractionHand.values()[buf.readVarInt()];
            blockPosition = buf.readBlockPosition();
            faceId = buf.readVarInt();
            cursorPosition = new Vector(buf.readFloat(), buf.readFloat(), buf.readFloat());
            insideBlock = buf.readBoolean();
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_19)) {
                /*
                if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_21_2)) {
                    this.worldBorderHit = Optional.of(this.readBoolean());
                }
                */
                sequence = buf.readVarInt();
            }
        } else {
            blockPosition = buf.readBlockPosition();
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_9)) {
                faceId = buf.readVarInt();
                interactionHand = InteractionHand.values()[buf.readVarInt()];
            } else {
                faceId = buf.readUnsignedByte();
                if (faceId == 255) {
                    face = BlockFace.SELF;
                }
                itemStack = buf.readBukkitItemStack();
                interactionHand = InteractionHand.MAIN_HAND;
            }
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_11)) {
                cursorPosition = new Vector(buf.readFloat(), buf.readFloat(), buf.readFloat());
            } else {
                cursorPosition = new Vector(buf.readUnsignedByte() / 16.0F, buf.readUnsignedByte() / 16.0F, buf.readUnsignedByte() / 16.0F);
            }
        }

        if (face != null) {
            face = CARTESIAN_FACES.get(faceId % CARTESIAN_FACES.size());
        }
    }

    public InteractionHand getInteractionHand() {
        return interactionHand;
    }

    public WrappedBlockPosition getBlockPosition() {
        return blockPosition;
    }

    public BlockFace getFace() {
        return face;
    }

    public int getFaceId() {
        return faceId;
    }

    public Vector getCursorPosition() {
        return cursorPosition;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public boolean isInsideBlock() {
        return insideBlock;
    }

    public boolean isWorldBorderHit() {
        return worldBorderHit;
    }

    public int getSequence() {
        return sequence;
    }
}
