package com.eddierangel.southkern.android.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.PreferenceUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


// TODO: Add Documentation to Public Interface
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private CoordinatorLayout mLoginLayout;
    private HashMap<String, String> userData;
    private ContentLoadingProgressBar mProgressBar;
    private BroadcastReceiver mConnReceiver;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseFunctions mFunctions;
    private static final int RC_SIGN_IN = 9;

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"networkChangeReceiver: app: Network connectivity change");

            Bundle bundle = intent.getExtras();

            NetworkInfo networkInfo = (NetworkInfo) bundle.get("networkInfo");

            if (networkInfo.isConnected() && mFirebaseAuth.getCurrentUser() != null) {
                String userId = PreferenceUtils.getUserId(LoginActivity.this);
                String sendbirdToken = PreferenceUtils.getSendbirdToken(LoginActivity.this);
                String nickname;
                if (userData != null) {
                    nickname = userData.get("user_name");
                } else {
                    nickname = "resident";
                }
                connectToSendBird(userId, nickname, sendbirdToken);
            }
        }
    };


    /***
     *  function makes a call to the firebase function api to handle users with tokens. before we
     * were using just the UID to connect with sendbird which was not secure. using firebase functions, we create
     * a secure endpoint that communicates with the sendbird platform API to provide a secure way to authorize users.
     * @param userID        user ID to register or log into sendbird service
     * @param nickname      user nickname to register with. this is automatically upated on the client when connecting
     * @param firebaseToken firebase token is required to use endpoint, so only firebase authenticated users can authenticate with sendbird
     * this function returns the sendbird API token that is accessed by using task.getResult();
     * */
    private Task<HashMap> getSendbirdUserWithToken(String userID, String nickname, String firebaseToken) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("userID", userID);
        data.put("nickname", nickname);
        data.put("token", firebaseToken);

        return mFunctions
                .getHttpsCallable("getSendbirdUserWithToken")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, HashMap>() {
                    @Override
                    public HashMap then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        return (HashMap) task.getResult().getData();
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase components
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFunctions = FirebaseFunctions.getInstance();

        setContentView(R.layout.activity_login);

        mLoginLayout = (CoordinatorLayout) findViewById(R.id.layout_login);

        // A loading indicator
        mProgressBar = (ContentLoadingProgressBar) findViewById(R.id.progress_bar_login);


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // user is signed in
                    PreferenceUtils.setUserId(LoginActivity.this, user.getEmail());
                    PreferenceUtils.setNickname(LoginActivity.this, "Resident");

                    // Show the loading indicator
                    showProgressBar(true);
                    // Retrieve firebase token from user.
                    user.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult result) {
                            String idToken = result.getToken();
                            // save token to shared store.
                            PreferenceUtils.setFirebaseToken(LoginActivity.this.getApplicationContext(), idToken);

                            final String userId = user.getEmail();

                            /* Use firebase functions to issue a sendbird token to the user after authorizing with firebase. */
                            getSendbirdUserWithToken(userId, "Guest", idToken)
                                    .addOnCompleteListener(new OnCompleteListener<HashMap>() {
                                        @Override
                                        public void onComplete(@NonNull Task<HashMap> task) {
                                            if (!task.isSuccessful()) {
                                                Exception e = task.getException();
                                                if (e instanceof FirebaseFunctionsException) {
                                                    FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                                    FirebaseFunctionsException.Code code = ffe.getCode();
                                                    Object details = ffe.getDetails();
                                                }

                                                // ...
                                            }
                                            // success
                                            // save sendbird token to shared store
                                            String sendbirdToken;
                                            try {
                                                sendbirdToken = (String) task.getResult().get("token");
                                                Boolean firstLogin = (Boolean) task.getResult().get("firstLogin");
                                                PreferenceUtils.setSendbirdToken(LoginActivity.this.getApplicationContext(), sendbirdToken);
                                                if (firstLogin) {
                                                    // It is the users first time logging in -> show them UserCreation form
                                                    Intent intent = new Intent(LoginActivity.this, UserCreation.class);
                                                    startActivityForResult(intent, 1);
                                                }
                                                if (!firstLogin && task.getResult().get("nickname") != null) {
                                                    // This isn't the users first rodeo -> connect and show them main feed
                                                    String userNickname = (String) task.getResult().get("nickname");
                                                    connectToSendBird(userId, userNickname, sendbirdToken);
                                                }
                                            }
                                            catch(Exception e) {
                                                Log.i(TAG, "onCreate: getSendbirdUserWithToken: sendbirdtokenErr" + e);
                                            }
                                        }
                                    });;
                        }
                    });
                } else {
                    // user is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setTheme(R.style.LoginTheme)
                                    .setLogo(R.drawable.bhc_logo_color_centered)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }
    // Obtain user and social profile data from UserCreation class and use it to connect to sendbird
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        HashMap<String, String> result;
            if (requestCode == 1) {
                if (resultCode == Activity.RESULT_OK) {
                    String userId = PreferenceUtils.getUserId(LoginActivity.this);
                    String sendbirdToken = PreferenceUtils.getSendbirdToken(LoginActivity.this);
                    result = (HashMap<String, String>) data.getSerializableExtra("userData");
                    userData = result;
                    Log.i(TAG, "onActivityResult: getSerializableExtra: userdata2: " + result);
                    connectToSendBird(userId, result.get("user_name"), sendbirdToken);
                }
            }

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PreferenceUtils.getConnected(this)) {
            Log.i(TAG, "onStart: getConnected: I am auto connecting: 123");
            connectToSendBird(PreferenceUtils.getUserId(this), PreferenceUtils.getNickname(this), PreferenceUtils.getFirebaseToken(LoginActivity.this));
        }
    }

    /**
     * Attempts to connect a user to SendBird.
     * @param userId    The unique ID of the user.
     * @param userNickname  The user's nickname, which will be displayed in chats.
     * @param sendbirdToken The user's token that we will use to connect to sendbird securely.
     */
    private void connectToSendBird(final String userId, final String userNickname, final String sendbirdToken) {
                showProgressBar(true);

        SendBird.connect(userId, sendbirdToken, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                // Callback received; hide the progress bar.
                showProgressBar(false);

                if (e != null) {
                    // Error!
                    Log.e(TAG,  "connectToSendbird: connect: login_error: " + e.getCode() + " " + e);

                    // Show login failure snackbar
                    if (e.getCode() == 400302) {
                        return; // Error message already propagated (access token is not valid)
                    } else {
                        showSnackbar("Login to SendBird failed. Reconnecting...");
                    }

                    PreferenceUtils.setConnected(LoginActivity.this, false);
                    return;
                }
                if (userData != null) {
                    createUserMetaData(userData);
                }

                PreferenceUtils.setConnected(LoginActivity.this, true);
                PreferenceUtils.setUserId(LoginActivity.this.getApplicationContext(), userId);
                PreferenceUtils.setNickname(LoginActivity.this.getApplicationContext(), userNickname);

                // Update the user's nickname
                if (userData != null) {
                    updateCurrentUserInfo(userData.get("user_name"));
                } else {
                    updateCurrentUserInfo(userNickname);
                }
                updateCurrentUserPushToken();

                // Proceed to MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
    /* Attaches new user's organization and social profile data, and attaches them to the
     * sendbird user object. This data can be later accessed by querying for user meta data using a userId.
     * @param: data - HashMap of user data such as first/last name and profile pic */
    private void createUserMetaData(HashMap<String, String> data) {
        User user = SendBird.getCurrentUser();
        try {
            if (data.containsKey("user_picture")) {

                String picURL = data.get("user_picture");

                SendBird.updateCurrentUserInfo(data.get("user_name"), picURL, new SendBird.UserInfoUpdateHandler() {
                    @Override
                    public void onUpdated(SendBirdException e) {
                        if (e != null) {
                            // Error!
                            Toast.makeText(
                                    LoginActivity.this, "" + e.getCode() + ":" + e.getMessage(),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
            }

            user.updateMetaData(data, new User.MetaDataHandler() {
                @Override
                public void onResult(Map<String, String> map, SendBirdException e) {
                    if (e != null) {    // Error.
                        Log.i(TAG, "createUserMetadata: updateMetaData: meta data error: " + e);
                    }
                }
            });
        } catch(Exception e) {
            Log.e(TAG, "createUserMetadata: updateMetaData: meta data error: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Update the user's push token.
     */
    private void updateCurrentUserPushToken() {
        // Register Firebase Token
        SendBird.registerPushTokenForCurrentUser(FirebaseInstanceId.getInstance().getToken(),
            new SendBird.RegisterPushTokenWithStatusHandler() {
                @Override
                public void onRegistered(SendBird.PushTokenRegistrationStatus pushTokenRegistrationStatus, SendBirdException e) {
                    if (e != null) {
                        // Error!
                        Toast.makeText(LoginActivity.this, "" + e.getCode() + ":" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(LoginActivity.this, "Push token registered.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    /**
     * Updates the user's nickname.
     * @param userNickname  The new nickname of the user.
     */
    private void updateCurrentUserInfo(String userNickname) {
        SendBird.updateCurrentUserInfo(userNickname, null, new SendBird.UserInfoUpdateHandler() {
            @Override
            public void onUpdated(SendBirdException e) {
                if (e != null) {
                    // Error!
                    Toast.makeText(
                            LoginActivity.this, "" + e.getCode() + ":" + e.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();

                    // Show update failed snackbar
                    showSnackbar("Update user nickname failed");

                    return;
                }

            }
        });
    }

    // Displays a Snackbar from the bottom of the screen
    private void showSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(mLoginLayout, text, Snackbar.LENGTH_SHORT);

        snackbar.show();
    }

    // Shows or hides the ProgressBar
    private void showProgressBar(boolean show) {
        if (show) {
            mProgressBar.show();
        } else {
            mProgressBar.hide();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkChangeReceiver);

        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
}
