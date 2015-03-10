package de.fabmax.pubsub.util;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * Created by Max on 10.03.2015.
 */
public class DnsConfiguration {

    private static InetAddress sBindAddress = null;
    private static final Object sLock = new Object();

    public static void setDiscoveryBindAddress(InetAddress bindAddress) {
        synchronized (sLock) {
            DnsConfiguration.sBindAddress = bindAddress;
        }
    }

    public static InetAddress getDiscoveryBindAddress() {
        synchronized (sLock) {
            if (sBindAddress == null) {
                sBindAddress = determineBestBindAddress();
            }
        }
        return sBindAddress;
    }

    private static InetAddress determineBestBindAddress() {
        Logger.debug("Service discovery bind address was not explicitly set, guessing one...");

        InetAddress result = null;
        String resultIf = null;

        // choose the first network interface with internet connectivity
        // since InetAddress#isReachable() does not reliably work, we use an ugly workaround...
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface nif = ifs.nextElement();
                if (nif.isUp() && !nif.isLoopback()) {
                    Enumeration<InetAddress> addrs = nif.getInetAddresses();
                    Logger.debug("Testing " + nif.getDisplayName());
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (result == null) {
                            // set some valid InetAddress, still better than null....
                            result = addr;
                            resultIf = nif.getDisplayName();
                        }
                        // try to connect to google.com to determine internet connectivity (again: isReachable is not reliable)
                        try {
                            Socket testSock = new Socket("google.com", 443, addr, 0);
                            // when we get here, google.com seems to be reachable...
                            testSock.close();
                            result = addr;
                            resultIf = nif.getDisplayName();
                            break;
                        } catch (IOException e) {
                            Logger.debug("unable to reach google.com from " + addr);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if (result == null) {
            Logger.warn("Found no suitable local bind address");
        }

        Logger.info("Chose local bind address: " + result + ", interface: \"" + resultIf + "\"");
        return result;
    }
}
