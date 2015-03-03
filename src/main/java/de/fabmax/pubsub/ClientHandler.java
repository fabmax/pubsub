package de.fabmax.pubsub;

import de.fabmax.pubsub.util.ChannelEndpoint;
import de.fabmax.pubsub.util.EndpointParameter;
import de.fabmax.pubsub.util.MessageMapper;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;

/**
 * Created by Max on 24.02.2015.
 */
public class ClientHandler implements ChannelProvider, ReceiveListener {

    private final ServerNode mServer;
    private final Connection mClientConnection;
    private final String mClientAddress;
    private final Channel mControlChannel;

    private final HashSet<String> mRegisteredChannels = new HashSet<>();

    public ClientHandler(ServerNode server, Socket clientSock, boolean isDaemon) throws IOException {
        mClientConnection = new Connection(clientSock, Codec.defaultCodecFactory, isDaemon);
        mClientAddress = clientSock.getInetAddress().getHostName() + ":" + clientSock.getLocalPort();
        mServer = server;
        mControlChannel = new Channel(this, ControlMessages.CONTROL_CHANNEL_ID);
        mControlChannel.addChannelListener(new MessageMapper(this));

        mClientConnection.setReceiveListener(this);
        mClientConnection.open();
    }

    public String getClientAddress() {
        return mClientAddress;
    }

    public void close() {
        if (!mClientConnection.isClosed()) {
            mClientConnection.close();
            mServer.clientDisconnected(this);
        }
    }

    @Override
    public void messageReceived(Message message) {
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
