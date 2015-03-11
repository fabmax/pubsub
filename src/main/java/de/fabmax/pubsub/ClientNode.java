package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Max on 24.02.2015.
 */
public class ClientNode extends Node implements ConnectionListener {

    private final boolean mIsDaemon;
    private final ConnectThread mConnector;
    private final Channel mControlChannel;

    public ClientNode(String serverAddr, int serverPort, boolean isDaemon) throws UnknownHostException {
        this(InetAddress.getByName(serverAddr), serverPort, isDaemon);
    }

    public ClientNode(InetAddress serverAddr, int serverPort, boolean isDaemon) {
        mIsDaemon = isDaemon;

        mConnector = new ConnectThread(serverAddr, serverPort);
        mControlChannel = openChannel(ControlMessages.CONTROL_CHANNEL_ID);
        mControlChannel.addMessageListener(new ControlChannelListener());
    }

    @Override
    public void close() {
        mConnector.close();
    }

    @Override
    public void publish(Message message) {
        Connection con = mConnector.mServerConnection;
        if (con != null) {
            con.sendMessage(message);
        } else {
            Logger.debug("Discarding message: not connected");
        }
    }

    @Override
    protected void registerChannel(Channel channel) {
        if (mControlChannel != null) {
            mControlChannel.publish(ControlMessages.registerChannel(channel.getChannelId()));
        }
    }

    private void onConnect() {
        Logger.info("Connected to server");

        // register this node's ID
        mControlChannel.publish(ControlMessages.registerNode(getNodeId()));
        // send registered channels to server
        for (String channelId : mChannels.keySet()) {
            mControlChannel.publish(ControlMessages.registerChannel(channelId));
        }
    }

    @Override
    public void onConnectionClosed() {
        Logger.info("Disconnected from server");
    }

    private class ControlChannelListener implements MessageListener {
        @Override
        public void onMessageReceived(Message message) {
            Logger.debug("Control message received: topic=" + message.getTopic());
        }
    }

    private class ConnectThread extends Thread {
        private InetAddress mServerAddr;
        private int mServerPort;

        private Connection mServerConnection;

        private boolean mClosed = false;

        public ConnectThread(InetAddress serverAddr, int serverPort) {
            mServerAddr = serverAddr;
            mServerPort = serverPort;
            setDaemon(mIsDaemon);
            start();
        }

        public void close() {
            mClosed = true;

            Connection con = mServerConnection;
            if (con != null) {
                con.close();
            }
        }

        @Override
        public void run() {
            while (!mClosed) {
                try {
                    Socket sock = new Socket(mServerAddr, mServerPort);
                    mServerConnection = new Connection(sock, Codec.defaultCodecFactory, mIsDaemon);
                    mServerConnection.open();
                    onConnect();
                    mServerConnection.setConnectionListener(ClientNode.this);
                    mServerConnection.waitForClose();
                    mServerConnection = null;
                } catch (IOException | InterruptedException e) {
                    // Server is not available, this is silently ignored
                    // wait a little and try to reconnect
                }
                if (!mClosed) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        // whatever, keep on truckin'...
                    }
                }
            }
        }
    }
}
