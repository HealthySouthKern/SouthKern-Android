package com.eddierangel.southkern.android.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.CalendarAuthorization;
import com.eddierangel.southkern.android.utils.EventAdapter;
import com.eddierangel.southkern.android.utils.EventManager;
import com.eddierangel.southkern.android.utils.EventParser;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

// TODO: Add Documentation to Public Interface
public class SubmissionManager extends AppCompatActivity {
    private static final String TAG = "SubmissionManager";
    private Event eventToView;
    private Calendar service;
    private List<Event> eventList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private Dialog editEventDialogue;
    private RecyclerView recyclerView;
    private EventAdapter mAdapter;
    private EventDateTime eventStart = new EventDateTime();
    private EditText editEventName, editEventLocation, editEventDescription;
    private TextView editEventTitle, approveText, deleteText;
    private LinearLayout emptyListText, submissionListView;
    private TimePickerDialog mTimePicker;
    private int year, month, day, endHour, endMinute, startHour, startMinute = 0;
    private Boolean endTimeSelected;
    private String CALENDAR_ID;
    private Button selectTimeButtonEdit, selectStartTimeButtonEdit, editSubmissionButton, finishManagingButton;
    private String APPLICATION_NAME;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public Event getEventInfoFromDialogue() {
        java.util.Calendar startCalendar = java.util.Calendar.getInstance();
        java.util.Calendar endCalendar = java.util.Calendar.getInstance();

        if (startHour != 0) {
            try {
                String date = eventToView.getStart().getDate().toString();
                Log.i(TAG, "getEventInfoFromDialogue: getStart: datetime: " + date);
                year = Integer.parseInt(date.substring(0, 4));
                month = Integer.parseInt(date.substring(5, 7));
                day = Integer.parseInt(date.substring(7, 9));
                startCalendar.set(year, month, day , startHour, startMinute);
            } catch(Exception e) {
                Log.e(TAG, "getEventInfoFromDialogue: edit sub parse time err: " + e);
                e.printStackTrace();
            }
        }

        if (endHour != 0) {
            try {
                String date = eventToView.getEnd().getDate().toString();
                Log.i(TAG, "getEventInfoFromDialogue: getEnd: datetime: " + date);
                year = Integer.parseInt(date.substring(0, 4));
                month = Integer.parseInt(date.substring(5, 7));
                day = Integer.parseInt(date.substring(7, 9));
                endCalendar.set(year, month, day , endHour, endMinute);
            } catch(Exception e) {
                Log.e("getEventInfoFromDialogue: edit sub parse time err", "" + e);
                e.printStackTrace();
            }
        }

        editEventName = (EditText) editEventDialogue.findViewById(R.id.event_name_edit);
        editEventLocation = (EditText) editEventDialogue.findViewById(R.id.event_location_edit);
        editEventDescription = (EditText) editEventDialogue.findViewById(R.id.event_description_edit);

        String newEventName = editEventName.getText().toString();
        String newEventLocation = editEventLocation.getText().toString();
        String newEventDescription = editEventDescription.getText().toString();

        DateTime newEventEndTime = new DateTime(0);
        if (endHour != 0) {
            newEventEndTime = new DateTime(new Timestamp(endCalendar.getTimeInMillis()));
        }

        DateTime newEventStartTime = new DateTime(0);
        if (startHour != 0) {
            newEventStartTime = new DateTime(new Timestamp(startCalendar.getTimeInMillis()));
        }

        Event dummyEvent = new Event();
        dummyEvent.setSummary(newEventName);
        dummyEvent.setLocation(newEventLocation);
        dummyEvent.setDescription(newEventDescription);
        Log.i(TAG, "getEventInfoFromDialogue: eventendhour: " + endHour + "");
        if (endHour != 0) {
            dummyEvent.setEnd(new EventDateTime().setDateTime(newEventEndTime));
        } else {
            dummyEvent.setEnd(new EventDateTime().setDateTime(eventToView.getEnd().getDate()));
        }

        if (startHour != 0) {
            dummyEvent.setStart(new EventDateTime().setDateTime(newEventStartTime));
        } else {
            dummyEvent.setStart(new EventDateTime().setDateTime(eventToView.getStart().getDate()));
        }
        return dummyEvent;
    }

    public int convertTimeType(int time) {
        if (time > 12) {
            return time - 12;
        }
        else if (time < 12 && time != 0) {
            return time;
        }
        else {
            return 12;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submission_manager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.submission_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Event Submissions");

        APPLICATION_NAME = SubmissionManager.this.getApplicationContext().getString(R.string.app_name);
        CALENDAR_ID = SubmissionManager.this.getApplicationContext().getString(R.string.google_calendar_id);

        Credential credential;
        CalendarAuthorization calAuth = new CalendarAuthorization();
        final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport(); // GoogleNetHttpTransport.newTrustedTransport();
        Context mContext = SubmissionManager.this.getApplicationContext();

        HashMap params = new HashMap();
        params.put("transport", HTTP_TRANSPORT);
        params.put("context", mContext);
        calAuth.execute(params);

        try {
            credential = calAuth.get();
            service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch(Exception e) {
            Log.e(TAG, "onCreate: get credential: Failed to create credential: " + e);
            e.printStackTrace();
        }

        submissionListView = (LinearLayout) findViewById(R.id.submission_list_view);
        emptyListText = (LinearLayout) findViewById(R.id.empty_submissions);

        editEventDialogue = new Dialog(SubmissionManager.this);
        editEventDialogue.setContentView(R.layout.dialogue_edit_event);
        approveText = (TextView) editEventDialogue.findViewById(R.id.approve_text_view);
        deleteText = (TextView) editEventDialogue.findViewById(R.id.delete_text_view);

        mDatabase = FirebaseDatabase.getInstance().getReference().getRoot();
        mDatabase.child("submissions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i(TAG, "onCreate: onDataChange: databaseval: " + dataSnapshot.getValue());
                eventList.clear();
                for (DataSnapshot mSnapshot : dataSnapshot.getChildren()) {
                    Log.i(TAG, "onCreate: getChildren: databaseval2: " + mSnapshot.getValue());
                    Event event = EventParser.parseSingleEvent(mSnapshot.getValue());
                    eventList.add(event);
                }
                if (!eventList.isEmpty()) {
                    Log.i(TAG, "onCreate: isEmpty (210): entered if not empty: 123");
                    emptyListText.setVisibility(View.GONE);
                    submissionListView.setVisibility(View.VISIBLE);
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (eventList.isEmpty()) {
            Log.i(TAG, "onCreate: isEmpty (224): entered if sub: 123");
            emptyListText.setVisibility(View.VISIBLE);
            submissionListView.setVisibility(View.GONE);
        }

        mTimePicker = new TimePickerDialog(SubmissionManager.this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                if (endTimeSelected) {
                    endHour = selectedHour;
                    endMinute = selectedMinute;
                    selectTimeButtonEdit.setText(String.format(endHour - 12 > 0 ? "Ending at %d:%02d PM" : "Ending at %d:%02d AM", convertTimeType(endHour), endMinute));
                } else {
                    startHour = selectedHour;
                    startMinute = selectedMinute;
                    selectStartTimeButtonEdit.setText(String.format(startHour - 12 > 0 ? "Starting at %d:%02d PM" : "Starting at %d:%02d AM", convertTimeType(startHour), startMinute));
                }
            }
        }, 0, 0, false); // No 24 hour time
        selectTimeButtonEdit = (Button) editEventDialogue.findViewById(R.id.button_select_time_edit);
        selectTimeButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endTimeSelected = true;
                mTimePicker.setTitle("Select Event End Time");
                mTimePicker.show();
            }
        });

        selectStartTimeButtonEdit = (Button) editEventDialogue.findViewById(R.id.button_select_start_time_edit);
        selectStartTimeButtonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endTimeSelected = false;
                mTimePicker.setTitle("Select Event Start Time");
                mTimePicker.show();
            }
        });

        editSubmissionButton = (Button) editEventDialogue.findViewById(R.id.button_edit_event);
        editSubmissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Event dummyEvent = getEventInfoFromDialogue();

                mDatabase.child("submissions").child(eventToView.getSummary()).removeValue();
                mDatabase.child("submissions").child(dummyEvent.getSummary()).setValue(dummyEvent);
                mAdapter.notifyDataSetChanged();

                //year = 0; month = 0; day = 0; endHour = 0; endMinute = 0; startHour = 0; startMinute = 0;
                selectStartTimeButtonEdit.setVisibility(View.GONE);
                approveText.setVisibility(View.GONE);
                editEventDialogue.dismiss();
            }
        });


        recyclerView = (RecyclerView) findViewById(R.id.submission_recycler_view);

        mAdapter = new EventAdapter(eventList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mAdapter.setOnItemClickListener(new EventAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                Log.d(TAG, "onCreate: onItemClick: onItemClick position: " + position);
                Event event = eventList.get(position);
                Log.i(TAG, "onCreate: get event: eventclick: " + event);
                eventToView = event;
                eventStart = event.getStart();
                DateFormat df = new SimpleDateFormat("h:mm a");
                String sdtEnd, sdtStart;
                if (event.getEnd().getDate() != null) {
                    sdtEnd = df.format(new Date(event.getEnd().getDate().getValue()));
                } else {
                    sdtEnd = df.format(new Date(event.getEnd().getDateTime().getValue()));
                }

                if (event.getStart().getDate() != null) {
                    sdtStart = df.format(new Date(event.getStart().getDate().getValue()));
                } else {
                    sdtStart = df.format(new Date(event.getStart().getDateTime().getValue()));
                }

                editEventTitle = (TextView) editEventDialogue.findViewById(R.id.event_title_edit);
                editEventName = (EditText) editEventDialogue.findViewById(R.id.event_name_edit);
                editEventLocation = (EditText) editEventDialogue.findViewById(R.id.event_location_edit);
                editEventDescription = (EditText) editEventDialogue.findViewById(R.id.event_description_edit);
                selectTimeButtonEdit = (Button) editEventDialogue.findViewById(R.id.button_select_time_edit);

                editEventName.setText(event.getSummary());
                editEventLocation.setText(event.getLocation());
                editEventDescription.setText(event.getDescription());
                selectTimeButtonEdit.setText("Ending at " + sdtEnd);
                selectStartTimeButtonEdit.setText("Starting at " + sdtStart);

                editEventTitle.setText("View Submission");
                editEventTitle.setTextSize(20);

                selectStartTimeButtonEdit.setVisibility(View.VISIBLE);
                approveText.setVisibility(View.VISIBLE);
                editEventDialogue.show();
            }

            @Override
            public void onItemLongClick(int position, View v) {
                Log.d(TAG, "onCreate: onItemLongClick: pos = " + position);
            }
        });

        approveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventManager.insertEvent insertEvent = new EventManager.insertEvent(SubmissionManager.this);

                Event dummyEvent = getEventInfoFromDialogue();

                HashMap params = new HashMap();
                params.put("service", service);
                params.put("event", dummyEvent);
                params.put("bool", true);
                params.put("calendarID", CALENDAR_ID);

                try {
                    insertEvent.execute(params);
                } catch(Exception e) {
                    Log.e(TAG, "onCreate: insertEvent: error inserting event: " + e);
                    e.printStackTrace();
                }
                mDatabase.child("submissions").child(eventToView.getSummary()).removeValue();
                editEventDialogue.dismiss();
            }
        });

        deleteText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (eventToView != null) {
                    AlertDialog.Builder builder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder = new AlertDialog.Builder(SubmissionManager.this, android.R.style.Theme_Material_Dialog_Alert);
                    } else {
                        builder = new AlertDialog.Builder(SubmissionManager.this);
                    }
                    builder.setTitle("Delete submission")
                            .setMessage("Are you sure you want to delete this submission?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mDatabase.child("submissions").child(eventToView.getSummary()).removeValue();
                                    editEventDialogue.dismiss();
                                    mAdapter.notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    throw new NullPointerException("eventToView does not exist");
                }
            }
        });

        finishManagingButton = (Button) findViewById(R.id.finish_submissions);
        finishManagingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SubmissionManager.this, CalendarActivity.class);
                startActivity(intent);
                finish();
            }
        });

        recyclerView.setAdapter(mAdapter);
    }
}

