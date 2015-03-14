package de.fabmax.pubsub;

/**
 * A Point-to-point {@link de.fabmax.pubsub.Message}.
 */
public class PtpMessage extends Message {

    private static final String KEY_FROM_NODE = "de.fabmax.pubsub.FROM";
    private static final String KEY_TO_NODE = "de.fabmax.pubsub.TO";

    public static boolean isPtpMessage(Message msg) {
        return msg.getData() != null &&
                msg.getData().containsKey(KEY_FROM_NODE) &&
                msg.getData().containsKey(KEY_TO_NODE);
    }

    public static long getFromNodeId(Message ptpMsg) {
        if (!isPtpMessage(ptpMsg)) {
            throw new IllegalArgumentException("Supplied message is not a PtpMessage");
        }
        return ptpMsg.getData().getLong(KEY_FROM_NODE);
    }

    public static long getToNodeId(Message ptpMsg) {
        if (!isPtpMessage(ptpMsg)) {
            throw new IllegalArgumentException("Supplied message is not a PtpMessage");
        }
        return ptpMsg.getData().getLong(KEY_TO_NODE);
    }

    public PtpMessage(Node fromNode, long toNodeId) {
        mData = new Bundle();
        mData.putLong(KEY_FROM_NODE, fromNode.getNodeId());
        mData.putLong(KEY_TO_NODE, toNodeId);
    }

    public PtpMessage(Message message) {
        if (!isPtpMessage(message)) {
            throw new IllegalArgumentException("Supplied message is not a PtpMessage");
        }
        mData = new Bundle();
        setData(message.getData());
    }

    public long getFromNodeId() {
        return mData.getLong(KEY_FROM_NODE);
    }

    public long getToNodeId() {
        return mData.getLong(KEY_TO_NODE);
    }

    @Override
    public void setData(Bundle data) {
        mData.putBundle(data);
    }
}
