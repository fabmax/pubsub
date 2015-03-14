package de.fabmax.pubsub.codec;

import de.fabmax.pubsub.Bundle;
import de.fabmax.pubsub.Message;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pmw.tinylog.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by Max on 24.02.2015.
 */
public class JsonCodec extends Codec {

    private final StringBuffer mReceiveBuffer = new StringBuffer();
    private final Deque<Message> mReceivedMessages = new ArrayDeque<>();

    @Override
    public byte[] encodeMessage(Message message) {
        JSONObject obj = new JSONObject();
        obj.put("cId", message.getChannelId());
        obj.put("top", message.getTopic());

        if (message.getData() != null) {
            obj.put("data", serializeBundle(message.getData()));
        }

        return (obj.toString() + "\n").getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void decodeData(byte[] data, int off, int len) {
        String recv = new String(data, off, len, StandardCharsets.UTF_8);
        int lenBefore = mReceiveBuffer.length();
        mReceiveBuffer.append(recv);

        int lfPos = recv.indexOf('\n');
        if (lfPos != -1) {
            lfPos += lenBefore;
        }

        while (lfPos != -1) {
            recv = mReceiveBuffer.substring(0, lfPos);
            mReceiveBuffer.replace(0, lfPos+1, "");
            lfPos = mReceiveBuffer.indexOf("\n");

            try {
                mReceivedMessages.addLast(deserializeMessage(recv));
            } catch (Exception e) {
                Logger.error("Failed to deserialize message: " + mReceiveBuffer, e);
            }
        }
    }

    @Override
    public boolean hasMessage() {
        return !mReceivedMessages.isEmpty();
    }

    @Override
    public Message getNextMessage() {
        return mReceivedMessages.pollFirst();
    }

    private Message deserializeMessage(String representation) throws Exception {
        JSONObject obj = new JSONObject(representation);
        Message msg = new Message();
        msg.setTopic(obj.getString("top"));
        msg.setChannelId(obj.getString("cId"));

        if (obj.has("data")) {
            msg.setData(deserializeBundle(obj.getJSONArray("data")));
        }

        return msg;
    }

    private Bundle deserializeBundle(JSONArray data) {
        Bundle b = new Bundle();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);
            String key = item.getString("k");
            Bundle.ElementType type = Enum.valueOf(Bundle.ElementType.class, item.getString("t"));
            switch (type) {
                case BOOLEAN:
                    b.putBoolean(key, item.getBoolean("v"));
                    break;
                case BOOLEAN_ARRAY:
                    b.putBooleanArray(key, booleanArray(item.getJSONArray("v")));
                    break;
                case BYTE:
                    b.putByte(key, (byte) item.getInt("v"));
                    break;
                case BYTE_ARRAY:
                    b.putByteArray(key, Base64.decode(item.getString("v")));
                    break;
                case CHAR:
                    b.putChar(key, (char) item.getInt("v"));
                    break;
                case CHAR_ARRAY:
                    b.putCharArray(key, charArray(item.getJSONArray("v")));
                    break;
                case DOUBLE:
                    b.putDouble(key, item.getDouble("v"));
                    break;
                case DOUBLE_ARRAY:
                    b.putDoubleArray(key, doubleArray(item.getJSONArray("v")));
                    break;
                case FLOAT:
                    b.putFloat(key, (float) item.getDouble("v"));
                    break;
                case FLOAT_ARRAY:
                    b.putFloatArray(key, floatArray(item.getJSONArray("v")));
                    break;
                case INT:
                    b.putInt(key, item.getInt("v"));
                    break;
                case INT_ARRAY:
                    b.putIntArray(key, intArray(item.getJSONArray("v")));
                    break;
                case LONG:
                    b.putLong(key, item.getLong("v"));
                    break;
                case LONG_ARRAY:
                    b.putLongArray(key, longArray(item.getJSONArray("v")));
                    break;
                case SHORT:
                    b.putShort(key, (short) item.getInt("v"));
                    break;
                case SHORT_ARRAY:
                    b.putShortArray(key, shortArray(item.getJSONArray("v")));
                    break;
                case STRING:
                    b.putString(key, item.getString("v"));
                    break;
                case STRING_ARRAY:
                    b.putStringArray(key, stringArray(item.getJSONArray("v")));
                    break;
            }
        }
        return b;
    }

    private JSONArray serializeBundle(Bundle bundle) {
        JSONArray data = new JSONArray();

        for (String key : bundle.keySet()) {
            Bundle.ElementType type = bundle.getType(key);
            JSONObject item = new JSONObject();
            item.put("k", key);
            item.put("t", type.toString());
            switch (type) {
                case BOOLEAN:
                    item.put("v", bundle.getBoolean(key));
                    break;
                case BOOLEAN_ARRAY:
                    item.put("v", booleanArray(bundle.getBooleanArray(key)));
                    break;
                case BYTE:
                    item.put("v", bundle.getByte(key));
                    break;
                case BYTE_ARRAY:
                    item.put("v", Base64.encode(bundle.getByteArray(key)));
                    break;
                case CHAR:
                    item.put("v", (int) bundle.getChar(key));
                    break;
                case CHAR_ARRAY:
                    item.put("v", charArray(bundle.getCharArray(key)));
                    break;
                case DOUBLE:
                    item.put("v", bundle.getDouble(key));
                    break;
                case DOUBLE_ARRAY:
                    item.put("v", doubleArray(bundle.getDoubleArray(key)));
                    break;
                case FLOAT:
                    item.put("v", bundle.getFloat(key));
                    break;
                case FLOAT_ARRAY:
                    item.put("v", floatArray(bundle.getFloatArray(key)));
                    break;
                case INT:
                    item.put("v", bundle.getInt(key));
                    break;
                case INT_ARRAY:
                    item.put("v", intArray(bundle.getIntArray(key)));
                    break;
                case LONG:
                    item.put("v", bundle.getLong(key));
                    break;
                case LONG_ARRAY:
                    item.put("v", longArray(bundle.getLongArray(key)));
                    break;
                case SHORT:
                    item.put("v", bundle.getShort(key));
                    break;
                case SHORT_ARRAY:
                    item.put("v", shortArray(bundle.getShortArray(key)));
                    break;
                case STRING:
                    item.put("v", bundle.getString(key));
                    break;
                case STRING_ARRAY:
                    item.put("v", stringArray(bundle.getStringArray(key)));
                    break;
            }
            data.put(item);
        }
        return data;
    }

    private JSONArray booleanArray(boolean[] array) {
        JSONArray arr = new JSONArray();
        for (boolean b : array) {
            arr.put(b);
        }
        return arr;
    }

    private boolean[] booleanArray(JSONArray array) {
        boolean[] arr = new boolean[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = array.getBoolean(i);
        }
        return arr;
    }

    private JSONArray charArray(char[] array) {
        JSONArray arr = new JSONArray();
        for (char c : array) {
            arr.put(c);
        }
        return arr;
    }

    private char[] charArray(JSONArray array) {
        char[] arr = new char[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = (char) array.getInt(i);
        }
        return arr;
    }

    private JSONArray doubleArray(double[] array) {
        JSONArray arr = new JSONArray();
        for (double d : array) {
            arr.put(d);
        }
        return arr;
    }

    private double[] doubleArray(JSONArray array) {
        double[] arr = new double[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = array.getDouble(i);
        }
        return arr;
    }

    private JSONArray floatArray(float[] array) {
        JSONArray arr = new JSONArray();
        for (float f : array) {
            arr.put(f);
        }
        return arr;
    }

    private float[] floatArray(JSONArray array) {
        float[] arr = new float[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = (float) array.getDouble(i);
        }
        return arr;
    }

    private JSONArray intArray(int[] array) {
        JSONArray arr = new JSONArray();
        for (int i : array) {
            arr.put(i);
        }
        return arr;
    }

    private int[] intArray(JSONArray array) {
        int[] arr = new int[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = array.getInt(i);
        }
        return arr;
    }

    private JSONArray longArray(long[] array) {
        JSONArray arr = new JSONArray();
        for (long l : array) {
            arr.put(l);
        }
        return arr;
    }

    private long[] longArray(JSONArray array) {
        long[] arr = new long[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = array.getLong(i);
        }
        return arr;
    }

    private JSONArray shortArray(short[] array) {
        JSONArray arr = new JSONArray();
        for (short s : array) {
            arr.put(s);
        }
        return arr;
    }

    private short[] shortArray(JSONArray array) {
        short[] arr = new short[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = (short) array.getInt(i);
        }
        return arr;
    }

    private JSONArray stringArray(String[] array) {
        JSONArray arr = new JSONArray();
        for (String s : array) {
            arr.put(s);
        }
        return arr;
    }

    private String[] stringArray(JSONArray array) {
        String[] arr = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            arr[i] = array.getString(i);
        }
        return arr;
    }
}
