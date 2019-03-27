package com.eddierangel.southkern.android.main;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.eddierangel.southkern.android.utils.LogUtility;
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
import com.google.firebase.iid.InstanceIdResult;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 *  Main Entry point into App
 * */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private CoordinatorLayout mLoginLayout;
    private HashMap<String, String> userData;
    private ContentLoadingProgressBar mProgressBar;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private OnCompleteListener mUserWithTokenListener;
    private OnSuccessListener<GetTokenResult> mGetTokenListener;
    private FirebaseFunctions mFunctions;
    private DatabaseReference mDatabase;
    private Boolean firstTimeLogin, ranOnlyOnce = true;
    private String uniqueId, generatedProfileUrl;
    private static final int RC_SIGN_IN = 9;

    private static final int REQUEST_PERMISSION_ALL = 1;
    private String[] PERMISSIONS = {
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE
    };

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"networkChangeReceiver: app: Network connectivity change");

            Bundle bundle = intent.getExtras();

            if(bundle != null)
            {
                NetworkInfo networkInfo = (NetworkInfo) bundle.get("networkInfo");

                if (networkInfo != null)
                {
                    if (networkInfo.isConnected() && mFirebaseAuth.getCurrentUser() != null) {
                        if (firstTimeLogin != null) {
                            if (!firstTimeLogin) {
                                String userId = PreferenceUtils.getUserId(LoginActivity.this);
                                String sendbirdToken = PreferenceUtils.getSendbirdToken(LoginActivity.this);
                                connectToSendBird(userId, mFirebaseAuth.getCurrentUser().getDisplayName(),
                                        mFirebaseAuth.getCurrentUser().getPhotoUrl().toString(), sendbirdToken);
                            }
                        }
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
                    public HashMap then(@NonNull Task<HttpsCallableResult> task) {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        if( task.getResult().getData() != null)
                        {
                            return (HashMap) task.getResult().getData();
                        }
                        return new HashMap<>();
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(checkPermissions())
        {
            LogUtility.i(TAG,"@string/internet_permission_granted");
        } else {
            requestPermissions();
        }

        initializeFirebaseComponents();
        setContentView(R.layout.activity_login);

        mLoginLayout = findViewById(R.id.layout_login);
        mProgressBar = findViewById(R.id.progress_bar_login);
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseLogin();
    }

    private void firebaseLogin() {

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                // Valid User Logged In
                if (firebaseUser != null) {
                    setAppUserInfo(firebaseUser);

                } else {
                    // user is signed out
                    startFirebaseSignInActivity();
                }
            }
        };
    }

    private void startFirebaseSignInActivity() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setTheme(R.style.LoginTheme)
                        .setLogo(R.drawable.bhc_logo_color_centered)
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build(),
                                new AuthUI.IdpConfig.FacebookBuilder().build(),
                                new AuthUI.IdpConfig.TwitterBuilder().build())).build(),
                RC_SIGN_IN);
    }

    private void setAppUserInfo(FirebaseUser firebaseUser) {
        // Set Values
        PreferenceUtils.setUserId(LoginActivity.this, firebaseUser.getEmail());
        PreferenceUtils.setNickname(LoginActivity.this, firebaseUser.getDisplayName());
        PreferenceUtils.setProfileUrl(LoginActivity.this, firebaseUser.getPhotoUrl().toString());

        // Show the loading indicator
        showProgressBar(true);

        // Retrieve firebase token from user.
        getFirebaseToken(firebaseUser);
    }

    private void getFirebaseToken(final FirebaseUser firebaseUser) {

        mGetTokenListener = new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult result) {
                // Prevent any unnecessary calls
                if (ranOnlyOnce) {
                    ranOnlyOnce = false;
                    getFirebaseResults(result, firebaseUser);
                }
            }
        };
        firebaseUser.getIdToken(true).addOnSuccessListener(mGetTokenListener);
    }

    private void getFirebaseResults(GetTokenResult result, FirebaseUser firebaseUser) {

        String authUIToken = result.getToken();
        // save token to shared store.
        PreferenceUtils.setFirebaseToken(LoginActivity.this.getApplicationContext(), authUIToken);
        uniqueId = firebaseUser.getUid();

        final String userId = firebaseUser.getEmail();
        final String userName = firebaseUser.getDisplayName();
        final String profileUrl = firebaseUser.getPhotoUrl().toString();

        /* Use firebase functions to issue a sendbird token to the user after authorizing with firebase. */
        Task<HashMap> userWithToken = getSendbirdUserWithToken(userId, userName, authUIToken);
        mUserWithTokenListener = new OnCompleteListener<HashMap>() {
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
                setSendBirdInfo(task, userId, userName, profileUrl);
            }
        };
        userWithToken.addOnCompleteListener(mUserWithTokenListener);
    }

    private void setSendBirdInfo(@NonNull Task<HashMap> task, String userId, String userName, String profileUrl) {

        String sendbirdToken;
        try {
            sendbirdToken = (String) task.getResult().get("token");
            Boolean firstLogin = (Boolean) task.getResult().get("firstLogin");
            PreferenceUtils.setSendbirdToken(LoginActivity.this.getApplicationContext(), sendbirdToken);
            firstTimeLogin = firstLogin;

            if (firstLogin) {
                // Get generated profile url from sendbird and set as temporary profile picture
                // this is only used if they do not have a social media profile picture
                generatedProfileUrl = (String) task.getResult().get("userPicture");

                // It is the users first time logging in -> show them UserCreation form
                Intent intent = new Intent(LoginActivity.this, UserCreation.class);
                startActivityForResult(intent, 1);
            }
            if (!firstLogin && task.getResult().get("nickname") != null) {
                // This isn't the users first rodeo -> connect and show them main feed
                connectToSendBird(userId, userName, profileUrl, sendbirdToken);
            }

        } catch (Exception e) {
            LogUtility.e(TAG, "setSendBirdInfo: " + e.getMessage());
        }
    }

    private void initializeFirebaseComponents() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFunctions = FirebaseFunctions.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private boolean checkPermissions() {
        return hasPermissions(this, PERMISSIONS);
    }

    private void requestPermissions()
    {
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_ALL);
    }

    public boolean hasPermissions(Context context, String... permissions) {
        boolean status = false;
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.INTERNET)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.ACCESS_NETWORK_STATE)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        showMessageOKCancel("You need to allow access to the Internet",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                       requestPermissions();
                                    }
                                });
                    }
                    else {
                        // No explanation needed; request the permission
                        requestPermissions();
                    }
                } else {
                    // Permission has already been granted
                    status = true;
                }
            }
        }
        return status;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(LoginActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ALL:
            {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(android.Manifest.permission.INTERNET, PackageManager.PERMISSION_GRANTED);
                perms.put(android.Manifest.permission.ACCESS_NETWORK_STATE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                        && perms.get(android.Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    LogUtility.i(TAG, "@string/internet_permission_granted");

                } else {
                    // Permission Denied
                    LogUtility.i(TAG, "@string/internet_permission_denied");
                    Toast.makeText(this, "@string/internet_permission_denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Obtain user and social profile data from UserCreation class and use it to connect to sendbird
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        HashMap<String, String> result;
            if (requestCode == 1) {
                if (resultCode == Activity.RESULT_OK) {
                    final String userId = PreferenceUtils.getUserId(LoginActivity.this);
                    final String user_name = PreferenceUtils.getNickname(LoginActivity.this);
                    final String profileUrl = PreferenceUtils.getProfileUrl(LoginActivity.this);
                    final String firebaseToken = PreferenceUtils.getFirebaseToken(LoginActivity.this);
                    String sendbirdToken = PreferenceUtils.getSendbirdToken(LoginActivity.this);
                    result = (HashMap<String, String>) data.getSerializableExtra("userData");

                    // Don't bother checking if the user exists since if they don't we will create them anyway.
                    // Get data from the first time login form (user creation form) and store with relative information.
                    userData = result;
                    userData.put("uid", uniqueId);
                    userData.put("user_id", userId);
                    userData.put("user_name", user_name);
                    userData.put("sendbirdToken", sendbirdToken);
                    userData.put("firebaseToken", firebaseToken);
                    userData.put("user_picture", profileUrl);

                    // If the user did not opt to integrate social media then give them a generated profile url.
                    if (userData.get("user_picture") == null || userData.get("user_picture") == "") {
                        userData.put("user_picture", generatedProfileUrl);
                    }

                    //Cache UserData
                    PreferenceUtils.setUser(LoginActivity.this.getApplicationContext(), userData);

                    //Persist UserData
                    mDatabase.child("southkernUsers").child(uniqueId).setValue(userData);

                    connectToSendBird(userId, user_name, profileUrl, sendbirdToken);
                }
            }

    }

    /**
     * Attempts to connect a user to SendBird.
     * @param userId    The email address of the user.
     * @param userName  The user's name, which will be displayed in chats.
     * @param sendbirdToken The user's token that we will use to connect to sendbird securely.
     */
    private void connectToSendBird(final String userId, final String userName, final String profileUrl, final String sendbirdToken) {
        LogUtility.i(TAG, "@string/attempt_sendbird_connection");
        showProgressBar(true);
        if (mFirebaseAuth.getCurrentUser() != null) {

            SendBird.connect(userId, sendbirdToken, new SendBird.ConnectHandler() {
                @Override
                public void onConnected(User sendbirdUser, SendBirdException e) {
                    // Callback received; hide the progress bar.
                    showProgressBar(false);

                    if (e != null) {
                        // Error!
                        LogUtility.e(TAG, "@string/sendbird_login_error" + e.getCode() + " " + e);

                        // Show login failure snackbar
                        if (e.getCode() != 400302) {
                            showSnackbar("@string/sendbird_login_failed");
                        }

                        PreferenceUtils.setConnected(LoginActivity.this, false);
                        return;
                    }

                    if (userData == null) {
                        // Check if user meta data exists in sendbird. We do this check in order to migrate users from sendbird
                        // to firebase. Once all accounts are migrated we can safely remove this check.
                        if (!SendBird.getCurrentUser().getMetaData().isEmpty()) {
                            Map<String, String> tempUser = SendBird.getCurrentUser().getMetaData();
                            tempUser.put("uid", uniqueId);
                            tempUser.put("user_id", userId);
                            tempUser.put("user_name", userName);
                            tempUser.put("user_picture", profileUrl);

                            mDatabase.child("southkernUsers").child(uniqueId).setValue(tempUser);

                            // Delete old meta data once userMetaData has been ported to firebase. This is important
                            // since if we don't delete the old data we will keep overwriting firebase data with old sendbird data.
                            SendBird.getCurrentUser().deleteAllMetaData(new User.DeleteMetaDataHandler() {
                                @Override
                                public void onResult(SendBirdException e) {
                                    if (e != null) {
                                        LogUtility.e(TAG, "@string/sendbird_delete_metadata_error"+ " " + e);
                                    }
                                }
                            });

                        }

                        // If userData is null then it is safe to assume this is not their first time logging in.
                        // During the first time login user information such as id and nickname are already set.
                        // Just update tokens.
                        mDatabase.child("southkernUsers").child(uniqueId).child("sendbirdToken").setValue(sendbirdToken);
                        mDatabase.child("southkernUsers").child(uniqueId).child("firebaseToken")
                                .setValue(PreferenceUtils.getFirebaseToken(LoginActivity.this));
                    }

                    PreferenceUtils.setConnected(LoginActivity.this, true);
                    PreferenceUtils.setUserId(LoginActivity.this.getApplicationContext(), userId);
                    PreferenceUtils.setNickname(LoginActivity.this.getApplicationContext(), userName);
                    PreferenceUtils.setProfileUrl(LoginActivity.this.getApplicationContext(), profileUrl);

                    // Update the user's nickname
                    if (userData != null) {
                        updateCurrentUserInfo(userData.get("user_name"),userData.get("user_picture"));
                    } else {
                        updateCurrentUserInfo(userName, profileUrl);
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
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Exception ffe = task.getException();
                            if (ffe instanceof FirebaseFunctionsException) {

                                FirebaseFunctionsException.Code code = ((FirebaseFunctionsException) ffe).getCode();
                                Object details = code;
                                LogUtility.d(TAG, "updateCurrentUserPushToken" + details.toString());
                            }

                            // ...
                            // success
                            String firebaseInstanceId;
                            try {
                                firebaseInstanceId = task.getResult().getToken();
                                SendBird.registerPushTokenForCurrentUser(firebaseInstanceId,
                                        new SendBird.RegisterPushTokenWithStatusHandler() {
                                            @Override
                                            public void onRegistered(SendBird.PushTokenRegistrationStatus
                                                                 pushTokenRegistrationStatus, SendBirdException sendbirdException) {
                                                if (sendbirdException != null) {
                                                    // Error!
                                                    LogUtility.d(TAG, "RegisterPushTokenWithStatusHandler: " + sendbirdException.getMessage());
                                                    Toast.makeText(LoginActivity.this,
                                                            "" + sendbirdException.getCode() + ":" + sendbirdException.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                Toast.makeText(LoginActivity.this, "Push token registered.", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                            } catch (Exception e) {
                                LogUtility.e(TAG, "firebaseInstanceId Failed: " + e.getMessage());
                            }
                        }
                        }
                });

    }

    /**
     * Updates the user's nickname.
     * @param userNickname  The new nickname of the user.
     */
    private void updateCurrentUserInfo(String userNickname, String profileUrl) {

        SendBird.updateCurrentUserInfo(userNickname, profileUrl, new SendBird.UserInfoUpdateHandler() {
            @Override
            public void onUpdated(SendBirdException sendbirdException) {
                if (sendbirdException != null) {
                    // Error!
                    LogUtility.d(TAG, "RegisterPushTokenWithStatusHandler: " + sendbirdException.getMessage());
                    Toast.makeText(
                            LoginActivity.this, "" + sendbirdException.getCode() + ":" + sendbirdException.getMessage(),
                            Toast.LENGTH_SHORT)
                            .show();

                    // Show update failed snackbar
                    showSnackbar("@string/sendbird_udpate_nickname_failed");
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

        if(checkPermissions())
        {
            LogUtility.i(TAG,"@string/internet_permission_granted");
        } else {
            requestPermissions();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
}
