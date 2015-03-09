package de.fabmax.pubsub.util;

import org.pmw.tinylog.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Max on 07.03.2015.
 */
public class DnsServiceDiscovery implements Runnable, ServiceListener {

    private boolean mDiscoveryEnabled = true;
    private boolean mEnumerate = false;
    private final String mServiceType;
    private final Object mLock = new Object();

    private final List<DiscoveredService> mServices = new ArrayList<>();

    private final List<DiscoveryListener> mListeners = new ArrayList<>();

    public DnsServiceDiscovery(String serviceType) {
        mServiceType = serviceType;
        Thread thread = new Thread(this);
        thread.start();
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
        Logger.debug("Enumerating services");
        long t = System.currentTimeMillis();
        ServiceInfo[] services = dns.list(mServiceType);
        t = System.currentTimeMillis() - t;
        Logger.debug(String.format(Locale.ENGLISH, "Enumeration finished, got %d results (took %.3f s)",
                services.length, t / 1e3f));

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
            dns = JmDNS.create();
            dns.addServiceListener(mServiceType, this);
            Logger.debug("Service discovery started");
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
        Logger.debug("Service discovery stopped");
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        Logger.debug("service added: " + event);
        // do nothing, the interesting stuff comes when service is resolved
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        Logger.debug("service removed: " + event);
        // there was a service removed, but we don't really know which one, enumerate all remaining services
        triggerFullEnumeration();
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        Logger.debug("service resolved: " + event);
        DiscoveredService service = DiscoveredService.create(event.getInfo());
        if (service != null) {
            synchronized (mServices) {
                mServices.add(service);
            }
            fireDiscoveryUpdate();
        }
    }

    public interface DiscoveryListener {
        public void onDiscoveredServicesUpdated(List<DiscoveredService> discoveredServices);
    }

    public static class DiscoveredService {
        public final String type;
        public final InetAddress address;

        private DiscoveredService(String type, InetAddress address) {
            this.type = type;
            this.address = address;
        }

        private static DiscoveredService create(ServiceInfo info) {
            InetAddress[] addrs = info.getInetAddresses();
            if (addrs != null && addrs.length > 0) {
                return new DiscoveredService(info.getType(), addrs[0]);
            } else {
                return null;
            }

        }
    }
}
