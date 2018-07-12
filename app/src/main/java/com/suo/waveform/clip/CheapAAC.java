/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.suo.waveform.clip;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * CheapAAC is a CheapSoundFile implementation for AAC (Advanced Audio
 * Codec) encoded sound files.  It supports files with an MP4 header,
 * including unencrypted files encoded by Apple iTunes, and also
 * files with a more basic ADTS header.
 */

/**
 * CheapAAC 是一种 CheapSoundFile 实现为 AAC （高级音频 * 编解码器) 编码的声音文件。它支持用 MP4 头文件
 * 和未加密的文件由苹果公司 iTunes 的编码和还 * 具有更多的基本 ADTS 头文件。
 */
@SuppressLint("UseSparseArrays")
public class CheapAAC extends CheapSoundFile {
    public static Factory getFactory() {
        return new Factory() {
            public CheapSoundFile create() {
                return new CheapAAC();
            }

            public String[] getSupportedExtensions() {
                return new String[] {
                        "aac", "m4a"
                };
            }
        };
    }

    class Atom {
        public int start;

        public int len; // including header

        public byte[] data;
    };

    public static final int kDINF = 0x64696e66;

    public static final int kFTYP = 0x66747970;

    public static final int kHDLR = 0x68646c72;

    public static final int kMDAT = 0x6d646174;

    public static final int kMDHD = 0x6d646864;

    public static final int kMDIA = 0x6d646961;

    public static final int kMINF = 0x6d696e66;

    public static final int kMOOV = 0x6d6f6f76;

    public static final int kMP4A = 0x6d703461;

    public static final int kMVHD = 0x6d766864;

    public static final int kSMHD = 0x736d6864;

    public static final int kSTBL = 0x7374626c;

    public static final int kSTCO = 0x7374636f;

    public static final int kSTSC = 0x73747363;

    public static final int kSTSD = 0x73747364;

    public static final int kSTSZ = 0x7374737a;

    public static final int kSTTS = 0x73747473;

    public static final int kTKHD = 0x746b6864;

    public static final int kTRAK = 0x7472616b;

    public static final int[] kRequiredAtoms = {
            kDINF, kHDLR, kMDHD, kMDIA, kMINF, kMOOV, kMVHD, kSMHD, kSTBL, kSTSD, kSTSZ, kSTTS,
            kTKHD, kTRAK,
    };

    public static final int[] kSaveDataAtoms = {
            kDINF, kHDLR, kMDHD, kMVHD, kSMHD, kTKHD, kSTSD,
    };

    // Member variables containing frame info
    private int mNumFrames;

    private int[] mFrameOffsets;

    private int[] mFrameLens;

    private int[] mFrameGains;

    private int mFileSize;

    private HashMap<Integer, Atom> mAtomMap;

    // Member variables containing sound file info
    private int mSampleRate;

    private int mChannels;

    private int mSamplesPerFrame;

    // Member variables used only while initially parsing the file
    private int mOffset;

    private int mMinGain;

    private int mMaxGain;

    private int mMdatOffset;

    private int mMdatLength;

    public CheapAAC() {
    }

    public int getNumFrames() {
        return mNumFrames;
    }

    public int getSamplesPerFrame() {
        return mSamplesPerFrame;
    }

    public int[] getFrameOffsets() {
        return mFrameOffsets;
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
        return mFileSize / (mNumFrames * mSamplesPerFrame);
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getChannels() {
        return mChannels;
    }

    public String getFiletype() {
        return "AAC";
    }

    public String atomToString(int atomType) {
        String str = "";
        str += (char) ((atomType >> 24) & 0xff);
        str += (char) ((atomType >> 16) & 0xff);
        str += (char) ((atomType >> 8) & 0xff);
        str += (char) (atomType & 0xff);
        return str;
    }

    public void ReadFile(File inputFile) throws java.io.FileNotFoundException, IOException {
        super.ReadFile(inputFile);
        mChannels = 0;
        mSampleRate = 0;
        mSamplesPerFrame = 0;
        mNumFrames = 0;
        mMinGain = 255;
        mMaxGain = 0;
        mOffset = 0;
        mMdatOffset = -1;
        mMdatLength = -1;

        mAtomMap = new HashMap<Integer, Atom>();

        // No need to handle filesizes larger than can fit in a 32-bit int
        mFileSize = (int) mInputFile.length();

        /* System.out.println("File size = " + mFileSize); */

        if (mFileSize < 128) {
            throw new IOException("File too small to parse");
        }

        // "mp3hash..."
        FileInputStream checkStream = new FileInputStream(mInputFile);
        final int gFlagSize = 41;
        byte[] buf = new byte[gFlagSize];

        long skipResult = checkStream.skip(mFileSize - gFlagSize);
        if (skipResult >= 0) {
            int readResult = checkStream.read(buf, 0, gFlagSize);
            if (readResult < 0){
                throw new IOException("can not read checkstream");
            }
        }
        if (buf[0] == 'k' && buf[1] == 'g' && buf[2] == 'm' && buf[3] == 'p' && buf[4] == '3' && buf[5] == 'h' &&
                buf[6] == 'a' && buf[7] == 's' && buf[8] == 'h') {
            mFileSize -= gFlagSize;
        }
        checkStream.close();

        // Read the first 8 bytes
        FileInputStream stream = new FileInputStream(mInputFile);
        byte[] header = new byte[8];
        int headerResult = stream.read(header, 0, 8);
        if (headerResult < 0){
            throw new IOException("can not read header");
        }

        if (header[0] == 0 && header[4] == 'f' && header[5] == 't' && header[6] == 'y'
                && header[7] == 'p') {
            // Create a new stream, reset to the beginning of the file
            stream = new FileInputStream(mInputFile);
            parseMp4(stream, mFileSize);
        } else {
            throw new IOException("Unknown file format");
        }

        if (mMdatOffset > 0 && mMdatLength > 0) {
            final int neroAACFrom = 570;
            int neroSkip = 0;
            if (mMdatOffset - neroAACFrom > 0) {
                FileInputStream cs = new FileInputStream(mInputFile);
                long skipAACFromResult = cs.skip(mMdatOffset - neroAACFrom);
                if (skipAACFromResult < 0){
                    throw new IOException("can not skip skipAACFromResult");
                }
                final int flagSize = 14;
                byte[] buffer = new byte[flagSize];
                int readCsResult = cs.read(buffer, 0, flagSize);
                if (readCsResult < 0){
                    throw new IOException("can not read cs");
                }
                if (buffer[0] == 'N' && buffer[1] == 'e' && buffer[2] == 'r' && buffer[3] == 'o' && buffer[5] == 'A'
                        && buffer[6] == 'A' && buffer[7] == 'C' && buffer[9] == 'c' && buffer[10] == 'o'
                        && buffer[11] == 'd' && buffer[12] == 'e' && buffer[13] == 'c') {
                    neroSkip = 8;
                }
                cs.close();
            }

            stream = new FileInputStream(mInputFile);
            mMdatOffset += neroSkip;
            long lSkipMdat = stream.skip(mMdatOffset);
            if (lSkipMdat < 0){
                throw new IOException("can not skip mdat");
            }
            mOffset = mMdatOffset;
            parseMdat(stream, mMdatLength);
        } else {
            throw new IOException("Didn't find mdat");
        }

        /*
         * for (int i = 0; i < mNumFrames; i++) { System.out.println("Gain " + i
         * + ": " + mFrameGains[i]); }
         */

        /*
         * System.out.println("Atoms found:"); for (int atomType :
         * mAtomMap.keySet()) { System.out.println("    " +
         * atomToString(atomType)); }
         */

        boolean bad = false;
        for (int requiredAtomType : kRequiredAtoms) {
            if (!mAtomMap.containsKey(requiredAtomType)) {
                bad = true;
            }
        }

        if (bad) {
            throw new IOException("Could not parse MP4 file");
        }
    }

    private void parseMp4(InputStream stream, int maxLen) throws IOException {
        /* System.out.println("parseMp4 maxLen = " + maxLen); */

        while (maxLen > 8) {
            int initialOffset = mOffset;

            byte[] atomHeader = new byte[8];

            if (stream.read(atomHeader, 0, 8) < 0){
                throw new IOException("Could not read atomHeader");
            }
            int atomLen = ((0xff & atomHeader[0]) << 24) | ((0xff & atomHeader[1]) << 16)
                    | ((0xff & atomHeader[2]) << 8) | ((0xff & atomHeader[3]));
            /*
             * System.out.println("atomType = " + (char)atomHeader[4] +
             * (char)atomHeader[5] + (char)atomHeader[6] + (char)atomHeader[7] +
             * "  " + "offset = " + mOffset + "  " + "atomLen = " + atomLen);
             */
            if (atomLen > maxLen)
                atomLen = maxLen;
            int atomType = ((0xff & atomHeader[4]) << 24) | ((0xff & atomHeader[5]) << 16)
                    | ((0xff & atomHeader[6]) << 8) | ((0xff & atomHeader[7]));

            Atom atom = new Atom();
            atom.start = mOffset;
            atom.len = atomLen;
            mAtomMap.put(atomType, atom);

            mOffset += 8;

            if (atomType == kMOOV || atomType == kTRAK || atomType == kMDIA || atomType == kMINF
                    || atomType == kSTBL) {
                parseMp4(stream, atomLen);
            } else if (atomType == kSTSZ) {
                parseStsz(stream, atomLen - 8);
            } else if (atomType == kSTTS) {
                parseStts(stream, atomLen - 8);
            } else if (atomType == kMDAT) {
                mMdatOffset = mOffset;
                mMdatLength = atomLen - 8;
            } else {
                for (int savedAtomType : kSaveDataAtoms) {
                    if (savedAtomType == atomType) {
                        byte[] data = new byte[atomLen - 8];
                        if (stream.read(data, 0, atomLen - 8) < 0){
                            throw new IOException("Could not read savedAtom");
                        }
                        mOffset += atomLen - 8;
                        mAtomMap.get(atomType).data = data;
                    }
                }
            }

            if (atomType == kSTSD) {
                parseMp4aFromStsd();
            }

            maxLen -= atomLen;
            int skipLen = atomLen - (mOffset - initialOffset);
            /* System.out.println("* atomLen: " + atomLen); */
            /* System.out.println("* mOffset: " + mOffset); */
            /* System.out.println("* initialOffset: " + initialOffset); */
            /* System.out.println("*   diff: " + (mOffset - initialOffset)); */
            /* System.out.println("* skipLen: " + skipLen); */

            if (skipLen < 0) {
                throw new IOException("Went over by " + (-skipLen) + " bytes");
            }

            if (stream.skip(skipLen) < 0) {
                throw new IOException("Could not skip result");
            }
            mOffset += skipLen;
        }
    }

    void parseStts(InputStream stream, int maxLen) throws IOException {
        byte[] sttsData = new byte[16];
        if (stream.read(sttsData, 0, 16) < 0){
            throw new IOException("Could not read sttsData");
        }
        mOffset += 16;
        mSamplesPerFrame = ((0xff & sttsData[12]) << 24) | ((0xff & sttsData[13]) << 16)
                | ((0xff & sttsData[14]) << 8) | ((0xff & sttsData[15]));

        /* System.out.println("STTS samples per frame: " + mSamplesPerFrame); */
    }

    // 每一帧占的字节数
    void parseStsz(InputStream stream, int maxLen) throws IOException {
        byte[] stszHeader = new byte[12];
        if (stream.read(stszHeader, 0, 12) < 0) {
            throw new IOException("Could not read stszHeader");
        }
        mOffset += 12;
        mNumFrames = ((0xff & stszHeader[8]) << 24) | ((0xff & stszHeader[9]) << 16)
                | ((0xff & stszHeader[10]) << 8) | ((0xff & stszHeader[11]));
        /* System.out.println("mNumFrames = " + mNumFrames); */

        mFrameOffsets = new int[mNumFrames];
        mFrameLens = new int[mNumFrames];
        mFrameGains = new int[mNumFrames];
        byte[] frameLenBytes = new byte[4 * mNumFrames];
        if (stream.read(frameLenBytes, 0, 4 * mNumFrames) < 0) {
            throw new IOException("Could not read frameLenBytes");
        }
        mOffset += 4 * mNumFrames;
        for (int i = 0; i < mNumFrames; i++) {
            mFrameLens[i] = ((0xff & frameLenBytes[4 * i + 0]) << 24)
                    | ((0xff & frameLenBytes[4 * i + 1]) << 16)
                    | ((0xff & frameLenBytes[4 * i + 2]) << 8)
                    | ((0xff & frameLenBytes[4 * i + 3]));
            /* System.out.println("FrameLen[" + i + "] = " + mFrameLens[i]); */
        }
    }

    void parseMp4aFromStsd() {
        byte[] stsdData = mAtomMap.get(kSTSD).data;
        mChannels = ((0xff & stsdData[32]) << 8) | ((0xff & stsdData[33]));
        mSampleRate = ((0xff & stsdData[40]) << 8) | ((0xff & stsdData[41]));

        /*
         * System.out.println("%% channels = " + mChannels + ", " +
         * "sampleRate = " + mSampleRate);
         */
    }

    void parseMdat(InputStream stream, int maxLen) throws IOException {
        /* System.out.println("***MDAT***"); */
        int initialOffset = mOffset;
        for (int i = 0; i < mNumFrames; i++) {
            mFrameOffsets[i] = mOffset;
            /* System.out.println("&&& start: " + (mOffset - initialOffset)); */
            /*
             * System.out.println("&&& start + len: " + (mOffset - initialOffset
             * + mFrameLens[i]));
             */
            /* System.out.println("&&& maxLen: " + maxLen); */

            if (mOffset - initialOffset + mFrameLens[i] > maxLen - 8) {
                mFrameGains[i] = 0;
            } else {
                readFrameAndComputeGain(stream, i);
            }
            if (mFrameGains[i] < mMinGain)
                mMinGain = mFrameGains[i];
            if (mFrameGains[i] > mMaxGain)
                mMaxGain = mFrameGains[i];

            if (mProgressListener != null) {
                boolean keepGoing = mProgressListener.reportProgress(mOffset * 1.0 / mFileSize);
                if (!keepGoing) {
                    break;
                }
            }
        }
    }

    void readFrameAndComputeGain(InputStream stream, int frameIndex) throws IOException {

        if (mFrameLens[frameIndex] < 4) {
            mFrameGains[frameIndex] = 0;
            if (stream.skip(mFrameLens[frameIndex]) < 0) {
                throw new IOException("Could not skip mFrameLens");
            }
            return;
        }

        int initialOffset = mOffset;

        byte[] data = new byte[4];
        if (stream.read(data, 0, 4) < 0){
            throw new IOException("Could not read stream");
        }
        mOffset += 4;

        /*
         * System.out.println( "Block " + frameIndex + ": " + data[0] + " " +
         * data[1] + " " + data[2] + " " + data[3]);
         */

        int idSynEle = (0xe0 & data[0]) >> 5;
        /* System.out.println("idSynEle = " + idSynEle); */

        switch (idSynEle) {
            case 0: // ID_SCE: mono
                int monoGain = ((0x01 & data[0]) << 7) | ((0xfe & data[1]) >> 1);
                /* System.out.println("monoGain = " + monoGain); */
                mFrameGains[frameIndex] = monoGain;
                break;
            case 1: // ID_CPE: stereo
                int windowSequence = (0x60 & data[1]) >> 5;
                /* System.out.println("windowSequence = " + windowSequence); */
                // int windowShape = (0x10 & data[1]) >> 4;
                /* System.out.println("windowShape = " + windowShape); */

                int maxSfb;
                int scaleFactorGrouping;
                int maskPresent;
                int startBit;

                if (windowSequence == 2) {
                    maxSfb = 0x0f & data[1];

                    scaleFactorGrouping = (0xfe & data[2]) >> 1;

                    maskPresent = ((0x01 & data[2]) << 1) | ((0x80 & data[3]) >> 7);

                    startBit = 25;
                } else {
                    maxSfb = ((0x0f & data[1]) << 2) | ((0xc0 & data[2]) >> 6);

                    scaleFactorGrouping = -1;

                    maskPresent = (0x18 & data[2]) >> 3;

                    startBit = 21;
                }

                /* System.out.println("maxSfb = " + maxSfb); */
                /*
                 * System.out.println("scaleFactorGrouping = " +
                 * scaleFactorGrouping);
                 */
                /* System.out.println("maskPresent = " + maskPresent); */
                /* System.out.println("startBit = " + startBit); */

                if (maskPresent == 1) {
                    int sfgZeroBitCount = 0;
                    for (int b = 0; b < 7; b++) {
                        if ((scaleFactorGrouping & (1 << b)) == 0) {
                            /*
                             * System.out.println("  1 point for bit " + b +
                             * ": " + (1 << b) + ", " + (scaleFactorGrouping &
                             * (1 << b)));
                             */
                            sfgZeroBitCount++;
                        }
                    }
                    /*
                     * System.out.println("sfgZeroBitCount = " +
                     * sfgZeroBitCount);
                     */

                    int numWindowGroups = 1 + sfgZeroBitCount;
                    /*
                     * System.out.println("numWindowGroups = " +
                     * numWindowGroups);
                     */

                    int skip = maxSfb * numWindowGroups;
                    /* System.out.println("skip = " + skip); */

                    startBit += skip;
                    /* System.out.println("new startBit = " + startBit); */
                }

                // We may need to fill our buffer with more than the 4
                // bytes we've already read, here.
                int bytesNeeded = 1 + ((startBit + 7) / 8);
                byte[] oldData = data;
                data = new byte[bytesNeeded];
                data[0] = oldData[0];
                data[1] = oldData[1];
                data[2] = oldData[2];
                data[3] = oldData[3];
                if (stream.read(data, 4, bytesNeeded - 4) < 0){
                    throw new IOException("Could not read stream");
                }
                mOffset += (bytesNeeded - 4);
                /* System.out.println("bytesNeeded: " + bytesNeeded); */

                int firstChannelGain = 0;
                for (int b = 0; b < 8; b++) {
                    int b0 = (b + startBit) / 8;
                    int b1 = 7 - ((b + startBit) % 8);
                    int add = (((1 << b1) & data[b0]) >> b1) << (7 - b);
                    /*
                     * System.out.println("Bit " + (b + startBit) + " " + "b0 "
                     * + b0 + " " + "b1 " + b1 + " " + "add " + add);
                     */
                    firstChannelGain += add;
                }
                /* System.out.println("firstChannelGain = " + firstChannelGain); */

                mFrameGains[frameIndex] = firstChannelGain;
                break;

            default:
                if (frameIndex > 0) {
                    mFrameGains[frameIndex] = mFrameGains[frameIndex - 1];
                } else {
                    mFrameGains[frameIndex] = 0;
                }
                /* System.out.println("Unhandled idSynEle"); */
                break;
        }

        int skip = mFrameLens[frameIndex] - (mOffset - initialOffset);
        /* System.out.println("frameLen = " + mFrameLens[frameIndex]); */
        /* System.out.println("Skip = " + skip); */

        if (stream.skip(skip) < 0){
            throw new IOException("Could not skip stream");
        }
        mOffset += skip;
    }

    public void StartAtom(FileOutputStream out, int atomType) throws IOException {
        byte[] atomHeader = new byte[8];
        int atomLen = mAtomMap.get(atomType).len;
        atomHeader[0] = (byte) ((atomLen >> 24) & 0xff);
        atomHeader[1] = (byte) ((atomLen >> 16) & 0xff);
        atomHeader[2] = (byte) ((atomLen >> 8) & 0xff);
        atomHeader[3] = (byte) (atomLen & 0xff);
        atomHeader[4] = (byte) ((atomType >> 24) & 0xff);
        atomHeader[5] = (byte) ((atomType >> 16) & 0xff);
        atomHeader[6] = (byte) ((atomType >> 8) & 0xff);
        atomHeader[7] = (byte) (atomType & 0xff);
        out.write(atomHeader, 0, 8);
    }

    public void WriteAtom(FileOutputStream out, int atomType) throws IOException {
        Atom atom = mAtomMap.get(atomType);
        StartAtom(out, atomType);
        out.write(atom.data, 0, atom.len - 8);
    }

    public void SetAtomData(int atomType, byte[] data) {
        Atom atom = mAtomMap.get(atomType);
        if (atom == null) {
            atom = new Atom();
            mAtomMap.put(atomType, atom);
        }
        atom.len = data.length + 8;
        atom.data = data;
    }

    public void WriteFile(File outputFile, int startFrame, int numFrames)
            throws IOException {
        outputFile.createNewFile();
        FileInputStream in = new FileInputStream(mInputFile);
        FileOutputStream out = new FileOutputStream(outputFile);

        SetAtomData(kFTYP, new byte[] {
                'M', '4', 'A', ' ', 0, 0, 0, 0, 'M', '4', 'A', ' ', 'm', 'p', '4', '2', 'i', 's',
                'o', 'm', 0, 0, 0, 0
        });

        SetAtomData(kSTTS, new byte[] {
                0,
                0,
                0,
                0, // version / flags
                0,
                0,
                0,
                1, // entry count
                (byte) ((numFrames >> 24) & 0xff), (byte) ((numFrames >> 16) & 0xff),
                (byte) ((numFrames >> 8) & 0xff), (byte) (numFrames & 0xff),
                (byte) ((mSamplesPerFrame >> 24) & 0xff), (byte) ((mSamplesPerFrame >> 16) & 0xff),
                (byte) ((mSamplesPerFrame >> 8) & 0xff), (byte) (mSamplesPerFrame & 0xff)
        });

        SetAtomData(kSTSC, new byte[] {
                0, 0, 0,
                0, // version / flags
                0, 0, 0,
                1, // entry count
                0, 0, 0,
                1, // first chunk
                (byte) ((numFrames >> 24) & 0xff), (byte) ((numFrames >> 16) & 0xff),
                (byte) ((numFrames >> 8) & 0xff), (byte) (numFrames & 0xff), 0, 0, 0, 1
        // Smaple desc index
                });

        byte[] stszData = new byte[12 + 4 * numFrames];
        stszData[8] = (byte) ((numFrames >> 24) & 0xff);
        stszData[9] = (byte) ((numFrames >> 16) & 0xff);
        stszData[10] = (byte) ((numFrames >> 8) & 0xff);
        stszData[11] = (byte) (numFrames & 0xff);
        for (int i = 0; i < numFrames; i++) {
            stszData[12 + 4 * i] = (byte) ((mFrameLens[startFrame + i] >> 24) & 0xff);
            stszData[13 + 4 * i] = (byte) ((mFrameLens[startFrame + i] >> 16) & 0xff);
            stszData[14 + 4 * i] = (byte) ((mFrameLens[startFrame + i] >> 8) & 0xff);
            stszData[15 + 4 * i] = (byte) (mFrameLens[startFrame + i] & 0xff);
        }
        SetAtomData(kSTSZ, stszData);

        int mdatOffset = 144 + 4 * numFrames + mAtomMap.get(kSTSD).len + mAtomMap.get(kSTSC).len
                + mAtomMap.get(kMVHD).len + mAtomMap.get(kTKHD).len + mAtomMap.get(kMDHD).len
                + mAtomMap.get(kHDLR).len + mAtomMap.get(kSMHD).len + mAtomMap.get(kDINF).len;

        /* System.out.println("Mdat offset: " + mdatOffset); */

        SetAtomData(kSTCO, new byte[] {
                0, 0, 0,
                0, // version / flags
                0, 0, 0,
                1, // entry count
                (byte) ((mdatOffset >> 24) & 0xff), (byte) ((mdatOffset >> 16) & 0xff),
                (byte) ((mdatOffset >> 8) & 0xff), (byte) (mdatOffset & 0xff),
        });


        long time = System.currentTimeMillis() / 1000;
        time += (66 * 365 + 16) * 24 * 60 * 60;  // number of seconds between 1904 and 1970
        byte[] createTime = new byte[4];
        createTime[0] = (byte)((time >> 24) & 0xFF);
        createTime[1] = (byte)((time >> 16) & 0xFF);
        createTime[2] = (byte)((time >> 8) & 0xFF);
        createTime[3] = (byte)(time & 0xFF);
        long numSamples = 1024 * numFrames;
        long durationMS = (numSamples * 1000) / mSampleRate;
        if ((numSamples * 1000) % mSampleRate > 0) {  // round the duration up.
            durationMS++;
        }
//        byte[] numSaplesBytes = new byte[] {
//                (byte)((numSamples >> 26) & 0XFF),
//                (byte)((numSamples >> 16) & 0XFF),
//                (byte)((numSamples >> 8) & 0XFF),
//                (byte)(numSamples & 0XFF)
//        };
        byte[] durationMSBytes = new byte[] {
                (byte)((durationMS >> 26) & 0XFF),
                (byte)((durationMS >> 16) & 0XFF),
                (byte)((durationMS >> 8) & 0XFF),
                (byte)(durationMS & 0XFF)
        };

        int type = kMDHD;
        Atom atom = mAtomMap.get(type);
        if (atom == null) {
            atom = new Atom();
            mAtomMap.put(type, atom);
        }
        atom.data = new byte[] {
                0, // version, 0 or 1
                0, 0, 0,  // flag
                createTime[0], createTime[1], createTime[2], createTime[3],  // creation time.
                createTime[0], createTime[1], createTime[2], createTime[3],  // modification time.
                0, 0, 0x03, (byte)0xE8,  // timescale = 1000 => duration expressed in ms.  1000为单位
                durationMSBytes[0], durationMSBytes[1], durationMSBytes[2], durationMSBytes[3],  // duration in ms.
                0, 0,     // languages
                0, 0      // pre-defined;
        };
        atom.len = atom.data.length + 8;

        type = kMVHD;
        atom = mAtomMap.get(type);
        if (atom == null) {
            atom = new Atom();
            mAtomMap.put(type, atom);
        }
        atom.data = new byte[] {
                0, // version, 0 or 1
                0, 0, 0, // flag
                createTime[0], createTime[1], createTime[2], createTime[3],  // creation time.
                createTime[0], createTime[1], createTime[2], createTime[3],  // modification time.
                0, 0, 0x03, (byte)0xE8,  // timescale = 1000 => duration expressed in ms.  1000为单位
                durationMSBytes[0], durationMSBytes[1], durationMSBytes[2], durationMSBytes[3],  // duration in ms.
                0, 1, 0, 0,  // rate = 1.0
                1, 0,        // volume = 1.0
                0, 0,        // reserved
                0, 0, 0, 0,  // reserved
                0, 0, 0, 0,  // reserved
                0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // unity matrix for video, 36bytes
                0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0x40, 0, 0, 0,
                0, 0, 0, 0,  // pre-defined
                0, 0, 0, 0,  // pre-defined
                0, 0, 0, 0,  // pre-defined
                0, 0, 0, 0,  // pre-defined
                0, 0, 0, 0,  // pre-defined
                0, 0, 0, 0,  // pre-defined
                0, 0, 0, 2   // next track ID, 4bytes
        };
        atom.len = atom.data.length + 8;

//        type = kTKHD;
//        atom = mAtomMap.get(type);
//        if (atom == null) {
//            atom = new Atom();
//            mAtomMap.put(type, atom);
//        }
//        atom.data = new byte[] {
//                0, // version, 0 or 1
//                0, 0, (byte)0x1, // flag
//                createTime[0], createTime[1], createTime[2], createTime[3],  // creation time.
//                createTime[0], createTime[1], createTime[2], createTime[3],  // modification time.
//                0, 0, 0, 1,  // track ID
//                0, 0, 0, 0,  // reserved
//                durationMSBytes[0], durationMSBytes[1], durationMSBytes[2], durationMSBytes[3],  // duration in ms.
//                0, 0, 0, 0,  // reserved
//                0, 0, 0, 0,  // reserved
//                0, 0,        // layer for video
//                0, 0,        // alternate group
//                1, 0,        // volume = 1.0
//                0, 0,        // reserved
//                0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  // unity matrix for video, 36bytes
//                0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
//                0, 0, 0, 0, 0, 0, 0, 0, 0x40, 0, 0, 0,
//                0, 0, 0, 0,  // width
//                0, 0, 0, 0   // height
//        };
//        atom.len = atom.data.length + 8;

        mAtomMap.get(kSTBL).len = 8 + mAtomMap.get(kSTSD).len + mAtomMap.get(kSTTS).len
                + mAtomMap.get(kSTSC).len + mAtomMap.get(kSTSZ).len + mAtomMap.get(kSTCO).len;

        mAtomMap.get(kMINF).len = 8 + mAtomMap.get(kDINF).len + mAtomMap.get(kSMHD).len
                + mAtomMap.get(kSTBL).len;

        mAtomMap.get(kMDIA).len = 8 + mAtomMap.get(kMDHD).len + mAtomMap.get(kHDLR).len
                + mAtomMap.get(kMINF).len;

        mAtomMap.get(kTRAK).len = 8 + mAtomMap.get(kTKHD).len + mAtomMap.get(kMDIA).len;

        mAtomMap.get(kMOOV).len = 8 + mAtomMap.get(kMVHD).len + mAtomMap.get(kTRAK).len;

        int mdatLen = 8;
        for (int i = 0; i < numFrames; i++) {
            mdatLen += mFrameLens[startFrame + i];
        }
        mAtomMap.get(kMDAT).len = mdatLen;

        WriteAtom(out, kFTYP);
        StartAtom(out, kMOOV);
        {
            WriteAtom(out, kMVHD);
            StartAtom(out, kTRAK);
            {
                WriteAtom(out, kTKHD);
                StartAtom(out, kMDIA);
                {
                    WriteAtom(out, kMDHD);
                    WriteAtom(out, kHDLR);
                    StartAtom(out, kMINF);
                    {
                        WriteAtom(out, kDINF);
                        WriteAtom(out, kSMHD);
                        StartAtom(out, kSTBL);
                        {
                            WriteAtom(out, kSTSD);
                            WriteAtom(out, kSTTS);
                            WriteAtom(out, kSTSC);
                            WriteAtom(out, kSTSZ);
                            WriteAtom(out, kSTCO);
                        }
                    }
                }
            }
        }
        StartAtom(out, kMDAT);

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
            if (skip < 0) {
                continue;
            }
            if (skip > 0) {
                if (in.skip(skip) > 0) {
                    pos += skip;
                }
            }
            if (in.read(buffer, 0, len) > 0){
                out.write(buffer, 0, len);
                pos += len;
            }
        }

        in.close();
        out.close();
    }

};
