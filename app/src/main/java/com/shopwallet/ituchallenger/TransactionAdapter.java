package com.shopwallet.ituchallenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * TransactionAdapter is a custom ArrayAdapter for displaying a list of Transaction objects.
 * It handles two types of views: one for transactions and another for an empty state
 * when no transactions are available.
 */
public class TransactionAdapter extends ArrayAdapter<Transaction> {

    // Constant representing the view type for a transaction item
    private static final int VIEW_TYPE_TRANSACTION = 0;

    // Constant representing the view type for an empty view
    private static final int VIEW_TYPE_EMPTY = 1;

    /**
     * Constructor for TransactionAdapter.
     *
     * @param context The current context.
     * @param transactions The list of transactions to be displayed.
     */
    public TransactionAdapter(Context context, ArrayList<Transaction> transactions) {
        super(context, 0, transactions);
    }

    /**
     * Returns the number of different view types managed by the adapter.
     *
     * @return The number of view types (2: transaction and empty).
     */
    @Override
    public int getViewTypeCount() {
        return 2; // Two types of views: transaction and empty
    }

    /**
     * Returns the view type for the item at the specified position.
     *
     * @param position The position of the item.
     * @return The view type (transaction or empty).
     */
    @Override
    public int getItemViewType(int position) {
        if (super.getCount() == 0) {
            return VIEW_TYPE_EMPTY; // Return empty view type if there are no transactions
        } else {
            return VIEW_TYPE_TRANSACTION; // Return transaction view type otherwise
        }
    }

    /**
     * Returns the number of items in the adapter, including the empty view.
     *
     * @return The number of items (1 if empty view is shown, otherwise the actual count).
     */
    @Override
    public int getCount() {
        if (super.getCount() == 0) {
            return 1; // Return 1 to show the empty view
        } else {
            return super.getCount(); // Return the actual count of transactions
        }
    }

    /**
     * Provides a view for an AdapterView, either a transaction item or an empty view.
     *
     * @param position The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_EMPTY) {
            // Inflate and return the empty view if no transactions are available
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_no_transactions, parent, false);
            }
            TextView emptyMessage = convertView.findViewById(R.id.emptyMessage);
            emptyMessage.setText(R.string.no_recent_transactions); // Set the empty message text
        } else {
            // Inflate and return the transaction view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_transaction, parent, false);
            }

            // Get the transaction for the current position
            Transaction transaction = getItem(position);

            // Retrieve and set the transaction data
            ImageView icon = convertView.findViewById(R.id.transactionIcon);
            TextView title = convertView.findViewById(R.id.transactionTitle);
            TextView amount = convertView.findViewById(R.id.transactionAmount);
            TextView date = convertView.findViewById(R.id.transactionDate);

            // Set the transaction details in the views
            title.setText(transaction.getTitle());
            amount.setText(transaction.getAmount());
            date.setText(transaction.getDatetime());

            // Set the icon resource for the transaction type
            icon.setImageResource(transaction.getIconResId());
        }

        return convertView;
    }
}