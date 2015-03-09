package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by Max on 24.02.2015.
 */
public class ClientNode extends Node {

    private final boolean mIsDaemon;
    private final ConnectThread mConnector;
    private final Channel mControlChannel;

    public ClientNode(String serverAddr, int serverPort, boolean isDaemon) {
        mIsDaemon = isDaemon;

        mConnector = new ConnectThread(serverAddr, serverPort);
        mControlChannel = super.openChannel(ControlMessages.CONTROL_CHANNEL_ID);
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
        }
    }

    @Override
    public Channel openChannel(String channelId) {
        mControlChannel.publish(ControlMessages.registerChannel(channelId));
        return super.openChannel(channelId);
    }

    private void onConnect() {
        Logger.debug("Server connected");

        // send registered channels to server
        for (String channelId : mChannels.keySet()) {
            mControlChannel.publish(ControlMessages.registerChannel(channelId));
        }
    }

    private void onDisconnect() {
        Logger.debug("Server disconnected");
    }

    private class ControlChannelListener implements MessageListener {
        @Override
        public void onMessageReceived(Message message) {
            Logger.debug("Control message received: topic=" + message.getTopic());
        }
    }

    private class ConnectThread extends Thread {
        private String mServerAddr;
        private int mServerPort;

        private Connection mServerConnection;

        private boolean mClosed = false;

        public ConnectThread(String serverAddr, int serverPort) {
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
                    mServerConnection.setMessageListener(ClientNode.this);
                    mServerConnection.waitForClose();
                    mServerConnection = null;
                    onDisconnect();
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
