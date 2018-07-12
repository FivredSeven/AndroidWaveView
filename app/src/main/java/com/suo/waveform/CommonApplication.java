package com.suo.waveform;

import android.app.Application;
import android.content.Context;

public class CommonApplication extends Application {
    protected static Context mContext;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mContext = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
//        CrashHandler.getInstance().init(this);
    }

    public static Context getContext() {
        return mContext;
    }
}
