package com.shopwallet.ituchallenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class TransactionAdapter extends ArrayAdapter<Transaction> {

    private static final int VIEW_TYPE_TRANSACTION = 0;
    private static final int VIEW_TYPE_EMPTY = 1;

    public TransactionAdapter(Context context, ArrayList<Transaction> transactions) {
        super(context, 0, transactions);
    }

    @Override
    public int getViewTypeCount() {
        return 2; // Two types of views: transaction and empty
    }

    @Override
    public int getItemViewType(int position) {
        if (super.getCount() == 0) {
            return VIEW_TYPE_EMPTY;
        } else {
            return VIEW_TYPE_TRANSACTION;
        }
    }

    @Override
    public int getCount() {
        if (super.getCount() == 0) {
            return 1; // Show the empty view
        } else {
            return super.getCount();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_EMPTY) {
            // Inflate and return the empty view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_no_transactions, parent, false);
            }
            TextView emptyMessage = convertView.findViewById(R.id.emptyMessage);
            emptyMessage.setText(R.string.no_recent_transactions);
        } else {
            // Inflate and return the transaction view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, parent, false);
            }

            Transaction transaction = getItem(position);

            ImageView icon = convertView.findViewById(R.id.transactionIcon);
            TextView title = convertView.findViewById(R.id.transactionTitle);
            TextView amount = convertView.findViewById(R.id.transactionAmount);
            TextView date = convertView.findViewById(R.id.transactionDate);

            // Set the transaction data
            title.setText(transaction.getTitle());
            amount.setText(transaction.getAmount());
            date.setText(transaction.getDatetime());

            // Set the icon based on the transaction
            icon.setImageResource(transaction.getIconResId());
        }

        return convertView;
    }
}