package de.fabmax.pubsub;

import de.fabmax.pubsub.util.ChannelEndpoint;
import de.fabmax.pubsub.util.EndpointParameter;
import de.fabmax.pubsub.util.MessageMapper;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;

/**
 * ClientHandler has to be public for reflections used by {@link de.fabmax.pubsub.util.MessageMapper} to work.
 */
public class ClientHandler implements ChannelProvider, ConnectionListener {

    private final ServerNode mServer;
    private final Connection mClientConnection;
    private final String mClientAddress;
    private final Channel mControlChannel;
    private long mNodeId = 0;
    private boolean mClosed = false;

    private final HashSet<String> mRegisteredChannels = new HashSet<>();

    public ClientHandler(ServerNode server, Socket clientSock, boolean isDaemon) throws IOException {
        mClientConnection = new Connection(clientSock, Codec.defaultCodecFactory, isDaemon);
        mClientAddress = clientSock.getRemoteSocketAddress().toString();
        mServer = server;
        mControlChannel = new Channel(this, ControlMessages.CONTROL_CHANNEL_ID);
        mControlChannel.addMessageListener(new MessageMapper(this));

        mClientConnection.setConnectionListener(this);
        mClientConnection.open();
    }

    public long getClientNodeId() {
        return mNodeId;
    }

    public String getClientAddress() {
        return mClientAddress;
    }

    /**
     * Is called by {@link de.fabmax.pubsub.ServerNode} when server is closed.
     */
    void close() {
        mClosed = true;
        if (!mClientConnection.isClosed()) {
            mClientConnection.close();
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        if (message.getChannelId().equals(ControlMessages.CONTROL_CHANNEL_ID)) {
            mControlChannel.onMessageReceived(message);
        } else {
            mServer.clientMessageReceived(this, message);
        }
    }

    @Override
    public void onConnectionClosed() {
        if (!mClosed) {
            // if mClosed is true, this handler was closed by the server and we don't have to notify him anymore
            mServer.clientDisconnected(this);
            mClosed = true;
        }
    }

    @Override
    public void publish(Message message) {
        if (mRegisteredChannels.contains(message.getChannelId())) {
            mClientConnection.sendMessage(message);
        }
    }

    protected void sendControlMessage(Message ctrlMessage) {
        ctrlMessage.setChannelId(ControlMessages.CONTROL_CHANNEL_ID);
        mClientConnection.sendMessage(ctrlMessage);
    }

    @ChannelEndpoint
    public void registerNode(@EndpointParameter(name = "nodeId") long nodeId) {
        Logger.debug(mClientAddress + " registered node: " + nodeId);
        mNodeId = nodeId;
        mServer.clientRegistered(mNodeId, this);
    }

    @ChannelEndpoint
    public void registerChannel(@EndpointParameter(name = "channelId") String channelId) {
        Logger.debug(mClientAddress + " registered channel: " + channelId);
        mRegisteredChannels.add(channelId);
    }
}
