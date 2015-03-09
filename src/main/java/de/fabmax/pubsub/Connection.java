package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Max on 24.02.2015.
 */
class Connection {

    private Socket mSocket;

    private InputStream mInStream;
    private OutputStream mOutStream;

    private final ConnectionReceiver mReceiver;
    private final ConnectionSender mSender;

    private boolean mClosed = false;

    public Connection(Socket socket, Codec.CodecFactory<?> codecFactory, boolean isDaemon) throws IOException {
        mSocket = socket;

        mInStream = mSocket.getInputStream();
        mOutStream = mSocket.getOutputStream();

        mReceiver = new ConnectionReceiver(this, codecFactory);
        mSender = new ConnectionSender(this, codecFactory);
        mReceiver.setDaemon(isDaemon);
        mSender.setDaemon(isDaemon);
    }

    public void setMessageListener(MessageListener messageListener) {
        mReceiver.setMessageListener(messageListener);
    }

    public void waitForClose() throws InterruptedException {
        mSender.join();
    }

    public boolean isClosed() {
        return mClosed;
    }

    protected InputStream getInputStream() {
        return mInStream;
    }

    protected OutputStream getOutputStream() {
        return mOutStream;
    }

    public void sendMessage(Message message) {
        if (mClosed) {
            throw new IllegalStateException("ClientHandler is closed");
        }
        mSender.sendData(message);
    }

    public void open() {
        mReceiver.start();
        mSender.start();
    }

    public void close() {
        boolean wasClosed = mClosed;
        mClosed = true;

        // sender thread might be blocked in BlockingQueue.take()
        mSender.interrupt();

        if (mOutStream != null) {
            try {
                mOutStream.close();
            } catch(IOException e) {
                Logger.error("Failed closing output stream", e);
            }
            mOutStream = null;
        }

        if (mInStream != null) {
            try {
                mInStream.close();
            } catch(IOException e) {
                Logger.error("Failed closing input stream", e);
            }
            mInStream = null;
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch(IOException e) {
                Logger.error("Failed closing socket", e);
            }
            mSocket = null;
        }

        if (!wasClosed) {
            Logger.debug("Connection closed");
        }
    }

    private static class ConnectionReceiver extends Thread {
        private final Codec mCodec;
        private final Connection mConnection;
        private MessageListener mMessageListener;

        public ConnectionReceiver(Connection connection, Codec.CodecFactory<?> codecFactory) {
            mConnection = connection;
            mCodec = codecFactory.createCodec();
        }

        public void setMessageListener(MessageListener messageListener) {
            mMessageListener = messageListener;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[4096];
                InputStream inStream = mConnection.getInputStream();
                while (!mConnection.isClosed()) {
                    int len = inStream.read(buffer);
                    if (len > 0) {
                        mCodec.decodeData(buffer, 0, len);
                        while (mCodec.hasMessage() && mMessageListener != null) {
                            Message msg = mCodec.getNextMessage();
                            mMessageListener.onMessageReceived(msg);
                        }
                    } else if (len < 0) {
                        // connection closed
                        mConnection.close();
                    }
                }
            } catch (IOException e) {
                // if closed socket was closed on purpose
                if (!mConnection.isClosed()) {
                    Logger.error("Error in client receiver", e);
                }
                mConnection.close();
            }
        }
    }

    private static class ConnectionSender extends Thread {
        private final ArrayBlockingQueue<Message> mSendQueue = new ArrayBlockingQueue<>(10);
        private final Connection mConnection;
        private final Codec mCodec;

        public ConnectionSender(Connection connection, Codec.CodecFactory codecFactory) {
            mConnection = connection;
            mCodec = codecFactory.createCodec();
        }

        public void sendData(Message message) {
            if (!mSendQueue.offer(message)) {
                Logger.error("Unable to send data: Client send queue is full");
            }
        }

        @Override
        public void run() {
            try {
                while (!mConnection.isClosed()) {
                    byte[] data = mCodec.encodeMessage(mSendQueue.take());
                    mConnection.getOutputStream().write(data);
                }
            } catch (IOException | InterruptedException e) {
                // if closed socket was closed on purpose
                if (!mConnection.isClosed()) {
                    Logger.error("Error in client sender", e);
                }
                mConnection.close();
            }
        }
    }
}
