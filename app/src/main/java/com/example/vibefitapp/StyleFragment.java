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

// Used by GenerativeModelFutures
import com.google.firebase.vertexai.type.Content; // Represents user/model message content
import com.google.firebase.vertexai.type.GenerateContentResponse; // Response from the model
import com.google.firebase.vertexai.type.Part;
import com.google.firebase.vertexai.type.TextPart;
import com.google.firebase.vertexai.type.ImagePart;
import com.google.firebase.vertexai.type.FinishReason;
import com.google.firebase.vertexai.type.Candidate;

import androidx.lifecycle.ViewModelProvider;

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
// Helper for lists
import java.util.Arrays;
import java.util.List;
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

        viewModel = new ViewModelProvider(this).get(StyleViewModel.class);

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
                                    Toast.makeText(getContext(), "Failed to decode image", Toast.LENGTH_SHORT).show();
                                    selectedImageBitmap = null;
                                    userInputEditText.setHint(R.string.send_message);
                                    return;
                                }

                                selectedImageBitmap = compressBitmap(bitmap);

                                Toast.makeText(getContext(), "Image selected, add text and send!", Toast.LENGTH_SHORT).show();
                                userInputEditText.setHint("Enter text related to the image...");

                            } catch (IOException e) {
                                Log.e(TAG, "Error loading image", e);
                                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                                selectedImageBitmap = null;
                                userInputEditText.setHint(R.string.send_message);

                            } catch (Exception e) {
                                Log.e(TAG, "Unexpected error selecting image", e);
                                Toast.makeText(getContext(), "An error occurred selecting the image", Toast.LENGTH_SHORT).show();
                                selectedImageBitmap = null;
                                userInputEditText.setHint(R.string.send_message);
                            }
                        } else {
                            // --- Handle the case where imageUri is unexpectedly null ---
                            Log.w(TAG, "Image Uri is null from result data even though data Intent was not null.");
                            Toast.makeText(getContext(), "Could not get image Uri", Toast.LENGTH_SHORT).show();
                            selectedImageBitmap = null; // Ensure it's null
                            userInputEditText.setHint(R.string.send_message); // Restore hint
                        }

                    } else {
                        Log.d(TAG, "Image picking cancelled or no data returned.");
                        selectedImageBitmap = null; // Clear any potentially half-selected image
                        userInputEditText.setHint(R.string.send_message); // Restore hint
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

        // Set click listeners for buttons
        sendButton.setOnClickListener(v -> sendMessage());
        uploadImageButton.setOnClickListener(v -> pickImage());

        if (viewModel.getMessageList().isEmpty()) {
            addMessage(new ChatMessage("AI", "Hello! I'm your personal fashion assistant. Tell me about your style, body shape, or upload clothing photos, and I'll give you pairing suggestions!", false)); // AI welcome message
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
            return original; // Return original if scaled dimensions are invalid or no scaling occurred
        }

        try {
            return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        } catch (Exception e) {
            Log.e(TAG, "Error creating scaled bitmap", e);
            return original; // Return original if scaling fails
        }
    }

    // Handles sending a message to the AI
    private void sendMessage() {
        String userInputText = userInputEditText.getText().toString().trim();

        // --- Handle the user's confirmation for images ---
        if (awaitingImageConfirmation) {
            handlePromptForRecommendation(userInputText);
            userInputEditText.setText(""); // Clear input field after handling confirmation
            return;
        }

        // Prevent sending multiple messages while waiting for a response
        if (isAwaitingResponse) {
            Toast.makeText(getContext(), "Please wait for the previous response to complete...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the Content object to send to the model
        List<Part> parts = new ArrayList<>();

        if (selectedImageBitmap != null) {
            parts.add(new ImagePart(selectedImageBitmap));
            selectedImageBitmap = null; // Clear the selected image after adding it to the message
            userInputEditText.setHint(R.string.send_message);
        }

        if (!userInputText.isEmpty()) {
            // If the user entered text, add it as a part
            parts.add(new TextPart(userInputText));
        }

        if (parts.isEmpty()) {
            // Don't send if there's no text and no image
            Toast.makeText(getContext(), "Please enter text or select an image!", Toast.LENGTH_SHORT).show();
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
        addMessage(new ChatMessage("You", formatContentForDisplay(userContent), true)); // true indicates user message
        userInputEditText.setText(""); // Clear the input field
        chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);

        // Add a placeholder message for the AI response
        ChatMessage aiThinkingMessage = new ChatMessage("AI", "Thinking...", false);
        addMessage(aiThinkingMessage); // false indicates AI message
        chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);

        // Update UI state
        isAwaitingResponse = true;
        setControlsEnabled(false); // Disable input controls

        // Send the message to the AI model using the chat object (streaming)
        Publisher<GenerateContentResponse> publisher = viewModel.getChat().sendMessageStream(userContent);

        // Subscribe to the streaming response
        publisher.subscribe(new Subscriber<>() {

            private final ChatMessage currentAiMessage = aiThinkingMessage; // Reference to the placeholder message
            private final StringBuilder aiResponseBuilder = new StringBuilder(); // To build the full AI response text
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

                    // Only process if there's actual text received in this chunk
                    if (!delta.isEmpty()) {
                        List<ChatMessage> currentMessageList = viewModel.getMessageList();

                        if (!thinkingMessageRemoved) {
                            int thinkingMessagePosition = currentMessageList.indexOf(currentAiMessage);

                            if (thinkingMessagePosition != -1) {
                                currentMessageList.remove(thinkingMessagePosition);
                                chatAdapter.notifyItemRemoved(thinkingMessagePosition);
                                thinkingMessageRemoved = true; // Mark placeholder as removed

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
                                // This case might happen if onNext is called after onComplete/onError cleared things.
                                Log.w(TAG, "Received delta but AI thinking placeholder not found.");
                                // If placeholder somehow not found, just add as a new message (less ideal)
                                if (aiResponseBuilder.length() == 0) { // Only add if this is the very first chunk
                                    addMessage(new ChatMessage("AI", delta, false));
                                    aiResponseBuilder.append(delta); // Start building
                                } else { // Otherwise, append to the last message if it's AI's
                                    if (!currentMessageList.isEmpty() && !currentMessageList.get(currentMessageList.size() - 1).isUser()) {
                                        ChatMessage lastAiMessage = currentMessageList.get(currentMessageList.size() - 1);
                                        lastAiMessage.setText(lastAiMessage.getText() + delta);
                                        chatAdapter.notifyItemChanged(currentMessageList.size() - 1);
                                    } else {
                                        // If the last message isn't AI's or list is empty, something is wrong. Add new.
                                        addMessage(new ChatMessage("AI", delta, false));
                                    }
                                    aiResponseBuilder.append(delta); // Continue building
                                }
                                chatRecyclerView.scrollToPosition(currentMessageList.size() - 1);
                            }

                        } else {

                            aiResponseBuilder.append(delta);
                            // Update the text of the *last* AI message in the list
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
                    // Received an empty delta chunk, sometimes happens. Log it but no UI update.
                    Log.d(TAG, "Received empty delta chunk.");
                }
            });
        }

            @Override
            public void onError(Throwable t) {
                // An error occurred during streaming
                Log.e(TAG, "Error during AI response streaming", t);
                // Update UI on the main thread to show the error
                requireActivity().runOnUiThread(() -> {
                    List<ChatMessage> currentMessageList = viewModel.getMessageList();

                    int thinkingMessagePosition = currentMessageList.indexOf(currentAiMessage);

                    // Remove the placeholder message
                    if (thinkingMessagePosition != -1) {
                        currentMessageList.remove(thinkingMessagePosition);
                        chatAdapter.notifyItemRemoved(thinkingMessagePosition);
                    }
                    // Add an error message to the UI
                    addMessage(new ChatMessage("AI", "Sorry, an error occurred: " + t.getLocalizedMessage(), false));

                    // Scroll to the latest message (which is now the error message)
                    chatRecyclerView.scrollToPosition(currentMessageList.size() - 1);

                    // Re-enable controls and reset state
                    setControlsEnabled(true);
                    isAwaitingResponse = false;
                    awaitingImageConfirmation = false; // Reset image confirmation state on error
                    lastRecommendationQuery = null; // Clear stored query on error
                });
                currentSubscription = null; // Clear the subscription reference
            }

            @Override
            public void onComplete() {
                // Streaming completed successfully
                requireActivity().runOnUiThread(() -> {
                    List<ChatMessage> currentMessageList = viewModel.getMessageList();

                    if (aiResponseBuilder.length() == 0) {
                        int thinkingMessagePosition = currentMessageList.indexOf(currentAiMessage);
                        if (thinkingMessagePosition != -1) {
                            currentAiMessage.setText("Hmm... I didn't get that. Try rephrasing?");
                            chatAdapter.notifyItemChanged(thinkingMessagePosition);
                        } else {
                            addMessage(new ChatMessage("AI", "Hmm... I didn't get that. Try rephrasing?", false));
                        }
                    }

                    // Get the final AI response text
                    String finalAiResponse = aiResponseBuilder.toString().trim();

                    if (finalAiResponse.toLowerCase().contains("would you like") && finalAiResponse.toLowerCase().contains("image")) {
                        awaitingImageConfirmation = true;
                        // Attempt to extract the core fashion recommendation query from the AI's response.
                        lastRecommendationQuery = extractPotentialImageQueryFromAIResponse(finalAiResponse);
                        if (lastRecommendationQuery == null || lastRecommendationQuery.isEmpty()) {
                            // Fallback: if extraction failed, maybe use the last user input text as a query?
                            Log.w(TAG, "Failed to extract a specific image query from AI response. Awaiting confirmation for general query.");
                        }
                        Log.d(TAG, "AI asked for image confirmation. Awaiting 'yes'. Stored query: " + lastRecommendationQuery);

                    } else {
                        awaitingImageConfirmation = false; // No confirmation needed
                        lastRecommendationQuery = null; // Clear any old query
                    }

                    // Re-enable controls for next input
                    isAwaitingResponse = false;
                    setControlsEnabled(true);
                });
                currentSubscription = null; // Clear the subscription reference
            }
        });
    }

    private void handlePromptForRecommendation(String userInputText) {
        if (userInputText.toLowerCase().matches(".*(yes|好啊|行|可以|要|发吧).*")) {
            awaitingImageConfirmation = false;
            if (lastRecommendationQuery != null && !lastRecommendationQuery.isEmpty()) {
                addMessage(new ChatMessage("AI", "Fetching images for: " + lastRecommendationQuery + "...", false));
                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);

                fetchOutfitImagesFromWeb(lastRecommendationQuery);
                lastRecommendationQuery = null;
            } else {
                addMessage(new ChatMessage("AI", "Okay, but I don't have a specific recommendation to show images for right now.", false));
                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
            }
        }else{
            addMessage(new ChatMessage("AI", "Okay, I won't show images then.", false));
            chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1); // Use list from ViewModel
            awaitingImageConfirmation = false; // Reset confirmation state
            lastRecommendationQuery = null; // Clear stored query
        }
    }

    private String extractPotentialImageQueryFromAIResponse(String aiResponse) {
        if (aiResponse == null) return null;
        // Try to parse [query: ...] marker first (most reliable)
        try {
            Pattern queryPattern = Pattern.compile("\\[query:([^]]+)]");
            Matcher matcher = queryPattern.matcher(aiResponse);
            if (matcher.find()) {
                String group = matcher.group(1);
                return group != null ? group.trim() : null;
            }

            // Fallback: Heuristic method based on location of image request
            String lowerResponse = aiResponse.toLowerCase();
            int imageQuestionIndex = -1;

            // Look for common image offering questions
            List<String> triggers = Arrays.asList(
                    "would you like to see image",
                    "would you like to see some pictures",
                    "would you like to see photos",
                    "should i show you images",
                    "want to see how it looks"
            );

            for (String trigger : triggers) {
                if (lowerResponse.contains(trigger)) {
                    imageQuestionIndex = lowerResponse.indexOf(trigger);
                    break;
                }
            }

            if (imageQuestionIndex > 0) {
                int lastPeriod = lowerResponse.lastIndexOf('.', imageQuestionIndex - 1);
                int lastQuestion = lowerResponse.lastIndexOf('?', imageQuestionIndex - 1);
                int lastExclamation = lowerResponse.lastIndexOf('!', imageQuestionIndex - 1);
                int sentenceEnd = Math.max(lastPeriod, Math.max(lastQuestion, lastExclamation));

                if (sentenceEnd != -1 && sentenceEnd + 1 < imageQuestionIndex) {
                    String potentialQuerySection = aiResponse.substring(sentenceEnd + 1, imageQuestionIndex);
                    String cleaned = cleanQueryString(potentialQuerySection);
                    if (cleaned.length() >= 3) {  // Require at least 3 characters for a meaningful query
                        return cleaned;
                    }
                } else {
                    Log.w(TAG, "Could not find sentence end before image question.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during query extraction heuristic", e);
        }
        return null;
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
                .trim();
    }

    /**
     * Fetches outfit images from a web API (like Unsplash) based on a query string.
     * IMPORTANT: Replace "YOUR_UNSPLASH_API_KEY" with your actual API key.
     * This uses a basic HttpURLConnection and org.json for parsing.
     * For a production app, consider using a dedicated library like Retrofit or Volley.
     * This runs on the executor service to avoid blocking the main thread.
     */
    private void fetchOutfitImagesFromWeb(String query) {
        // Ensure the query is not null or empty
        if (query == null || query.trim().isEmpty()) {
            Log.w(TAG, "Attempted to fetch images with empty query.");
            requireActivity().runOnUiThread(() -> {
                addMessage(new ChatMessage("AI", "Sorry, I couldn't find a specific query for images right now.", false));
                // Using notifyItemInserted after addMessage is important for the adapter to know a new item exists.
                chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
            });
            return;
        }

        // Execute the network request on the background executor service
        executor.execute(() -> {
            String apiKey = BuildConfig.UNSPLASH_API_KEY;

            HttpURLConnection connection = null; // Initialize connection variable
            try {
                // Build the API URL. Using Uri.encode to handle spaces and special characters in the query.
                // Added per_page=3 to limit results and orientation=portrait for potentially better outfit photos.
                String urlString = "https://api.unsplash.com/search/photos?query=" +
                        Uri.encode(query) +
                        "&client_id=" + apiKey + "&per_page=3&orientation=portrait";

                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                // Set read and connect timeouts to prevent ANRs on slow networks
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

                    for (int i = 0; i < photos.length(); i++) {
                        JSONObject photo = photos.getJSONObject(i);

                        JSONObject urls = photo.optJSONObject("urls");
                        String imageUrl = null;
                        if (urls != null) {
                            imageUrl = urls.optString("regular", null);
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            String finalImageUrl = imageUrl; // for lambda
                            requireActivity().runOnUiThread(() -> {
                                addMessage(new ChatMessage("AI", "Image for: " + query, false, finalImageUrl));
                                chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
                                chatRecyclerView.scrollToPosition(viewModel.getMessageList().size() - 1);
                            });
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


    // Utility: Add a ChatMessage to the list and notify adapter
            private void addMessage(ChatMessage message) {
                viewModel.getMessageList().add(message);
                chatAdapter.notifyItemInserted(viewModel.getMessageList().size() - 1);
            }

            // Utility: Enable or disable input buttons and text field
            private void setControlsEnabled(boolean enabled) {
                // Null checks added for safety, although UI elements should be initialized in onCreateView
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

            // Optional helper: Format Content object to a display-friendly string
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
                        // Append the text from the TextPart
                        deltaBuilder.append(((TextPart) part).getText());
                    }
                    // TODO (Requirement 4): If the AI response includes information that
                    // indicates an image reference (like a specific markdown syntax or
                    // if the AI could actually generate structured output indicating URLs,
                    // though Gemini Pro generally doesn't generate images itself but can
                    // generate text about them), you would handle parsing that Part here.
                    // This part of the code would likely need to identify specific Part types
                    // beyond TextPart or parse the text for image URLs/identifiers.
                }
            }

            if(candidate.getFinishReason() != null && candidate.getFinishReason() != FinishReason.UNKNOWN) {
                Log.d(TAG, "Candidate FinishReason: " + candidate.getFinishReason() +
                        " - Text collected so far: " + deltaBuilder);
                // Depending on the finish reason (e.g., SAFETY, PROHIBITED_CONTENT), you might
                // want to append a specific message to the deltaBuilder or log more details.
            }

            // TODO: You might also want to check candidate.getFinishReason() or candidate.getSafetyRatings()
            // for this specific candidate if you needed to react per-candidate rather than for the whole response.
        }

        return deltaBuilder.toString();
    }
}

