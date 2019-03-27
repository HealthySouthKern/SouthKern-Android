package com.eddierangel.southkern.android.utils;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eddierangel.southkern.android.R;
import com.google.api.services.calendar.model.Event;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TODO: Add Documentation to Public Interface
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {
    private static final String TAG = "EventAdapter";
    private List<Event> eventList;
    private SimpleDateFormat baseFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
    private Calendar tempCalendar = Calendar.getInstance();
    private static ClickListener clickListener;
    private Boolean approved = false;

    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView summary, description, date, time;

        public MyViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            summary = (TextView) view.findViewById(R.id.event_summary);
            description = (TextView) view.findViewById(R.id.event_description);
            time = (TextView) view.findViewById(R.id.event_time);
            date = (TextView) view.findViewById(R.id.event_date);
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
        EventAdapter.clickListener = clickListener;
    }

    public void setApproved(Boolean bool) {
        approved = bool;
    }


    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.submission_single, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.summary.setText(event.getSummary());
        holder.description.setText(event.getLocation());
        if (event.getEnd() != null) {
            try {
                Date parsedStartDate, parsedEndDate;
                if (event.getStart().getDate() != null) {
                    parsedStartDate = baseFormat.parse(event.getStart().getDate().toString());
                    parsedEndDate = baseFormat.parse(event.getEnd().getDate().toString());
                } else {
                    parsedStartDate = baseFormat.parse(event.getStart().getDateTime().toString());
                    parsedEndDate = baseFormat.parse(event.getEnd().getDateTime().toString());

                }
                String formattedDate = dateFormat.format(parsedEndDate);

                int year = Integer.parseInt(formattedDate.substring(0, 4));
                int month = Integer.parseInt(formattedDate.substring(5, 7));
                int day = Integer.parseInt(formattedDate.substring(8, 10));
                tempCalendar.set(year, month - 1, day);
                String dateDay = tempCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());

                holder.date.setText(dateDay + " | " + formattedDate);

                String formattedEndTime = timeFormat.format(parsedEndDate);
                String formattedStartTime = timeFormat.format(parsedStartDate);
                holder.time.setText(formattedStartTime + " - " + formattedEndTime);
            } catch(Exception e) {
                LogUtility.i(TAG, "onBindViewHolder: get dates: date parse err: " + e);
                e.printStackTrace();
            }
        }
//        if (holder.approved) {
//            holder.itemView.setBackgroundColor(Color.parseColor("#699C45"));
//        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }
}
