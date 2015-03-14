package de.fabmax.pubsub;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * Created by Max on 24.02.2015.
 */
public class Bundle {

    public enum ElementType {
        BOOLEAN(Boolean.class),
        BOOLEAN_ARRAY(boolean[].class),
        BYTE(Byte.class),
        BYTE_ARRAY(byte[].class),
        CHAR(Character.class),
        CHAR_ARRAY(char[].class),
        DOUBLE(Double.class),
        DOUBLE_ARRAY(double[].class),
        FLOAT(Float.class),
        FLOAT_ARRAY(float[].class),
        INT(Integer.class),
        INT_ARRAY(int[].class),
        LONG(Long.class),
        LONG_ARRAY(long[].class),
        SHORT(Short.class),
        SHORT_ARRAY(short[].class),
        STRING(String.class),
        STRING_ARRAY(String[].class);

        private Class<?> mType;

        private ElementType(Class<?> type) {
            mType = type;
        }

        public Class<?> getType() {
            return mType;
        }
    }

    private static class Item {
        public ElementType type;
        public Object value;

        public Item(ElementType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private final Map<String, Item> mData = new Hashtable<>();

    private <T> T typedGet(Class<T> clazz, String key) {
        Object o = mData.get(key).value;
        if (o != null && o.getClass() == clazz) {
            return clazz.cast(o);
        }
        return null;
    }

    public boolean isEmpty() {
        return mData.isEmpty();
    }

    public boolean containsKey(String key) {
        return mData.containsKey(key);
    }

    public Set<String> keySet() {
        return mData.keySet();
    }

    public ElementType getType(String key) {
        Item item = mData.get(key);
        if (item != null) {
            return item.type;
        }
        return null;
    }

    public Object get(String key) {
        Item item = mData.get(key);
        return item != null ? item.value : null;
    }

    public Boolean getBoolean(String key) {
        return typedGet(Boolean.class, key);
    }

    public boolean[] getBooleanArray(String key) {
        return typedGet(boolean[].class, key);
    }

    public Byte getByte(String key) {
        return typedGet(Byte.class, key);
    }

    public byte[] getByteArray(String key) {
        return typedGet(byte[].class, key);
    }

    public Character getChar(String key) {
        return typedGet(Character.class, key);
    }

    public char[] getCharArray(String key) {
        return typedGet(char[].class, key);
    }

    public Double getDouble(String key) {
        return typedGet(Double.class, key);
    }

    public double[] getDoubleArray(String key) {
        return typedGet(double[].class, key);
    }

    public Float getFloat(String key) {
        return typedGet(Float.class, key);
    }

    public float[] getFloatArray(String key) {
        return typedGet(float[].class, key);
    }

    public Integer getInt(String key) {
        return typedGet(Integer.class, key);
    }

    public int[] getIntArray(String key) {
        return typedGet(int[].class, key);
    }

    public Long getLong(String key) {
        return typedGet(Long.class, key);
    }

    public long[] getLongArray(String key) {
        return typedGet(long[].class, key);
    }

    public Short getShort(String key) {
        return typedGet(Short.class, key);
    }

    public short[] getShortArray(String key) {
        return typedGet(short[].class, key);
    }

    public String getString(String key) {
        return typedGet(String.class, key);
    }

    public String[] getStringArray(String key) {
        return typedGet(String[].class, key);
    }

    public void put(String key, ElementType type, Object value) {
        if (!type.getType().isInstance(value)) {
            throw new IllegalArgumentException("value is not of specified type");
        }
        mData.put(key, new Item(type, value));
    }

    public void putBundle(Bundle other) {
        for (String key : other.keySet()) {
            put(key, other.getType(key), other.get(key));
        }
    }

    public void putBoolean(String key, boolean value) {
        mData.put(key, new Item(ElementType.BOOLEAN, value));
    }

    public void putBooleanArray(String key, boolean[] value) {
        mData.put(key, new Item(ElementType.BOOLEAN_ARRAY, value));
    }

    public void putByte(String key, byte value) {
        mData.put(key, new Item(ElementType.BYTE, value));
    }

    public void putByteArray(String key, byte[] value) {
        mData.put(key, new Item(ElementType.BYTE_ARRAY, value));
    }

    public void putChar(String key, char value) {
        mData.put(key, new Item(ElementType.CHAR, value));
    }

    public void putCharArray(String key, char[] value) {
        mData.put(key, new Item(ElementType.CHAR_ARRAY, value));
    }

    public void putDouble(String key, double value) {
        mData.put(key, new Item(ElementType.DOUBLE, value));
    }

    public void putDoubleArray(String key, double[] value) {
        mData.put(key, new Item(ElementType.DOUBLE_ARRAY, value));
    }

    public void putFloat(String key, float value) {
        mData.put(key, new Item(ElementType.FLOAT, value));
    }

    public void putFloatArray(String key, float[] value) {
        mData.put(key, new Item(ElementType.FLOAT_ARRAY, value));
    }

    public void putInt(String key, int value) {
        mData.put(key, new Item(ElementType.INT, value));
    }

    public void putIntArray(String key, int[] value) {
        mData.put(key, new Item(ElementType.INT_ARRAY, value));
    }

    public void putLong(String key, long value) {
        mData.put(key, new Item(ElementType.LONG, value));
    }

    public void putLongArray(String key, long[] value) {
        mData.put(key, new Item(ElementType.LONG_ARRAY, value));
    }

    public void putShort(String key, short value) {
        mData.put(key, new Item(ElementType.SHORT, value));
    }

    public void putShortArray(String key, short[] value) {
        mData.put(key, new Item(ElementType.SHORT_ARRAY, value));
    }

    public void putString(String key, String value) {
        mData.put(key, new Item(ElementType.STRING, value));
    }

    public void putStringArray(String key, String[] value) {
        mData.put(key, new Item(ElementType.STRING_ARRAY, value));
    }

}
