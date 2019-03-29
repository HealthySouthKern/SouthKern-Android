package com.eddierangel.southkern.android.main;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.eddierangel.southkern.android.utils.LogUtility;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.User;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import retrofit2.Call;


public class UserCreation extends AppCompatActivity {

    private static final String TAG = "UserCreation";
    private TextInputEditText mUserPosition, mUserOrganization;
    private Button mFinishUserButton;
    private LoginButton facebookButton;
    private TwitterLoginButton twitterButton;
    private HashMap<String, String> data = new HashMap<String, String>();
    private CallbackManager callbackManager;
    private String profileURL;
    private static String FACEBOOK_FIELD_PICTURE = "picture";
    private static String FACEBOOK_FIELD_DATA = "data";
    private static String FACEBOOK_FIELD_URL = "url";
    private static String FACEBOOK_FIELD_PROFILE_IMAGE = "picture.type(large)";
    private static String FACEBOOK_FIELDS = "fields";

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

    private String getImageUrl(GraphResponse response) {
        String url = null;
        try {
            url = response.getJSONObject()
                    .getJSONObject(FACEBOOK_FIELD_PICTURE)
                    .getJSONObject(FACEBOOK_FIELD_DATA)
                    .getString(FACEBOOK_FIELD_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_create_user);
        configureLabels();
    }

    @Override
    protected void onStart() {
        super.onStart();
        configureButtons();
    }


    private void configureButtons() {
        mFinishUserButton = (Button) findViewById(R.id.button_user_create);

        mFinishUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userOrganization = mUserOrganization.getText().toString();
                String userPosition = mUserPosition.getText().toString();

                data.put("user_organization", userOrganization);
                data.put("user_position", userPosition);

                if (profileURL != null) {
                    data.put("user_picture", profileURL);
                    LogUtility.d(TAG, "configureButtons: Attempting to add image to Firebase user");
                    //Can delete the log tag once it's been tested & confirmed working
                    putImageInFirebaseUser(profileURL);
                }

                if (!userOrganization.equals("")) {
                    data.put("user_type", "organization");
                } else {
                    data.put("user_type", "resident");
                }

                /* Uncomment line below in order to make new admins. Right now we have no clear way to make a user
                 * an admin. However, in the future we should have a javascript function to create admins. For now,
                 * use this temporary solution. */
                // data.put("user_type", "admin");

                Intent returnIntent = new Intent();
                returnIntent.putExtra("userData", data);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    private void configureLabels() {
        mUserPosition = (TextInputEditText) findViewById(R.id.create_user_position);
        mUserOrganization = (TextInputEditText) findViewById(R.id.create_user_organization);
    }

    private void configureTwitterButton() {
        //twitterButton = (TwitterLoginButton) findViewById(R.id.login_button_twitter);
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

                        data.put("user_picture", userResult.data.profileImageUrl);
                        LogUtility.d(TAG, "configureTwitterButton: Attempting to add twitter image to Firebase user");
                        //Can delete the log tag once it's been tested & confirmed working
                        putImageInFirebaseUser(userResult.data.profileImageUrl);

                        userData.put("user_name", userResult.data.name);
                        userData.put("user_background_image", userResult.data.profileBackgroundImageUrl);
                        userData.put("user_screen_name", userResult.data.screenName);
                        userData.put("user_description", userResult.data.description);
                        userData.put("user_twitter", "true");

                        mFinishUserButton.setEnabled(true);
                        facebookButton.setEnabled(false);

                    }

                    @Override
                    public void failure(TwitterException e) {
                        mFinishUserButton.setEnabled(true);
                        twitterButton.setEnabled(true);
                        facebookButton.setEnabled(true);
                        LogUtility.d(TAG, "onCreate: failure: Verify Credentials Failure " + e);
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
    }

    private void configureFacebookButton() {

        //facebookButton = (LoginButton) findViewById(R.id.login_button_facebook);
        facebookButton.setTextSize(14);
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
                    public void onSuccess(final LoginResult loginResult) {

                        // fetch permanent facebook profile URL.
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        profileURL = getImageUrl(response);
                                        putImageInFirebaseUser(profileURL); //(Put Facebook image into Firebase user) ((Not sure if this one will work))
                                        mFinishUserButton.setEnabled(true);
                                        twitterButton.setEnabled(false);
                                    }
                                });

                        Bundle parameters = new Bundle();
                        parameters.putString(FACEBOOK_FIELDS, FACEBOOK_FIELD_PROFILE_IMAGE);
                        request.setParameters(parameters);
                        request.executeAsync();

                        // Now that we have a token for facebook, we can use that to fetch profile data

                        Bundle params = new Bundle();
                        params.putString("fields", "first_name, last_name, locale, gender, cover");
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
                                                data.put("user_twitter", "false");
                                                mFinishUserButton.setEnabled(true);
                                                twitterButton.setEnabled(false);
                                            } catch (Exception e) {
                                                LogUtility.i(TAG, "graph err: " + e);
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                        ).executeAsync();

                    }

                    @Override
                    public void onCancel() {
                        mFinishUserButton.setEnabled(true);
                        twitterButton.setEnabled(true);
                        facebookButton.setEnabled(true);

                    }

                    @Override
                    public void onError(FacebookException exception) {
                        LogUtility.e(TAG, "onCreate: onError: Facebook auth error: " + exception);
                        mFinishUserButton.setEnabled(true);
                        twitterButton.setEnabled(true);
                        facebookButton.setEnabled(true);
                    }
                });
    }

    private void initializeTwitter() {
        // Twitter initialization
        TwitterAuthConfig authConfig =  new TwitterAuthConfig(
                getString(R.string.twitter_consumer_key),
                getString(R.string.twitter_consumer_secret));

        TwitterConfig twitterConfig = new TwitterConfig.Builder(this)
                .twitterAuthConfig(authConfig)
                .build();

        Twitter.initialize(twitterConfig);
    }

    private void initializeFacebook() {
        // Facebook initialization
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data);

        // Twitter
        twitterButton.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void putImageInFirebaseUser(String imageURL) {
        //Added because we get the image from the firebase user in LoginActivity
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(Uri.parse(imageURL))
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            LogUtility.d(TAG, "putImageInFirebaseUser: user profile updated");
                        }
                    }
                });
    }

}
