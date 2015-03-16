package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.util.*;

/**
 * Created by Max on 24.02.2015.
 */
public abstract class Node implements ChannelProvider, MessageListener {

    private final long mNodeId;

    private final List<NodeListener> mNodeListeners = new ArrayList<>();
    protected final HashMap<String, Channel> mChannels = new HashMap<>();

    protected Node() {
        // naive approach for generating a unique id, but should be good enough
        mNodeId = new Random().nextLong();
    }

    public final long getNodeId() {
        return mNodeId;
    }

    public void addNodeListener(NodeListener listener) {
        mNodeListeners.add(listener);
    }

    public void removeNodeListener(NodeListener listener) {
        mNodeListeners.remove(listener);
    }

    protected void fireOnConnect() {
        for (NodeListener l : mNodeListeners) {
            l.onConnect();
        }
    }

    protected void fireOnDisconnect() {
        for (NodeListener l : mNodeListeners) {
            l.onDisconnect();
        }
    }

    protected void fireOnRemoteNoteConnected(long nodeId) {
        for (NodeListener l : mNodeListeners) {
            l.onRemoteNodeConnected(nodeId);
        }
    }

    protected void fireOnRemoteNoteDisconnected(long nodeId) {
        for (NodeListener l : mNodeListeners) {
            l.onRemoteNodeDisconnected(nodeId);
        }
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
