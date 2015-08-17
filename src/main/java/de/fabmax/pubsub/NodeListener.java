package de.fabmax.pubsub;

/**
 * Created by Max on 16.03.2015.
 */
public interface NodeListener {

    void onConnect();
    void onDisconnect();
    void onRemoteNodeConnected(long nodeId);
    void onRemoteNodeDisconnected(long nodeId);

}
