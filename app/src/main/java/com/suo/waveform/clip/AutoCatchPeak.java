
package com.suo.waveform.clip;

public class AutoCatchPeak {

    int XValue;

    int XLeft;

    int XRight;

    int XWidth;

    int NumPeaks;

    public int getXValue() {
        return XValue;
    }

    public void setXValue(int xValue) {
        XValue = xValue;
    }

    public int getXLeft() {
        return XLeft;
    }

    public void setXLeft(int xLeft) {
        XLeft = xLeft;
    }

    public int getXRight() {
        return XRight;
    }

    public void setXRight(int xRight) {
        XRight = xRight;
    }

    public int getXWidth() {
        return XWidth;
    }

    public void setXWidth(int xWidth) {
        XWidth = xWidth;
    }

    public void ChangeToZipRate(double zipRate) {
        XLeft = (int) (XLeft * zipRate);
        XRight = (int) (XRight * zipRate);
        XWidth = (int) (XWidth * zipRate);
        XValue = (int) (XValue * zipRate);
        if (XLeft == XRight && XLeft == XValue) {
            if (XRight - XWidth > 0) {
                XLeft = XRight - XWidth;
            } else {
                XRight = XLeft + XWidth;
            }
        }
    }

    public int getNumPeaks() {
        return NumPeaks;
    }

    public void setNumPeaks(int numPeaks) {
        NumPeaks = numPeaks;
    }

}
