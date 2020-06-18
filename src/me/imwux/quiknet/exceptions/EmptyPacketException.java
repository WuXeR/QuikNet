package me.imwux.quiknet.exceptions;

public class EmptyPacketException extends RuntimeException {
    
    public EmptyPacketException() {
        super("Tried To Send An Empty Packet");
    }
    
}
