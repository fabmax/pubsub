package de.fabmax.pubsub;

/**
 * Created by Max on 25.02.2015.
 */
class ControlMessages {

    static final String CONTROL_CHANNEL_ID = "de.fabmax.pubsub.ctrl";

    static final String TOPIC_REGISTER_NODE = "registerNode";
    static final String TOPIC_REGISTER_CHANNEL = "registerChannel";

    static Message registerNode(long nodeId) {
        Bundle data = new Bundle();
        data.putLong("nodeId", nodeId);
        return new Message(TOPIC_REGISTER_NODE, data);
    }

    static Message registerChannel(String channelId) {
        Bundle data = new Bundle();
        data.putString("channelId", channelId);
        return new Message(TOPIC_REGISTER_CHANNEL, data);
    }
}
