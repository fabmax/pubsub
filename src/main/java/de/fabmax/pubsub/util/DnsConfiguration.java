package de.fabmax.pubsub.util;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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
            List<NetworkInterface> candidates = new ArrayList<>();
            while (ifs.hasMoreElements()) {
                NetworkInterface nif = ifs.nextElement();
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                if (nif.isUp() && !nif.isLoopback() && addrs.hasMoreElements()) {
                    candidates.add(nif);
                }
            }
            if (candidates.size() == 1) {
                // exactly one interface available, makes the choice easy...
                NetworkInterface nif = candidates.get(0);
                result = nif.getInetAddresses().nextElement();
                resultIf = nif.getDisplayName();

            } else if (candidates.size() > 1) {
                // more than one interface available, choose the first one with internet access
                for (NetworkInterface nif : candidates) {
                    Logger.debug("Testing " + nif.getDisplayName() + "...");
                    Enumeration<InetAddress> addrs = nif.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        // try to connect to google.com to determine internet connectivity (again: isReachable is not reliable)
                        try {
                            result = addr;
                            resultIf = nif.getDisplayName();
                            Socket testSock = new Socket("google.com", 443, addr, 0);
                            // when we get here, google.com seems to be reachable...
                            testSock.close();
                            break;
                        } catch (IOException e) {
                            // unable to reach google.com from this address, try the next one
                        }
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }

        if (result == null) {
            Logger.error("Found no suitable local bind address");
        }

        Logger.info("Chose local bind address: " + result + ", interface: \"" + resultIf + "\"");
        return result;
    }
}
