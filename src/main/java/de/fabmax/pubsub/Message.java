package de.fabmax.pubsub;

/**
 * Created by Max on 24.02.2015.
 */
public class Message {

    protected String mChannelId;
    protected String mTopic;
    protected Bundle mData;

    public Message() {
        this(null);
    }

    public Message(String topic) {
        this(topic, null);
    }

    public Message(String topic, Bundle data) {
        mTopic = topic;
        mData = data;
    }

    public void setChannelId(String channelId) {
        mChannelId = channelId;
    }

    public String getChannelId() {
        return mChannelId;
    }

    public String getTopic() {
        return mTopic;
    }

    public void setTopic(String topic) {
        mTopic = topic;
    }

    public void setData(Bundle data) {
        mData = data;
    }

    public Bundle getData() {
        return mData;
    }

    @Override
    public String toString() {
        return String.format("Message:{channel:%s;topic:%s}", mChannelId, mTopic);
    }

    public String prettyPrint() {
        if (mData == null) {
            return String.format("[%s#%s]", mChannelId, mTopic);
        } else {
            return String.format("[%s#%s] %s", mChannelId, mTopic, mData.prettyPrint());
        }
    }
}
