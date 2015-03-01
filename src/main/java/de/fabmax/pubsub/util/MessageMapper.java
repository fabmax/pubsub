package de.fabmax.pubsub.util;

import de.fabmax.pubsub.ChannelListener;
import de.fabmax.pubsub.Message;
import org.pmw.tinylog.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Hashtable;

/**
 * Created by Max on 25.02.2015.
 */
public class MessageMapper implements ChannelListener {

    private final Object mReceiver;
    private final Hashtable<String, MessageMethodMapper> mEndpoints = new Hashtable<>();

    public MessageMapper(Object receiver) {
        Method[] methods = receiver.getClass().getMethods();
        for (Method method : methods) {
            ChannelEndpoint ep = method.getAnnotation(ChannelEndpoint.class);
            if (ep != null) {
                mEndpoints.put(method.getName(), new MessageMethodMapper(ep, method));
            }
        }

        if (mEndpoints.isEmpty()) {
            throw new IllegalArgumentException(
                    "Receiver object must expose endpoint methods annotated with @ChannelEndpoint");
        }

        mReceiver = receiver;
    }

    @Override
    public void onMessageReceived(Message message) {
        MessageMethodMapper mapper = mEndpoints.get(message.getTopic());
        if (mapper != null && !mapper.map(message)) {
            Logger.warn("Found method for topic but mapping failed");
        }
    }

    private class MessageMethodMapper {
        final ChannelEndpoint mEndpoint;
        final Method mMethod;
        final String[] mParamNames;
        final Object[] mCallParams;

        MessageMethodMapper(ChannelEndpoint ep, Method method) {
            mEndpoint = ep;
            mMethod = method;

            Parameter[] params = method.getParameters();
            mParamNames = new String[params.length];
            mCallParams = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                EndpointParameter epp = params[i].getAnnotation(EndpointParameter.class);
                if (epp != null) {
                    mParamNames[i] = epp.name();
                } else {
                    throw new IllegalArgumentException(
                            "Endpoint method parameters must be annotated with @EndpointParameter");
                }
            }
        }

        boolean map(Message message) {
            if (!mEndpoint.channelId().isEmpty() && !mEndpoint.channelId().equals(message.getChannelId())) {
                return false;
            }
            boolean success = true;
            if (mParamNames.length > 0 && message.getData() == null) {
                success = false;
            }

            for (int i = 0; i < mParamNames.length && success; i++) {
                // parameters are not type checked, wrong type will result in a ClassCastException when invoking the
                // method, which is ok...
                mCallParams[i] = message.getData().get(mParamNames[i]);
                if (mCallParams[i] == null) {
                    // parameter was missing in message
                    success = false;
                }
            }

            if (success) {
                try {
                    mMethod.invoke(mReceiver, mCallParams);
                } catch (Exception e) {
                    Logger.error("Failed invoking method", e);
                    e.printStackTrace();
                    success = false;
                }
            }

            return success;
        }
    }
}
