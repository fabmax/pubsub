package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;

/**
 * Created by Max on 24.02.2015.
 */
public abstract class Node implements ChannelProvider, MessageListener {

    private final long mNodeId;

    protected final HashMap<String, Channel> mChannels = new HashMap<>();

    protected Node() {
        // super naive approach for generating a unique id, but should be good enough
        mNodeId = new Random().nextLong();
    }

    public final long getNodeId() {
        return mNodeId;
    }

    public abstract Set<Long> getKnownNodeIds();

    public abstract void close();

    @Override
    public abstract void publish(Message message);

    @Override
    public void onMessageReceived(Message message) {
        Channel channel = mChannels.get(message.getChannelId());
        if (channel != null) {
            channel.onMessageReceived(message);
        }
    }

    public Channel openChannel(String channelId) {
        Channel channel = mChannels.get(channelId);
        if (channel == null) {
            channel = new Channel(this, channelId);
            mChannels.put(channelId, channel);
            registerChannel(channel);
        }
        return channel;
    }

    protected abstract void registerChannel(Channel channel);

}
