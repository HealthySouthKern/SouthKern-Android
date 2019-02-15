package com.eddierangel.southkern.android.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.main.LoginActivity;
import com.eddierangel.southkern.android.main.MainActivity;

import java.util.Timer;
import java.util.TimerTask;

public class ReconnectionManager extends AppCompatActivity {

    private LinearLayout mLayout;
    private Timer mTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reconnect_splash);

        mLayout = (LinearLayout) findViewById(R.id.lost_connection_splash);

        mTimer = new Timer();

        mTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                new InternetCheck(new InternetCheck.Consumer() {
                    @Override
                    public void accept(Boolean internet) {
                        // Once an internet connection has been reestablished, reroute to login so that the current sendbird user
                        // can be reestablished.
                        if (internet) {
                            Intent intent = new Intent(ReconnectionManager.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            showSnackbar("Reconnection failed. Trying again in 10 seconds.");
                        }
                    }
                });
            }
        },0,10 * 1000); // 10 seconds
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mTimer.cancel();
    }

    // Displays a Snackbar from the bottom of the screen
    private void showSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(mLayout, text, Snackbar.LENGTH_SHORT);

        snackbar.show();
    }
}
