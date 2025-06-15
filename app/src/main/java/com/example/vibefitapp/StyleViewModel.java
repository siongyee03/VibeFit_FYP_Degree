package com.example.vibefitapp;

import androidx.lifecycle.ViewModel;

import android.util.Log;

import com.google.firebase.ai.java.ChatFutures;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;

import java.util.ArrayList;
import java.util.List;

public class StyleViewModel extends ViewModel {

    private static final String TAG = "StyleViewModel";

    private final GenerativeModelFutures generativeModel;
    private final ChatFutures chat;
    private final List<ChatMessage> messageList = new ArrayList<>();

    public StyleViewModel() {
        Log.d(TAG, "StyleViewModel created");
        generativeModel = MyApplication.getGeminiModel();

        List<Content> initialHistoryForNewChat = new ArrayList<>();
        chat = generativeModel.startChat(initialHistoryForNewChat);
        messageList.clear();
    }

    public ChatFutures getChat() {
        return chat;
    }

    public List<ChatMessage> getMessageList() {
        return messageList;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "StyleViewModel cleared");
    }
}

