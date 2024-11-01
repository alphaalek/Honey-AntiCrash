package me.alek.honey.netty.modelwrappers;

import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.nbt.NBTBase;
import me.alek.honey.netty.nbt.NBTCompound;
import me.alek.honey.netty.nbt.NBTFactory;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemReflection;
import me.alek.honey.util.reflection.NMS;
import me.alek.honey.util.reflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public class WrappedImmutableItemStack {

    private static final ConstructorInvoker<ItemMeta> defaultMetaConstructor =
            Reflection.getConstructor(NMS.getCraftMetaItemClass(), NMS.getNBTTagCompoundClass());

    public static WrappedImmutableItemStack read(PacketDataSerializer serializer, int amount, int itemId, short durability) {
        Material type = ItemReflection.getMaterialFromId(itemId);
        if (type == null) {
            return null; // no type found
        }

        // wrap nbt
        NBTCompound nbt = Optional.ofNullable(NBTFactory.read(serializer)).map(NBTBase::asCompound).orElse(null);

        // create stack
        if (durability != -1) {
            // before item update
            return new WrappedImmutableItemStack(type, amount, durability, nbt);
        } else {
            return new WrappedImmutableItemStack(type, amount, nbt);
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

    public static WrappedImmutableItemStack fromBukkit(ItemStack bukkitStack) {
        if (bukkitStack == null || bukkitStack.getType() == Material.AIR) {
            return new WrappedImmutableItemStack();
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
            return new WrappedImmutableItemStack();
        }

        ItemStack bukkitStack = ItemReflection.asCraftMirror(stack);
        NBTCompound wrappedNbt = getNBTFromNMS(stack);

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

    public ItemStack toBukkit() {
        return ItemReflection.asCraftMirror(toNMS());
    }

    private NBTCompound nbt;
    private int amount;
    private short durability = -1; // until 1.13
    private Material type = Material.AIR;

    private ItemMeta cachedDefaultMeta;
    private ItemMeta cachedSpecificMeta;

    public WrappedImmutableItemStack(Material type, int amount, NBTCompound nbt) {
        this();
        this.type = type;
        this.amount = amount;
        this.nbt = nbt;
    }

    public WrappedImmutableItemStack(Material type, int amount, short damage, NBTCompound nbt) {
        this(type, amount, nbt);

        if (NetProtocol.getProtocol().isNewerThanOrEqual(NetProtocol.v1_13)) {
            throw new IllegalStateException();
        }
        this.durability = damage;
    }

    public WrappedImmutableItemStack() {
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_13)) {
            this.durability = 0;
        }
    }

    public boolean hasNBT() {
        return this.nbt != null && !this.nbt.isEmpty();
    }

    public boolean hasItemMeta() {
        if (this.cachedDefaultMeta != null || this.cachedSpecificMeta != null) {
            return true;
        }
        return Bukkit.getItemFactory().getItemMeta(this.type) != null && hasNBT();
    }

    public ItemMeta getDefaultMeta() {
        if (this.cachedDefaultMeta != null) {
            return this.cachedSpecificMeta;
        } else if (this.cachedSpecificMeta != null) {
            return this.cachedSpecificMeta;
        }
        return (this.cachedDefaultMeta = defaultMetaConstructor.invoke(this.nbt.getHandle()));
    }

    public ItemMeta getSpecificMeta() {
        if (this.cachedSpecificMeta != null) {
            return this.cachedSpecificMeta;
        }
        return (cachedSpecificMeta = toBukkit().getItemMeta());
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
