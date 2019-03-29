package com.eddierangel.southkern.android.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.ImageUtils;
import com.eddierangel.southkern.android.utils.LogUtility;
import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.sendbird.android.SendBird;
import com.sendbird.android.User;

import java.util.HashMap;
import java.util.Map;


// TODO: Add Documentation to Public Interface
public class ViewProfile extends AppCompatActivity {

    private static final String TAG = "ViewProfile";
    private FirebaseFunctions mFunctions;
    private HashMap<String, String> userData;
    private Map<String, String> userMetaData;
    private ImageButton backButton;
    private DatabaseReference mDatabase;

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
                    public Object then(@NonNull Task<HttpsCallableResult> task) {

                        return task.getResult().getData();
                    }
                });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        mDatabase = FirebaseDatabase.getInstance().getReference().getRoot();

        String userID = getIntent().getStringExtra("userId");
        User user = SendBird.getCurrentUser();

        // Initialize mFunctions
        mFunctions = FirebaseFunctions.getInstance();
        backButton = findViewById(R.id.menu_button_back);

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
                    userData = (HashMap<String, String>) user;
                    mDatabase.child("southkernUsers").child(userData.get("uid")).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            userMetaData = (Map<String, String>) dataSnapshot.getValue();

                            // Moved settexts inside of callback of fetchUserwithID since we need to use this data
                            // once the function returns.

                            // metadata is a nested object inside the user object which contains the useful profile data.

                            String userPicture = userMetaData.get("user_picture");
                            String name = userMetaData.get("user_name");
                            String organization = userMetaData.get("user_organization");
                            String position = userMetaData.get("user_position");
                            String role = userMetaData.get("user_type");

                            final ImageView profileImage = findViewById(R.id.profile_image);
                            if (userPicture != null) {
                                if (!userPicture.isEmpty()) {
                                    ImageUtils.displayImageFromUrl(ViewProfile.this, userPicture, profileImage);
                                }
                            }

                            TextView userName = findViewById(R.id.text_user_real_name);
                            userName.setText(name);

                            TextView userOrganization = findViewById(R.id.text_user_organization);
                            userOrganization.setText(organization);

                            TextView userPosition = findViewById(R.id.text_user_position);
                            userPosition.setText(position);

                            TextView userRole = findViewById(R.id.text_user_role);
                            if (role != null) {
                                userRole.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                catch (Exception e) {
                    LogUtility.e(TAG, "onCreate: InsideTaskComplete: Error getting user or creating map");
                }
            }
        });

    }
}
