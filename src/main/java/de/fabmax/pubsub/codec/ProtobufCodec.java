package de.fabmax.pubsub.codec;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import de.fabmax.pubsub.*;
import de.fabmax.pubsub.Message;
import org.pmw.tinylog.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Max on 28.02.2015.
 */
public class ProtobufCodec extends Codec {

    private static final int MAGIC = 0xefb24af1;
    private static final byte[] MAGIC_BYTES = new byte[] {
            (byte) (MAGIC >> 24), (byte) (MAGIC >> 16), (byte) (MAGIC >> 8), (byte) MAGIC
    };
    private static final int DEFAULT_MAX_MSG_SIZE = 16 * 1024 * 1024;

    private final Deque<Message> mReceivedMessages = new ArrayDeque<>();
    private final byte[] mReceiveBuffer;
    private final int mMaxMsgSize;
    private int mBufferPos = 0;

    public ProtobufCodec() {
        this(DEFAULT_MAX_MSG_SIZE);
    }

    public ProtobufCodec(int maxMsgSize) {
        mMaxMsgSize = maxMsgSize;
        mReceiveBuffer = new byte[maxMsgSize + 8];
    }

    @Override
    public byte[] encodeMessage(Message message) {
        byte[] payload = encode(message);
        byte[] data = new byte[payload.length + 8];

        System.arraycopy(MAGIC_BYTES, 0, data, 0, 4);
        System.arraycopy(payload, 0, data, 8, payload.length);

        data[4] = (byte) (payload.length >> 24);
        data[5] = (byte) (payload.length >> 16);
        data[6] = (byte) (payload.length >> 8);
        data[7] = (byte) payload.length;

        return data;
    }

    @Override
    public void decodeData(byte[] buf, int off, int len) {
        while (len > 0) {
            int cpLen = Math.min(len, mReceiveBuffer.length - mBufferPos);
            System.arraycopy(buf, off, mReceiveBuffer, mBufferPos, cpLen);
            mBufferPos += cpLen;
            len -= cpLen;

            // search for sync bytes / packet start
            int startPos = 0;
            while (!hasSync(mReceiveBuffer, startPos) && startPos < mBufferPos) {
                startPos++;
            }
            if (startPos > 0) {
                // skip garbage data before sync, this only happens if we have lost sync before
                if (startPos == mBufferPos) {
                    mBufferPos = 0;
                    startPos = 0;
                } else {
                    System.arraycopy(mReceiveBuffer, startPos, mReceiveBuffer, 0, mBufferPos - startPos);
                    mBufferPos -= startPos;
                    startPos = 0;
                }
            }

            while (mBufferPos > 8) {
                // valid sync bytes found + packet length available
                int pktLen = ((mReceiveBuffer[startPos + 4] & 0xff) << 24) |
                             ((mReceiveBuffer[startPos + 5] & 0xff) << 16) |
                             ((mReceiveBuffer[startPos + 6] & 0xff) << 8) |
                              (mReceiveBuffer[startPos + 7] & 0xff);
                if (pktLen <= mBufferPos - 8) {
                    // packet is completely available, decode it
                    Message msg = decode(ByteString.copyFrom(mReceiveBuffer, 8, pktLen));
                    if (msg != null) {
                        mReceivedMessages.addLast(msg);
                    }

                    // remove decoded bytes from buffer
                    if (mBufferPos > pktLen + 8) {
                        System.arraycopy(mReceiveBuffer, pktLen + 8, mReceiveBuffer, 0, mBufferPos - pktLen - 8);
                        mBufferPos -= pktLen + 8;
                    } else {
                        mBufferPos = 0;
                    }
                } else if (pktLen >= mMaxMsgSize) {
                    Logger.error("To large packet size: " + pktLen + " bytes, max is: " + mMaxMsgSize + " bytes");
                    // drop data
                    mBufferPos = 0;
                } else {
                    // packet is not yet complete
                    break;
                }
            }
        }
    }

    private boolean hasSync(byte[] buf, int pos) {
        return buf.length >= pos + 4 &&
               buf[pos]     == MAGIC_BYTES[0] &&
               buf[pos + 1] == MAGIC_BYTES[1] &&
               buf[pos + 2] == MAGIC_BYTES[2] &&
               buf[pos + 3] == MAGIC_BYTES[3];
    }

    @Override
    public boolean hasMessage() {
        return !mReceivedMessages.isEmpty();
    }

    @Override
    public Message getNextMessage() {
        return mReceivedMessages.pollFirst();
    }

    private byte[] encode(Message message) {
        BundleOuterClass.BundleMessage.Builder builder = BundleOuterClass.BundleMessage.newBuilder();

        builder.setChannel(message.getChannelId());
        builder.setTopic(message.getTopic());
        Bundle bundle = message.getData();
        if (bundle != null && !bundle.isEmpty()) {
            BundleOuterClass.BundleMessage.Bundle.Builder bundleBuilder = serialize(bundle);
            builder.setData(bundleBuilder);
        }

        return builder.build().toByteArray();
    }

    private BundleOuterClass.BundleMessage.Bundle.Builder serialize(Bundle bundle) {
        BundleOuterClass.BundleMessage.Bundle.Builder bundleBuilder = BundleOuterClass.BundleMessage.Bundle.newBuilder();
        for (String key : bundle.keySet()) {
            BundleOuterClass.BundleMessage.Item.Builder itBuilder = BundleOuterClass.BundleMessage.Item.newBuilder();
            itBuilder.setKey(key);
            switch (bundle.getType(key)) {
                case BOOLEAN:       itBuilder.setBoolVal(bundle.getBoolean(key)); break;
                case BOOLEAN_ARRAY: itBuilder.setBoolArray(createBoolArray(bundle.getBooleanArray(key))); break;
                case BUNDLE:        itBuilder.setBundleVal(serialize(bundle.getBundle(key))); break;
                case BUNDLE_ARRAY:  itBuilder.setBundleArray(createBundleArray(bundle.getBundleArray(key))); break;
                case BYTE_ARRAY:    itBuilder.setByteArray(ByteString.copyFrom(bundle.getByteArray(key))); break;
                case DOUBLE:        itBuilder.setDoubleVal(bundle.getDouble(key)); break;
                case DOUBLE_ARRAY:  itBuilder.setDoubleArray(createDoubleArray(bundle.getDoubleArray(key))); break;
                case FLOAT:         itBuilder.setFloatVal(bundle.getFloat(key)); break;
                case FLOAT_ARRAY:   itBuilder.setFloatArray(createFloatArray(bundle.getFloatArray(key))); break;
                case INT:           itBuilder.setIntVal(bundle.getInt(key)); break;
                case INT_ARRAY:     itBuilder.setIntArray(createSint32Array(bundle.getIntArray(key))); break;
                case LONG:          itBuilder.setLongVal(bundle.getLong(key)); break;
                case LONG_ARRAY:    itBuilder.setLongArray(createSint64Array(bundle.getLongArray(key))); break;
                case STRING:        itBuilder.setStringVal(bundle.getString(key)); break;
                case STRING_ARRAY:  itBuilder.setStringArray(createStringArray(bundle.getStringArray(key))); break;
            }
            bundleBuilder.addData(itBuilder);
        }
        return bundleBuilder;
    }

    private Bundle deserilaize(BundleOuterClass.BundleMessage.BundleOrBuilder bundle) {
        Bundle data = new Bundle();
        for (int i = 0; i < bundle.getDataCount(); i++) {
            BundleOuterClass.BundleMessage.Item it = bundle.getData(i);
            String key = it.getKey();
            switch (it.getValueCase()) {
                case BOOLVAL:     data.putBoolean(key, it.getBoolVal()); break;
                case BOOLARRAY:   data.putBooleanArray(key, getBoolArray(it.getBoolArray())); break;
                case BUNDLEVAL:   data.putBundle(key, deserilaize(it.getBundleVal())); break;
                case BUNDLEARRAY: data.putBundleArray(key, getBundleArray(it.getBundleArray())); break;
                case BYTEARRAY:   data.putByteArray(key, it.getByteArray().toByteArray()); break;
                case DOUBLEVAL:   data.putDouble(key, it.getDoubleVal()); break;
                case DOUBLEARRAY: data.putDoubleArray(key, getDoubleArray(it.getDoubleArray())); break;
                case FLOATVAL:    data.putFloat(key, it.getFloatVal()); break;
                case FLOATARRAY:  data.putFloatArray(key, getFloatArray(it.getFloatArray())); break;
                case INTVAL:      data.putInt(key, it.getIntVal()); break;
                case INTARRAY:    data.putIntArray(key, getIntArray(it.getIntArray())); break;
                case LONGVAL:     data.putLong(key, it.getLongVal()); break;
                case LONGARRAY:   data.putLongArray(key, getLongArray(it.getLongArray())); break;
                case STRINGVAL:   data.putString(key, it.getStringVal()); break;
                case STRINGARRAY: data.putStringArray(key, getStringArray(it.getStringArray())); break;
            }
        }
        return data;
    }

    private Message decode(ByteString buf) {
        try {
            BundleOuterClass.BundleMessage msg = BundleOuterClass.BundleMessage.parseFrom(buf);

            Message dec = new Message();
            dec.setChannelId(msg.getChannel());
            dec.setTopic(msg.getTopic());

            if (msg.hasData()) {
                Bundle data = new Bundle();
                dec.setData(data);

                BundleOuterClass.BundleMessage.Bundle bundle = msg.getData();
                for (int i = 0; i < bundle.getDataCount(); i++) {
                    BundleOuterClass.BundleMessage.Item it = bundle.getData(i);
                    String key = it.getKey();
                    switch (it.getValueCase()) {
                        case BOOLVAL:     data.putBoolean(key, it.getBoolVal()); break;
                        case BOOLARRAY:   data.putBooleanArray(key, getBoolArray(it.getBoolArray())); break;
                        case BUNDLEVAL:   data.putBundle(key, deserilaize(it.getBundleVal())); break;
                        case BUNDLEARRAY: data.putBundleArray(key, getBundleArray(it.getBundleArray())); break;
                        case BYTEARRAY:   data.putByteArray(key, it.getByteArray().toByteArray()); break;
                        case DOUBLEVAL:   data.putDouble(key, it.getDoubleVal()); break;
                        case DOUBLEARRAY: data.putDoubleArray(key, getDoubleArray(it.getDoubleArray())); break;
                        case FLOATVAL:    data.putFloat(key, it.getFloatVal()); break;
                        case FLOATARRAY:  data.putFloatArray(key, getFloatArray(it.getFloatArray())); break;
                        case INTVAL:      data.putInt(key, it.getIntVal()); break;
                        case INTARRAY:    data.putIntArray(key, getIntArray(it.getIntArray())); break;
                        case LONGVAL:     data.putLong(key, it.getLongVal()); break;
                        case LONGARRAY:   data.putLongArray(key, getLongArray(it.getLongArray())); break;
                        case STRINGVAL:   data.putString(key, it.getStringVal()); break;
                        case STRINGARRAY: data.putStringArray(key, getStringArray(it.getStringArray())); break;
                    }
                }
            }
            return dec;

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return null;
    }

    private BundleOuterClass.BundleMessage.BoolArray.Builder createBoolArray(boolean[] array) {
        BundleOuterClass.BundleMessage.BoolArray.Builder builder = BundleOuterClass.BundleMessage.BoolArray.newBuilder();
        for (boolean b : array) {
            builder.addArray(b);
        }
        return builder;
    }

    private boolean[] getBoolArray(BundleOuterClass.BundleMessage.BoolArray array) {
        boolean[] a = new boolean[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private BundleOuterClass.BundleMessage.BundleArray.Builder createBundleArray(Bundle[] array) {
        BundleOuterClass.BundleMessage.BundleArray.Builder builder = BundleOuterClass.BundleMessage.BundleArray.newBuilder();
        for (Bundle b : array) {
            builder.addArray(serialize(b));
        }
        return builder;
    }

    private Bundle[] getBundleArray(BundleOuterClass.BundleMessage.BundleArray array) {
        Bundle[] a = new Bundle[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = deserilaize(array.getArray(i));
        }
        return a;
    }

    private BundleOuterClass.BundleMessage.DoubleArray.Builder createDoubleArray(double[] array) {
        BundleOuterClass.BundleMessage.DoubleArray.Builder builder = BundleOuterClass.BundleMessage.DoubleArray.newBuilder();
        for (double d : array) {
            builder.addArray(d);
        }
        return builder;
    }

    private double[] getDoubleArray(BundleOuterClass.BundleMessage.DoubleArray array) {
        double[] a = new double[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private BundleOuterClass.BundleMessage.FloatArray.Builder createFloatArray(float[] array) {
        BundleOuterClass.BundleMessage.FloatArray.Builder builder = BundleOuterClass.BundleMessage.FloatArray.newBuilder();
        for (float f : array) {
            builder.addArray(f);
        }
        return builder;
    }

    private float[] getFloatArray(BundleOuterClass.BundleMessage.FloatArray array) {
        float[] a = new float[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private BundleOuterClass.BundleMessage.Sint32Array.Builder createSint32Array(int[] array) {
        BundleOuterClass.BundleMessage.Sint32Array.Builder builder = BundleOuterClass.BundleMessage.Sint32Array.newBuilder();
        for (int i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private int[] getIntArray(BundleOuterClass.BundleMessage.Sint32Array array) {
        int[] a = new int[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private BundleOuterClass.BundleMessage.Sint32Array.Builder createSint32Array(char[] array) {
        BundleOuterClass.BundleMessage.Sint32Array.Builder builder = BundleOuterClass.BundleMessage.Sint32Array.newBuilder();
        for (int i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private BundleOuterClass.BundleMessage.Sint32Array.Builder createSint32Array(short[] array) {
        BundleOuterClass.BundleMessage.Sint32Array.Builder builder = BundleOuterClass.BundleMessage.Sint32Array.newBuilder();
        for (int i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private short[] getShortArray(BundleOuterClass.BundleMessage.Sint32Array array) {
        short[] a = new short[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (short) array.getArray(i);
        }
        return a;
    }

    private BundleOuterClass.BundleMessage.Sint64Array.Builder createSint64Array(long[] array) {
        BundleOuterClass.BundleMessage.Sint64Array.Builder builder = BundleOuterClass.BundleMessage.Sint64Array.newBuilder();
        for (long l : array) {
            builder.addArray(l);
        }
        return builder;
    }

    private long[] getLongArray(BundleOuterClass.BundleMessage.Sint64Array array) {
        long[] a = new long[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private BundleOuterClass.BundleMessage.StringArray.Builder createStringArray(String[] array) {
        BundleOuterClass.BundleMessage.StringArray.Builder builder = BundleOuterClass.BundleMessage.StringArray.newBuilder();
        for (String i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private String[] getStringArray(BundleOuterClass.BundleMessage.StringArray array) {
        String[] a = new String[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }
}
