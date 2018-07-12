package com.suo.waveform.clip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CheapMP3 extends CheapSoundFile {
    static {
        short[] arrayOfShort = new short[8];
        arrayOfShort[1] = 128;
        arrayOfShort[2] = 192;
        arrayOfShort[3] = 224;
        arrayOfShort[4] = 240;
        arrayOfShort[5] = 248;
        arrayOfShort[6] = 252;
        arrayOfShort[7] = 254;
        maskLeft8bits = arrayOfShort;
        maskRight8bits = new short[]{255, 127, 63, 31, 15, 7, 3, 1};
    }

    public static Factory getFactory() {
        return new Factory() {
            public CheapSoundFile create() {
                return new CheapMP3();
            }

            public String[] getSupportedExtensions() {
                return new String[]{"mp3"};
            }
        };
    }

    // Member variables representing frame data
    private int mNumFrames;
    private int[] mFrameOffsets;
    private int[] mFrameLens;
    private int[] mFrameGains;
    private int mFileSize;
    private int mAvgBitRate;
    private int mGlobalSampleRate;
    private int mGlobalChannels;

    // Member variables used during initialization
    private int mMaxFrames;
    private int mBitrateSum;
    private int mMinGain;
    private int mMaxGain;

    public CheapMP3() {
    }

    public int getNumFrames() {
        return mNumFrames;
    }

    public int[] getFrameOffsets() {
        return mFrameOffsets;
    }

    public int getSamplesPerFrame() {
        return 1152;
    }

    public int[] getFrameLens() {
        return mFrameLens;
    }

    public int[] getFrameGains() {
        return mFrameGains;
    }

    public int getFileSizeBytes() {
        return mFileSize;
    }

    public int getAvgBitrateKbps() {
        return mAvgBitRate;
    }

    public int getSampleRate() {
        return mGlobalSampleRate;
    }

    public int getChannels() {
        return mGlobalChannels;
    }

    public String getFiletype() {
        return "MP3";
    }

    /**
     * MP3 supports seeking into the middle of the file, no header needed,
     * so this method is supported to hear exactly what a "cut" of the file
     * sounds like without needing to actually save a file to disk first.
     */
    public int getSeekableFrameOffset(int frame) {
        if (frame <= 0) {
            return 0;
        } else if (frame >= mNumFrames) {
            return mFileSize;
        } else {
            return mFrameOffsets[frame];
        }
    }

    public void ReadFile(File inputFile) throws java.io.FileNotFoundException, IOException {
        super.ReadFile(inputFile);
        mNumFrames = 0;
        mMaxFrames = 64;  // This will grow as needed
        mFrameOffsets = new int[mMaxFrames];
        mFrameLens = new int[mMaxFrames];
        mFrameGains = new int[mMaxFrames];
        mBitrateSum = 0;
        mMinGain = 255;
        mMaxGain = 0;
        mFileSize = (int) mInputFile.length();
        FileInputStream stream = new FileInputStream(mInputFile);
        int pos = 0;
        int offset = 0;
        byte[] buffer = new byte[12];
        while (pos < mFileSize - 12) {
            // Read 12 bytes at a time and look for a sync code (0xFF)
            while (offset < 12) {
                offset += stream.read(buffer, offset, 12 - offset);
            }
            int bufferOffset = 0;
            while (bufferOffset < 12 && buffer[bufferOffset] != -1)
                bufferOffset++;
            if (mProgressListener != null) {
                boolean keepGoing = mProgressListener.reportProgress(pos * 1.0 / mFileSize);
                if (!keepGoing) {
                    break;
                }
            }
            if (bufferOffset > 0) {
                // We didn't find a sync code (0xFF) at position 0;
                // shift the buffer over and try again
                for (int i = 0; i < 12 - bufferOffset; i++)
                    buffer[i] = buffer[bufferOffset + i];
                pos += bufferOffset;
                offset = 12 - bufferOffset;
                continue;
            }
            // Check for MPEG 1 Layer III or MPEG 2 Layer III codes
            int mpgVersion = 0;
            if (buffer[1] == -6 || buffer[1] == -5) {
                mpgVersion = 1;
            } else if (buffer[1] == -14 || buffer[1] == -13) {
                mpgVersion = 2;
            } else {
                bufferOffset = 1;
                for (int i = 0; i < 12 - bufferOffset; i++)
                    buffer[i] = buffer[bufferOffset + i];
                pos += bufferOffset;
                offset = 12 - bufferOffset;
                continue;
            }

            // The third byte has the bitrate and samplerate
            int bitRate;
            int sampleRate;
            if (mpgVersion == 1) {
                // MPEG 1 Layer III
                int index = (buffer[2] & 0xF0) >> 4;
                if (index < 0 || index >= BITRATES_MPEG1_L3.length){
                    index = 0;
                }
                bitRate = BITRATES_MPEG1_L3[index];
                int sampleIndex = (buffer[2] & 0x0C) >> 2;
                if (sampleIndex < 0 || sampleIndex >= SAMPLERATES_MPEG1_L3.length){
                    sampleIndex = 0;
                }
                sampleRate = SAMPLERATES_MPEG1_L3[sampleIndex];
            } else {
                // MPEG 2 Layer III
                int index = (buffer[2] & 0xF0) >> 4;
                if (index < 0 || index >=BITRATES_MPEG2_L3.length){
                    index = 0;
                }
                bitRate = BITRATES_MPEG2_L3[index];

                int sampleIndex = (buffer[2] & 0x0C) >> 2;
                if (sampleIndex < 0 || sampleIndex >= SAMPLERATES_MPEG2_L3.length){
                    sampleIndex = 0;
                }
                sampleRate = SAMPLERATES_MPEG2_L3[sampleIndex];
            }

            if (bitRate == 0 || sampleRate == 0) {
                bufferOffset = 2;
                for (int i = 0; i < 12 - bufferOffset; i++)
                    buffer[i] = buffer[bufferOffset + i];
                pos += bufferOffset;
                offset = 12 - bufferOffset;
                continue;
            }

            // From here on we assume the frame is good
            mGlobalSampleRate = sampleRate;
            int padding = (buffer[2] & 2) >> 1;
            int frameLen = 144 * bitRate * 1000 / sampleRate + padding;

            int gain;
            if ((buffer[3] & 0xC0) == 0xC0) {
                // 1 channel
                mGlobalChannels = 1;
                if (mpgVersion == 1) {
                    gain = ((buffer[10] & 0x01) << 7) + ((buffer[11] & 0xFE) >> 1);
                } else {
                    gain = ((buffer[9] & 0x03) << 6) + ((buffer[10] & 0xFC) >> 2);
                }
            } else {
                // 2 channels
                mGlobalChannels = 2;
                if (mpgVersion == 1) {
                    gain = ((buffer[9] & 0x7F) << 1) + ((buffer[10] & 0x80) >> 7);
                } else {
                    gain = 0;  // ???
                }
            }

            mBitrateSum += bitRate;
            mFrameOffsets[mNumFrames] = pos;
            mFrameLens[mNumFrames] = frameLen;
            mFrameGains[mNumFrames] = gain;
            if (gain < mMinGain)
                mMinGain = gain;
            if (gain > mMaxGain)
                mMaxGain = gain;

            mNumFrames++;
            if (mNumFrames == mMaxFrames) {
                // We need to grow our arrays.  Rather than naively
                // doubling the array each time, we estimate the exact
                // number of frames we need and add 10% padding.  In
                // practice this seems to work quite well, only one
                // resize is ever needed, however to avoid pathological
                // cases we make sure to always double the size at a minimum.

                mAvgBitRate = mBitrateSum / mNumFrames;
                int totalFramesGuess = ((mFileSize / mAvgBitRate) * sampleRate) / 144000;
                int newMaxFrames = totalFramesGuess * 11 / 10;
                if (newMaxFrames < mMaxFrames * 2)
                    newMaxFrames = mMaxFrames * 2;
                int[] newOffsets = new int[newMaxFrames];
                int[] newLens = new int[newMaxFrames];
                int[] newGains = new int[newMaxFrames];
                for (int i = 0; i < mNumFrames; i++) {
                    newOffsets[i] = mFrameOffsets[i];
                    newLens[i] = mFrameLens[i];
                    newGains[i] = mFrameGains[i];
                }
                mFrameOffsets = newOffsets;
                mFrameLens = newLens;
                mFrameGains = newGains;
                mMaxFrames = newMaxFrames;
            }

            if (stream.skip(frameLen - 12) < 0){
                throw new IOException("Can not skip frameLen");
            }
            pos += frameLen;
            offset = 0;
        }
        // We're done reading the file, do some postprocessing
        if (mNumFrames > 0) {
            mAvgBitRate = mBitrateSum / mNumFrames;
        } else {
            mAvgBitRate = 0;
        }
    }

    //private SimpleDateFormat formatter=new SimpleDateFormat("mm:ss");
    public void WriteFile(File outputFile, int startFrame, int numFrames) throws IOException {
        outputFile.createNewFile();
        FileInputStream in = new FileInputStream(mInputFile);
        FileOutputStream out = new FileOutputStream(outputFile);
        int maxFrameLen = 0;
        for (int i = 0; i < numFrames; i++) {
            if (mFrameLens[startFrame + i] > maxFrameLen)
                maxFrameLen = mFrameLens[startFrame + i];
        }
        byte[] buffer = new byte[maxFrameLen];
        int pos = 0;
        for (int i = 0; i < numFrames; i++) {
            int skip = mFrameOffsets[startFrame + i] - pos;
            int len = mFrameLens[startFrame + i];
            if (skip > 0) {
                if (in.skip(skip) < 0){
                    throw new IOException("Can not skip skip len");
                }
                pos += skip;
            }
            if (in.read(buffer, 0, len) < 0){
                throw new IOException("Can not read buffer");
            }
            if (i < 5) {
                //buffer[i]=(byte) (70);
            }
            out.write(buffer, 0, len);
            pos += len;
        }
        in.close();
        out.close();
    }

    byte[] outBuffer;
    int bitPos = 0;
    int bytePos = 0;

    public int peek8Bits() {
        return 0xFF & ((0xFF & this.outBuffer[this.bytePos]) << 8 | 0xFF & this.outBuffer[(1 + this.bytePos)]) >> 8 - this.bitPos;
    }

    static short[] maskLeft8bits;
    static short[] maskRight8bits;

    public void set8Bits(int paramInt) {
        int i = paramInt << 8 - this.bitPos;
        byte[] arrayOfByte1 = this.outBuffer;
        int j = this.bytePos;
        arrayOfByte1[j] = (byte) (arrayOfByte1[j] & maskLeft8bits[this.bitPos]);
        byte[] arrayOfByte2 = this.outBuffer;
        int k = this.bytePos;
        arrayOfByte2[k] = (byte) (arrayOfByte2[k] | i >> 8);
        byte[] arrayOfByte3 = this.outBuffer;
        int l = 1 + this.bytePos;
        arrayOfByte3[l] = (byte) (arrayOfByte3[l] & maskRight8bits[this.bitPos]);
        byte[] arrayOfByte4 = this.outBuffer;
        int i1 = 1 + this.bytePos;
        arrayOfByte4[i1] = (byte) (arrayOfByte4[i1] | i & 0xFF);
    }

    public void skipBits(int paramInt) {
        this.bitPos = (paramInt + this.bitPos);
        this.bytePos += (this.bitPos >> 3);
        this.bitPos = (0x7 & this.bitPos);
    }


    public void WriteFileTemp(File outputFile, int startFrame, int numFrames, boolean is, boolean out) throws IOException {
        super.WriteFileTemp(outputFile, startFrame, numFrames, is, out);
    }

    static private int BITRATES_MPEG1_L3[] = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};
    static private int BITRATES_MPEG2_L3[] = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0};
    static private int SAMPLERATES_MPEG1_L3[] = {44100, 48000, 32000, 0};
    static private int SAMPLERATES_MPEG2_L3[] = {22050, 24000, 16000, 0};
}
