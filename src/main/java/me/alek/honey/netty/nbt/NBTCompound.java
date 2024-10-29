package me.alek.honey.netty.nbt;

import me.alek.honey.netty.nbt.array.GeneticArrayTypes;
import me.alek.honey.netty.nbt.array.NBTAbstractArray;
import me.alek.honey.netty.nbt.array.NBTArray;
import me.alek.honey.netty.structure.ReflectStructure;
import me.alek.honey.netty.structure.converters.ApplicableConverter;
import me.alek.honey.netty.structure.converters.Converters;
import me.alek.honey.netty.structure.converters.JavaConverter;
import me.alek.honey.netty.structure.converters.MapConverter;

import java.io.DataOutput;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NBTCompound extends NBTBase<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>> {

    private MapConverter<String, Object, NBTBase<?, ?>> delegate;

    public NBTCompound(Object rawNms, NBTRoot root, NBTBase<?, ?> owner, boolean forceCreation) {
        super(rawNms, root, owner, forceCreation);
    }

    public NBTCompound() {
        super(NBTType.COMPOUND.create());
    }

    @Override
    public Function<ReflectStructure<Object, ?>, ReflectStructure<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>>> applyStructure(ApplicableConverter<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>> converter) {
        return (structure) -> structure.withSpecificType(Map.class, converter, false);
    }

    public ApplicableConverter<Map<String, Object>, MapConverter<String, Object, NBTBase<?, ?>>> getNBTConverter() {
        if (delegate == null) {
            delegate = Converters.getNBTCompoundConverter(getRoot(), this, forceCreateChildren);
        }
        return delegate;
    }

    public JavaConverter<Object, NBTBase<?, ?>> getConverterHandle() {
        return delegate.getHandle();
    }

    public NBTBase<?, ?> remove(String key) {
        NBTBase<?, ?> val = getValue().removeConverted(key);
        if (val != null) {
            deleteInherited(val);
        }
        return val;
    }

    public void clear() {
        for (NBTBase<?, ?> value : getValue().convertedValues()) {
            if (value != null) {
                deleteInherited(value);
            }
        }
        getValue().clear();
    }

    public boolean isEmpty() {
        return getValue().isEmpty();
    }

    public Set<String> getKeys() {
        return getValue().keySet();
    }

    public Collection<NBTBase<?, ?>> getBases() {
        return getValue().convertedValues();
    }

    public NBTBase<?, ?> get(String key) {
        return getValue().getConverted(key);
    }

    public Object getRaw(String key) {
        return getValue().getRaw(key);
    }

    public boolean hasKey(String key) {
        return getValue().containsKey(key);
    }

    public boolean hasKeyOfType(String key, byte typeId) {
        return hasKey(key) && get(key).getType().getTypeId() == typeId;
    }

    public boolean hasKeyOfType(String key, NBTType nbtType) {
        return hasKey(key) && get(key).getType() == nbtType;
    }

    public void put(String key, NBTBase<?, ?> base) {
        getValue().putConverted(key, base);
        setInherited(base);
    }

    public void putIfAbsent(String key, NBTBase<?, ?> base) {
        if (!hasKey(key)) {
            put(key, base);
        }
    }

    @Override
    public void write(DataOutput output) throws Exception {
        for (String key : getValue().keySet()) {
            NBTBase<?, ?> base = getValue().getConverted(key);
            output.writeByte((byte) getType().getTypeId());
            if (getType() == NBTType.END) {
                continue;
            }
            output.writeUTF(key);
            base.write(output);
        }
        output.writeByte(0);
    }

    @SuppressWarnings("unchecked")
    public <R extends NBTBase<?, ?>> NBTAbstractArray<R> getAbstractListOf(String key, NBTType nbtType) {
        NBTAbstractArray<R> array = getAbstractArray(key);
        if (array != null && nbtType == array.getTypeOf()) {
            return array;
        }

        NBTRoot root = getRoot();
        NBTBase<?, ?> base = get(key);

        if (base == null) {
            return null;
        } else if (nbtType == NBTType.COMPOUND) {
            return (NBTAbstractArray<R>) new GeneticArrayTypes.NBTArrayOfCompound(base.getHandle(), root, base.getOwner(), forceCreateChildren);
        } else if (nbtType == NBTType.ARRAY) {
            return (NBTAbstractArray<R>) new GeneticArrayTypes.NBTArrayOfCompound(base.getHandle(), root, base.getOwner(), forceCreateChildren);
        } else {
            return (NBTAbstractArray<R>) new NBTArray<>(base.getHandle(), nbtType, root, base.getOwner(), forceCreateChildren);
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends NBTBase<?, ?>> NBTAbstractArray<R> getAbstractListOf(String key, Class<?> clazz) {
        return getAbstractListOf(key, NBTType.getFromClass(clazz));
    }

    @SuppressWarnings("unchecked")
    public <R> NBTArray<R> getListOf(String key, Class<R> clazz) {
        return (NBTArray<R>) (NBTAbstractArray<?>) getAbstractListOf(key, clazz);
    }

    @SuppressWarnings("unchecked")
    public <R> NBTArray<R> getListOf(String key, NBTType nbtType) {
        return (NBTArray<R>) (NBTAbstractArray<?>) getAbstractListOf(key, nbtType);
    }

    public GeneticArrayTypes.NBTArrayOfCompound getArrayOfCompound(String key) {
        return (GeneticArrayTypes.NBTArrayOfCompound) (NBTAbstractArray<?>) getAbstractListOf(key, NBTType.COMPOUND);
    }

    public GeneticArrayTypes.NBTArrayOfArray getArrayOfArray(String key) {
        return (GeneticArrayTypes.NBTArrayOfArray) (NBTAbstractArray<?>) getAbstractListOf(key, NBTType.ARRAY);
    }

    public <T> NBTArray<T> getArray(String key) {
        return (NBTArray<T>) (NBTAbstractArray<?>) getAbstractArray(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getUnspecific(String key) {
        if (hasKey(key)) {
            try {
                return (T) get(key).getValue();
            } catch (ClassCastException ignored) {
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <R extends NBTBase<?, ?>> NBTAbstractArray<R> getAbstractArray(String key) {
        return (NBTAbstractArray<R>) get(key);
    }

    public NBTCompound getCompound(String key) {
        return (NBTCompound) get(key);
    }

    public byte getByte(String key) {
        return getUnspecific(key);
    }

    public short getShort(String key) {
        return getUnspecific(key);
    }

    public int getInt(String key) {
        return getUnspecific(key);
    }

    public long getLong(String key) {
        return getUnspecific(key);
    }

    public float getFloat(String key) {
        return getUnspecific(key);
    }

    public double getDouble(String key) {
        return getUnspecific(key);
    }

    public byte[] getByteArray(String key) {
        return getUnspecific(key);
    }

    public String getString(String key) {
        return getUnspecific(key);
    }

    public int[] getIntArray(String key) {
        return getUnspecific(key);
    }

    public long[] getLongArray(String key) {
        return getUnspecific(key);
    }

    private void setInherited(NBTBase<?, ?> value) {
        value.setRoot(getRoot());
        value.setOwner(this);
    }

    private void deleteInherited(NBTBase<?, ?> value) {
        value.setRoot(NBTRoot.createNewDefault());
        value.setOwner(null);
    }
}
