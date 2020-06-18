package me.imwux.quiknet;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

public class QuikServer {
    
    private Thread tcpThread, udpThread, keepAliveThread;
    
    private ServerSocket tcpSocket;
    private DatagramSocket udpSocket;
    
    private CopyOnWriteArrayList<QuikPeer> peers;
    private CopyOnWriteArrayList<QuikListener> listeners;
    
    private int maxUdpPacketSize;
    private boolean isClosed;
    
    public QuikServer() {
        this(1024);
    }
    
    public QuikServer(int maxUdpPacketSize) {
        peers = new CopyOnWriteArrayList<>();
        listeners = new CopyOnWriteArrayList<>();
        this.maxUdpPacketSize = maxUdpPacketSize;
        
        tcpThread = new Thread(() -> {
            while(!tcpThread.isInterrupted()) {
                try {
                    peers.add(new QuikPeer(tcpSocket.accept(), this));
                } catch (SocketException e) {
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        
        udpThread = new Thread(() -> {
            while(!udpThread.isInterrupted()) {
                try {
                    byte[] buf = new byte[maxUdpPacketSize];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(packet);
    
                    ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
                    int dataLength = buffer.getInt();
    
                    byte[] data = Arrays.copyOfRange(packet.getData(), 4, 4 + dataLength);
    
                    for (QuikPeer peer : peers) {
                        if (peer.getConnection().getUdpAddress().getPort() == packet.getPort())
                            for (QuikListener listener : listeners) {
                                new Thread(() -> listener.received(peer.getConnection(), new QuikBuffer(data))).start();
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
    
    public void bind(int port) throws IOException {
        bind(new InetSocketAddress(InetAddress.getByName("localhost"), port));
    }
    
    public void bind(InetSocketAddress address) throws IOException {
        tcpSocket = new ServerSocket();
        tcpSocket.bind(address);
        udpSocket = new DatagramSocket(address);
        tcpThread.start();
        udpThread.start();
    }
    
    public void startKeepAlive(int delay) {
        keepAliveThread = new Thread(() -> {
           while(!keepAliveThread.isInterrupted()) {
               for(QuikPeer peer : peers) {
                   peer.getConnection().sendTCP(new QuikBuffer().writeBytes((byte) 0));
               }
               try {
                   Thread.sleep(delay);
               } catch (InterruptedException ignored) {}
           }
        });
        keepAliveThread.start();
    }
    
    public QuikPeer getPeer(int socketId) {
        for(QuikPeer peer : peers) {
            if(peer.getConnection().getSocketId() == socketId)
                return peer;
        }
        return null;
    }
    
    public void removePeer(QuikPeer peer) {
        peers.remove(peer);
    }
    
    public void sendTCP(int socketId, QuikBuffer data) {
        QuikPeer peer = getPeer(socketId);
        if(peer == null)
            return;
        
        peer.getConnection().sendTCP(data);
    }
    
    public void sendUDP(int socketId, QuikBuffer data) {
        QuikPeer peer = getPeer(socketId);
        if(peer == null)
            return;
        
        peer.getConnection().sendUDP(data);
    }
    
    public void sendToAllTCP(QuikBuffer data) {
        for(QuikPeer peer : peers) {
            if(peer != null)
                peer.getConnection().sendTCP(data);
        }
    }
    
    public void sendToAllUDP(QuikBuffer data) {
        for(QuikPeer peer : peers) {
            if(peer != null)
                peer.getConnection().sendUDP(data);
        }
    }
    
    public void sendToAllExceptTCP(QuikBuffer data, int[] exceptions) {
        for(QuikPeer peer : peers) {
            if(peer != null) {
                for(int exception : exceptions)
                    if(exception == peer.getConnection().getSocketId())
                        return;
                peer.getConnection().sendTCP(data);
            }
        }
    }
    
    public void sendToAllExceptUDP(QuikBuffer data, int[] exceptions) {
        for(QuikPeer peer : peers) {
            if(peer != null) {
                for(int exception : exceptions)
                    if(exception == peer.getConnection().getSocketId())
                        return;
                peer.getConnection().sendUDP(data);
            }
        }
    }
    
    public void close() {
        isClosed = true;
        try {
            tcpSocket.close();
            udpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tcpThread.interrupt();
        udpThread.interrupt();
        if(keepAliveThread != null)
            keepAliveThread.interrupt();
        for(QuikPeer peer : peers) {
            peer.close();
        }
    }
    
    protected CopyOnWriteArrayList<QuikListener> getListeners() {
        return listeners;
    }
    
    public void registerListener(QuikListener listener) {
        listeners.add(listener);
    }
    
    public void unregisterListener(QuikListener listener) {
        listeners.remove(listener);
    }
    
    public ServerSocket getTcpSocket() {
        return tcpSocket;
    }
    
    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }
    
    public int getMaxUdpPacketSize() {
        return maxUdpPacketSize;
    }
    
    public boolean isClosed() {
        return isClosed;
    }
    
}
