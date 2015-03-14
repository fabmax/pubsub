package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 24.02.2015.
 */
public class Channel {

    private final ChannelProvider mProvider;
    private final String mChannelId;

    private final List<MessageListener> mChannelListeners = new ArrayList<>();

    protected Channel(ChannelProvider provider, String channelId) {
        mProvider = provider;
        mChannelId = channelId;
    }

    public String getChannelId() {
        return mChannelId;
    }

    protected void onMessageReceived(Message message) {
        synchronized (mChannelListeners) {
            for (MessageListener l : mChannelListeners) {
                l.onMessageReceived(message);
            }
        }
    }

    public void addMessageListener(MessageListener listener) {
        synchronized (mChannelListeners) {
            mChannelListeners.add(listener);
        }
    }

    public void removeMessageListener(MessageListener listener) {
        synchronized (mChannelListeners) {
            mChannelListeners.remove(listener);
        }
    }

    public void sendPtpMessage(Message message, Node fromNode, long toNodeId) {
        if (!fromNode.getKnownNodeIds().contains(toNodeId)) {
            Logger.warn("sendPtpMessage called with unknown receiver node ID: " + toNodeId);
        }
        PtpMessage ptp = new PtpMessage(fromNode, toNodeId);
        ptp.setTopic(message.getTopic());
        if (message.getData() != null) {
            ptp.setData(message.getData());
        }
        publish(ptp);
    }

    public void publish(Message message) {
        message.setChannelId(mChannelId);
        mProvider.publish(message);
    }

}
