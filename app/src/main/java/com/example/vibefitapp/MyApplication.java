package com.example.vibefitapp;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.RequestOptions;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static GenerativeModelFutures model;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);


        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();


        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );
        Log.d(TAG, "Firebase App Check initialized with Debug Provider.");

        FirebaseAppCheck.getInstance().getToken(false)
                .addOnSuccessListener(tokenResponse -> Log.d(TAG, "App Check token is valid now. Ready to call AI."))
                .addOnFailureListener(exception -> Log.e(TAG, "Failed to get App Check token.", exception));

        String systemInstructionText = "You are a helpful and concise fashion assistant. " +
                "Only respond to questions related to fashion, clothing, personal style, or outfit advice. " +
                "If a question is unrelated, politely say: 'I'm here to help with fashion and style questions!' " +
                "Provide outfit ideas, styling tips, and accessory suggestions based on the user's input, photos. " +
                "Keep your responses professional and personalized. " +
                "Stay updated with seasonal trends and current fashion movements (e.g., trending colors, silhouettes, celebrity looks) when giving recommendations. "+
                "If a visual example would be helpful, ask at the end if the user would like to see one, " +
                "include the exact search query you'd use in square brackets, prefixed with `query:`"+
                "Make sure all such queries are strictly fashion-related, using clear keywords like 'outfit', 'look', 'style', or specific clothing items. " +
                "Avoid vague or unrelated terms. Be warm, polite, and supportive.";

        Content systemInstructionContent = new Content.Builder()
                .addText(systemInstructionText)
                .build();

        GenerationConfig generationConfig = new GenerationConfig.Builder().build();
        RequestOptions requestOptions = new RequestOptions();

        GenerativeModel gm = FirebaseAI.getInstance(GenerativeBackend.vertexAI())
                .generativeModel(
                        "gemini-2.0-flash",
                        generationConfig,
                        null,
                        null,
                        null,
                        systemInstructionContent,
                        requestOptions
                );
        model = GenerativeModelFutures.from(gm);

    }

    public static GenerativeModelFutures getGeminiModel() {
        return model;
    }
}
