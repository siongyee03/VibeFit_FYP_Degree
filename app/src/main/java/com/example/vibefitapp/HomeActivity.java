package com.example.vibefitapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        String targetTab = getIntent().getStringExtra("target_tab");
        HeaderFragment headerFragment = new HeaderFragment();

        if (targetTab != null) {
            Bundle args = new Bundle();
            args.putString("target_tab", targetTab);
            headerFragment.setArguments(args);
        }

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
