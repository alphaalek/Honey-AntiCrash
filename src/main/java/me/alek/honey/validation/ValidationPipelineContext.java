package me.alek.honey.validation;

import me.alek.honey.services.ViolationService;
import me.alek.honey.util.Array;
import me.alek.honey.util.statics.NumberUtils;
import me.alek.honey.validation.types.HoneyValidation;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ValidationPipelineContext {

    private final Array<Array<DevelopmentData>> developmentDataMap = new Array<>();

    public DevelopmentData getDevelopmentData(ViolationKey key) {
        return developmentDataMap
                .get(Optional.ofNullable(key.getKey()).map(ViolationService.HoneyKey::getRegistry).orElse(0), Array::new)
                .get(key.getValidation().getID(), DevelopmentData::new);
    }

    public static class DevelopmentData {

        private final List<String> developmentDataSet = new ArrayList<>();
        private final List<String> internalData = new ArrayList<>();

        public int size() {
            return developmentDataSet.size();
        }

        public void addInternalData(String data) {
            internalData.add(data);
        }

        public void removeLastInternalData() {
            internalData.remove(internalData.size() - 1);
        }

        public List<String> getInternalData() {
            return internalData;
        }

        public void addText(String line) {
            developmentDataSet.add(line);
        }

        public void setText(int index, String line) {
            developmentDataSet.set(index, line);
        }

        public void addEmpty() {
            addText("");
        }

        public void addLine(String line) {
            addText(" §7" + line);
        }

        public void addHeader(String line, String specification) {
            addText("§6* §e" + line + ":" + (specification == null ? "" : " (" + specification + ")"));
        }

        public void addPoint(String key, Object value) {
            if (value instanceof Number) value = NumberUtils.format((Number) value);
            addText(" §8* " + key + ": " + value);
            addInternalData(key + "=" + value);
        }

        public void addValue(String key, Object value) {
            if (value instanceof Number) value = NumberUtils.format((Number) value);
            addText("§6" + key + ": §e" + value);
            addInternalData(key + "=" + value);
        }

        public void setEmpty(int index) {
            setText(index, "");
        }

        public void setLine(int index, String line) {
            setText(index, " §7" + line);
        }

        public void setPoint(int index, String key, Object value) {
            if (value instanceof Number) value = NumberUtils.format((Number) value);
            setText(index, " §8* " + key + ": " + value);
            addInternalData(key + "=" + value);
        }

        public void setValue(int index, String key, Object value) {
            if (value instanceof Number) value = NumberUtils.format((Number) value);
            setText(index, "§6" + key + ": §e" + value);
            addInternalData(key + "=" + value);
        }

        public List<String> getDataSet() {
            return developmentDataSet;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            for (String data : internalData) {
                builder.append(data.trim()).append(", ");
            }
            return builder.substring(0, builder.length() - 1);
        }
    }

    private final Array<ContainerStructure<?>> dataContextStorage = new Array<>();

    public void putBoolean(boolean bool) {
        put(ContainerStorageTypes.BOOLEAN, bool);
    }

    public void putString(String string) {
        put(ContainerStorageTypes.STRING, string);
    }

    public void putChar(char character) {
        put(ContainerStorageTypes.CHAR, character);
    }

    public void putInt(int integer) {
        put(ContainerStorageTypes.INT, integer);
    }

    public void putFloat(float floatValue) {
        put(ContainerStorageTypes.FLOAT, floatValue);
    }

    public void putByte(byte byteValue) {
        put(ContainerStorageTypes.BYTE, byteValue);
    }

    public void putDouble(double doubleValue) {
        put(ContainerStorageTypes.DOUBLE, doubleValue);
    }

    public void putLong(long longValue) {
        put(ContainerStorageTypes.LONG, longValue);
    }

    public void putLocation(Location location) {
        put(ContainerStorageTypes.LOCATION, location);
    }

    public String getString(int index) {
        return get(ContainerStorageTypes.STRING, index);
    }

    public boolean getBoolean(int index) {
        return getVal(index, ContainerStorageTypes.BOOLEAN, false);
    }

    public char getChar(int index) {
        return getVal(index, ContainerStorageTypes.CHAR, ' ');
    }

    public int getInt(int index) {
        return getVal(index, ContainerStorageTypes.INT, 0);
    }

    public float getFloat(int index) {
        return getVal(index, ContainerStorageTypes.FLOAT, 0f);
    }

    public byte getByte(int index) {
        return getVal(index, ContainerStorageTypes.BYTE, (byte) 0);
    }

    public double getDouble(int index) {
        return getVal(index, ContainerStorageTypes.DOUBLE, 0d);
    }

    public long getLong(int index) {
        return getVal(index, ContainerStorageTypes.LONG, 0L);
    }

    public Location getLocation(int index) {
        return get(index, ContainerStorageTypes.LOCATION);
    }

    public synchronized <T> void put(int index, T value) {
        ContainerStructure<T> structure = getStructure(index);
        structure.add(value);
    }

    public <T> void put(Class<T> clazz, T value) {
        put(ContainerStorageTypes.getIndex(clazz), value);
    }

    public <T> T getVal(int index, int typeIndex, T or) {
        T val = get(index, typeIndex);
        return val != null ? val : or;
    }

    public synchronized <T> T get(Class<T> clazz, int index) {
        return get(index, ContainerStorageTypes.getIndex(clazz));
    }

    public synchronized <T> T get(int index, int typeIndex) {
        ContainerStructure<T> structure = getStructure(typeIndex);
        return structure.get(index);
    }

    public <T> ContainerStructure<T> getStructure(int index) {
        return (ContainerStructure<T>) dataContextStorage.get(index, () -> new ContainerStructure<>());
    }

    public static final class ContainerStructure<T> {

        private final Array<T> values = new Array<>();

        public void add(T value) {
            values.add(value);
        }

        public T get(int index) {
            if (!values.has(index)) return null;

            return values.get(index);
        }
    }
}
