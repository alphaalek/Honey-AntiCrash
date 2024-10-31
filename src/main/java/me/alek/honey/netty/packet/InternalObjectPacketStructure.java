package me.alek.honey.netty.packet;

import me.alek.honey.netty.api.IStructureModifier;
import me.alek.honey.netty.api.ObjectPacketStructure;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.modelwrappers.WrappedBlockPosition;
import me.alek.honey.netty.modelwrappers.WrappedGameProfile;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.util.reflection.NMS;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class InternalObjectPacketStructure implements ObjectPacketStructure {

    private final Object raw;
    private final ReflectStructure<Object, ?> structure;

    public InternalObjectPacketStructure(Object raw, ReflectStructure<Object, ?> structure) {
        this.raw = raw;
        this.structure = structure;

        if (this.structure.isStaticFieldsAllowed()) {
            this.structure.withoutStatic();
        }
    }

    public <T> IStructureModifier<T> getModifier(Class<T> clazz) {
        return getModifier(clazz, null);
    }

    public <T, C> IStructureModifier<C> getModifier(Class<T> clazz, JavaConverter<T, C> converter, Object... params) {
        if (this.structure == null) {
            return null;
        }

        return this.structure.withType(clazz, converter).withTarget(this.raw, params);
    }

    @Override
    public IStructureModifier<Double> getDoubles() {
        return getModifier(double.class);
    }

    @Override
    public IStructureModifier<Long> getLongs() {
        return getModifier(long.class);
    }

    @Override
    public IStructureModifier<Integer> getInts() {
        return getModifier(int.class);
    }

    @Override
    public IStructureModifier<Short> getShorts() {
        return getModifier(short.class);
    }

    @Override
    public IStructureModifier<Float> getFloats() {
        return getModifier(float.class);
    }

    @Override
    public IStructureModifier<Byte> getBytes() {
        return getModifier(byte.class);
    }

    @Override
    public IStructureModifier<Boolean> getBooleans() {
        return getModifier(boolean.class);
    }

    @Override
    public IStructureModifier<String> getStrings() {
        return getModifier(String.class);
    }

    @Override
    public IStructureModifier<Object> getObjectsOf() {
        return getModifier(Object.class);
    }

    @Override
    public IStructureModifier<Object> getAll() {
        if (this.structure == null) {
            return null;
        }

        return (IStructureModifier<Object>) this.structure.withTarget(raw);
    }

    @Override
    public IStructureModifier<UUID> getUUIDS() {
        return getModifier(UUID.class);
    }

    @Override
    public IStructureModifier<String[]> getStringArrays() {
        return getModifier(String[].class);
    }

    @Override
    public IStructureModifier<long[]> getLongArrays() {
        return getModifier(long[].class);
    }

    @Override
    public IStructureModifier<int[]> getIntArrays() {
        return getModifier(int[].class);
    }

    @Override
    public IStructureModifier<short[]> getShortArrays() {
        return getModifier(short[].class);
    }

    @Override
    public IStructureModifier<byte[]> getByteArrays() {
        return getModifier(byte[].class);
    }

    @Override
    public IStructureModifier<ItemStack> getItems() {
        return getModifier(NMS.getItemStackClass(), Converters.getCopyItemStackConverter());
    }

    @Override
    public IStructureModifier<WrappedGameProfile> getGameProfiles() {
        return getModifier(NMS.getGameProfileClass(), Converters.getGameProfileConverter());
    }

    @Override
    public IStructureModifier<PacketDataSerializer> getDataSerializers(boolean copy) {
        return getModifier(NMS.getPacketDataSerializerClass(), Converters.getPacketDataSerializerConverter(), copy);
    }

    @Override
    public IStructureModifier<WrappedBlockPosition> getBlockPositions() {
        return getModifier(NMS.getBlockPositionClass(), Converters.getBlockPositionConverter());
    }

    @Override
    public <T> IStructureModifier<T> getObjectsOf(Class<T> target) {
        return getModifier(target);
    }
}
