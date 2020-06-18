package me.imwux.quiknet;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class QuikPeer {
    
    private Thread tcpThread;
    private QuikServer server;
    private QuikConnection connection;
    
    private boolean isClosed;
    
    public QuikPeer(Socket tcpSocket, QuikServer server) {
        this.server = server;
    
        // Start tcpThread
        tcpThread = new Thread(() -> {
            while(!tcpThread.isInterrupted()) {
                try {
                    InputStream stream = tcpSocket.getInputStream();
                    if (stream.available() >= 4) {
                        ByteBuffer buffer = ByteBuffer.wrap(stream.readNBytes(4));
                        int dataLength = buffer.getInt();
                        
                        while (!tcpThread.isInterrupted()) {
                            if(connection.isClosed())
                                close(); // Fixes Getting Stuck Waiting For Data
                            
                            if (stream.available() >= dataLength) {
                                byte[] data = stream.readNBytes(dataLength);
                                for (QuikListener listener : server.getListeners()) {
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
        
        // Startup Thread
        new Thread(() -> {
            // Sending Max UDP Packet Size To Client
            try {
                tcpSocket.getOutputStream().write(ByteBuffer.allocate(4).putInt(server.getMaxUdpPacketSize()).array());
                tcpSocket.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // We Need To Wait For The Client To Send Their UDP Port
            while(true) {
                try {
                    InputStream stream = tcpSocket.getInputStream();
                    if(stream.available() >= 4) {
                        ByteBuffer buffer = ByteBuffer.wrap(stream.readNBytes(4));
                        connection = new QuikConnection(tcpSocket, server.getUdpSocket(), buffer.getInt(), server.getMaxUdpPacketSize());
                        for (QuikListener listener : server.getListeners()) {
                            new Thread(() -> listener.connected(connection)).start();
                        }
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            tcpThread.start();
        }).start();
    }
    
    public void close() {
        isClosed = true;
        connection.close();
        tcpThread.interrupt();
        server.removePeer(this);
        for (QuikListener listener : server.getListeners()) {
            new Thread(() -> listener.disconnected(connection)).start();
        }
    }
    
    public QuikConnection getConnection() {
        return connection;
    }
    
    public boolean isClosed() {
        return isClosed;
    }
    
}