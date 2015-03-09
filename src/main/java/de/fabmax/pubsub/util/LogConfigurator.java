package de.fabmax.pubsub.util;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.writers.ConsoleWriter;
import org.pmw.tinylog.writers.Writer;

/**
 * Created by fth on 03.03.2015.
 */
public class LogConfigurator {
    public static Level LOG_LEVEL = Level.TRACE;
    public static String LOG_PATTERN = "{date:yyyy-MM-dd HH:mm:ss} {level}: {message} [{class}#{method}]";
    public static Writer WRITER = new ConsoleWriter();

    public static void configureLogging() {
        Configurator.defaultConfig()
                .level(LOG_LEVEL)
                .formatPattern(LOG_PATTERN)
                .writer(WRITER)
                .activate();
    }
}
