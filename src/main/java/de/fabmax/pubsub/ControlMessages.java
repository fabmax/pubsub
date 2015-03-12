package de.fabmax.pubsub;

import java.util.Set;

/**
 * Created by Max on 25.02.2015.
 */
class ControlMessages {

    static final String CONTROL_CHANNEL_ID = "de.fabmax.pubsub.ctrl";

    static final String TOPIC_REGISTER_NODE = "registerNode";
    static final String TOPIC_REGISTER_NODES = "registerNodes";
    static final String TOPIC_UNREGISTER_NODE = "unregisterNode";
    static final String TOPIC_REGISTER_CHANNEL = "registerChannel";

    static Message registerNode(long nodeId) {
        Bundle data = new Bundle();
        data.putLong("nodeId", nodeId);
        return new Message(TOPIC_REGISTER_NODE, data);
    }

    static Message registerNodes(Set<Long> nodeIds) {
        long[] ids = new long[nodeIds.size()];
        int i = 0;
        for (long id : nodeIds) {
            ids[i++] = id;
        }
        return registerNodes(ids);
    }

    static Message registerNodes(long[] nodeIds) {
        Bundle data = new Bundle();
        data.putLongArray("nodeIds", nodeIds);
        return new Message(TOPIC_REGISTER_NODES, data);
    }

    static Message unregisterNode(long nodeId) {
        Bundle data = new Bundle();
        data.putLong("nodeId", nodeId);
        return new Message(TOPIC_UNREGISTER_NODE, data);
    }

    static Message registerChannel(String channelId) {
        Bundle data = new Bundle();
        data.putString("channelId", channelId);
        return new Message(TOPIC_REGISTER_CHANNEL, data);
    }
}
