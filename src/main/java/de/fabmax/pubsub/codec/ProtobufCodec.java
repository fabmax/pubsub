package de.fabmax.pubsub.codec;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import de.fabmax.pubsub.*;
import de.fabmax.pubsub.Message;
import org.pmw.tinylog.Logger;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by Max on 28.02.2015.
 */
public class ProtobufCodec extends Codec {

    private static final int MAGIC = 0xefb24af1;
    private static final byte[] MAGIC_BYTES = new byte[] {
            (byte) (MAGIC >> 24), (byte) (MAGIC >> 16), (byte) (MAGIC >> 8), (byte) MAGIC
    };
    private static final int MAX_MSG_SIZE = 512 * 1024;

    private final Deque<Message> mReceivedMessages = new ArrayDeque<>();
    private final byte[] mReceiveBuffer = new byte[MAX_MSG_SIZE + 8];
    private int mBufferPos = 0;

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
                } else if (pktLen >= MAX_MSG_SIZE) {
                    Logger.error("To large packet size: " + pktLen + " bytes, max is: " + MAX_MSG_SIZE + " bytes");
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

    public byte[] encode(Message message) {
        ProtobufMessage.ProtoMessage.Builder builder = ProtobufMessage.ProtoMessage.newBuilder();

        builder.setChannel(message.getChannelId());
        builder.setTopic(message.getTopic());

        Bundle bundle = message.getData();
        if (bundle != null && !bundle.isEmpty()) {
            ProtobufMessage.ProtoMessage.Bundle.Builder bundleBuilder = ProtobufMessage.ProtoMessage.Bundle.newBuilder();
            for (String key : message.getData().keySet()) {
                ProtobufMessage.ProtoMessage.Item.Builder itBuilder = ProtobufMessage.ProtoMessage.Item.newBuilder();
                itBuilder.setKey(key);
                itBuilder.setType(ProtobufMessage.ProtoMessage.Type.valueOf(bundle.getType(key).toString()));
                switch (bundle.getType(key)) {
                    case BOOLEAN:
                        itBuilder.setBoolVal(bundle.getBoolean(key));
                        break;
                    case BOOLEAN_ARRAY:
                        itBuilder.setBoolArray(createBoolArray(bundle.getBooleanArray(key)));
                        break;
                    case BYTE:
                        itBuilder.setIntVal(bundle.getByte(key));
                        break;
                    case BYTE_ARRAY:
                        itBuilder.setByteArray(ByteString.copyFrom(bundle.getByteArray(key)));
                        break;
                    case CHAR:
                        itBuilder.setIntVal(bundle.getChar(key));
                        break;
                    case CHAR_ARRAY:
                        itBuilder.setIntArray(createSint32Array(bundle.getCharArray(key)));
                        break;
                    case DOUBLE:
                        itBuilder.setDoubleVal(bundle.getDouble(key));
                        break;
                    case DOUBLE_ARRAY:
                        itBuilder.setDoubleArray(createDoubleArray(bundle.getDoubleArray(key)));
                        break;
                    case FLOAT:
                        itBuilder.setFloatVal(bundle.getFloat(key));
                        break;
                    case FLOAT_ARRAY:
                        itBuilder.setFloatArray(createFloatArray(bundle.getFloatArray(key)));
                        break;
                    case INT:
                        itBuilder.setIntVal(bundle.getInt(key));
                        break;
                    case INT_ARRAY:
                        itBuilder.setIntArray(createSint32Array(bundle.getIntArray(key)));
                        break;
                    case LONG:
                        itBuilder.setLongVal(bundle.getLong(key));
                        break;
                    case LONG_ARRAY:
                        itBuilder.setLongArray(createSint64Array(bundle.getLongArray(key)));
                        break;
                    case SHORT:
                        itBuilder.setIntVal(bundle.getShort(key));
                        break;
                    case SHORT_ARRAY:
                        itBuilder.setIntArray(createSint32Array(bundle.getShortArray(key)));
                        break;
                    case STRING:
                        itBuilder.setStringVal(bundle.getString(key));
                        break;
                    case STRING_ARRAY:
                        itBuilder.setStringArray(createStringArray(bundle.getStringArray(key)));
                        break;
                }
                bundleBuilder.addData(itBuilder);
            }
            builder.setData(bundleBuilder);
        }
        return builder.build().toByteArray();
    }

    private Message decode(ByteString buf) {
        try {
            ProtobufMessage.ProtoMessage msg = ProtobufMessage.ProtoMessage.parseFrom(buf);

            Message dec = new Message();
            dec.setChannelId(msg.getChannel());
            dec.setTopic(msg.getTopic());

            if (msg.hasData()) {
                Bundle data = new Bundle();
                dec.setData(data);

                ProtobufMessage.ProtoMessage.Bundle bundle = msg.getData();
                for (int i = 0; i < bundle.getDataCount(); i++) {
                    ProtobufMessage.ProtoMessage.Item it = bundle.getData(i);
                    String key = it.getKey();
                    switch (it.getType()) {
                        case BOOLEAN:
                            data.putBoolean(key, it.getBoolVal());
                            break;
                        case BOOLEAN_ARRAY:
                            data.putBooleanArray(key, getBoolArray(it.getBoolArray()));
                            break;
                        case BYTE:
                            data.putByte(key, (byte) it.getIntVal());
                            break;
                        case BYTE_ARRAY:
                            data.putByteArray(key, it.getByteArray().toByteArray());
                            break;
                        case CHAR:
                            data.putChar(key, (char) it.getIntVal());
                            break;
                        case CHAR_ARRAY:
                            data.putCharArray(key, getCharArray(it.getIntArray()));
                            break;
                        case DOUBLE:
                            data.putDouble(key, it.getDoubleVal());
                            break;
                        case DOUBLE_ARRAY:
                            data.putDoubleArray(key, getDoubleArray(it.getDoubleArray()));
                            break;
                        case FLOAT:
                            data.putFloat(key, it.getFloatVal());
                            break;
                        case FLOAT_ARRAY:
                            data.putFloatArray(key, getFloatArray(it.getFloatArray()));
                            break;
                        case INT:
                            data.putInt(key, it.getIntVal());
                            break;
                        case INT_ARRAY:
                            data.putIntArray(key, getIntArray(it.getIntArray()));
                            break;
                        case LONG:
                            data.putLong(key, it.getLongVal());
                            break;
                        case LONG_ARRAY:
                            data.putLongArray(key, getLongArray(it.getLongArray()));
                            break;
                        case SHORT:
                            data.putShort(key, (short) it.getIntVal());
                            break;
                        case SHORT_ARRAY:
                            data.putShortArray(key, getShortArray(it.getIntArray()));
                            break;
                        case STRING:
                            data.putString(key, it.getStringVal());
                            break;
                        case STRING_ARRAY:
                            data.putStringArray(key, getStringArray(it.getStringArray()));
                            break;
                    }
                }
            }
            return dec;

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ProtobufMessage.ProtoMessage.BoolArray.Builder createBoolArray(boolean[] array) {
        ProtobufMessage.ProtoMessage.BoolArray.Builder builder = ProtobufMessage.ProtoMessage.BoolArray.newBuilder();
        for (boolean b : array) {
            builder.addArray(b);
        }
        return builder;
    }

    private boolean[] getBoolArray(ProtobufMessage.ProtoMessage.BoolArray array) {
        boolean[] a = new boolean[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private ProtobufMessage.ProtoMessage.DoubleArray.Builder createDoubleArray(double[] array) {
        ProtobufMessage.ProtoMessage.DoubleArray.Builder builder = ProtobufMessage.ProtoMessage.DoubleArray.newBuilder();
        for (double d : array) {
            builder.addArray(d);
        }
        return builder;
    }

    private double[] getDoubleArray(ProtobufMessage.ProtoMessage.DoubleArray array) {
        double[] a = new double[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private ProtobufMessage.ProtoMessage.FloatArray.Builder createFloatArray(float[] array) {
        ProtobufMessage.ProtoMessage.FloatArray.Builder builder = ProtobufMessage.ProtoMessage.FloatArray.newBuilder();
        for (float f : array) {
            builder.addArray(f);
        }
        return builder;
    }

    private float[] getFloatArray(ProtobufMessage.ProtoMessage.FloatArray array) {
        float[] a = new float[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private ProtobufMessage.ProtoMessage.Sint32Array.Builder createSint32Array(int[] array) {
        ProtobufMessage.ProtoMessage.Sint32Array.Builder builder = ProtobufMessage.ProtoMessage.Sint32Array.newBuilder();
        for (int i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private int[] getIntArray(ProtobufMessage.ProtoMessage.Sint32Array array) {
        int[] a = new int[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private ProtobufMessage.ProtoMessage.Sint32Array.Builder createSint32Array(char[] array) {
        ProtobufMessage.ProtoMessage.Sint32Array.Builder builder = ProtobufMessage.ProtoMessage.Sint32Array.newBuilder();
        for (int i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private char[] getCharArray(ProtobufMessage.ProtoMessage.Sint32Array array) {
        char[] a = new char[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (char) array.getArray(i);
        }
        return a;
    }

    private ProtobufMessage.ProtoMessage.Sint32Array.Builder createSint32Array(short[] array) {
        ProtobufMessage.ProtoMessage.Sint32Array.Builder builder = ProtobufMessage.ProtoMessage.Sint32Array.newBuilder();
        for (int i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private short[] getShortArray(ProtobufMessage.ProtoMessage.Sint32Array array) {
        short[] a = new short[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (short) array.getArray(i);
        }
        return a;
    }

    private ProtobufMessage.ProtoMessage.Sint64Array.Builder createSint64Array(long[] array) {
        ProtobufMessage.ProtoMessage.Sint64Array.Builder builder = ProtobufMessage.ProtoMessage.Sint64Array.newBuilder();
        for (long l : array) {
            builder.addArray(l);
        }
        return builder;
    }

    private long[] getLongArray(ProtobufMessage.ProtoMessage.Sint64Array array) {
        long[] a = new long[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }

    private ProtobufMessage.ProtoMessage.StringArray.Builder createStringArray(String[] array) {
        ProtobufMessage.ProtoMessage.StringArray.Builder builder = ProtobufMessage.ProtoMessage.StringArray.newBuilder();
        for (String i : array) {
            builder.addArray(i);
        }
        return builder;
    }

    private String[] getStringArray(ProtobufMessage.ProtoMessage.StringArray array) {
        String[] a = new String[array.getArrayCount()];
        for (int i = 0; i < a.length; i++) {
            a[i] = array.getArray(i);
        }
        return a;
    }
}
