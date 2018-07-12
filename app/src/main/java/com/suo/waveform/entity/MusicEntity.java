package com.suo.waveform.entity;

import java.io.Serializable;
import java.util.List;

public class MusicEntity implements Serializable{
    public int id;
    public String filename;// 老狼 - 虎口脱险.mp3
    public String musicname;// 虎口脱险
    public String displayname;// 老狼 - 虎口脱险
    public String path;// **/**/**/老狼 - 虎口脱险.mp3
    public String singer;
    public int duration;
    public String album;
    public long size;
    public String type;
    public String image;
    public String letter;

    public String cut_path;//剪辑后的路径

    public String arrangement;//作曲
    public String lyric;//作词

    /**********/
    public boolean isFirst;

}
