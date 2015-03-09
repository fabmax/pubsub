package de.fabmax.pubsub;

import java.util.HashMap;

/**
 * Created by Max on 24.02.2015.
 */
public abstract class Node implements ChannelProvider, MessageListener {

    protected final HashMap<String, Channel> mChannels = new HashMap<>();

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
        }
        return channel;
    }

}
