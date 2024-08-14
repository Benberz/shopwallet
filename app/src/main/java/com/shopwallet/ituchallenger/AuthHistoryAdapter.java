package com.shopwallet.ituchallenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView Adapter for displaying a list of authentication history items.
 */
public class AuthHistoryAdapter extends RecyclerView.Adapter<AuthHistoryAdapter.AuthHistoryViewHolder> {

    private final Context context;
    private final List<AuthHistory> authHistoryList;

    /**
     * Constructs an AuthHistoryAdapter with the specified context and authentication history list.
     *
     * @param context          The context for inflating views.
     * @param authHistoryList  The list of authentication history items to be displayed.
     */
    public AuthHistoryAdapter(Context context, List<AuthHistory> authHistoryList) {
        this.context = context;
        this.authHistoryList = authHistoryList;
    }

    @NonNull
    @Override
    public AuthHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the RecyclerView
        View view = LayoutInflater.from(context).inflate(R.layout.item_auth_history, parent, false);
        return new AuthHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuthHistoryViewHolder holder, int position) {
        // Bind the data to the views
        AuthHistory authHistory = authHistoryList.get(position);
        holder.authHistoryTitleTextView.setText(authHistory.getTitle());
        holder.authHistoryDateTimeTextView.setText(authHistory.getDateTime());

        // Set icon and status based on the authentication status
        if (authHistory.getStatus().equalsIgnoreCase("success")) {
            holder.authHistoryIconImageView.setImageResource(R.drawable.ic_success);
            holder.authStatusTextView.setText(R.string.status_success);
        } else {
            holder.authHistoryIconImageView.setImageResource(R.drawable.ic_failure);
            holder.authStatusTextView.setText(R.string.status_failure);
        }
    }

    @Override
    public int getItemCount() {
        // Return the number of items in the authentication history list
        return authHistoryList.size();
    }

    /**
     * ViewHolder for holding and managing views of each item in the RecyclerView.
     */
    public static class AuthHistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView authHistoryTitleTextView;
        private final TextView authHistoryDateTimeTextView;
        private final ImageView authHistoryIconImageView;
        private final TextView authStatusTextView;

        /**
         * Constructs an AuthHistoryViewHolder and initializes the view references.
         *
         * @param itemView The view for an individual item in the RecyclerView.
         */
        public AuthHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            authHistoryTitleTextView = itemView.findViewById(R.id.authHistoryTitleTextView);
            authHistoryDateTimeTextView = itemView.findViewById(R.id.authHistoryDateTimeTextView);
            authHistoryIconImageView = itemView.findViewById(R.id.authHistoryIconImageView);
            authStatusTextView = itemView.findViewById(R.id.authStatusTextView);
        }
    }
}