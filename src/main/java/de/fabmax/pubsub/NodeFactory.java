package de.fabmax.pubsub;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.Writer;

import java.io.IOException;

/**
 * Created by Max on 24.02.2015.
 */
public class NodeFactory {

    public static Node createServerNode(int port) throws IOException {
        return createServerNode(port, false);
    }

    public static Node createServerNode(int port, boolean isDaemon) throws IOException {
        return new ServerNode(port, isDaemon);
    }

    public static Node createClientNode(String serverAddr, int port) {
        return createClientNode(serverAddr, port, false);
    }

    public static Node createClientNode(String serverAddr, int port, boolean isDaemon) {
        return new ClientNode(serverAddr, port, isDaemon);
    }

}
