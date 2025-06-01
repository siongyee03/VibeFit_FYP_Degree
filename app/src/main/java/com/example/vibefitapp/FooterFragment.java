package com.example.vibefitapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FooterFragment extends Fragment {

    private LinearLayout footerHome, footerCloset, footerStyle, footerMe;
    private ImageView iconHome, iconCloset, iconStyle, iconMe;
    private TextView textHome, textCloset, textStyle, textMe;
    private ImageButton uploadButton;
    private String recommendedCategory = "tutorial/pattern";
    private int selectedTab = -1; // Track the currently selected Tab; -1 indicates that none is selected.
    private ActivityResultLauncher<Intent> mediaPickerLauncher;

    public FooterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == FragmentActivity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        boolean hasVideo = false;
                        Uri videoUri = null;
                        List<Uri> imageUris = new ArrayList<>();

                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = data.getClipData().getItemAt(i).getUri();
                                String type = requireActivity().getContentResolver().getType(uri);
                                if (type != null && type.startsWith("video")) {
                                    hasVideo = true;
                                    videoUri = uri;
                                    break;
                                } else if (type != null && type.startsWith("image")) {
                                    imageUris.add(uri);
                                }
                            }

                            if (hasVideo && count > 1) {
                                Toast.makeText(getContext(), "Cannot select both video and image at the same time.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                        } else if (data.getData() != null) {
                            Uri uri = data.getData();
                            String type = requireActivity().getContentResolver().getType(uri);
                            if (type != null && type.startsWith("video")) {
                                videoUri = uri;
                            } else if (type != null && type.startsWith("image")) {
                                imageUris.add(uri);
                            }
                        }

                        Intent intent = new Intent(getActivity(), MediaEditActivity.class);
                        if (videoUri != null) {
                            intent.putExtra("video_uri", videoUri.toString());
                        } else if (!imageUris.isEmpty()) {
                            ArrayList<String> imageUriStrings = new ArrayList<>();
                            for (Uri uri : imageUris) {
                                imageUriStrings.add(uri.toString());
                            }
                            intent.putStringArrayListExtra("image_uris", imageUriStrings);
                    } else {
                            Toast.makeText(getContext(), "Please select an image or video.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (getActivity() instanceof HomeActivity) {
                            String currentTab = ((HomeActivity) getActivity()).getCurrentHeaderTab();
                            intent.putExtra("source_tab", currentTab);
                        }

                        intent.putExtra("recommended_category", recommendedCategory);

                        startActivity(intent);
                    }
                }
        );
    }

    private void selectMedia() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        mediaPickerLauncher.launch(intent);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_footer, container, false);

        footerHome = view.findViewById(R.id.footer_home);
        footerCloset = view.findViewById(R.id.footer_closet);
        footerStyle = view.findViewById(R.id.footer_style);
        footerMe = view.findViewById(R.id.footer_me);

        iconHome = view.findViewById(R.id.icon_home);
        iconCloset = view.findViewById(R.id.icon_closet);
        iconStyle = view.findViewById(R.id.icon_style);
        iconMe = view.findViewById(R.id.icon_me);

        textHome = view.findViewById(R.id.text_home);
        textCloset = view.findViewById(R.id.text_closet);
        textStyle = view.findViewById(R.id.text_style);
        textMe = view.findViewById(R.id.text_me);

        uploadButton = view.findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                HomeActivity homeActivity = (HomeActivity) getActivity();
                String currentTab = homeActivity.getCurrentHeaderTab();

                switch (currentTab) {
                    case "explore":
                        recommendedCategory = "tutorial/pattern";
                        break;
                    case "forum":
                        recommendedCategory = "forum";
                        break;
                }
                selectMedia();
            }
        });


        // Set up click events
        footerHome.setOnClickListener(v -> selectTab(0));
        footerCloset.setOnClickListener(v -> selectTab(1));
        footerStyle.setOnClickListener(v -> selectTab(2));
        footerMe.setOnClickListener(v -> selectTab(3));

        selectTab(0);
        return view;
    }

    private void selectTab(int tabIndex) {
        selectedTab = tabIndex;
        resetIcons();

        Fragment selectedFragment = null;
        int purpleMain = ContextCompat.getColor(requireContext(), R.color.purple_main);

        if (getActivity() instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) getActivity();

            switch (tabIndex) {
                case 0:
                    iconHome.setImageResource(R.drawable.ic_home_hover);
                    textHome.setTextColor(purpleMain);
                    uploadButton.setVisibility(View.VISIBLE);
                    selectedFragment = new ExploreFragment();

                    homeActivity.setHeaderVisibility(true);
                    homeActivity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.header_container, new HeaderFragment())
                            .commit();
                    break;

                case 1:
                    iconCloset.setImageResource(R.drawable.ic_closet_hover);
                    textCloset.setTextColor(purpleMain);
                    uploadButton.setVisibility(View.GONE);
                    selectedFragment = new ClosetFragment();
                    homeActivity.setHeaderVisibility(false);
                    break;

                case 2:
                    iconStyle.setImageResource(R.drawable.ic_style_hover);
                    textStyle.setTextColor(purpleMain);
                    uploadButton.setVisibility(View.GONE);
                    selectedFragment = new StyleFragment();
                    homeActivity.setHeaderVisibility(false);
                    break;

                case 3:
                    iconMe.setImageResource(R.drawable.ic_me_hover);
                    textMe.setTextColor(purpleMain);
                    uploadButton.setVisibility(View.GONE);
                    selectedFragment = new MeFragment();
                    homeActivity.setHeaderVisibility(false);
                    break;
            }

            if (selectedFragment != null) {
                homeActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, selectedFragment)
                        .commit();
            }
        }
    }


    private void resetIcons() {
        // Reset all icons to non-hover style.
        iconHome.setImageResource(R.drawable.ic_home);
        iconCloset.setImageResource(R.drawable.ic_closet);
        iconStyle.setImageResource(R.drawable.ic_style);
        iconMe.setImageResource(R.drawable.ic_me);

        // Reset all button borders to default.
        footerHome.setBackgroundResource(0);
        footerCloset.setBackgroundResource(0);
        footerStyle.setBackgroundResource(0);
        footerMe.setBackgroundResource(0);

        int gray = ContextCompat.getColor(requireContext(), R.color.gray);
        textHome.setTextColor(gray);
        textCloset.setTextColor(gray);
        textStyle.setTextColor(gray);
        textMe.setTextColor(gray);
    }

}