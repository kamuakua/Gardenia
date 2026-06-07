package cn.gardenia.client.module;

import java.util.function.Consumer;

public class Setting<T> {
    public enum SettingType {
        BOOLEAN, INTEGER, DOUBLE, MODE, COLOR
    }

    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private final Consumer<T> onChange;
    private final SettingType type;
    private String[] options;
    private double min;
    private double max;
    private double step;

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChange = null;
        this.options = null;
        this.min = 0;
        this.max = 100;
        this.step = 1;
        this.type = inferType(defaultValue);
    }

    public Setting(String name, String description, T defaultValue, Consumer<T> onChange) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChange = onChange;
        this.options = null;
        this.min = 0;
        this.max = 100;
        this.step = 1;
        this.type = inferType(defaultValue);
    }

    public Setting(String name, String description, T defaultValue, String[] options) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChange = null;
        this.options = options;
        this.min = 0;
        this.max = 100;
        this.step = 1;
        this.type = inferType(defaultValue);
    }

    public Setting(String name, String description, T defaultValue, Consumer<T> onChange, String[] options) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChange = onChange;
        this.options = options;
        this.min = 0;
        this.max = 100;
        this.step = 1;
        this.type = inferType(defaultValue);
    }

    // Integer with range
    public Setting(String name, String description, int defaultValue, int min, int max) {
        this.name = name;
        this.description = description;
        this.value = (T) Integer.valueOf(defaultValue);
        this.defaultValue = (T) Integer.valueOf(defaultValue);
        this.onChange = null;
        this.options = null;
        this.min = min;
        this.max = max;
        this.step = 1;
        this.type = SettingType.INTEGER;
    }

    public Setting(String name, String description, int defaultValue, int min, int max, Consumer<T> onChange) {
        this.name = name;
        this.description = description;
        this.value = (T) Integer.valueOf(defaultValue);
        this.defaultValue = (T) Integer.valueOf(defaultValue);
        this.onChange = onChange;
        this.options = null;
        this.min = min;
        this.max = max;
        this.step = 1;
        this.type = SettingType.INTEGER;
    }

    // Explicit COLOR type (wraps Integer)
    public static Setting<Integer> color(String name, String description, int defaultColor) {
        return new Setting<>(name, description, defaultColor, SettingType.COLOR);
    }

    public static Setting<Integer> color(String name, String description, int defaultColor, Consumer<Integer> onChange) {
        return new Setting<>(name, description, defaultColor, onChange, SettingType.COLOR);
    }

    // Internal: construct with explicit type
    private Setting(String name, String description, T defaultValue, SettingType type) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChange = null;
        this.options = null;
        this.min = 0;
        this.max = 100;
        this.step = 1;
        this.type = type;
    }

    private Setting(String name, String description, T defaultValue, Consumer<T> onChange, SettingType type) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.onChange = onChange;
        this.options = null;
        this.min = 0;
        this.max = 100;
        this.step = 1;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    private SettingType inferType(T val) {
        if (val instanceof Boolean) return SettingType.BOOLEAN;
        if (val instanceof Integer) return SettingType.INTEGER;
        if (val instanceof Double) return SettingType.DOUBLE;
        if (val instanceof Float) return SettingType.DOUBLE;
        if (val instanceof String) return SettingType.MODE;
        return SettingType.BOOLEAN;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T getValue() { return value; }
    public T getDefaultValue() { return defaultValue; }
    public SettingType getType() { return type; }
    public String[] getOptions() { return options; }
    public void setOptions(String[] options) { this.options = options; }
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }
    public void setRange(double min, double max, double step) {
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public void setValue(T value) {
        this.value = value;
        if (onChange != null) onChange.accept(value);
    }

    public void reset() { setValue(defaultValue); }

    public int getColor() {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0xFFFFFFFF;
    }

    public double getDouble() {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    public int getInt() {
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    public boolean getBoolean() {
        if (value instanceof Boolean) return (Boolean) value;
        return false;
    }

    public String getString() {
        if (value instanceof String) return (String) value;
        return "";
    }
}
