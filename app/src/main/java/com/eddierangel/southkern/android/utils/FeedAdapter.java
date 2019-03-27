package com.eddierangel.southkern.android.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.eddierangel.southkern.android.R;
import com.eddierangel.southkern.android.openchannel.OpenChatFeed;
import com.eddierangel.southkern.android.openchannel.OpenChatFragment;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.sendbird.android.OpenChannel;
import com.sendbird.android.SendBirdException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// TODO: Add Documentation to Public Interface
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.MyViewHolder> {
    private static final String TAG = "FeedAdapter";
    private List<Event> eventList;
    private Context context;
    private SimpleDateFormat baseFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
    private ImageView chatFeedIcon;
    private Calendar tempCalendar = Calendar.getInstance();
    private static ClickListener clickListener;
    private Boolean approved = false;
    private long twoWeekTime = 1209600000;


    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public TextView summary, description, location, date, time;
        public RelativeLayout mLayout;

        public MyViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            summary = (TextView) view.findViewById(R.id.feed_summary);
            description = (TextView) view.findViewById(R.id.feed_description);
            location = (TextView) view.findViewById(R.id.feed_location);
            time = (TextView) view.findViewById(R.id.feed_time);
            date = (TextView) view.findViewById(R.id.feed_date);

            mLayout = (RelativeLayout) view.findViewById(R.id.rlv2);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mLayout.getLayoutParams();
            params.setMargins(new Random().nextInt(300 - 20) + 20, 0, 0, -20);
            mLayout.setLayoutParams(params);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onItemClick(getAdapterPosition(), v);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (clickListener != null) {
                clickListener.onItemLongClick(getAdapterPosition(), v);
            }
            return false;
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        FeedAdapter.clickListener = clickListener;
    }

    public FeedAdapter(List<Event> eventList, Context context) {
        this.eventList = eventList;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_single, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final Event event = eventList.get(position);
        if (!event.getSummary().equals("Status update")) {

            holder.itemView.findViewById(R.id.image_chat_logo).setBackground(ContextCompat.getDrawable(context, R.drawable.ic_group_channels_black));
            holder.summary.setText(event.getSummary());
            holder.description.setText(event.getDescription());
            holder.location.setText(event.getLocation());
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

                    if (((Math.abs(new Date().getTime() - tempCalendar.getTimeInMillis())) < (twoWeekTime / 2))  &&
                            (new Date().getTime() < tempCalendar.getTimeInMillis()) &&
                            (tempCalendar.getTimeInMillis() < new Date().getTime() + (twoWeekTime / 2))
                            ) {
                        holder.date.setText(context.getResources().getText(R.string.current_week) + " " + dateDay + " | " + formattedDate);

                    } else if (((Math.abs(new Date().getTime() - tempCalendar.getTimeInMillis())) > (twoWeekTime / 2) &&
                            (new Date().getTime() < tempCalendar.getTimeInMillis()) &&
                            (tempCalendar.getTimeInMillis() > new Date().getTime() + (twoWeekTime / 2))
                    )) {
                        holder.date.setText(context.getResources().getText(R.string.next_week) + " " + dateDay + " | " + formattedDate);

                    } else if (new Date().getTime() - tempCalendar.getTimeInMillis() < 86400000) { // Less than one day
                        holder.date.setText(context.getResources().getText(R.string.today) + " | " + formattedDate);

                    } else {
                        holder.date.setText(dateDay + " | " + formattedDate);
                    }

                    String formattedEndTime = timeFormat.format(parsedEndDate);
                    String formattedStartTime = timeFormat.format(parsedStartDate);
                    holder.time.setText(formattedStartTime + " - " + formattedEndTime);
                    holder.itemView.findViewById(R.id.feed_item_layout).setBackground(ContextCompat.getDrawable(context, R.drawable.rectangle));
                    holder.itemView.findViewById(R.id.image_chat_logo).setBackground(ContextCompat.getDrawable(context, R.drawable.ic_group_channels_black));
                    holder.itemView.findViewById(R.id.rlv2).setBackground(ContextCompat.getDrawable(context, R.drawable.triangle));
                    ImageView imView = (ImageView) holder.itemView.findViewById(R.id.image_main_logo);
                    imView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_event_black_18dp));
                    holder.summary.setTextColor(Color.BLACK);
                    holder.location.setTextColor(Color.BLACK);
                    holder.date.setTextColor(Color.BLACK);
                    holder.time.setTextColor(Color.BLACK);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            String createdAt = event.getStart().getDate().toString();
            Date parsedStartDate;

            try {
                parsedStartDate = baseFormat.parse(createdAt);
                String formattedDate = dateFormat.format(parsedStartDate);

                int year = Integer.parseInt(formattedDate.substring(0, 4));
                int month = Integer.parseInt(formattedDate.substring(5, 7));
                int day = Integer.parseInt(formattedDate.substring(8, 10));
                tempCalendar.set(year, month - 1, day);
                String dateDay = tempCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());

                if (((Math.abs(new Date().getTime() - tempCalendar.getTimeInMillis())) < (twoWeekTime / 2))  &&
                        (new Date().getTime() < tempCalendar.getTimeInMillis()) &&
                        (tempCalendar.getTimeInMillis() < new Date().getTime() + (twoWeekTime / 2))
                        ) {
                    holder.date.setText(context.getResources().getText(R.string.current_week) + " " + dateDay + " | " + formattedDate);

                } else if (((Math.abs(new Date().getTime() - tempCalendar.getTimeInMillis())) > (twoWeekTime / 2) &&
                        (new Date().getTime() < tempCalendar.getTimeInMillis()) &&
                        (tempCalendar.getTimeInMillis() > new Date().getTime() + (twoWeekTime / 2))
                )) {
                    holder.date.setText(context.getResources().getText(R.string.next_week) + " " + dateDay + " | " + formattedDate);

                } else if (new Date().getTime() - tempCalendar.getTimeInMillis() < 86400000) { // Less than one day
                    holder.date.setText(context.getResources().getText(R.string.today) + " | " + formattedDate);

                } else {
                    holder.date.setText(dateDay + " | " + formattedDate);
                }

                String formattedStartTime = timeFormat.format(parsedStartDate);
                holder.time.setText(formattedStartTime);
                holder.location.setText(event.getDescription());
                holder.summary.setText(event.getSummary());
                holder.description.setText("");

                holder.itemView.findViewById(R.id.feed_item_layout).setBackground(ContextCompat.getDrawable(context, R.drawable.alert_rectangle));
                holder.itemView.findViewById(R.id.image_chat_logo).setBackground(ContextCompat.getDrawable(context, R.drawable.ic_group_channels_white));
                holder.itemView.findViewById(R.id.rlv2).setBackground(ContextCompat.getDrawable(context, R.drawable.alert_triangle));
                ImageView imView = (ImageView) holder.itemView.findViewById(R.id.image_main_logo);
                imView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_announcement_24));
                holder.summary.setTextColor(Color.WHITE);
                holder.location.setTextColor(Color.WHITE);
                holder.date.setTextColor(Color.WHITE);
                holder.time.setTextColor(Color.WHITE);

            } catch(Exception e) {
                LogUtility.i(TAG, "onBindViewHolder: status parse err: " + e);
            }
        }

        holder.itemView.findViewById(R.id.image_chat_logo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, OpenChatFeed.class);
                intent.putExtra("name", event.getSummary());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }
}
