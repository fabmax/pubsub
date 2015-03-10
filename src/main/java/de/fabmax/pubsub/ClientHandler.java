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
public class ClientHandler implements ChannelProvider, MessageListener {

    private final ServerNode mServer;
    private final Connection mClientConnection;
    private final String mClientAddress;
    private final Channel mControlChannel;

    private final HashSet<String> mRegisteredChannels = new HashSet<>();

    public ClientHandler(ServerNode server, Socket clientSock, boolean isDaemon) throws IOException {
        mClientConnection = new Connection(clientSock, Codec.defaultCodecFactory, isDaemon);
        mClientAddress = clientSock.getRemoteSocketAddress().toString();
        mServer = server;
        mControlChannel = new Channel(this, ControlMessages.CONTROL_CHANNEL_ID);
        mControlChannel.addMessageListener(new MessageMapper(this));

        mClientConnection.setMessageListener(this);
        mClientConnection.open();
    }

    public String getClientAddress() {
        return mClientAddress;
    }

    /**
     * Is called by {@link de.fabmax.pubsub.ServerNode} when server is closed.
     */
    void close() {
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
    public void publish(Message message) {
        if (mRegisteredChannels.contains(message.getChannelId())) {
            mClientConnection.sendMessage(message);
        }
    }

    @ChannelEndpoint
    public void registerChannel(@EndpointParameter(name = "channelId") String channelId) {
        Logger.debug(mClientAddress + " registered channel: " + channelId);
        mRegisteredChannels.add(channelId);
    }
}
