package com.eddierangel.southkern.android.main;


import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.sendbird.android.SendBird;

import io.fabric.sdk.android.Fabric;


public class BaseApplication extends Application {

    // TODO: String.xml
    private static final String APP_ID = "91EEEE6D-8D4A-4FA4-9A52-0B8E2DDD64D0"; // SouthKernTest01

    @Override
    public void onCreate() {
        super.onCreate();
        SendBird.init(APP_ID, getApplicationContext());

        final Fabric fabric = new Fabric.Builder(this)
                .kits(new Crashlytics())
                .debuggable(true)
                .build();
        Fabric.with(fabric);
    }
}
