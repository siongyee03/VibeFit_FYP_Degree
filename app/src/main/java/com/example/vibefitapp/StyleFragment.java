package com.example.vibefitapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.ai.type.Candidate;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.FinishReason;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.ImagePart;
import com.google.firebase.ai.type.Part;
import com.google.firebase.ai.type.TextPart;

import org.json.JSONArray;
import org.json.JSONObject;
import org.reactivestreams.Publisher; // For streaming API
import org.reactivestreams.Subscriber; // For consuming streaming results
import org.reactivestreams.Subscription; // Manages the subscription to the Publisher

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService; // For running tasks asynchronously
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StyleFragment extends Fragment {

    private static final String TAG = "StyleFragment";
    private EditText userInputEditText;
    private ImageButton sendButton;
    private ImageButton uploadImageButton;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private StyleViewModel viewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Bitmap selectedImageBitmap;
    private Subscription currentSubscription;
    private boolean isAwaitingResponse = false;

    private boolean awaitingImageConfirmation = false;
    private String lastRecommendationQuery = null;

    public StyleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StyleViewModelFactory factory = new StyleViewModelFactory();
        viewModel = new ViewModelProvider(this, factory).get(StyleViewModel.class);

        // Initialize the ActivityResultLauncher for picking images
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);

                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inSampleSize = 2;

                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);

                                if (bitmap == null) {
                                    Toast.makeText(getContext(), "Couldn't decode the image.", Toast.LENGTH_SHORT).show();
                                    selectedImageBitmap = null;
                                    userInputEditText.setHint(R.string.send_message);
                                    return;
                                }

                                selectedImageBitmap = compressBitmap(bitmap);

                                Toast.makeText(getContext(), "Image ready! Add your text and send.", Toast.LENGTH_SHORT).show();
                                userInputEditText.setHint("Enter text related to the image...");

                            } catch (IOException e) {
                                Log.e(TAG, "Error loading image", e);
                                Toast.makeText(getContext(), "Couldn't load image. Please try again.", Toast.LENGTH_SHORT).show();
                                selectedImageBitmap = null;
                                userInputEditText.setHint(R.string.send_message);

                            } catch (Exception e) {
                                Log.e(TAG, "Unexpected error selecting image", e);
                                Toast.makeText(getContext(), "Couldn't select image. Please try again.", Toast.LENGTH_SHORT).show();
                                selectedImageBitmap = null;
                                userInputEditText.setHint(R.string.send_message);
                            }
                        } else {
                            Log.w(TAG, "Image Uri is null from result data even though data Intent was not null.");
                            Toast.makeText(getContext(), "Unable to get image. Please try again.", Toast.LENGTH_SHORT).show();
                            selectedImageBitmap = null;
                            userInputEditText.setHint(R.string.send_message);
                        }

                    } else {
                        Log.d(TAG, "Image picking cancelled or no data returned.");
                        selectedImageBitmap = null;
                        userInputEditText.setHint(R.string.send_message);
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_style, container, false);

        // Initialize UI elements
        userInputEditText = view.findViewById(R.id.user_input);
        sendButton = view.findViewById(R.id.send_button);
        uploadImageButton = view.findViewById(R.id.upload_image_button);
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);

        chatAdapter = new ChatAdapter(viewModel.getMessageList(), getContext());
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        if (!viewModel.getMessageList().isEmpty()) {
            chatAdapter.notifyDataSetChanged();
            chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
        }

        sendButton.setOnClickListener(v -> sendMessage());
        uploadImageButton.setOnClickListener(v -> pickImage());

        if (viewModel.getMessageList().isEmpty()) {
            addMessage(new ChatMessage("AI", "Hello! I'm your personal fashion assistant. Tell me about your style, body shape, or upload clothing photos, and I'll give you pairing suggestions!", false));
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentSubscription != null) {
            currentSubscription.cancel();
            currentSubscription = null;
        }
        executor.shutdownNow();
    }

    private Bitmap compressBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int longSide = Math.max(width, height);

        float scale = 1.0f;
        if (longSide > 3000) {
            scale = 0.25f;
        } else if (longSide > 2000) {
            scale = 0.33f;
        } else if (longSide > 1000) {
            scale = 0.5f;
        }

        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        if (newWidth <= 0 || newHeight <= 0 || (newWidth == width && newHeight == height)) {
            return original;
        }

        try {
            return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        } catch (Exception e) {
            Log.e(TAG, "Error creating scaled bitmap", e);
            return original;
        }
    }

    // Handles sending a message to the AI
    private void sendMessage() {
        String userInputText = userInputEditText.getText().toString().trim();

        // --- Handle the user's confirmation for images ---
        if (awaitingImageConfirmation) {
            handlePromptForRecommendation(userInputText);
            userInputEditText.setText("");
            return;
        }

        // Prevent sending multiple messages while waiting for a response
        if (isAwaitingResponse) {
            Toast.makeText(getContext(), "Please wait for the current response to finish.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Part> parts = new ArrayList<>();

        if (selectedImageBitmap != null) {
            parts.add(new ImagePart(selectedImageBitmap));
            selectedImageBitmap = null;
            userInputEditText.setHint(R.string.send_message);
        }

        if (!userInputText.isEmpty()) {
            // If the user entered text, add it as a part
            parts.add(new TextPart(userInputText));
        }

        if (parts.isEmpty()) {
            Toast.makeText(getContext(), "Don't forget to write something or pick an image!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the user's Content object
        Content.Builder contentBuilder = new Content.Builder();
        contentBuilder.setRole("user");
        for (Part part : parts) {
            contentBuilder.addPart(part);
        }
        Content userContent = contentBuilder.build();

        // Add the user message to the UI list
        addMessage(new ChatMessage("You", formatContentForDisplay(userContent), true));
        userInputEditText.setText(""); // Clear the input field
        chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);

        // Add a placeholder message for the AI response
        ChatMessage aiThinkingMessage = new ChatMessage("AI", "Just a moment, thinking...", false);
        addMessage(aiThinkingMessage); // false indicates AI message
        chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);

        isAwaitingResponse = true;
        setControlsEnabled(false);

        Publisher<GenerateContentResponse> publisher = viewModel.getChat().sendMessageStream(userContent);

        publisher.subscribe(new Subscriber<>() {

            private final ChatMessage currentAiMessage = aiThinkingMessage;
            private final StringBuilder aiResponseBuilder = new StringBuilder();
            private boolean thinkingMessageRemoved = false;

            @Override
            public void onSubscribe(Subscription s) {
                currentSubscription = s;
                s.request(Long.MAX_VALUE); // Request all available data chunks
            }

            @Override
            public void onNext(GenerateContentResponse generateContentResponse) {
                // Received the next part (delta) of the streaming response
                requireActivity().runOnUiThread(() -> {
                    String delta = extractTextDeltaFromResponse(generateContentResponse);

                    if (!delta.isEmpty()) {
                        List<ChatMessage> currentMessageList = viewModel.getMessageList();

                        if (!thinkingMessageRemoved) {
                            int thinkingMessagePosition = currentMessageList.indexOf(currentAiMessage);

                            if (thinkingMessagePosition != -1) {
                                currentMessageList.remove(thinkingMessagePosition);
                                chatAdapter.notifyItemRemoved(thinkingMessagePosition);
                                thinkingMessageRemoved = true;

                                ChatMessage actualAiMessage = new ChatMessage("AI", delta, false);
                                currentMessageList.add(actualAiMessage); // Add the new message
                                chatAdapter.notifyItemInserted(currentMessageList.size() - 1);

                                aiResponseBuilder.append(delta);

                                if (!currentMessageList.isEmpty() && !currentMessageList.get(currentMessageList.size() - 1).isUser()) {
                                    currentMessageList.get(currentMessageList.size() - 1).setText(aiResponseBuilder.toString());
                                    chatAdapter.notifyItemChanged(currentMessageList.size() - 1);
                                }
                                chatRecyclerView.scrollToPosition(currentMessageList.size() - 1);

                            } else {
                                Log.w(TAG, "Received delta but AI thinking placeholder not found.");
                                // If placeholder somehow not found, just add as a new message (less ideal)
                                if (aiResponseBuilder.length() == 0) {
                                    addMessage(new ChatMessage("AI", delta, false));
                                    aiResponseBuilder.append(delta);
                                } else {
                                    if (!currentMessageList.isEmpty() && !currentMessageList.get(currentMessageList.size() - 1).isUser()) {
                                        ChatMessage lastAiMessage = currentMessageList.get(currentMessageList.size() - 1);
                                        lastAiMessage.setText(lastAiMessage.getText() + delta);
                                        chatAdapter.notifyItemChanged(currentMessageList.size() - 1);
                                    } else {
                                        addMessage(new ChatMessage("AI", delta, false));
                                    }
                                    aiResponseBuilder.append(delta); // Continue building
                                }
                                chatRecyclerView.scrollToPosition(currentMessageList.size() - 1);
                            }

                        } else {

                            aiResponseBuilder.append(delta);
                            if (!currentMessageList.isEmpty() && !currentMessageList.get(currentMessageList.size() - 1).isUser()) {
                                ChatMessage lastAiMessage = currentMessageList.get(currentMessageList.size() - 1);
                                lastAiMessage.setText(aiResponseBuilder.toString());
                                chatAdapter.notifyItemChanged(currentMessageList.size() - 1); // Notify adapter that the content of the last item changed
                            } else {
                                addMessage(new ChatMessage("AI", delta, false));
                                aiResponseBuilder.append(delta);
                            }
                            chatRecyclerView.scrollToPosition(currentMessageList.size() - 1);
                        }
                    } else {
                        Log.d(TAG, "Received empty delta chunk.");
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Error during AI response streaming", t);

                requireActivity().runOnUiThread(() -> {
                    List<ChatMessage> currentMessageList = viewModel.getMessageList();

                    int thinkingMessagePosition = currentMessageList.indexOf(currentAiMessage);

                    if (thinkingMessagePosition != -1) {
                        currentMessageList.remove(thinkingMessagePosition);
                        chatAdapter.notifyItemRemoved(thinkingMessagePosition);
                    }
                    addMessage(new ChatMessage("AI", "The assistant is thinking too hard—please try again later.", false));

                    chatRecyclerView.scrollToPosition(currentMessageList.size() - 1);

                    setControlsEnabled(true);
                    isAwaitingResponse = false;
                    awaitingImageConfirmation = false;
                    lastRecommendationQuery = null;
                });
                currentSubscription = null;
            }

            @Override
            public void onComplete() {
                // Streaming completed successfully
                requireActivity().runOnUiThread(() -> {
                    List<ChatMessage> currentMessageList = viewModel.getMessageList();

                    if (aiResponseBuilder.length() == 0) {
                        int thinkingMessagePosition = currentMessageList.indexOf(currentAiMessage);
                        if (thinkingMessagePosition != -1) {
                            currentAiMessage.setText("Oops, I didn’t quite catch that. Could you try rephrasing?");
                            chatAdapter.notifyItemChanged(thinkingMessagePosition);
                        } else {
                            addMessage(new ChatMessage("AI", "Sorry, I didn’t quite get that. Could you try rephrasing?", false));
                        }
                    } else {

                        if (!currentMessageList.isEmpty() && !currentMessageList.get(currentMessageList.size() - 1).isUser()) {
                            currentMessageList.get(currentMessageList.size() - 1).setText(aiResponseBuilder.toString().trim());
                            chatAdapter.notifyItemChanged(currentMessageList.size() - 1);
                        } else {
                            addMessage(new ChatMessage("AI", aiResponseBuilder.toString().trim(), false));
                        }
                    }


                    String finalAiResponse = aiResponseBuilder.toString().trim();
                    Log.d(TAG, "onComplete: Final AI Response String for extraction: '" + finalAiResponse + "'");

                    lastRecommendationQuery = extractPotentialImageQueryFromAIResponse(finalAiResponse);
                    Log.d(TAG, "onComplete: lastRecommendationQuery set to: '" + lastRecommendationQuery + "'");

                    if (lastRecommendationQuery != null && !lastRecommendationQuery.isEmpty()) {

                        awaitingImageConfirmation = true;
                        Log.d(TAG, "AI's response contained an image query. App is now awaiting user confirmation for: " + lastRecommendationQuery);
                    } else {

                        if (finalAiResponse.toLowerCase().contains("would you like") &&
                                (finalAiResponse.toLowerCase().contains("image") ||
                                        finalAiResponse.toLowerCase().contains("example") ||
                                        finalAiResponse.toLowerCase().contains("picture") ||
                                        finalAiResponse.toLowerCase().contains("visual"))) {

                            addMessage(new ChatMessage("AI", "I can help with image suggestions, but I'm not sure what specific images to look for at the moment. Could you be more specific about what you'd like to see?", false));
                        }
                        awaitingImageConfirmation = false;
                        lastRecommendationQuery = null;
                    }

                    isAwaitingResponse = false;
                    setControlsEnabled(true);
                    chatRecyclerView.scrollToPosition(currentMessageList.size() - 1);
                });
                currentSubscription = null;
            }
        });
    }

    private void handlePromptForRecommendation(String userInputText) {
        Log.d(TAG, "handlePromptForRecommendation called. current lastRecommendationQuery: '" + lastRecommendationQuery + "'");

        if (userInputText.toLowerCase().matches(".*(yes|ok|good|want|sure|show|image|photo|picture|look|see|ya|好啊|行|可以|要|看看|发吧|图片|照片).*")) {
            awaitingImageConfirmation = false;

            if (lastRecommendationQuery != null && !lastRecommendationQuery.isEmpty()) {
                addMessage(new ChatMessage("AI", "Fetching images for: " + lastRecommendationQuery + "...", false));
                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);

                fetchOutfitImagesFromWeb(ensureFashionContext(lastRecommendationQuery));
                lastRecommendationQuery = null;
            } else {
                addMessage(new ChatMessage("AI", "Got it! I just don’t have any image ideas to share at the moment.", false));
                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
            }
        } else {
            addMessage(new ChatMessage("AI", "Alright, I won’t show any images then.", false));
            chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
            awaitingImageConfirmation = false;
            lastRecommendationQuery = null;
        }
    }

    private String extractPotentialImageQueryFromAIResponse(String aiResponse) {
        if (aiResponse == null) return null;

        try {
            // First, try to extract from a specific [query:...] tag
            Pattern queryPattern = Pattern.compile("`query:([^`]+)`");
            Matcher matcher = queryPattern.matcher(aiResponse);
            if (matcher.find()) {
                String group = matcher.group(1);
                String extractedQuery = group != null ? group.trim() : "";
                if (!extractedQuery.isEmpty()) {
                    Log.d(TAG, "Extracted query from `query:tag`: " + extractedQuery);
                    return extractedQuery;
                }
            }

            // If a [query:...] tag wasn't found or was empty, then try strict fashion patterns
            Pattern strictFashionPattern = Pattern.compile(
                    "\\b(?:a|an|some|this|that|your)?\\s*" +
                            "((?:(?:casual|formal|trendy|elegant|bohemian|street|minimalist|vintage|athleisure|classic|sporty)\\s+){0,2}?" + // Adjectives
                            "(outfit|look|style|combo|set|fit|clothing|attire|" + // General fashion terms
                            "blazer|jacket|coat|skirt|pants|jeans|shorts|t-shirt|shirt|sweater|hoodie|dress|gown|suit|shoes|sneakers|boots|" + // Specific items
                            "denim|leather|linen|silk|cotton)\\b)",
                    Pattern.CASE_INSENSITIVE);
            String bestMatch = getString(aiResponse, strictFashionPattern);
            if (bestMatch != null) {
                Log.d(TAG, "Extracted query from strict fashion pattern: " + bestMatch);
                return bestMatch;
            }


            String lowerResponse = aiResponse.toLowerCase();
            List<String> triggers = Arrays.asList(
                    "would you like to see", "want to see", "should i show you", "do you want images",
                    "shall i show you", "image", "picture", "photo", "example", "visual"
            );

            for (String trigger : triggers) {
                int idx = lowerResponse.indexOf(trigger);
                if (idx > 0) {
                    int sentenceEnd = Math.max(
                            lowerResponse.lastIndexOf('.', idx),
                            Math.max(lowerResponse.lastIndexOf('!', idx), lowerResponse.lastIndexOf('?', idx))
                    );
                    String context = getString(aiResponse, trigger, sentenceEnd);

                    String cleaned = cleanQueryString(context);
                    if (cleaned.length() >= 5 && cleaned.matches(".*\\b(outfit|look|style|dress|clothing|blazer|jacket|skirt|pants|jeans|shirt|sweater)\\b.*")) {
                        Log.d(TAG, "Extracted query from general trigger context: " + cleaned);
                        return cleaned;
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during query extraction heuristic", e);
        }
        Log.d(TAG, "No suitable query extracted from AI response.");
        return null;
    }

    @NonNull
    private static String getString(String aiResponse, String trigger, int sentenceEnd) {
        String context = "";
        if (sentenceEnd >= 0 && sentenceEnd + 1 < aiResponse.length()) {
            context = aiResponse.substring(sentenceEnd + 1).trim();
            int triggerInContextIdx = context.toLowerCase().indexOf(trigger);
            if (triggerInContextIdx != -1) {
                context = context.substring(0, triggerInContextIdx).trim();
            }
        } else if (sentenceEnd == -1) {
            context = aiResponse.trim();
            int triggerInContextIdx = context.toLowerCase().indexOf(trigger);
            if (triggerInContextIdx != -1) {
                context = context.substring(0, triggerInContextIdx).trim();
            }
        }
        return context;
    }

    @Nullable
    private static String getString(String aiResponse, Pattern strictFashionPattern) {
        Matcher strictMatcher = strictFashionPattern.matcher(aiResponse);

        String bestMatch = null;
        while (strictMatcher.find()) {
            String phrase = Objects.requireNonNull(strictMatcher.group(1)).trim();
            if (phrase.length() >= 5) { // Require a minimum length for the phrase
                if (bestMatch == null || phrase.length() > bestMatch.length()) {
                    bestMatch = phrase;
                }
            }
        }
        return bestMatch;
    }


    private String cleanQueryString(String input) {
        if (input == null) return "";
        return input
                .replaceAll("(?i)i recommend( you)?", "")
                .replaceAll("(?i)you could try", "")
                .replaceAll("(?i)consider wearing", "")
                .replaceAll("(?i)how about", "")
                .replaceAll("(?i)try", "")
                .replaceAll("(?i)something like", "")
                .replaceAll("(?i)here are some", "")
                .replaceAll("(?i)here are a few", "")
                .replaceAll("(?i)to help you visualize them better", "")
                .replaceAll("(?i)for more details", "")
                .replaceAll("(?i)these outfits or styling tips", "")
                .replaceAll("(?i)ll want to look", "")
                .replaceAll("(?i)you might like", "")
                .replaceAll("(?i)is what you are looking for", "")
                .replaceAll("(?i)I could show you", "")
                .replaceAll("[.,!?:]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String ensureFashionContext(String query) {
        if (query == null) return null;

        if (!query.toLowerCase().matches(".*\\b(outfit|style|fashion|clothing|attire|wear)\\b.*")) {
            return query + " outfit";
        }

        return query;
    }

    private void fetchOutfitImagesFromWeb(String query) {
        Log.d(TAG, "About to fetch images using query: '" + lastRecommendationQuery + "'");

        // Ensure the query is not null or empty
        if (query == null || query.trim().isEmpty()) {
            Log.w(TAG, "Attempted to fetch images with empty query.");
            requireActivity().runOnUiThread(() -> {
                addMessage(new ChatMessage("AI", "Sorry, I couldn't find anything to show images for right now.", false));

                chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
            });
            return;
        }

        requireActivity().runOnUiThread(() -> {
            addMessage(new ChatMessage("AI", "Fetching images for '" + query + "'...", false));
            chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
            chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
        });

        executor.execute(() -> {
            String apiKey = BuildConfig.UNSPLASH_API_KEY;

            HttpURLConnection connection = null;
            try {

                String urlString = "https://api.unsplash.com/search/photos?query=" +
                        Uri.encode(query) +
                        "&client_id=" + apiKey + "&per_page=3&orientation=portrait";

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(10000); // 10 seconds
                connection.setConnectTimeout(15000); // 15 seconds

                connection.setRequestProperty("User-Agent", "VibeFitApp/1.0 (Android)");

                int responseCode = connection.getResponseCode();

                // Check if the response was successful (HTTP OK)
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    String result = new BufferedReader(new InputStreamReader(inputStream))
                            .lines().collect(Collectors.joining("\n"));
                    inputStream.close();

                    // Parse the JSON response string
                    JSONObject json = new JSONObject(result);
                    JSONArray photos = json.getJSONArray("results");

                    // Check if any photos were found
                    if (photos.length() == 0) {
                        // No images found for the query
                        requireActivity().runOnUiThread(() -> {
                            addMessage(new ChatMessage("AI", "Sorry, I couldn't find any images for '" + query + "'.", false));
                            chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                            chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
                        });
                        return;
                    }

                    boolean firstImageAdded = false;

                    for (int i = 0; i < photos.length(); i++) {
                        JSONObject photo = photos.getJSONObject(i);

                        JSONObject urls = photo.optJSONObject("urls");
                        String imageUrl = null;
                        if (urls != null) {
                            imageUrl = urls.optString("regular", null);
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            String finalImageUrl = imageUrl;
                            boolean finalFirstImageAdded = firstImageAdded;
                            requireActivity().runOnUiThread(() -> {
                                if (!finalFirstImageAdded) {
                                    List<ChatMessage> currentMessages = viewModel.getMessageList();
                                    if (!currentMessages.isEmpty() && currentMessages.get(currentMessages.size() - 1).getText().contains("Fetching images")) {
                                        currentMessages.remove(currentMessages.size() - 1);
                                        chatAdapter.notifyItemRemoved(currentMessages.size());
                                    }
                                }

                                addMessage(new ChatMessage("AI", "", false, finalImageUrl));
                                chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
                            });
                            firstImageAdded = true;
                        } else {
                            Log.w(TAG, "No valid image URL found for photo index " + i);
                        }
                    }

                } else {
                    // Handle HTTP response codes other than OK
                    Log.e(TAG, "Image API Error: HTTP Response Code " + responseCode + " for query: " + query);
                    requireActivity().runOnUiThread(() -> {
                        addMessage(new ChatMessage("AI", "Sorry, I couldn't fetch images due to an API error (Code: " + responseCode + ").", false));
                        chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                        chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
                    });
                }

            } catch (IOException e) {
                // Handle network I/O errors (e.g., no internet connection)
                Log.e(TAG, "Network Error fetching outfit images for query: " + query, e);
                requireActivity().runOnUiThread(() -> {
                    addMessage(new ChatMessage("AI", "Sorry, I encountered a network error while fetching images.", false));
                    chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                    chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
                });
            } catch (Exception e) {
                // Handle JSON parsing errors or other unexpected exceptions
                Log.e(TAG, "Error processing image API response for query: " + query, e);
                requireActivity().runOnUiThread(() -> {
                    addMessage(new ChatMessage("AI", "Sorry, an unexpected error occurred while processing images.", false));
                    chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                    chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
                });
            } finally {
                // disconnect the HttpURLConnection
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }


    private void addMessage(ChatMessage message) {
        viewModel.getMessageList().add(message);
        chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
    }

    private void setControlsEnabled(boolean enabled) {
        if (userInputEditText != null) {
            userInputEditText.setEnabled(enabled);
        }
        if (sendButton != null) {
            sendButton.setEnabled(enabled);
        }
        if (uploadImageButton != null) {
            uploadImageButton.setEnabled(enabled);
        }
    }

    // Starts image picker intent
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private String formatContentForDisplay(Content content) {
        StringBuilder sb = new StringBuilder();
        for (Part part : content.getParts()) {
            if (part instanceof TextPart) {
                sb.append(((TextPart) part).getText()).append("\n");
            } else if (part instanceof ImagePart) {
                sb.append("[Image Attached]\n");
            }
        }
        return sb.toString().trim();
    }

    @SuppressWarnings({"ConstantConditions"})
    private String extractTextDeltaFromResponse(@NonNull GenerateContentResponse generateContentResponse) {
        StringBuilder deltaBuilder = new StringBuilder();

        for (Candidate candidate : generateContentResponse.getCandidates()) {
            // Get the actual Content from the Candidate
            Content generatedContent = candidate.getContent();

            // Check if Content and its parts are not null
            if (generatedContent != null && generatedContent.getParts() != null) {
                for (Part part : generatedContent.getParts()) {
                    if (part instanceof TextPart) {

                        String text = ((TextPart) part).getText();
                        deltaBuilder.append(text);

                        extractAndLogImageUrls(text);
                    } else {
                        Log.w(TAG, "Unhandled Part type: " + part.getClass().getSimpleName());
                    }
                }
            }

            if (candidate.getFinishReason() != null && candidate.getFinishReason() != FinishReason.UNKNOWN) {
                Log.d(TAG, "Candidate FinishReason: " + candidate.getFinishReason() +
                        " - Text collected so far: " + deltaBuilder);
            }

            if (candidate.getSafetyRatings() != null) {
                Log.d(TAG, "Safety Ratings: " + candidate.getSafetyRatings());
            }
        }

        return deltaBuilder.toString();
    }

    private void extractAndLogImageUrls(String text) {
        Pattern markdownImagePattern = Pattern.compile("!\\[[^]]*]\\(([^)]+)\\)");
        Matcher matcher = markdownImagePattern.matcher(text);
        while (matcher.find()) {
            String imageUrl = matcher.group(1);
            Log.d(TAG, "Detected image reference: " + imageUrl);
        }
    }

}

