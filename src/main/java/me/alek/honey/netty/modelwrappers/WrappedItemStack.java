package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.nbt.NBTBase;
import me.alek.honey.netty.nbt.NBTCompound;
import me.alek.honey.netty.nbt.NBTFactory;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class WrappedItemStack {

    public static WrappedItemStack read(PacketDataSerializer serializer) {
        short itemId = serializer.readShort();
        if (itemId < 0) {
            return null;
        } else if (itemId == 0) {
            return new WrappedItemStack(); // air
        }

        byte amount = serializer.readByte();
        short durability = -1;
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13)) {
            durability = serializer.readShort(); // before item update
        }

        Material type = ItemReflection.getMaterialFromId(itemId);
        if (type == null) {
            return null;
        }

        // wrap nbt
        NBTCompound nbt = Optional.ofNullable(NBTFactory.read(serializer)).map(NBTBase::asCompound).orElse(null);
        // create stack
        if (durability != -1) {
            // before item update
            return new WrappedItemStack(type, amount, durability, nbt);
        } else {
            return new WrappedItemStack(type, amount, nbt);
        }
    }

    private static NBTCompound getNBTFromNMS(Object nmsStack) {
        Object nbt = ReflectStructure.Provider.acquireStructure(ItemReflection.getNMSItemStackClass())
                .withType(NMS.getNBTTagCompoundClass()).withTarget(nmsStack).readField(0);

        if (nbt != null) {
            return NBTFactory.fromDelegate(nbt, true).asCompound();
        } else {
            return null;
        }
    }

    public static WrappedItemStack fromBukkit(ItemStack bukkitStack) {
        Object nmsItem = ItemReflection.getHandleItem(bukkitStack);
        if (nmsItem == null) {
            if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
                return new WrappedItemStack(bukkitStack.getType(), bukkitStack.getAmount(), null);
            } else {
                return new WrappedItemStack(bukkitStack.getType(), bukkitStack.getAmount(), bukkitStack.getDurability(), null);
            }
        }

        return fromNMS(nmsItem);
    }

    public static WrappedItemStack fromNMS(Object stack) {
        ItemStack bukkitStack = ItemReflection.asCraftMirror(stack);
        NBTCompound wrappedNbt = getNBTFromNMS(stack);

        // create stack
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            return new WrappedItemStack(bukkitStack.getType(), bukkitStack.getAmount(), wrappedNbt);
        } else {
            return new WrappedItemStack(bukkitStack.getType(), bukkitStack.getAmount(), bukkitStack.getDurability(), wrappedNbt);
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

    private NBTCompound nbt;
    private int amount;
    private short durability = -1; // until 1.13
    private Material type = Material.AIR;

    public WrappedItemStack(Material type, int amount, NBTCompound nbt) {
        this();
        this.type = type;
        this.amount = amount;
        this.nbt = nbt;
    }

    public WrappedItemStack(Material type, int amount, short damage, NBTCompound nbt) {
        this(type, amount, nbt);

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new IllegalStateException();
        }
        this.durability = damage;
    }

    public WrappedItemStack() {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13)) {
            this.durability = 0;
        }
    }

    public NBTCompound getNBT() {
        return this.nbt;
    }

    public void setNBT(NBTCompound nbt) {
        this.nbt = nbt;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Material getType() {
        return this.type;
    }

    public void setType(Material type) {
        this.type = type;
    }

    public short getDurability() {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new UnsupportedOperationException();
        }
        return durability;
    }

    public void setDurability(short durability) {
        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new UnsupportedOperationException();
        }
        this.durability = durability;
    }
}
