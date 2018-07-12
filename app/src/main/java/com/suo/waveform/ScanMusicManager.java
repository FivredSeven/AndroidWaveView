package com.suo.waveform;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.suo.waveform.entity.MusicEntity;
import com.suo.waveform.util.SystemUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ScanMusicManager {

    private static final String TAG = "ScanMusicManager";
    private static volatile ScanMusicManager mInstance;

    public ScanMusicManager() {
    }

    public static ScanMusicManager getInstance(){
        if(mInstance==null){
            synchronized (ScanMusicManager.class){
                if(mInstance==null){
                    mInstance = new ScanMusicManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取sd卡所有的音乐文件
     *
     * @return
     * @throws Exception
     */
    public ArrayList<MusicEntity> getAllSongs(Context context) {

        ArrayList<MusicEntity> musics = null;

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATA },
                MediaStore.Audio.Media.MIME_TYPE + "=? or "
                        + MediaStore.Audio.Media.MIME_TYPE + "=?",
                new String[] { "audio/mpeg", "audio/x-ms-wma" }, null);

        musics = new ArrayList<MusicEntity>();

        if (cursor.moveToFirst()) {

            MusicEntity entity = null;

            do {
                entity = new MusicEntity();
                // 文件名
                entity.filename = cursor.getString(1);
                // 歌曲名
                entity.musicname = cursor.getString(2);
                // 时长
                entity.duration = cursor.getInt(3);
                // 歌手名
                entity.singer = cursor.getString(4);
                // 专辑名
                entity.album = cursor.getString(5);
//                // 年代
//                if (cursor.getString(6) != null) {
//                    entity.setYear(cursor.getString(6));
//                } else {
//                    entity.setYear("未知");
//                }
                // 歌曲格式
                if ("audio/mpeg".equals(cursor.getString(7).trim())) {
                    entity.type = "mp3";
                } else if ("audio/x-ms-wma".equals(cursor.getString(7).trim())) {
                    entity.type = "wma";
                } else if ("audio/mp4a-latm".equals(cursor.getString(7).trim())) {
                    entity.type = "m4a";
                }
                // 文件大小
//                if (cursor.getString(8) != null) {
//                    float size = cursor.getInt(8) / 1024f / 1024f;
//                    entity.size = (size + "").substring(0, 4) + "M";
//                } else {
//                    entity.size = "未知";
//                }
                if (cursor.getInt(8) > 0) {
                    entity.size = cursor.getInt(8);
                }
                // 文件路径
                if (cursor.getString(9) != null) {
                    entity.path = cursor.getString(9);
                }
                musics.add(entity);
            } while (cursor.moveToNext());

            cursor.close();

        }
        return musics;
    }

    private ArrayList<MusicEntity> mMediaLists = new ArrayList<>();
    private Object mObject = new Object();
    /**
     * 通过遍历的方式来获取
     */
    public void querySongs(Context context) {
        if (mMediaLists != null) {
            mMediaLists.clear();
        }
        String dirName = Environment.getExternalStorageDirectory() + File.separator;
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DATA + " like ?",
                new String[]{dirName + "%"},
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        if (cursor == null) return;

        MusicEntity music;
        synchronized (mObject) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                // 如果不是音乐
                String isMusic = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
                if (isMusic != null && isMusic.equals("")) continue;

                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                if (TextUtils.isEmpty(path)) {
                    continue;
                }
                int index = path.lastIndexOf("/");
                String filename = path.substring(index+1, path.length());

                music = new MusicEntity();
                music.filename = filename;
                if (!SystemUtils.isMusicFormatSupport(filename)) {
                    continue;
                }
                music.displayname = filename.substring(0, filename.lastIndexOf("."));
                music.id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                music.musicname = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                music.singer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                if (TextUtils.equals("<unknown>", music.singer)) {
                    music.singer = "";
                }
                if (TextUtils.isEmpty(music.singer)) {
                    String[] singerArray = music.displayname.split("-");
                    if (singerArray.length > 1) {
                        music.singer = singerArray[0];
                    }
                }

                music.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                music.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                music.image = getAlbumImage(context, cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));

                mMediaLists.add(music);
            }
            cursor.close();
        }

    }

    public ArrayList<MusicEntity> getAllSongs() {
        synchronized (mObject) {
            return mMediaLists;
        }
    }

    /**
     * 根据音乐名称和艺术家来判断是否重复包含了
     *
     * @param title
     * @param artist
     * @return
     */
    private boolean isRepeat(String title, String artist) {
        for (MusicEntity music : mMediaLists) {
            if (title.equals(music.musicname) && artist.equals(music.singer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据歌曲id获取图片
     *
     * @param albumId
     * @return
     */
    private String getAlbumImage(Context context, int albumId) {
        String result = "";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Uri.parse("content://media/external/audio/albums/"
                            + albumId), new String[]{"album_art"}, null,
                    null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); ) {
                result = cursor.getString(0);
                break;
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return result;
    }
}
