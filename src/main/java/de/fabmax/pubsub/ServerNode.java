package de.fabmax.pubsub;

import de.fabmax.pubsub.util.DnsConfiguration;
import de.fabmax.pubsub.util.DnsServiceAdvertiser;
import de.fabmax.pubsub.util.MessageMapper;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

/**
 * Created by Max on 24.02.2015.
 */
public class ServerNode extends Node {

    /** Default port used for pubsub protocol */
    public static final int DEFAULT_PORT = 9874;

    private final boolean mIsDaemon;
    private final int mPort;
    private final List<ClientHandler> mClients = new ArrayList<>();
    private final HashMap<Long, ClientHandler> mRegisteredClients = new HashMap<>();

    private ClientAcceptor mClientAcceptor;
    private DnsServiceAdvertiser mServiceAdvertiser = null;

    public ServerNode() {
        this(DEFAULT_PORT);
    }

    public ServerNode(int port) {
        this(port, false);
    }

    public ServerNode(int port, boolean isDaemon) {
        mIsDaemon = isDaemon;
        mPort = port;
    }

    /**
     * Package visible constructor for AutoNode to start a server node with predefined node ID.
     */
    ServerNode(int port, boolean isDaemon, long nodeId) {
        super(nodeId);
        mIsDaemon = isDaemon;
        mPort = port;
    }

    @Override
    public void open() throws IOException {
        mClientAcceptor = new ClientAcceptor(this, mPort);
        mClientAcceptor.setDaemon(mIsDaemon);
        mClientAcceptor.start();
        Logger.info("Server started, nodeId: " + getNodeId());
    }

    public boolean isDaemon() {
        return mIsDaemon;
    }

    protected void clientConnected(ClientHandler clientHandler) {
        boolean first;
        synchronized (mClients) {
            first = mClients.isEmpty();
            mClients.add(clientHandler);
            Logger.info("Client connected: " + clientHandler.getClientAddress());
        }
        if (first) {
            // the first client connected, fire onConnect event
            fireOnConnect();
        }
    }

    protected void clientDisconnected(ClientHandler client) {
        boolean last;
        synchronized (mClients) {
            mClients.remove(client);
            last = mClients.isEmpty();
            long nodeId = client.getClientNodeId();
            if (nodeId != 0) {
                mRegisteredClients.remove(nodeId);
                Message unregisteredMsg = ControlMessages.unregisterNode(nodeId);
                for (ClientHandler handler : mClients) {
                    if (handler != client) {
                        handler.sendControlMessage(unregisteredMsg);
                    }
                }
                fireOnRemoteNoteDisconnected(nodeId);
            }
        }
        Logger.info("Client disconnected: " + client.getClientAddress());
        if (last) {
            // last client disconnected, fire onDisconnect event
            fireOnDisconnect();
        }
    }

    protected void clientRegistered(long clientId, ClientHandler client) {
        Message registeredMsg = ControlMessages.registerNode(clientId);
        synchronized (mClients) {
            mRegisteredClients.put(clientId, client);
            for (ClientHandler handler : mClients) {
                if (handler != client) {
                    handler.sendControlMessage(registeredMsg);
                }
            }
        }
        fireOnRemoteNoteConnected(clientId);
    }

    protected void clientMessageReceived(ClientHandler client, Message message) {
        // redistribute received message to other clients
        publish(message, client, false);
    }

    public void enableServiceAdvertising(String serviceName) {
        enableServiceAdvertising(serviceName, "_pubsub._tcp.local.");
    }

    public void enableServiceAdvertising(String serviceName, String serviceType) {
        if (mServiceAdvertiser != null) {
            mServiceAdvertiser.close();
        }
        mServiceAdvertiser = new DnsServiceAdvertiser(serviceName, serviceType, mPort);
    }

    public void disableServiceAdvertising() {
        if (mServiceAdvertiser != null) {
            mServiceAdvertiser.close();
            mServiceAdvertiser = null;
        }
    }

    @Override
    public Set<Long> getKnownNodeIds() {
        synchronized (mClients) {
            return new HashSet<>(mRegisteredClients.keySet());
        }
    }

    @Override
    public void close() {
        mClientAcceptor.close();
        disableServiceAdvertising();

        synchronized (mClients) {
            for (ClientHandler handler : mClients) {
                handler.close();
            }
            mClients.clear();
            mRegisteredClients.clear();
        }
        Logger.info("Server closed");
    }

    @Override
    public void publish(Message message) {
        publish(message, null, true);
    }

    private void publish(Message message, ClientHandler excluded, boolean fromServer) {
        if (PtpMessage.isPtpMessage(message)) {
            // this is a point-to-point message, send it only to the addressed client
            long toNodeId = PtpMessage.getToNodeId(message);
            if (toNodeId == getNodeId()) {
                // message is for server
                onMessageReceived(message);
            } else {
                // message is for an other client
                synchronized (mClients) {
                    ClientHandler handler = mRegisteredClients.get(toNodeId);
                    if (handler != null) {
                        handler.publish(message);
                    } else {
                        Logger.warn("Received PtpMessage for unknown client: " + toNodeId);
                    }
                }
            }

        } else {
            // this is a regular broadcast message, send it to all clients (relevance is checked by ClientHandler
            synchronized (mClients) {
                for (ClientHandler handler : mClients) {
                    if (handler != excluded) {
                        handler.publish(message);
                    }
                }
            }
            if (!fromServer) {
                onMessageReceived(message);
            }
        }
    }
}
