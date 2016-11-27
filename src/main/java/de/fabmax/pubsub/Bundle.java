package de.fabmax.pubsub;

import java.util.Arrays;
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
        BYTE_ARRAY(byte[].class),
        DOUBLE(Double.class),
        DOUBLE_ARRAY(double[].class),
        FLOAT(Float.class),
        FLOAT_ARRAY(float[].class),
        INT(Integer.class),
        INT_ARRAY(int[].class),
        LONG(Long.class),
        LONG_ARRAY(long[].class),
        STRING(String.class),
        STRING_ARRAY(String[].class),
        BUNDLE(Bundle.class),
        BUNDLE_ARRAY(Bundle[].class);

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
        Item it = mData.get(key);
        if (it != null) {
            Object o = it.value;
            if (o != null && o.getClass() == clazz) {
                return clazz.cast(o);
            }
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

    public Bundle getBundle(String key) {
        return typedGet(Bundle.class, key);
    }

    public Bundle[] getBundleArray(String key) {
        return typedGet(Bundle[].class, key);
    }

    public byte[] getByteArray(String key) {
        return typedGet(byte[].class, key);
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

    public void putBoolean(String key, boolean value) {
        mData.put(key, new Item(ElementType.BOOLEAN, value));
    }

    public void putBooleanArray(String key, boolean[] value) {
        mData.put(key, new Item(ElementType.BOOLEAN_ARRAY, value));
    }

    public void putBundle(String key, Bundle value) {
        mData.put(key, new Item(ElementType.BUNDLE, value));
    }

    public void putBundleArray(String key, Bundle[] value) {
        mData.put(key, new Item(ElementType.BUNDLE_ARRAY, value));
    }

    public void putByteArray(String key, byte[] value) {
        mData.put(key, new Item(ElementType.BYTE_ARRAY, value));
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

    public void putString(String key, String value) {
        mData.put(key, new Item(ElementType.STRING, value));
    }

    public void putStringArray(String key, String[] value) {
        mData.put(key, new Item(ElementType.STRING_ARRAY, value));
    }

    public String prettyPrint() {
        return print("");
    }

    private String print(String indent) {
        StringBuilder buf = new StringBuilder();
        buf.append("{\n");
        for (String key : keySet()) {
            ElementType t = getType(key);
            String val;
            switch (t) {
                case BOOLEAN_ARRAY: val = Arrays.toString(getBooleanArray(key)); break;
                case BYTE_ARRAY:    val = Arrays.toString(getByteArray(key));    break;
                case DOUBLE_ARRAY:  val = Arrays.toString(getDoubleArray(key));  break;
                case FLOAT_ARRAY:   val = Arrays.toString(getFloatArray(key));   break;
                case INT_ARRAY:     val = Arrays.toString(getIntArray(key));     break;
                case LONG_ARRAY:    val = Arrays.toString(getLongArray(key));    break;
                case STRING_ARRAY:  val = Arrays.toString(getStringArray(key));  break;
                case BUNDLE:        val = getBundle(key).print(indent + "  ");   break;
                case BUNDLE_ARRAY:  val = print(getBundleArray(key), indent + "  "); break;
                default: val = get(key).toString(); break;
            }
            buf.append(String.format("%s  \"%s\": %s\n", indent, key, val));
        }
        buf.append(indent).append("}");
        return buf.toString();
    }

    private static String print(Bundle[] bundles, String indent) {
        StringBuilder buf = new StringBuilder();
        buf.append("[");
        for (int i = 0; i < bundles.length; i++) {
            if (i > 0) {
                buf.append(indent);
            }
            Bundle bnd = bundles[i];
            buf.append(bnd.print(indent + "  "));
            if (i < bundles.length - 1) {
                buf.append(",\n").append(indent);
            }
        }
        return buf.append("]").toString();
    }
}
