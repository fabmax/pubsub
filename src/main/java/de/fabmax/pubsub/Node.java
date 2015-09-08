package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * Created by Max on 24.02.2015.
 */
public abstract class Node implements Closeable, ChannelProvider, MessageListener {

    private final long mNodeId;

    private final List<NodeListener> mNodeListeners = new ArrayList<>();
    protected final HashMap<String, Channel> mChannels = new HashMap<>();

    protected Node() {
        // naive approach for generating a unique id, but should be good enough
        this(new Random().nextLong());
    }

    protected Node(long nodeId) {
        mNodeId = nodeId;
    }

    public final long getNodeId() {
        return mNodeId;
    }

    public void addNodeListener(NodeListener listener) {
        synchronized (mNodeListeners) {
            mNodeListeners.add(listener);
        }
    }

    public void removeNodeListener(NodeListener listener) {
        synchronized (mNodeListeners) {
            mNodeListeners.remove(listener);
        }
    }

    protected void fireOnConnect() {
        synchronized (mNodeListeners) {
            for (NodeListener l : mNodeListeners) {
                l.onConnect();
            }
        }
    }

    protected void fireOnDisconnect() {
        synchronized (mNodeListeners) {
            for (NodeListener l : mNodeListeners) {
                l.onDisconnect();
            }
        }
    }

    protected void fireOnRemoteNoteConnected(long nodeId) {
        synchronized (mNodeListeners) {
            for (NodeListener l : mNodeListeners) {
                l.onRemoteNodeConnected(nodeId);
            }
        }
    }

    protected void fireOnRemoteNoteDisconnected(long nodeId) {
        synchronized (mNodeListeners) {
            for (NodeListener l : mNodeListeners) {
                l.onRemoteNodeDisconnected(nodeId);
            }
        }
    }

    public abstract Set<Long> getKnownNodeIds();

    public abstract void open() throws IOException;

    @Override
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
            registerChannel(channel);
        }
        return channel;
    }

    protected void registerChannel(Channel channel) {
        mChannels.put(channel.getChannelId(), channel);
    }

}
