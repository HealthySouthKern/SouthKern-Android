package com.eddierangel.southkern.android.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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


public class ViewProfile extends AppCompatActivity {

    private FirebaseFunctions mFunctions;
    private Map<String, String> userData;

    private Task<Object> fetchUserwithUserID(String firebaseToken, String userID) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("userID", userID);
        data.put("firebaseToken", firebaseToken);

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

        String userId = getIntent().getStringExtra("userId");
        User user = SendBird.getCurrentUser();
        String userID = user.getUserId();

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
                    userData = (Map<String, String>) user;
                }
                catch (Exception e) {
                    Log.e("InsideTaskComplete", "Error getting user or creating map");
                }
            }
        });


        /* COMMENTED OUT BECAUSE CURRENTLY NOT FUNCTIONAl
        String userPicture = userData.get("profileImageUrl");
        String nickname = userData.get("nickname");
        String name = userData.get("name");
        String organization = userData.get("organization");
        String position = userData.get"position");
        String role = userData.get("role");
        */

        ImageView profileImage = (ImageView)findViewById(R.id.profile_image);
      //if (!userPicture.isEmpty()){
      //    Picasso.get().load(userPicture).into(profileImage);
      //} COMMENTED OUT BECAUSE CURRENTLY NOT FUNCTIONAl

        TextView userNickname = (TextView)findViewById(R.id.text_user_nickname);
        //userNickname.setText(nickname); COMMENTED OUT BECAUSE CURRENTLY NOT FUNCTIONAl

        TextView userName = (TextView)findViewById(R.id.text_user_real_name);
        //userName.setText(name); COMMENTED OUT BECAUSE CURRENTLY NOT FUNCTIONAl

        TextView userOrganization = (TextView)findViewById(R.id.text_user_organization);
        //userOrganization.setText(organization); COMMENTED OUT BECAUSE CURRENTLY NOT FUNCTIONAl

        TextView userPosition = (TextView)findViewById(R.id.text_user_position);
        //userPosition.setText(position); COMMENTED OUT BECAUSE CURRENTLY NOT FUNCTIONAl

        TextView userRole = (TextView)findViewById(R.id.text_user_role);
        //userRole.setText(role); COMMENTED OUT BECAUSE CURRENTLY NOT FUNCTIONAl

    }
}
