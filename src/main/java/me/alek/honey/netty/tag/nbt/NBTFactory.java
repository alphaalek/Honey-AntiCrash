package me.alek.honey.netty.tag.nbt;

import io.netty.buffer.ByteBufInputStream;
import me.alek.honey.netty.structure.IStructureModifier;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.codec.types.Type;
import me.alek.honey.netty.tag.nbt.array.AbstractArrayTag;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.ListConverter;
import me.alek.honey.util.protocol.NetProtocol;
import me.alek.honey.util.reflection.ConstructorInvoker;
import me.alek.honey.util.reflection.ItemFactory;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class NBTFactory {

    private static final int SIXTY_FOUR_KILOBYTES = 1024 * 64;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_COMPLEXITY = 256;

    public static class ProtocolType implements Type.NoContext.In<AbstractTag<?, ?, ?>> {

        @SuppressWarnings("unchecked")
        public static <TYPE extends Type.NoContext.In<AbstractTag<?, ?, ?>>> TYPE forProtocol(NetProtocol protocol) {
            return (TYPE) new NBTFactory.ProtocolType(protocol);
        }

        private final NetProtocol protocol;

        public ProtocolType(NetProtocol protocol) {
            this.protocol = protocol;
        }

        @Override
        public AbstractTag<?, ?, ?> read(PacketDataSerializer buf) {
            try (ByteBufInputStream stream = new ByteBufInputStream(buf)) {
                return NBTFactory.read(stream, protocol);
            } catch (IOException ex) {
                return null;
            }
        }
    }

    private static AbstractTag<?, ?, ?> read(ByteBufInputStream stream, NetProtocol protocol) throws IOException {
        byte id = stream.readByte();
        TagType type = TagType.getFromTypeId(id);
        if (type == TagType.END || type == null) {
            return null;
        }
        if (type != TagType.getFromCollection(TagType.Collection.COMPOUND)) {
            throw new IOException("Expected compound as root tag");
        }

        // named compound tag, skip these bytes
        if (protocol.isOlderThan(NetProtocol.v1_20_2)) {
            stream.skipBytes(stream.readUnsignedShort());
        }

        // deserialize tag
        return type.getCodec().read(new DataInputStream(stream),
                new TagLimiter(SIXTY_FOUR_KILOBYTES, MAX_DEPTH, MAX_COMPLEXITY));
    }

    public static int getAmountOfTags(AbstractTag<?, ?, ?> tag) {
        if (tag instanceof AbstractArrayTag) {
            int i = 0;
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?, ?>)tag).getValues())  {
                i += getAmountOfTags(tagIn);
            }
            return i;
        } else if (tag instanceof CompoundTag) {
            int i = 0;
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag)tag).getBases()) {
                i += getAmountOfTags(tagIn);
            }
            return i;
        } else {
            return 1;
        }
    }

    public static int getComplexityOfTag(AbstractTag<?, ?, ?> tag) {
        return getComplexityOfTag0(tag, 0);
    }

    private static int getComplexityOfTag0(AbstractTag<?, ?, ?> tag, int complexity) {
        if (tag instanceof AbstractArrayTag) {
            int max = complexity;
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?, ?>)tag).getValues()) {
                int complexity0 = getComplexityOfTag0(tagIn, complexity + 1);
                if (complexity0 > max) {
                    max = complexity0;
                }
            }
            return max;
        } else if (tag instanceof CompoundTag) {
            int max = complexity;
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag)tag).getBases()) {
                int complexity0 = getComplexityOfTag0(tagIn, complexity + 1);
                if (complexity0 > max) {
                    max = complexity0;
                }
            }
            return max;
        } else {
            return complexity;
        }
    }

    public static <T extends AbstractTag<?, ?, ?>> List<T> getAllTagsAs(AbstractTag<?, ?, ?> tag, TagType type) {
        List<T> tags = new ArrayList<>();
        getAllTagsAs0(tag, type, tags);
        return tags;
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractTag<?, ?, ?>> void getAllTagsAs0(AbstractTag<?, ?, ?> tag, TagType type, List<T> tags) {
        if (tag instanceof AbstractArrayTag) {
            for (AbstractTag<?, ?, ?> tagIn : ((AbstractArrayTag<?, ?, ?>)tag).getValues())  {
                getAllTagsAs0(tagIn, type, tags);
            }
        } else if (tag instanceof CompoundTag) {
            for (AbstractTag<?, ?, ?> tagIn : ((CompoundTag)tag).getBases()) {
                getAllTagsAs0(tagIn, type, tags);
            }
        }

        if (tag.getType() == type) {
            tags.add((T) tag);
        }
    }

    private static final ReflectStructure<Object, ?> ITEM_STACK_STRUCTURE = new ReflectStructure<>(NMS.getItemStackClass());
    private static final ReflectStructure<Object, AbstractTag<?, ?, ?>> TAG_STRUCTURE = ITEM_STACK_STRUCTURE
            .withType(NMS.getNBTTagCompoundClass(), TagConverter.CONVERTER);

    public static CompoundTag fromItem(ItemStack item, boolean createIfNone) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        Object nmsItem;
        try {
            nmsItem = ItemFactory.getHandleItem(item);
            if (nmsItem == null) {
                return null;
            }
        } catch (Exception ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to get handle item", ex);
            return null;
        }

        IStructureModifier<AbstractTag<?, ?, ?>> structureModifier = TAG_STRUCTURE.withTarget(nmsItem);
        AbstractTag<?, ?, ?> base = structureModifier.readField(0);
        if (base == null && createIfNone) {
            try {
                CompoundTag nbt = new CompoundTag();
                structureModifier.writeField(0, nbt);
                return nbt;
            } catch (Exception ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to create tag", ex);
            }
        }

        if (base != null && !base.getType().getCollection().equals(TagType.Collection.COMPOUND)) {
            throw new IllegalStateException("Item with invalid compound tag");
        }

        return (CompoundTag) base;
    }

    public static <T extends AbstractTag<K, V, T>, K, V> T fromDelegate(Object handle) {
        return copyFromType(getType(handle, false), handle);
    }

    public static <T extends AbstractTag<K, V, T>, K, V> T copyBase(T base) {
        return copyFromType(base.getType(), base.getHandle());
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractTag<K, V, T>, K, V> T copyFromType(TagType type, Object handle) {
        if (type == null) {
            return null;
        }

        ConstructorInvoker<? extends AbstractTag<?, ?, ?>> constructorInvoker = type.getBridgeWrapperInvoker();
        if (constructorInvoker == null) {
            return null;
        }

        if (type.getCollection() == TagType.Collection.ARRAY) {
            return (T) constructorInvoker.invoke(handle, getArrayType(handle));
        } else {
            return (T) constructorInvoker.invoke(handle);
        }
    }

    public static TagType getArrayType(Object handle) {
        if (handle == null) {
            return null;
        }

        IStructureModifier<Byte> byteModifier = ReflectStructure.Provider.acquireStructure(NMS.getNBTTagListClass())
                .withBytes()
                .withTarget(handle);
        byte typeId = byteModifier.readField(0);
        if (typeId != 0) {
            return TagType.getFromTypeId(typeId);
        }

        IStructureModifier<ListConverter<Object, AbstractTag<?, ?, ?>>> arrayStructure = ReflectStructure.Provider.acquireStructure(handle.getClass())
            .withWeakType(List.class, Converters.<AbstractTag<?, ?, ?>>getNBTListConverter())
            .withTarget(handle);
        List<? extends AbstractTag<?, ?, ?>> array = arrayStructure.readFieldAndApplyMapping(0)
                .getDelegatedList();
        if (array.isEmpty()) {
            return null;
        }

        AbstractTag<?, ?, ?> value = array.get(0);
        if (value == null) {
            return null;
        }

        return value.getType();
    }

    public static TagType getType(Object handle, boolean ignoreNestedArrays) {
        if (handle == null) {
            return null;
        }

        IStructureModifier<Byte> structureModifier = ReflectStructure.Provider.acquireStructure(NMS.getNBTBaseClass())
                .withBytes()
                .withTarget(handle);

        byte typeId;
        if (NetProtocol.getProtocol().isOlderThan(NetProtocol.v1_17)) {
            typeId = structureModifier.invokeMethod(null, "getTypeId", 0);
        } else {
            typeId = structureModifier.invokeMethod(null,
                    (index, invoker) -> invoker.getHandle().getDeclaringClass().equals(NMS.getNBTBaseClass()));
        }

        TagType type = TagType.getFromTypeId(typeId);
        if (type != null && type.getCollection() == TagType.Collection.ARRAY && !ignoreNestedArrays) {
            TagType arrayType = getArrayType(handle);
            if (arrayType.hasCollection()) {
                switch (arrayType.getCollection()) {
                    case COMPOUND:
                        return TagType.getFromCollection(TagType.Collection.ARRAY_OF_COMPOUND);
                    case ARRAY:
                        return TagType.getFromCollection(TagType.Collection.ARRAY_OF_ARRAY);
                }
            }
        }

        return type;
    }
}
