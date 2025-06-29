package com.example.vibefitapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavouritePostsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> favouritePosts;
    private FirebaseFirestore db;
    private String userId;
    private ImageButton btnBack;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite_posts);

        recyclerView = findViewById(R.id.recyclerViewFavourites);
        btnBack = findViewById(R.id.btn_back);
        emptyText = findViewById(R.id.emptyText);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        favouritePosts = new ArrayList<>();
        postAdapter = new PostAdapter(this, favouritePosts, null);
        recyclerView.setAdapter(postAdapter);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Please log in to continue.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
        loadFavouritePosts();

        swipeRefreshLayout.setOnRefreshListener(this::loadFavouritePosts);

    }

    private void loadFavouritePosts() {
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setRefreshing(true);

        db.collection("users").document(userId).collection("favourites")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> favPostIds = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        favPostIds.add(doc.getId());
                    }

                    favouritePosts.clear();
                    postAdapter.notifyDataSetChanged();

                    if (favPostIds.isEmpty()) {
                        swipeRefreshLayout.setRefreshing(false);
                        emptyText.setVisibility(View.VISIBLE);

                        return;
                    }

                    final int batchSize = 10;
                    int totalBatches = (favPostIds.size() + batchSize - 1) / batchSize;

                    final int[] completedBatches = {0};

                    for (int batch = 0; batch < totalBatches; batch++) {
                        int start = batch * batchSize;
                        int end = Math.min(start + batchSize, favPostIds.size());
                        List<String> batchIds = favPostIds.subList(start, end);

                        db.collection("posts")
                                .whereIn(FieldPath.documentId(), batchIds)
                                .get()
                                .addOnSuccessListener(postsSnap -> {
                                    for (DocumentSnapshot doc : postsSnap.getDocuments()) {
                                        Post post = doc.toObject(Post.class);
                                        if (post != null) {
                                            post.setId(doc.getId());
                                            favouritePosts.add(post);
                                        }
                                    }

                                    completedBatches[0]++;
                                    if (completedBatches[0] == totalBatches) {
                                        postAdapter.notifyDataSetChanged();
                                        swipeRefreshLayout.setRefreshing(false);
                                        emptyText.setVisibility(favouritePosts.isEmpty() ? View.VISIBLE : View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to load posts.", Toast.LENGTH_SHORT).show();
                                    swipeRefreshLayout.setRefreshing(false);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load favourites.", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

}