# **QuikNet**
QuikNet is a very simple but effective java networking framework.
The idea behind QuikNet is to make it very easy to create a dual protocol server-client setup.

Sending/Receiving packets is done in a packet-like way where you use the QuikBuffer class that
is then sent and received.
# **Simple Client-Server Example**
```java
public static void ExampleServerSetup() throws IOException {
    QuikServer server = new QuikServer();
    server.registerListener(new QuikListener() {
        ...

        @Override
        public void received(QuikConnection connection, QuikBuffer buffer) {
            System.out.println(buffer.readString());
        }
    }
    server.bind(new InetSocketAddress(17005));
}

public static void ExampleClientSetup() throws IOException {
    QuikClient client = new QuikClient();
    client.connect(new InetSocketAddress("127.0.0.1", 17005));
    
    client.sendTCP(new QuikBuffer().writeString("Hello Server!"));
}
```

# **Important**
QuikNet is still in early development and I haven't properly tested it. It may contain multiple bugs/issues that I'm not aware of.
If you do find any, please feel free and open up a pull request and I will be more than happy to merge it with the main branch.
Thanks!
