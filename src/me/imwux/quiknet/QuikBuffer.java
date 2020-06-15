package me.imwux.quiknet;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class QuikBuffer {
    
    private final Object bufferLock = new Object();
    private byte[] source;
    private int position;
    
    public QuikBuffer() {
        this(new byte[0]);
    }
    
    public QuikBuffer(byte[] data) {
        source = data;
    }
    
    public QuikBuffer writeBytes(byte bytes) {
        return writeBytes(new byte[] {bytes});
    }
    
    public QuikBuffer writeBytes(byte[] bytes) {
        synchronized(bufferLock) {
            byte[] temp = new byte[source.length + bytes.length];
            System.arraycopy(source, 0, temp, 0, source.length);
            System.arraycopy(bytes, 0, temp, source.length, bytes.length);
            source = temp;
        }
        return this;
    }
    
    public byte[] readBytes(int length) {
        byte[] temp = Arrays.copyOfRange(source, position, position+length);
        position += length;
        return temp;
    }
    
    public QuikBuffer writeBoolean(boolean value) {
        return writeBytes(value ? (byte) 1 : 0);
    }
    
    public QuikBuffer writeInt(int value) {
        return writeBytes(ByteBuffer.allocate(4).putInt(value).array());
    }
    
    public QuikBuffer writeDouble(double value) {
        return writeBytes(ByteBuffer.allocate(8).putDouble(value).array());
    }
    
    public QuikBuffer writeString(String value) {
        writeBytes(ByteBuffer.allocate(4).putInt(value.getBytes().length).array());
        return writeBytes(ByteBuffer.allocate(value.getBytes().length).put(value.getBytes()).array());
    }

    public boolean readBoolean() {
        return readBytes(1)[0] == 1;
    }

    public int readInt() {
        return ByteBuffer.wrap(readBytes(4)).getInt();
    }

    public double readDouble() {
        return ByteBuffer.wrap(readBytes(8)).getDouble();
    }
    
    public String readString() {
        int length = ByteBuffer.wrap(readBytes(4)).getInt();
        ByteBuffer buffer = ByteBuffer.wrap(readBytes(length));
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < length; i++) {
            builder.append((char) buffer.get());
        }
        return builder.toString();
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public void resetPosition() {
        setPosition(0);
    }
    
    public int getPosition() {
        return position;
    }
    
    public byte[] toBytes() {
        return source;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(source);
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof QuikBuffer))
            return false;
        return Arrays.equals(source, ((QuikBuffer) obj).toBytes());
    }
}
