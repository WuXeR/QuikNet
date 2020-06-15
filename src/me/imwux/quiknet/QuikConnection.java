package me.imwux.quiknet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class QuikConnection {
    
    private static int socketIdCounter;
    
    private int socketId;
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private InetSocketAddress udpAddress;
    
    private int maxUdpPacketSize;
    private boolean isClosed;
    
    public QuikConnection(Socket tcpSocket, DatagramSocket udpSocket, int udpPort, int maxUdpPacketSize) {
        this.socketId = ++QuikConnection.socketIdCounter;
        this.tcpSocket = tcpSocket;
        this.udpSocket = udpSocket;
        udpAddress = new InetSocketAddress(tcpSocket.getInetAddress(), udpPort);
        this.maxUdpPacketSize = maxUdpPacketSize;
    }
    
    private byte[] createPacket(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
        buffer.putInt(data.length);
        buffer.put(data);
        return buffer.array();
    }
    
    public void sendTCP(QuikBuffer buffer) {
        byte[] data = createPacket(buffer.toBytes());
        try {
            OutputStream stream = tcpSocket.getOutputStream();
            stream.write(data);
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void sendUDP(QuikBuffer buffer) {
        byte[] data = createPacket(buffer.toBytes());
        if(data.length > maxUdpPacketSize)
            throw new OversizedPacketException(maxUdpPacketSize, data.length);
        DatagramPacket packet = new DatagramPacket(data, data.length, udpAddress);
        try {
            udpSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
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
    }
    
    public int getSocketId() {
        return socketId;
    }
    
    public Socket getTcpSocket() {
        return tcpSocket;
    }
    
    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }
    
    public InetSocketAddress getTcpAddress() {
        return (InetSocketAddress) tcpSocket.getRemoteSocketAddress();
    }
    
    public InetSocketAddress getUdpAddress() {
        return udpAddress;
    }
    
    public int getMaxUdpPacketSize() {
        return maxUdpPacketSize;
    }
    
    public boolean isClosed() {
        return isClosed;
    }
    
}
