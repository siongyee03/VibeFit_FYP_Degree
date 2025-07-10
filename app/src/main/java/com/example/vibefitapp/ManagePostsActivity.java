package com.example.vibefitapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class ManagePostsActivity extends AppCompatActivity implements PostAdapter.OnPostVisibilityChangedListener {

    private ImageView btnBack;
    private EditText etSearchPost;
    private RecyclerView recyclerViewPosts;

    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    private FirebaseFirestore db;

    private ListenerRegistration postsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_posts);

        btnBack = findViewById(R.id.btnBack);
        etSearchPost = findViewById(R.id.etSearchPost);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);

        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        setupBackButton();
        setupSearchListener();

        loadPosts(null);
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, postList, this);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPosts.setAdapter(postAdapter);

        recyclerViewPosts.addOnItemTouchListener(new RecyclerItemClickListener(this, recyclerViewPosts,
                (view, position) -> {
                    Post clickedPost = postList.get(position);
                    showCategoryDialog(clickedPost);
                }));
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupSearchListener() {
        etSearchPost.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                loadPosts(query.isEmpty() ? null : query);
            }
        });
    }

    private void loadPosts(@Nullable String searchQuery) {
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }

        postList.clear();
        postAdapter.notifyDataSetChanged();

        Query baseQuery = db.collection("posts")
                .orderBy("likeCount", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (searchQuery == null) {
            postsListener = baseQuery.addSnapshotListener(postEventListener);
        } else {
            baseQuery.get().addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Post post = doc.toObject(Post.class);
                    if (post == null) continue;
                    post.setId(doc.getId());

                    String title = post.getTitle() != null ? post.getTitle().toLowerCase() : "";
                    String content = post.getContent() != null ? post.getContent().toLowerCase() : "";
                    String q = searchQuery.toLowerCase();

                    if (title.contains(q) || content.contains(q)) {
                        postList.add(post);
                    }
                }
                postAdapter.notifyDataSetChanged();
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    private final EventListener<QuerySnapshot> postEventListener = (value, error) -> {
        if (error != null) {
            Toast.makeText(this, "Error loading posts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (value == null) return;

        for (DocumentChange dc : value.getDocumentChanges()) {
            Post post = dc.getDocument().toObject(Post.class);
            post.setId(dc.getDocument().getId());

            switch (dc.getType()) {
                case ADDED:
                    postList.add(dc.getNewIndex(), post);
                    postAdapter.notifyItemInserted(dc.getNewIndex());
                    break;
                case MODIFIED:
                    if (dc.getOldIndex() != dc.getNewIndex()) {
                        postList.remove(dc.getOldIndex());
                        postList.add(dc.getNewIndex(), post);
                        postAdapter.notifyItemMoved(dc.getOldIndex(), dc.getNewIndex());
                        postAdapter.notifyItemChanged(dc.getNewIndex());
                    } else {
                        postList.set(dc.getOldIndex(), post);
                        postAdapter.notifyItemChanged(dc.getOldIndex());
                    }
                    break;
                case REMOVED:
                    postList.remove(dc.getOldIndex());
                    postAdapter.notifyItemRemoved(dc.getOldIndex());
                    break;
            }
        }
    };

    private void showCategoryDialog(Post post) {
        final List<String> displayCategories = new ArrayList<>(CategoryUtil.valueToDisplayMap.values());

        int checkedItem = -1;
        if (post.getCategory() != null) {
            String displayCategory = CategoryUtil.valueToDisplayMap.get(post.getCategory().toLowerCase());
            if (displayCategory != null) {
                checkedItem = displayCategories.indexOf(displayCategory);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Category");
        builder.setSingleChoiceItems(displayCategories.toArray(new String[0]), checkedItem, (dialog, which) -> {
            String displayCategory = displayCategories.get(which);
            String actualCategory = CategoryUtil.displayToValueMap.get(displayCategory);
            updatePostCategory(post, actualCategory);
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updatePostCategory(Post post, String newCategory) {
        db.collection("posts").document(post.getId())
                .update("category", newCategory)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show();
                    post.setCategory(newCategory);
                    int index = postList.indexOf(post);
                    if (index != -1) {
                        postAdapter.notifyItemChanged(index);
                    } else {
                        postAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update category: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postsListener != null) {
            postsListener.remove();
        }
    }

    @Override
    public void onPostInvisible(String postId) {
    }
}
