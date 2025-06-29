package com.example.vibefitapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BasePostsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private boolean isLoading = false;
    private DocumentSnapshot lastVisible;
    private final int PAGE_SIZE = 10;
    private final Map<String, ListenerRegistration> likeListeners = new HashMap<>();

    protected abstract String getCategory();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshPosts();
            swipeRefreshLayout.setRefreshing(false);
        });

        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new SpacesItemDecoration(12));

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(getContext(), postList, this::stopLikeListener);
        recyclerView.setAdapter(postAdapter);

        db = FirebaseFirestore.getInstance();

        loadPosts();
        setupScrollListener();

        return view;
    }

    private void loadPosts() {
        if (isLoading) return;
        isLoading = true;

        String category = getCategory();
        Query query;

        if ("tutorial/pattern".equals(category)) {
            List<String> categories = Arrays.asList("tutorial", "pattern");
            query = db.collection("posts")
                    .whereIn("category", categories)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);
        } else {
            query = db.collection("posts")
                    .whereEqualTo("category", category.toLowerCase())
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);
        }

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                int start = postList.size();
                for (DocumentSnapshot doc : documents) {
                    Post post = doc.toObject(Post.class);
                    if (post != null) {
                        post.setId(doc.getId());
                        postList.add(post);
                        startLikeListener(post);
                    }
                }
                postAdapter.notifyItemRangeInserted(start, documents.size());

                if (!documents.isEmpty()) {
                    lastVisible = documents.get(documents.size() - 1);
                }
            }
            isLoading = false;
        });
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (!rv.canScrollVertically(1)) {
                    loadPosts();
                }
            }
        });
    }

    private void startLikeListener(Post post) {
        if (post.getId() == null || likeListeners.containsKey(post.getId())) return;

        ListenerRegistration registration = db.collection("posts")
                .document(post.getId())
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Long newLikeCount = snapshot.getLong("likeCount");
                        if (newLikeCount != null) {
                            post.setLikeCount(newLikeCount.intValue());
                            int index = postList.indexOf(post);
                            if (index != -1) {
                                postAdapter.notifyItemChanged(index);
                            }
                        }
                    }
                });

        likeListeners.put(post.getId(), registration);
    }

    private void stopLikeListener(String postId) {
        ListenerRegistration reg = likeListeners.remove(postId);
        if (reg != null) reg.remove();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration reg : likeListeners.values()) {
            reg.remove();
        }
        likeListeners.clear();
    }

    public void refreshPosts() {
        if (isLoading) return;
        int oldSize = postList.size();
        postList.clear();
        lastVisible = null;

        if (oldSize > 0) {
            postAdapter.notifyItemRangeRemoved(0, oldSize);
        }
        loadPosts();
    }
}
