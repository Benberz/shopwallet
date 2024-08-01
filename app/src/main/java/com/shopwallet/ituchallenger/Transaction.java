package com.shopwallet.ituchallenger;

import java.text.NumberFormat;
import java.util.Locale;

public class Transaction {
    private final String title;
    private final String amount;
    private final String datetime;
    private final int iconResId;

    public Transaction(String title, String amount, String datetime, int iconResId) {
        this.title = title;
        this.amount = amount;
        this.datetime = datetime;
        this.iconResId = iconResId;
    }

    public String getTitle() { return title; }

    public String getAmount() {
        return amount;
    }

    public String getDatetime() { return datetime; }

    public int getIconResId() {
        return iconResId;
    }
}


