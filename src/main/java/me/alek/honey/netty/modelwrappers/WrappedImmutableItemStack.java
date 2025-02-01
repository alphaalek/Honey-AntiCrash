package me.alek.honey.netty.modelwrappers;

import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Type;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.codec.validation.CodecTypeValidation;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInBookEdit;
import me.alek.honey.netty.tag.ItemTag;
import me.alek.honey.netty.tag.nbt.AbstractTag;
import me.alek.honey.netty.tag.nbt.BookTag;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.netty.tag.nbt.TagType;
import me.alek.honey.netty.tag.nbt.array.ArrayTag;
import me.alek.honey.util.LazyInit;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public class WrappedImmutableItemStack implements Cloneable {

    public static final WrappedImmutableItemStack AIR = new WrappedImmutableItemStack();

    public static class ProtocolType implements Type.NoContext.In<WrappedImmutableItemStack> {

        public static WrappedImmutableItemStack.ProtocolType forProtocol(NetProtocol protocol) {
            return new WrappedImmutableItemStack.ProtocolType(protocol);
        }

        private final NetProtocol protocol;

        public ProtocolType(NetProtocol protocol) {
            this.protocol = protocol;
        }

        @Override
        public WrappedImmutableItemStack read(PacketDataSerializer buf) {
            int amount = -1;
            short durability = -1;
            if (this.protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                amount = buf.readVarInt();
                if (amount < 1) {
                    return null;
                }
            } else if (this.protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)
                    && !buf.readBoolean()) {
                return null;
            }

            int id;
            if (this.protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)) {
                id = buf.readVarInt();
            } else {
                id = buf.readShort();
            }
            if (id < 0) {
                return null;
            }

            if (amount == -1) { // before 1.20.5
                amount = buf.readByte();
            }

            if (this.protocol.isOlderThan(NetProtocol.v1_13_2)) {
                durability = buf.readShort();
            }

            try {
                return WrappedImmutableItemStack.read(this.protocol, buf, id, amount, durability);
            } catch (Exception ex) {
                throw new DecoderException("Failed to decode item stack", ex);
            }
        }
    }

    static {
        CodecTypeValidation.ITEM_STACK.addValidation(WrappedImmutableItemStack::validate);
    }

    public static void validate(WrappedImmutableItemStack item, DecodeContext<?> context) {
        if (item.getType() == ItemTypes.WRITABLE_BOOK || item.getType() == ItemTypes.WRITTEN_BOOK) {
            BookTag.validateBook(item, context);
        }
    }

    public static WrappedImmutableItemStack read(NetProtocol protocol, PacketDataSerializer serializer, int itemId, int amount, short durability) throws IOException {
        ItemType type = ItemTypes.getById(protocol.toPECVersion(), itemId);
        if (type == null) {
            return null; // no type found
        }

        ItemTag tag;
        if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
            // wrap component map
            tag = Types.COMPONENT_MAP.forProtocol(protocol)
                    .read(serializer, Type.TypeContext.OneArg.of(type));
        } else {
            // wrap nbt
            tag = Optional.ofNullable(
                            Types.NBT.forProtocol(protocol).read(serializer, null))
                    .map(AbstractTag::asCompound)
                    .orElse(null);
        }

        if (type == ItemTypes.AIR) {
            // done reading this item stack, but just ignore all other components if the type is air
            return AIR.clone();
        }

        // create stack
        if (durability != -1) {
            // before item update
            return new WrappedImmutableItemStack(protocol, type, amount, durability, tag);
        } else {
            return new WrappedImmutableItemStack(protocol, type, amount, tag);
        }
    }

    public Object toNMSSameVersion() {
        Material type = ItemFactory.getMaterialFromId(this.type.getId(this.protocol.toPECVersion()));
        ItemStack bukkitStack;
        if (this.protocol.isNewerThanOrEqual(NetProtocol.v1_13)) {
            bukkitStack = new ItemStack(type, this.amount);
        } else {
            bukkitStack = new ItemStack(type, this.amount, this.durability);
        }

        try {
            Object nmsStack = ItemFactory.asNMSCopy(bukkitStack);
            if (hasTag()) {
                ItemFactory.setItemTag(nmsStack, this.tag);
            }
            return nmsStack;
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to apply item tag", ex);
            return null;
        }
    }

    private final NetProtocol protocol;
    private final ItemType type;
    private ItemTag tag = ItemTag.EMPTY;
    private int amount;
    private short durability = -1; // until 1.13

    public WrappedImmutableItemStack(NetProtocol protocol, ItemType type, int amount, ItemTag tag) {
        this.protocol = protocol;
        this.type = Optional.ofNullable(type).orElse(ItemTypes.AIR);
        this.amount = amount;
        if (tag != null) {
            this.tag = tag;
        }

        if (protocol.isOlderThan(NetProtocol.v1_13_2)) {
            this.durability = 0;
        }
    }

    public WrappedImmutableItemStack(NetProtocol protocol, ItemType type, int amount, short durability, ItemTag tag) {
        this(protocol, type, amount, tag);
        this.durability = durability;

        if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)) {
            throw new UnsupportedOperationException();
        }
    }

    private WrappedImmutableItemStack() {
        this.type = ItemTypes.AIR;
        this.protocol = NetProtocol.UNKNOWN;
    }

    public boolean hasTag() {
        return !this.tag.isEmpty();
    }

    public ItemTag getTag() {
        return this.tag;
    }

    public void setTag(ItemTag tag) {
        this.tag = tag;
        if (this.tag == null) {
            this.tag = ItemTag.EMPTY;
        }
    }

    public int getAmount() {
        return this.amount;
    }

    public ItemType getType() {
        return this.type;
    }

    public short getDurability() {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new UnsupportedOperationException();
        }
        return durability;
    }

    public boolean isSimilar(ItemStack stack) {
        // TODO: fix
        return false;
    }

    public boolean isSimilar(WrappedImmutableItemStack stack) {
        return this.type.equals(stack.getType()) && (!hasTag() && !stack.hasTag() || this.tag.isSimilar(stack.getTag()));
    }

    @Override
    public String toString() {
        // TODO: format
        return super.toString();
    }

    @Override
    public WrappedImmutableItemStack clone() {
        try {
            return (WrappedImmutableItemStack) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
