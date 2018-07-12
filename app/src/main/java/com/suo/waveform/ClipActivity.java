package com.suo.waveform;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.suo.waveform.entity.MusicEntity;
import com.suo.waveform.util.TimeShowUtils;
import com.suo.waveform.util.ToastUtils;

public class ClipActivity extends AppCompatActivity {
    private AudioClipManager manager;
    private TextView mDesc;
    private ImageView mPlayPause;
    private View mSubmit;
    private MusicEntity mEntity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.audio_clip_main);
        manager = new AudioClipManager(this);

        mEntity = (MusicEntity) getIntent().getSerializableExtra("music");
        mDesc = (TextView) findViewById(R.id.text_desc);
        mPlayPause = (ImageView) findViewById(R.id.play_pause_music);

        String time = TimeShowUtils.getDurationFromTime(mEntity.duration);
        mDesc.setText(time + " " + mEntity.filename);

        mSubmit = findViewById(R.id.submit);
        manager.setPlayOrPauseButton(mPlayPause, R.mipmap.music_play, R.mipmap.music_pause);

        manager.setMusicEntity(mEntity);

        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.save();
                manager.setAudioClipListener(new AudioClipManager.AudioClipListener() {
                    @Override
                    public void success(String path) {
                        ToastUtils.showToastShort(ClipActivity.this, "音频文件剪辑成功");
                    }

                    @Override
                    public void fail(String message) {
                        if (!TextUtils.isEmpty(message) && message.equals("No space left on device")) {
                            ToastUtils.showToastShort(ClipActivity.this, "SD卡容量不足");
                        } else if (!TextUtils.isEmpty(message)){
                            ToastUtils.showToastShort(ClipActivity.this, message);
                        } else {
                            ToastUtils.showToastShort(ClipActivity.this, "选择失败");
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        manager.release();

    }
}
