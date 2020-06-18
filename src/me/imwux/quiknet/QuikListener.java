package me.imwux.quiknet;

public interface QuikListener {
    
    void connected(QuikConnection connection);
    
    void received(QuikConnection connection, QuikBuffer buffer);
    
    void disconnected(QuikConnection connection);
    
}
