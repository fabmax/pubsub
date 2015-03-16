package de.fabmax.pubsub;

/**
 * Created by Max on 16.03.2015.
 */
public interface NodeListener {

    public void onConnect();
    public void onDisconnect();
    public void onRemoteNodeConnected(long nodeId);
    public void onRemoteNodeDisconnected(long nodeId);

}
