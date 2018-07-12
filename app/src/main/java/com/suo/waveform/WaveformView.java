/*
 * Copyright (C) 2008 Google Inc.
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

package com.suo.waveform;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.suo.waveform.clip.AutoCatchPeak;
import com.suo.waveform.clip.CheapSoundFile;
import com.suo.waveform.util.SystemUtils;

public class WaveformView extends View {
    public interface WaveformListener {
        public void waveformTouchStart(float x);

        public void waveformTouchMove(float x);

        public void waveformTouchEnd();

        public void waveformFling(float x);

        public void waveformDraw();
    };

    // Colors
    private Paint mGridPaint;

    private Paint mBgPaint;

    private Paint mSelectedLinePaint;

    private Paint mUnselectedLinePaint;

    private Paint mUnselectedBkgndLinePaint;

    private Paint mselectedBkgndLinePaint;

    private Paint mPlaybackLinePaint;

    private Paint mTimecodePaint;

    private CheapSoundFile mSoundFile;

    private int[] mLenByZoomLevel;

    /**
     * 不同伸缩倍率存放频谱的数组（最大值不超过1）
     */
    private double[][] mValuesByZoomLevel;

    private double[] mZoomFactorByZoomLevel;

    /**
     * 当前伸缩倍率对应的频谱高度坐标
     */
    private int[] mHeightsAtThisZoomLevel;

    private int mZoomLevel;

    private int mNumZoomLevels;

    private int mSampleRate;

    private int mSamplesPerFrame;

    private int mOffset;

    private int mSelectionStart;

    private int mSelectionEnd;

    private int mPlaybackPos;

    private double mWidthCompressRate = 1; // 屏幕宽度与铃声长度的压缩比

    private int mOffsetLeftDip = 20; // 左边偏移量

    private int mOffsetRightDip = 20; // 右边偏移量

    public int mLeftOffsetPix;

    public int mRightOffsetPix;

    private Context mContext;

    private WaveformListener mListener;

    private GestureDetector mGestureDetector;

    private boolean mInitialized;

    private float mDensity;

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // We don't want keys, the markers get these
        mContext = context;
        setFocusable(false);
        mBgPaint = new Paint();
        mBgPaint.setAntiAlias(true);
        mGridPaint = new Paint();
        mGridPaint.setAntiAlias(false);
        mGridPaint.setColor(getResources().getColor(R.color.grid_line));
        mSelectedLinePaint = new Paint();
        mSelectedLinePaint.setAntiAlias(true);
        mSelectedLinePaint.setColor(getResources().getColor(R.color.waveform_selected));
        mUnselectedLinePaint = new Paint();
        mUnselectedLinePaint.setAntiAlias(false);
        mUnselectedLinePaint.setColor(getResources().getColor(R.color.waveform_unselected));
        mUnselectedBkgndLinePaint = new Paint();
        mUnselectedBkgndLinePaint.setAntiAlias(false);
        mUnselectedBkgndLinePaint.setColor(getResources().getColor(
                R.color.waveform_unselected_bkgnd_overlay));

        mselectedBkgndLinePaint = new Paint();
        mselectedBkgndLinePaint.setAntiAlias(false);
        mselectedBkgndLinePaint.setColor(getResources().getColor(R.color.t));
        mPlaybackLinePaint = new Paint();
        mPlaybackLinePaint.setAntiAlias(false);
        mPlaybackLinePaint.setColor(getResources().getColor(R.color.title));
        mTimecodePaint = new Paint();
        mTimecodePaint.setTextSize(12);
        mTimecodePaint.setAntiAlias(true);
        mTimecodePaint.setColor(getResources().getColor(R.color.timecode));
        mTimecodePaint.setShadowLayer(2, 1, 1,
                getResources().getColor(R.color.timecode_shadow));

        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                        // mListener.waveformFling(vx);
                        return true;
                    }
                });

        mSoundFile = null;
        mLenByZoomLevel = null;
        mValuesByZoomLevel = null;
        mHeightsAtThisZoomLevel = null;
        mOffset = 0;
        mPlaybackPos = -1;
        mSelectionStart = 0;
        mSelectionEnd = 0;
        mInitialized = false;
        mDensity = 1.0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mListener.waveformTouchStart(event.getX());
                break;
            case MotionEvent.ACTION_MOVE:
                mListener.waveformTouchMove(event.getX());
                break;
            case MotionEvent.ACTION_UP:
                mListener.waveformTouchEnd();
                break;
        }
        return true;
    }

    public void setSoundFile(CheapSoundFile soundFile) {
        mSoundFile = soundFile;
        mSampleRate = mSoundFile.getSampleRate();
        mSamplesPerFrame = mSoundFile.getSamplesPerFrame();
        computeDoublesForAllZoomLevels();
        mHeightsAtThisZoomLevel = null;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public int getZoomLevel() {
        return mZoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        while (mZoomLevel > zoomLevel) {
            zoomIn();
        }
        while (mZoomLevel < zoomLevel) {
            zoomOut();
        }
    }

    public boolean canZoomIn() {
        return (mZoomLevel > 0);
    }

    public void zoomIn() {
        if (canZoomIn()) {
            mZoomLevel--;
            mSelectionStart *= 2;
            mSelectionEnd *= 2;
            mHeightsAtThisZoomLevel = null;
            int offsetCenter = mOffset + getMeasuredWidth() / 2;
            offsetCenter *= 2;
            mOffset = offsetCenter - getMeasuredWidth() / 2;
            if (mOffset < 0)
                mOffset = 0;
            invalidate();
        }
    }

    public boolean canZoomOut() {
        return (mZoomLevel < mNumZoomLevels - 1);
    }

    public void zoomOut() {
        if (canZoomOut()) {
            mZoomLevel++;
            mSelectionStart /= 2;
            mSelectionEnd /= 2;
            int offsetCenter = mOffset + getMeasuredWidth() / 2;
            offsetCenter /= 2;
            mOffset = offsetCenter - getMeasuredWidth() / 2;
            if (mOffset < 0)
                mOffset = 0;
            mHeightsAtThisZoomLevel = null;
            invalidate();
        }
    }

    public int maxPos() {
        return mLenByZoomLevel[mZoomLevel];
    }

    public int secondsToFrames(double seconds) {
        return (int) (1.0 * seconds * mSampleRate / mSamplesPerFrame + 0.5);
    }

    public int secondsToPixels(double seconds) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int) (z * seconds * mSampleRate / (mSamplesPerFrame * mWidthCompressRate) + 0.5);
    }

    public int gecondsToPixels() {
        return 40;
    }

    public double pixelsToSeconds(int pixels) {
        if (mZoomLevel < 0 || mZoomLevel >= mZoomFactorByZoomLevel.length){
            mZoomLevel = 0;
        }
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (pixels * (double) mSamplesPerFrame * mWidthCompressRate / (mSampleRate * z));
    }

    public int millisecsToPixels(int msecs) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int) ((msecs * 1.0 * mSampleRate * z)
                / (1000.0 * mSamplesPerFrame * mWidthCompressRate) + 0.5);
    }

    public int pixelsToMillisecs(int pixels) {
        double z = mZoomFactorByZoomLevel[mZoomLevel];
        return (int) (pixels * (1000.0 * mSamplesPerFrame) * mWidthCompressRate / (mSampleRate * z) + 0.5);
    }

    public void setParameters(int start, int end, int offset) {
        mSelectionStart = start;
        mSelectionEnd = end;
        mOffset = offset;
    }

    public int getStart() {
        return mSelectionStart;
    }

    public int getEnd() {
        return mSelectionEnd;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setPlayback(int pos) {
        mPlaybackPos = pos;
    }

    public int getPlayback() {
        return mPlaybackPos;
    }

    public void setListener(WaveformListener listener) {
        mListener = listener;
    }

    public void recomputeHeights(float density) {
        mHeightsAtThisZoomLevel = null;
        mTimecodePaint.setTextSize((int) (12 * density));
        mDensity = density;
        invalidate();
    }

    protected void drawWaveformYLine(Canvas canvas, int x, int y0, int y1, Paint paint) {
        canvas.drawLine(x, y0, x, y1, paint);
    }

    protected void drawWaveformXLine(Canvas canvas, int x, int x1, int y, Paint paint) {
        canvas.drawLine(x, y, x1, y, paint);
    }

    protected void drawWaveformLine2Pix(Canvas canvas, int x, int y0, int y1, Paint paint) {
        canvas.drawLine(x, y0, x + 1, y1, paint);
    }

    public double getWidthCompressRate() {
        return mWidthCompressRate;
    }

    public void setWidthCompressRate(double mWidthCompressRate) {
        this.mWidthCompressRate = mWidthCompressRate;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSoundFile == null)
            return;
        if (mHeightsAtThisZoomLevel == null)
            computeIntsForThisZoomLevel();
        // Draw waveform
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int start = mOffset;
        int width = mHeightsAtThisZoomLevel.length - start;
        int ctr = measuredHeight / 2;
        if (width > measuredWidth)
            width = measuredWidth;

        // Draw grid
        double onePixelInSecs = pixelsToSeconds(1);
        // boolean onlyEveryFiveSecs = (onePixelInSecs > 1.0 / 50.0);
        double fractionalSecs = mOffset * onePixelInSecs;
        int integerSecs = (int) fractionalSecs;
        int i = 0;
        /*
         * while (i < width) { i++; fractionalSecs += onePixelInSecs; int
         * integerSecsNew = (int) fractionalSecs; if (integerSecsNew !=
         * integerSecs) { integerSecs = integerSecsNew; if (!onlyEveryFiveSecs
         * || 0 == (integerSecs % 5)) { //画出与时间对应的线 canvas.drawLine(i, 0, i,
         * measuredHeight, mGridPaint); } } }
         */

        // Draw waveform
        // 画已选区域背景（绘制背景）

//        Rect dst = new Rect();// 屏幕 >>目标矩形
//
//        dst.left = 0;
//        dst.top = 0;
//        dst.right = measuredWidth;
//        dst.bottom = 0 + measuredHeight;
//        Shader mBgShader = new LinearGradient(dst.left,dst.top,dst.right,dst.bottom
//                ,new int[]{getResources().getColor(R.color.gradient_begin_color),getResources().getColor(R.color.gradient_end_color)},null, Shader.TileMode.REPEAT);
//        mBgPaint.setShader(mBgShader);
//        canvas.drawRect(dst,mBgPaint);
//        Bitmap wfBg = BitmapFactory.decodeResource(getResources(),
//                R.drawable.musicmake_waveform_bg);
//        Rect src = new Rect();
//        src.left = 0;
//        src.top = 0;
//        src.right = wfBg.getWidth();
//        src.bottom = wfBg.getHeight();
//
//        canvas.drawBitmap(wfBg, src, dst, null);

        // 画频谱
        Paint paintPP = mSelectedLinePaint;

        for (i = 0; i < width - 3; i++) {
            int nMiddleHeight = (mHeightsAtThisZoomLevel[i] + mHeightsAtThisZoomLevel[i + 1] + mHeightsAtThisZoomLevel[i + 2]) / 3;
            nMiddleHeight *= 0.8;
            if ((i % 2 == 0)) {
                drawWaveformLine2Pix(canvas, mLeftOffsetPix + i, ctr - nMiddleHeight, ctr + 1
                        + nMiddleHeight, paintPP); // 画频谱
            }
        }

        int lastLineY = measuredHeight;
        // 画背景栅栏
        Paint linePaint = new Paint();
        linePaint.setColor(getResources().getColor(R.color.waveform_bg_line));
        int lineY = SystemUtils.dip2px(getContext(), 16);
        for (int x=0;x<8;x++) {
            int y = lineY * x;
            if (x == 7) {
                lastLineY = y;
            }
            drawWaveformXLine(canvas, mLeftOffsetPix, measuredWidth - mRightOffsetPix, y, linePaint);
        }

        int lineX = SystemUtils.dip2px(mContext, 30);
        int count = (measuredWidth - mLeftOffsetPix - mRightOffsetPix) / lineX;
        lineX = (measuredWidth - mLeftOffsetPix - mRightOffsetPix) / count;
        for (int x=0;x<count+1;x++) {
            drawWaveformYLine(canvas, mLeftOffsetPix + x * lineX, 0, lastLineY, linePaint);
        }




        // 画选中区域 不透明度26%的skin_common_widget
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(R.color.waveform_border_line));
        paint.setStyle(Paint.Style.FILL);
        Rect rect = new Rect();
        rect.left = mSelectionStart;
        rect.top = 0;
        rect.right = mSelectionEnd;
        rect.bottom = lastLineY;

        canvas.drawRect(rect, paint);


        // 画频谱上的白色
//        Rect dstWhite = new Rect();// 屏幕 >>目标矩形
//
//        dstWhite.left = mSelectionStart;
//        dstWhite.top = 0;
//        dstWhite.right = mSelectionEnd;
//        dstWhite.bottom = 0 + measuredHeight;
//        Bitmap wfPPWhite = BitmapFactory.decodeResource(getResources(),
//                R.drawable.musicmake_waveform_pinpu_white);
//
//        Rect srcWhite = new Rect();
//        srcWhite.left = 0;
//        srcWhite.top = 0;
//        srcWhite.right = wfPPWhite.getWidth();
//        srcWhite.bottom = wfPPWhite.getHeight();
//
//        canvas.drawBitmap(wfPPWhite, srcWhite, dstWhite, null);

        // 画顶层左黑色+顶层右黑色

        // Rect srcTopBlack =
        // this.getResources().getDrawable(R.drawable.musicmake_waveform_top_black)
        // .getBounds();// 图片 >>原矩形
        //

//        Rect dstTopLeftBlack = new Rect();// 屏幕 >>目标矩形
//        Rect dstTopRightBlack = new Rect();
//
//        dstTopLeftBlack.left = 0;
//        dstTopLeftBlack.top = 0;
//        dstTopLeftBlack.right = mSelectionStart;
//        dstTopLeftBlack.bottom = 0 + measuredHeight;
//
//        dstTopRightBlack.left = mSelectionEnd;
//        dstTopRightBlack.top = 0;
//        dstTopRightBlack.right = measuredWidth;
//        dstTopRightBlack.bottom = 0 + measuredHeight;
//
//        Bitmap wfTopFirstBlackBg = BitmapFactory.decodeResource(getResources(),
//                R.drawable.musicmake_waveform_top_black);
//
//        Rect srcTopBlack = new Rect();
//        srcTopBlack.left = 0;
//        srcTopBlack.top = 0;
//        srcTopBlack.right = wfTopFirstBlackBg.getWidth();
//        srcTopBlack.bottom = wfTopFirstBlackBg.getHeight();
//
//        Paint paintTopBlack = new Paint();
//        paintTopBlack.setAlpha(0xff); // 设置透明程度
//        canvas.drawBitmap(wfTopFirstBlackBg, srcTopBlack, dstTopLeftBlack, paintTopBlack);
//        canvas.drawBitmap(wfTopFirstBlackBg, srcTopBlack, dstTopRightBlack, paintTopBlack);

        // 画顶层白色
        // Rect srcTopWhite =
        // this.getResources().getDrawable(R.drawable.musicmake_waveform_top_white)
        // .getBounds();// 图片 >>原矩形
//        Rect dstTopWhite = new Rect();// 屏幕 >>目标矩形
//
//        dstTopWhite.left = mSelectionStart;
//        dstTopWhite.top = 0;
//        dstTopWhite.right = mSelectionEnd;
//        dstTopWhite.bottom = 0 + measuredHeight;
//        Bitmap wfTopWhiteBg = BitmapFactory.decodeResource(getResources(),
//                R.drawable.musicmake_waveform_top_white);
//
//        Rect srcTopWhite = new Rect();
//        srcTopWhite.left = 0;
//        srcTopWhite.top = 0;
//        srcTopWhite.right = wfTopWhiteBg.getWidth();
//        srcTopWhite.bottom = wfTopWhiteBg.getHeight();
//
//        canvas.drawBitmap(wfTopWhiteBg, srcTopWhite, dstTopWhite, null);

        // 画播放线条
        for (i = 0; i < width + mLeftOffsetPix; i++) {
            if (i + start == mPlaybackPos) {
                // canvas.drawLine(i, 0, i, measuredHeight, mPlaybackLinePaint);
                // // 画播放线条

                Paint playPaint = new Paint();
                playPaint.setColor(getResources().getColor(R.color.skin_primary_text));
                playPaint.setStyle(Paint.Style.FILL);
                Rect playRect = new Rect();
                playRect.left = i;
                playRect.top = 0;
                playRect.right = i + 3;
                playRect.bottom = lastLineY;

                canvas.drawRect(playRect, playPaint);

//                Rect dstPlayback = new Rect();// 屏幕 >>目标矩形
//
//                dstPlayback.left = i;
//                dstPlayback.top = 0;
//                dstPlayback.right = i + 3;
//                dstPlayback.bottom = 0 + measuredHeight;
//                Bitmap wfPlayback = BitmapFactory.decodeResource(getResources(),
//                        R.drawable.music_make_player_line);
//                Rect srcPlayback = new Rect();
//                srcPlayback.left = 0;
//                srcPlayback.top = 0;
//                srcPlayback.right = wfPlayback.getWidth();
//                srcPlayback.bottom = wfPlayback.getHeight();
//
//                canvas.drawBitmap(wfPlayback, srcPlayback, dstPlayback, null);
            }
        }

        // Draw borders
        Paint mBorderLinePaint = new Paint();
        // mBorderLinePaint.setAntiAlias(false);
        mBorderLinePaint.setColor(getResources().getColor(R.color.waveform_border_line));
        drawWaveformYLine(canvas, mSelectionStart - start, 0, measuredHeight, mBorderLinePaint);
        drawWaveformYLine(canvas, mSelectionEnd - start, 0, measuredHeight, mBorderLinePaint);
        // Draw timecode, 时间轴均分成3份，再显示两个时间刻度
//        double timecodeIntervalSecs = (width * onePixelInSecs) / 3;
        // Draw grid
        fractionalSecs = mOffset * onePixelInSecs;
//        int integerTimecode = (int) (fractionalSecs / timecodeIntervalSecs);
        i = 0;
        float startOffset = (float) (0.5 * mTimecodePaint.measureText("0:00"));
//        canvas.drawText("0:00", mLeftOffsetPix - startOffset, ((float)measuredHeight - 10*mDensity), mTimecodePaint);
        while (i < width) {
            i++;
            fractionalSecs += onePixelInSecs;
            integerSecs = (int) fractionalSecs;
//            int integerTimecodeNew = (int) (fractionalSecs / timecodeIntervalSecs);
            // if (integerTimecodeNew != integerTimecode) {
            // integerTimecode = integerTimecodeNew;
            // // Turn, e.g. 67 seconds into "1:07"
            // String timecodeMinutes = "" + (integerSecs / 60);
            // String timecodeSeconds = "" + (integerSecs % 60);
            // if ((integerSecs % 60) < 10) {
            // timecodeSeconds = "0" + timecodeSeconds;
            // }
            // String timecodeStr = timecodeMinutes + ":" + timecodeSeconds;
            // float offset = (float) (0.5 *
            // mTimecodePaint.measureText(timecodeStr));
            // // 画时间
            // canvas.drawText(timecodeStr, i - offset + mLeftOffsetPix, (int)
            // (12 * mDensity),
            // mTimecodePaint);
            // }
        }
        int nTotalSeconds = (int) pixelsToSeconds(width);
        String totalMinutes = "" + (integerSecs / 60);
        String totalSeconds = "" + (integerSecs % 60);
        if ((nTotalSeconds % 60) < 10) {
            totalSeconds = "0" + totalSeconds;
        }
        String totalTimecodeStr = totalMinutes + ":" + totalSeconds;
        float totalOffset = (float) (0.5 * mTimecodePaint.measureText(totalTimecodeStr));
//        canvas.drawText(totalTimecodeStr, i - totalOffset + mLeftOffsetPix, ((float)measuredHeight - 10*mDensity),
//                mTimecodePaint);
        //

        if (mListener != null) {
            mListener.waveformDraw();
        }
    }

    /**
     * Called once when a new sound file is added
     */
    private void computeDoublesForAllZoomLevels() {
        int numFrames = mSoundFile.getNumFrames();
        int[] frameGains = mSoundFile.getFrameGains();
        double[] smoothedGains = new double[numFrames];
        if (numFrames == 1) {
            smoothedGains[0] = frameGains[0];
        } else if (numFrames == 2) {
            smoothedGains[0] = frameGains[0];
            smoothedGains[1] = frameGains[1];
        } else if (numFrames > 2) {
            smoothedGains[0] = (double) ((frameGains[0] / 2.0) + (frameGains[1] / 2.0));
            for (int i = 1; i < numFrames - 1; i++) {
                smoothedGains[i] = (double) ((frameGains[i - 1] / 3.0) + (frameGains[i] / 3.0) + (frameGains[i + 1] / 3.0));
            }
            smoothedGains[numFrames - 1] = (double) ((frameGains[numFrames - 2] / 2.0) + (frameGains[numFrames - 1] / 2.0));
        } else {
            // 小于等于0的情况是频谱解析错误的情况，在上层已屏蔽入口
            return;
        }

        // 需要压缩numFrames跟手机屏幕频宽一致（根据numFrames与频宽像素值的比率，获取频谱的平均值
        mLeftOffsetPix = SystemUtils.dip2px(mContext, mOffsetLeftDip);
        mRightOffsetPix = SystemUtils.dip2px(mContext, mOffsetRightDip);

        int numWidthPixFrames = getMeasuredWidth();
        if (numWidthPixFrames == 0)
            numWidthPixFrames = SystemUtils.getDisplayWidth(mContext);
        numWidthPixFrames = numWidthPixFrames - mLeftOffsetPix - mRightOffsetPix;

        mWidthCompressRate = (double) numFrames / numWidthPixFrames;
//        if (mWidthCompressRate <= 1) {
//            numWidthPixFrames = numFrames;
//        }
        double[] smoothedWidthGains = new double[numWidthPixFrames];
        for (int i = 0; i < numWidthPixFrames; i++) {
            // 取原频谱的均值
            double sumGain = 0;
            int j = 0;
            int nBei = (int) (i * mWidthCompressRate);
            for (j = 0; j < mWidthCompressRate; j++) {
                sumGain += smoothedGains[j + nBei];
            }
            if (j > 0) {
                smoothedWidthGains[i] = sumGain / j;
            } else {
                smoothedWidthGains[i] = smoothedGains[i];
            }
        }
        numFrames = numWidthPixFrames;
        smoothedGains = smoothedWidthGains;

        // end 压缩

        // Make sure the range is no more than 0 - 255
        double maxGain = 1.0;
        for (int i = 0; i < numFrames; i++) {
            if (smoothedGains[i] > maxGain) {
                maxGain = smoothedGains[i];
            }
        }
        double scaleFactor = 1.0;
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain;
        }

        // Build histogram of 256 bins and figure out the new scaled max //
        // gainHist表示某个波值的统计次数
        maxGain = 0;
        int gainHist[] = new int[256];
        for (int i = 0; i < numFrames; i++) {
            int smoothedGain = (int) (smoothedGains[i] * scaleFactor);
            if (smoothedGain < 0)
                smoothedGain = 0;
            if (smoothedGain > 255)
                smoothedGain = 255;

            if (smoothedGain > maxGain)
                maxGain = smoothedGain;

            gainHist[smoothedGain]++;
        }

        // Re-calibrate the min to be 5%
        double minGain = 0;
        int sum = 0;
        while (minGain < 255 && sum < numFrames / 20) {
            sum += gainHist[(int) minGain];
            minGain++;
        }

        // Re-calibrate the max to be 99%
        sum = 0;
        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[(int) maxGain];
            maxGain--;
        }

        // Compute the heights
        double[] heights = new double[numFrames];
        double range = maxGain - minGain;
        for (int i = 0; i < numFrames; i++) {
            double value = (smoothedGains[i] * scaleFactor - minGain) / range;
            if (value < 0.0)
                value = 0.0;
            if (value > 1.0)
                value = 1.0;
            heights[i] = value * value;
        }

        mNumZoomLevels = 5;
        mLenByZoomLevel = new int[5];
        mZoomFactorByZoomLevel = new double[5];
        mValuesByZoomLevel = new double[5][];

        // Level 0 is doubled, with interpolated values
        mLenByZoomLevel[0] = numFrames * 2;
        mZoomFactorByZoomLevel[0] = 2.0;
        mValuesByZoomLevel[0] = new double[mLenByZoomLevel[0]];
        if (numFrames > 0) {
            mValuesByZoomLevel[0][0] = 0.5 * heights[0];
            mValuesByZoomLevel[0][1] = heights[0];
        }
        for (int i = 1; i < numFrames; i++) {
            mValuesByZoomLevel[0][2 * i] = 0.5 * (heights[i - 1] + heights[i]);
            mValuesByZoomLevel[0][2 * i + 1] = heights[i];
        }

        // Level 1 is normal
        mLenByZoomLevel[1] = numFrames;
        mValuesByZoomLevel[1] = new double[mLenByZoomLevel[1]];
        mZoomFactorByZoomLevel[1] = 1.0;
        for (int i = 0; i < mLenByZoomLevel[1]; i++) {
            mValuesByZoomLevel[1][i] = heights[i];
        }

        // 3 more levels are each halved
        for (int j = 2; j < 5; j++) {
            mLenByZoomLevel[j] = mLenByZoomLevel[j - 1] / 2;
            mValuesByZoomLevel[j] = new double[mLenByZoomLevel[j]];
            mZoomFactorByZoomLevel[j] = mZoomFactorByZoomLevel[j - 1] / 2.0;
            for (int i = 0; i < mLenByZoomLevel[j]; i++) {
                mValuesByZoomLevel[j][i] = 0.5 * (mValuesByZoomLevel[j - 1][2 * i] + mValuesByZoomLevel[j - 1][2 * i + 1]);
            }
        }

        mZoomLevel = 1;

        // if (numFrames > 5000) {
        // mZoomLevel = 3;
        // } else if (numFrames > 1000) {
        // mZoomLevel = 2;
        // } else if (numFrames > 300) {
        // mZoomLevel = 1;
        // } else {
        // mZoomLevel = 0;
        // }

        mInitialized = true;
    }

    /**
     * Called the first time we need to draw when the zoom level has changed or
     * the screen is resized
     */
    private void computeIntsForThisZoomLevel() {
        int halfHeight = (getMeasuredHeight() / 2) - 1;
        mHeightsAtThisZoomLevel = new int[mLenByZoomLevel[mZoomLevel]];
        for (int i = 0; i < mLenByZoomLevel[mZoomLevel]; i++) {
            mHeightsAtThisZoomLevel[i] = (int) (mValuesByZoomLevel[mZoomLevel][i] * halfHeight);
        }
    }

    /**
     * 自动截取高潮
     */
    public AutoCatchPeak onAutoCatch() {
        int nX = mLenByZoomLevel[1];
        // 由于有些频谱是给处理过的，振幅很小，如果是按照一定固定量递减的话，有些歌曲取样取不到准确的，因此需要做些处理
        // Sample的区间应该在振幅max和avg之间，变量区间是(max-avg)/20;
        int nXNums = mLenByZoomLevel[1]; // x的值(0到【屏幕宽-偏差】）
        double[] YValues = mValuesByZoomLevel[1];

        //
        double dSampleMax = 0;
        double dSampleAvg = 0;
        double dSampleSum = 0;
        for (int i = 0; i < nXNums; i++) {
            dSampleSum += YValues[i];
            if (YValues[i] > dSampleMax) {
                dSampleMax = YValues[i];
            }
        }
        dSampleAvg = dSampleSum / nXNums;
        double dDtSample = dSampleAvg / 20; // 每次的取样变化幅度
        double sample = dSampleAvg + dDtSample * 19;

        AutoCatchPeak mdlMaxPeak = CatchSongBySample(sample);
        double dCatchRate = (double) mdlMaxPeak.getXWidth() / nX;
        while (dCatchRate < 0.2 && sample > 0) {
            sample -= dDtSample;
            mdlMaxPeak = CatchSongBySample(sample);
            dCatchRate = (double) mdlMaxPeak.getXWidth() / nX;
        }

        return mdlMaxPeak;
    }

    /**
     * 通过波峰的采样值进行截取
     * 
     * @param sample 波峰的值大小
     * @return
     */
    private AutoCatchPeak CatchSongBySample(double sample) {
        if (sample <= 0) {
            AutoCatchPeak badAutoCatch = new AutoCatchPeak();
            badAutoCatch.setXLeft(0);
            badAutoCatch.setXRight(0);
            badAutoCatch.setXValue(0);
            badAutoCatch.setXWidth(0);
            return badAutoCatch;
        }
        // 初始化
        int nX = mLenByZoomLevel[1]; // x的值(0到【屏幕宽-偏差】）
        double[] YValues = mValuesByZoomLevel[1];
        // 1 首先选定一个默认的压缩率（压缩率越大，越容易找到高潮部分，但是也会越不准确）
        double dXZipRate = 1;
        AutoCatchPeak mdlMaxPeak = ZipSong2Catch(dXZipRate, YValues, nX, sample);
        // 5 判断当前的时间区间(x_width)是否合理，不合理的地方（时间值太短）需要跳转到步骤1，增大压缩比，重新进行计算(2倍压缩比)
        double dCatchRate = (double) mdlMaxPeak.getXWidth() / nX;
        while (dCatchRate < 0.2 && dXZipRate < 513 && mdlMaxPeak.getNumPeaks() > 3) {
            // 0.2 表示截取的长度要大于歌曲的20%;257 表示压缩比要小于256; 3表示波峰样本要大于3
            dXZipRate *= 2;
            mdlMaxPeak = ZipSong2Catch(dXZipRate, YValues, nX, sample);
            dCatchRate = (double) mdlMaxPeak.getXWidth() / nX;
        }
        // 6 根据max(x_width)的值定位时间轴
        return mdlMaxPeak;
    }

    /**
     * 压缩频谱，找到一定压缩比的高潮部分
     * 
     * @param dXZipRate
     * @param dY
     * @param nX
     * @return
     */
    private AutoCatchPeak ZipSong2Catch(double dXZipRate, double[] dY, int nX, double sampleMax) {
        // 按照压缩比进行压缩
        int nXNums = (int) (nX / dXZipRate);
        double[] YValues = new double[nXNums]; // y的值
        double dYSum = 0; // y值总数
        for (int i = 0; i < nXNums; i++) {
            // 压缩原频谱的值
            double sumGain = 0;
            int j = 0;
            int nBei = (int) (i * dXZipRate);
            for (j = 0; j < dXZipRate; j++) {
                sumGain += dY[j + nBei];
            }
            if (j > 0) {
                YValues[i] = sumGain / j;
                dYSum += sumGain / j;
            } else {
                YValues[i] = dY[i];
                dYSum += dY[i];
            }
            // end 压缩原频谱的值
        }
        double dYAverage = dYSum / nXNums; // y值平均数
        // 2 根据压缩率筛选出合适的波峰样本
        int maxYPoints[] = new int[nXNums]; // 波峰数组，值对应x
        int maxPointNums = 0; // 波峰样本数量
        for (int i = 0; i < nXNums; i++) {
            if (Double.compare(sampleMax, YValues[i]) < 0) { // 0.1为偏差量，可调节
                maxYPoints[maxPointNums] = i;
                maxPointNums++;
            }
        }

        // 3 计算波峰附近大于频谱平均值的宽度x_width
        // 4 帅选出max(x_width)
        int maxWidth = 0;
        AutoCatchPeak mdlMaxPeak = new AutoCatchPeak();
        for (int i = 0; i < maxPointNums; i++) {
            AutoCatchPeak mdlPeak = new AutoCatchPeak();
            mdlPeak.setNumPeaks(maxPointNums);
            int xValue = maxYPoints[i];
            mdlPeak.setXValue(xValue);
            // 计算比x小且大于平均值的目标
            int xLeft = computeMinLargeAvg(YValues, dYAverage, xValue);
            mdlPeak.setXLeft(xLeft);
            // 计算比x大且大于平均值的目标
            int xRight = computeMaxLargeAvg(YValues, dYAverage, xValue);
            mdlPeak.setXRight(xRight);
            // 计算x_width
            int xWidth = xRight - xLeft + 1;
            if (xWidth > maxWidth) {
                maxWidth = xWidth;
                mdlMaxPeak = mdlPeak;
            }
            mdlPeak.setXWidth(xWidth);
        }
        // 将返回原倍数的值
        mdlMaxPeak.ChangeToZipRate(dXZipRate);
        return mdlMaxPeak;
    }

    /**
     * 计算最小大于平均数的坐标
     * 
     * @param YValues
     * @param dYAverage
     * @param xValue
     * @return
     */
    private int computeMinLargeAvg(double[] YValues, double dYAverage, int xValue) {
        for (int i = xValue; i >= 0; i--) {
            if (Double.compare(YValues[i], dYAverage) < 0) {
                return i + 1;
            }
        }
        return xValue;
    }

    /**
     * 计算最大大于平均数的坐标
     * 
     * @param YValues
     * @param dYAverage
     * @param xValue
     * @return
     */
    private int computeMaxLargeAvg(double[] YValues, double dYAverage, int xValue) {
        for (int i = xValue; i < YValues.length; i++) {
            if (Double.compare(YValues[i], dYAverage) < 0) {
                return i - 1;
            }
        }
        return xValue;
    }

}
