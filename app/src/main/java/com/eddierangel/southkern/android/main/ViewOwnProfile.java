package com.eddierangel.southkern.android.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eddierangel.southkern.android.R;
import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;
import com.sendbird.android.User.MetaDataHandler;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewOwnProfile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_own_profile);

        String nickname = null;
        String name = null;
        String organization = null;
        String position = null;
        String role = null;
        String profileImageUrl = null;

        final User user = SendBird.getCurrentUser();
        try {
            nickname = user.getMetaData("user_nickname");
            name = user.getMetaData("user_name");
            organization = user.getMetaData("user_organization");
            position = user.getMetaData("user_position");
            role = user.getMetaData("user_role");
            profileImageUrl = user.getMetaData("user_picture)");
        }
        catch (Exception e) {
            Log.e("ViewOwnProfile", "User Missing Metadata");
        }


        final HashMap<String, String> data = new HashMap<String, String>();

        ImageView userProfileImage = (ImageView)findViewById(R.id.image_user_profile);
        try {
            if (!profileImageUrl.isEmpty()) {
                Picasso.get().load(profileImageUrl).into(userProfileImage);
            }
        }
        catch (Exception e) {
            Log.e("ViewOwnProfile", "null profileImageUrl");
        }

        EditText nicknameEditText = (EditText)findViewById(R.id.text_user_nickname);
        nicknameEditText.setText(nickname);
        nicknameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable nickname) {
                data.put("user_nickname", nickname.toString());
            }
        });

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

        EditText roleEditText = (EditText)findViewById(R.id.text_user_role);
        roleEditText.setText(role);
        roleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable role) {
                data.put("user_role", role.toString());
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

    protected void updateMetaData(HashMap<String, String> hashMap, User user) {
        user.updateMetaData(hashMap, new User.MetaDataHandler() {
            @Override
            public void onResult(Map<String, String> map, SendBirdException e) {
                if (e != null) {
                    return;
                }
            }
        });
    }


}
