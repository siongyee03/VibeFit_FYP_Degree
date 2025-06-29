package com.example.vibefitapp;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.OutputStream;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private final List<ChatMessage> messageList;
    private final Context context;

    public ChatAdapter(List<ChatMessage> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_USER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_model, parent, false);
            return new AiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((AiMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private Spanned convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return Html.fromHtml("", Html.FROM_HTML_MODE_COMPACT);
        }

        // Convert **bold** to <b>bold</b>
        String html = markdown.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");

        // Convert double newlines to paragraph breaks
        html = html.replaceAll("\n\n", "<br><br>");

        // Convert single newline to line break
        html = html.replaceAll("(?<!<br>)\n", "<br>");

        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
    }


    // ViewHolder for user messages
    class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView userImageView;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.user_message_text);
            userImageView = itemView.findViewById(R.id.user_image_view);
        }

        void bind(ChatMessage message) {
            if (message.getText() != null && !message.getText().isEmpty()) {
                messageText.setText(convertMarkdownToHtml(message.getText()));
                messageText.setVisibility(View.VISIBLE);
            } else {
                messageText.setVisibility(View.GONE);
            }

            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                userImageView.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(message.getImageUrl()).into(userImageView);

                userImageView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ZoomableImageActivity.class);
                    intent.putExtra("imageUrl", message.getImageUrl());
                    context.startActivity(intent);
                });

                userImageView.setOnLongClickListener(v -> {
                    saveImage(message.getImageUrl());
                    return true;
                });
            } else {
                userImageView.setVisibility(View.GONE);
            }
        }
    }

    // ViewHolder for AI messages
    class AiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView aiImageView;
        ImageButton saveButton;

        AiMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.model_message_text);
            aiImageView = itemView.findViewById(R.id.image_view);
            saveButton = itemView.findViewById(R.id.save_button);
        }

        void bind(ChatMessage message) {
            if (message.getText() != null && !message.getText().isEmpty()) {
                messageText.setText(convertMarkdownToHtml(message.getText()));
                messageText.setVisibility(View.VISIBLE);
            } else {
                messageText.setVisibility(View.GONE);
            }

            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                aiImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getImageUrl()).into(aiImageView);
                saveButton.setVisibility(View.VISIBLE);

                aiImageView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ZoomableImageActivity.class);
                    intent.putExtra("imageUrl", message.getImageUrl());
                    context.startActivity(intent);
                });

                aiImageView.setOnLongClickListener(v -> {
                    saveImage(message.getImageUrl());
                    return true;
                });

                saveButton.setOnClickListener(v -> saveImage(message.getImageUrl()));
            } else {
                aiImageView.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
            }
        }
    }

    private void saveImage(String imageUrl) {
        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        String displayName = "chat_image_" + System.currentTimeMillis() + ".jpg";
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VibeFit");

                        Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        if (imageUri != null) {
                            try {
                                OutputStream out = context.getContentResolver().openOutputStream(imageUri);
                                if (out != null) {
                                    try (out) {
                                        resource.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                        Toast.makeText(context, "Image saved to gallery!", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(context, "Failed to open output stream.", Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Log.e("ChatAdapter", "Failed to save image", e);
                                Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Failed to create media entry.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }
}
