package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.header_container, new HeaderFragment())
                .commit();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.footer_container, new FooterFragment())
                .commit();

        // Default loading of ExploreFragment.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new ExploreFragment())
                .commit();

        // logout button
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    public void setHeaderVisibility(boolean visible) {
        View header = findViewById(R.id.header_container);
        if (header != null) {
            header.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String currentHeaderTab = "explore";

    public void setCurrentHeaderTab(String tab) {
        this.currentHeaderTab = tab;
    }

    public String getCurrentHeaderTab() {
        return currentHeaderTab;
    }

}
