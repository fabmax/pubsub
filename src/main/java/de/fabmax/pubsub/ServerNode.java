package de.fabmax.pubsub;

import de.fabmax.pubsub.util.DnsServiceAdvertiser;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Max on 24.02.2015.
 */
public class ServerNode extends Node {

    /** Type string used for DNS service discovery / zeroconf */
    public static final String DNS_SD_TYPE = "_pubsub._tcp.local.";
    /** Default port used for pubsub protocol */
    public static final int DEFAULT_PORT = 9874;

    private final boolean mIsDaemon;
    private final int mPort;
    private final ClientAcceptor mClientAcceptor;
    private final List<ClientHandler> mClients = new ArrayList<>();

    private DnsServiceAdvertiser mServiceAdvertiser = null;

    public ServerNode(int port, boolean isDaemon) throws IOException {
        mIsDaemon = isDaemon;

        mPort = port;
        mClientAcceptor = new ClientAcceptor(this, port);
        mClientAcceptor.setDaemon(mIsDaemon);
        mClientAcceptor.start();

        Logger.info("Server started");
    }

    protected void clientConnected(Socket clientSock) {
        try {
            synchronized (mClients) {
                ClientHandler handler = new ClientHandler(this, clientSock, mIsDaemon);
                mClients.add(handler);
                Logger.info("Client connected: " + handler.getClientAddress());
            }
        } catch (IOException e) {
            Logger.error("Unable to initialize client connection", e);
        }
    }

    protected void clientDisconnected(ClientHandler client) {
        synchronized (mClients) {
            mClients.remove(client);
        }
        Logger.info("Client disconnected: " + client.getClientAddress());
    }

    protected void clientMessageReceived(ClientHandler client, Message message) {
        synchronized (mClients) {
            for (ClientHandler handler : mClients) {
                if (handler != client) {
                    handler.publish(message);
                }
            }
        }
        if (mChannels.containsKey(message.getChannelId())) {
            onMessageReceived(message);
        }
    }

    public void setServiceAdvertisingEnabled(boolean enabled) {
        if (enabled && mServiceAdvertiser == null) {
            mServiceAdvertiser = new DnsServiceAdvertiser(DNS_SD_TYPE, mPort);
        } else if (!enabled && mServiceAdvertiser != null) {
            mServiceAdvertiser.close();
            mServiceAdvertiser = null;
        }
    }

    @Override
    public void close() {
        mClientAcceptor.close();
        setServiceAdvertisingEnabled(false);

        synchronized (mClients) {
            for (ClientHandler handler : mClients) {
                handler.close();
            }
            mClients.clear();
        }
        Logger.info("Server closed");
    }

    @Override
    protected void registerChannel(Channel channel) {
        // server has nothing to do here
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
