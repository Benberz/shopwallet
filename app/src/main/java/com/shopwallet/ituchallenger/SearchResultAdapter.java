package com.shopwallet.ituchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// SearchResultAdapter.java
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private final List<Website> websites;

    public SearchResultAdapter(List<Website> websites) {
        this.websites = websites;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Website website = websites.get(position);
        holder.websiteName.setText(website.getName());
        holder.addButton.setOnClickListener(v -> {
            // Handle add button click logic
        });
    }

    @Override
    public int getItemCount() {
        return websites.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView websiteName;
        public ImageButton addButton;

        public ViewHolder(View itemView) {
            super(itemView);
            websiteName = itemView.findViewById(R.id.website_name);
            addButton = itemView.findViewById(R.id.addButton);
        }
    }
}

