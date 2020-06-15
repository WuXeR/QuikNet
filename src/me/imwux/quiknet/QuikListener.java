package me.imwux.quiknet;

public interface QuikListener {
    
    void received(QuikConnection connection, QuikBuffer buffer);
    
}
