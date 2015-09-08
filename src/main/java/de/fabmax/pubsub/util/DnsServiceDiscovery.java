package de.fabmax.pubsub.util;

import org.pmw.tinylog.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by Max on 07.03.2015.
 */
public class DnsServiceDiscovery implements Closeable, Runnable, ServiceListener {

    private final Thread mThread;
    private boolean mDiscoveryEnabled = true;
    private boolean mEnumerate = false;
    private final String mServiceType;
    private final Object mLock = new Object();

    private final List<DiscoveredService> mServices = new ArrayList<>();

    private final List<DiscoveryListener> mListeners = new ArrayList<>();

    public DnsServiceDiscovery(String serviceType) {
        mServiceType = serviceType;
        mThread = new Thread(this);
    }

    public void addDiscoveryListener(DiscoveryListener listener) {
        mListeners.add(listener);
    }

    public void removeDiscoveryListener(DiscoveryListener listener) {
        mListeners.remove(listener);
    }

    private void fireDiscoveryUpdate() {
        for (DiscoveryListener l : mListeners) {
            l.onDiscoveredServicesUpdated(getServices());
        }
    }

    public void start() {
        mThread.start();
    }

    @Override
    public void close() {
        mDiscoveryEnabled = false;
        synchronized (mLock) {
            mLock.notify();
        }
    }

    private void triggerFullEnumeration() {
        synchronized (mLock) {
            mEnumerate = true;
            mLock.notify();
        }
    }

    public List<DiscoveredService> getServices() {
        synchronized (mServices) {
            return new ArrayList<>(mServices);
        }
    }

    private void enumerateServices(JmDNS dns) {
        ServiceInfo[] services = dns.list(mServiceType);

        synchronized (mServices) {
            mServices.clear();
            for (ServiceInfo info : services) {
                DiscoveredService service = DiscoveredService.create(info);
                if (service != null) {
                    mServices.add(service);
                }
            }
            fireDiscoveryUpdate();
        }
    }

    @Override
    public void run() {
        JmDNS dns = null;

        try {
            dns = JmDNS.create(DnsConfiguration.getDiscoveryBindAddress());
            dns.addServiceListener(mServiceType, this);
            Logger.info("Service discovery started, bind address: " + DnsConfiguration.getDiscoveryBindAddress() +
                        ", service type: " + mServiceType);

            // do initial discovery, to force fireDiscoveryUpdate(), even if no services are found
            enumerateServices(dns);
        } catch (IOException e) {
            Logger.error("Failed starting service discovery: " + e.getClass().getName() + ": " + e.getMessage());
        }

        // wait until we get closed
        while (dns != null && mDiscoveryEnabled) {
            try {
                synchronized (mLock) {
                    if (mEnumerate) {
                        mEnumerate = false;
                        enumerateServices(dns);
                    }
                    mLock.wait();
                }
            } catch (InterruptedException e) {
                mDiscoveryEnabled = false;
                e.printStackTrace();
            }
        }

        if (dns != null) {
            try {
                dns.close();
            } catch (IOException e) {
                Logger.error("Exception on closing discovery: " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
        Logger.info("Service discovery stopped");
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        // do nothing, the interesting stuff comes when service is resolved
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        Logger.debug("Service removed: " + event.getName() + "[" + event.getType() + "]");
        // there was a service removed, but we don't really know which one, enumerate all remaining services
        triggerFullEnumeration();
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        String addrs = Arrays.toString(event.getInfo().getInetAddresses()) + ", port:" + event.getInfo().getPort();
        Logger.debug("Service resolved: " + event.getName() + "[" + event.getType() + "]: " + addrs);
        DiscoveredService service = DiscoveredService.create(event.getInfo());
        if (service != null) {
            synchronized (mServices) {
                mServices.add(service);
            }
            fireDiscoveryUpdate();
        }
    }

    public interface DiscoveryListener {
        void onDiscoveredServicesUpdated(List<DiscoveredService> discoveredServices);
    }

    public static class DiscoveredService {
        public final String name;
        public final String type;
        public final InetAddress address;
        public final int port;

        private DiscoveredService(String name, String type, InetAddress address, int port) {
            this.name = name;
            this.type = type;
            this.address = address;
            this.port = port;
        }

        private static DiscoveredService create(ServiceInfo info) {
            InetAddress[] addrs = info.getInetAddresses();
            if (addrs != null && addrs.length > 0) {
                return new DiscoveredService(info.getName(), info.getType(), addrs[0], info.getPort());
            } else {
                return null;
            }

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DiscoveredService that = (DiscoveredService) o;

            if (port != that.port) return false;
            if (!address.equals(that.address)) return false;
            if (!type.equals(that.type)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + address.hashCode();
            result = 31 * result + port;
            return result;
        }
    }
}
