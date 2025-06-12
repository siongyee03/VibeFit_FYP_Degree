package com.example.vibefitapp;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ai.BuildConfig;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.RequestOptions;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static GenerativeModelFutures model;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);


        /*
            com.google.firebase.auth.FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099);
            com.google.firebase.firestore.FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080);
            com.google.firebase.storage.FirebaseStorage.getInstance().useEmulator("10.0.2.2", 9199);
        */

        String systemInstructionText = "You are a friendly and helpful personal fashion assistant. " +
                "Provide detailed outfit recommendations, styling tips, and fashion advice, including suggestions for hairstyles and accessories. " +
                "Analyze user descriptions and uploaded clothing images to offer personalized advice. " +
                "If a recommendation could be better understood with visual examples, politely ask at the end of your response if the user would like to see related images. " +
                "Always be polite, warm, and encouraging.";

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

        /*
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );
        Log.d(TAG, "Firebase App Check initialized with Debug Provider.");
        */
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

    }

    public static GenerativeModelFutures getGeminiModel() {
        return model;
    }
}
