package de.fabmax.pubsub;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max on 24.02.2015.
 */
public class Channel {

    private final ChannelProvider mProvider;
    private final String mChannelId;

    private final List<ChannelListener> mChannelListeners = new ArrayList<>();

    protected Channel(ChannelProvider provider, String channelId) {
        mProvider = provider;
        mChannelId = channelId;
    }

    protected void onMessageReceived(Message message) {
        synchronized (mChannelListeners) {
            for (ChannelListener l : mChannelListeners) {
                l.onMessageReceived(message);
            }
        }
    }

    public void addChannelListener(ChannelListener listener) {
        synchronized (mChannelListeners) {
            mChannelListeners.add(listener);
        }
    }

    public void removeChannelListener(ChannelListener listener) {
        synchronized (mChannelListeners) {
            mChannelListeners.remove(listener);
        }
    }

    public void publish(Message message) {
        message.setChannelId(mChannelId);
        mProvider.publish(message);
    }

}
