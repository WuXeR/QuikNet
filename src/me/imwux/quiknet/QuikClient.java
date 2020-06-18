package me.imwux.quiknet;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

public class QuikClient {
    
    private Thread udpThread, tcpThread;
    
    private QuikConnection connection;
    private CopyOnWriteArrayList<QuikListener> listeners;
    
    private boolean isClosed;
    
    public QuikClient() {
        listeners = new CopyOnWriteArrayList<>();
    
        udpThread = new Thread(() -> {
            while(!udpThread.isInterrupted()) {
                try {
                    byte[] buf = new byte[connection.getMaxUdpPacketSize()];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    connection.getUdpSocket().receive(packet);
                
                    ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                    int dataLength = buffer.getInt();
                
                    byte[] data = Arrays.copyOfRange(packet.getData(), 4, 4+dataLength);
                
                    for(QuikListener listener : listeners) {
                        new Thread(() -> listener.received(connection, new QuikBuffer(data))).start();
                    }
                } catch (SocketException e) {
                    close();
                } catch  (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    
        tcpThread = new Thread(() -> {
            while(!tcpThread.isInterrupted()) {
                try {
                    InputStream stream = connection.getTcpSocket().getInputStream();
                    if (connection.getTcpSocket().getInputStream().available() >= 4) {
                        ByteBuffer buffer = ByteBuffer.wrap(stream.readNBytes(4));
                        int dataLength = buffer.getInt();
    
                        while (!tcpThread.isInterrupted()) {
                            if (stream.available() >= dataLength) {
                                byte[] data = stream.readNBytes(dataLength);
                                for (QuikListener listener : listeners) {
                                    new Thread(() -> listener.received(connection, new QuikBuffer(data))).start();
                                }
                                break;
                            }
                        }
                    }
                } catch (SocketException e) {
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    public void connect(InetSocketAddress address) throws IOException {
        Socket tcpSocket = new Socket();
        tcpSocket.connect(address);
        
        DatagramSocket udpSocket = new DatagramSocket();
        
        tcpSocket.getOutputStream().write(ByteBuffer.allocate(4).putInt(udpSocket.getLocalPort()).array());
        tcpSocket.getOutputStream().flush();
    
        // Waiting For The Server To Tell Us The Max UDP Packet Size
        int maxUdpPacketSize;
        while(true) {
            try {
                InputStream stream = tcpSocket.getInputStream();
                if(stream.available() >= 4) {
                    ByteBuffer buffer = ByteBuffer.wrap(stream.readNBytes(4));
                    maxUdpPacketSize = buffer.getInt();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        connection = new QuikConnection(tcpSocket, udpSocket, address.getPort(), maxUdpPacketSize);
        for (QuikListener listener : listeners) {
            new Thread(() -> listener.connected(connection)).start();
        }
    
        // Ensures The Client Is Ready
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {}
    
        tcpThread.start();
        udpThread.start();
    }
    
    public void close() {
        isClosed = true;
        tcpThread.interrupt();
        udpThread.interrupt();
        connection.close();
        for (QuikListener listener : listeners) {
            new Thread(() -> listener.disconnected(connection)).start();
        }
    }
    
    public void sendTCP(QuikBuffer data) {
        connection.sendTCP(data);
    }
    
    public void sendUDP(QuikBuffer data) {
        connection.sendUDP(data);
    }
    
    public void registerListener(QuikListener listener) {
        listeners.add(listener);
    }
    
    public void unregisterListener(QuikListener listener) {
        listeners.remove(listener);
    }
    
    public QuikConnection getConnection() {
        return connection;
    }
    
    public boolean isClosed() {
        return isClosed;
    }
    
}
