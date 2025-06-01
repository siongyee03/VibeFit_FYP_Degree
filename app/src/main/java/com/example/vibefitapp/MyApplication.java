package com.example.vibefitapp;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.vertexai.FirebaseVertexAI;
import com.google.firebase.vertexai.GenerativeModel;
import com.google.firebase.vertexai.java.GenerativeModelFutures;

import com.google.firebase.vertexai.type.Content;
import com.google.firebase.vertexai.type.GenerationConfig;
import com.google.firebase.vertexai.type.RequestOptions;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static GenerativeModelFutures model;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        String systemInstructionText = "You are a friendly and helpful personal fashion assistant. " +
                "Provide detailed outfit recommendations, styling tips, and fashion advice including hairstyle, accessories. " +
                "Analyze user descriptions and uploaded clothing images. " +
                "When you suggest an outfit or item that could be shown with an image, " +
                "always ask the user if they would like to see example images"+
                "include the exact phrase '[query: your search query here]' in your response " +
                "where 'your search query here' is a concise phrase suitable for searching images." +
                "Be polite and encouraging.";

        Content systemInstructionContent = new Content.Builder()
                .addText(systemInstructionText)
                .build();

        GenerationConfig generationConfig = new GenerationConfig.Builder().build();
        RequestOptions requestOptions = new RequestOptions();

        GenerativeModel gm = FirebaseVertexAI.getInstance()
                .generativeModel(
                        "gemini-2.0-flash",     // modelName (String)
                        generationConfig,
                        null,
                        null,
                        null,
                        systemInstructionContent,
                        requestOptions
                );
        model = GenerativeModelFutures.from(gm);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );
        Log.d(TAG, "Firebase App Check initialized with Debug Provider.");

    }
    public static GenerativeModelFutures getGeminiModel() {
        return model;
    }
}
