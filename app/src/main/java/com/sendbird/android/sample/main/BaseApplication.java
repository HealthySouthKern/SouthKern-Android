package com.sendbird.android.sample.main;


import android.app.Application;

import com.sendbird.android.SendBird;

public class BaseApplication extends Application {

    private static final String APP_ID = "91EEEE6D-8D4A-4FA4-9A52-0B8E2DDD64D0"; // SouthKernTest01
    public static final String VERSION = "3.0.36";

    @Override
    public void onCreate() {
        super.onCreate();
        SendBird.init(APP_ID, getApplicationContext());
    }
}
