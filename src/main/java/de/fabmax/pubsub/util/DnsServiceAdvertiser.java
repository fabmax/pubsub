package de.fabmax.pubsub.util;

import org.pmw.tinylog.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;

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
            dns = JmDNS.create();
            dns.registerService(info);
            Logger.debug("Service advertising started");
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
        Logger.debug("Service advertising stopped");
    }
}
