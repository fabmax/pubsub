#PubSub

(Yet another) light-weight publish/subscribe network library written in Java.

Communication is organized by one server node and an arbitrary number of client nodes.
Nodes register channels and publish messages in these channels. All nodes registered to
a particular channel receive the messages published in it.

##Features:
* Clients automatically recover from server connection loss
* Two message de-/serialization codecs:
  * Google protocol buffers based for fast and bandwith efficient message serialization
  * JSON for easy debugging
* Messages can have an arbitrary number of data arguments
* Message can be directly mapped to handler methods using annotations, enabling some sort of RPC operation

##Hello world example:
```java
// create a server and a client node
Node server = NodeFactory.createServerNode(9874);
Node client = NodeFactory.createClientNode("localhost", 9874);

// register the same channel on server and client
Channel clientChannel = client.openChannel("test");
clientChannel.addChannelListener(new ChannelListener() {
    @Override
    public void onMessageReceived(Message message) {
        System.out.println("Client received message: [" + message.getChannelId() + "]: " + message.getTopic());
        System.out.println("  " + message.getData().getString("string"));
    }
});
Channel serverChannel = server.openChannel("test");
serverChannel.addChannelListener(new ChannelListener() {
    @Override
    public void onMessageReceived(Message message) {
        System.out.println("Server received message: [" + message.getChannelId() + "]: " + message.getTopic());
        System.out.println("  " + message.getData().getString("string"));
    }
});

// wait a little while connection is negotiated
Thread.sleep(100);

// send a messsage with some data from client to server
Bundle clientData = new Bundle();
clientData.putString("string", "some payload from client");
clientChannel.publish(new Message("Sent from client", clientData));

// send a messsage with some data from server to client
Bundle serverData = new Bundle();
serverData.putString("string", "some payload from server");
serverChannel.publish(new Message("Sent from server", serverData));

// wait a little until messages are processed
Thread.sleep(100);

// close server and client
client.close();
server.close();
```

##Planned features:
* Auto-discovery of server nodes using dns-sd / zeroconf
* Auto-nodes which can act as server or client, as required

##License:
```
Copyright 2015 Max Thiele

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```