package com.eddierangel.southkern.android.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eddierangel.southkern.android.utils.PreferenceUtils;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class LoginActivity extends AppCompatActivity {

    private CoordinatorLayout mLoginLayout;
    private TextInputEditText mUserIdConnectEditText, mUserNicknameEditText;
    private Button mConnectButton;
    private ContentLoadingProgressBar mProgressBar;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseFunctions mFunctions;
    private static final int RC_SIGN_IN = 9;


    /* function makes a call to the firebase function api to handle users with tokens. before we
     * were using just the UID to connect with sendbird which was not secure. using firebase functions, we create
     * a secure endpoint that communicates with the sendbird platform API to provide a secure way to authorize users.
     * @param userID        user ID to register or log into sendbird service
     * @param nickname      user nickname to register with. this is automatically upated on the client when connecting
     * @param firebaseToken firebase token is required to use endpoint, so only firebase authenticated users can authenticate with sendbird
     * this function returns the sendbird API token that is accessed by using task.getResult();
     * */
    private Task<String> getSendbirdUserWithToken(String userID, String nickname, String firebaseToken) {
        Log.i("FIREBASE TOKEN", "" + firebaseToken);
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("userID", userID);
        data.put("nickname", nickname);
        data.put("token", firebaseToken);

        return mFunctions
                .getHttpsCallable("getSendbirdUserWithToken")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        String result = (String) task.getResult().getData();
                        return result;
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

        mUserIdConnectEditText = (TextInputEditText) findViewById(R.id.edittext_login_user_id);
        mUserNicknameEditText = (TextInputEditText) findViewById(R.id.edittext_login_user_nickname);

        mUserIdConnectEditText.setText(PreferenceUtils.getUserId(this));
        mUserNicknameEditText.setText(PreferenceUtils.getNickname(this));

        mConnectButton = (Button) findViewById(R.id.button_login_connect);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = mUserIdConnectEditText.getText().toString();
                // Remove all spaces from userID
                final String userIdFormatted = userId.replaceAll("\\s", "");

                final String userNickname = mUserNicknameEditText.getText().toString();

                PreferenceUtils.setUserId(LoginActivity.this, userId);
                PreferenceUtils.setNickname(LoginActivity.this, userNickname);
                String firebaseToken = PreferenceUtils.getFirebaseToken(LoginActivity.this);

                /* Use firebase functions to issue a sendbird token to the user after authorizing with firebase. */
                getSendbirdUserWithToken(userIdFormatted, userNickname, firebaseToken)
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
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
                            PreferenceUtils.setSendbirdToken(LoginActivity.this, task.getResult());
                            connectToSendBird(userIdFormatted, userNickname, task.getResult());
                        }
                    });;

            }
        });

        // A loading indicator
        mProgressBar = (ContentLoadingProgressBar) findViewById(R.id.progress_bar_login);

        // Display current SendBird and app versions in a TextView
        String sdkVersion = String.format(getResources().getString(R.string.all_app_version),
                BaseApplication.VERSION, SendBird.getSDKVersion());
        ((TextView) findViewById(R.id.text_login_versions)).setText(sdkVersion);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // user is signed in

                    // Retrieve firebase token from user.
                    user.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult result) {
                            String idToken = result.getToken();
                            // save token to shared store.
                            PreferenceUtils.setFirebaseToken(LoginActivity.this, idToken);
                        }
                    });
                } else {
                    // user is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(PreferenceUtils.getConnected(this)) {
            connectToSendBird(PreferenceUtils.getUserId(this), PreferenceUtils.getNickname(this), PreferenceUtils.getFirebaseToken(LoginActivity.this));
        }
    }

    /**
     * Attempts to connect a user to SendBird.
     * @param userId    The unique ID of the user.
     * @param userNickname  The user's nickname, which will be displayed in chats.
     * @param sendbirdToken The user's token that we will use to connect to sendbird securely.
     */
    private void connectToSendBird(final String userId, final String userNickname, final String sendbirdToken ) {
        // Show the loading indicator
        showProgressBar(true);
        mConnectButton.setEnabled(false);

        SendBird.connect(userId, sendbirdToken, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                // Callback received; hide the progress bar.
                showProgressBar(false);

                if (e != null) {
                    // Error!
                    Toast.makeText(
                            LoginActivity.this, "" + e.getCode() + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();

                    // Show login failure snackbar
                    showSnackbar("Login to SendBird failed");
                    mConnectButton.setEnabled(true);
                    PreferenceUtils.setConnected(LoginActivity.this, false);
                    return;
                }

                PreferenceUtils.setConnected(LoginActivity.this, true);

                // Update the user's nickname
                updateCurrentUserInfo(userNickname);
                updateCurrentUserPushToken();

                // Proceed to MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
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
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
}
