package com.eddierangel.southkern.android.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.facebook.AccessToken;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



public class UserCreation extends AppCompatActivity {

    private TextInputEditText mUserName, mUserPosition, mUserOrganization;
    private CoordinatorLayout mLoginLayout;
    private Button mFinishUserButton;
    private LoginButton facebookButton;
    private String EMAIL;
    private HashMap<String, String> data = new HashMap<String, String>();
    private CallbackManager callbackManager;

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

        setContentView(R.layout.activity_create_user);

        // Facebook initialization
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        callbackManager = CallbackManager.Factory.create();
        EMAIL = PreferenceUtils.getUserId(UserCreation.this.getApplicationContext()); // Users log in with email

        mLoginLayout = (CoordinatorLayout) findViewById(R.id.layout_create_user);

        mUserName = (TextInputEditText) findViewById(R.id.create_user_name);
        mUserPosition = (TextInputEditText) findViewById(R.id.create_user_position);
        mUserOrganization = (TextInputEditText) findViewById(R.id.create_user_organization);

        mFinishUserButton = (Button) findViewById(R.id.button_user_create);


        facebookButton = (LoginButton) findViewById(R.id.login_button);
        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFinishUserButton.setEnabled(false);
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
                                if (response != null) {
                                    try {
                                        String resData = response.getRawResponse();
                                        // Convert JSON String to HashMap using jsonToMap so we can create user meta data
                                        data = jsonToMap(resData);
                                        mFinishUserButton.setEnabled(true);
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
                }

                @Override
                public void onError(FacebookException exception) {
                    Log.d("Facebook auth error: ", "" + exception);
                    mFinishUserButton.setEnabled(true);
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

                Intent returnIntent = new Intent();
                returnIntent.putExtra("userData", data);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }


}
