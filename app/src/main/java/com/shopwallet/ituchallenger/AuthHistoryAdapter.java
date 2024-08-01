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

public class AuthHistoryAdapter extends RecyclerView.Adapter<AuthHistoryAdapter.AuthHistoryViewHolder> {

    private final Context context;
    private final List<AuthHistory> authHistoryList;

    public AuthHistoryAdapter(Context context, List<AuthHistory> authHistoryList) {
        this.context = context;
        this.authHistoryList = authHistoryList;
    }

    @NonNull
    @Override
    public AuthHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_auth_history, parent, false);
        return new AuthHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuthHistoryViewHolder holder, int position) {
        AuthHistory authHistory = authHistoryList.get(position);
        holder.authHistoryTitleTextView.setText(authHistory.getTitle());
        holder.authHistoryDateTimeTextView.setText(authHistory.getDateTime());

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
        return authHistoryList.size();
    }

    public static class AuthHistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView authHistoryTitleTextView;
        private final TextView authHistoryDateTimeTextView;
        private final ImageView authHistoryIconImageView;
        private final TextView authStatusTextView;

        public AuthHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            authHistoryTitleTextView = itemView.findViewById(R.id.authHistoryTitleTextView);
            authHistoryDateTimeTextView = itemView.findViewById(R.id.authHistoryDateTimeTextView);
            authHistoryIconImageView = itemView.findViewById(R.id.authHistoryIconImageView);
            authStatusTextView = itemView.findViewById(R.id.authStatusTextView);
        }
    }
}