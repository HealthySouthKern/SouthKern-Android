package com.eddierangel.southkern.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eddierangel.southkern.android.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;

// TODO: Add Documentation to Public Interface
public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.MyViewHolder> {
    private List<Object> userList;
    private SimpleDateFormat baseFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
    private String userProfileUrl;
    private String userName;
    private String userEmailText;
    private String userOrganizationText;
    private Context context;
    private static ClickListener clickListener;

    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView userNickname, userEmail, isLoggedIn, userOrganization;
        public ImageView userProfilePic;

        public MyViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            userNickname = (TextView) view.findViewById(R.id.user_nick_name);
            userEmail = (TextView) view.findViewById(R.id.user_email);
            isLoggedIn = (TextView) view.findViewById(R.id.is_online);
            userOrganization = (TextView) view.findViewById(R.id.user_organization);
            userProfilePic = (ImageView) view.findViewById(R.id.user_profile_pic);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(), v);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onItemLongClick(getAdapterPosition(), v);
            return false;
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        AddressAdapter.clickListener = clickListener;
    }

    public AddressAdapter(List<Object> userList, Context context) {
        this.userList = userList;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_single, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final HashMap user = (HashMap) userList.get(position);
        final HashMap userMetaData = (HashMap) user.get("metadata");
        userProfileUrl = (String) user.get("profile_url");
        userName = (String) user.get("nickname");
        userEmailText = (String) user.get("user_id");
        userOrganizationText = (String) userMetaData.get("user_organization");

        ImageUtils.displayRoundImageFromUrl(context, userProfileUrl, holder.userProfilePic);
        holder.userNickname.setText(userName);
        holder.userEmail.setText(userEmailText);
        if (userOrganizationText != null) {
            if (!userOrganizationText.equals("")) {
                // substrings are to capitalize first letter of the string
                holder.userOrganization.setText(userOrganizationText.substring(0, 1).toUpperCase() + userOrganizationText.substring(1));
            } else {
                holder.userOrganization.setText("");
            }
        } else {
            holder.userOrganization.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }
}
