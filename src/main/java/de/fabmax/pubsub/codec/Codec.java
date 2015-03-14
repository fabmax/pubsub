package de.fabmax.pubsub.codec;

import de.fabmax.pubsub.Message;

/**
 * Created by Max on 25.02.2015.
 */
public abstract class Codec {

    public static Codec.CodecFactory<?> defaultCodecFactory = new Codec.CodecFactory<>(ProtobufCodec.class);

    public abstract byte[] encodeMessage(Message message);

    public abstract void decodeData(byte[] buf, int off, int len);

    public abstract boolean hasMessage();

    public abstract Message getNextMessage();

    public static class CodecFactory<T extends Codec> {

        private final Class<T> mCodecClass;

        public CodecFactory(Class<T> clazz) {
            mCodecClass = clazz;
        }

        public T createCodec() {
            try {
                return mCodecClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed instantiating codec", e);
            }
        }
    }
}
