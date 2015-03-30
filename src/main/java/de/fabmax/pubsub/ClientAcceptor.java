package de.fabmax.pubsub;

import org.pmw.tinylog.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Max on 24.02.2015.
 */
class ClientAcceptor extends Thread implements Closeable {

    private final ServerNode mServer;
    private ServerSocket mServerSock;

    private boolean mClosed;

    public ClientAcceptor(ServerNode server, int serverPort) throws IOException {
        mServer = server;
        mServerSock = new ServerSocket(serverPort);
        mClosed = false;
    }

    @Override
    public void close() {
        mClosed = true;
        if (mServerSock != null) {
            try {
                mServerSock.close();
            } catch(IOException e) {
                Logger.error("Failed closing server socket", e);
            }
            mServerSock = null;
        }
    }

    @Override
    public void run() {
        Logger.debug("Waiting for clients to connect at " +
                mServerSock.getInetAddress() + ":" + mServerSock.getLocalPort());
        while (!mClosed) {
            try {
                Socket clientSock = mServerSock.accept();
                ClientHandler handler = new ClientHandler(mServer, clientSock);
                mServer.clientConnected(handler);

            } catch (IOException e) {
                // if closed server sock was closed on purpose
                if (!mClosed) {
                    Logger.error("Error on waiting for client connections", e);
                }
                close();
            }
        }
    }
}
