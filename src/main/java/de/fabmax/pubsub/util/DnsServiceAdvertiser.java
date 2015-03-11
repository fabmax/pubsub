package de.fabmax.pubsub.util;

import org.pmw.tinylog.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Max on 07.03.2015.
 */
public class DnsServiceAdvertiser implements Runnable {

    private boolean mAdvertisingEnabled = true;
    private final String mAdvertiseType;
    private final int mAdvertisePort;

    private final Object mLock = new Object();

    public DnsServiceAdvertiser(String advertiseType, int advertisePort) {
        mAdvertiseType = advertiseType;
        mAdvertisePort = advertisePort;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void close() {
        mAdvertisingEnabled = false;
        synchronized (mLock) {
            mLock.notify();
        }
    }

    @Override
    public void run() {
        ServiceInfo info = ServiceInfo.create(mAdvertiseType, "PubSubServer", mAdvertisePort, "");
        JmDNS dns = null;

        try {
            dns = JmDNS.create(DnsConfiguration.getDiscoveryBindAddress());
            dns.registerService(info);
            Logger.info("Service advertising started, type=" + mAdvertiseType + ", port=" + mAdvertisePort +
                    ", bind address: " + DnsConfiguration.getDiscoveryBindAddress());
        } catch (IOException e) {
            Logger.error("Failed advertising service: " + e.getClass().getName() + ": " + e.getMessage());
        }

        // wait until we get closed
        while (dns != null && mAdvertisingEnabled) {
            try {
                synchronized (mLock) {
                    mLock.wait();
                }
            } catch (InterruptedException e) {
                mAdvertisingEnabled = false;
                e.printStackTrace();
            }
        }

        if (dns != null) {
            try {
                dns.unregisterService(info);
                dns.close();
            } catch (IOException e) {
                Logger.error("Exception on closing advertiser: " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
        Logger.info("Service advertising stopped");
    }
}
