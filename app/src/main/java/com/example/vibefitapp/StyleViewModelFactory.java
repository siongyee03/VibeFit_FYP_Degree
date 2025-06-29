package com.example.vibefitapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class StyleViewModelFactory implements ViewModelProvider.Factory {

        public StyleViewModelFactory() {
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(StyleViewModel.class)) {
                return (T) new StyleViewModel();
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
