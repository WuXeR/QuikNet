package me.imwux.quiknet.exceptions;

public class OversizedPacketException extends RuntimeException {
    
    public OversizedPacketException(int maxPacketSize, int actualPacketSize) {
        super(String.format("Packet (%s bytes) Exceeds The Max Packet Size Of %s", actualPacketSize, maxPacketSize));
    }
    
}
