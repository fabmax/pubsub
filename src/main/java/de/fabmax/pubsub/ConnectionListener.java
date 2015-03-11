package de.fabmax.pubsub;

/**
 * Created by Max on 11.03.2015.
 */
public interface ConnectionListener extends MessageListener {

    public void onConnectionClosed();

}
