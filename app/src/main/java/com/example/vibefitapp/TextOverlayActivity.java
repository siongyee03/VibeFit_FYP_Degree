package com.example.vibefitapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class TextOverlayActivity extends AppCompatActivity {

    public static final String EXTRA_MEDIA_URI = "media_uri";
    public static final String EXTRA_IS_VIDEO = "is_video";
    public static final String EXTRA_TEXT = "extra_text";
    public static final String EXTRA_TEXT_COLOR = "extra_text_color";
    public static final String EXTRA_TEXT_X = "extra_text_x";
    public static final String EXTRA_TEXT_Y = "extra_text_y";
    public static final String EXTRA_TEXT_FONT_PATH = "extra_text_font_path";

    public static final String RESULT_TEXT = "result_text";
    public static final String RESULT_TEXT_COLOR = "result_text_color";
    public static final String RESULT_TEXT_X = "result_text_x";
    public static final String RESULT_TEXT_Y = "result_text_y";
    public static final String RESULT_TEXT_FONT_PATH = "result_text_font_path";

    private FrameLayout textContainer;
    private DraggableTextView textView;

    private EditText inputEditText;
    private LinearLayout fontSelector;
    private LinearLayout colorSelector;
    private ImageButton btnConfirm, btnCancel;

    private Typeface selectedTypeface = Typeface.DEFAULT;
    private String selectedFontPath = null;
    private int selectedColor = 0xFFFFFFFF;

    private final String[] fontNames = {"Default", "Roboto", "DancingScript", "Pacifico"};
    private final String[] fontAssets = {null, "fonts/roboto_regular.ttf", "fonts/dancingscript_regular.ttf", "fonts/pacifico_regular.ttf"};
    private final int[] colors = {0xFFFFFFFF, 0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_overlay);

        textContainer = findViewById(R.id.text_container);
        inputEditText = findViewById(R.id.input_edit_text);
        fontSelector = findViewById(R.id.font_selector);
        colorSelector = findViewById(R.id.color_selector);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnCancel = findViewById(R.id.btn_cancel);

        setupTextView();
        loadInitialData();
        setupFontSelector();
        setupColorSelector();
        setupListeners();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithResult();
            }
        });
    }

    private void setupTextView() {
        textView = new DraggableTextView(this);
        textView.setText("Please Enter Text");
        textView.setTextColor(selectedColor);
        textView.setTypeface(selectedTypeface);
        textView.setTextSize(28);
        textView.setSelectedBorder(true);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textContainer.addView(textView, lp);

        textContainer.post(() -> {
            float cx = (textContainer.getWidth() - textView.getWidth()) / 2f;
            float cy = (textContainer.getHeight() - textView.getHeight()) / 2f;
            textView.setX(cx);
            textView.setY(cy);
        });

        textView.setOnClickListener(v -> selectTextView());

        selectTextView();
    }

    private void selectTextView() {
        textView.setSelectedBorder(true);
        inputEditText.setText(textView.getText());
        inputEditText.setSelection(inputEditText.getText().length());
        inputEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(inputEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void clearSelection() {
        textView.setSelectedBorder(false);
        inputEditText.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(inputEditText.getWindowToken(), 0);
    }

    private void setupFontSelector() {
        fontSelector.removeAllViews();
        for (int i = 0; i < fontNames.length; i++) {
            final int index = i;
            Button btn = new Button(this);
            btn.setText(fontNames[i]);
            btn.setAllCaps(false);
            btn.setTextSize(14);
            btn.setPadding(20, 8, 20, 8);

            if (fontAssets[index] != null) {
                try {
                    Typeface tf = Typeface.createFromAsset(getAssets(), fontAssets[index]);
                    btn.setTypeface(tf);
                } catch (Exception e) {
                    // ignore
                }
            }

            btn.setOnClickListener(v -> {
                if (fontAssets[index] == null) {
                    selectedTypeface = Typeface.DEFAULT;
                    selectedFontPath = null;
                } else {
                    selectedTypeface = Typeface.createFromAsset(getAssets(), fontAssets[index]);
                    selectedFontPath = fontAssets[index];
                }
                textView.setTypeface(selectedTypeface);
                inputEditText.setTypeface(selectedTypeface);
            });
            fontSelector.addView(btn);
        }
    }

    private void setupColorSelector() {
        colorSelector.removeAllViews();
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        for (int c : colors) {
            View v = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(12, 0, 12, 0);
            v.setLayoutParams(lp);
            v.setBackgroundColor(c);
            v.setOnClickListener(view -> {
                selectedColor = c;
                textView.setTextColor(c);
                inputEditText.setTextColor(c);
            });
            colorSelector.addView(v);
        }
    }

    private void setupListeners() {
        inputEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                textView.setText(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnConfirm.setOnClickListener(v -> finishWithResult());

        btnCancel.setOnClickListener(v -> {
            clearSelection();
            finish();
        });
    }

    private void finishWithResult() {
        Intent intent = new Intent();
        intent.putExtra(RESULT_TEXT, textView.getText().toString());
        intent.putExtra(RESULT_TEXT_COLOR, textView.getCurrentTextColor());
        intent.putExtra(RESULT_TEXT_X, textView.getX());
        intent.putExtra(RESULT_TEXT_Y, textView.getY());
        intent.putExtra(RESULT_TEXT_FONT_PATH, selectedFontPath);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void loadInitialData() {
        Intent intent = getIntent();
        String text = intent.getStringExtra(EXTRA_TEXT);
        int color = intent.getIntExtra(EXTRA_TEXT_COLOR, 0xFFFFFFFF);
        float x = intent.getFloatExtra(EXTRA_TEXT_X, -1f);
        float y = intent.getFloatExtra(EXTRA_TEXT_Y, -1f);
        String fontPath = intent.getStringExtra(EXTRA_TEXT_FONT_PATH);

        if (text != null) {
            textView.setText(text);
            inputEditText.setText(text);
        }
        selectedColor = color;
        textView.setTextColor(color);
        inputEditText.setTextColor(color);

        if (fontPath != null) {
            try {
                selectedTypeface = Typeface.createFromAsset(getAssets(), fontPath);
                selectedFontPath = fontPath;
                textView.setTypeface(selectedTypeface);
                inputEditText.setTypeface(selectedTypeface);
            } catch (Exception e) {
                selectedTypeface = Typeface.DEFAULT;
                selectedFontPath = null;
            }
        }

        if (x >= 0 && y >= 0) {
            textView.setX(x);
            textView.setY(y);
        } else {
            textContainer.post(() -> {
                float cx = (textContainer.getWidth() - textView.getWidth()) / 2f;
                float cy = (textContainer.getHeight() - textView.getHeight()) / 2f;
                textView.setX(cx);
                textView.setY(cy);
            });
        }
    }

}
