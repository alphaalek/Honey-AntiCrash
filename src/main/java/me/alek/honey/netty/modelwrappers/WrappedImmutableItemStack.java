package me.alek.honey.netty.modelwrappers;

import io.netty.handler.codec.DecoderException;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Type;
import me.alek.honey.netty.codec.types.Types;
import me.alek.honey.netty.codec.validation.CodecType;
import me.alek.honey.netty.codec.validation.DecodeContext;
import me.alek.honey.netty.nbt.AbstractTag;
import me.alek.honey.netty.nbt.CompoundTag;
import me.alek.honey.netty.nbt.NBTFactory;
import me.alek.honey.netty.nbt.TagType;
import me.alek.honey.netty.nbt.array.ArrayTag;
import me.alek.honey.netty.packetwrappers.impl.play.client.WrappedPlayInBookEdit;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.MaterialMappings;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.Optional;

public class WrappedImmutableItemStack implements Cloneable {

    public static final WrappedImmutableItemStack AIR = new WrappedImmutableItemStack();

    public static class ProtocolType implements Type.In<WrappedImmutableItemStack> {

        public static Type.In<WrappedImmutableItemStack> forProtocol(NetProtocol protocol) {
            return new WrappedImmutableItemStack.ProtocolType(protocol);
        }

        private final NetProtocol protocol;

        public ProtocolType(NetProtocol protocol) {
            this.protocol = protocol;
        }

        @Override
        public WrappedImmutableItemStack read(PacketDataSerializer buf, Object... params) {
            int amount = -1;
            short durability = -1;
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_20_5)) {
                amount = buf.readVarInt();
                if (amount < 1) {
                    return null;
                }
            } else if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)
                    && !buf.readBoolean()) {
                return null;
            }

            int id;
            if (protocol.isNewerThanOrEqual(NetProtocol.v1_13_2)) {
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

            if (protocol.isOlderThan(NetProtocol.v1_13)) {
                durability = buf.readShort();
            }
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
                durability = -1;
            }

            try {
                return WrappedImmutableItemStack.read(protocol, buf, id, amount, durability);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static final ConstructorInvoker<ItemMeta> defaultMetaConstructor;

    static {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_5)) {
            defaultMetaConstructor = Reflection.getConstructor(NMS.getCraftMetaItemClass(), NMS.getNBTTagCompoundClass());
        } else {
            defaultMetaConstructor = null;
        }

        CodecType.ITEM_STACK.addValidation(WrappedImmutableItemStack::validate);
        CodecType.BOOK_EDIT_ITEM_STACK.addValidation(WrappedImmutableItemStack::validateBookEdit);
    }

    public static void validate(WrappedImmutableItemStack item, DecodeContext<?> context) throws DecoderException {
        if (item.getType() == Material.WRITTEN_BOOK || item.getType() == MaterialMappings.getTypeOnly("writable-book")) {
            CompoundTag nbt = item.getNBT();
            if (nbt != null) {
                if (nbt.hasKeyOfType("pages", TagType.getFromCollection(TagType.Collection.ARRAY))) {
                    ArrayTag<String> pages = item.getNBT().getArrayOf("pages", TagType.STRING);
                    if (context.protocol().isNewerThanOrEqual(NetProtocol.v1_14) && pages.size() > WrappedPlayInBookEdit.MAX_PAGES
                            || context.protocol().isOlderThan(NetProtocol.v1_14) && pages.size() > WrappedPlayInBookEdit.MAX_PAGES_1_13_2) {
                        throw new IllegalStateException("Too many pages: " + pages.size() + ", limit: "
                                + (context.protocol().isNewerThanOrEqual(NetProtocol.v1_14) ? WrappedPlayInBookEdit.MAX_PAGES : WrappedPlayInBookEdit.MAX_PAGES_1_13_2));
                    }
                }

                if (nbt.hasKeyOfType("title", TagType.STRING)
                        && (nbt.getString("title").isEmpty() || nbt.getString("title").length() > WrappedPlayInBookEdit.TITLE_CHAR_LIMIT)) {
                    throw new DecoderException("Invalid book data! Too long title: " + nbt.getString("title").length() + ", limit: " + WrappedPlayInBookEdit.TITLE_CHAR_LIMIT);
                }

                if (nbt.hasKeyOfType("author", TagType.STRING)
                        && (nbt.getString("author").isEmpty() || nbt.getString("author").length() > WrappedPlayInBookEdit.AUTHOR_CHAR_LIMIT)) {
                    throw new DecoderException("Invalid book data! Too long author: " + nbt.getString("author").length() + ", limit: " + WrappedPlayInBookEdit.AUTHOR_CHAR_LIMIT);
                }
            }
        }
    }

    public static void validateBookEdit(WrappedImmutableItemStack item, DecodeContext.BookEditContext<?> context) throws DecoderException {
        if (item.getType() != Material.WRITTEN_BOOK && item.getType() != MaterialMappings.getTypeOnly("writable-book")) {
            throw new DecoderException("Invalid book edit item-stack! Item-stack type: " + item.getType());
        }

        CompoundTag tag = item.getNBT();
        if (tag == null) {
            throw new DecoderException("Invalid book data: no tag");
        }

        if (!tag.hasKeyOfType("pages", TagType.getFromCollection(TagType.Collection.ARRAY))) {
            throw new DecoderException("Invalid book data: no pages tag");
        }

        if (context.isSigning()) {
            if (!tag.hasKeyOfType("title", TagType.STRING)) {
                throw new DecoderException("Invalid book data: no title tag in sign context");
            }

            if (!tag.hasKeyOfType("author", TagType.STRING) || !tag.getString("author").equals(context.packet().getContainer().getPlayerName())) {
                throw new DecoderException("Invalid book data: no author tag in sign context");
            }

            if (tag.size() > 3) {
                throw new DecoderException("Invalid book data: extra tags");
            }
        } else if (tag.size() > 1) {
            throw new DecoderException("Invalid book data: extra tags");
        }
    }

    public static WrappedImmutableItemStack read(NetProtocol protocol, PacketDataSerializer serializer, int itemId, int amount, short durability) throws IOException {
        Material type = ItemReflection.getMaterialFromId(itemId);
        if (type == null) {
            return null; // no type found
        }

        // wrap nbt
        CompoundTag nbt = Optional.ofNullable(
                Types.NBT.forProtocol(protocol).read(serializer))
                .map(AbstractTag::asCompound)
                .orElse(null);

        if (type == Material.AIR) {
            // done reading this item stack, but just ignore all other components if the type is air
            return AIR.clone();
        }

        // create stack
        if (durability != -1) {
            // before item update
            return new WrappedImmutableItemStack(type, amount, durability, nbt);
        } else {
            return new WrappedImmutableItemStack(type, amount, nbt);
        }
    }

    private static CompoundTag getNBTFromNMS(Object nmsStack) {
        Object nbt = ReflectStructure.Provider.acquireStructure(NMS.getItemStackClass())
                .withType(NMS.getNBTTagCompoundClass()).withTarget(nmsStack).readField(0);

        if (nbt != null) {
            return NBTFactory.fromDelegate(nbt, true).asCompound();
        } else {
            return null;
        }
    }

    public static WrappedImmutableItemStack fromBukkit(ItemStack bukkitStack) {
        if (bukkitStack == null || bukkitStack.getType() == Material.AIR) {
            return AIR.clone();
        }

        Object nmsItem = null;
        if (ItemReflection.isCraftItem(bukkitStack)) {
            nmsItem = ItemReflection.getHandleItem(bukkitStack);
        }
        if (nmsItem == null) {
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
                return new WrappedImmutableItemStack(bukkitStack.getType(), bukkitStack.getAmount(), null);
            } else {
                return new WrappedImmutableItemStack(bukkitStack.getType(), bukkitStack.getAmount(), bukkitStack.getDurability(), null);
            }
        }

        return fromNMS(nmsItem);
    }

    public static WrappedImmutableItemStack fromNMS(Object stack) {
        if (stack == null) {
            return AIR.clone();
        }

        ItemStack bukkitStack = ItemReflection.asCraftMirror(stack);
        CompoundTag wrappedNbt = getNBTFromNMS(stack);

        // create stack
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            return new WrappedImmutableItemStack(bukkitStack.getType(), bukkitStack.getAmount(), wrappedNbt);
        } else {
            return new WrappedImmutableItemStack(bukkitStack.getType(), bukkitStack.getAmount(), bukkitStack.getDurability(), wrappedNbt);
        }
    }

    public Object toNMS() {
        ItemStack bukkitStack;
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            bukkitStack = new ItemStack(this.type, this.amount);
        } else {
            bukkitStack = new ItemStack(this.type, this.amount, this.durability);
        }

        Object nmsStack = ItemReflection.asNMSCopy(bukkitStack);
        if (this.nbt != null) {
            ItemReflection.setItemTag(nmsStack, this.nbt.getHandle());
        }

        return nmsStack;
    }

    public ItemStack toCraftBukkit() {
        return ItemReflection.asCraftMirror(toNMS());
    }

    private CompoundTag nbt;
    private int amount;
    private short durability = -1; // until 1.13
    private Material type = Material.AIR;

    private ItemMeta cachedDefaultMeta;
    private ItemMeta cachedSpecificMeta;

    public WrappedImmutableItemStack(Material type, int amount, CompoundTag nbt) {
        this();
        this.type = type;
        this.amount = amount;
        this.nbt = nbt;
    }

    public WrappedImmutableItemStack(Material type, int amount, short durability, CompoundTag nbt) {
        this(type, amount, nbt);

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new IllegalStateException();
        }
        this.durability = durability;
    }

    public WrappedImmutableItemStack() {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13)) {
            this.durability = 0;
        }
    }

    public boolean hasNBT() {
        return this.nbt != null && !this.nbt.isEmpty();
    }

    public boolean hasDefaultItemMeta() {
        if (this.cachedDefaultMeta != null || this.cachedSpecificMeta != null) {
            return true;
        }
        return hasNBT() && Bukkit.getItemFactory().getItemMeta(this.type) != null;
    }

    public boolean hasSpecificItemMeta() {
        if (this.cachedSpecificMeta != null) {
            return true;
        }
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(this.type);
        return hasNBT() && meta != null && !NMS.getCraftMetaItemClass().equals(meta.getClass());
    }

    public ItemMeta getDefaultMeta() {
        if (this.cachedDefaultMeta != null) {
            return this.cachedDefaultMeta;
        } else if (this.cachedSpecificMeta != null) {
            return this.cachedSpecificMeta;
        }

        // TODO
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_20_5)) {
            return (this.cachedDefaultMeta = orNewMeta(defaultMetaConstructor.invoke(this.nbt.getHandle())));
        } else {
            return (this.cachedDefaultMeta = getSpecificMeta());
        }
    }

    public ItemMeta getSpecificMeta() {
        if (this.cachedSpecificMeta != null) {
            return this.cachedSpecificMeta;
        }

        return (cachedSpecificMeta = toCraftBukkit().getItemMeta());
    }

    private ItemMeta orNewMeta(ItemMeta got) {
        return got != null ? got : Bukkit.getItemFactory().getItemMeta(this.type);
    }

    public void clearDefaultMetaCache() {
        this.cachedDefaultMeta = null;
    }

    public void clearSpecificMetaCache() {
        this.cachedSpecificMeta = null;
    }

    public void clearMetaCache() {
        clearDefaultMetaCache();
        clearSpecificMetaCache();
    }

    public CompoundTag getNBT() {
        return this.nbt;
    }

    public void setNBT(CompoundTag nbt) {
        this.nbt = nbt;
        clearMetaCache();
    }

    public int getAmount() {
        return this.amount;
    }

    public Material getType() {
        return this.type;
    }

    public short getDurability() {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new UnsupportedOperationException();
        }
        return durability;
    }

    @Override
    public String toString() {
        return toCraftBukkit().toString();
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
