package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Max on 24.02.2015.
 */
public class ServerNode extends Node {

    private final boolean mIsDaemon;
    private final ClientAcceptor mClientAcceptor;
    private final List<ClientHandler> mClients = new ArrayList<>();

    private final HashSet<String> mRegisteredChannels = new HashSet<>();

    public ServerNode(int port, boolean isDaemon) throws IOException {
        mIsDaemon = isDaemon;

        mClientAcceptor = new ClientAcceptor(this, port);
        mClientAcceptor.setDaemon(mIsDaemon);
        mClientAcceptor.start();

        Logger.debug("ServerNode started");
    }

    protected void clientConnected(Socket clientSock) {
        Logger.debug("Client connected: " + clientSock.getInetAddress().getHostAddress() +
                ":" + clientSock.getLocalPort());
        try {
            synchronized (mClients) {
                ClientHandler handler = new ClientHandler(this, clientSock, mIsDaemon);
                mClients.add(handler);
            }
        } catch (IOException e) {
            Logger.error("Unable to initialize client connection", e);
        }
    }

    protected void clientDisconnected(ClientHandler client) {
        synchronized (mClients) {
            mClients.remove(client);
        }
        Logger.debug("Client disconnected: " + client.getClientAddress());
    }

    protected void clientMessageReceived(ClientHandler client, Message message) {
        synchronized (mClients) {
            for (ClientHandler handler : mClients) {
                if (handler != client) {
                    handler.publish(message);
                }
            }
        }
        if (mRegisteredChannels.contains(message.getChannelId())) {
            messageReceived(message);
        }
    }

    @Override
    public void close() {
        mClientAcceptor.close();

        synchronized (mClients) {
            for (ClientHandler handler : mClients) {
                handler.close();
            }
            mClients.clear();
        }
        Logger.debug("Server closed");
    }

    @Override
    public Channel openChannel(String channelId) {
        mRegisteredChannels.add(channelId);
        return super.openChannel(channelId);
    }

    @Override
    public void publish(Message message) {
        synchronized (mClients) {
            for (ClientHandler handler : mClients) {
                handler.publish(message);
            }
        }
    }
}
