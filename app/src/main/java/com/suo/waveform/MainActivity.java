package com.suo.waveform;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.suo.waveform.entity.MusicEntity;
import com.suo.waveform.util.CommonThreadPool;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private SelectLocalAudioAdapter mAdapter;
    private List<MusicEntity> mEntities;
    private Button scan_music;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.permission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyStoragePermissions(MainActivity.this);
            }
        });
        scan_music = (Button) findViewById(R.id.scan_music);

        scan_music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanMusicManager.getInstance().querySongs(MainActivity.this);
                mEntities = ScanMusicManager.getInstance().getAllSongs();
                if (mEntities == null || mEntities.size() == 0) {
//                            showEmptyView();
                    return;
                }
                findViewById(R.id.permission).setVisibility(View.GONE);
                scan_music.setVisibility(View.GONE);
                mAdapter.setData(mEntities);
            }
        });

        mListView = (ListView) findViewById(R.id.select_listview);
        mAdapter =  new SelectLocalAudioAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicEntity entity = mEntities.get(position);
                Intent intent = new Intent(MainActivity.this, ClipActivity.class);
                intent.putExtra("music", entity);
                startActivity(intent);
            }
        });

    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
