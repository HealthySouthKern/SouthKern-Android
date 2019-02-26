package com.eddierangel.southkern.android.utils;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private static final String TAG = "EventParser";

    public static Events parse(Object obj) {
        HashMap eventsObject = (HashMap) obj;
        Events dummyEvents = new Events();
        Class aClass = dummyEvents.getClass();
        for (Field field : aClass.getDeclaredFields()) {
            field.setAccessible(true);
            Log.i(TAG, "parse: field setAccessible: " + field.getName() + " " + eventsObject.get(field.getName()) + " : " + field.getType());
            if (eventsObject.containsKey(field.getName())) {
                if (field.getName().equals("items")) {
                    List<Event> listOfEvents = new ArrayList<Event>();
                    List<HashMap> listOfMaps = (List<HashMap>) eventsObject.get(field.getName());

                    Event.Creator dummyCreator = new Event.Creator();
                    HashMap dummyMap = new HashMap();
                    Class creatorClass = dummyCreator.getClass();

                    Event.Organizer dummyOrganizer = new Event.Organizer();
                    Class organizerClass = dummyOrganizer.getClass();

                    Event.ExtendedProperties dummyProperties = new Event.ExtendedProperties();
                    Class propertiesClass = dummyProperties.getClass();
                    for (HashMap map : listOfMaps) {
                        Event dummyEvent = new Event();
                        Class secondClass = dummyEvent.getClass();
                        for (Field innerField : secondClass.getDeclaredFields()) {
                            innerField.setAccessible(true);
                            if (map.containsKey(innerField.getName())) {
                                if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$ExtendedProperties")) {
                                    dummyMap = (HashMap) map.get(innerField.getName());
                                    for (Field innerSubField : propertiesClass.getDeclaredFields()) {
                                        innerSubField.setAccessible(true);
                                        if (dummyMap.containsKey(innerSubField.getName())) {
                                            dummyProperties.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
                                        }
                                    }
                                    dummyEvent.setExtendedProperties(dummyProperties);
                                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$Organizer")) {
                                    dummyMap = (HashMap) map.get(innerField.getName());
                                    for (Field innerSubField : organizerClass.getDeclaredFields()) {
                                        innerSubField.setAccessible(true);
                                        if (dummyMap.containsKey(innerSubField.getName())) {
                                            dummyOrganizer.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
                                        }
                                    }
                                    dummyEvent.setOrganizer(dummyOrganizer);
                                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$Creator")) {
                                    dummyMap = (HashMap) map.get(innerField.getName());
                                    for (Field innerSubField : creatorClass.getDeclaredFields()) {
                                        innerSubField.setAccessible(true);
                                        if (dummyMap.containsKey(innerSubField.getName())) {
                                            dummyCreator.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
                                        }
                                    }
                                    dummyEvent.setCreator(dummyCreator);
                                } else if (innerField.getType().toString().equals("class com.google.api.client.util.DateTime")) {
                                    try {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                        Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) map.get(innerField.getName())).getTime());
                                        DateTime date = new DateTime(datetime);
                                        dummyEvent.setUpdated(date);
                                    } catch (Exception e) {
                                        Log.e(TAG, "parse: Error parsing event.updated: " + e);
                                    }
                                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.EventDateTime")) {
                                    try {
                                        HashMap dateObj = (HashMap) map.get(innerField.getName());
                                        String dateTimeHolder = (String) dateObj.get("dateTime");
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                                        Timestamp datetime = new Timestamp(simpleDateFormat.parse(dateTimeHolder).getTime());
                                        DateTime date = new DateTime(datetime);
                                        EventDateTime eventDateTime = new EventDateTime();
                                        eventDateTime.setDate(date);
                                        dummyEvent.set(innerField.getName(), eventDateTime);
                                    } catch (Exception e) {
                                        Log.e(TAG, "parse: Error parsing start time: " + e);
                                    }
                                } else {
                                    dummyEvent.set(innerField.getName(), map.get(innerField.getName()));
                                }
                            }
                        }
                        listOfEvents.add(dummyEvent);
                    }
                    dummyEvents.set("items", listOfEvents);
                } else if (field.getType().toString().equals("class com.google.api.client.util.DateTime")) {
                    try {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) eventsObject.get(field.getName())).getTime());
                        DateTime date = new DateTime(datetime);
                        dummyEvents.setUpdated(date);
                    } catch (Exception e) {
                        Log.e(TAG, "parse: Error parsing events.updated: " + e);
                    }
                } else {
                    dummyEvents.set(field.getName(), eventsObject.get(field.getName()));
                }
            }
        }
        return dummyEvents;
    }

    public static Event parseSingleEvent(Object obj) {
        HashMap map = (HashMap) obj;
        Log.i(TAG, "parseSingleEvent: databasemap: " + map);
        Event dummyEvent = new Event();

        HashMap dummyMap = new HashMap();
        Event.ExtendedProperties dummyProperties = new Event.ExtendedProperties();
        Event.Organizer dummyOrganizer = new Event.Organizer();
        Event.Creator dummyCreator = new Event.Creator();

        Class propertiesClass = dummyProperties.getClass();
        Class organizerClass = dummyOrganizer.getClass();
        Class creatorClass = dummyCreator.getClass();
        Class secondClass = dummyEvent.getClass();
        for (Field innerField : secondClass.getDeclaredFields()) {
            innerField.setAccessible(true);
            Log.i(TAG, "parseSingleEvent: databasevalfield: " + innerField.getName() + " | " + innerField.getType());
            if (map.containsKey(innerField.getName())) {
                if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$ExtendedProperties")) {
                    dummyMap = (HashMap) map.get(innerField.getName());
                    for (Field innerSubField : propertiesClass.getDeclaredFields()) {
                        innerSubField.setAccessible(true);
                        if (dummyMap.containsKey(innerSubField.getName())) {
                            dummyProperties.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
                        }
                    }
                    dummyEvent.setExtendedProperties(dummyProperties);
                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$Organizer")) {
                    dummyMap = (HashMap) map.get(innerField.getName());
                    for (Field innerSubField : organizerClass.getDeclaredFields()) {
                        innerSubField.setAccessible(true);
                        if (dummyMap.containsKey(innerSubField.getName())) {
                            dummyOrganizer.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
                        }
                    }
                    dummyEvent.setOrganizer(dummyOrganizer);
                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.Event$Creator")) {
                    dummyMap = (HashMap) map.get(innerField.getName());
                    for (Field innerSubField : creatorClass.getDeclaredFields()) {
                        innerSubField.setAccessible(true);
                        if (dummyMap.containsKey(innerSubField.getName())) {
                            dummyCreator.set(innerSubField.getName(), dummyMap.get(innerSubField.getName()));
                        }
                    }
                    dummyEvent.setCreator(dummyCreator);
                } else if (innerField.getType().toString().equals("class com.google.api.client.util.DateTime")) {
                    try {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                        Timestamp datetime = new Timestamp(simpleDateFormat.parse((String) map.get(innerField.getName())).getTime());
                        DateTime date = new DateTime(datetime);
                        dummyEvent.setUpdated(date);
                    } catch (Exception e) {
                        Log.e(TAG, "parseSingleEvent: Error parsing event.updated: " + e);
                    }
                } else if (innerField.getType().toString().equals("class com.google.api.services.calendar.model.EventDateTime")) {
                    try {
                        Log.i(TAG, "parseSingleEvent: databaseparseTime: " + innerField.getName());
                        HashMap dateObj = (HashMap) map.get(innerField.getName());
                        HashMap dateTimeObj;
                        if (dateObj.get("dateTime") != null) {
                            dateTimeObj = (HashMap) dateObj.get("dateTime");
                        } else {
                            dateTimeObj = (HashMap) dateObj.get("date");
                        }
                        Long longHolder = (Long) dateTimeObj.get("value");
                        Timestamp datetime = new Timestamp(longHolder);
                        DateTime date = new DateTime(datetime);
                       // Log.i("databasedate", "" + date);
                        EventDateTime eventDateTime = new EventDateTime();
                        eventDateTime.setDate(date);
                        Log.i(TAG, "parseSingleEvent: databaseeventtime: " + eventDateTime);
                        dummyEvent.set(innerField.getName(), eventDateTime);
                    } catch (Exception e) {
                        Log.e(TAG, "parseSingleEvent: Error parsing start time: " + e);
                    }
                } else {
                    dummyEvent.set(innerField.getName(), map.get(innerField.getName()));
                }
            }
        }
        Log.i(TAG, "parseSingleEvent: databasedummy: " + dummyEvent);
        return dummyEvent;
    }
}

