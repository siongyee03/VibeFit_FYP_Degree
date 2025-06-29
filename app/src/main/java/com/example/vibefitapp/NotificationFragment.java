package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration notificationListenerRegistration;
    private TextView noNotificationsText;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        notificationRecyclerView = view.findViewById(R.id.recyclerViewNotifications);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        noNotificationsText = view.findViewById(R.id.text_no_notifications);

        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(getContext(), notificationList, this);
        notificationRecyclerView.setAdapter(notificationAdapter);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            setupNotificationListener();
        } else {
            Toast.makeText(getContext(), "Please log in to see notifications.", Toast.LENGTH_SHORT).show();
            noNotificationsText.setVisibility(View.VISIBLE);
            notificationRecyclerView.setVisibility(View.GONE);
        }

        return view;
    }

    private void setupNotificationListener() {
        if (currentUserId == null) {
            Log.e("NotificationFragment", "Current user ID is null, cannot set up listener.");
            noNotificationsText.setVisibility(View.VISIBLE);
            notificationRecyclerView.setVisibility(View.GONE);
            return;
        }

        notificationListenerRegistration = db.collection("notifications")
                .whereEqualTo("toUserId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("NotificationFragment", "Listen failed.", e);
                        noNotificationsText.setVisibility(View.VISIBLE);
                        notificationRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    if (snapshots == null) {
                        Log.d("NotificationFragment", "Snapshots is null, returning.");
                        noNotificationsText.setVisibility(View.VISIBLE);
                        notificationRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    List<Notification> oldList = new ArrayList<>(notificationList);

                    List<Notification> newNotifications = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null) {
                            notification.setId(doc.getId());
                            newNotifications.add(notification);
                        }
                    }

                    DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                            new NotificationDiffCallback(oldList, newNotifications));

                    notificationList.clear();
                    notificationList.addAll(newNotifications);
                    diffResult.dispatchUpdatesTo(notificationAdapter);

                    Log.d("NotificationFragment", "Notifications updated. Count: " + notificationList.size());

                    if (notificationList.isEmpty()) {
                        noNotificationsText.setVisibility(View.VISIBLE);
                        notificationRecyclerView.setVisibility(View.GONE);
                    } else {
                        noNotificationsText.setVisibility(View.GONE);
                        notificationRecyclerView.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onNotificationClick(Notification notification) {

        if (notification.getPostId() != null) {
            db.collection("posts").document(notification.getPostId()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Post post = documentSnapshot.toObject(Post.class);
                            if (post != null) {
                                post.setId(documentSnapshot.getId());

                                if (!isAdded() || getContext() == null) return;

                                Intent intent = new Intent(getContext(), PostDetailActivity.class);
                                intent.putExtra("post", post);
                                startActivity(intent);

                                new Handler(Looper.getMainLooper()).postDelayed(() -> markNotificationAsRead(notification), 300);
                            }
                        } else {
                            Toast.makeText(getContext(), "Post not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error fetching post.", Toast.LENGTH_SHORT).show();
                        Log.e("NotificationFragment", "Error fetching post for notification: " + e.getMessage());
                    });
        }
    }

    private void markNotificationAsRead(Notification notification) {
        if (notification.getId() != null && !notification.isRead()) {
            db.collection("notifications").document(notification.getId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("NotificationFragment", "Notification marked as read: " + notification.getId());
                        notification.setRead(true);

                        int index = notificationList.indexOf(notification);
                        if (index != -1) {
                            notificationAdapter.notifyItemChanged(index);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("NotificationFragment", "Error marking notification as read: " + e.getMessage()));
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
            Log.d("NotificationFragment", "Notification listener removed in onDestroyView.");
        }
    }

    public static class NotificationDiffCallback extends DiffUtil.Callback {
        private final List<Notification> oldList, newList;

        public NotificationDiffCallback(List<Notification> oldList, List<Notification> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId()
                    .equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

}