package com.eddierangel.southkern.android.openchannel;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.eddierangel.southkern.android.R;
import com.sendbird.android.OpenChannel;
import com.sendbird.android.OpenChannelListQuery;
import com.sendbird.android.SendBirdException;

import java.util.List;

// TODO: Add Documentation to Public Interface
public class OpenChatFeed extends AppCompatActivity {

    private static final String TAG = "OpenChatFeed";
    private String channelURL;
    private String channelName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_channel);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_open_channel);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        channelName = (String) intent.getStringExtra("name");

        OpenChannelListQuery channelListQuery = OpenChannel.createOpenChannelListQuery();
        channelListQuery.next(new OpenChannelListQuery.OpenChannelListQueryResultHandler() {
            @Override
            public void onResult(List<OpenChannel> channels, SendBirdException e) {
                if (e != null) {    // Error.
                    Log.i(TAG, "onCreate: createOpenChannelListQuery: channelerr: " + e);
                    return;
                }

                for (OpenChannel channel : channels) {
                    if (channel.getName().equals(channelName)) {
                        channelURL = channel.getUrl();
                        Log.i(TAG, "onCreate: getUrl: channelurl: " + channelURL);
                    }
                }

                // Load list of Open Channels
                Fragment fragment = OpenChatFragment.newInstance(channelURL);
                ((OpenChatFragment) fragment).setCustomView(true, channelURL, channelName);

                FragmentManager manager = getSupportFragmentManager();
                manager.popBackStack();

                manager.beginTransaction()
                        .replace(R.id.container_open_channel, fragment)
                        .commit();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void setActionBarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }
}
