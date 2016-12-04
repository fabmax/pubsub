#PubSub

(Yet another) light-weight publish/subscribe network library written in Java.

Communication is organized by one server node and an arbitrary number of client nodes.
Nodes register channels and publish messages in these channels. All nodes registered to
a particular channel receive the messages published in it.

##Features:
* Messages can be broadcasted as well as being sent to a specific client
* Auto-discovery of server nodes using dns-sd / zeroconf
* Auto-nodes which can act as server or client, as required
* Clients automatically recover from server connection loss
* Two message de-/serialization codecs:
  * Google protocol buffers based for fast and bandwith efficient message serialization
  * JSON for easy debugging
* Messages can have an arbitrary number of data arguments
* Message can be directly mapped to handler methods using annotations, enabling some sort of RPC operation

##Hello world example:
```java
// create a server and a client node
Node server = new ServerNode();
Node client = new ClientNode("localhost");

server.open();
client.open();

// register the same channel on server and client
Channel clientChannel = client.openChannel("test");
Channel serverChannel = server.openChannel("test");

// add channel listeners to receive messages on client and server side
clientChannel.addMessageListener(new MessageListener() {
    @Override
    public void onMessageReceived(Message message) {
        Logger.info("Client received message: [" + message.getChannelId() + ":" +
                message.getTopic() + "]:  " + message.getData().getString("string"));
    }
});
serverChannel.addMessageListener(new MessageListener() {
    @Override
    public void onMessageReceived(Message message) {
        Logger.info("Server received message: [" + message.getChannelId() + ":" +
                message.getTopic() + "]:  " + message.getData().getString("string"));
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
```

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
