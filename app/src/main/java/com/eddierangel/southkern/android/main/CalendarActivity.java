package com.eddierangel.southkern.android.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.utils.AlertAdapter;
import com.eddierangel.southkern.android.utils.CalendarAuthorization;
import com.eddierangel.southkern.android.utils.EventManager;
import com.eddierangel.southkern.android.utils.EventParser;
import com.eddierangel.southkern.android.utils.InternetCheck;
import com.eddierangel.southkern.android.utils.LogUtility;
import com.eddierangel.southkern.android.utils.PreferenceUtils;
import com.eddierangel.southkern.android.utils.ReconnectionManager;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.sendbird.android.SendBird;
import com.sendbird.android.User;

import org.w3c.dom.Text;

import java.lang.reflect.Field;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

// TODO: Add Documentation to Public Interface
public class CalendarActivity extends AppCompatActivity {


    private List<Event> events;

    private static final String TAG = "CalendarActivity";

    private List<Event> listOfSubmissions = new ArrayList<>();
    private boolean editTimeButtonClicked;
    private WeekView mWeekView;
    private ProgressBar progressBarCalendar;
    private FirebaseFunctions mFunctions;
    private ImageView mLogo;
    private Calendar service;
    private int hour, minute = 0;
    private java.util.Calendar timePressed, eventStart;
    private TimePickerDialog mTimePicker, mEditTimePicker;
    private TextView deleteTextView, createEventTitle, viewEventName, viewEventLocation, viewEventDescription;
    private EditText eventName, eventLocation, eventDescription, editEventName, editEventLocation, editEventDescription;
    private ImageButton menuButton;
    private Button createEventButton, selectTimeButton, editEventButton, selectTimeButtonEdit, submissionButton, closeViewDialogueButton;
    private Dialog createEventDialogue, editEventDialogue, viewEventDialogue;
    private Event eventToView = new Event();
    private String CALENDAR_ID;
    private DatabaseReference mDatabase;
    private String APPLICATION_NAME;
    private long twoWeekTime = 1209600000;
    private NavigationView alertNavView;
    private AlertAdapter alertAdapter;
    private RecyclerView alertRecyclerView;
    private ImageButton viewAlertButton;
    private List<Event> alertEvents = new ArrayList<>();
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private Task<List<Event>> getCalendarEvents(String firebaseToken) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("firebaseToken", firebaseToken);

        return mFunctions
                .getHttpsCallable("getCalendarEvents")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, List<Event>>() {
                    @Override
                    public List<Event> then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        return EventParser.parse((List) task.getResult().getData());
                    }
                });
    }

    private List<Event> getEvents(int newYear, int newMonth) {
        if (events != null) {
            List<Event> listOfEvents = events;
            if (!listOfSubmissions.isEmpty()) {
                listOfEvents.addAll(listOfSubmissions);
            }
            List<Event> eventsWithinRange = new ArrayList<>();
            String dateHolder;
            int eventMonth, eventYear;

            for (int i = 0 ; i < listOfEvents.size() ; i++) {
                Event event = listOfEvents.get(i);
                if (event.getStart().getDate() != null) {
                    dateHolder = event.getStart().getDate().toString();
                } else {
                    dateHolder = event.getStart().getDateTime().toString();
                }

                // event date format is yyyy-MM-dd HH:mm:ss.SSS, knowing this, we can parse month and year from the string
                eventMonth = Integer.parseInt(dateHolder.substring(5, 7));
                eventYear = Integer.parseInt(dateHolder.substring(0, 4));

                if (eventMonth == newMonth && eventYear == newYear && !eventsWithinRange.contains(event)) {
                    eventsWithinRange.add(event);
                }
            }

            return eventsWithinRange;
        } else {
            return null;
        }
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

        new InternetCheck(new InternetCheck.Consumer() {
            @Override
            public void accept(Boolean internet) {
                if (!internet) {
                    Intent intent = new Intent(CalendarActivity.this, ReconnectionManager.class);
                    startActivity(intent);
                    finish();
                } else {

                    mDatabase = FirebaseDatabase.getInstance().getReference().getRoot();

                    final Map<String, String> userData = PreferenceUtils.getUser(CalendarActivity.this.getApplicationContext());

                    CALENDAR_ID = CalendarActivity.this.getApplicationContext().getString(R.string.google_calendar_id);
                    APPLICATION_NAME = CalendarActivity.this.getApplicationContext().getString(R.string.app_name);

                    createEventDialogue = new Dialog(CalendarActivity.this);
                    editEventDialogue = new Dialog(CalendarActivity.this);
                    viewEventDialogue = new Dialog(CalendarActivity.this);

                    LogUtility.i(TAG, "OnCreate: InternetCheck: userdata: " + userData);

                    if (userData.get("user_type") != null) {
                        if (userData.get("user_type").equals("admin")) {
                            Credential credential;
                            CalendarAuthorization calAuth = new CalendarAuthorization();
                            final NetHttpTransport HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport(); // GoogleNetHttpTransport.newTrustedTransport();
                            Context mContext = CalendarActivity.this.getApplicationContext();

                            HashMap params = new HashMap();
                            params.put("transport", HTTP_TRANSPORT);
                            params.put("context", mContext);
                            calAuth.execute(params);

                            try {
                                credential = calAuth.get();
                                service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                                        .setApplicationName(APPLICATION_NAME)
                                        .build();
                            } catch (Exception e) {
                                LogUtility.e(TAG, "OnCreate: InternetCheck: credential failed: " + e);
                                e.printStackTrace();
                            }
                        }
                    }

                    setContentView(R.layout.activity_calendar);
                    mFunctions = FirebaseFunctions.getInstance();
                    mLogo = (ImageView) findViewById(R.id.image_calendar_logo);

                    Toolbar toolbar = (Toolbar) findViewById(R.id.calendar_toolbar);
                    setSupportActionBar(toolbar);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);

                    createEventDialogue.setContentView(R.layout.dialogue_create_event);
                    createEventDialogue.setTitle(userData.get("user_type").equals("admin") ? "Create Event" : "Create Submission");
                    editEventDialogue.setContentView(R.layout.dialogue_edit_event);
                    editEventDialogue.setTitle(userData.get("user_type").equals("admin") ? "Edit Event" : "Edit Submission");
                    viewEventDialogue.setContentView(R.layout.dialogue_view_event);
                    viewEventDialogue.setTitle("View Event");

                    menuButton = (ImageButton) findViewById(R.id.menu_button_calendar);
                    menuButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });

                    submissionButton = (Button) findViewById(R.id.manage_submissions);
                    submissionButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(CalendarActivity.this, SubmissionManager.class);
                            startActivity(intent);
                            finish();
                        }
                    });

                    deleteTextView = (TextView) editEventDialogue.findViewById(R.id.delete_text_view);
                    if (userData.get("user_type").equals("admin"))
                        deleteTextView.setVisibility(View.VISIBLE);
                    deleteTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (eventToView != null) {
                                AlertDialog.Builder builder;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    builder = new AlertDialog.Builder(CalendarActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                                } else {
                                    builder = new AlertDialog.Builder(CalendarActivity.this);
                                }
                                builder.setTitle("Delete event")
                                        .setMessage("Are you sure you want to delete this event?")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                HashMap params = new HashMap();

                                                params.put("service", (Object) service);
                                                params.put("calendarID", CALENDAR_ID);
                                                params.put("id", eventToView.getId());

                                                List<Event> listOfEvents = events;
                                                for (int i = 0; i < listOfEvents.size(); i++) {
                                                    if (listOfEvents.get(i).getId().equals(eventToView.getId())) {
                                                        events.remove(i);
                                                    }
                                                }

                                                mWeekView.notifyDatasetChanged();
                                                editEventDialogue.dismiss();

                                                EventManager.deleteEvent deleteEvent = new EventManager.deleteEvent(CalendarActivity.this);

                                                try {
                                                    deleteEvent.execute(params);
                                                } catch (Exception e) {
                                                    LogUtility.e(TAG, "onCreate: deleteEvent: delete event err: " + e);
                                                    e.printStackTrace();
                                                }
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

                    selectTimeButton = (Button) createEventDialogue.findViewById(R.id.button_select_time);
                    selectTimeButtonEdit = (Button) editEventDialogue.findViewById(R.id.button_select_time_edit);
                    mTimePicker = new TimePickerDialog(CalendarActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                            hour = selectedHour;
                            minute = selectedMinute;
                            if (editTimeButtonClicked) {
                                selectTimeButtonEdit.setText(String.format(hour - 12 > 0 ? "Ending at %d:%02d PM" : "Ending at %d:%02d AM", convertTimeType(hour), minute));
                            } else {
                                selectTimeButton.setText(String.format(hour - 12 > 0 ? "Ending at %d:%02d PM" : "Ending at %d:%02d AM", convertTimeType(hour), minute));
                            }

                        }
                    }, hour, minute, false); // No 24 hour time
                    selectTimeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            editTimeButtonClicked = false;
                            mTimePicker.setTitle("Select Event End Time");
                            mTimePicker.show();
                        }
                    });

                    selectTimeButtonEdit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            editTimeButtonClicked = true;
                            mTimePicker.setTitle("Edit Event End Time");
                            mTimePicker.show();
                        }
                    });

                    createEventButton = (Button) createEventDialogue.findViewById(R.id.button_create_event);
                    createEventButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            eventName = (EditText) createEventDialogue.findViewById(R.id.event_name_create);
                            eventLocation = (EditText) createEventDialogue.findViewById(R.id.event_location_create);
                            eventDescription = (EditText) createEventDialogue.findViewById((R.id.event_description_create));

                            if (hour != 0 && eventName.length() != 0) {
                                String name = eventName.getText().toString();
                                String location = eventLocation.getText().toString();
                                String description = eventDescription.getText().toString();

                                java.util.Calendar calendar = java.util.Calendar.getInstance();
                                calendar.set(timePressed.get(java.util.Calendar.YEAR), timePressed.get(java.util.Calendar.MONTH), timePressed.get(java.util.Calendar.DATE), hour, minute);
                                DateTime endTime = new DateTime(calendar.getTime());
                                DateTime startTime = new DateTime(timePressed.getTime());

                                EventDateTime eventStartTime = new EventDateTime().setDateTime(startTime);
                                EventDateTime eventEndTime = new EventDateTime().setDateTime(endTime);

                                Event dummyEvent = new Event();
                                dummyEvent.setSummary(name);
                                dummyEvent.setDescription(description);
                                dummyEvent.setLocation(location);
                                dummyEvent.setStart(eventStartTime);
                                dummyEvent.setEnd(eventEndTime);

                                // Perform optimistic UI update
                                if (userData.get("user_type").equals("admin")) {
                                    events.add(dummyEvent);
                                    mWeekView.notifyDatasetChanged();
                                }

                                HashMap params = new HashMap();
                                params.put("service", (Object) service);
                                params.put("event", (Object) dummyEvent);
                                params.put("calendarID", CALENDAR_ID);

                                if (userData.get("user_type").equals("admin")) {
                                    EventManager.insertEvent insertEvent = new EventManager.insertEvent(CalendarActivity.this);

                                    try {
                                        insertEvent.execute(params);
                                    } catch (Exception e) {
                                        LogUtility.e(TAG, "onCreate: insertEvent: insert event err: " + e);
                                        e.printStackTrace();
                                    }
                                } else {
                                    Event.Creator dummyCreator = new Event.Creator();
                                    dummyCreator.setDisplayName(userData.get("user_name"));
                                    dummyEvent.setCreator(dummyCreator);
                                    LogUtility.i(TAG, "onCreate: setCreator: created sub: " + dummyCreator);
                                    mDatabase.child("submissions").child(name).setValue(dummyEvent).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //Failed
                                            LogUtility.i(TAG, "onCreate: database: " + e);
                                            e.printStackTrace();
                                        }
                                    });
                                }

                                createEventDialogue.dismiss();
                                hour = 0;
                                minute = 0;
                                eventName = (EditText) createEventDialogue.findViewById(R.id.event_name_create);
                                eventLocation = (EditText) createEventDialogue.findViewById(R.id.event_location_create);
                                eventDescription = (EditText) createEventDialogue.findViewById((R.id.event_description_create));

                                eventName.setText("");
                                eventLocation.setText("");
                                eventDescription.setText("");
                                selectTimeButton.setText("Edit event end time");
                            } else {
                                Toast.makeText(CalendarActivity.this, "Please enter an end time.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                    editEventButton = (Button) editEventDialogue.findViewById(R.id.button_edit_event);
                    editEventButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            java.util.Calendar calendar = java.util.Calendar.getInstance();
                            if (hour != 0) {
                                calendar = java.util.Calendar.getInstance();
                                calendar.set(eventStart.get(java.util.Calendar.YEAR),
                                        eventStart.get(java.util.Calendar.MONTH),
                                        eventStart.get(java.util.Calendar.DATE), hour, minute);
                            }

                            editEventName = (EditText) editEventDialogue.findViewById(R.id.event_name_edit);
                            editEventLocation = (EditText) editEventDialogue.findViewById(R.id.event_location_edit);
                            editEventDescription = (EditText) editEventDialogue.findViewById(R.id.event_description_edit);

                            String newEventName = editEventName.getText().toString();
                            String newEventLocation = editEventLocation.getText().toString();
                            String newEventDescription = editEventDescription.getText().toString();
                            DateTime newEventEndTime = new DateTime(0);
                            if (hour != 0) {
                                newEventEndTime = new DateTime(calendar.getTime());
                            }

                            HashMap params = new HashMap();
                            params.put("name", newEventName);
                            params.put("location", newEventLocation);
                            params.put("description", newEventDescription);
                            params.put("id", eventToView.getId());
                            params.put("service", service);
                            params.put("calendarID", CALENDAR_ID);

                            if (hour != 0) {
                                params.put("end", newEventEndTime);
                            }

                            // Perform optimistic UI update
                            List<Event> listOfEvents = events;
                            for (int i = 0; i < listOfEvents.size(); i++) {
                                if (listOfEvents.get(i).getId().equals(eventToView.getId())) {
                                    Event dummyEvent = events.get(i);
                                    dummyEvent.setSummary(newEventName);
                                    dummyEvent.setLocation(newEventLocation);
                                    dummyEvent.setDescription(newEventDescription);
                                    if (hour != 0) {
                                        dummyEvent.setEnd(new EventDateTime().setDate(newEventEndTime));
                                        dummyEvent.setStart(eventToView.getStart());
                                    }
                                }
                            }

                            mWeekView.notifyDatasetChanged();

                            EventManager.updateEvent updateEvent = new EventManager.updateEvent(CalendarActivity.this);

                            try {
                                updateEvent.execute(params);
                            } catch (Exception e) {
                                LogUtility.e(TAG, "onCreate: updateEvent: update error: " + e);
                                e.printStackTrace();
                            }

                            editEventDialogue.dismiss();
                            hour = 0;
                            minute = 0;
                        }
                    });


                    // Get a reference for the week view in the layout.
                    mWeekView = (WeekView) findViewById(R.id.weekView);
                    mWeekView.setVisibility(View.GONE);

                    createEventTitle = (TextView) createEventDialogue.findViewById(R.id.create_event_title);
                    if (!userData.get("user_type").equals("admin")) {
                        createEventTitle.setText("Create submission");
                        createEventTitle.setTextSize(20);
                    }

                    progressBarCalendar = (ProgressBar) findViewById(R.id.progressBarCalendar);
                    progressBarCalendar.setEnabled(true);

                    alertNavView = (NavigationView) findViewById(R.id.nav_view_alerts);

                    getCalendarEvents(PreferenceUtils.getFirebaseToken(CalendarActivity.this.getApplicationContext()))
                            .addOnCompleteListener(new OnCompleteListener<List<Event>>() {
                                @Override
                                public void onComplete(@NonNull Task<List<Event>> task) {
                                    if (!task.isSuccessful()) {
                                        Exception e = task.getException();
                                        if (e instanceof FirebaseFunctionsException) {
                                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                            FirebaseFunctionsException.Code code = ffe.getCode();
                                            Object details = ffe.getDetails();
                                        }
                                    }
                                    // Success
                                    Log.i("EVENTS RESULT", "" + task.getResult());
                                    events = (List<Event>) task.getResult();

                                    Log.i("EVENTS CALENDAR", "" + events);

                                    // Set time on calendar to the time of the first event. This stops the calendar from starting at weird times such
                                    // as 1 AM which could cause confusion.
                                    if (events != null) {
                                        String start = events.get(0).getStart().getDate().toString();
                                        String hours = start.substring(11, 13);
                                        String minutes = start.substring(14, 16);
                                        double timeToStart = Double.parseDouble(hours + "." + minutes);
                                        mWeekView.goToHour(timeToStart - 2.0);
                                    }

                                    mLogo.setVisibility(View.VISIBLE);
                                    mWeekView.setVisibility(View.VISIBLE);
                                    createEventButton.setVisibility(View.VISIBLE);

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

                                            alertAdapter.notifyDataSetChanged();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                                    if (userData.get("user_type").equals("admin")) {
                                        //submissionButton.setVisibility(View.VISIBLE);
                                    } else {
                                        final String username = userData.get("user_name");
                                        mDatabase.child("submissions").addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                LogUtility.i(TAG, "onCreate: onDataChange: databaseval: " + dataSnapshot.getValue());
                                                listOfSubmissions.clear();
                                                for (DataSnapshot mSnapshot : dataSnapshot.getChildren()) {
                                                    LogUtility.i(TAG, "onCreate: onDataChange: databaseval2: " + mSnapshot.getValue());
                                                    Event event = EventParser.parseSingleEvent(mSnapshot.getValue());
                                                    if (event.getCreator().getDisplayName().equals(username)) {
                                                        listOfSubmissions.add(event);
                                                    }
                                                }
                                                mWeekView.notifyDatasetChanged();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                    progressBarCalendar.setVisibility(View.GONE);
                                }
                            });

                    closeViewDialogueButton = (Button) viewEventDialogue.findViewById(R.id.finish_viewing_button);
                    closeViewDialogueButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            viewEventDialogue.dismiss();
                        }
                    });

                    WeekView.EventLongPressListener eventLongPressListener = new WeekView.EventLongPressListener() {
                        @Override
                        public void onEventLongPress(WeekViewEvent event, RectF eventRect) {
                            // If the event color is gray, it is in the process of being created and does not yet exist in the google calendar.
                            // because of this, we do not allow gray events to be edited.
                            if (event.getColor() == Color.GRAY) return;
                            // TODO: add validation to the case where event does not have a name. (event.getName() returns null reference)
                            for (Event eventObj : events) {
                                if (event.getName().equals(eventObj.getSummary())) {
                                    eventToView = eventObj;
                                }
                            }
                            eventStart = event.getStartTime();
                            DateFormat df = new SimpleDateFormat("h:mm");
                            String sdt;
                            if (eventToView.getEnd().getDate() != null) {
                                sdt = df.format(new Date(eventToView.getEnd().getDate().getValue()));
                            } else {
                                sdt = df.format(new Date(eventToView.getEnd().getDateTime().getValue()));
                            }

                            if (userData.get("user_type").equals("admin")) {
                                editEventName = (EditText) editEventDialogue.findViewById(R.id.event_name_edit);
                                editEventLocation = (EditText) editEventDialogue.findViewById(R.id.event_location_edit);
                                editEventDescription = (EditText) editEventDialogue.findViewById(R.id.event_description_edit);

                                editEventName.setText(eventToView.getSummary());
                                editEventLocation.setText(eventToView.getLocation());
                                editEventDescription.setText(eventToView.getDescription());
                                selectTimeButtonEdit.setText("Ending at " + sdt);

                                //editEventDialogue.show();
                            } else {
                                viewEventName = (TextView) viewEventDialogue.findViewById(R.id.event_name_view);
                                viewEventLocation = (TextView) viewEventDialogue.findViewById(R.id.event_location_view);
                                viewEventDescription = (TextView) viewEventDialogue.findViewById(R.id.event_description_view);

                                viewEventName.setText(eventToView.getSummary());
                                viewEventLocation.setText(eventToView.getLocation());
                                viewEventDescription.setText(eventToView.getDescription());

                                //viewEventDialogue.show();
                            }
                        }
                    };
                    mWeekView.setEventLongPressListener(eventLongPressListener);

                    WeekView.EmptyViewLongPressListener emptyViewLongPressListener = new WeekView.EmptyViewLongPressListener() {
                        @Override
                        public void onEmptyViewLongPress(java.util.Calendar time) {
                            //timePressed = time;
                            //createEventDialogue.show();
                        }
                    };
                    mWeekView.setEmptyViewLongPressListener(emptyViewLongPressListener);

                    MonthLoader.MonthChangeListener mMonthChangeListener = new MonthLoader.MonthChangeListener() {
                        @Override
                        public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
                            // Populate the week view with some events.
                            List<WeekViewEvent> filteredEvents = new ArrayList<WeekViewEvent>();
                            Event dummyEvent = new Event();
                            if (events != null) {
                                List<Event> listOfEvents = getEvents(newYear, newMonth);
                                Log.i("RETRIEVED EVENTS", ""  + listOfEvents);
                                Class aClass = dummyEvent.getClass();
                                for (Event event : listOfEvents) {
                                    WeekViewEvent weekEvent = new WeekViewEvent();
                                    for (Field field : aClass.getDeclaredFields()) {
                                        field.setAccessible(true);
                                        LogUtility.i(TAG, "onCreate: field: " + field.getName());

                                        switch (field.getName()) {
                                            case "end":
                                                java.util.Calendar endCalendar = new GregorianCalendar();
                                                String end;
                                                if (event.getEnd().getDate() != null) {
                                                    end = event.getEnd().getDate().toString();
                                                } else {
                                                    end = event.getEnd().getDateTime().toString();
                                                }
                                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                                                Date date = new Date();
                                                Long endMillis = 0L;
                                                try {
                                                    date = simpleDateFormat.parse(end);
                                                    endMillis = date.getTime();
                                                } catch (ParseException e) {
                                                    LogUtility.e(TAG, "onCreate: getTime (720): err: " + e);
                                                }
                                                endCalendar.setTimeInMillis(endMillis);
                                                Log.i("WEEK EVENT TIME", "" + endCalendar);
                                                weekEvent.setEndTime(endCalendar);

                                            case "start":
                                                java.util.Calendar startCalendar = new GregorianCalendar();
                                                String start;
                                                if (event.getStart().getDate() != null) {
                                                    start = event.getStart().getDate().toString();
                                                } else {
                                                    start = event.getStart().getDateTime().toString();
                                                }
                                                SimpleDateFormat simpleDateFormatStart = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                                                Date startDate = new Date();
                                                Long startMillis = 0L;
                                                try {
                                                    startDate = simpleDateFormatStart.parse(start);
                                                    startMillis = startDate.getTime();
                                                } catch (ParseException e) {
                                                    LogUtility.e(TAG, "onCreate: getTime (740): err: " + e);
                                                }
                                                startCalendar.setTimeInMillis(startMillis);
                                                Log.i("WEEK EVENT TIME", "" + startCalendar);

                                                weekEvent.setStartTime(startCalendar);

                                            case "id":
                                                if (event.getEtag() != null) {
                                                    weekEvent.setId(Long.parseLong(event.getEtag().replace("\"", "")));
                                                }

                                            case "name":
                                                weekEvent.setName(event.getSummary());
                                                weekEvent.setLocation(event.getLocation());

                                            case "colorId":
                                                if (event.getEtag() == null) {
                                                    weekEvent.setColor(Color.GRAY);
                                                }

                                                // Decided to forgo porting colors from google calendar events to save time
//                                        LogUtility.i(TAG, "color: " + event.getColorId());
//                                        if (event.getColorId() != null) {
//                                            weekEvent.setColor(Color.parseColor(event.getColorId()));
//                                        }

                                        }
                                    }
                                    filteredEvents.add(weekEvent);
                                }
                                Log.i("FILTERED EVENTS", "" + filteredEvents);
                                return filteredEvents;
                            } else {
                                return filteredEvents;
                            }
                        }
                    };
                    mWeekView.setMonthChangeListener(mMonthChangeListener);
                }
            }
        });
    }

    // UI handlers

    public void handleEventChange(Events refetchedEvents) {
        events = refetchedEvents.getItems();
        mWeekView.notifyDatasetChanged();
    }

}
