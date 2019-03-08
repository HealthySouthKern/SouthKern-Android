package com.eddierangel.southkern.android.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


// TODO: Add Documentation to Public Interface
public class LoginActivity extends AppCompatActivity {

    private CoordinatorLayout mLoginLayout;
    private HashMap<String, String> userData;
    private ContentLoadingProgressBar mProgressBar;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseFunctions mFunctions;
    private DatabaseReference mDatabase;
    private Boolean firstTimeLogin, ranOnlyOnce = true;
    private String firebaseUserId, generatedProfileUrl;
    private static final int RC_SIGN_IN = 9;

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();

            NetworkInfo networkInfo = (NetworkInfo) bundle.get("networkInfo");

            if (networkInfo.isConnected() && mFirebaseAuth.getCurrentUser() != null) {
                if (firstTimeLogin != null) {
                    if (!firstTimeLogin) {
                        String userId = PreferenceUtils.getUserId(LoginActivity.this);
                        String sendbirdToken = PreferenceUtils.getSendbirdToken(LoginActivity.this);
                        connectToSendBird(userId, mFirebaseAuth.getCurrentUser().getDisplayName(), sendbirdToken);
                    }
                }
            }
        }
    };


    /***
     *  function makes a call to the firebase function api to handle users with tokens. before we
     * were using just the UID to connect with sendbird which was not secure. using firebase functions, we create
     * a secure endpoint that communicates with the sendbird platform API to provide a secure way to authorize users.
     * @param userID        user ID to register or log into sendbird service
     * @param name          user nickname to register with. this is automatically upated on the client when connecting
     * @param firebaseToken firebase token is required to use endpoint, so only firebase authenticated users can authenticate with sendbird
     * this function returns the sendbird API token that is accessed by using task.getResult();
     * */
    private Task<HashMap> getSendbirdUserWithToken(String userID, String name, String firebaseToken) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("userID", userID);
        data.put("nickname", name);
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
        mDatabase = FirebaseDatabase.getInstance().getReference();

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
                            // Prevent any unnecessary calls
                            if (ranOnlyOnce) {
                                ranOnlyOnce = false;
                                String idToken = result.getToken();

                                // save token to shared store.
                                PreferenceUtils.setFirebaseToken(LoginActivity.this.getApplicationContext(), idToken);
                                firebaseUserId = user.getUid();

                                final String userId = user.getEmail();
                                final String userName = user.getDisplayName();

                                /* Use firebase functions to issue a sendbird token to the user after authorizing with firebase. */
                                getSendbirdUserWithToken(userId, userName, idToken)
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
                                                    firstTimeLogin = firstLogin;

                                                    if (firstLogin) {
                                                        // Get generated profile url from sendbird and set as temporary profile picture
                                                        generatedProfileUrl = (String) task.getResult().get("userPicture");

                                                        // It is the users first time logging in -> show them UserCreation form
                                                        Intent intent = new Intent(LoginActivity.this, UserCreation.class);
                                                        startActivityForResult(intent, 1);
                                                    }
                                                    if (!firstLogin && task.getResult().get("nickname") != null) {
                                                        // This isn't the users first rodeo -> connect and show them main feed
                                                        connectToSendBird(userId, userName, sendbirdToken);
                                                    }

                                                } catch (Exception e) {
                                                    Log.e("sendbirdtokenErr", "" + e);
                                                }
                                            }
                                        });
                            }
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
                    final String userId = PreferenceUtils.getUserId(LoginActivity.this);
                    String sendbirdToken = PreferenceUtils.getSendbirdToken(LoginActivity.this);
                    result = (HashMap<String, String>) data.getSerializableExtra("userData");

                    // Don't bother checking if the user exists since if they don't we will create them anyway.
                    // Get data from the first time login form (user creation form) and store with relative information.
                    userData = result;
                    userData.put("uid", firebaseUserId);
                    userData.put("user_id", userId);
                    userData.put("user_name", mFirebaseAuth.getCurrentUser().getDisplayName());
                    userData.put("sendbirdToken", sendbirdToken);
                    userData.put("firebaseToken", PreferenceUtils.getFirebaseToken(LoginActivity.this));

                    // If the user did not opt to integrate social media then give them a generated profile url.
                    if (userData.get("user_picture") == null) {
                        userData.put("user_picture", generatedProfileUrl);
                    }

                    PreferenceUtils.setUser(LoginActivity.this.getApplicationContext(), userData);

                    mDatabase.child("southkernUsers").child(firebaseUserId).setValue(userData);

                    connectToSendBird(userId, mFirebaseAuth.getCurrentUser().getDisplayName(), sendbirdToken);
                }
            }

    }

    /**
     * Attempts to connect a user to SendBird.
     * @param userId    The unique ID of the user.
     * @param userName  The user's name, which will be displayed in chats.
     * @param sendbirdToken The user's token that we will use to connect to sendbird securely.
     */
    private void connectToSendBird(final String userId, final String userName, final String sendbirdToken) {
        Log.i("apptest123", "attempting connection to sendbird");
        showProgressBar(true);
        if (mFirebaseAuth.getCurrentUser() != null) {

            SendBird.connect(userId, sendbirdToken, new SendBird.ConnectHandler() {
                @Override
                public void onConnected(User user, SendBirdException e) {
                    // Callback received; hide the progress bar.
                    showProgressBar(false);

                    if (e != null) {
                        // Error!
                        Log.e("login_error", e.getCode() + " " + e);

                        // Show login failure snackbar
                        if (e.getCode() != 400302) {
                            showSnackbar("Login to SendBird failed. Reconnecting...");
                        }

                        PreferenceUtils.setConnected(LoginActivity.this, false);
                        return;
                    }

                    if (userData == null) {
                        // Check if user meta data exists in sendbird. We do this check in order to migrate users from sendbird
                        // to firebase. Once all accounts are migrated we can safely remove this check.
                        if (!SendBird.getCurrentUser().getMetaData().isEmpty()) {
                            Map<String, String> tempUser = SendBird.getCurrentUser().getMetaData();
                            tempUser.put("uid", firebaseUserId);
                            tempUser.put("user_id", userId);
                            tempUser.put("user_name", userName);

                            if (SendBird.getCurrentUser().getProfileUrl() != null) {
                                tempUser.put("user_picture", SendBird.getCurrentUser().getProfileUrl());
                            }

                            mDatabase.child("southkernUsers").child(firebaseUserId).setValue(tempUser);

                            // Delete old meta data once userMetaData has been ported to firebase. This is important
                            // since if we don't delete the old data we will keep overwriting firebase data with old sendbird data.
                            SendBird.getCurrentUser().deleteAllMetaData(new User.DeleteMetaDataHandler() {
                                @Override
                                public void onResult(SendBirdException e) {
                                    if (e != null) {
                                        Log.e("delete meta err", "" + e);
                                    }
                                }
                            });

                        }

                        // If userData is null then it is safe to assume this is not their first time logging in.
                        // During the first time login user information such as id and nickname are already set.
                        // Just update tokens.
                        mDatabase.child("southkernUsers").child(firebaseUserId).child("sendbirdToken").setValue(sendbirdToken);
                        mDatabase.child("southkernUsers").child(firebaseUserId).child("firebaseToken").setValue(PreferenceUtils.getFirebaseToken(LoginActivity.this));
                    }

                    PreferenceUtils.setConnected(LoginActivity.this, true);
                    PreferenceUtils.setUserId(LoginActivity.this.getApplicationContext(), userId);
                    PreferenceUtils.setNickname(LoginActivity.this.getApplicationContext(), userName);

                    // Update the user's nickname
                    if (userData != null) {
                        updateCurrentUserInfo(userData.get("user_name"));
                    } else {
                        updateCurrentUserInfo(userName);
                    }
                    updateCurrentUserPushToken();

                    // Proceed to MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
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

        SendBird.updateCurrentUserInfo(userNickname, SendBird.getCurrentUser().getProfileUrl(), new SendBird.UserInfoUpdateHandler() {
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
