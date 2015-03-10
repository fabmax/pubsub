package de.fabmax.pubsub;

import de.fabmax.pubsub.util.DnsConfiguration;
import de.fabmax.pubsub.util.DnsServiceDiscovery;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * Created by Max on 09.03.2015.
 */
public class AutoNode extends Node implements DnsServiceDiscovery.DiscoveryListener {

    private final AddressChecker mAddressChecker;
    private final DnsServiceDiscovery mDiscovery;

    private ServerNode mServer = null;
    private ClientNode mClient = null;
    private DnsServiceDiscovery.DiscoveredService mRemoteHost;

    public AutoNode() {
        mAddressChecker = new AddressChecker();
        mDiscovery = new DnsServiceDiscovery(ServerNode.DNS_SD_TYPE);
        mDiscovery.addDiscoveryListener(this);

        // don't start server yet, wait for discovery result, which is initially fired even if no services are found
        //startServer();
    }

    private void startServer() {
        stopClient();
        if (mServer == null) {
            try {
                Logger.info("Starting server");
                mServer = new ServerNode(ServerNode.DEFAULT_PORT, true);
                mServer.setServiceAdvertisingEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopServer() {
        if (mServer != null) {
            mServer.close();
            mServer = null;
        }
    }

    private void startClient(DnsServiceDiscovery.DiscoveredService service) {
        stopServer();
        if (mClient != null && !service.equals(mRemoteHost)) {
            stopClient();
        }
        if (mClient == null) {
            mRemoteHost = service;
            Logger.info("Starting client connection to " + mRemoteHost.address + ", port: " + mRemoteHost.port);
            mClient = new ClientNode(mRemoteHost.address, mRemoteHost.port, true);
        }
    }

    private void stopClient() {
        if (mClient!= null) {
            mRemoteHost = null;
            mClient.close();
            mClient = null;
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
            Logger.debug("No server available anymore, start server");
            startServer();
        } else if (!isServer() && highestPrioSrv != null && !highestPrioSrv.equals(mRemoteHost)) {
            Logger.debug("Switch to higher priority server");
            startClient(highestPrioSrv);
        }
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
