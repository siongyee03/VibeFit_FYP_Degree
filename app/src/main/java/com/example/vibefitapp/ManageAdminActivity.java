package com.example.vibefitapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageAdminActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView recyclerViewAdmin;
    private FloatingActionButton fabAddAdmin;
    private SearchView searchViewAdmin;
    private SwitchCompat switchEnable;
    private FirebaseFirestore db;
    private AdminAdapter adminAdapter;
    private final List<Admin> adminList = new ArrayList<>();
    private ListenerRegistration adminListener;


    private static final String[] ROLES = {"admin", "superadmin"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_admin);

        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        recyclerViewAdmin = findViewById(R.id.recyclerViewAdmin);
        searchViewAdmin = findViewById(R.id.searchViewAdmin);

        ConstraintLayout rootLayout = findViewById(R.id.main);
        fabAddAdmin = new FloatingActionButton(this);
        fabAddAdmin.setImageResource(android.R.drawable.ic_input_add);
        ConstraintLayout.LayoutParams fabParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        fabParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        fabParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        fabParams.setMargins(0, 0, 32, 32);
        fabAddAdmin.setLayoutParams(fabParams);
        rootLayout.addView(fabAddAdmin);

        recyclerViewAdmin.setLayoutManager(new LinearLayoutManager(this));
        adminAdapter = new AdminAdapter(adminList);
        recyclerViewAdmin.setAdapter(adminAdapter);

        btnBack.setOnClickListener(v -> finish());

        fabAddAdmin.setOnClickListener(v -> showAddAdminDialog());

        loadAdmins();

        searchViewAdmin.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterAdmins(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAdmins(newText);
                return true;
            }
        });
    }

    private void filterAdmins(String keyword) {
        List<Admin> filteredList = new ArrayList<>();
        for (Admin admin : adminList) {
            if (admin.getUsername().toLowerCase().contains(keyword.toLowerCase()) ||
                    admin.getEmail().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(admin);
            }
        }
        adminAdapter = new AdminAdapter(filteredList);
        recyclerViewAdmin.setAdapter(adminAdapter);
    }

    private void loadAdmins() {
        if (adminListener != null) {
            adminListener.remove(); // Remove previous listener to avoid memory leaks and duplicate updates
        }

        adminListener = db.collection("users")
                .whereIn("role", Arrays.asList("admin", "superadmin"))
                .orderBy("username")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load admin list: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("ManageAdminActivity", "Error loading admin list: " + error.getMessage(), error);
                        return;
                    }
                    if (snapshots == null) return;

                    adminList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Admin admin = doc.toObject(Admin.class);
                        if (admin != null) {
                            admin.setId(doc.getId());
                            if (doc.contains("disabled")) {
                                admin.setDisabled(Boolean.TRUE.equals(doc.getBoolean("disabled")));
                            } else {
                                admin.setDisabled(false);
                            }
                            adminList.add(admin);
                        }
                    }
                    adminAdapter.notifyDataSetChanged();
                });
    }

    private void showAddAdminDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_admin, null);
        EditText etUsername = view.findViewById(R.id.etUsername);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        Spinner spinnerRole = view.findViewById(R.id.spinnerRole);
        switchEnable = view.findViewById(R.id.switchEnable);

        etUsername.setVisibility(View.VISIBLE);
        etEmail.setVisibility(View.VISIBLE);
        etPassword.setVisibility(View.VISIBLE);
        spinnerRole.setVisibility(View.VISIBLE);
        switchEnable.setVisibility(View.GONE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ROLES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Add Admin")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String username = etUsername.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();
                    String role = (String) spinnerRole.getSelectedItem();

                    if (username.isEmpty() || email.isEmpty() || password.isEmpty() || (!role.equals("admin") && !role.equals("superadmin"))) {
                        Toast.makeText(this, "All fields must be filled and role selected.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || isPasswordValid(password)){
                        Toast.makeText(this,"Valid email or password is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showPasswordConfirmDialog(username, email, password, role);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private boolean isPasswordValid(String password) {
        return !password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
    }

    private void addAdmin(String username, String email, String password, String role,
                          String superAdminEmail, String superAdminPassword) {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    List<String> methods = result.getSignInMethods();
                    if (methods != null && !methods.isEmpty()) {
                        Toast.makeText(this, "Email already in use. Please log in.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                FirebaseUser firebaseUser = authResult.getUser();
                                if (firebaseUser == null) {
                                    Toast.makeText(this, "Account creation failed. Please try again.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                firebaseUser.sendEmailVerification()
                                        .addOnSuccessListener(unused -> Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show());

                                db.collection("users")
                                        .document(firebaseUser.getUid())
                                        .set(new Admin() {{
                                            setId(firebaseUser.getUid());
                                            setUsername(username);
                                            setEmail(email);
                                            setRole(role);
                                            setDisabled(false);
                                        }})
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Admin added successfully!", Toast.LENGTH_SHORT).show();

                                            auth.signOut();
                                            auth.signInWithEmailAndPassword(superAdminEmail, superAdminPassword)
                                                    .addOnSuccessListener(signInResult -> Toast.makeText(this, "Switched back to superadmin.", Toast.LENGTH_SHORT).show())
                                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to switch back: " + e.getMessage(), Toast.LENGTH_LONG).show());
                                        });

                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to register: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to check email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showPasswordConfirmDialog(String username, String email, String newPassword, String role) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Superadmin Password");

        final EditText input = new EditText(this);
        input.setHint("Enter your password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(40, 0, 40, 0);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String superAdminPassword = input.getText().toString();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser == null || currentUser.getEmail() == null) {
                Toast.makeText(this, "User session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                return;
            }

            String superAdminEmail = currentUser.getEmail();
            addAdmin(username, email, newPassword, role, superAdminEmail, superAdminPassword);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEditAdminRoleDialog(Admin admin) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_admin, null);
        EditText etUsername = view.findViewById(R.id.etUsername);
        EditText etEmail = view.findViewById(R.id.etEmail);
        EditText etPassword = view.findViewById(R.id.etPassword);
        Spinner spinnerRole = view.findViewById(R.id.spinnerRole);
        switchEnable = view.findViewById(R.id.switchEnable);

        switchEnable.setVisibility(View.VISIBLE);
        switchEnable.setChecked(!admin.isDisabled());
        etUsername.setVisibility(View.GONE);
        etEmail.setVisibility(View.GONE);
        etPassword.setVisibility(View.GONE);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ROLES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        int position = 0;
        for (int i = 0; i < ROLES.length; i++) {
            if (ROLES[i].equals(admin.getRole())) {
                position = i;
                break;
            }
        }
        spinnerRole.setSelection(position);

        new AlertDialog.Builder(this)
                .setTitle("Edit Admin: " + admin.getUsername())
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newRole = (String) spinnerRole.getSelectedItem();
                    boolean newDisabledStatus = !switchEnable.isChecked();

                    if (!newRole.equals("admin") && !newRole.equals("superadmin")) {
                        Toast.makeText(this, "Please select a valid role.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newRole.equals(admin.getRole())) {
                        Toast.makeText(this, "Role is already set to " + newRole, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateAdminRole(admin.getId(), newRole, newDisabledStatus);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void updateAdminRole(String adminId, String newRole, boolean disabled) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", newRole);
        updates.put("disabled", disabled);

        db.collection("users").document(adminId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Admin updated successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update admin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void toggleAdminDisabled(Admin admin) {
        boolean newStatus = !admin.isDisabled();
        db.collection("users").document(admin.getId())
                .update("disabled", newStatus)
                .addOnSuccessListener(aVoid -> {
                    String message = newStatus ? "Admin disabled." : "Admin enabled.";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        if (adminListener != null) {
            adminListener.remove();
        }
        super.onDestroy();
    }

    public static class Admin {
        private String id;
        private String username;
        private String role;
        private String email;
        private boolean disabled;


        public Admin() {}

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }
        public void setRole(String role) {
            this.role = role;
        }

        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isDisabled() {
            return disabled;
        }
        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }
    }


    private class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

        private final List<Admin> admins;

        public AdminAdapter(List<Admin> admins) {
            this.admins = admins;
        }

        @NonNull
        @Override
        public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin, parent, false);
            return new AdminViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
            Admin admin = admins.get(position);
            holder.bind(admin);
        }

        @Override
        public int getItemCount() {
            return admins.size();
        }

        class AdminViewHolder extends RecyclerView.ViewHolder {

            TextView tvUsername, tvRole;
            ImageView btnActions;

            public AdminViewHolder(@NonNull View itemView) {
                super(itemView);
                tvUsername = itemView.findViewById(R.id.tvUsername);
                tvRole = itemView.findViewById(R.id.tvRole);
                btnActions = itemView.findViewById(R.id.btnActions);
            }

            public void bind(Admin admin) {
                tvUsername.setText(admin.getUsername());
                String roleText = admin.isDisabled() ?
                        getString(R.string.admin_role_disabled, admin.getRole()) :
                        getString(R.string.admin_role, admin.getRole());
                tvRole.setText(roleText);

                btnActions.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(ManageAdminActivity.this, btnActions);
                    popup.getMenuInflater().inflate(R.menu.menu_admin_actions, popup.getMenu());

                    MenuItem disableItem = popup.getMenu().findItem(R.id.action_disable);
                    disableItem.setTitle(admin.isDisabled() ? "Enable" : "Disable");

                    popup.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.action_edit) {
                            showEditAdminRoleDialog(admin);
                            return true;
                        } else if (id == R.id.action_disable) {
                            toggleAdminDisabled(admin);
                            return true;
                        } else {
                            return false;
                        }
                    });
                    popup.show();
                });
            }
        }
    }
}
