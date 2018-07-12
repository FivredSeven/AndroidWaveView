package com.suo.waveform;

import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class AbsAdapter<T> extends BaseAdapter {

    protected List<T> mDatas = new ArrayList<T>();
    public boolean isSrcolling=false;

    public void setSrcolling(boolean srcolling) {
        isSrcolling = srcolling;
    }
    public AbsAdapter() {
    }

    public AbsAdapter(List<T> datas) {
        setData(datas);
    }

    public AbsAdapter(T[] datas) {
        setData(datas);
    }

    public void setData(T[] datas) {
        this.mDatas.clear();
        if (datas != null) {
            for (T t : datas) {
                this.mDatas.add(t);
            }
        }
        notifyDataSetChanged();
    }

    public void setData(List<T> datas) {
        this.mDatas.clear();
        if (datas != null) {
            this.mDatas.addAll(datas);
        }
        notifyDataSetChanged();
    }

    public void addData(List<T> datas) {
        if (datas != null && datas.size() > 0) {
            this.mDatas.addAll(datas);
            notifyDataSetChanged();
        }
    }

    public void addData(T[] datas) {
        if (datas != null && datas.length > 0) {
            for (T data : datas) {
                this.mDatas.add(data);
                notifyDataSetChanged();
            }
            notifyDataSetChanged();
        }
    }

    public void addData(T data) {
        if (data != null) {
            this.mDatas.add(data);
            notifyDataSetChanged();
        }
    }

    public void addData(int index, T data) {
        if (index >= 0 && index <= mDatas.size()) {
            if (data != null) {
                this.mDatas.add(index, data);
                notifyDataSetChanged();
            }
        }
    }

    public void addData(int index, List<T> datas) {
        if (index >= 0 && index <= mDatas.size()) {
            if (datas != null && datas.size() > 0) {
                this.mDatas.addAll(index, datas);
                notifyDataSetChanged();
            }
        }
    }

    public void removeData(T data) {
        if (data != null) {
            this.mDatas.remove(data);
            notifyDataSetChanged();
        }
    }

    public void removeData(int index) {
        if (index >= 0 && index < mDatas.size()) {
            this.mDatas.remove(index);
            notifyDataSetChanged();
        }
    }

    public void clearData() {
        this.mDatas.clear();
        notifyDataSetChanged();
    }
    public void addDataToFirst(T data) {
        if (data != null) {
            this.mDatas.add(0,data);
            notifyDataSetChanged();
        }
    }

    public List<T> getDatas() {
        return this.mDatas;
    }

    public void modifyData(T data) {
        if (this.mDatas.contains(data)) {
            int index = this.mDatas.indexOf(data);
            if (index != -1) {
                this.mDatas.remove(index);
                this.mDatas.add(index, data);
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getCount() {
        return this.mDatas.size();
    }

    @Override
    public T getItem(int position) {
        if (position >= 0 && position < this.mDatas.size()) {
            return this.mDatas.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


}
