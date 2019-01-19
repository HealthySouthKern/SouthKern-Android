package com.eddierangel.southkern.android.utils;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eddierangel.southkern.android.R;
import com.google.api.services.calendar.model.Event;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.MyViewHolder> {
    private List<Event> eventList;
    private SimpleDateFormat baseFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
    private Calendar tempCalendar = Calendar.getInstance();
    private static ClickListener clickListener;

    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView description, location, date, time;

        public MyViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            description = (TextView) view.findViewById(R.id.alert_description);
            time = (TextView) view.findViewById(R.id.feed_time);
            date = (TextView) view.findViewById(R.id.feed_date);

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
        AlertAdapter.clickListener = clickListener;
    }

    public AlertAdapter(List<Event> eventList) {
        this.eventList = eventList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.alert_single, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Event event = eventList.get(position);
        if (event.getSummary().equals("Status update")) {
            String createdAt = event.getStart().getDate().toString();
            Date parsedStartDate;

            try {
                parsedStartDate = baseFormat.parse(createdAt);
                String formattedDate = dateFormat.format(parsedStartDate);


                String formattedStartTime = timeFormat.format(parsedStartDate);
                holder.time.setText(formattedStartTime);
                holder.description.setText(event.getDescription());
                holder.date.setText(formattedDate);

                holder.description.setTextColor(Color.WHITE);
                holder.date.setTextColor(Color.WHITE);
                holder.time.setTextColor(Color.WHITE);

            } catch(Exception e) {
                Log.i("status parse err", "" + e);
            }
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }
}
