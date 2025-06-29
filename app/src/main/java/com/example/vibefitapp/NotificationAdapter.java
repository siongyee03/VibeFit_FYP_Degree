package com.example.vibefitapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<Notification> notificationList;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(Context context, List<Notification> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        // Set background based on read status
        if (notification.isRead()) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.light_blue_50));
        }

        Glide.with(context)
                .load(notification.getFromUserAvatarUrl())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(holder.fromUserAvatar);

        holder.notificationContent.setText(notification.getContent());

        if (notification.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            holder.timestamp.setText(sdf.format(notification.getTimestamp()));
        } else {
            holder.timestamp.setText(R.string.unknown_time);
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView fromUserAvatar;
        TextView notificationContent;
        TextView timestamp;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            fromUserAvatar = itemView.findViewById(R.id.notification_from_avatar);
            notificationContent = itemView.findViewById(R.id.notification_content);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
        }
    }
}