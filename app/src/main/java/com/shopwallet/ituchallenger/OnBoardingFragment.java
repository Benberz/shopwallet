package com.shopwallet.ituchallenger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass for displaying onBoarding screens.
 * This fragment shows a title, description, and image.
 * Use the {@link OnBoardingFragment#newInstance} factory method to
 * create an instance of this fragment with specific content.
 */
public class OnBoardingFragment extends Fragment {

    // Argument keys for storing and retrieving fragment data
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IMAGE = "image";

    /**
     * Required empty public constructor.
     */
    public OnBoardingFragment() {
        // This is intentionally left empty as required by Fragment subclasses
    }

    /**
     * Factory method to create a new instance of OnBoardingFragment using the provided parameters.
     *
     * @param title       The title text for the fragment.
     * @param description The description text for the fragment.
     * @param imageResId  The resource ID of the image to be displayed.
     * @return A new instance of OnBoardingFragment.
     */
    public static OnBoardingFragment newInstance(String title, String description, int imageResId) {
        // Create a new instance of the fragment
        OnBoardingFragment fragment = new OnBoardingFragment();

        // Bundle the provided arguments to pass to the fragment
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_IMAGE, imageResId);

        // Set the arguments on the fragment
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_onboarding, container, false);

        // Initialize the UI elements
        TextView textViewTitle = view.findViewById(R.id.textViewTitle);
        TextView textViewDescription = view.findViewById(R.id.textViewDescription);
        ImageView imageView = view.findViewById(R.id.imageView);

        // If arguments were passed to the fragment, set the corresponding UI elements
        if (getArguments() != null) {
            textViewTitle.setText(getArguments().getString(ARG_TITLE));
            textViewDescription.setText(getArguments().getString(ARG_DESCRIPTION));
            imageView.setImageResource(getArguments().getInt(ARG_IMAGE));
        }

        // Return the fully constructed view to be displayed
        return view;
    }
}