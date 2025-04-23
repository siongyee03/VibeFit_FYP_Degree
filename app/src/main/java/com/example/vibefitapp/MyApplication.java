package com.example.vibefitapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyApplication extends Application {

    private static FirebaseFirestore db;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this); // 确保 FirebaseApp 被初始化
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseFirestore getFirestore() {
        return db;
    }
}
