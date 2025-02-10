package me.alek.honey.validation;

import me.alek.honey.util.Array;
import me.alek.honey.util.Callback;
import me.alek.honey.util.statics.NumberUtils;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class ValidationPipelineContext {

    private final Array<Array<DevelopmentData>> developmentDataMap = new Array<>();
    private final Callback callback = new Callback();
    private final boolean useDevelopmentData;

    public ValidationPipelineContext(boolean useDevelopmentData) {
        this.useDevelopmentData = useDevelopmentData;
    }

    public DevelopmentData getDevelopmentData(ViolationKey key) {
        return this.developmentDataMap
                .get(key.getRegistryId(), Array::new)
                .get(key.getValidation().getID(), DevelopmentData::new);
    }

    public boolean usesDevelopmentData() {
        return this.useDevelopmentData;
    }

    public Callback getCallback() {
        return this.callback;
    }

    public static class DevelopmentData {

        private final List<String> developmentDataSet = new ArrayList<>();
        private final List<String> internalData = new ArrayList<>();

        public int size() {
            return this.developmentDataSet.size();
        }

        public void addInternalData(String data) {
            this.internalData.add(data);
        }

        public void removeLastInternalData() {
            this.internalData.remove(this.internalData.size() - 1);
        }

        public List<String> getInternalData() {
            return this.internalData;
        }

        public void addText(String line) {
            this.developmentDataSet.add(line);
        }

        public void setText(int index, String line) {
            this.developmentDataSet.set(index, line);
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
            return this.developmentDataSet;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            for (String data : this.internalData) {
                builder.append(data.trim()).append(", ");
            }
            return builder.substring(0, builder.length() - 1);
        }
    }
}
