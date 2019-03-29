package com.eddierangel.southkern.android.utils;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventParser {

    /*
     * The parse function takes in an object that is returned from an API call to a public google calendar. The events from the calendar are not
     * returned as the correct type. So we use reflection to turn the incorrectly typed object into the correct type which is
     * calendar.model.Events. I could not find a better way to get events from a public google calendar without having to authenticate.
     * I do not see why we would need to authenticate when accessing a public google calendar.
     *
     * @param obj - object returned from a google calendar API call. The contents of the object are events from the google calendar.
     * */
    // summary, location, end date, start date, description, color?, id
    public static List<Event> parse(List obj) {
        List<HashMap> eventList = obj;
        List<Event> reflectedEventList = new ArrayList<>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        Log.i("EVENT_LIST_PARSE", "" + eventList);

        for (HashMap tempEvent : eventList) {
            Log.i("TEMP EVENT", "" + tempEvent);

            Event dummyEvent = new Event();
            EventDateTime eventDateTimeEnd = new EventDateTime();
            EventDateTime eventDateTimeStart = new EventDateTime();


            try {
                Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) tempEvent.get("end_date")).getTime());
                DateTime date = new DateTime(datetime);
                eventDateTimeEnd.setDate(date);
            } catch (Exception e) {
                Log.e("date parse", "error parsing event datetime");
            }

            dummyEvent.setEnd(eventDateTimeEnd);
            Log.i("EVENT PART", "" + eventDateTimeEnd);

            try {
                Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) tempEvent.get("start_date")).getTime());
                DateTime date = new DateTime(datetime);
                eventDateTimeStart.setDate(date);
            } catch (Exception e) {
                Log.e("date parse", "error parsing event datetime");
            }

            dummyEvent.setStart(eventDateTimeStart);

            dummyEvent.setSummary((String) tempEvent.get("title"));

            dummyEvent.setDescription((String) tempEvent.get("description"));

            dummyEvent.setLocation((String) ((HashMap) tempEvent.get("venue")).get("venue"));

            dummyEvent.setEtag("" + tempEvent.get("id"));

            reflectedEventList.add(dummyEvent);

        }

        return reflectedEventList;
    }

    public static Event parseSingleEvent(Object obj) {
        //HashMap map = (HashMap) obj;
        HashMap tempEvent = (HashMap) obj;

        Event dummyEvent = new Event();
        HashMap dummyMap = new HashMap();
        EventDateTime eventDateTime = new EventDateTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

//        Event.ExtendedProperties dummyProperties = new Event.ExtendedProperties();
//        Event.Organizer dummyOrganizer = new Event.Organizer();
//        Event.Creator dummyCreator = new Event.Creator();

        try {
            Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) tempEvent.get("end_date")).getTime());
            DateTime date = new DateTime(datetime);
            eventDateTime.setDate(date);
        } catch (Exception e) {
            Log.e("date parse", "error parsing event datetime");
        }

        dummyEvent.setEnd(eventDateTime);

        try {
            Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) tempEvent.get("date")).getTime());
            DateTime date = new DateTime(datetime);
            eventDateTime.setDate(date);
        } catch (Exception e) {
            Log.e("date parse", "error parsing event datetime");
        }

        dummyEvent.setStart(eventDateTime);

        dummyEvent.setSummary((String) tempEvent.get("title"));

        dummyEvent.setDescription((String) tempEvent.get("excerpt"));

        dummyEvent.setLocation((String) ((HashMap) tempEvent.get("venue")).get("venue"));

        dummyEvent.setEtag((String) tempEvent.get("id"));

//        Class propertiesClass = dummyProperties.getClass();
//        Class organizerClass = dummyOrganizer.getClass();
//        Class creatorClass = dummyCreator.getClass();
//        Class secondClass = dummyEvent.getClass();
//        for (Field innerField : secondClass.getDeclaredFields()) {
//            innerField.setAccessible(true);
//            Log.i("databasevalfield", "" + innerField.getName() + " | " + innerField.getType());
//            if (map.containsKey(innerField.getName())) {
//                if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$ExtendedProperties")) {
//                    dummyMap = (HashMap) map.get(innerField.getName());
//                    for (Field innerSubField : propertiesClass.getDeclaredFields()) {
//                        innerSubField.setAccessible(true);
//                        if (dummyMap.containsKey(innerSubField.getName())) {
//                            dummyProperties.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
//                        }
//                    }
//                    dummyEvent.setExtendedProperties(dummyProperties);
//                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$Organizer")) {
//                    dummyMap = (HashMap) map.get(innerField.getName());
//                    for (Field innerSubField : organizerClass.getDeclaredFields()) {
//                        innerSubField.setAccessible(true);
//                        if (dummyMap.containsKey(innerSubField.getName())) {
//                            dummyOrganizer.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
//                        }
//                    }
//                    dummyEvent.setOrganizer(dummyOrganizer);
//                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$Creator")) {
//                    dummyMap = (HashMap) map.get(innerField.getName());
//                    for (Field innerSubField : creatorClass.getDeclaredFields()) {
//                        innerSubField.setAccessible(true);
//                        if (dummyMap.containsKey(innerSubField.getName())) {
//                            dummyCreator.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
//                        }
//                    }
//                    dummyEvent.setCreator(dummyCreator);
//                } else if (innerField.getType().toString().equals("class com.google.api.client.util.DateTime")) {
//                    try {
//                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//                        Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) map.get(innerField.getName())).getTime());
//                        DateTime date = new DateTime(datetime);
//                        dummyEvent.setUpdated(date);
//                    } catch (Exception e) {
//                        Log.e("Error parsing event.updated", "" + e);
//                    }
//                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.EventDateTime")) {
//                    try {
//                        Log.i("databaseparseTime", "" + innerField.getName());
//                        HashMap dateObj = (HashMap) map.get(innerField.getName());
//                        HashMap dateTimeObj;
//                        if (dateObj.get("dateTime") != null) {
//                            dateTimeObj = (HashMap) dateObj.get("dateTime");
//                        } else {
//                            dateTimeObj = (HashMap) dateObj.get("date");
//                        }
//                        Long longHolder = (Long) dateTimeObj.get("value");
//                        Timestamp datetime = new Timestamp(longHolder);
//                        DateTime date = new DateTime(datetime);
//                       // Log.i("databasedate", "" + date);
//                        EventDateTime eventDateTime = new EventDateTime();
//                        eventDateTime.setDate(date);
//                        Log.i("databaseeventtime", "" + eventDateTime);
//                        dummyEvent.set(innerField.getName(), eventDateTime);
//                    } catch (Exception e) {
//                        Log.e("Error parsing start time", "" + e);
//                    }
//                } else {
//                    dummyEvent.set(innerField.getName(), map.get(innerField.getName()));
//                }
//            }
//        }
        return dummyEvent;
    }
}

