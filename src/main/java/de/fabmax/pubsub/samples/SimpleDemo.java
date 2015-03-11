package de.fabmax.pubsub.samples;

import de.fabmax.pubsub.*;
import de.fabmax.pubsub.JsonCodec;
import de.fabmax.pubsub.extra.ProtobufCodec;
import de.fabmax.pubsub.util.LogConfigurator;
import org.pmw.tinylog.Logger;

import java.util.Arrays;

/**
 * Created by Max on 24.02.2015.
 */
public class SimpleDemo {

    public static void main(String[] args) throws Exception {
        LogConfigurator.configureLogging();

        clientServerTest();
        //messageTest();
        //codecBenchmark();
    }

    public static void clientServerTest() throws Exception {
        // create a server and a client node
        Node server = NodeFactory.createServerNode(9874);
        Node client = NodeFactory.createClientNode("localhost", 9874);

        // register the same channel on server and client
        Channel clientChannel = client.openChannel("test");
        Channel serverChannel = server.openChannel("test");

        // add channel listeners to receive messages on client and server side
        clientChannel.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
                Logger.info("Client received message: [" + message.getChannelId() + "]: " + message.getTopic());
                Logger.info("  " + message.getData().getString("string"));
            }
        });
        serverChannel.addMessageListener(new MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
                Logger.info("Server received message: [" + message.getChannelId() + "]: " + message.getTopic());
                Logger.info("  " + message.getData().getString("string"));
            }
        });

        // wait a little while connection is negotiated
        Thread.sleep(500);

        // send a messsage with some data from client to server
        Bundle clientData = new Bundle();
        clientData.putString("string", "Hello World from client");
        clientChannel.publish(new Message("hot stuff", clientData));

        // send a messsage with some data from server to client
        Bundle serverData = new Bundle();
        serverData.putString("string", "Hello World from server");
        serverChannel.publish(new Message("hot stuff", serverData));

        // wait a little until messages are processed
        Thread.sleep(500);

        // close server and client
        client.close();
        server.close();
    }

    public static void messageTest() throws Exception {
        Message test = testMessage();

        Codec codec = new ProtobufCodec();
        byte[] encoded = codec.encodeMessage(test);
        codec.decodeData(encoded, 0, encoded.length);
        codec.hasMessage();
        test = codec.getNextMessage();

        //System.out.println(new String(encoded));

        System.out.println("channel: " + test.getChannelId());
        System.out.println("topic: " + test.getTopic());

        Bundle data = test.getData();
        if (data != null) {
            System.out.println("data:");
            System.out.println("bool: " + data.getBoolean("bool"));
            System.out.println("boolArray: " + Arrays.toString(data.getBooleanArray("boolArray")));
            System.out.println("byte: " + data.getByte("byte"));
            System.out.println("byteArray: " + Arrays.toString(data.getByteArray("byteArray")));
            System.out.println("char: " + data.getChar("char"));
            System.out.println("charArray: " + Arrays.toString(data.getCharArray("charArray")));
            System.out.println("double: " + data.getDouble("double"));
            System.out.println("doubleArray: " + Arrays.toString(data.getDoubleArray("doubleArray")));
            System.out.println("float: " + data.getFloat("float"));
            System.out.println("floatArray: " + Arrays.toString(data.getFloatArray("floatArray")));
            System.out.println("int: " + data.getInt("int"));
            System.out.println("intArray: " + Arrays.toString(data.getIntArray("intArray")));
            System.out.println("long: " + data.getLong("long"));
            System.out.println("longArray: " + Arrays.toString(data.getLongArray("longArray")));
            System.out.println("short: " + data.getShort("short"));
            System.out.println("shortArray: " + Arrays.toString(data.getShortArray("shortArray")));
            System.out.println("string: " + data.getString("string"));
            System.out.println("stringArray: " + Arrays.toString(data.getStringArray("stringArray")));
        }
    }

    public static void codecBenchmark() throws Exception {
        Message test = testMessage();

        JsonCodec jsonCodec = new JsonCodec();
        ProtobufCodec protoCodec = new ProtobufCodec();

        byte[] jsonEnc = jsonCodec.encodeMessage(test);
        byte[] protoEnc = protoCodec.encodeMessage(test);

        System.out.println("Json size: " + jsonEnc.length + ", proto size: " + protoEnc.length);

        System.out.println("Warming up...");
        for (int i = 0; i < 1000; i++) {
            test.getData().putInt("int", i);
            byte[] data = jsonCodec.encodeMessage(test);
            jsonCodec.decodeData(data, 0, data.length);
            data = protoCodec.encodeMessage(test);
            protoCodec.decodeData(data, 0, data.length);
        }

        int n = 50000;

        System.out.println("Benchmarking JSON codec...");
        long tJsonEnc = System.nanoTime();
        for (int i = 0; i < n; i++) {
            test.getData().putInt("int", i);
            jsonCodec.encodeMessage(test);
        }
        tJsonEnc = System.nanoTime() - tJsonEnc;
        long tJsonDec = System.nanoTime();
        for (int i = 0; i < n; i++) {
            jsonCodec.decodeData(jsonEnc, 0, jsonEnc.length);
        }
        tJsonDec = System.nanoTime() - tJsonDec;

        System.out.println("Benchmarking Protobuf codec...");
        long tProtoEnc = System.nanoTime();
        for (int i = 0; i < n; i++) {
            test.getData().putInt("int", i);
            protoCodec.encodeMessage(test);
        }
        tProtoEnc = System.nanoTime() - tProtoEnc;
        long tProtoDec = System.nanoTime();
        for (int i = 0; i < n; i++) {
            protoCodec.decodeData(protoEnc, 0, protoEnc.length);
        }
        tProtoDec = System.nanoTime() - tProtoDec;

        System.out.println("Encoding:");
        System.out.printf("  JSON:     %8.3f ms (%6.3f us / message)\n", tJsonEnc / 1e6, tJsonEnc / 1e3 / n);
        System.out.printf("  Protobuf: %8.3f ms (%6.3f us / message)\n", tProtoEnc / 1e6, tProtoEnc / 1e3 / n);
        System.out.println("Decoding:");
        System.out.printf("  JSON:     %8.3f ms (%6.3f us / message)\n", tJsonDec / 1e6, tJsonDec / 1e3 / n);
        System.out.printf("  Protobuf: %8.3f ms (%6.3f us / message)\n", tProtoDec / 1e6, tProtoDec / 1e3 / n);
    }

    private static Message testMessage() {
        Message test = new Message("test");
        test.setChannelId("testChannel");

        Bundle data = new Bundle();
        data.putBoolean("bool", true);
        data.putBooleanArray("boolArray", new boolean[]{true, false, false, false, true, true});
        data.putByte("byte", (byte) 8);
        data.putByteArray("byteArray", "Hallo Welt".getBytes());
        data.putChar("char", 'a');
        data.putCharArray("charArray", new char[]{'a', 'b', 'c'});
        data.putDouble("double", 3.75);
        data.putDoubleArray("doubleArray", new double[]{1.034, 2.342, 4.324e100});
        data.putFloat("float", 3.25f);
        data.putFloatArray("floatArray", new float[]{3.2f, 4.5f, 32.32134f});
        data.putInt("int", 17);
        data.putIntArray("intArray", new int[]{1, 2, 3, 4});
        data.putLong("long", 42);
        data.putLongArray("longArray", new long[]{1, 2, 3, 123456789012l});
        data.putShort("short", (short) 3);
        data.putShortArray("shortArray", new short[]{4, 5, 6});
        data.putString("string", "Hallo Welt");
        data.putStringArray("stringArray", new String[]{"Hallo", "Welt", "bla"});
        test.setData(data);

        return test;
    }
}
