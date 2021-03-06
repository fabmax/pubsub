package de.fabmax.pubsub;

import de.fabmax.pubsub.codec.Codec;
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

    private ConnectionListener mListener;
    private boolean mClosed = false;

    public Connection(Socket socket, Codec.CodecFactory<?> codecFactory, boolean isDaemon) throws IOException {
        mSocket = socket;

        mInStream = mSocket.getInputStream();
        mOutStream = mSocket.getOutputStream();

        mReceiver = new ConnectionReceiver(codecFactory);
        mSender = new ConnectionSender(codecFactory);
        mReceiver.setDaemon(isDaemon);
        mSender.setDaemon(isDaemon);
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        mListener = connectionListener;
        mReceiver.setMessageListener(connectionListener);
    }

    public void sendMessage(Message message) {
        if (!mClosed) {
            mSender.sendData(message);
        } else {
            Logger.debug("Discarding message: connection is closed");
        }
    }

    public void open() {
        mReceiver.start();
        mSender.start();
    }

    public synchronized void close() {
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
            if (mListener != null) {
                mListener.onConnectionClosed();
            }
            Logger.debug("Connection closed");
        }
    }

    public void waitForClose() throws InterruptedException {
        mSender.join();
    }

    public boolean isClosed() {
        return mClosed;
    }

    private class ConnectionReceiver extends Thread {
        private final Codec mCodec;
        private MessageListener mMessageListener;

        public ConnectionReceiver(Codec.CodecFactory<?> codecFactory) {
            mCodec = codecFactory.createCodec();
        }

        public void setMessageListener(MessageListener messageListener) {
            mMessageListener = messageListener;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[4096];
                InputStream inStream = mInStream;
                while (!isClosed()) {
                    int len = inStream.read(buffer);
                    if (len > 0) {
                        mCodec.decodeData(buffer, 0, len);
                        while (mCodec.hasMessage() && mMessageListener != null) {
                            Message msg = mCodec.getNextMessage();
                            mMessageListener.onMessageReceived(msg);
                        }
                    } else if (len < 0) {
                        // connection closed
                        close();
                    }
                }
            } catch (IOException e) {
                // if closed socket was closed on purpose
                if (!isClosed()) {
                    Logger.debug("Connection closed by server", e);
                }
                close();
            }
        }
    }

    private class ConnectionSender extends Thread {
        private final ArrayBlockingQueue<Message> mSendQueue = new ArrayBlockingQueue<>(1000);
        private final Codec mCodec;

        public ConnectionSender(Codec.CodecFactory codecFactory) {
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
                while (!isClosed()) {
                    byte[] data = mCodec.encodeMessage(mSendQueue.take());
                    OutputStream out = mOutStream;
                    if (out != null) {
                        out.write(data);
                    }
                }
            } catch (IOException | InterruptedException e) {
                // if closed socket was closed on purpose
                if (!isClosed()) {
                    Logger.error("Error in client sender", e);
                }
                close();
            }
        }
    }
}
