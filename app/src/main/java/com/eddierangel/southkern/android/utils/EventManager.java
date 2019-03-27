package com.eddierangel.southkern.android.utils;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.eddierangel.southkern.android.main.CalendarActivity;
import com.eddierangel.southkern.android.main.SubmissionManager;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;


import java.lang.ref.WeakReference;
import java.util.HashMap;

// TODO: Add Documentation to Public Interface
public class EventManager {

    private static final String TAG = "EventManager";

    public static class updateEvent extends AsyncTask<Object, Void, Void> {

        private Object callingParams;
        private final WeakReference<CalendarActivity> callingActivity;

        public updateEvent(CalendarActivity activity) {
            callingActivity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Object... params) {
            HashMap mapParams = (HashMap) params[0];
            String eventID = (String) mapParams.get("id");
            String calendarID = (String) mapParams.get("calendarID");
            Calendar service = (Calendar) mapParams.get("service");

            Event dummyEvent = new Event();
            dummyEvent.setSummary((String) mapParams.get("name"));
            dummyEvent.setLocation((String) mapParams.get("location"));
            dummyEvent.setDescription((String) mapParams.get("description"));
            if (mapParams.get("end") != null) {
                EventDateTime eventDateTime = new EventDateTime().setDateTime((DateTime) mapParams.get("end"));
                dummyEvent.setEnd(eventDateTime);
            }

            try {
                service.events().patch(calendarID, eventID, dummyEvent).execute();
                callingParams = params[0];
                return null;

            } catch(Exception e) {
                LogUtility.e(TAG, "doInBackground: patch: insert error: " + e);
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Void result) {
            CalendarActivity originalClass = callingActivity.get();
            EventManager.refetchEvents refetchEvents = new EventManager.refetchEvents(originalClass);
            refetchEvents.execute(callingParams);
            super.onPostExecute(result);
        }
    }

    public static class insertEvent extends AsyncTask<Object, Void, Void> {

        private Object callingParams;
        private WeakReference<CalendarActivity> callingActivity = null;
        private WeakReference<SubmissionManager> foreignActivity = null;

        public insertEvent(CalendarActivity activity) {
            callingActivity = new WeakReference<>(activity);
        }

        public insertEvent(SubmissionManager activity) {
            foreignActivity = new WeakReference<>(activity);
        }

        @Override
        public Void doInBackground(Object... params) {
            HashMap mapParams = (HashMap) params[0];
            String calendarID = (String) mapParams.get("calendarID");
            Calendar service = (Calendar) mapParams.get("service");
            Event dummyEvent = (Event) mapParams.get("event");

            LogUtility.i(TAG, "doInBackground: datetimedevent: " + dummyEvent);
            try {
                service.events().insert(calendarID, dummyEvent).execute();
                callingParams = params[0];
                return null;
            } catch(Exception e) {
                LogUtility.e(TAG, "doInBackground: insert: insert error:" + e);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            HashMap dummyParams = (HashMap) callingParams;
            if (!dummyParams.containsKey("bool")) {
                CalendarActivity originalClass = callingActivity.get();
                EventManager.refetchEvents refetchEvents = new EventManager.refetchEvents(originalClass);
                refetchEvents.execute(callingParams);
            }
            super.onPostExecute(result);
        }
    }

    public static class deleteEvent extends AsyncTask<Object, Void, Void> {

        private Object callingParams;
        private final WeakReference<CalendarActivity> callingActivity;

        public deleteEvent(CalendarActivity activity) {
            callingActivity = new WeakReference<>(activity);
        }

        @Override
        public Void doInBackground(Object... params) {
            HashMap mapParams = (HashMap) params[0];
            String calendarID = (String) mapParams.get("calendarID");
            Calendar service = (Calendar) mapParams.get("service");
            String eventID = (String) mapParams.get("id");

            try {
                service.events().delete(calendarID, eventID).execute();
                callingParams = params[0];
                return null;
            } catch(Exception e) {
                LogUtility.e(TAG, "doInBackground: delete: delete error: " + e);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            CalendarActivity originalClass = callingActivity.get();
            EventManager.refetchEvents refetchEvents = new EventManager.refetchEvents(originalClass);
            refetchEvents.execute(callingParams);
            super.onPostExecute(result);
        }
    }

    public static class refetchEvents extends AsyncTask<Object, Void, Events> {

        private final WeakReference<CalendarActivity> callingActivity;

        public refetchEvents(CalendarActivity activity) {
            callingActivity = new WeakReference<>(activity);
        }

        @Override
        public Events doInBackground(Object... params) {
            HashMap mapParams = (HashMap) params[0];
            String calendarID = (String) mapParams.get("calendarID");
            Calendar service = (Calendar) mapParams.get("service");

            try {
                return service.events().list(calendarID).execute();
            } catch(Exception e) {
                LogUtility.e(TAG, "doInBackground: list: refetch events error: " + e);
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Events events) {
            CalendarActivity originalClass = callingActivity.get();
            originalClass.handleEventChange(events);
            super.onPostExecute(events);
        }
    }
}
