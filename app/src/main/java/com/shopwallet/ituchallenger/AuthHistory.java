package com.shopwallet.ituchallenger;

public class AuthHistory {
    private final String title;
    private final String dateTime;
    private final String status;

    public AuthHistory(String title, String dateTime, String status) {
        this.title = title;
        this.dateTime = dateTime;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getStatus() {
        return status;
    }
}

