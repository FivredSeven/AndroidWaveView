
package com.suo.waveform.clip;

public class AudioBuffer {

    public AudioBuffer(byte[] data, int length) {
        this.data = data;
        this.length = length;
    }

    private byte[] data;

    private int length;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

}
