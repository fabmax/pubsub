package de.fabmax.pubsub;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.Writer;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by Max on 24.02.2015.
 */
public class NodeFactory {

    public static ServerNode createServerNode(int port) throws IOException {
        return createServerNode(port, false);
    }

    public static ServerNode createServerNode(int port, boolean isDaemon) throws IOException {
        return new ServerNode(port, isDaemon);
    }

    public static ClientNode createClientNode(String serverAddr, int port) {
        return createClientNode(serverAddr, port, false);
    }

    public static ClientNode createClientNode(String serverAddr, int port, boolean isDaemon) {
        try {
            return new ClientNode(serverAddr, port, isDaemon);
        } catch (UnknownHostException e) {
            Logger.error(e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

}
