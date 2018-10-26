package com.eddierangel.southkern.android.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;

import com.eddierangel.southkern.android.R;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.User;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import retrofit2.Call;


public class UserCreation extends AppCompatActivity {

    private TextInputEditText mUserName, mUserPosition, mUserOrganization;
    private CoordinatorLayout mLoginLayout;
    private Button mFinishUserButton;
    private LoginButton facebookButton;
    private TwitterLoginButton twitterButton;
    private String EMAIL;
    private HashMap<String, String> data = new HashMap<String, String>();
    private CallbackManager callbackManager;
    private TwitterAuthClient authClient = new TwitterAuthClient();

    /* Helper function that converts a JSON string to a HashMap.
     * @param: String str is the JSON string to convert */
    public static HashMap<String, String> jsonToMap(String str) throws JSONException {

        HashMap<String, String> map = new HashMap<String, String>();
        JSONObject jObject = new JSONObject(str);
        Iterator<?> keys = jObject.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
            String value = jObject.getString(key);
            map.put("user_" + key, value);

        }

        return map;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Facebook initialization
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();
        EMAIL = PreferenceUtils.getUserId(UserCreation.this.getApplicationContext()); // Users log in with email

        // Twitter initialization
        TwitterAuthConfig authConfig =  new TwitterAuthConfig(
                getString(R.string.twitter_consumer_key),
                getString(R.string.twitter_consumer_secret));

        TwitterConfig twitterConfig = new TwitterConfig.Builder(this)
                .twitterAuthConfig(authConfig)
                .build();

        Twitter.initialize(twitterConfig);

        setContentView(R.layout.activity_create_user);

        mLoginLayout = (CoordinatorLayout) findViewById(R.id.layout_create_user);

        mUserName = (TextInputEditText) findViewById(R.id.create_user_name);
        mUserPosition = (TextInputEditText) findViewById(R.id.create_user_position);
        mUserOrganization = (TextInputEditText) findViewById(R.id.create_user_organization);

        mFinishUserButton = (Button) findViewById(R.id.button_user_create);
        facebookButton = (LoginButton) findViewById(R.id.login_button_facebook);
        facebookButton.setTextSize(14);

        twitterButton = (TwitterLoginButton) findViewById(R.id.login_button_twitter);
        twitterButton.setText("Continue with Twitter");
        twitterButton.setTextSize(14);

        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFinishUserButton.setEnabled(false);
                facebookButton.setEnabled(false);
            }
        });

        twitterButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                TwitterSession session = result.data;
                // Do something with result, which provides a TwitterSession for making API calls
                Call<User> user = TwitterCore.getInstance().getApiClient(session).getAccountService().verifyCredentials(false, false, true);
                        user.enqueue(new Callback<User>() {
                    @Override
                    public void success(Result<User> userResult) {
                        HashMap<String, String> userData = new HashMap<String, String>();

                        userData.put("user_picture", userResult.data.profileImageUrl);
                        userData.put("user_name", userResult.data.name);
                        userData.put("user_background_image", userResult.data.profileBackgroundImageUrl);
                        userData.put("user_screen_name", userResult.data.screenName);
                        userData.put("user_description", userResult.data.description);
                        userData.put("user_twitter", "true");

                        data = userData;

                        mFinishUserButton.setEnabled(true);
                        facebookButton.setEnabled(false);

                    }

                    @Override
                    public void failure(TwitterException e) {
                        mFinishUserButton.setEnabled(true);
                        twitterButton.setEnabled(true);
                        facebookButton.setEnabled(true);
                        Log.d("TwitterKit", "Verify Credentials Failure", e);
                    }
                });
            }

            @Override
            public void failure(TwitterException exception) {
                mFinishUserButton.setEnabled(true);
                twitterButton.setEnabled(true);
                facebookButton.setEnabled(true);

            }
        });

        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFinishUserButton.setEnabled(false);
                twitterButton.setEnabled(false);
            }
        });
        facebookButton.setReadPermissions("public_profile");
        LoginManager.getInstance().registerCallback(callbackManager,

            new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    // Now that we have a token for facebook, we can use that to fetch profile data
                    Bundle params = new Bundle();
                    params.putString("fields", "first_name, last_name, picture.type(large), locale, gender, cover");
                    new GraphRequest(
                        loginResult.getAccessToken(),
                        "/" + loginResult.getAccessToken().getUserId() + "/",
                        params,
                        HttpMethod.GET,
                        new GraphRequest.Callback() {
                            public void onCompleted(GraphResponse response) {
                                if (response.getError() == null) {
                                    try {
                                        String resData = response.getRawResponse();
                                        // Convert JSON String to HashMap using jsonToMap so we can create user meta data
                                        data = jsonToMap(resData);
                                        mFinishUserButton.setEnabled(true);
                                        twitterButton.setEnabled(false);
                                    } catch (Exception e) {
                                        Log.i("graph err", "" + e);
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    ).executeAsync();

                }

                @Override
                public void onCancel() {
                    // App code
                    mFinishUserButton.setEnabled(true);
                    twitterButton.setEnabled(true);
                    facebookButton.setEnabled(true);

                }

                @Override
                public void onError(FacebookException exception) {
                    Log.d("Facebook auth error: ", "" + exception);
                    mFinishUserButton.setEnabled(true);
                    twitterButton.setEnabled(true);
                    facebookButton.setEnabled(true);
                }
            });

        mFinishUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = mUserName.getText().toString();
                String userOrganization = mUserOrganization.getText().toString();
                String userPosition = mUserPosition.getText().toString();

                data.put("user_name", userName);
                data.put("user_organization", userOrganization);
                data.put("user_position", userPosition);

                Log.i("data_before", "" + data);

                Intent returnIntent = new Intent();
                returnIntent.putExtra("userData", data);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Twitter
        twitterButton.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }


}