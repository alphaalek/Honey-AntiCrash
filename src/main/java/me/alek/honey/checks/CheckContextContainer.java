package me.alek.honey.checks;

import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.util.Array;
import me.alek.honey.util.NumberUtils;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CheckContextContainer {

    private final SharedCheckPipelineStorage storage = new SharedCheckPipelineStorage();
    private final Array<DevelopmentData> developmentDataMap = new Array<>();
    private final DevelopmentData defaultDevelopmentData = new DevelopmentData();

    public SharedCheckPipelineStorage getStorage() {
        return storage;
    }

    public DevelopmentData getDevelopmentData(HoneyCheck check) {
        return developmentDataMap.get(check.getID(), DevelopmentData::new);
    }

    public DevelopmentData getDefaultDevelopmentData() {
        return defaultDevelopmentData;
    }

    public class DevelopmentData {

        private final List<String> developmentDataSet = new ArrayList<>();
        private final List<String> internalData = new ArrayList<>();

        public DevelopmentData() {
            if (defaultDevelopmentData != null && defaultDevelopmentData != this) {
                developmentDataSet.addAll(defaultDevelopmentData.developmentDataSet);
                internalData.addAll(defaultDevelopmentData.internalData);
            }
        }

        public void forDelegated(Consumer<DevelopmentData> run) {
            if (defaultDevelopmentData == this && !developmentDataMap.isEmpty()) {
                for (DevelopmentData developmentDataDelegate : developmentDataMap) {
                    if (developmentDataDelegate != null) run.accept(developmentDataDelegate);
                }
            }
        }

        public int size() {
            return developmentDataSet.size();
        }

        public void addInternalData(String data) {
            internalData.add(data);

            forDelegated(d -> d.addInternalData(data));
        }

        public void removeLastInternalData() {
            internalData.remove(internalData.size() - 1);

            forDelegated(DevelopmentData::removeLastInternalData);
        }

        public List<String> getInternalData() {
            return internalData;
        }

        public void addText(String line) {
            developmentDataSet.add(line);

            forDelegated(d -> d.addText(line));
        }

        public void setText(int index, String line) {
            developmentDataSet.set(index, line);

            forDelegated(d -> d.setText(index, line));
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

    public static class SharedCheckPipelineStorage {

        private final Array<ContainerStructure<?>> dataContextStorage = new Array<>();

        public void putBoolean(boolean bool) {
            put(ContainerStorageTypes.getIndexForBoolean(), bool);
        }

        public void putString(String string) {
            put(ContainerStorageTypes.getIndexForString(), string);
        }

        public void putChar(char character) {
            put(ContainerStorageTypes.getIndexForChar(), character);
        }

        public void putInt(int integer) {
            put(ContainerStorageTypes.getIndexForInt(), integer);
        }

        public void putFloat(float floatValue) {
            put(ContainerStorageTypes.getIndexForFloat(), floatValue);
        }

        public void putByte(byte byteValue) {
            put(ContainerStorageTypes.getIndexForByte(), byteValue);
        }

        public void putDouble(double doubleValue) {
            put(ContainerStorageTypes.getIndexForDouble(), doubleValue);
        }

        public void putLong(long longValue) {
            put(ContainerStorageTypes.getIndexForLong(), longValue);
        }

        public void putLocation(Location location) {
            put(ContainerStorageTypes.getIndexForLocation(), location);
        }

        public String getString(int index) {
            return get(ContainerStorageTypes.getIndexForString(), index);
        }

        public boolean getBoolean(int index) {
            return getVal(index, ContainerStorageTypes.getIndexForBoolean(), false);
        }

        public char getChar(int index) {
            return getVal(index, ContainerStorageTypes.getIndexForChar(), ' ');
        }

        public int getInt(int index) {
            return getVal(index, ContainerStorageTypes.getIndexForInt(), 0);
        }

        public float getFloat(int index) {
            return getVal(index, ContainerStorageTypes.getIndexForFloat(), 0f);
        }

        public byte getByte(int index) {
            return getVal(index, ContainerStorageTypes.getIndexForByte(), (byte) 0);
        }

        public double getDouble(int index) {
            return getVal(index, ContainerStorageTypes.getIndexForDouble(), 0d);
        }

        public long getLong(int index) {
            return getVal(index, ContainerStorageTypes.getIndexForLong(), 0L);
        }

        public Location getLocation(int index) {
            return get(index, ContainerStorageTypes.getIndexForLocation());
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
}
