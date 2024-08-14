package com.shopwallet.ituchallenger;

/**
 * The Transaction class represents a financial transaction with a title, amount, date/time, and an associated icon resource ID.
 * This class encapsulates the data for a single transaction and provides methods to access these properties.
 */
public class Transaction {
    // Fields representing the transaction's title, amount, date/time, and icon resource ID.
    private final String title;     // Title of the transaction (e.g., "Payment to John")
    private final String amount;    // Amount of the transaction as a String (e.g., "$100")
    private final String datetime;  // Date and time of the transaction
    private final int iconResId;    // Resource ID for the icon representing the transaction type

    /**
     * Constructs a new Transaction with the specified title, amount, datetime, and icon resource ID.
     *
     * @param title The title of the transaction.
     * @param amount The amount of the transaction.
     * @param datetime The date and time of the transaction.
     * @param iconResId The resource ID of the icon associated with the transaction.
     */
    public Transaction(String title, String amount, String datetime, int iconResId) {
        this.title = title;
        this.amount = amount;
        this.datetime = datetime;
        this.iconResId = iconResId;
    }

    /**
     * Gets the title of the transaction.
     *
     * @return The title of the transaction.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the amount of the transaction.
     *
     * @return The amount of the transaction.
     */
    public String getAmount() {
        return amount;
    }

    /**
     * Gets the date and time of the transaction.
     *
     * @return The datetime of the transaction.
     */
    public String getDatetime() {
        return datetime;
    }

    /**
     * Gets the resource ID of the icon associated with the transaction.
     *
     * @return The icon resource ID.
     */
    public int getIconResId() {
        return iconResId;
    }
}