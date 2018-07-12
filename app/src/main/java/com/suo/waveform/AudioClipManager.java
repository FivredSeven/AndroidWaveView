package com.suo.waveform;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.suo.waveform.clip.AutoCatchPeak;
import com.suo.waveform.clip.CheapSoundFile;
import com.suo.waveform.entity.MusicEntity;
import com.suo.waveform.util.CommonThreadPool;
import com.suo.waveform.util.IOUtils;
import com.suo.waveform.util.StorageUtils;
import com.suo.waveform.util.SystemUtils;
import com.suo.waveform.util.TimeShowUtils;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;

/**
 * 音频剪辑管理类
 * 可自定义频谱和选择器
 * 支持剪辑状态回调
 */
public class AudioClipManager implements WaveformView.WaveformListener, MarkerView.MarkerListener{
    private Activity mActivity;
    private ImageView mPlayPause;
    private int mPlayIconResource;
    private int mPauseIconResource;



    private WaveformView mWaveformView;
    private MarkerView mEndMarker, mStartMarker;
    private ImageView mStartMarkerImage, mEndMarkerImage;
    private View mStartMarkerLine, mEndMarkerLine;
    private TextView mTextView_start, mTextView_stop;
    private TextView mTotalTime;
    private TextView mSelectTime;

    private File mFile;
    private MediaPlayer mPlayer;
    private long mSongDuration;
    private CheapSoundFile mSoundFile;

    private String mExtension;

    private float mDensity;
    private boolean mIsPlaying = false;
    private boolean mTouchDragging;
    private float mTouchMarkStart;
    private float mTouchMarkEnd;
    private int mTouchInitialOffset;
    private int mFlingVelocity;
    private long mWaveformTouchStartMsec;
    private int mOffset;
    private int mOffsetGoal;
    private int mPlayStartMsec;
    private int mPlayEndMsec;
    private int mPlayStartOffset;

    private int mTouchInitialStartPos;
    private int mTouchInitialEndPos;
    public boolean bAutoCatchSonged = true;
    private boolean mTouchOnlyStart = true;
    private boolean canReuseMakedRing = false;
    private int mSongWaveFormPix;

    private int mWidth;
    private boolean mKeyDown;
    private String mTitle;

    private boolean hasClickPlayBtn = false;

    private int mMaxPos;
    private int mStartPos;
    private int mEndPos;
    private int mLastDisplayedStartPos;
    private int mLastDisplayedEndPos;
    private int mDuration;
    public boolean isIn = false, isOut = false;
    private Handler mHandler;

    private final SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");

    public AudioClipManager(Activity activity) {
        mActivity = activity;
        mHandler = new Handler();
        initViews();
    }

    /**
     * 初始化view
     */
    private void initViews() {
        // 剪辑时，前面节点上显示的歌曲时间点
        mTextView_start = (TextView) mActivity.findViewById(R.id.mTextView_start);
        // 剪辑时，后面节点上显示的歌曲时间点
        mTextView_stop = (TextView) mActivity.findViewById(R.id.mTextView_stop);
        // 歌曲总时长
        mTotalTime = (TextView) mActivity.findViewById(R.id.csv_end_time);
        // 剪辑时，选中部分的时长
        mSelectTime = (TextView) mActivity.findViewById(R.id.csv_select_time);
        // 频谱布局
        mWaveformView = (WaveformView) mActivity.findViewById(R.id.waveform);
        // 用于剪辑音频范围的滑动控件
        mStartMarker = (MarkerView) mActivity.findViewById(R.id.startmarker);
        mEndMarker = (MarkerView) mActivity.findViewById(R.id.endmarker);
        // 滑动控件的图片
        mStartMarkerImage = (ImageView) mActivity.findViewById(R.id.startmarker_stroke);
        mEndMarkerImage = (ImageView) mActivity.findViewById(R.id.endmarker_stroke);
        // 滑动控件的线
        mStartMarkerLine = mActivity.findViewById(R.id.stroke_start_line);
        mEndMarkerLine = mActivity.findViewById(R.id.stroke_end_line);
    }

    /**
     * 自定义频谱样式
     * 待补充
     */
    public void customWaveformView() {

    }

    /**
     * 自定义滑动控件的图片
     * @param drawable
     */
    public void setMarkerImageBackground(Drawable drawable) {
        if (drawable == null) {
            mStartMarkerImage.setVisibility(View.INVISIBLE);
            mEndMarkerImage.setVisibility(View.INVISIBLE);
            return;
        }
        mStartMarkerImage.setBackgroundDrawable(drawable);
        mEndMarkerImage.setBackgroundDrawable(drawable);
    }

    /**
     * 自定义滑动控件的控制线
     * @param color
     */
    public void setMarkerLineColor(int color) {
        if (color <= 0) {
            mStartMarkerLine.setVisibility(View.INVISIBLE);
            mEndMarkerLine.setVisibility(View.INVISIBLE);
            return;
        }
        mStartMarkerLine.setBackgroundColor(mActivity.getResources().getColor(color));
        mEndMarkerLine.setBackgroundColor(mActivity.getResources().getColor(color));
    }

    /**
     * 设置可控制播放暂停的按钮
     * @param btn
     * @param playIcon 播放图标 暂停时使用
     * @param pauseIcon 暂停图标 播放时使用
     */
    public void setPlayOrPauseButton(ImageView btn, @DrawableRes int playIcon, @DrawableRes int pauseIcon) {
        mPlayPause = btn;
        mPlayIconResource = playIcon;
        mPauseIconResource = pauseIcon;

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 播放或者暂停歌曲
                player();
                hasClickPlayBtn = !hasClickPlayBtn;
            }
        });
    }

    /**
     * 支持传入MusicEntity
     * @param entity
     */
    public void setMusicEntity(MusicEntity entity) {
        if (TextUtils.isEmpty(entity.path)) {
            return;
        }
        mFile = new File(entity.path);
        if (!mFile.exists()) {
            return;
        }
        String time = TimeShowUtils.getDurationFromTime(entity.duration);
        if (mTotalTime != null) {
            mTotalTime.setText(time);
        }
        mTitle = entity.displayname;
        prepare();
    }

    /**
     * 支持传入音乐具体内容
     * @param path
     * @param duration
     * @param displayName
     */
    public void setMusicEntity(String path, int duration, String displayName) {
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mFile = new File(path);
        if (!mFile.exists()) {
            return;
        }
        String time = TimeShowUtils.getDurationFromTime(duration);
        if (mTotalTime != null) {
            mTotalTime.setText(time);
        }
        mTitle = displayName;
        prepare();
    }

    /**
     * 给外部调用，保存当前剪辑音频
     */
    public void save() {
        if (isClickable()) {
            if (listener != null) {
                listener.fail("铃声小于1秒，保存失败");
            }
            return;
        }
        if (mIsPlaying) {
            handlePause();
        }
        String outPath = makeAudioFilename(mTitle, mExtension);
        saveAudio(outPath, mTitle, true);
    }

    /**
     * 离开页面，释放播放器
     */
    public void release() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        mPlayer = null;

        mActivity = null;

        mHandler.removeCallbacks(mTimerRunnable);
        mHandler.removeCallbacks(displayRunnable);
    }

    /**
     * 监听物理按键，支持空格键处理播放
     * @param keyCode
     * @param event
     * @return
     */
    public boolean handleKeyEvent(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            onPlay(mStartPos);
            return true;
        }
        return false;
    }

    /**
     * 设置监听器，通知业务层剪辑音频文件成功或失败
     */
    public AudioClipListener listener;
    public interface AudioClipListener {
        void success(String path);
        void fail(String message);
    }
    public void setAudioClipListener(AudioClipListener listener) {
        this.listener = listener;
    }







    /****************************** 以下为音频剪辑私有方法 ******************************/

    /**
     * 准备工作包括初始化播放和加载音频文件
     */
    private void prepare() {
        initPlayerManager();
        loadFile();
    }

    /**
     * 初始化播放器，这里直接用sdk里的MediaPlayer
     */
    private void initPlayerManager() {
        CommonThreadPool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaPlayer player = new MediaPlayer();
                    player.setDataSource(mFile.getAbsolutePath());
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.prepare();
                    mPlayer = player;
                    mSongDuration = mPlayer.getDuration();
                } catch (final java.io.IOException e) {
                }
            }
        });
    }

    /**
     * 判断音频文件可用性，可用则加载
     */
    private void loadFile() {
        try {
            mSoundFile = CheapSoundFile.create(mFile.getAbsolutePath(), null);
            if (mSoundFile == null || mSoundFile.getNumFrames() <= 0) {
                String name = mFile.getName().toLowerCase();
                String[] components = name.split("\\.");
                String err;
                if (components.length < 2) {
                    err = "该文件格式不支持,请重新选择";
                } else if (mSoundFile.getNumFrames() <= 0) {
                    err = "文件频谱解析失败，请重新选择";
                } else {
                    err = "暂不支持" + components[components.length - 1] + "格式,请重新选择";
                }
                if (listener != null) {
                    listener.fail(err);
                }
                return;
            }
        } catch (final Exception e) {
            // e.printStackTrace();
            if (listener != null) {
                listener.fail("加载异常");
            }
            return;
        }
        mExtension = getExtensionFromFilename(mFile.getAbsolutePath());
        // load success
        loadGui();
        finishOpeningSoundFile();
    }

    /**
     * 真正的音频文件频谱解析并展示
     */
    private void loadGui() {
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;

        enableDisableButtons();

        mWaveformView.setListener(this);

        mMaxPos = 0;
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        if (mSoundFile != null) {
            mWaveformView.setSoundFile(mSoundFile);
            mWaveformView.recomputeHeights(mDensity);
            mMaxPos = mWaveformView.maxPos();
        }


        mStartMarker.setListener(this);
        mStartMarker.setFocusable(true);
        mStartMarker.setFocusableInTouchMode(true);


        mEndMarker.setListener(this);
        mEndMarker.setFocusable(true);
        mEndMarker.setFocusableInTouchMode(true);
        updateDisplay();
    }

    /**
     * 打开音频文件结束
     */
    private void finishOpeningSoundFile() {
        mWaveformView.setSoundFile(mSoundFile);
        mWaveformView.recomputeHeights(mDensity);

        mMaxPos = mWaveformView.maxPos();
        mSongWaveFormPix = SystemUtils.getScreenWidth(mActivity) - 2 * mWaveformView.mLeftOffsetPix;
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        mTouchDragging = false;

        mOffset = 0;
        mOffsetGoal = 0;
        mFlingVelocity = 0;
        onAutoCatch();
        updateDisplay();


        mHandler.post(mTimerRunnable);
        mStartMarker.requestFocus();
    }

    /**
     * 自动截取高潮
     */
    private void onAutoCatch() {

        // 计算出自动截取高潮的开始和结束节点
        AutoCatchPeak mdlMaxPeak = mWaveformView.onAutoCatch();
        // 设置时间轴的位置
        mStartPos = mdlMaxPeak.getXLeft() + mWaveformView.mLeftOffsetPix;
        mEndPos = mdlMaxPeak.getXRight() + mWaveformView.mLeftOffsetPix;
        hasClickPlayBtn = false;
        handleStop();
        setOffsetGoalEnd();
        canReuseMakedRing = false;
    }

    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            // Updating an EditText is slow on Android. Make sure
            // we only do the update if the text has actually changed.
            if (mStartPos != mLastDisplayedStartPos && mTextView_start != null && !mTextView_start.hasFocus()) {
                mTextView_start.setText(formatter.format(formatTime(mStartPos
                        - mWaveformView.mLeftOffsetPix) * 1000)); // 起始点
                mLastDisplayedStartPos = mStartPos;
            }
            if (mEndPos != mLastDisplayedEndPos && mTextView_stop != null && !mTextView_stop.hasFocus()) {
                mTextView_stop.setText(formatter.format(formatTime(mEndPos
                        - mWaveformView.mLeftOffsetPix) * 1000)); // 结束点
                mLastDisplayedEndPos = mEndPos;
            }
            String cutTime = formateMinSecsTime(formatTime(mEndPos - mStartPos));
            if (mSelectTime != null) {
                mSelectTime.setText(cutTime);
            }
            mHandler.postDelayed(mTimerRunnable, 100);
        }
    };

    /**
     * @param dSeconds
     * @return
     */
    private String formateMinSecsTime(double dSeconds) {
        if (dSeconds < 0) {
            return "";
        }
        int integerSecs = (int) Math.ceil(dSeconds);

        mDuration = integerSecs;
        String strOutput = "";
        if (bAutoCatchSonged) {
            strOutput += "已智能选取" + integerSecs + "秒";
        } else {
            strOutput += "已选取" + integerSecs + "秒";
        }

        return strOutput;
    }

    /**
     * 给播放按钮设置播放和暂停icon
     */
    private void enableDisableButtons() {
        if (mPlayPause == null) {
            // 若没有播放按钮则不需要替换图标
            return;
        }
        if (mIsPlaying) {
            mPlayPause.setImageResource(mPauseIconResource);
        } else {
            mPlayPause.setImageResource(mPlayIconResource);
        }
    }

    @Override
    public void waveformTouchStart(float x) {
        mTouchDragging = true;
        mTouchMarkStart = x;
        mTouchInitialOffset = mOffset;
        mFlingVelocity = 0;
        mWaveformTouchStartMsec = System.currentTimeMillis();
    }

    @Override
    public void waveformTouchMove(float x) {
        // mOffset = trap((int) (mTouchInitialOffset + (mTouchStart - x)));
        // updateDisplay();
    }

    @Override
    public void waveformTouchEnd() {
        mTouchDragging = false;
        mOffsetGoal = mOffset;
        if (mTouchMarkStart < mWaveformView.mLeftOffsetPix) {
            return;
        }
        long elapsedMsec = System.currentTimeMillis() - mWaveformTouchStartMsec;
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                int seekMsec = mWaveformView
                        .pixelsToMillisecs((int) (mTouchMarkStart + mOffset - mWaveformView.mLeftOffsetPix));
                if (seekMsec >= mPlayStartMsec && seekMsec < mPlayEndMsec) {
                    mPlayer.seekTo(seekMsec - mPlayStartOffset);
                } else {
                    handleStop();
                }
            } else {
                mWaveformView.setPlayback(-1);
                onPlay((int) (mTouchMarkStart + mOffset));
            }
        }
    }

    @Override
    public void waveformFling(float x) {
        mTouchDragging = false;
        // mOffsetGoal = mOffset;
        // mFlingVelocity = (int) (-vx);
        mOffsetGoal = 0;
        updateDisplay();
    }

    @Override
    public void waveformDraw() {
        mWidth = mWaveformView.getMeasuredWidth();
        mOffsetGoal = mOffset = 0; // 此版本不允许滑动
        if (mOffsetGoal != mOffset && !mKeyDown)
            updateDisplay();
        else if (mIsPlaying) {
            updateDisplay();
        } else if (mFlingVelocity != 0) {
            updateDisplay();
        }
    }

    /**
     * 停止
     */
    private synchronized void handleStop() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        mWaveformView.setPlayback(-1);
        mIsPlaying = false;
        updateDisplay();
        enableDisableButtons();
    }

    /**
     * 暂停
     */
    private synchronized void handlePause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        mIsPlaying = false;
        updateDisplay();
        enableDisableButtons();
    }

    /**
     * 更新裁剪区域
     */
    private synchronized void updateDisplay() {
        if (mIsPlaying) {
            int now = mPlayer == null?0:mPlayer.getCurrentPosition() + mPlayStartOffset;
            int frames = (int) (mWaveformView.millisecsToPixels(now))
                    + mWaveformView.mLeftOffsetPix;
            mWaveformView.setPlayback(frames);
            // setOffsetGoalNoUpdate(frames - mWidth / 2);
            if (now >= mPlayEndMsec) {
                mWaveformView.setPlayback(-1);
                handlePause();
            }
        }

        mWaveformView.setParameters(mStartPos, mEndPos, mOffset);
        mWaveformView.invalidate();
        mStartMarker.setContentDescription("【开始标记】 "
                + formatTime(mStartPos));
        mEndMarker.setContentDescription("【结束标记 "
                + formatTime(mEndPos));
        int startX = mStartPos - mOffset - mStartMarker.getMeasuredWidth() / 2;
        int endX = mEndPos - mOffset - mEndMarker.getMeasuredWidth() / 2;

        RelativeLayout.LayoutParams start = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        start.leftMargin = startX;
        mStartMarker.setLayoutParams(start);

        RelativeLayout.LayoutParams end = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        end.leftMargin = endX;
        mEndMarker.setLayoutParams(end);

        if (mTextView_start != null) {
            // 定位提示文字的高度
            int nTextViewY = mWaveformView.getMeasuredHeight() / 2 + SystemUtils.dip2px(mActivity, 12);
            RelativeLayout.LayoutParams textViewStartY = (RelativeLayout.LayoutParams) mTextView_start
                    .getLayoutParams();
            textViewStartY.bottomMargin = nTextViewY;
            mTextView_start.setLayoutParams(textViewStartY);

            RelativeLayout.LayoutParams textViewEndY = (RelativeLayout.LayoutParams) mTextView_stop
                    .getLayoutParams();
            textViewEndY.bottomMargin = nTextViewY;
            mTextView_stop.setLayoutParams(textViewEndY);
        }

        // 为了处理MarkerView在末尾重叠的时候，不能拖动StartMarker的问题，当两个MarkerView在最右边时，隐藏EndMarker，使StartMarker可以拖动
        if (isClickable()) {
            mEndMarker.setVisibility(View.INVISIBLE);
        } else {
            mEndMarker.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 播放剪辑选中区域
     * @param startPosition
     */
    private synchronized void onPlay(int startPosition) {

        if (mIsPlaying) {
            handlePause();
            return;
        }
        if (mPlayer == null) {
            // Not initialized yet
            return;
        }
        // 停止其他播放
        // todo 这里需要关注是否有其他播放要先停止
        try {
            if (mWaveformView.getPlayback() > 0) {
                mPlayStartMsec = mWaveformView.pixelsToMillisecs(mWaveformView.getPlayback()
                        - mWaveformView.mLeftOffsetPix);
                startPosition = mWaveformView.getPlayback();
            } else {
                mPlayStartMsec = mWaveformView.pixelsToMillisecs(startPosition
                        - mWaveformView.mLeftOffsetPix);
            }
            if (startPosition < mStartPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mStartPos
                        - mWaveformView.mLeftOffsetPix);
            } else if (startPosition > mEndPos) {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mMaxPos);
            } else {
                mPlayEndMsec = mWaveformView.pixelsToMillisecs(mEndPos
                        - mWaveformView.mLeftOffsetPix);
            }
            mPlayStartOffset = 0;
            int startFrame = mWaveformView.secondsToFrames(mPlayStartMsec * 0.001);
            int endFrame = mWaveformView.secondsToFrames(mPlayEndMsec * 0.001);
            int startByte = mSoundFile.getSeekableFrameOffset(startFrame);
            int endByte = mSoundFile.getSeekableFrameOffset(endFrame);
            if (startByte >= 0 && endByte >= 0) {
                FileInputStream subsetInputStream = null;
                try {
                    mPlayer.reset();
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    subsetInputStream = new FileInputStream(mFile.getAbsolutePath());
                    mPlayer.setDataSource(subsetInputStream.getFD(), startByte, endByte - startByte);
                    mPlayer.prepare();
                    mPlayStartOffset = mPlayStartMsec;
                } catch (Exception e) {
                    mPlayer.reset();
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mPlayer.setDataSource(mFile.getAbsolutePath());
                    mPlayer.prepare();
                    mPlayStartOffset = 0;

                } finally {
                    IOUtils.closeQuietly(subsetInputStream);
                }
            }

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public synchronized void onCompletion(MediaPlayer arg0) {
                    handleStop();
                    hasClickPlayBtn = false;
                }
            });
            mIsPlaying = true;

            if (mPlayStartOffset == 0) {
                mPlayer.seekTo(mPlayStartMsec);
            }
            mPlayer.start();
            updateDisplay();
            enableDisableButtons();

        } catch (Exception e) {
            if (listener != null) {
                listener.fail("不能打开这种媒体文件");
            }
        }
    }

    /**
     * 保存最终剪辑的音频
     * @param outPath
     * @param title
     * @param save
     */
    private void saveAudio(final String outPath, final CharSequence title, final Boolean save) {
        if (!SystemUtils.checkSDCard()) {
            if (listener != null) {
                listener.fail("暂未发现SD卡");
            }
            return;
        }
        if (!SystemUtils.checkSize()) {
            if (listener != null) {
                listener.fail("SD卡容量不足");
            }
            return;
        }
        if (outPath == null) {
            if (listener != null) {
                listener.fail("无法找到文件名");
            }
            return;
        }
        double startTime = mWaveformView.pixelsToSeconds(mStartPos - mWaveformView.mLeftOffsetPix);
        double endTime = mWaveformView.pixelsToSeconds(mEndPos - mWaveformView.mLeftOffsetPix);
        final int startFrame = mWaveformView.secondsToFrames(startTime);
        final int endFrame = mWaveformView.secondsToFrames(endTime);
        final int duration = mDuration;
        CommonThreadPool.getInstance().execute(new Runnable(){
            @Override
            public void run() {
                final File outFile = new File(outPath);
                try {
                    if (isIn || isOut) {
                        mSoundFile.WriteFileTemp(outFile, startFrame, endFrame - startFrame, isIn,
                                isOut);
                    } else {
                        mSoundFile.WriteFile(outFile, startFrame, endFrame - startFrame);
                    }
                    CheapSoundFile cheapSoundFile = CheapSoundFile.create(outPath, null);
                    if (cheapSoundFile != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    // 保存剪辑的音频文件成功
                                    listener.success(outPath);
                                }
                            }
                        });
                    } else {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.fail("");
                                }
                            }
                        });

                    }
                } catch (final Exception e) {
                    if (e.getMessage() != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.fail(e.getMessage());
                                }
                            }
                        });
                    } else {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) {
                                    listener.fail("");
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void markerTouchStart(MarkerView marker, float x) {
        mTouchDragging = true;
        mTouchInitialStartPos = mStartPos;
        mTouchInitialEndPos = mEndPos;
        bAutoCatchSonged = false;
        if (marker == mStartMarker) {
            mTouchMarkStart = x;
            if (mTextView_start != null) {
                mTextView_start.setVisibility(View.VISIBLE);
            }
        } else {
            mTouchOnlyStart = false;
            mTouchMarkEnd = x;
            if (mTextView_stop != null) {
                mTextView_stop.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void markerTouchMove(MarkerView marker, float x) {
        float delta = 0;
        if (marker == mStartMarker) {
            if (x < mWaveformView.mLeftOffsetPix) {
                x = mWaveformView.mLeftOffsetPix;
            }
            delta = x - mTouchMarkStart;
            if ((mStartPos + delta) <= mWaveformView.mLeftOffsetPix) {
                // 不能直接返回，直接返回的话会造成无法快速滑动到最左边的情况
                mStartPos =  mWaveformView.mLeftOffsetPix;
            } else {
                mStartPos = trap((int) (mTouchInitialStartPos + delta));
            }
            if (mTextView_start != null) {
                mTextView_start.setVisibility(View.VISIBLE);
            }
        } else {
            delta = x - mTouchMarkEnd;
            if (mTextView_stop != null) {
                mTextView_stop.setVisibility(View.VISIBLE);
            }
            mEndPos = trap((int) (mTouchInitialEndPos + delta));
        }
        if (mEndPos < mStartPos)
            mEndPos = mStartPos;
        canReuseMakedRing = false;
        updateDisplay();
    }

    @Override
    public void markerTouchEnd(MarkerView marker) {
        mTouchDragging = false;
        if (marker == mStartMarker) {
            if (mTextView_start != null) {
                mTextView_start.setVisibility(View.INVISIBLE);
            }
            setOffsetGoalStart();
        } else {
            if (mTextView_stop != null) {
                mTextView_stop.setVisibility(View.INVISIBLE);
            }
            setOffsetGoalEnd();
            mTouchOnlyStart = true;
        }
        handleStop();
        if (hasClickPlayBtn){
            player();
        }
    }

    @Override
    public void markerFocus(MarkerView marker) {
        mKeyDown = false;
        if (marker == mStartMarker) {
            setOffsetGoalStartNoUpdate();
        } else {
            setOffsetGoalEndNoUpdate();
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler.postDelayed(displayRunnable, 100);
    }

    private final Runnable displayRunnable = new Runnable() {
        @Override
        public void run() {
            updateDisplay();
        }
    };

    @Override
    public void markerLeft(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos = trap(mStartPos - velocity);
            mEndPos = trap(mEndPos - (saveStart - mStartPos));
            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity);
                mEndPos = mStartPos;
            } else {
                mEndPos = trap(mEndPos - velocity);
            }

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    @Override
    public void markerRight(MarkerView marker, int velocity) {
        mKeyDown = true;
        if (marker == mStartMarker) {
            int saveStart = mStartPos;
            mStartPos += velocity;
            if (mStartPos > mMaxPos)
                mStartPos = mMaxPos;
            mEndPos += (mStartPos - saveStart);
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;
            setOffsetGoalStart();
        }

        if (marker == mEndMarker) {
            mEndPos += velocity;
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    @Override
    public void markerEnter(MarkerView marker) {

    }

    @Override
    public void markerKeyUp() {
        mKeyDown = false;
        updateDisplay();
    }

    @Override
    public void markerDraw() {

    }

    private double formatTime(int pixels) {
        try {
            if (mWaveformView != null && mWaveformView.isInitialized()) {
                double ds = mWaveformView.pixelsToSeconds(pixels);
                if (ds < 0) {
                    ds = 0;
                }
                String time = formatDecimal(Math.abs(ds)).replace("", "");
                return Double.parseDouble(time);
            } else {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String formatDecimal(double x) {
        int xWhole = (int) x;
        int xFrac = (int) (100 * (x - xWhole) + 0.5);
        if (xFrac >= 100) {
            xWhole++; // Round up
            xFrac -= 100; // Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10; // we need a fraction that is 2 digits long
            }
        }

        if (xFrac < 10)
            return xWhole + ".0" + xFrac;
        else
            return xWhole + "." + xFrac;
    }

    private long getMusicleght() {
        // 计算mp3文件的长度
        if (mSoundFile == null) {
            return 0;
        }
        if (mWaveformView == null) {
            return 0;
        }
        return mWaveformView.pixelsToMillisecs(mWaveformView.maxPos());
    }

    private Boolean isClickable() {
        if (mEndPos > mStartPos) {
            return false;
        }
        return true;
    }

    private int trap(int pos) {
        if (pos < mWaveformView.mLeftOffsetPix)
            return mWaveformView.mLeftOffsetPix;
        if (pos > mMaxPos + mWaveformView.mLeftOffsetPix)
            return mMaxPos + mWaveformView.mLeftOffsetPix;
        return pos;
    }

    private void setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2);
    }

    private void setOffsetGoal(int offset) {
        // setOffsetGoalNoUpdate(offset); // 不移动wavefrom
        updateDisplay();
    }

    private void setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2);
    }

    private void setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2);
    }

    private void setOffsetGoalNoUpdate(int offset) {
        if (mTouchDragging) {
            return;
        }

        mOffsetGoal = offset;
        if (mOffsetGoal + mWidth / 2 > mMaxPos)
            mOffsetGoal = mMaxPos - mWidth / 2;
        if (mOffsetGoal < 0)
            mOffsetGoal = 0;
    }

    private void player() {
        if (isClickable() && !mIsPlaying) {
            return;
        }
        onPlay(mStartPos);
    }

    /**
     * 生成最终剪辑的音频文件名称
     * @param title
     * @param extension
     * @return
     */
    private String makeAudioFilename(CharSequence title, String extension) {
        // Create the parent directory
        File parentDirFile = new File(StorageUtils.getDiskFileDir(CommonApplication.getContext(),"audio") + "/clip");
        if (!parentDirFile.exists()) {
            parentDirFile.mkdirs();
        }
        String filename = title.toString();
        // Try to make the filename unique
        String path = null;
        for (int i = 0; i < 200; i++) {
            path = parentDirFile.getAbsolutePath() + "/" + filename + "_" + String.valueOf(i + 1) + extension;
            if (!new File(path).exists()){
                mTitle = filename + "_" + String.valueOf(i + 1);
                break;
            }
        }
        return path.replace("?","").replace("？","").replace(":","");
    }

    /**
     * Return extension including dot, like ".mp3"
     */
    private String getExtensionFromFilename(String filename) {
        try {
            return filename.substring(filename.lastIndexOf('.'), filename.length());
        } catch (Exception e) {
        }
        return "不支持";
    }
}
