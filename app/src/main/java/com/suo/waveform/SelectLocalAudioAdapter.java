package com.suo.waveform;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.suo.waveform.entity.MusicEntity;
import com.suo.waveform.util.TimeShowUtils;

import java.util.HashMap;
import java.util.List;

public class SelectLocalAudioAdapter extends AbsAdapter<MusicEntity> {

    private Context mContext;
    private HashMap<String, Integer> mLetterHashMap = new HashMap<String, Integer>();

    public SelectLocalAudioAdapter(Context context) {
        mContext = context;
    }

    @Override
    public void setData(List<MusicEntity> list) {
        mDatas = list;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.local_audio_item, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final MusicEntity entity = getItem(position);
        if(entity == null){
            return convertView;
        }

        viewHolder.tv_filename.setText(entity.filename);
        viewHolder.tv_duration.setText(TimeShowUtils.getDurationFromTime(entity.duration));

        return convertView;
    }

    public static class ViewHolder {
        public TextView tv_filename;
        public TextView tv_duration;

        public ViewHolder(View view) {
            tv_filename = (TextView) view.findViewById(R.id.local_audio_item_filename);
            tv_duration = (TextView) view.findViewById(R.id.local_audio_item_duration);
        }
    }
}
