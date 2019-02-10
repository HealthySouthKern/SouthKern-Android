package com.eddierangel.southkern.android.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.sendbird.android.SendBird;
import com.sendbird.android.User;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;


// TODO: Add Documentation to Public Interface
public class ViewProfile extends AppCompatActivity {

    private FirebaseFunctions mFunctions;
    private Object userData;
    private Map<String, String> userMetaData;
    private ImageButton backButton;

    private Task<Object> fetchUserwithUserID(String firebaseToken, String userID) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("userID", userID);

        // Changed firebaseToken -> token (this ambiguity is my fault, I used firebaseToken for some functions, but just token for others)
        data.put("token", firebaseToken);

        return mFunctions
                .getHttpsCallable("fetchUserwithUserID")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, Object>() {
                    @Override
                    public Object then(@NonNull Task<HttpsCallableResult> task) throws Exception {

                        return task.getResult().getData();
                    }
                });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        String userID = getIntent().getStringExtra("userId");
        User user = SendBird.getCurrentUser();

        // Initialize mFunctions
        mFunctions = FirebaseFunctions.getInstance();
        backButton = (ImageButton) findViewById(R.id.menu_button_back);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        fetchUserwithUserID(PreferenceUtils.getFirebaseToken(this.getApplicationContext()), userID).addOnCompleteListener(new OnCompleteListener<Object>() {
            @Override
            public void onComplete(@NonNull Task<Object> task) {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                        FirebaseFunctionsException.Code code = ffe.getCode();
                        Object details = ffe.getDetails();
                    }
                }
                try {
                    Object user = task.getResult();
                    userData = (Map<String, Object>) user;
                    userMetaData = (Map<String, String>) ((Map) userData).get("metadata");

                    // Moved settexts inside of callback of fetchUserwithID since we need to use this data
                    // once the function returns.

                    // metadata is a nested object inside the user object which contains the useful profile data.

                    String userPicture = userMetaData.get("user_picture");
                    String nickname = userMetaData.get("user_nickname");
                    String name = userMetaData.get("user_name");
                    String organization = userMetaData.get("user_organization");
                    String position = userMetaData.get("user_position");
                    String role = userMetaData.get("user_type");

                    ImageView profileImage = (ImageView)findViewById(R.id.profile_image);
                    if (userPicture != null) {
                        if (!userPicture.isEmpty()) {
                            Picasso.get().load(userPicture).into(profileImage);
                        }
                    }

                    TextView userNickname = (TextView)findViewById(R.id.text_user_nickname);
                    userNickname.setText(nickname);

                    TextView userName = (TextView)findViewById(R.id.text_user_real_name);
                    userName.setText(name);

                    TextView userOrganization = (TextView)findViewById(R.id.text_user_organization);
                    userOrganization.setText(organization);

                    TextView userPosition = (TextView)findViewById(R.id.text_user_position);
                    userPosition.setText(position);

                    TextView userRole = (TextView)findViewById(R.id.text_user_role);
                    userRole.setText(role);
                }
                catch (Exception e) {
                    Log.e("InsideTaskComplete", "Error getting user or creating map");
                }
            }
        });

    }
}
