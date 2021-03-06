package de.fabmax.pubsub;

import de.fabmax.pubsub.util.DnsConfiguration;
import de.fabmax.pubsub.util.DnsServiceDiscovery;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Max on 09.03.2015.
 */
public class AutoNode extends Node implements DnsServiceDiscovery.DiscoveryListener, NodeListener {

    private final AddressChecker mAddressChecker;
    private final DnsServiceDiscovery mDiscovery;
    private final String mServiceType;
    private final String mServiceName;
    private final int mServerPort;

    private ServerNode mServer = null;
    private ClientNode mClient = null;
    private DnsServiceDiscovery.DiscoveredService mRemoteHost;

    private final Object mLock = new Object();

    public AutoNode() {
        this(ServerNode.DEFAULT_PORT);
    }

    public AutoNode(int serverPort) {
        this(serverPort, "PubSubServer");
    }

    public AutoNode(int serverPort, String serviceName) {
        this(serverPort, serviceName, "_pubsub._tcp.local.");
    }

    public AutoNode(int serverPort, String serviceName, String serviceType) {
        mServerPort = serverPort;
        mServiceType = serviceType;
        mServiceName = serviceName;
        mAddressChecker = new AddressChecker();
        mDiscovery = new DnsServiceDiscovery(serviceType);
        mDiscovery.addDiscoveryListener(this);
    }

    @Override
    public void open() {
        // don't start server yet, wait for discovery result, which is initially fired even if no services are found
        //startServer();
        mDiscovery.start();
    }

    @Override
    public Set<Long> getKnownNodeIds() {
        if (mServer != null) {
            return mServer.getKnownNodeIds();
        } else if (mClient != null) {
            return mClient.getKnownNodeIds();
        }
        return new HashSet<>();
    }

    private void startServer() {
        stopClient();
        synchronized (mLock) {
            if (mServer == null) {
                try {
                    Logger.info("Switching to server role");
                    mServer = new ServerNode(mServerPort, true, getNodeId());
                    mServer.addNodeListener(this);
                    mServer.enableServiceAdvertising(mServiceName, mServiceType);
                    mServer.open();

                    // register all active channels
                    for (Channel ch : mChannels.values()) {
                        mServer.registerChannel(ch);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void stopServer() {
        synchronized (mLock) {
            if (mServer != null) {
                mServer.removeNodeListener(this);
                mServer.close();
                mServer = null;
            }
        }
    }

    private void startClient(DnsServiceDiscovery.DiscoveredService service) {
        stopServer();
        if (mClient != null && !service.equals(mRemoteHost)) {
            stopClient();
        }
        synchronized (mLock) {
            if (mClient == null) {
                mRemoteHost = service;
                Logger.info("Switching to client role");
                mClient = new ClientNode(mRemoteHost.address, mRemoteHost.port, true, getNodeId());
                mClient.addNodeListener(this);
                mClient.open();

                // register all active channels
                for (Channel ch : mChannels.values()) {
                    mClient.registerChannel(ch);
                }
            }
        }
    }

    private void stopClient() {
        synchronized (mLock) {
            if (mClient != null) {
                mRemoteHost = null;
                mClient.removeNodeListener(this);
                mClient.close();
                mClient = null;
            }
        }
    }

    public boolean isServer() {
        return mServer != null;
    }

    @Override
    public void close() {
        stopClient();
        stopServer();
        mDiscovery.close();
    }

    @Override
    public void publish(Message message) {
        synchronized (mLock) {
            if (mClient != null) {
                mClient.publish(message);
            }
            if (mServer != null) {
                mServer.publish(message);
            }
        }
    }

    @Override
    protected void registerChannel(Channel channel) {
        super.registerChannel(channel);
        synchronized (mLock) {
            if (mClient != null) {
                mClient.registerChannel(channel);
            }
            if (mServer != null) {
                mServer.registerChannel(channel);
            }
        }
    }

    @Override
    public void onDiscoveredServicesUpdated(List<DnsServiceDiscovery.DiscoveredService> discoveredServices) {
        InetAddress localAddr = DnsConfiguration.getDiscoveryBindAddress();
        DnsServiceDiscovery.DiscoveredService highestPrioSrv = null;
        InetAddress highestPrioAddr = isServer() ? localAddr : null;
        for (DnsServiceDiscovery.DiscoveredService srv : discoveredServices) {
            if (highestPrioAddr == null || mAddressChecker.isHigherPriority(highestPrioAddr, srv.address)) {
                highestPrioAddr = srv.address;
                highestPrioSrv = srv;
            }
        }

        if (isServer() && !highestPrioAddr.equals(localAddr)) {
            Logger.debug("Other server with higher priority found, switch from server to client role");
            startClient(highestPrioSrv);
        } else if (!isServer() && highestPrioSrv == null) {
            Logger.debug("No server available, start server");
            startServer();
        } else if (!isServer() && highestPrioSrv != null && !highestPrioSrv.equals(mRemoteHost)) {
            Logger.debug("Switch to higher priority server");
            startClient(highestPrioSrv);
        }
    }

    @Override
    public void onConnect() {
        fireOnConnect();
    }

    @Override
    public void onDisconnect() {
        fireOnDisconnect();
    }

    @Override
    public void onRemoteNodeConnected(long nodeId) {
        fireOnRemoteNoteConnected(nodeId);
    }

    @Override
    public void onRemoteNodeDisconnected(long nodeId) {
        fireOnRemoteNoteDisconnected(nodeId);
    }

    private static class AddressChecker implements Comparator<InetAddress> {

        boolean isHigherPriority(InetAddress addr1, InetAddress addr2) {
            return compare(addr1, addr2) < 0;
        }

        @Override
        public int compare(InetAddress addr1, InetAddress addr2) {
            return addr1.getHostAddress().compareTo(addr2.getHostAddress());
        }
    }
}
