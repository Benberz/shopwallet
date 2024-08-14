package com.shopwallet.ituchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter class for displaying a list of search results in a RecyclerView.
 * Each item in the RecyclerView represents a website with a name and an "add" button.
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    // List of websites to be displayed in the RecyclerView
    private final List<Website> websites;

    /**
     * Constructor for the SearchResultAdapter.
     * @param websites List of Website objects to be displayed.
     */
    public SearchResultAdapter(List<Website> websites) {
        this.websites = websites;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the item layout and create a new ViewHolder instance
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Bind the data to the ViewHolder
        Website website = websites.get(position);
        holder.websiteName.setText(website.getName());

        // Set a click listener for the add button
        holder.addButton.setOnClickListener(v -> {
            // Handle add button click logic here
            // For example, add the website to a list of favorites
        });
    }

    @Override
    public int getItemCount() {
        // Return the total number of items in the data set
        return websites.size();
    }

    /**
     * ViewHolder class for holding references to the views for each item in the RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView websiteName;
        public ImageButton addButton;

        /**
         * Constructor for the ViewHolder.
         * @param itemView The view of the item layout.
         */
        public ViewHolder(View itemView) {
            super(itemView);
            // Initialize the views for this ViewHolder
            websiteName = itemView.findViewById(R.id.website_name);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}