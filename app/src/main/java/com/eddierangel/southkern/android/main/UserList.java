package com.eddierangel.southkern.android.main;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.AddressAdapter;
import com.eddierangel.southkern.android.utils.AlertAdapter;
import com.eddierangel.southkern.android.utils.EventAdapter;
import com.eddierangel.southkern.android.utils.FeedAdapter;
import com.eddierangel.southkern.android.utils.InternetCheck;
import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.eddierangel.southkern.android.utils.ReconnectionManager;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO: Add Documentation to Public Interface
public class UserList extends AppCompatActivity {

    private FirebaseFunctions mFunctions;
    private List<Object> userList = new ArrayList<>();
    private List<Object> originalUserList = new ArrayList<>();
    private List<Object> sortedOrgList = new ArrayList<>();
    private List<Object> sortedNameList = new ArrayList<>();
    private Toolbar mToolbar;
    private ImageButton viewAlertButton;
    private List<Event> alertEvents = new ArrayList<>();
    private ImageView sortName, sortOrganization;
    private ImageButton backButton;
    private AlertAdapter alertAdapter;
    private RecyclerView alertRecyclerView;
    private EditText organizationSearch;
    private long twoWeekTime = 1209600000;
    private NavigationView alertNavView;
    private LinearLayout userListControls;
    private DatabaseReference mDatabase;
    private Boolean sortAZ = true, sortOrg = true;
    private String firstTempHolder, secondTempHolder;
    private RecyclerView addressRecyclerView;
    private RecyclerView.Adapter mAdapter;

    private void sortUserListOrganization(List<Object> userList) {
        Collections.sort(userList, new Comparator<Object>() {
            @Override
            public int compare(Object firstUserObj, Object secondUserObj) {
                HashMap firstUser = (HashMap) firstUserObj;
                HashMap secondUser = (HashMap) secondUserObj;

                firstTempHolder = (String) firstUser.get("user_organization");
                secondTempHolder = (String) secondUser.get("user_organization");


                if (!firstTempHolder.isEmpty() && !secondTempHolder.isEmpty()) {


                    int res = String.CASE_INSENSITIVE_ORDER.compare(firstTempHolder, secondTempHolder);
                    return (res != 0) ? res : firstTempHolder.compareTo(secondTempHolder);

                } else if (!firstTempHolder.isEmpty()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
    }

    private Comparator<Object> getMoveToFrontComparator(final String part) {
        return new Comparator<Object>() {

            @Override
            public int compare(Object firstUserObj, Object secondUserObj) {
                HashMap firstUser = (HashMap) firstUserObj;
                HashMap secondUser = (HashMap) secondUserObj;

                firstTempHolder = (String) firstUser.get("user_name");
                secondTempHolder = (String) secondUser.get("user_name");
                if (firstTempHolder != null && secondTempHolder != null) {

                    boolean containsFirst = firstTempHolder.toLowerCase().contains(part.toLowerCase());
                    boolean containsSecond = secondTempHolder.toLowerCase().contains(part.toLowerCase());

                    // Match vs No-Match: Match has the priority
                    if (containsFirst && containsSecond)
                        return firstTempHolder.compareTo(secondTempHolder);
                    if (containsFirst && !containsSecond)
                        return -1;
                    if (!containsFirst && containsSecond)
                        return 1;

                    // Match vs Match or No-Match vs No-Match: no sorting is needed
                    return 0;
                } else if (!firstTempHolder.isEmpty()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };
    }

    private void sortUserListNickname(List<Object> userList) {
        Collections.sort(userList, new Comparator<Object>() {
            @Override
            public int compare(Object firstUserObj, Object secondUserObj) {
                HashMap firstUser = (HashMap) firstUserObj;
                HashMap secondUser = (HashMap) secondUserObj;

                firstTempHolder = (String) firstUser.get("user_name");
                secondTempHolder = (String) secondUser.get("user_name");

                int res = String.CASE_INSENSITIVE_ORDER.compare(firstTempHolder, secondTempHolder);
                return (res != 0) ? res : firstTempHolder.compareTo(secondTempHolder);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new InternetCheck(new InternetCheck.Consumer() {
            @Override
            public void accept(Boolean internet) {
                if (!internet) {
                    Intent intent = new Intent(UserList.this, ReconnectionManager.class);
                    startActivity(intent);
                    finish();
                } else {

                    setContentView(R.layout.activity_address_book);
                    mToolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
                    setSupportActionBar(mToolbar);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);

                    sortName = (ImageView) findViewById(R.id.sort_name);
                    sortOrganization = (ImageView) findViewById(R.id.sort_group);
                    backButton = (ImageButton) findViewById(R.id.menu_button_back);
                    organizationSearch = (EditText) findViewById(R.id.organization_search);
                    userListControls = (LinearLayout) findViewById(R.id.user_list_controls);
                    alertNavView = (NavigationView) findViewById(R.id.nav_view_alerts);


                    mFunctions = FirebaseFunctions.getInstance();
                    mDatabase = FirebaseDatabase.getInstance().getReference().getRoot();

                    mDatabase.child("southkernUsers").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // Success
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                userList.add(child.getValue());
                            }
                            originalUserList = userList;

                            sortedNameList = userList;
                            sortedOrgList = userList;

                            sortUserListNickname(sortedNameList);
                            sortUserListOrganization(sortedOrgList);


                            userListControls.setVisibility(View.VISIBLE);

                            addressRecyclerView = (RecyclerView) findViewById(R.id.user_recycler_view);
                            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
                            addressRecyclerView.setLayoutManager(mLayoutManager);
                            addressRecyclerView.setItemAnimator(new DefaultItemAnimator());

                            mAdapter = new AddressAdapter(userList, UserList.this.getApplicationContext());
                            addressRecyclerView.setAdapter(mAdapter);

                            ((AddressAdapter) mAdapter).setOnItemClickListener(new AddressAdapter.ClickListener() {
                                @Override
                                public void onItemClick(int position, View v) {
                                    HashMap user = (HashMap) userList.get(position);
                                    Intent intent = new Intent(UserList.this, ViewProfile.class);
                                    intent.putExtra("userId", (String) user.get("user_id"));
                                    startActivity(intent);
                                }

                                @Override
                                public void onItemLongClick(int position, View v) {
                                    Log.d("onItemLongClick pos = ", "" + position);
                                }

                            });

                            mDatabase.child("statusUpdates").addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                    for (DataSnapshot mSnapshot : dataSnapshot.getChildren()) {
                                        HashMap statusObj = (HashMap) mSnapshot.getValue();
                                        Event tempEvent = new Event();
                                        tempEvent.setDescription((String) statusObj.get("text"));
                                        tempEvent.setSummary("Status update");

                                        EventDateTime dummyTime = new EventDateTime();
                                        Long dateTime = Long.parseLong(statusObj.get("createdAt").toString());
                                        DateTime createdAtTime = new DateTime(dateTime);
                                        dummyTime.setDate(createdAtTime);
                                        tempEvent.setStart(dummyTime);

                                        if (!alertEvents.contains(tempEvent) && tempEvent.getStart().getDate().getValue() > (twoWeekTime / 2)) {
                                            alertEvents.add(tempEvent);
                                        }

                                    }

                                    Collections.reverse(alertEvents);

                                    mAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    sortName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            userList = sortedNameList;

                            Collections.reverse(userList);

                            mAdapter.notifyDataSetChanged();
                        }
                    });

                    sortOrganization.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            userList = sortedOrgList;

                            Collections.reverse(userList);

                            mAdapter.notifyDataSetChanged();
                        }
                    });

                    organizationSearch.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    });

                    organizationSearch.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            final String searchTerm = editable.toString().toLowerCase();
                            userList = originalUserList;

                            Collections.sort(userList, getMoveToFrontComparator(searchTerm));

                            mAdapter.notifyDataSetChanged();
                        }
                    });

                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(UserList.this, MainActivity.class);
                            startActivity(intent);
                        }
                    });

                    alertRecyclerView = (RecyclerView) findViewById(R.id.alert_recycler_view);
                    RecyclerView.LayoutManager mLayoutAlertManager = new LinearLayoutManager(getApplicationContext());
                    alertRecyclerView.setLayoutManager(mLayoutAlertManager);
                    alertRecyclerView.setItemAnimator(new DefaultItemAnimator());
                    alertAdapter = new AlertAdapter(alertEvents);
                    alertRecyclerView.setAdapter(alertAdapter);

                    viewAlertButton = (ImageButton) findViewById(R.id.alert_view_button);
                    viewAlertButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertNavView.setVisibility(alertNavView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                            viewAlertButton.setBackgroundColor(alertNavView.getVisibility() == View.VISIBLE ? Color.DKGRAY : Color.TRANSPARENT);

                        }
                    });
                }
            }
        });
    }
}
