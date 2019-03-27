package com.eddierangel.southkern.android.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.ImageUtils;
import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;


// TODO: Add Documentation to Public Interface
public class ViewOwnProfile extends AppCompatActivity {

    private static final String TAG = "ViewOwnProfile";
    private ImageButton backButton;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_own_profile);

        mDatabase = FirebaseDatabase.getInstance().getReference().getRoot();

        backButton = (ImageButton) findViewById(R.id.menu_button_back);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        String name = null;
        String organization = null;
        String position = null;
        String profileImageUrl = null;

        final HashMap<String, String> user = PreferenceUtils.getUser(ViewOwnProfile.this.getApplicationContext());
        try {
            name = user.get("user_name");
            organization = user.get("user_organization");
            position = user.get("user_position");
            profileImageUrl = user.get("user_picture");
        }
        catch (Exception e) {
            Log.e(TAG, "onCreate: getMetaData: User Missing Metadata");
        }


        final HashMap<String, String> data = new HashMap<String, String>();

        ImageView userProfileImage = (ImageView)findViewById(R.id.image_user_profile);
        try {
            if (profileImageUrl != null) {
                ImageUtils.displayImageFromUrl(ViewOwnProfile.this, profileImageUrl, userProfileImage, new RequestListener() {
                    @Override
                    public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
                        Log.e("error fetching image", "" + e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Object resource, Object model, Target target, boolean isFromMemoryCache, boolean isFirstResource) {
                        return false;
                    }
                });
            }
        }
        catch (Exception e) {
            Log.e(TAG, "onCreate: load profileImage: null profileImageUrl");
        }

        EditText nameEditText = (EditText)findViewById(R.id.text_user_real_name);
        nameEditText.setText(name);
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable name) {
                data.put("user_name", name.toString());
            }
        });

        EditText organizationEditText = (EditText)findViewById(R.id.text_user_organization);
        organizationEditText.setText(organization);
        organizationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable organization) {
                data.put("user_organization", organization.toString());
            }
        });

        EditText positionEditText = (EditText)findViewById(R.id.text_user_position);
        positionEditText.setText(position);
        positionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable position) {
                data.put("user_position", position.toString());
            }
        });


        Button applyChangesButton = (Button)findViewById(R.id.apply_changes_button);
        applyChangesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateMetaData(data, user);
                Toast toast = Toast.makeText(getApplicationContext(), "Changes Applied", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

    }

    // hashMap contains changed values, user contains original values.
    // loop through hashMap and set corresponding values to the newly edited values.
    protected void updateMetaData(HashMap<String, String> hashMap, HashMap<String, String> user) {
        String firebaseUID = user.get("uid");

        if (firebaseUID != null) {
            for (String key : hashMap.keySet()) {
                mDatabase.child("southkernUsers").child(firebaseUID).child(key).setValue(hashMap.get(key));
            }

            // Once the fields have been updated, optimistically update the user stored in shared preferences so that
            // the app has the updated user. Before, you could update your profile, exit and return, and edited values would
            // not show up despite the values being updated server side.
            mDatabase.child("southkernUsers").child(firebaseUID).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    PreferenceUtils.setUser(ViewOwnProfile.this.getApplicationContext(), (HashMap) dataSnapshot.getValue());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }


}
