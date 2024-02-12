package me.alek.honey.checks;

import me.alek.honey.checks.types.HoneyCheck;
import me.alek.honey.utility.NumberConversions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckContextContainer {

    private static final DecimalFormat decimalFormatter = new DecimalFormat("#.##");

    private final Storage storage = new Storage();
    private final Map<HoneyCheck, DevelopmentData> developmentDataMap = new HashMap<>();
    private final DevelopmentData defaultDevelopmentData = new DevelopmentData();

    public Storage getStorage() {
        return storage;
    }

    public DevelopmentData getDevelopmentData(HoneyCheck check) {
        return developmentDataMap.computeIfAbsent(check, (d) -> new DevelopmentData());
    }

    public DevelopmentData getDefaultDevelopmentData() {
        return defaultDevelopmentData;
    }

    public class DevelopmentData {

        private final List<String> developmentDataSet = new ArrayList<>();

        public DevelopmentData() {
            if (defaultDevelopmentData != null && defaultDevelopmentData != this) {
                developmentDataSet.addAll(defaultDevelopmentData.developmentDataSet);
            }
        }

        public int size() {
            return developmentDataSet.size();
        }

        public void addText(String line) {
            developmentDataSet.add(line);

            if (defaultDevelopmentData == this) {
                for (DevelopmentData developmentDataDelegate : developmentDataMap.values()) {
                    developmentDataDelegate.addText(line);
                }
            }
        }

        public void setText(int index, String line) {
            developmentDataSet.set(index, line);

            if (defaultDevelopmentData == this) {
                for (DevelopmentData developmentDataDelegate : developmentDataMap.values()) {
                    developmentDataDelegate.setText(index, line);
                }
            }
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
            if (value instanceof Number) value = NumberConversions.format((Number) value);
            addText(" §8* " + key + ": " + value);
        }

        public void addValue(String key, Object value) {
            if (value instanceof Number) value = NumberConversions.format((Number) value);
            addText("§6" + key + ": §e" + value);
        }

        public void setEmpty(int index) {
            setText(index, "");
        }

        public void setLine(int index, String line) {
            setText(index, " §7" + line);
        }

        public void setHeader(int index, String line, String specification) {
            setText(index, "§6* §e" + line + ":" + (specification == null ? "" : " (" + specification + ")"));
        }

        public void setPoint(int index, String key, Object value) {
            if (value instanceof Number) value = NumberConversions.format((Number) value);
            setText(index, " §8* " + key + ": " + value);
        }

        public void setValue(int index, String key, Object value) {
            if (value instanceof Number) value = NumberConversions.format((Number) value);
            setText(index, "§6" + key + ": §e" + value);
        }

        public List<String> getDataSet() {
            return developmentDataSet;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            for (String data : developmentDataSet) {
                builder.append(data.trim()).append(" ");
            }
            return builder.substring(0, builder.length() - 1);
        }
    }

    public static class Storage {

        private final Map<Class<?>, ContainerStructure<?>> dataContextStorageMap = new HashMap<>();

        public void putBoolean(boolean bool) {
            put(Boolean.class, bool);
        }

        public void putString(String string) {
            put(String.class, string);
        }

        public void putChar(char character) {
            put(Character.class, character);
        }

        public void putInt(int integer) {
            put(Integer.class, integer);
        }

        public void putFloat(float floatValue) {
            put(Float.class, floatValue);
        }

        public void putByte(byte byteValue) {
            put(Byte.class, byteValue);
        }

        public void putDouble(double doubleValue) {
            put(Double.class, doubleValue);
        }

        public void putLong(long longValue) {
            put(Long.class, longValue);
        }

        public boolean getBoolean(int index) {
            Boolean bool = get(Boolean.class, index);
            if (bool == null) return false;

            return bool;
        }

        public String getString(int index) {
            return get(String.class, index);
        }

        public char getChar(int index) {
            Character character = get(Character.class, index);
            if (character == null) return ' ';

            return character;
        }

        public int getInt(int index) {
            Integer integer = get(Integer.class, index);
            if (integer == null) return 0;

            return integer;
        }

        public float getFloat(int index) {
            Float floatValue = get(Float.class, index);
            if (floatValue == null) return 0f;

            return floatValue;
        }

        public byte getByte(int index) {
            Byte byteValue = get(Byte.class, index);
            if (byteValue == null) return 0;

            return byteValue;
        }

        public double getDouble(int index) {
            Double doubleValue = get(Double.class, index);
            if (doubleValue == null) return 0d;

            return doubleValue;
        }

        public long getLong(int index) {
            Long longValue = get(Long.class, index);
            if (longValue == null) return 0L;

            return longValue;
        }

        public synchronized <T> void put(Class<T> clazz, T value) {
            ContainerStructure<T> structure = getStructure(clazz);
            if (structure == null) {
                dataContextStorageMap.put(clazz, new ContainerStructure<>());
                put(clazz, value);
                return;
            }
            structure.add(value);
        }

        public synchronized <T> T get(Class<T> clazz, int index) {
            ContainerStructure<T> structure = getStructure(clazz);
            if (structure == null) return null;

            return structure.get(index);
        }

        public synchronized <T> T getLast(Class<T> clazz) {
            ContainerStructure<T> structure = getStructure(clazz);
            if (structure == null) return null;

            return structure.get(structure.getSize());
        }

        public <T> T peek(Class<T> clazz) {
            return get(clazz, 0);
        }

        public <T> T poll(Class<T> clazz) {
            return take(clazz, 0);
        }

        public synchronized <T> T take(Class<T> clazz, int index) {
            ContainerStructure<T> structure = getStructure(clazz);
            if (structure == null) return null;

            T value = structure.get(index);
            structure.remove(index);
            return value;
        }

        public <T> ContainerStructure<T> getStructure(Class<T> clazz) {
            return (ContainerStructure<T>) dataContextStorageMap.get(clazz);
        }

        public final static class ContainerStructure<T> {

            private final List<T> values = new ArrayList<>();

            public void add(T value) {
                values.add(value);
            }

            public T get(int index) {
                if (!has(index)) {
                    return null;
                }
                return values.get(index);
            }

            public void remove(int index) {
                values.remove(index);
            }

            public boolean has(int index) {
                return values.size() > index;
            }

            public int getSize() {
                return values.size();
            }

            public List<T> getValues() {
                return values;
            }
        }

    }
}
