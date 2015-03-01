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

    private static final Level LOG_LEVEL = Level.TRACE;
    private static final String LOG_PATTERN = "{date:yyyy-MM-dd HH:mm:ss} {level}: {message} [{class}.{method}]";
    private static final Writer WRITER = new ConsoleWriter();

    static {
        Configurator.defaultConfig()
                .level(LOG_LEVEL)
                .formatPattern(LOG_PATTERN)
                .writer(WRITER)
                .activate();
    }

    public static Node createServerNode(int port) throws IOException {
        return new ServerNode(port);
    }

    public static Node createClientNode(String serverAddr, int port) {
        return new ClientNode(serverAddr, port);
    }

}
